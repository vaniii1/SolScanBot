package ivan.solscanbot.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class SingleTokenPortfolioResponseDto {
    @JsonProperty("token_name")
    private String tokenName;
    @JsonProperty("token_address")
    private String tokenAddress;
    @JsonProperty("token_balance")
    private String tokenBalance;
    @JsonProperty("value")
    private BigDecimal tokenValue;
}
