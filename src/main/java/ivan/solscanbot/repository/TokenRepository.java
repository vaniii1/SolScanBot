package ivan.solscanbot.repository;

import ivan.solscanbot.dto.internal.Token;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByTokenAddress(String tokenAddress);
}
