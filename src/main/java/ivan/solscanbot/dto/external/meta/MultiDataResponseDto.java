package ivan.solscanbot.dto.external.meta;

import java.util.Set;
import lombok.Data;

@Data
public class MultiDataResponseDto {
    private Set<TokenMetaResponseDto> data;
}
