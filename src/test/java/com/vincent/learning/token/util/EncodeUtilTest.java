package com.vincent.learning.token.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EncodeUtilTest {

    @Test
    void encodeBase64() {
        // verify the compliance with spring base64Util.
        Assertions.assertEquals("", EncodeUtil.encodeBase64(""));
        Assertions.assertEquals("YWJjZGU=", EncodeUtil.encodeBase64("abcde"));
    }

    @Test
    void decodeBase64() {
        // verify the compliance with spring base64Util.
        Assertions.assertEquals("", EncodeUtil.decodeBase64(""));
        Assertions.assertEquals("abcde", EncodeUtil.decodeBase64("YWJjZGU="));
    }
}
