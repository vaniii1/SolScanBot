package ivan.solscanbot.service;

import com.google.common.util.concurrent.RateLimiter;
import ivan.solscanbot.bot.TelegramBot;
import ivan.solscanbot.dto.external.activity.SingleBalanceActivityResponseDto;
import ivan.solscanbot.dto.external.meta.TokenMetaResponseDto;
import ivan.solscanbot.dto.internal.BalanceActivity;
import ivan.solscanbot.dto.internal.MonitoredAddress;
import ivan.solscanbot.dto.internal.Token;
import ivan.solscanbot.mapper.ActivityMapper;
import ivan.solscanbot.mapper.TokenMapper;
import ivan.solscanbot.repository.ActivityRepository;
import ivan.solscanbot.repository.MonitoredAddressRepository;
import ivan.solscanbot.repository.TokenRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

    private final MonitoredAddressRepository addressRepository;
    private final TelegramBot telegramBot;
    private final SolScanServiceImpl solScanService;
    private final ActivityRepository activityRepository;
    private final TokenRepository tokenRepository;
    private final ActivityMapper activityMapper;
    private final TokenMapper tokenMapper;
    private RateLimiter rateLimiter = RateLimiter.create(1.0);

    @Scheduled(fixedDelay = 60000)
    public void newActivityFound() {
        addressRepository.findAll().forEach(address -> {
            try {
                rateLimiter.acquire();
                Set<BalanceActivity> activities = fetchAndProcessActivities(address);
                if (!activities.isEmpty()) {
                    activityRepository.saveAll(activities);
                    log.info("New activity found");
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
        log.info("Fetching new activities for address {}", address.getAddress());
        if (newActivities.isEmpty()) {
            return Collections.emptySet();
        }
        List<String> tokenAddresses = newActivities.stream()
                .map(SingleBalanceActivityResponseDto::getTokenAddress)
                .distinct()
                .toList();

        Map<String, TokenMetaResponseDto> metaMap = batchFetchTokenMetadata(tokenAddresses);

        return newActivities.stream()
                .map(dto -> {
                    TokenMetaResponseDto meta = metaMap.get(dto.getTokenAddress());
                    if (meta == null) {
                        return Optional.<BalanceActivity>empty();
                    }
                    return enrichWithTokenMeta(dto, meta);
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .peek(act -> act.setMonitoredAddress(address))
                .collect(Collectors.toSet());
    }

    private Map<String, TokenMetaResponseDto> batchFetchTokenMetadata(List<String> tokenAddresses) {
        Map<String, TokenMetaResponseDto> metaMap = new ConcurrentHashMap<>();
        log.info("Fetching token metadata for addresses {}", tokenAddresses);
        if (tokenAddresses.isEmpty()) {
            return metaMap;
        }

        List<List<String>> chunks = partitionList(tokenAddresses);
        chunks.parallelStream().forEach(chunk -> {
            try {
                Map<String, TokenMetaResponseDto> chunkResults =
                        solScanService.getMetaMapFromAddresses(chunk);
                metaMap.putAll(chunkResults);
            } catch (Exception e) {
                log.warn("Batch metadata fetch failed, falling back to individual requests", e);
                fetchTokenMetadata(chunk, metaMap);
            }
        });

        return metaMap;
    }

    private <T> List<List<T>> partitionList(List<T> list) {
        return IntStream.range(0, (list.size() + CHUNK_SIZE - 1) / CHUNK_SIZE)
                .mapToObj(i -> list.subList(
                        i * CHUNK_SIZE, Math.min(list.size(), (i + 1) * CHUNK_SIZE)))
                .collect(Collectors.toList());
    }

    private void fetchTokenMetadata(List<String> tokenAddresses,
                                    Map<String, TokenMetaResponseDto> metaMap) {
        tokenAddresses.forEach(address -> {
            try {
                TokenMetaResponseDto meta = solScanService.getTokenMeta(address);
                if (meta != null) {
                    metaMap.put(address, meta);
                }
            } catch (Exception e) {
                log.error("Failed to fetch metadata for token: {}", address, e);
            }
        });
    }

    private Optional<BalanceActivity> enrichWithTokenMeta(
            SingleBalanceActivityResponseDto dto,
            TokenMetaResponseDto meta
    ) {
        BigDecimal price = Optional.ofNullable(meta.getPrice()).orElse(BigDecimal.ZERO);
        int decimals = meta.getDecimals() > 0 ? meta.getDecimals() : 9;

        BigDecimal normalizedAmount = dto.getAmount()
                .divide(BigDecimal.TEN.pow(decimals), MathContext.DECIMAL32);
        BigDecimal valueInUsd = normalizedAmount.multiply(price);
        if (valueInUsd.compareTo(BigDecimal.valueOf(1)) > 0) {
            Token token = tokenRepository.findByAddress(meta.getAddress())
                    .orElseGet(() -> tokenRepository.save(tokenMapper.toModelFromMetaDto(meta)));

            BalanceActivity act = activityMapper.toModel(dto, token);
            act.setValueInUsd(valueInUsd);
            log.info("Saving the Token: {} and creating Balance Activity entity: {}.",
                    token.getName(), act.getValueInUsd());
            return Optional.of(act);
        }
        return Optional.empty();
    }

    private void sendTelegramNotification(MonitoredAddress address,
                                          Set<BalanceActivity> activities) {
        String tokens = formatTokensMessage(activities);
        String message = String.format("New activity for address: [%s](%s#balanceChanges)\n%s",
                shortenAddress(address.getAddress()),
                SOLSCAN_ACCOUNT_URL + address.getAddress(),
                tokens);
        telegramBot.sendNotification(address.getChatId(), message);
    }

    private String formatTokensMessage(Set<BalanceActivity> balanceActivities) {
        AtomicInteger counter = new AtomicInteger(1);
        return balanceActivities.stream()
                .map(act -> String.format(
                        "%d. Token: %s\nUSD Value: $%s\nToken link: [%s](%s#balanceChanges)\n",
                        counter.getAndIncrement(),
                        act.getToken().getName(),
                        act.getValueInUsd().setScale(2, RoundingMode.HALF_UP),
                        shortenAddress(act.getToken().getAddress()),
                        SOLSCAN_TOKEN_URL + act.getToken().getAddress()
                ))
                .collect(Collectors.joining());
    }

    private String shortenAddress(String address) {
        return address.length() > 8
                ? address.substring(0, 4) + "..." + address.substring(address.length() - 4)
                : address;
    }
}
