package com.ibm.capstone.bookstore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The security claims in the submission report, each pinned down by a test: forged and tampered tokens are refused,
 * roles are enforced, error bodies do not leak, secrets are not echoed, and the security headers are present.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class SecurityIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void useClientThatReadsErrorBodies() {
        rest.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api/v1" + path;
    }

    private ResponseEntity<String> get(String path, String token) {
        HttpHeaders h = new HttpHeaders();
        if (token != null) h.setBearerAuth(token);
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(h), String.class);
    }

    private String login(String email) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        String body = mapper.writeValueAsString(Map.of("email", email, "password", "Password123!"));
        ResponseEntity<String> r = rest.exchange(url("/auth/login"), HttpMethod.POST, new HttpEntity<>(body, h), String.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        return mapper.readTree(r.getBody()).get("token").asText();
    }

    private static String b64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    /** Changes one character in the middle of the signature (the last character is skipped: its low bits are padding). */
    private static String tamperSignature(String token) {
        String[] p = token.split("\\.");
        char c = p[2].charAt(10);
        return p[0] + "." + p[1] + "." + p[2].substring(0, 10) + (c == 'A' ? 'B' : 'A') + p[2].substring(11);
    }

    @Test
    @DisplayName("Forged, tampered and unsigned tokens are all refused, on a member route and on an admin route")
    void forgedTokensAreRefused() throws Exception {
        String valid = login("alex.dev@ibm.com");
        String[] parts = valid.split("\\.");
        String adminClaims = b64("{\"sub\":\"admin@bookworm.example\",\"exp\":9999999999}");

        String[] bad = {
                tamperSignature(valid),                                            // signature changed
                parts[0] + "." + adminClaims + "." + parts[2],                     // payload swapped to the admin, old signature
                parts[0] + "." + parts[1] + ".",                                   // signature removed
                b64("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "." + adminClaims + ".", // the classic alg=none forgery
                "abc",                                                             // not a token at all
        };
        for (String token : bad) {
            assertEquals(HttpStatus.UNAUTHORIZED, get("/cart", token).getStatusCode(), "member route accepted: " + token);
            assertEquals(HttpStatus.UNAUTHORIZED, get("/admin/overview", token).getStatusCode(), "admin route accepted: " + token);
        }
        assertEquals(HttpStatus.OK, get("/cart", valid).getStatusCode(), "the untouched token still works");
    }

    @Test
    @DisplayName("A customer's valid token is not enough for admin routes: 403, with the path the caller asked for")
    void customerIsForbiddenFromAdmin() throws Exception {
        ResponseEntity<String> r = get("/admin/overview", login("alex.dev@ibm.com"));
        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        assertEquals("/api/v1/admin/overview", mapper.readTree(r.getBody()).get("path").asText());
    }

    @Test
    @DisplayName("A 401 names the path that was requested, not the container's /error page (including for a tampered token)")
    void unauthorizedBodyReportsTheRealPath() throws Exception {
        // No token and a garbage token are rejected at the first gate; a well-formed token with a wrong signature
        // travels further and used to be reported as /error, so it is the case that matters here.
        String tampered = tamperSignature(login("alex.dev@ibm.com"));
        for (String token : new String[]{null, "abc", tampered}) {
            ResponseEntity<String> r = get("/cart", token);
            assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
            assertEquals("/api/v1/cart", mapper.readTree(r.getBody()).get("path").asText(), "token: " + token);
        }
    }

    @Test
    @DisplayName("Operational endpoints: health is public; everything else under actuator is admin-only")
    void actuatorIsLockedDown() throws Exception {
        assertEquals(HttpStatus.OK, get("/actuator/health", null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, get("/actuator/env", null).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, get("/actuator/env", login("alex.dev@ibm.com")).getStatusCode());
    }

    @Test
    @DisplayName("API responses are not framable or sniffable and carry a locked-down content policy; nothing secret is echoed")
    void headersAndNoSecrets() throws Exception {
        ResponseEntity<String> books = get("/books", null);
        HttpHeaders h = books.getHeaders();
        assertEquals("nosniff", h.getFirst("X-Content-Type-Options"));
        assertNotNull(h.getFirst("X-Frame-Options"));
        assertEquals("no-referrer", h.getFirst("Referrer-Policy"));
        assertTrue(h.getFirst("Content-Security-Policy").contains("default-src 'none'"));

        String me = get("/auth/me", login("alex.dev@ibm.com")).getBody();
        assertFalse(me.toLowerCase().contains("password"), "the profile must not include the password hash");
        JsonNode user = mapper.readTree(me);
        assertFalse(user.has("passwordHash"));
    }

    @Test
    @DisplayName("Hostile search and sort input is treated as data: no error, no extra rows, no damage")
    void injectionShapedInputIsInert() throws Exception {
        ResponseEntity<String> search = get("/books?query=" + "%27%20OR%201%3D1--", null);
        assertEquals(HttpStatus.OK, search.getStatusCode());
        assertEquals(0, mapper.readTree(search.getBody()).get("totalElements").asInt(), "an injected OR 1=1 must not match every book");

        ResponseEntity<String> sort = get("/books?sortBy=x%3BDROP%20TABLE%20books", null);
        assertEquals(HttpStatus.OK, sort.getStatusCode());
        assertEquals(15, mapper.readTree(get("/books?size=50", null).getBody()).get("totalElements").asInt(), "the catalogue is intact");
    }

    @Test
    @DisplayName("Unexpected server errors do not leak stack traces or class names")
    void errorsDoNotLeakInternals() throws Exception {
        ResponseEntity<String> r = get("/books/not-a-number", null);
        assertTrue(r.getStatusCode().is4xxClientError());
        String body = r.getBody();
        assertFalse(body.contains("Exception"), body);
        assertFalse(body.contains("at com.ibm"), body);
    }
}
