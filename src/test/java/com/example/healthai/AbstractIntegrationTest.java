package com.example.healthai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.example.healthai.auth.domain.User;
import com.example.healthai.auth.domain.UserType;
import com.example.healthai.auth.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;

/**
 * Abstract integration test class.
 *
 * @author yueqian
 * @since 2022-10-25
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserMapper userMapper;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private Flyway flyway;

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379)
        .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1))
        .withStartupTimeout(Duration.ofSeconds(60));

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
    }

    private static final AtomicBoolean schemaInitialized = new AtomicBoolean(false);

    @BeforeEach
    void cleanDatabase() {
        ensureSchema();
        resetTable("health_profiles", "id");
        resetTable("users", "id");
    }

    private void ensureSchema() {
        if (flyway != null && schemaInitialized.compareAndSet(false, true)) {
            flyway.migrate();
        }
    }

    private void resetTable(String tableName, String idColumn) {
        if (tableExists(tableName)) {
            jdbcTemplate.update("DELETE FROM " + tableName);
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ALTER COLUMN " + idColumn + " RESTART WITH 1");
        }
    }

    private boolean tableExists(String tableName) {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
            "WHERE TABLE_SCHEMA = CURRENT_SCHEMA() AND LOWER(TABLE_NAME) = LOWER(?)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
        return count != null && count > 0;
    }

    protected User createUser(String username) {
        return createUser(username, "Password123");
    }

    protected User createUser(String username, String rawPassword) {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
            .username(username)
            .passwordHash(passwordEncoder.encode(rawPassword))
            .fullName("Test User")
            .gender("unknown")
            .userType(UserType.PATIENT)
            .phone("13800000000")
            .email(username + "@example.com")
            .registeredAt(now)
            .createdAt(now)
            .updatedAt(now)
            .build();
        userMapper.insert(user);
        return user;
    }

    protected JsonNode loginAndGetAuthData(String username, String password) throws Exception {
        String payload = "{" +
            "\"username\":\"" + username + "\"," +
            "\"password\":\"" + password + "\"" +
            "}";

        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).path("data");
    }

    protected String loginAndGetToken(String username, String password) throws Exception {
        return loginAndGetAuthData(username, password).path("accessToken").asText();
    }
}
