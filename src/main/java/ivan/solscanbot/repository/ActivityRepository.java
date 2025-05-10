package ivan.solscanbot.repository;

import ivan.solscanbot.dto.internal.BalanceActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<BalanceActivity, Long> {
}
