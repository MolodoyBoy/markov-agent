package com.markov.agent.domain.open_ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ModelRequestMapper {

    private final ObjectMapper objectMapper;

    public ModelRequestMapper() {
        this.objectMapper = new ObjectMapper();
    }

    public ModelRequest map(String response) throws ModelResponseParsingException {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            String query = jsonNode.has("query") ? jsonNode.get("query").asText() : null;
            Integer daysForward = jsonNode.has("days_forward") ? jsonNode.get("days_forward").asInt() : null;

            return new ModelRequest(query, daysForward);
        } catch (ModelResponseParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new ModelResponseParsingException("Failed to parse response", e);
        }
    }
}
