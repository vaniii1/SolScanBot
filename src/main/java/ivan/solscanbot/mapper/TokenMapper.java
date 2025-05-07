package ivan.solscanbot.mapper;

import ivan.solscanbot.config.MapperConfig;
import ivan.solscanbot.dto.external.SingleTokenNameResponseDto;
import ivan.solscanbot.dto.internal.Token;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface TokenMapper {
    Token toModel(SingleTokenNameResponseDto dto);
}
