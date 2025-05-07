package ivan.solscanbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ivan.solscanbot.dto.external.SingleTokenNameResponseDto;
import ivan.solscanbot.dto.external.TokenNamesResponseDto;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SolScanServiceImpl implements SolScanService {
    private static final String SOL_SCAN_URL = "https://pro-api.solscan.io/v2.0/account/portfolio";
    @Value("${sol.scan.key}")
    private String solScanKey;
    private final ObjectMapper objectMapper;

    @Override
    public Set<SingleTokenNameResponseDto> getTokensByAddress(String address) {
        HttpClient httpClient = HttpClient.newHttpClient();
        String url = SOL_SCAN_URL + "?address=" + address;
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .header("accept", "application/json")
                .header("token", solScanKey)
                .build();
        try {
            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            TokenNamesResponseDto tokens =
                    objectMapper.readValue(response.body(), TokenNamesResponseDto.class);
            return tokens.getTokens();
        } catch (Exception e) {
            throw new RuntimeException("An error occurred. Please try again later.");
        }
    }
}
