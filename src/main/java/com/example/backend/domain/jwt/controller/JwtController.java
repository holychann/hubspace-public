package com.example.backend.domain.jwt.controller;

import com.example.backend.domain.jwt.dto.JWTResponseDTO;
import com.example.backend.domain.jwt.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/jwt")
@Slf4j
public class JwtController {

    private final JwtService jwtService;

    @PostMapping("/exchange")
    public JWTResponseDTO cookie2header(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        log.info(">>> [CTRL] cookie2header 진입");
        return jwtService.cookie2Header(request, response);
    }

    @PostMapping("/refresh")
    public JWTResponseDTO jwtRefreshCookie(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return jwtService.refreshToken(request, response);
    }

}
