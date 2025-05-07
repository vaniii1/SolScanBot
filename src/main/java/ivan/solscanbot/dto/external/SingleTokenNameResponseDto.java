package ivan.solscanbot.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SingleTokenNameResponseDto {
    @JsonProperty("token_name")
    private String tokenName;
}
