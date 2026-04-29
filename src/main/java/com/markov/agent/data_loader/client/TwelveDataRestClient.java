package com.markov.agent.data_loader.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markov.agent.data_loader.source.ImportDailyStockSource;
import com.markov.agent.data_loader.value_object.Company;
import com.markov.agent.domain.value_object.DailyStock;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.joining;
import static java.util.function.Function.identity;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@CircuitBreaker(name = "TwelveDataRestClient")
public class TwelveDataRestClient implements ImportDailyStockSource {

    private static final Logger logger = getLogger(TwelveDataRestClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public TwelveDataRestClient(@Value("${external_api.twelve_data.url}") String apiUrl,
                                @Value("${external_api.twelve_data.api_key}") String apiKey,
                                @Value("${external_api.twelve_data.interval}") String interval,
                                @Value("${external_api.twelve_data.format}") String format) {
        this.objectMapper = new ObjectMapper();
        this.restClient = RestClient.builder()
            .baseUrl(apiUrl)
            .defaultUriVariables(
                Map.of(
                    "apikey", apiKey,
                    "interval", interval,
                    "format", format
                )
            )
            .build();
    }

    @Override
    public Map<Integer, List<DailyStock>> getDailyStock(List<Company> companies, LocalDate startDate) throws ExternalClientException {
        if (companies == null || companies.isEmpty()) {
            return Map.of();
        }

        String symbols = mapSymbols(companies);

        try {
            String responseBody = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .queryParam("symbol", symbols)
                    .queryParam("apikey", "{apikey}")
                    .queryParam("interval", "{interval}")
                    .queryParam("format", "{format}")
                    .queryParam("start_date", startDate)
                    .build()
                )
                .retrieve()
                .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return Map.of();
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);

            checkForApiError(rootNode);

            Map<String, TwelveDataTimeSeriesResponse> response =
                objectMapper.readValue(responseBody, new TypeReference<>() {});

            if (response.isEmpty()) {
                return Map.of();
            }

           return map(companies, response);
        } catch (ExternalClientException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error while fetching stock data from twelve data. {}", e.getMessage());
            throw new ExternalClientException("Failed to fetch stock data from twelve data.", e);
        }
    }

    private void checkForApiError(JsonNode rootNode) throws ExternalClientException {
        if (rootNode.has("code")) {
            int code = rootNode.get("code").asInt();
            String message = rootNode.has("message") ? rootNode.get("message").asText() : "Unknown error";
            throw new ExternalClientException("Twelve Data API error (code: " + code + "): " + message, null);
        }
    }

    private Map<Integer, List<DailyStock>> map(List<Company> companies, Map<String, TwelveDataTimeSeriesResponse> response) {
        if (response == null || response.isEmpty()) {
            return Map.of();
        }

        Map<String, Company> tickerToCompany = companies.stream()
            .collect(toMap(Company::ticker, identity()));

        Map<Integer, List<DailyStock>> result = new HashMap<>();
        response.forEach((ticker, timeSeries) -> {
            Company company = tickerToCompany.get(ticker);
            if (company != null && timeSeries.values() != null) {

                List<DailyStock> dailyStocks = timeSeries.values()
                    .stream()
                    .map(v -> map(company.id(), v))
                    .toList();

                result.put(company.id(), dailyStocks);
            }
        });

        return result;
    }

    private String mapSymbols(List<Company> companies) {
        return companies.stream()
            .map(Company::ticker)
            .collect(joining(","));
    }

    private DailyStock map(int companyId, TwelveDataTimeSeriesResponse.TimeSeriesValue value) {
        BigDecimal stockReturn = new BigDecimal(value.close());
        LocalDate stockDate = LocalDate.parse(value.datetime());

        return new DailyStock(companyId, stockReturn, stockDate);
    }
}