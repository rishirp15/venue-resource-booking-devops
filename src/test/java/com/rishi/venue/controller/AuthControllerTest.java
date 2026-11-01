package com.rishi.venue.controller;

import com.rishi.venue.model.User;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.service.AuditLogService;
import com.rishi.venue.service.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:authcontrollertest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordService passwordService;

    @MockBean
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        reset(userRepository, passwordService, auditLogService);
    }

    @Test
    @DisplayName("GET /register should display registration page")
    void showRegisterPageShouldReturnRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    @DisplayName("POST /register with valid details should create user and redirect to login")
    void registerWithValidDetailsShouldSucceed() throws Exception {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordService.hashPassword("secret123")).thenReturn("hashed-secret");

        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setName("John Doe");
        savedUser.setEmail("test@example.com");
        savedUser.setRole("USER");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        mockMvc.perform(post("/register")
                        .param("name", "John Doe")
                        .param("email", "test@example.com")
                        .param("password", "secret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(userRepository, times(1)).save(argThat(u ->
                u.getName().equals("John Doe") &&
                u.getEmail().equals("test@example.com") &&
                u.getPassword().equals("hashed-secret") &&
                u.getRole().equals("USER")
        ));
    }

    @Test
    @DisplayName("POST /register with blank fields should return error on register page")
    void registerWithBlankFieldsShouldFail() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "")
                        .param("email", "test@example.com")
                        .param("password", "secret123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "All fields are required."));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST /register with existing email should reject with error")
    void registerWithDuplicateEmailShouldFail() throws Exception {
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        mockMvc.perform(post("/register")
                        .param("name", "Jane Doe")
                        .param("email", "existing@example.com")
                        .param("password", "pass123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "An account with this email already exists."));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("GET /login should display login page")
    void showLoginPageShouldReturnLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("POST /login with unregistered email should display invalid credentials error")
    void loginWithNonExistentEmailShouldFail() throws Exception {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/login")
                        .param("email", "unknown@example.com")
                        .param("password", "anyPass"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Invalid email or password."));
    }

    @Test
    @DisplayName("POST /login with wrong password should display invalid credentials error")
    void loginWithWrongPasswordShouldFail() throws Exception {
        User user = new User();
        user.setId(5L);
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setPassword("correctHash");
        user.setRole("USER");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordService.matches("wrongPass", "correctHash")).thenReturn(false);

        mockMvc.perform(post("/login")
                        .param("email", "alice@example.com")
                        .param("password", "wrongPass"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Invalid email or password."));
    }

    @Test
    @DisplayName("POST /login with valid credentials should establish user session and redirect")
    void loginWithValidCredentialsShouldEstablishSession() throws Exception {
        User user = new User();
        user.setId(7L);
        user.setName("Bob");
        user.setEmail("bob@example.com");
        user.setPassword("validHash");
        user.setRole("USER");

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
        when(passwordService.matches("password123", "validHash")).thenReturn(true);

        mockMvc.perform(post("/login")
                        .param("email", "bob@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(request().sessionAttribute("userId", 7L))
                .andExpect(request().sessionAttribute("userName", "Bob"))
                .andExpect(request().sessionAttribute("userRole", "USER"));
    }

    @Test
    @DisplayName("POST /logout should invalidate session and redirect to login?logout")
    void logoutShouldInvalidateSessionAndRedirect() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 7L);
        session.setAttribute("userName", "Bob");
        session.setAttribute("userRole", "USER");

        mockMvc.perform(post("/logout")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        assertTrue(session.isInvalid());
    }
}
