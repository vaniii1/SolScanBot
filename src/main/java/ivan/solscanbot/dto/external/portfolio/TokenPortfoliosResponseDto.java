package ivan.solscanbot.dto.external.portfolio;

import java.util.Set;
import lombok.Data;

@Data
public class TokenPortfoliosResponseDto {
    private Set<SingleTokenPortfolioResponseDto> tokens;
}
