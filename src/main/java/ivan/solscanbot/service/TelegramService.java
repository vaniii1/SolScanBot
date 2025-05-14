package ivan.solscanbot.service;

public interface TelegramService {
    String getHelp();

    String getPortfolios(long chatId, String messageText);

    String addAddresses(long chatId, String message);

    String listAddresses(long chatId);

    String removeAddresses(long chatId, String message);
}
