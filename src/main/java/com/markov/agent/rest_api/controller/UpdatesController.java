package com.markov.agent.rest_api.controller;

import com.markov.agent.rest_api.dto.TelegramResponse;
import com.markov.agent.rest_api.dto.Update;
import com.markov.agent.domain.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
public class UpdatesController {

    private final MessageService messageService;

    public UpdatesController(MessageService messageService) {
        this.messageService = messageService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        produces = { "application/json" },
        value = { "/test" }
    )
    public ResponseEntity<String> test() {
        return ok("Server is up and running!");
    }

    @RequestMapping(
        method = RequestMethod.POST,
        produces = { "application/json" },
        consumes = { "application/json" },
        value = { "/updates" }
    )
    public ResponseEntity<TelegramResponse> getUpdates(@RequestBody Update update) {
        if (update.message() == null || update.message().from() == null) {
            return ok().build();
        }

        String responseMessage = messageService.processMessage(update);

        var telegramResponse = createTelegramResponse(update, responseMessage);
        return ok(telegramResponse);
    }

    private TelegramResponse createTelegramResponse(Update update, String responseMessage) {
        long chatId = update.message().from().id();
        return new TelegramResponse("sendMessage", chatId, responseMessage);
    }
}