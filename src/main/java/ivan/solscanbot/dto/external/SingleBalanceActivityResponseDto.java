package ivan.solscanbot.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

@Data
public class SingleBalanceActivityResponseDto {
    private String address;
    @JsonProperty("token_address")
    private String tokenAddress;
    @JsonProperty("activity_type")
    private String activityType;
    private BigDecimal amount;
    @JsonProperty("pre_balance")
    private BigDecimal preBalance;
    private Date time;
}
