package com.markov.agent.domain.tool_callback;

import com.markov.agent.domain.open_ai.ModelRequest;
import com.markov.agent.domain.open_ai.ModelRequestMapper;
import com.markov.agent.domain.open_ai.ModelResponseParsingException;
import com.markov.agent.domain.source.CompanySearchSource;
import com.markov.agent.domain.source.MarkovChainSource;
import com.markov.agent.domain.value_object.MarkovChain;
import com.markov.agent.domain.value_object.MarkovChainPrediction;
import com.markov.agent.domain.value_object.DailyStock;
import com.markov.agent.domain.value_object.State;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Service;

@Service
public class CompanyStockStatusToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "get_company_stock_state";
    private static final String COMPANY_NOT_FOUND_RESPONSE = "Company not found";

    private static final String INPUT_SCHEMA = """
            {
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "Ticker or company name."
                    },
                    "days_forward": {
                        "type": "integer",
                        "description": "Number of days to predict forward. Optional, default is 1."
                    }
                },
                "required": ["query"]
            }
            """;

    private final MarkovChainSource markovChainSource;
    private final ModelRequestMapper modelRequestMapper;
    private final CompanySearchSource companySearchSource;

    public CompanyStockStatusToolCallback(MarkovChainSource markovChainSource,
                                          ModelRequestMapper modelRequestMapper,
                                          CompanySearchSource companySearchSource) {
        this.markovChainSource = markovChainSource;
        this.modelRequestMapper = modelRequestMapper;
        this.companySearchSource = companySearchSource;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return new DefaultToolDefinition(
            TOOL_NAME,
            "Gets company stock state." +
                " Input should be following fields: query with company name or ticker," +
                " and optional days_forward with number of days by which predict stock state." +
                " Output will be in format 'Company not found' if no company matches the input," +
                " 'Invalid input format' if failed to parse your input," +
                " or a description of the most probable market state for the company.",
            INPUT_SCHEMA
        );
    }

    @Override
    public String call(String toolInput) {
        ModelRequest modelRequest;
        try {
            modelRequest = modelRequestMapper.map(toolInput);
        } catch (ModelResponseParsingException e) {
            return "Invalid input format";
        }

        DailyStock latestStockReturn = companySearchSource.searchLatestDailyStockReturn(modelRequest.query());
        if (latestStockReturn == null) {
            return COMPANY_NOT_FOUND_RESPONSE;
        }

        MarkovChainPrediction prediction = markovChainSource.getMarkovChainPrediction(latestStockReturn);
        if (modelRequest.daysForward() > 1) {
            MarkovChain markovChain = markovChainSource.getMarkovChain();
            prediction = markovChain.predict(modelRequest.daysForward(), prediction);
        }

        State mostProbableState = prediction.getMostProbableState();

        return mostProbableState.getDescription();
    }
}
