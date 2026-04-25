package com.grid.app.controller;

import com.grid.app.dto.BotResponse;
import com.grid.app.dto.CreateBotRequest;
import com.grid.app.service.BotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bots")
@RequiredArgsConstructor
public class BotController {

    private final BotService botService;

    @PostMapping
    public ResponseEntity<BotResponse> createBot(@RequestBody CreateBotRequest request) {
        BotResponse response = botService.createBot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
