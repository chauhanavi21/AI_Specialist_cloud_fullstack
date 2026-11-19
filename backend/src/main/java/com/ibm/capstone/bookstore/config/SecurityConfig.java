package com.ibm.capstone.bookstore.config;

import com.ibm.capstone.bookstore.security.JwtAccessDeniedHandler;
import com.ibm.capstone.bookstore.security.JwtAuthenticationEntryPoint;
import com.ibm.capstone.bookstore.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** The H2 console is a development tool: it is reachable only while spring.h2.console.enabled is true. */
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    public SecurityConfig(JwtAuthenticationEntryPoint unauthorizedHandler, JwtAccessDeniedHandler accessDeniedHandler,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler).accessDeniedHandler(accessDeniedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> {
                    // The H2 console (dev profile only) is rendered in frames; everything else may not be framed.
                    if (h2ConsoleEnabled) {
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
                    } else {
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
                    }
                    headers.contentTypeOptions(Customizer.withDefaults());
                    headers.referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
                })
                // A strict policy for JSON responses; Swagger UI and the H2 console load their own assets, so they are exempt.
                .addFilterAfter(new ApiSecurityHeadersFilter(), org.springframework.security.web.header.HeaderWriterFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Guests may browse: sign-in, the public catalogue, writers, coupons and store policies
                        .requestMatchers("/auth/login", "/auth/register", "/auth/logout", "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories/**", "/api/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/publishers/**", "/api/v1/publishers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/books/**", "/api/v1/books/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/writers", "/writers/**", "/api/v1/writers", "/api/v1/writers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/coupons", "/api/v1/coupons").permitAll()
                        .requestMatchers(HttpMethod.GET, "/stores", "/stores/**", "/api/v1/stores", "/api/v1/stores/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/shipping/quote", "/api/v1/shipping/quote").permitAll()
                        // Role & entitlement: administration needs ROLE_ADMIN
                        .requestMatchers("/admin/**", "/api/v1/admin/**").hasAuthority("ROLE_ADMIN")
                        // API docs and health are public; the H2 console only exists on the dev profile
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/h2-console/**").access((authentication, context) ->
                                new org.springframework.security.authorization.AuthorizationDecision(h2ConsoleEnabled))
                        // Protected Endpoints
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Adds a locked-down Content-Security-Policy to API (JSON) responses only. */
    static class ApiSecurityHeadersFilter extends org.springframework.web.filter.OncePerRequestFilter {
        @Override
        protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                        jakarta.servlet.http.HttpServletResponse response,
                                        jakarta.servlet.FilterChain chain) throws java.io.IOException, jakarta.servlet.ServletException {
            String path = request.getRequestURI();
            boolean tooling = path.contains("/swagger-ui") || path.contains("/h2-console") || path.contains("/v3/api-docs");
            if (!tooling) {
                response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
            }
            chain.doFilter(request, response);
        }
    }
}
