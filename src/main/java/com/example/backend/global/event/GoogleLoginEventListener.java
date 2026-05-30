package com.example.backend.global.event;

import com.example.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleLoginEventListener {

    private final UserService userService;

    @EventListener
    public void handleGoogleLoginSuccess(GoogleRefreshTokenListener event) {

        userService.addGoogleRefreshToken(
                event.username(),
                event.googleRefreshToken()
        );
    }
}