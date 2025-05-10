package ivan.solscanbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ivan.solscanbot.dto.external.BalanceActivitiesResponseDto;
import ivan.solscanbot.dto.external.SingleBalanceActivityResponseDto;
import ivan.solscanbot.dto.external.SingleTokenPortfolioResponseDto;
import ivan.solscanbot.dto.external.TokenMetaResponseDto;
import ivan.solscanbot.dto.external.TokenPortfoliosResponseDto;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SolScanServiceImpl implements SolScanService {
    private static final String SOL_SCAN_PORTFOLIO_URL =
            "https://pro-api.solscan.io/v2.0/account/portfolio";
    private static final String SOL_SCAN_ACTIVITIES_URL =
            "https://pro-api.solscan.io/v2.0/account/balance_change";
    private static final String SOL_SCAN_TOKEN_URL =
            "https://pro-api.solscan.io/v2.0/token/meta";
    @Value("${sol.scan.key}")
    private String solScanKey;
    private final ObjectMapper objectMapper;

    @Override
    public Set<SingleTokenPortfolioResponseDto> getTokensByAddress(String address) {
        HttpClient httpClient = HttpClient.newHttpClient();
        String url = SOL_SCAN_PORTFOLIO_URL + "?address=" + address;
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .header("accept", "application/json")
                .header("token", solScanKey)
                .build();
        try {
            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            TokenPortfoliosResponseDto tokens =
                    objectMapper.readValue(response.body(), TokenPortfoliosResponseDto.class);
            return tokens.getTokens();
        } catch (Exception e) {
            throw new RuntimeException("An error occurred. Please try again later.");
        }
    }

    @Override
    public Set<SingleBalanceActivityResponseDto> getNewBalanceActivities(String address) {
        HttpClient httpClient = HttpClient.newHttpClient();
        long fromTime = Instant.now().getEpochSecond() - 60;
        String url = SOL_SCAN_ACTIVITIES_URL + "?address=" + address
                + "&from_time=" + fromTime;
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .header("accept", "application/json")
                .header("token", solScanKey)
                .build();
        try {
            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            BalanceActivitiesResponseDto activities =
                    objectMapper.readValue(response.body(), BalanceActivitiesResponseDto.class);
            return activities.getActivities();
        } catch (Exception e) {
            throw new RuntimeException("An error occurred. Please try again later.");
        }
    }

    @Override
    public TokenMetaResponseDto getTokenMetaFromAddress(String tokenAddress) {
        String apiUrl = SOL_SCAN_TOKEN_URL + tokenAddress;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("accept", "application/json")
                .header("token", solScanKey)
                .build();
        try {
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), TokenMetaResponseDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get token name. Please try again later.");
        }
    }
}
