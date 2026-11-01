package com.rishi.venue.controller;

import com.rishi.venue.model.Booking;
import com.rishi.venue.model.User;
import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.repository.VenueRepository;
import com.rishi.venue.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:adminworkflowtest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AdminApprovalWorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private VenueRepository venueRepository;

    @Test
    void shouldApproveBookingWhenAdmin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        User admin = new User("Admin", "admin@venue.com", "hash", "ADMIN");
        admin.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        mockMvc.perform(post("/admin/bookings/50/approve")
                        .session(session)
                        .param("remarks", "Approved for workshop"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/bookings"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(bookingService).approveBooking(eq(50L), contains("admin@venue.com"), eq("Approved for workshop"));
    }

    @Test
    void shouldRejectBookingWithReasonWhenAdmin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        User admin = new User("Admin", "admin@venue.com", "hash", "ADMIN");
        admin.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        mockMvc.perform(post("/admin/bookings/50/reject")
                        .session(session)
                        .param("rejectionReason", "Auditorium undergoes maintenance"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/bookings"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(bookingService).rejectBooking(eq(50L), contains("admin@venue.com"), eq("Auditorium undergoes maintenance"));
    }

    @Test
    void shouldFailRejectWithoutReason() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        User admin = new User("Admin", "admin@venue.com", "hash", "ADMIN");
        admin.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        mockMvc.perform(post("/admin/bookings/50/reject")
                        .session(session)
                        .param("rejectionReason", "  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/bookings"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(bookingService, never()).rejectBooking(anyLong(), anyString(), anyString());
    }

    @Test
    void shouldDenyNonAdminFromApproving() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 10L);

        User regularUser = new User("Normal User", "user@example.com", "hash", "USER");
        regularUser.setId(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(regularUser));

        mockMvc.perform(post("/admin/bookings/50/approve").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(bookingService, never()).approveBooking(anyLong(), anyString(), anyString());
    }

    @Test
    void shouldDenyNonAdminFromRejecting() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 10L);

        User regularUser = new User("Normal User", "user@example.com", "hash", "USER");
        regularUser.setId(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(regularUser));

        mockMvc.perform(post("/admin/bookings/50/reject")
                        .session(session)
                        .param("rejectionReason", "Attempt"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(bookingService, never()).rejectBooking(anyLong(), anyString(), anyString());
    }

    @Test
    void shouldFilterBookingsOnAdminPage() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        User admin = new User("Admin", "admin@venue.com", "hash", "ADMIN");
        admin.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        when(bookingService.getBookingsWithFilters(eq("PENDING"), any(), eq(2L)))
                .thenReturn(List.of());
        when(bookingService.getBookingStatusCounts())
                .thenReturn(Map.of("ALL", 5L, "PENDING", 2L, "APPROVED", 2L, "REJECTED", 1L, "CANCELLED", 0L, "COMPLETED", 0L));

        mockMvc.perform(get("/admin/bookings")
                        .session(session)
                        .param("status", "PENDING")
                        .param("venueId", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-bookings"))
                .andExpect(model().attributeExists("bookings"))
                .andExpect(model().attributeExists("statusCounts"))
                .andExpect(model().attribute("selectedStatus", "PENDING"))
                .andExpect(model().attribute("selectedVenueId", 2L));
    }
}

