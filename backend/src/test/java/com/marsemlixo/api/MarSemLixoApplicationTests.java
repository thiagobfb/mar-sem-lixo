package com.marsemlixo.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.marsemlixo.api.TestJwksConfig;

@SpringBootTest(properties = {
        "app.google.client-id=test-client-id",
        "app.jwt.secret=test-secret-key-must-be-at-least-32-bytes!"
})
@Import({TestcontainersConfig.class, TestJwksConfig.class})
class MarSemLixoApplicationTests {

    @Test
    void contextLoads() {
    }
}
