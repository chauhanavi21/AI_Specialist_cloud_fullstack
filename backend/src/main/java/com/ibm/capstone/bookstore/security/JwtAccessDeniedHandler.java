package com.ibm.capstone.bookstore.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.capstone.bookstore.domain.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * A signed-in user who lacks the role gets a clear 403 written straight to the response.
 *
 * Without this, the container forwards to its error page, and that forward is itself a protected request:
 * the caller ends up being told "log in" (401) when the truth is "you are logged in, but not allowed" (403).
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);
    private final ObjectMapper objectMapper;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Access denied on {}: missing role", request.getRequestURI());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse(
                HttpStatus.FORBIDDEN.value(), "Forbidden",
                "You do not have permission to access this resource", request.getRequestURI())));
    }
}
