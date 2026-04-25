package com.grid.app.service;

import com.grid.app.dto.CreateUserRequest;
import com.grid.app.dto.UserResponse;
import com.grid.app.model.AppUser;
import com.grid.app.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;

    public UserResponse createUser(CreateUserRequest request) {
        AppUser user = AppUser.builder()
                .username(request.getUsername())
                .isPremium(request.isPremium())
                .build();
        AppUser saved = appUserRepository.save(user);
        return UserResponse.builder()
                .id(saved.getId())
                .username(saved.getUsername())
                .isPremium(saved.isPremium())
                .build();
    }
}
