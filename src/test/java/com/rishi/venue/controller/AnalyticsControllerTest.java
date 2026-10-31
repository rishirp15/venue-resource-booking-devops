package com.rishi.venue.controller;

import com.rishi.venue.dto.analytics.AdminAnalyticsData;
import com.rishi.venue.dto.analytics.UserAnalyticsData;
import com.rishi.venue.model.User;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.repository.VenueRepository;
import com.rishi.venue.service.AnalyticsService;
import com.rishi.venue.service.BookingService;
import com.rishi.venue.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:analyticsctrltest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private RecommendationService recommendationService;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private VenueRepository venueRepository;

    @Test
    void shouldDisplayUserAnalyticsForAuthenticatedUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 10L);
        session.setAttribute("userRole", "USER");

        UserAnalyticsData dummyUserAnalytics = new UserAnalyticsData(
                5, 3, 4, 1, 0, 0, 60.0, 1, List.of(), Map.of(), List.of()
        );
        when(analyticsService.getUserAnalytics(10L)).thenReturn(dummyUserAnalytics);

        mockMvc.perform(get("/my-analytics").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("user-analytics"))
                .andExpect(model().attributeExists("analytics"));
    }

    @Test
    void shouldRedirectUnauthenticatedUserFromAnalytics() throws Exception {
        mockMvc.perform(get("/my-analytics"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void shouldDisplayRecommendationsWithCriteria() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 10L);

        when(recommendationService.recommendVenues(eq(25), any(), any(), any(), eq("MORNING"), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/recommendations")
                        .session(session)
                        .param("attendees", "25")
                        .param("slot", "MORNING"))
                .andExpect(status().isOk())
                .andExpect(view().name("recommendations"))
                .andExpect(model().attributeExists("recommendations"))
                .andExpect(model().attribute("attendees", 25))
                .andExpect(model().attribute("slot", "MORNING"));
    }

    @Test
    void shouldDisplayAdminAnalyticsForAdminUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        User admin = new User("Admin", "admin@venue.com", "hash", "ADMIN");
        admin.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        AdminAnalyticsData dummyAdminData = new AdminAnalyticsData(
                10, 7, 2, 1, 0, 5, List.of(), List.of(), List.of(), Map.of(), List.of(), List.of(), List.of(), null, null
        );
        when(analyticsService.getAdminAnalytics(any(), any())).thenReturn(dummyAdminData);

        mockMvc.perform(get("/admin/analytics").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-analytics"))
                .andExpect(model().attributeExists("analytics"));
    }

    @Test
    void shouldDenyRegularUserFromAdminAnalytics() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 20L);

        User regularUser = new User("Regular", "reg@example.com", "hash", "USER");
        regularUser.setId(20L);
        when(userRepository.findById(20L)).thenReturn(Optional.of(regularUser));

        mockMvc.perform(get("/admin/analytics").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}

