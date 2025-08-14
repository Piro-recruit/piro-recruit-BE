package com.pirogramming.recruit.global.config;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WebhookTokenFilter extends OncePerRequestFilter {
    @Value("${webhook.token:}")
    private String webhookToken;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/webhook/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (webhookToken == null || webhookToken.isBlank()) {
            // 토큰 미설정 환경(dev)에서는 통과
            filterChain.doFilter(request, response);
            return;
        }
        String token = request.getHeader("X-Webhook-Token");
        if (token == null || !token.equals(webhookToken)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Unauthorized webhook\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
