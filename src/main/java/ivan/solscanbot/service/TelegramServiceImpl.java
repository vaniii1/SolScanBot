package ivan.solscanbot.service;

import ivan.solscanbot.dto.external.portfolio.SingleTokenPortfolioResponseDto;
import ivan.solscanbot.dto.internal.MonitoredAddress;
import ivan.solscanbot.dto.internal.Token;
import ivan.solscanbot.mapper.TokenMapper;
import ivan.solscanbot.repository.MonitoredAddressRepository;
import ivan.solscanbot.repository.TokenRepository;
import ivan.solscanbot.verifier.SolanaAddressVerifier;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TelegramServiceImpl implements TelegramService {
    private static final String SOLSCAN_ACCOUNT_URL = "https://solscan.io/account/";
    private static final String SOLSCAN_TOKEN_URL = "https://solscan.io/token/";
    private static final int AMOUNT_OF_ADDRESSES = 3;

    private final MonitoredAddressRepository addressRepository;
    private final TokenRepository tokenRepository;
    private final SolScanServiceImpl solScanService;
    private final TokenMapper tokenMapper;
    private final SolanaAddressVerifier addressVerifier;

    @Override
    public String getHelp() {
        return getHelpMessage();
    }

    @Override
    public String getPortfolios(long chatId, String message) {
        Set<String> addresses = getSolanaAddressesFromMessage(AMOUNT_OF_ADDRESSES, message);
        addressVerifier.verifyUserHasCertainAddresses(chatId, addresses);

        List<MonitoredAddress> monitoredAddresses = addresses.stream()
                .map(addressRepository::findByAddress)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        String formattedAddresses = monitoredAddresses.stream()
                .map(this::formatSingleMonitoredAddress)
                .collect(Collectors.joining("\n\n"));
        return String.format("Tokens in portfolio:\n%s", formattedAddresses);
    }

    @Override
    public String addAddresses(long chatId, String message) {
        Set<String> addresses = getSolanaAddressesFromMessage(AMOUNT_OF_ADDRESSES, message);
        addressVerifier.verifyAddressesAlreadyAdded(chatId, addresses);
        Set<String> addedAddresses = addresses.stream()
                .map(address -> addSingleMonitoredAddress(chatId, address))
                .collect(Collectors.toSet());
        return String.format("Added following address(-es):\n%s",
                formatAddressList(addedAddresses));
    }

    @Override
    public String listAddresses(long chatId) {
        List<MonitoredAddress> monitoredAddresses = addressRepository.findByChatId(chatId);
        addressVerifier.verifyUserHasAddresses(monitoredAddresses);
        Set<String> solanaAddresses = monitoredAddresses.stream()
                .map(MonitoredAddress::getAddress)
                .collect(Collectors.toSet());
        return String.format("Your monitored address(-es):\n%s",
                formatAddressList(solanaAddresses));
    }

    @Override
    @Transactional
    public String removeAddresses(long chatId, String message) {
        Set<String> addresses = getSolanaAddressesFromMessage(AMOUNT_OF_ADDRESSES, message);
        addressVerifier.verifyUserHasCertainAddresses(chatId, addresses);
        addresses.forEach(address -> {
            addressRepository.deleteByAddressAndChatId(address, chatId);
        });
        return String.format("Removed address(-es):\n%s",
                formatAddressList(addresses));
    }

    private String formatAddressList(Set<String> addresses) {
        AtomicInteger count = new AtomicInteger(1);
        return addresses.stream().map(
                adr -> {
                    String url = buildSolscanAddressUrl(adr);
                    return String.format("%d. [%s](%s)",
                            count.getAndIncrement(),
                            shortenAddress(adr),
                            url);
                })
                .collect(Collectors.joining("\n")
        );
    }

    private String formatSingleMonitoredAddress(MonitoredAddress monitoredAddress) {
        String addressUrl = buildSolscanAddressUrl(monitoredAddress.getAddress());
        String tokens = formatTokenList(monitoredAddress);
        return String.format(
                "Address link: [%s](%s)\nTokens:\n%s",
                shortenAddress(monitoredAddress.getAddress()),
                addressUrl,
                tokens
        );
    }

    private String formatTokenList(MonitoredAddress solanaAddress) {
        AtomicInteger count = new AtomicInteger(1);
        return solanaAddress.getTokens().stream()
                .map(token -> {
                    String tokenUrl = SOLSCAN_TOKEN_URL + token.getTokenAddress() + "#holders";
                    return String.format(
                            "%d. %s: [%s](%s)",
                            count.getAndIncrement(),
                            token.getTokenSymbol(),
                            shortenAddress(token.getTokenAddress()),
                            tokenUrl
                    );
                })
                .collect(Collectors.joining("\n"));
    }

    private String addSingleMonitoredAddress(long chatId, String solanaAddress) {
        Set<SingleTokenPortfolioResponseDto> tokensFromSolScan
                = getFilteredTokensFromSolscan(solanaAddress);

        Set<Token> managedTokens = mapAndSaveTokensFromSolscan(tokensFromSolScan);

        MonitoredAddress address = new MonitoredAddress();
        address.setAddress(solanaAddress);
        address.setChatId(chatId);
        address.setTokens(managedTokens);
        addressRepository.save(address);
        return address.getAddress();
    }

    private String buildSolscanAddressUrl(String address) {
        return SOLSCAN_ACCOUNT_URL + address + "#balanceChanges";
    }

    private String shortenAddress(String address) {
        return address.length() > 8
                ? address.substring(0, 4) + "..." + address.substring(address.length() - 4)
                : address;
    }

    private Set<Token> mapAndSaveTokensFromSolscan(
            Set<SingleTokenPortfolioResponseDto> tokensFromSolScan) {
        return tokensFromSolScan.stream()
                .map(tokenMapper::toModel)
                .map(t -> tokenRepository.findByTokenAddress(t.getTokenAddress())
                        .orElseGet(() -> tokenRepository.save(t)))
                .collect(Collectors.toSet());
    }

    private Set<SingleTokenPortfolioResponseDto> getFilteredTokensFromSolscan(
            String solanaAddress) {
        return solScanService.getTokensByAddress(solanaAddress)
                .stream()
                .filter(tok -> tok.getTokenSymbol() != null)
                .filter(tok -> tok.getTokenValue().compareTo(BigDecimal.TEN) > 0)
                .sorted(Comparator.comparing(SingleTokenPortfolioResponseDto::getTokenValue)
                        .reversed())
                .limit(10)
                .collect(Collectors.toSet());
    }

    private Set<String> getSolanaAddressesFromMessage(int amountOfAddresses, String message) {
        addressVerifier.verifyAddressIsProvided(message);
        String normalizedMessage = message.replaceAll("\\s+", " ").trim();
        String[] addresses = normalizedMessage.substring(
                normalizedMessage.indexOf(" ") + 1).split("\\s+");
        addressVerifier.verifyAmountOfAddresses(amountOfAddresses, addresses);
        addressVerifier.verifyValidSolanaAddresses(addresses);
        return Arrays.stream(addresses)
                .filter(addr -> !addr.isEmpty())
                .collect(Collectors.toSet());
    }

    private String getHelpMessage() {
        return """
        SolScan Bot Help
        
        This bot helps you monitor Solana addresses and track their token portfolios.
        Here are the available commands:
        
        Basic Commands:
        `/help` - Show this help message
        `/list` - List all your monitored addresses
        
        Address Management:
        `/add <address1> <address2...>` - Add up to 3 Solana addresses to monitor
        `/remove <address1> <address2...>` - Remove addresses from monitoring
        
        Portfolio Tracking:
        `/portfolio <address1> <address2...>` - Show token portfolio
        of 10 most valuable tokens for specified addresses
        
        Usage Examples:
        Add addresses: `/add D8wZ...3j4H G2eF...7k9L`
        Remove address: `/remove D8wZ...3j4H`
        Check portfolio: `/portfolio D8wZ...3j4H`
        
        Notes:
        • With one request you can '/add', '/remove' or '/portfolio' up to 3 addresses at a time.
        If you want to manage more addresses send more requests
        • Addresses are displayed in shortened format (first 4 + last 4 chars)
            """;
    }
}
