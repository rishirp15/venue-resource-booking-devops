package com.rishi.venue.controller;

import com.rishi.venue.model.AuditLog;
import com.rishi.venue.model.User;
import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.repository.VenueRepository;
import com.rishi.venue.service.AnalyticsService;
import com.rishi.venue.service.AuditLogService;
import com.rishi.venue.service.BookingService;
import com.rishi.venue.service.VenueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private VenueRepository venueRepository;

    @MockitoBean
    private AnalyticsService analyticsService;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private VenueService venueService;

    @Test
    @DisplayName("GET /admin/audit redirects to /login for unauthenticated users")
    void shouldRedirectWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("GET /admin/audit redirects to /login for ordinary USER role")
    void shouldRedirectWhenNotAdmin() throws Exception {
        User user = new User("Normal User", "user@example.com", "hash", "USER");
        user.setId(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/admin/audit")
                        .sessionAttr("userId", 5L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("GET /admin/audit renders audit dashboard for ADMIN role")
    void shouldRenderAuditDashboardForAdmin() throws Exception {
        User admin = new User("Administrator", "admin@venue.com", "hash", "ADMIN");
        admin.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        AuditLog log1 = new AuditLog("admin@venue.com", "BOOKING_APPROVED", "BOOKING", 10L, "Approved");
        when(auditLogService.getAuditLogs(any(), any(), any(), any())).thenReturn(List.of(log1));
        when(auditLogService.getDistinctActions()).thenReturn(List.of("BOOKING_APPROVED", "BOOKING_CREATED"));
        when(auditLogService.getRecentLogs()).thenReturn(List.of(log1));

        mockMvc.perform(get("/admin/audit")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-audit"))
                .andExpect(model().attributeExists("logs"))
                .andExpect(model().attributeExists("actions"))
                .andExpect(model().attributeExists("recentLogs"));
    }

    @Test
    @DisplayName("GET /admin/venues renders venue management page for admin")
    void shouldRenderAdminVenuesPage() throws Exception {
        User admin = new User("Administrator", "admin@venue.com", "hash", "ADMIN");
        admin.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        Venue v1 = new Venue("Seminar Hall", "Main Block", 120, true);
        when(venueService.getAllVenues()).thenReturn(List.of(v1));

        mockMvc.perform(get("/admin/venues")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-venues"))
                .andExpect(model().attributeExists("venues"));
    }

    @Test
    @DisplayName("POST /admin/venues/create creates venue and redirects with flash message")
    void shouldCreateVenue() throws Exception {
        User admin = new User("Administrator", "admin@venue.com", "hash", "ADMIN");
        admin.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        mockMvc.perform(post("/admin/venues/create")
                        .sessionAttr("userId", 1L)
                        .param("name", "Innovation Center")
                        .param("location", "Tech Wing")
                        .param("capacity", "150")
                        .param("venueType", "LAB")
                        .param("facilities", "Computers, Projector"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/venues"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(venueService).createVenue(
                eq("Innovation Center"),
                eq("Tech Wing"),
                eq(150),
                eq(true),
                eq("LAB"),
                eq("Computers, Projector"),
                contains("admin@venue.com")
        );
    }

    @Test
    @DisplayName("POST /admin/venues/{id}/toggle-status toggles active status")
    void shouldToggleVenueStatus() throws Exception {
        User admin = new User("Administrator", "admin@venue.com", "hash", "ADMIN");
        admin.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        Venue v1 = new Venue("Auditorium", "Main", 200, false);
        when(venueService.toggleActiveStatus(eq(10L), anyString())).thenReturn(v1);

        mockMvc.perform(post("/admin/venues/10/toggle-status")
                        .sessionAttr("userId", 1L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/venues"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(venueService).toggleActiveStatus(eq(10L), contains("admin@venue.com"));
    }
}
