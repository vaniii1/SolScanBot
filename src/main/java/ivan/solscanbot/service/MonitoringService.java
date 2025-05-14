package ivan.solscanbot.service;

import com.google.common.util.concurrent.RateLimiter;
import ivan.solscanbot.bot.TelegramBot;
import ivan.solscanbot.dto.external.activity.SingleBalanceActivityResponseDto;
import ivan.solscanbot.dto.external.meta.TokenMetaResponseDto;
import ivan.solscanbot.dto.internal.BalanceActivity;
import ivan.solscanbot.dto.internal.MonitoredAddress;
import ivan.solscanbot.mapper.ActivityMapper;
import ivan.solscanbot.repository.ActivityRepository;
import ivan.solscanbot.repository.MonitoredAddressRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {
    private static final int CHUNK_SIZE = 20;
    private static final String SOLSCAN_ACCOUNT_URL = "https://solscan.io/account/";
    private static final String SOLSCAN_TOKEN_URL = "https://solscan.io/token/";
    private static final String FIRST_TRACKING_ADDRESS =
            "APZmQqyytWLMFioMsskqhWrGJCd9Fpo7L2f2YhdpSe6U";

    private final MonitoredAddressRepository addressRepository;
    private final TelegramBot telegramBot;
    private final SolScanServiceImpl solScanService;
    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;

    @Scheduled(fixedDelay = 300000)
    public void newActivityFound() {
        RateLimiter rateLimiter = RateLimiter.create(5.0);
        addressRepository.findAll().forEach(address -> {
            try {
                rateLimiter.acquire();
                Set<BalanceActivity> activities = fetchAndProcessActivities(address);
                if (!activities.isEmpty()) {
                    activityRepository.saveAll(activities);
                    sendTelegramNotification(address, activities);
                }
            } catch (Exception e) {
                log.error("Error checking activities for address {}", address.getAddress(), e);
            }
        });
    }

    private Set<BalanceActivity> fetchAndProcessActivities(MonitoredAddress address) {
        Set<SingleBalanceActivityResponseDto> newActivities =
                solScanService.getNewBalanceActivities(address.getAddress())
                        .stream()
                        .filter(act -> BigDecimal.ZERO.equals(act.getPreBalance()))
                        .collect(Collectors.toSet());

        List<String> tokenAddresses = newActivities.stream()
                .map(SingleBalanceActivityResponseDto::getTokenAddress)
                .toList();

        Map<String, TokenMetaResponseDto> metaMap = batchFetchTokenMetadata(tokenAddresses);

        return newActivities.stream()
                .map(activityMapper::toModel)
                .map(act -> enrichWithTokenMeta(act, metaMap.get(act.getTokenAddress())))
                .filter(tok -> tok.getValueInUsd().compareTo(BigDecimal.valueOf(100)) > 0)
                .peek(act -> act.setMonitoredAddress(address))
                .collect(Collectors.toSet());
    }

    private Map<String, TokenMetaResponseDto> batchFetchTokenMetadata(List<String> tokenAddresses) {
        Map<String, TokenMetaResponseDto> metaMap = new ConcurrentHashMap<>();
        List<String> addressList = new ArrayList<>(tokenAddresses);
        for (int i = 0; i < addressList.size(); i += CHUNK_SIZE) {
            List<String> chunk = addressList.subList(
                    i, Math.min(i + CHUNK_SIZE, addressList.size()));
            try {
                Map<String, TokenMetaResponseDto> chunkResults =
                        solScanService.getMetaMapFromAddresses(chunk);
                metaMap.putAll(chunkResults);
            } catch (Exception e) {
                log.error("Failed to fetch batch metadata for chunk: {}", chunk, e);
                fetchTokenMetadata(chunk, metaMap);
            }
        }
        return metaMap;
    }

    private void fetchTokenMetadata(List<String> tokenAddresses,
                                    Map<String, TokenMetaResponseDto> metaMap) {
        tokenAddresses.forEach(address -> {
            TokenMetaResponseDto meta = solScanService.getTokenMeta(address);
            metaMap.put(address, meta);
        });
    }

    private BalanceActivity enrichWithTokenMeta(BalanceActivity act, TokenMetaResponseDto meta) {
        BigDecimal price = Optional.ofNullable(meta.getPrice()).orElse(BigDecimal.ZERO);
        int decimals = meta.getDecimals() > 0 ? meta.getDecimals() : 9;
        BigDecimal normalizedAmount = act.getAmount()
                .divide(BigDecimal.TEN.pow(decimals), MathContext.DECIMAL32);
        act.setValueInUsd(normalizedAmount.multiply(price))
                .setTokenName(meta.getName())
                .setTokenSymbol(meta.getSymbol());
        return act;
    }

    private void sendTelegramNotification(MonitoredAddress address,
                                          Set<BalanceActivity> activities) {
        AtomicInteger count = new AtomicInteger(1);
        String tokens = activities.stream()
                .map(act -> String.format(
                        "%d. Token: %s\nUSD Value: $%s\nToken link: [%s](%s#balanceChanges)\n",
                        count.getAndIncrement(),
                        act.getTokenName(),
                        act.getValueInUsd().setScale(2, RoundingMode.HALF_UP),
                        shortenAddress(act.getTokenAddress()),
                        SOLSCAN_TOKEN_URL + act.getTokenAddress()
                ))
                .collect(Collectors.joining());
        String message = String.format("New activity for address: [%s](%s#balanceChanges)\n%s",
                shortenAddress(address.getAddress()),
                SOLSCAN_ACCOUNT_URL + address.getAddress(),
                tokens);
        telegramBot.sendNotification(address.getChatId(), message);
    }

    private String shortenAddress(String address) {
        return address.length() > 8
                ? address.substring(0, 4) + "..." + address.substring(address.length() - 4)
                : address;
    }

    /*public void monitorAddress() {
        try {
            if (solScanService.newTokenTransfer(FIRST_TRACKING_ADDRESS)) {
                Set<Long> ids =
                        addressRepository.findAll()
                                .stream()
                                .map(MonitoredAddress::getChatId)
                                .collect(Collectors.toSet());
                log.info("New transfer detected, sending notification");
                for (Long id : ids) {
                    telegramBot.sendNotification(id, "!!!");
                }
            }
        } catch (Exception e) {
            log.error("Error in scheduled task", e);
        }
    }*/
}
