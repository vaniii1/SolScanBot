package ivan.solscanbot.mapper;

import ivan.solscanbot.config.MapperConfig;
import ivan.solscanbot.dto.external.SingleBalanceActivityResponseDto;
import ivan.solscanbot.dto.internal.BalanceActivity;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface ActivityMapper {
    BalanceActivity toModel(SingleBalanceActivityResponseDto activityDto);
}
