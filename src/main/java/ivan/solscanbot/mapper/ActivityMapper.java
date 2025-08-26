package ivan.solscanbot.mapper;

import ivan.solscanbot.config.MapperConfig;
import ivan.solscanbot.dto.external.activity.SingleBalanceActivityResponseDto;
import ivan.solscanbot.dto.internal.BalanceActivity;
import ivan.solscanbot.dto.internal.Token;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface ActivityMapper {
    BalanceActivity toModel(SingleBalanceActivityResponseDto activityDto);

    default BalanceActivity toModel(SingleBalanceActivityResponseDto activityDto,
                                    Token token) {
        BalanceActivity act = toModel(activityDto);
        act.setToken(token);
        return act;
    }
}
