package com.ipscentir.appointments.presentation.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

@SpringBootTest
@ActiveProfiles("supabase")
public class DbInspectorTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void inspectDatabase() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("==================================================");
            System.out.println("FOREIGN KEYS OF core.profiles");
            System.out.println("==================================================");
            try (ResultSet rs = conn.getMetaData().getImportedKeys(null, "core", "profiles")) {
                while (rs.next()) {
                    System.out.println("FK Name: " + rs.getString("FK_NAME")
                            + " | PK Table Schema: " + rs.getString("PKTABLE_SCHEM")
                            + " | PK Table Name: " + rs.getString("PKTABLE_NAME")
                            + " | PK Column Name: " + rs.getString("PKCOLUMN_NAME")
                            + " | FK Column Name: " + rs.getString("FKCOLUMN_NAME"));
                }
            }
            System.out.println("==================================================");
        }
    }
}
