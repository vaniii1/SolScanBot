package ivan.solscanbot.bot;

import ivan.solscanbot.exception.AddressAlreadyExistsException;
import ivan.solscanbot.exception.AddressNotMonitoredException;
import ivan.solscanbot.exception.ExceedsAmountOfAddressesException;
import ivan.solscanbot.exception.InvalidAddressException;
import ivan.solscanbot.exception.UserNotHaveAnyMonitoredAddressesException;
import ivan.solscanbot.service.TelegramService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final String token;
    private final String username;
    private final TelegramService telegramService;

    public TelegramBot(
            @Value("${telegram.bot.token}") String token,
            @Value("${telegram.bot.username}") String username,
            TelegramService telegramService
    ) {
        this.token = token;
        this.username = username;
        this.telegramService = telegramService;
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

            if (messageText.startsWith("/help")) {
                handleHelp(chatId);
            } else if (messageText.startsWith("/add")) {
                handleAddAddresses(chatId, messageText);
            } else if (messageText.startsWith("/list")) {
                handleListAddresses(chatId);
            } else if (messageText.startsWith("/remove")) {
                handleRemoveAddress(chatId, messageText);
            } else if (messageText.startsWith("/portfolio")) {
                handleGetPortfolios(chatId, messageText);
            } else {
                sendMessage(
                        chatId, "Unknown command. Use /help to get clarifications");
            }
        }
    }

    public void sendNotification(long chatId, String notification) {
        sendMessage(chatId, notification);
    }

    private void handleHelp(long chatId) {
        String message = telegramService.getHelp();
        sendMessage(chatId, message);
    }

    private void handleGetPortfolios(long chatId, String messageText) {
        try {
            String message = telegramService.getPortfolios(chatId, messageText);
            sendMessage(chatId, message);
        } catch (InvalidAddressException | AddressNotMonitoredException
                 | ExceedsAmountOfAddressesException e) {
            sendMessage(chatId, e.getMessage());
        }
    }

    private void handleAddAddresses(long chatId, String messageText) {
        try {
            String message = telegramService.addAddresses(chatId, messageText);
            sendMessage(chatId, message);
        } catch (InvalidAddressException | AddressAlreadyExistsException
                 | ExceedsAmountOfAddressesException e) {
            sendMessage(chatId, e.getMessage());
        }
    }

    private void handleListAddresses(long chatId) {
        try {
            String message = telegramService.listAddresses(chatId);
            sendMessage(chatId, message);
        } catch (UserNotHaveAnyMonitoredAddressesException e) {
            sendMessage(chatId, e.getMessage());
        }
    }

    private void handleRemoveAddress(long chatId, String messageText) {
        try {
            String message = telegramService.removeAddresses(chatId, messageText);
            sendMessage(chatId, message);
        } catch (InvalidAddressException | AddressNotMonitoredException
                    | ExceedsAmountOfAddressesException e) {
            sendMessage(chatId, e.getMessage());
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.enableMarkdown(true);
        message.disableWebPagePreview();
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException("Error while sending a message.", e);
        }
    }
}
