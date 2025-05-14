package ivan.solscanbot.service;

import ivan.solscanbot.dto.external.activity.SingleBalanceActivityResponseDto;
import ivan.solscanbot.dto.external.meta.TokenMetaResponseDto;
import ivan.solscanbot.dto.external.portfolio.SingleTokenPortfolioResponseDto;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SolScanService {
    Set<SingleTokenPortfolioResponseDto> getTokensByAddress(String address);

    Set<SingleBalanceActivityResponseDto> getNewBalanceActivities(String address);

    Map<String, TokenMetaResponseDto> getMetaMapFromAddresses(List<String> addresses);

    TokenMetaResponseDto getTokenMeta(String address);

    boolean newTokenTransfer(String tokenAddress);
}
