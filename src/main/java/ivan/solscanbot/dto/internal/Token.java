package ivan.solscanbot.dto.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Data
@Entity
@NoArgsConstructor
@SQLDelete(sql = "UPDATE tokens SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
@Table(name = "tokens")
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "token_symbol")
    private String tokenSymbol;
    @Column(unique = true, nullable = false, name = "token_address")
    private String tokenAddress;
    @Column(name = "is_deleted")
    private boolean isDeleted = false;
}
