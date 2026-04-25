package com.grid.app.service;

import com.grid.app.dto.CreateBotRequest;
import com.grid.app.dto.BotResponse;
import com.grid.app.model.Bot;
import com.grid.app.repository.BotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotService {

    private final BotRepository botRepository;

    public BotResponse createBot(CreateBotRequest request) {
        Bot bot = Bot.builder()
                .name(request.getName())
                .personaDescription(request.getPersonaDescription())
                .build();
        Bot saved = botRepository.save(bot);
        return BotResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .personaDescription(saved.getPersonaDescription())
                .build();
    }
}
