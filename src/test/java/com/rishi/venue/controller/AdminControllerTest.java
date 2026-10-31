package com.rishi.venue.controller;

import com.rishi.venue.dto.AdminDashboardStats;
import com.rishi.venue.model.User;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admincontrollertest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void unauthenticatedUserShouldBeRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void regularUserShouldBeDeniedAndRedirectedToLogin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 20L);

        User normalUser = new User("Normal", "user@example.com", "hash", "USER");
        normalUser.setId(20L);
        when(userRepository.findById(20L)).thenReturn(Optional.of(normalUser));

        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void adminUserShouldAccessDashboardSuccessfully() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        User adminUser = new User("Admin", "admin@venue.com", "hash", "ADMIN");
        adminUser.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        AdminDashboardStats dummyStats = new AdminDashboardStats(
                10, 3, 25, 4, 18, 3, List.of(), List.of()
        );
        when(bookingService.getAdminDashboardStats()).thenReturn(dummyStats);

        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-dashboard"))
                .andExpect(model().attributeExists("stats"));
    }
}

