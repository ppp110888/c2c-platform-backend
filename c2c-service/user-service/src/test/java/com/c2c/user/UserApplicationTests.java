package com.c2c.user;

import cn.hutool.crypto.digest.BCrypt;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserApplicationTests {

    @Test
    void hashesAndVerifiesPassword() {
        String hash = BCrypt.hashpw("test-password", BCrypt.gensalt());
        assertTrue(BCrypt.checkpw("test-password", hash));
    }
}
