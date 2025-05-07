package ivan.solscanbot.service;

import ivan.solscanbot.dto.external.SingleTokenNameResponseDto;
import java.util.Set;

public interface SolScanService {
    Set<SingleTokenNameResponseDto> getTokensByAddress(String address);
}
