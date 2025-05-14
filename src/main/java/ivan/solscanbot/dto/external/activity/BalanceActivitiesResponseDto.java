package ivan.solscanbot.dto.external.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import lombok.Data;

@Data
public class BalanceActivitiesResponseDto {
    @JsonProperty("data")
    private Set<SingleBalanceActivityResponseDto> activities;
}
