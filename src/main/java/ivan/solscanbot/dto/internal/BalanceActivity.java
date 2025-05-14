package ivan.solscanbot.dto.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Data
@Entity
@NoArgsConstructor
@Accessors(chain = true)
@SQLDelete(sql = "UPDATE balance_activities SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
@Table(name = "balance_activities")
public class BalanceActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, name = "token_address")
    private String tokenAddress;
    @Column(name = "token_name")
    private String tokenName;
    @Column(nullable = false, name = "token_symbol")
    private String tokenSymbol;
    @Column(nullable = false, name = "value_in_usd")
    private BigDecimal valueInUsd;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = false)
    private Date time;
    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JoinColumn(nullable = false, name = "address_id")
    private MonitoredAddress monitoredAddress;
    @Column(nullable = false, name = "is_deleted")
    private boolean isDeleted = false;
}
