package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.AuthRequest;
import com.takima.backskeleton.DTO.TokenDto;
import com.takima.backskeleton.services.AuthService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Public: gives the token every /me and market write route asks for. Logging out = the client forgets the token.
@CrossOrigin
@RequestMapping("auth")
@RestController
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // New player with AuthService.STARTING_BALANCE memes. 409 if the name is taken, 400 if it or the password is invalid.
    @PostMapping("register")
    public TokenDto register(@RequestBody AuthRequest request) {
        return new TokenDto(authService.register(request.username(), request.password()));
    }

    // 401 if the name or the password is wrong.
    @PostMapping("login")
    public TokenDto login(@RequestBody AuthRequest request) {
        return new TokenDto(authService.login(request.username(), request.password()));
    }
}
