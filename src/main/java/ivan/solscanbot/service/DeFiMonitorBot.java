package ivan.solscanbot.service;

import ivan.solscanbot.dto.internal.MonitoredAddress;
import ivan.solscanbot.dto.internal.Token;
import ivan.solscanbot.exception.AddressAlreadyExistsException;
import ivan.solscanbot.exception.AddressNotMonitoredException;
import ivan.solscanbot.exception.InvalidAddressException;
import ivan.solscanbot.exception.UserNotHaveAnyMonitoredAddressesException;
import ivan.solscanbot.mapper.TokenMapper;
import ivan.solscanbot.repository.MonitoredAddressRepository;
import ivan.solscanbot.repository.TokenRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class DeFiMonitorBot extends TelegramLongPollingBot {
    private static final String SOLANA_ADDRESS_PATTERN = "[1-9A-HJ-NP-Za-km-z]{32,44}";

    private final String token;
    private final String username;
    private final MonitoredAddressRepository addressRepository;
    private final TokenRepository tokenRepository;
    private final SolScanServiceImpl solScanService;
    private final TokenMapper tokenMapper;

    public DeFiMonitorBot(
            @Value("${telegram.bot.token}") String token,
            @Value("${telegram.bot.username}") String username,
            MonitoredAddressRepository addressRepository, TokenRepository tokenRepository,
            SolScanServiceImpl solScanService, TokenMapper tokenMapper
    ) {
        this.token = token;
        this.username = username;
        this.addressRepository = addressRepository;
        this.tokenRepository = tokenRepository;
        this.solScanService = solScanService;
        this.tokenMapper = tokenMapper;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.startsWith("/add")) {
                handleAddAddress(chatId, messageText);
            } else if (messageText.startsWith("/list")) {
                handleListAddresses(chatId);
            } else if (messageText.startsWith("/remove")) {
                handleRemoveAddress(chatId, messageText);
            } else if (messageText.startsWith("/portfolio")) {
                handleAddressPortfolio(chatId, messageText);
            } else {
                sendMessage(
                        chatId, "Unknown command. Use /add, /list, or /remove");
            }
        }
    }

    public void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException("Error while sending a message.", e);
        }
    }

    private void handleAddressPortfolio(long chatId, String messageText) {
        String address = getSolanaAddressFromMessage(messageText);
        verifySolanaAddress(chatId, address);
        verifyUserHasCertainAddress(chatId, address);

        MonitoredAddress solanaAddress = addressRepository.findByAddress(address)
                .orElseThrow(() -> new AddressNotMonitoredException(address));

        String tokens = Optional.ofNullable(solanaAddress.getTokens())
                .orElse(Collections.emptySet())
                .stream()
                .map(Token::getTokenName)
                .collect(Collectors.joining(", "));

        sendMessage(chatId, "Tokens in portfolio:\n" + tokens);
    }

    public void handleAddAddress(long chatId, String message) {
        String solanaAddress = getSolanaAddressFromMessage(message);
        verifySolanaAddress(chatId, solanaAddress);
        verifyAddressAlreadyAdded(chatId, solanaAddress);
        Set<Token> tokensFromSolScan = solScanService.getTokensByAddress(solanaAddress)
                .stream()
                .map(tokenMapper::toModel)
                .collect(Collectors.toSet());
        Set<Token> managedTokens = tokensFromSolScan.stream()
                .map(t -> tokenRepository.findByTokenName(t.getTokenName())
                        .orElseGet(() -> tokenRepository.save(t)))
                .collect(Collectors.toSet());
        MonitoredAddress address = new MonitoredAddress();
        address.setAddress(solanaAddress);
        address.setChatId(chatId);
        address.setTokens(managedTokens);
        addressRepository.save(address);
        sendMessage(chatId, "address\n" + solanaAddress + "\nis added to monitoring");
    }

    private void handleListAddresses(long chatId) {
        List<MonitoredAddress> addresses = addressRepository.findByChatId(chatId);
        verifyUserHasAddresses(chatId, addresses);
        AtomicInteger count = new AtomicInteger(1);
        String addressList = addresses
                .stream()
                .map(adr -> count.getAndIncrement() + ". " + adr.getAddress())
                .collect(Collectors.joining("\n"));
        sendMessage(chatId, "List of monitored addresses:\n" + addressList);
    }

    private void handleRemoveAddress(long chatId, String message) {
        String solanaAddress = getSolanaAddressFromMessage(message);
        verifySolanaAddress(chatId, solanaAddress);
        addressRepository.deleteByAddressAndChatId(solanaAddress, chatId);
        sendMessage(
                chatId, "address\n" + solanaAddress + "\nis removed from monitoring");
    }

    public void sendNotification(long chatId, String notification) {
        sendMessage(chatId, notification);
    }

    private String getSolanaAddressFromMessage(String message) {
        return message.substring(4).trim();
    }

    private void verifySolanaAddress(long chatId, String address) {
        if (address == null || !address.matches(SOLANA_ADDRESS_PATTERN)) {
            sendMessage(chatId, "Invalid Solana Address");
            throw new InvalidAddressException("Invalid Solana Address");
        }
    }

    private void verifyAddressAlreadyAdded(long chatId, String address) {
        if (addressRepository.existsByAddressAndChatId(address, chatId)) {
            sendMessage(chatId,"You already have this address in your list");
            throw new AddressAlreadyExistsException("You already have this address in your list");
        }
    }

    private void verifyUserHasAddresses(long chatId, List<MonitoredAddress> addresses) {
        if (addresses.isEmpty()) {
            sendMessage(chatId, "You have no monitored addresses");
            throw new UserNotHaveAnyMonitoredAddressesException("You have no monitored addresses");
        }
    }

    private void verifyUserHasCertainAddress(long chatId, String address) {
        if (!addressRepository.existsByAddressAndChatId(address, chatId)) {
            sendMessage(chatId,
                    "You need to add this address to monitored address first '/add your_address'");
            throw new AddressNotMonitoredException(
                    "You need to add this address to monitored address first '/add your_address'");
        }
    }
}
