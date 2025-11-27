package com.StocksMCP.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;


import java.time.Instant;


@Service
public class StockService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final WebClient webClient;
    private final String apiKey;
    private final String baseUrl;

    public StockService(
            @Value("${alpha-vantage.api-key}") String apiKey,
            @Value("${alpha-vantage.base-url}") String baseUrl
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB limit
                .build();

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(strategies)
                .build();
    }

    @Tool(name = "getStockPrice", description = "Get the current stock price for a given symbol")
    public JsonNode getStockPrice(String symbol) {
        try {
            // Call Alpha Vantage API to get stock price
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("function", "GLOBAL_QUOTE")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            JsonNode quote = response.get("Global Quote");
            if (quote == null || quote.isEmpty()) {
                return errorResponse("No data found for symbol: " + symbol);
            }

            double price = quote.get("05. price").asDouble();

            ObjectNode result = mapper.createObjectNode();
            result.put("symbol", symbol);
            result.put("price", price);
            result.put("currency", "USD");
            result.put("time", Instant.now().toString());
            return result;
        } catch (Exception e) {
            return errorResponse("Error fetching stock price: " + e.getMessage());
        }
    }

    @Tool(name = "getStockNews", description = "Get the latest news articles for a given stock symbol")
    public JsonNode getStockNews(String symbols, Integer limit) {
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("function", "NEWS_SENTIMENT")
                            .queryParam("tickers", symbols)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.has("feed")) {
                return errorResponse("No news found for symbol: " + symbols);
            }

            ArrayNode articles = mapper.createArrayNode();
            int count = 0;

            for (JsonNode article : response.get("feed")) {
                if (limit != null && count >= limit) break;

                ObjectNode simplified = mapper.createObjectNode();
                simplified.put("title", article.path("title").asText(""));
                simplified.put("url", article.path("url").asText(""));
                simplified.put("summary", article.path("summary").asText(""));
                simplified.put("time", article.path("time_published").asText(""));
                simplified.put("sentiment", article.path("overall_sentiment_label").asText(""));
                simplified.put("source", article.path("source").asText(""));

                ArrayNode tickers = mapper.createArrayNode();
                if (article.has("ticker_sentiment")) {
                    for (JsonNode ticker : article.get("ticker_sentiment")) {
                        tickers.add(ticker.get("ticker").asText());
                    }
                }
                simplified.set("tickers", tickers);

                articles.add(simplified);
                count++;
            }

            ObjectNode result = mapper.createObjectNode();
            result.put("success", true);
            result.put("symbol", symbols);
            result.put("count", articles.size());
            result.set("articles", articles);

            return result;
        } catch (Exception e) {
            return errorResponse("Error fetching news: " + e.getMessage());
        }
    }

    @Tool(name = "getCompanyOverview", description = "Get the company overview for a given stock symbol")
    public JsonNode getCompanyOverview(String symbol) {
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("function", "OVERVIEW")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || response.isEmpty()) {
                return errorResponse("Error fetching company overview: " + symbol);
            }

            ObjectNode result = mapper.createObjectNode();
            result.put("symbol", symbol);
            result.put("assetType", response.path("AssetType").asText(""));
            result.put("description", response.path("Description").asText(""));
            result.put("country", response.path("Country").asText(""));
            result.put("industry", response.path("Industry").asText(""));
            result.put("latestQuarter", response.path("LatestQuarter").asText(""));
            return result;
        }  catch (Exception e) {
            return errorResponse("Error fetching company overview: " + e.getMessage());
        }
    }

    @Tool(name = "getInsiderTransactions", description = "Get insider transactions for a given stock symbol")
    public JsonNode getInsiderTransactions(String symbol, Integer limit) {
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("function", "INSIDER_TRANSACTIONS")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || response.isEmpty()) {
                return errorResponse("Error fetching insider transactions: " + symbol);
            }

            ArrayNode transactions = mapper.createArrayNode();
            int count = 0;

            for (JsonNode transaction : response.get("data")) {
                if (limit != null && count >= limit) break;

                ObjectNode row = mapper.createObjectNode();

                row.put("transactionDate", transaction.path("transaction_date").asText(""));
                row.put("symbol", transaction.path("ticker").asText(""));
                row.put("executiveName", transaction.path("executive").asText(""));
                row.put("executiveTitle", transaction.path("executive_title").asText(""));
                row.put("securityType", transaction.path("security_type").asText(""));
                row.put("acquisitionOrDisposal", transaction.path("acquisition_or_disposal").asText(""));
                row.put("shares", transaction.path("shares").asDouble(0.0));
                row.put("sharePrice", transaction.path("share_price").asDouble(0.0));

                transactions.add(row);
                count++;
            }

            ObjectNode result = mapper.createObjectNode();
            result.put("success", true);
            result.put("symbol", symbol);
            result.put("count", transactions.size());
            result.set("transactions", transactions);

            return result;
        } catch (Exception e) {
            return errorResponse("Error fetching insider transactions: " + e.getMessage());
        }
    }

    @Tool(name = "getIncomeStatement", description = "Get income statement for a given stock symbol")
    public JsonNode getIncomeStatement(String symbol, Integer limit) {
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("function", "INCOME_STATEMENT")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || response.isEmpty()) {
                return errorResponse("Error fetching income statement: " + symbol);
            }

            ArrayNode statements = mapper.createArrayNode();
            int count = 0;

            for (JsonNode statement : response.get("annualReports")) {
                if (limit != null && count >= limit) break;

                ObjectNode row = mapper.createObjectNode();

                row.put("fiscalDateEnding", statement.path("fiscalDateEnding").asText(""));
                row.put("reportedCurrency", statement.path("reportedCurrency").asText(""));
                row.put("grossProfit", statement.path("grossProfit").asDouble(0.0));
                row.put("totalRevenue", statement.path("totalRevenue").asDouble(0.0));
                row.put("costOfRevenue", statement.path("costOfRevenue").asDouble(0.0));
                row.put("costofGoodsAndServicesSold", statement.path("costOfGoodsAndServicesSold").asDouble(0.0));
                row.put("operatingIncome", statement.path("operatingIncome").asDouble(0.0));
                row.put("sellingGeneralAndAdministrative", statement.path("sellingGeneralAndAdministrative").asDouble(0.0));
                row.put("researchAndDevelopment", statement.path("researchAndDevelopment").asDouble(0.0));
                row.put("operatingExpenses", statement.path("operatingExpenses").asDouble(0.0));
                row.put("investmentIncomeNet", statement.path("investmentIncomeNet").asDouble(0.0));
                row.put("interestIncome", statement.path("interestIncome").asDouble(0.0));
                row.put("interestExpense", statement.path("interestExpense").asDouble(0.0));
                row.put("nonInterestIncome", statement.path("nonInterestIncome").asDouble(0.0));
                row.put("otherNonOperatingIncome", statement.path("otherNonOperatingIncome").asDouble(0.0));
                row.put("depriciation", statement.path("depreciation").asDouble(0.0));
                row.put("depriciationAndAmortization", statement.path("depreciationAndAmortization").asDouble(0.0));
                row.put("incomeBeforeTax", statement.path("incomeBeforeTax").asDouble(0.0));
                row.put("incomeTaxExpense", statement.path("incomeTaxExpense").asDouble(0.0));
                row.put("interestAndDebtExpense", statement.path("interestAndDebtExpense").asDouble(0.0));
                row.put("netIncomeFromContinuingOperations", statement.path("netIncomeFromContinuingOperations").asDouble(0.0));
                row.put("comprehensiveIncomeNetOfTax", statement.path("comprehensiveIncomeNetOfTax").asDouble(0.0));
                row.put("ebit", statement.path("ebit").asDouble(0.0));
                row.put("ebitda", statement.path("ebitda").asDouble(0.0));
                row.put("netIncome", statement.path("netIncome").asDouble(0.0));

                statements.add(row);
                count++;
            }

            ObjectNode result = mapper.createObjectNode();
            result.put("success", true);
            result.put("symbol", symbol);
            result.put("count", statements.size());
            result.set("incomeStatements", statements);

            return result;
        } catch (Exception e) {
            return errorResponse("Error fetching income statement: " + e.getMessage());
        }
    }

    @Tool(name = "getEarningsEstimates", description = "Get earnings estimates for a given stock symbol")
    public JsonNode getEarningsEstimates(String symbol, Integer limit) {
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("function", "EARNINGS_ESTIMATES")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || response.isEmpty()) {
                return errorResponse("Error fetching earnings estimates: " + symbol);
            }

            ArrayNode estimates = mapper.createArrayNode();
            int count = 0;

            for (JsonNode estimate : response.get("estimates")) {
                if (limit != null && count >= limit) break;

                ObjectNode row = mapper.createObjectNode();

                row.put("Date", estimate.path("date").asText(""));
                row.put("estimateAverageEPS", estimate.path("eps_estimate_average").asDouble(0.0));
                row.put("estimateHighEPS", estimate.path("eps_estimate_high").asDouble(0.0));
                row.put("estimateLowEPS", estimate.path("eps_estimate_low").asDouble(0.0));
                row.put("numberOfAnalysts", estimate.path("eps_estimate_analyst_count").asInt(0));
                row.put("estimateAverageRevenue", estimate.path("revenue_estimate_average").asDouble(0.0));
                row.put("numberOfAnalystsRevenue", estimate.path("revenue_estimate_analyst_count").asDouble(0.0));

                estimates.add(row);
                count++;
            }

            ObjectNode result = mapper.createObjectNode();
            result.put("success", true);
            result.put("symbol", symbol);
            result.put("count", estimates.size());
            result.set("earningsEstimates", estimates);

            return result;
        } catch (Exception e) {
            return errorResponse("Error fetching earnings estimates: " + e.getMessage());
        }
    }

    private ObjectNode errorResponse(String message) {
        ObjectNode error = mapper.createObjectNode();
        error.put("error", message);
        return error;
    }
}
