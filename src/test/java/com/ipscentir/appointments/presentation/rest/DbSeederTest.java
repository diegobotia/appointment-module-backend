package com.ipscentir.appointments.presentation.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootTest
@ActiveProfiles("supabase")
@EnabledIfEnvironmentVariable(named = "SUPABASE_DB_URL", matches = ".+")
public class DbSeederTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void seedDatabase() throws Exception {
        System.out.println("==================================================");
        System.out.println("DATABASE SEEDER: ELEGANT AND SAFE DATA POPULATION");
        System.out.println("==================================================");
        
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("Connected to: " + conn.getMetaData().getURL());
            System.out.println("User: " + conn.getMetaData().getUserName());
            System.out.println("Executing scripts/seed.sql...");
            
            // ScriptUtils handles multiline, comments, and delimiters correctly!
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/seed_test_data.sql"));
            
            System.out.println("Seeding completed successfully!");
            System.out.println("==================================================");
        }
    }
}
