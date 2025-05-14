package ivan.solscanbot.dto.external.meta;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class TokenMetaResponseDto {
    private String name;
    private String symbol;
    private String address;
    private int decimals;
    private BigDecimal price;
}
