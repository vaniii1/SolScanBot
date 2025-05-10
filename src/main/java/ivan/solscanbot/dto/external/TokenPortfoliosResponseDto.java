package ivan.solscanbot.dto.external;

import java.util.Set;
import lombok.Data;

@Data
public class TokenPortfoliosResponseDto {
    private Set<SingleTokenPortfolioResponseDto> tokens;
}
