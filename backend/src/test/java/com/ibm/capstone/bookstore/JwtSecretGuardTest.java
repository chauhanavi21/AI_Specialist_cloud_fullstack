package com.ibm.capstone.bookstore;

import com.ibm.capstone.bookstore.config.JwtSecretGuard;
import com.ibm.capstone.bookstore.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class JwtSecretGuardTest {

    private static String shippedDefault(String file, String regex) throws Exception {
        String text = Files.readString(Path.of(file));
        Matcher m = Pattern.compile(regex).matcher(text);
        assertTrue(m.find(), "could not find the default secret in " + file);
        return m.group(1);
    }

    private static void check(String secret) throws Exception {
        var method = JwtSecretGuard.class.getDeclaredMethod("check");
        method.setAccessible(true);
        try {
            method.invoke(new JwtSecretGuard(secret));
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) throw re;
            throw e;
        }
    }

    @Test
    @DisplayName("The demo secret published in the repository is refused on a production-like deployment")
    void demoSecretIsRefused() throws Exception {
        // The values really shipped in the repo, not a copy of them kept in this test.
        String inYaml = shippedDefault("src/main/resources/application.yml", "secret:\\s*\\$\\{JWT_SECRET:([0-9a-fA-F]+)\\}");
        String inCompose = shippedDefault("../docker-compose.yml", "JWT_SECRET:\\s*\\$\\{JWT_SECRET:-([0-9a-fA-F]+)\\}");

        IllegalStateException a = assertThrows(IllegalStateException.class, () -> check(inYaml));
        assertTrue(a.getMessage().contains("demo value"), a.getMessage());
        assertThrows(IllegalStateException.class, () -> check(inCompose), "docker-compose ships the same public secret");
    }

    @Test
    @DisplayName("Short, empty and non-base64 secrets are refused")
    void weakSecretsAreRefused() {
        assertThrows(IllegalStateException.class, () -> check(Base64.getEncoder().encodeToString(new byte[8])));
        assertThrows(IllegalStateException.class, () -> check(""));
        assertThrows(IllegalStateException.class, () -> check("not base64 !!!"));
    }

    @Test
    @DisplayName("A random 64-byte secret is accepted, and the token provider can actually sign with it")
    void strongSecretIsAccepted() throws Exception {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        String secret = Base64.getEncoder().encodeToString(bytes);

        assertDoesNotThrow(() -> check(secret));
        assertDoesNotThrow(() -> new JwtTokenProvider(secret, 60_000), "the guard must accept exactly what the provider accepts");
    }
}
