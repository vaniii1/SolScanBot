package ivan.solscanbot.dto.external.portfolio;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class SingleTokenPortfolioResponseDto {
    @JsonProperty("token_name")
    private String name;
    @JsonProperty("token_symbol")
    private String symbol;
    @JsonProperty("token_address")
    private String address;
    @JsonProperty("balance")
    private String tokenBalance;
    @JsonProperty("value")
    private BigDecimal tokenValue;
}
