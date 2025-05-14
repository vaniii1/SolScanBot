package ivan.solscanbot.repository;

import ivan.solscanbot.dto.internal.MonitoredAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoredAddressRepository
        extends JpaRepository<MonitoredAddress, Long> {

    void deleteByAddressAndChatId(String address, Long chatId);

    boolean existsByAddressAndChatId(String address, Long chatId);

    List<MonitoredAddress> findByChatId(Long chatId);

    @EntityGraph(attributePaths = "tokens")
    Optional<MonitoredAddress> findByAddress(String address);
}
