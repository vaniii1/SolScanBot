package ivan.solscanbot.service;

import ivan.solscanbot.dto.external.SingleBalanceActivityResponseDto;
import ivan.solscanbot.dto.external.SingleTokenPortfolioResponseDto;
import ivan.solscanbot.dto.external.TokenMetaResponseDto;
import java.util.Set;

public interface SolScanService {
    Set<SingleTokenPortfolioResponseDto> getTokensByAddress(String address);

    Set<SingleBalanceActivityResponseDto> getNewBalanceActivities(String address);

    TokenMetaResponseDto getTokenMetaFromAddress(String address);
}
