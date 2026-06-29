package com.example.security.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal HMAC-SHA256 JWT utility — no third-party library needed.
 * Format: Base64Url(header).Base64Url(payload).Base64Url(signature)
 *
 * NOTE: This is intentionally simple for demo purposes.
 *       Use a proper library (jjwt, nimbus-jose-jwt) in production.
 */
public class JwtUtil {

    private static final String SECRET = "super-secret-key-for-demo-only!";
    private static final long   EXPIRY_MS = 3_600_000; // 1 hour

    // ------------------------------------------------------------------ generate

    public static String generateToken(String username, java.util.List<String> roles) {
        String header  = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        long   now     = System.currentTimeMillis();
        String payload = base64Url(
            "{\"sub\":\"" + username + "\"," +
            "\"roles\":\"" + String.join(",", roles) + "\"," +
            "\"iat\":" + now + "," +
            "\"exp\":" + (now + EXPIRY_MS) + "}"
        );
        String sigInput = header + "." + payload;
        String sig = base64Url(hmacSha256(sigInput));
        return sigInput + "." + sig;
    }

    // ------------------------------------------------------------------ validate / parse

    /**
     * Returns a map with "sub" and "roles" if the token is valid, else throws.
     */
    public static Map<String, String> validateAndParse(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new IllegalArgumentException("Invalid JWT structure");

        // Verify signature
        String expectedSig = base64Url(hmacSha256(parts[0] + "." + parts[1]));
        if (!expectedSig.equals(parts[2])) {
            throw new SecurityException("JWT signature verification failed");
        }

        // Decode payload
        String payloadJson = new String(
            Base64.getUrlDecoder().decode(pad(parts[1])),
            StandardCharsets.UTF_8
        );

        // Tiny JSON parser for our known structure
        Map<String, String> claims = new HashMap<>();
        for (String entry : payloadJson.replaceAll("[{}]", "").split(",")) {
            String[] kv = entry.split(":", 2);
            if (kv.length == 2) {
                String k = kv[0].trim().replaceAll("\"", "");
                String v = kv[1].trim().replaceAll("\"", "");
                claims.put(k, v);
            }
        }

        // Check expiry
        long exp = Long.parseLong(claims.getOrDefault("exp", "0"));
        if (System.currentTimeMillis() > exp) {
            throw new SecurityException("JWT token has expired");
        }

        return claims;
    }

    // ------------------------------------------------------------------ helpers

    private static String base64Url(String input) {
        return Base64.getUrlEncoder().withoutPadding()
                     .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    private static String base64Url(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }

    private static byte[] hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 failed", e);
        }
    }

    private static String pad(String base64url) {
        int pad = base64url.length() % 4;
        if (pad == 2) return base64url + "==";
        if (pad == 3) return base64url + "=";
        return base64url;
    }
}
