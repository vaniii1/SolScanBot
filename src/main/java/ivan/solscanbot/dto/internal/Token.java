package ivan.solscanbot.dto.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Data
@Entity
@NoArgsConstructor
@SQLDelete(sql = "UPDATE roles SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false, name = "token_name")
    private String tokenName;
    @Column(unique = true, nullable = false, name = "token_address")
    private String tokenAddress;
    @Column(unique = true, nullable = false, name = "token_balance")
    private String tokenBalance;
    @Column(unique = true, nullable = false, name = "token_value")
    private BigDecimal tokenValue;
    @Column(name = "is_deleted")
    private boolean isDeleted = false;
}
