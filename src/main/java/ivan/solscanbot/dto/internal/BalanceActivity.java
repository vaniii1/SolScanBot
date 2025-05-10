package ivan.solscanbot.dto.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Data
@Entity
@NoArgsConstructor
@Accessors(chain = true)
@SQLDelete(sql = "UPDATE roles SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class BalanceActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String address;
    @Column(nullable = false, name = "token_address")
    private String tokenAddress;
    @Column(nullable = false, name = "token_name")
    private String tokenName;
    @Column(nullable = false, name = "token_symbol")
    private String tokenSymbol;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = false)
    private Date time;
    @Column(nullable = false, name = "is_deleted")
    private boolean isDeleted = false;
}
