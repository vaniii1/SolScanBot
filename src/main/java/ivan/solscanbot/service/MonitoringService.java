package ivan.solscanbot.service;

import ivan.solscanbot.dto.internal.MonitoredAddress;
import ivan.solscanbot.repository.MonitoredAddressRepository;
import org.jvnet.hk2.annotations.Service;
import org.springframework.scheduling.annotation.Scheduled;

@Service
public class MonitoringService {
    private final MonitoredAddressRepository addressRepository;
    private final DeFiMonitorBot deFiMonitorBot;
    private final SolScanServiceImpl solScanService;

    public MonitoringService(
            MonitoredAddressRepository addressRepository,
            DeFiMonitorBot deFiMonitorBot,
            SolScanServiceImpl solScanService) {
        this.addressRepository = addressRepository;
        this.deFiMonitorBot = deFiMonitorBot;
        this.solScanService = solScanService;
    }

    @Scheduled(fixedRate = 15000)
    public void sendTokenList() {
        for (MonitoredAddress address : addressRepository.findAll()) {
            try {
                String notification = "Tokens for address: " + address.getAddress()
                        + solScanService.getTokensByAddress(address.getAddress());
                deFiMonitorBot.sendNotification(address.getId(), notification);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
