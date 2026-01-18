package com.myapp.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.userservice.domain.User;
import com.myapp.userservice.domain.UserStatus;
import com.myapp.userservice.dto.request.CreateUserRequest;
import com.myapp.userservice.dto.request.UpdateUserRequest;
import com.myapp.userservice.repository.UserRepository;
import com.myapp.userservice.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private String authToken;
    private static String createdUserId;

    @BeforeEach
    void setUp() {
        // Generate test JWT token
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        authToken = "Bearer " + Jwts.builder()
                .claim("userId", "test-admin-id")
                .claim("phone", "+1111111111")
                .claim("role", "ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    @Test
    @Order(1)
    @DisplayName("POST /users - Should create user successfully")
    void shouldCreateUserSuccessfully() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setId("integration-test-user");
        request.setName("Integration Test User");
        request.setEmail("integration@test.com");
        request.setPhone("+1234567890");

        MvcResult result = mockMvc.perform(post("/users")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("User created successfully")))
                .andExpect(jsonPath("$.data.id", is("integration-test-user")))
                .andExpect(jsonPath("$.data.name", is("Integration Test User")))
                .andExpect(jsonPath("$.data.email", is("integration@test.com")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andReturn();

        createdUserId = "integration-test-user";
    }

    @Test
    @Order(2)
    @DisplayName("GET /users/:id - Should get user by ID")
    void shouldGetUserById() throws Exception {
        mockMvc.perform(get("/users/" + createdUserId)
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(createdUserId)))
                .andExpect(jsonPath("$.data.name", is("Integration Test User")));
    }

    @Test
    @Order(3)
    @DisplayName("GET /users - Should list users with pagination")
    void shouldListUsersWithPagination() throws Exception {
        mockMvc.perform(get("/users")
                        .header("Authorization", authToken)
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", isA(java.util.List.class)))
                .andExpect(jsonPath("$.pagination.page", is(1)))
                .andExpect(jsonPath("$.pagination.limit", is(10)));
    }

    @Test
    @Order(4)
    @DisplayName("PATCH /users/:id - Should update user")
    void shouldUpdateUser() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Updated Integration User");

        mockMvc.perform(patch("/users/" + createdUserId)
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("User updated successfully")))
                .andExpect(jsonPath("$.data.name", is("Updated Integration User")));
    }

    @Test
    @Order(5)
    @DisplayName("DELETE /users/:id - Should soft delete user")
    void shouldSoftDeleteUser() throws Exception {
        mockMvc.perform(delete("/users/" + createdUserId)
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("User deleted successfully")))
                .andExpect(jsonPath("$.data.status", is("DELETED")));
    }

    @Test
    @Order(6)
    @DisplayName("POST /users - Should return 409 for duplicate email")
    void shouldReturn409ForDuplicateEmail() throws Exception {
        // First create a user
        CreateUserRequest request1 = new CreateUserRequest();
        request1.setId("duplicate-email-test-1");
        request1.setName("User 1");
        request1.setEmail("duplicate@test.com");
        request1.setPhone("+2222222222");

        mockMvc.perform(post("/users")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Try to create another with same email
        CreateUserRequest request2 = new CreateUserRequest();
        request2.setId("duplicate-email-test-2");
        request2.setName("User 2");
        request2.setEmail("duplicate@test.com");
        request2.setPhone("+3333333333");

        mockMvc.perform(post("/users")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @Order(7)
    @DisplayName("GET /users/:id - Should return 404 for non-existent user")
    void shouldReturn404ForNonExistentUser() throws Exception {
        mockMvc.perform(get("/users/non-existent-id")
                        .header("Authorization", authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @Order(8)
    @DisplayName("Should return 401 without token")
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @AfterAll
    static void cleanup(@Autowired UserRepository userRepository) {
        userRepository.deleteAll();
    }
}
