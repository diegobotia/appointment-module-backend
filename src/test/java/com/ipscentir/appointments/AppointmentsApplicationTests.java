package com.ipscentir.appointments;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AppointmentsApplicationTests {

    @Test
    void contextLoads() {
        // Validation that the application context and Flyway migrations start properly
    }
}
