package com.grid.app.controller;

import com.grid.app.dto.CreateUserRequest;
import com.grid.app.dto.UserResponse;
import com.grid.app.service.AppUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService appUserService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        UserResponse response = appUserService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
