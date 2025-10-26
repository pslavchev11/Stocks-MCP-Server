package com.StocksMCP.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
public class McpServerRunner implements CommandLineRunner {

    private final StockService stockService;
    private final ObjectMapper mapper = new ObjectMapper();

    public McpServerRunner(StockService stockService) {
        this.stockService = stockService;
    }

    @Override
    public void run(String... args) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;

        System.err.println("MCP Server started. Waiting for JSON-RPC requests...");

        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            System.err.println("Received: " + line);

            ObjectNode response = mapper.createObjectNode();
            response.put("jsonrpc", "2.0");

            try {
                JsonNode request = mapper.readTree(line);
                String method = request.has("method") ? request.get("method").asText() : null;
                String id = request.has("id") ? request.get("id").asText() : null;
                response.put("id", id);

                if (method == null) {
                    response.set("error", mapper.createObjectNode().put("message", "Missing method"));
                } else if ("getStockPrice".equals(method)) {
                    if (request.has("params") && request.get("params").has("symbol")) {
                        String symbol = request.get("params").get("symbol").asText();
                        JsonNode result = stockService.getStockPrice(symbol);

                        if (result.has("error")) {
                            response.set("error", result.get("error"));
                        } else {
                            response.set("result", result);
                        }
                    } else {
                        response.set("error", mapper.createObjectNode().put("message", "Missing symbol parameter"));
                    }
                } else if ("getStockNews".equals(method)) {
                    if (request.has("params") && request.get("params").has("symbol")) {
                        String symbol = request.get("params").get("symbol").asText();
                        Integer limit = request.get("params").has("limit") ? request.get("params").get("limit").asInt() : null;
                        JsonNode news = stockService.getStockNews(symbol, limit);
                        response.set("result", news);

                        if (news.has("error")) {
                            response.set("error", news);
                        } else {
                            response.set("result", news);
                        }
                    } else {
                        response.set("error", mapper.createObjectNode().put("message", "Missing symbol parameter"));
                    }
                } else if ("getCompanyOverview".equals(method)) {
                    if (request.has("params") && request.get("params").has("symbol")) {
                        String symbol = request.get("params").get("symbol").asText();
                        JsonNode overview = stockService.getCompanyOverview(symbol);

                        if (overview.has("error")) {
                            response.set("error", overview.get("error"));
                        } else {
                            response.set("result", overview);
                        }
                    } else {
                        response.set("error", mapper.createObjectNode().put("message", "Missing symbol parameter"));
                    }
                } else if ("getInsiderTransactions".equals(method)) {
                    if (request.has("params") && request.get("params").has("symbol")) {
                        String symbol = request.get("params").get("symbol").asText();
                        Integer limit = request.get("params").has("limit") ? request.get("params").get("limit").asInt() : null;
                        JsonNode transactions = stockService.getInsiderTransactions(symbol, limit);

                        if (transactions.has("error")) {
                            response.set("error", transactions.get("error"));
                        } else {
                            response.set("result", transactions);
                        }
                    } else {
                        response.set("error", mapper.createObjectNode().put("message", "Missing symbol parameter"));
                    }
                } else if ("getIncomeStatement".equals(method)) {
                    if (request.has("params") && request.get("params").has("symbol")) {
                        String symbol = request.get("params").get("symbol").asText();
                        Integer limit = request.get("params").has("limit") ? request.get("params").get("limit").asInt() : null;
                        JsonNode income = stockService.getIncomeStatement(symbol, limit);

                        if (income.has("error")) {
                            response.set("error", income.get("error"));
                        } else {
                            response.set("result", income);
                        }
                    } else {
                        response.set("error", mapper.createObjectNode().put("message", "Missing symbol parameter"));
                    }
                } else {
                    response.set("error", mapper.createObjectNode().put("message", "Unknown method: " + method));
                }

            } catch (Exception e) {
                response.set("error", mapper.createObjectNode().put("message", "Invalid JSON: " + e.getMessage()));
            }

            String output = mapper.writeValueAsString(response);
            System.out.println(output);
            System.out.flush();

            System.err.println("Responded: " + output);
        }

        System.err.println("MCP Server stopped (stdin closed).");
    }
}
