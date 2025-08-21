package com.vincent.learning.token.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A Wrapper to encode/decode the string with jdk build-in base64 encode.
 */
public class EncodeUtil {
    private EncodeUtil() {
        // empty constructor
    }

    /**
     * encode the string to base64.
     */
    public static String encodeBase64(String source) {
        return Base64.getEncoder().encodeToString(source.getBytes());
    }

    /**
     * Decode from base64 to plaintext.
     */
    public static String decodeBase64(String base64Encoder) {
        return new String(Base64.getDecoder().decode(base64Encoder), StandardCharsets.UTF_8);
    }
}
