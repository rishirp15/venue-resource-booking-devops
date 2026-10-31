package com.rishi.venue.controller;

import com.rishi.venue.dto.UserDashboardStats;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:homecontrollertest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @Test
    void guestVisitingHomeShouldRenderLandingPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeDoesNotExist("dashboard"));
    }

    @Test
    void loggedInUserVisitingHomeShouldRenderUserDashboard() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 42L);
        session.setAttribute("userName", "Alice");
        session.setAttribute("userRole", "USER");

        UserDashboardStats dummyStats = new UserDashboardStats(
                5, 2, 2, 1, 3, List.of(), List.of()
        );
        when(bookingService.getUserDashboardStats(42L)).thenReturn(dummyStats);

        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("dashboard"));
    }

    @Test
    void loggedInAdminVisitingHomeShouldRedirectToAdminDashboard() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        session.setAttribute("userName", "Admin");
        session.setAttribute("userRole", "ADMIN");

        mockMvc.perform(get("/").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }
}
