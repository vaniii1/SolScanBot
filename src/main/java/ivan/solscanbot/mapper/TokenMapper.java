package ivan.solscanbot.mapper;

import ivan.solscanbot.config.MapperConfig;
import ivan.solscanbot.dto.external.SingleTokenPortfolioResponseDto;
import ivan.solscanbot.dto.internal.Token;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface TokenMapper {
    Token toModel(SingleTokenPortfolioResponseDto dto);
}
