package ivan.solscanbot.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class DeFiMonitorBotImpl extends TelegramLongPollingBot {
    private final String token;
    private final String username;

    public DeFiMonitorBotImpl(
            @Value("${telegram.bot.token}") String token,
            @Value("${telegram.bot.username}") String username
    ) {
        this.token = token;
        this.username = username;
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
            } else {
                sendMessage(chatId, "Unknown command. Use /add, /list, or /remove");
            }
        }
    }

    private void handleAddAddress(long chatId, String message) {
        sendMessage(chatId, "Address added to monitoring");
    }

    private void handleListAddresses(long chatId) {
        sendMessage(chatId, "List of monitored addresses...");
    }

    private void handleRemoveAddress(long chatId, String message) {
        sendMessage(chatId, "Address removed from monitoring");
    }

    public void sendNotification(long chatId, String notification) {
        sendMessage(chatId, notification);
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
