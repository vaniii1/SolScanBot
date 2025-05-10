package ivan.solscanbot.service;

import ivan.solscanbot.dto.external.TokenMetaResponseDto;
import ivan.solscanbot.dto.internal.BalanceActivity;
import ivan.solscanbot.dto.internal.MonitoredAddress;
import ivan.solscanbot.mapper.ActivityMapper;
import ivan.solscanbot.repository.ActivityRepository;
import ivan.solscanbot.repository.MonitoredAddressRepository;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jvnet.hk2.annotations.Service;
import org.springframework.scheduling.annotation.Scheduled;

@Service
@RequiredArgsConstructor
public class MonitoringService {
    private final MonitoredAddressRepository addressRepository;
    private final DeFiMonitorBot deFiMonitorBot;
    private final SolScanServiceImpl solScanService;
    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;

    @Scheduled(fixedRate = 15000)
    public void sendTokenList() {
        for (MonitoredAddress address : addressRepository.findAll()) {
            try {
                Set<BalanceActivity> activities =
                        solScanService.getNewBalanceActivities(address.getAddress())
                                .stream()
                                .filter(a -> BigDecimal.ZERO.equals(a.getPreBalance()))
                                .map(activityMapper::toModel)
                                .collect(Collectors.toSet());
                if (!activities.isEmpty()) {
                    StringBuilder notification = new StringBuilder();
                    notification.append("New activity for Solana address\n")
                            .append(address.getAddress())
                            .append("was found");
                    int count = 1;
                    for (BalanceActivity activity : activities) {
                        TokenMetaResponseDto meta =
                                solScanService.getTokenMetaFromAddress(address.getAddress());
                        activity.setTokenName(meta.getName())
                                .setTokenSymbol(meta.getSymbol());
                        notification.append("\n").append(count++)
                                .append(". token address: ").append(activity.getTokenAddress())
                                .append("\n")
                                .append(". token address: ").append(activity.getTokenAddress())
                                .append("\n")
                                .append("token amount: ").append(activity.getAmount()).append("\n")
                                .append("time: ").append(activity.getTime()).append("\n");
                    }
                    activityRepository.saveAll(activities);
                    deFiMonitorBot.sendNotification(address.getChatId(), notification.toString());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
