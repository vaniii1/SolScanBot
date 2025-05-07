package ivan.solscanbot;

import ivan.solscanbot.service.DeFiMonitorBot;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.telegram.telegrambots.meta.TelegramBotsApi;

@SpringBootTest
@MockitoBean(types = {DeFiMonitorBot.class, TelegramBotsApi.class})
class SolScanBotApplicationTests {
    @Test
    void contextLoads() {
    }
}
