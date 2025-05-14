package ivan.solscanbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ivan.solscanbot.dto.external.activity.BalanceActivitiesResponseDto;
import ivan.solscanbot.dto.external.activity.SingleBalanceActivityResponseDto;
import ivan.solscanbot.dto.external.meta.DataResponseDto;
import ivan.solscanbot.dto.external.meta.MultiDataResponseDto;
import ivan.solscanbot.dto.external.meta.TokenMetaResponseDto;
import ivan.solscanbot.dto.external.portfolio.PortfolioWrapperDto;
import ivan.solscanbot.dto.external.portfolio.SingleTokenPortfolioResponseDto;
import ivan.solscanbot.dto.external.transfer.Data;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private static final String SOL_SCAN_TOKEN_MULTI_URL =
            "https://pro-api.solscan.io/v2.0/token/meta/multi";
    private static final String SOL_SCAN_TOKEN_URL =
            "https://pro-api.solscan.io/v2.0/token/meta";
    private static final String SOL_SCAN_TOKEN_TRANSFERS =
            "https://pro-api.solscan.io/v2.0/token/transfer";

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
            PortfolioWrapperDto wrapper =
                    objectMapper.readValue(response.body(), PortfolioWrapperDto.class);
            return wrapper.getData().getTokens();
        } catch (Exception e) {
            throw new RuntimeException("An error occurred. Please try again later.");
        }
    }

    @Override
    public Set<SingleBalanceActivityResponseDto> getNewBalanceActivities(String address) {
        HttpClient httpClient = HttpClient.newHttpClient();
        long fromTime = Instant.now().getEpochSecond() - 300;
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
    public Map<String, TokenMetaResponseDto> getMetaMapFromAddresses(List<String> tokenAddresses) {
        String apiUrl = SOL_SCAN_TOKEN_MULTI_URL + "?address[]="
                + String.join("&address[]=", tokenAddresses);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("accept", "application/json")
                .header("token", solScanKey)
                .build();
        try {
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());
            MultiDataResponseDto data = objectMapper
                    .readValue(response.body(), MultiDataResponseDto.class);
            return data.getData().stream()
                    .collect(Collectors.toMap(
                            TokenMetaResponseDto::getAddress,
                            Function.identity(),
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to get multi token data. Please try again later.");
        }
    }

    @Override
    public TokenMetaResponseDto getTokenMeta(String address) {
        String apiUrl = SOL_SCAN_TOKEN_URL + "?address=" + address;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("accept", "application/json")
                .header("token", solScanKey)
                .build();
        try {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            DataResponseDto data =
                    objectMapper.readValue(response.body(), DataResponseDto.class);
            return data.getData();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get token data. Please try again later.");
        }
    }

    @Override
    public boolean newTokenTransfer(String tokenAddress) {
        String apiUrl = SOL_SCAN_TOKEN_TRANSFERS + "?address=" + tokenAddress;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("accept", "application/json")
                .header("token", solScanKey)
                .build();
        try {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            Data result = objectMapper.readValue(response.body(), Data.class);
            return result.getData().stream()
                    .anyMatch(transfer -> transfer.getTime()
                            .after(Date.from(Instant.now().minus(1, ChronoUnit.MINUTES))));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
