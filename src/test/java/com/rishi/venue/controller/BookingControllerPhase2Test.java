package com.rishi.venue.controller;

import com.rishi.venue.model.Booking;
import com.rishi.venue.model.User;
import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.VenueRepository;
import com.rishi.venue.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:bookingctrltest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BookingControllerPhase2Test {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private VenueRepository venueRepository;

    private final DefaultCsrfToken csrfToken =
            new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "dummy-csrf-token");

    @Test
    void shouldViewBookingDetailsWhenAuthorized() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 5L);
        session.setAttribute("userRole", "USER");

        User user = new User("Bob", "bob@example.com", "hash", "USER");
        user.setId(5L);

        Venue venue = new Venue("Auditorium", "Building A", 80);
        venue.setId(10L);

        Booking booking = new Booking(user, venue, LocalDate.now().plusDays(3), "MORNING");
        booking.setId(100L);

        when(bookingService.getBookingDetails(100L, 5L, "USER")).thenReturn(booking);

        mockMvc.perform(get("/bookings/100").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("booking-details"))
                .andExpect(model().attributeExists("booking"));
    }

    @Test
    void shouldShowRescheduleFormForActiveBooking() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 5L);
        session.setAttribute("userRole", "USER");

        User user = new User("Bob", "bob@example.com", "hash", "USER");
        user.setId(5L);

        Venue venue = new Venue("Auditorium", "Building A", 80);
        venue.setId(10L);

        Booking booking = new Booking(user, venue, LocalDate.now().plusDays(3), "MORNING");
        booking.setId(100L);
        booking.setStatus("PENDING");

        when(bookingService.getBookingDetails(100L, 5L, "USER")).thenReturn(booking);

        mockMvc.perform(get("/bookings/100/reschedule").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("reschedule"))
                .andExpect(model().attributeExists("booking"))
                .andExpect(model().attributeExists("venue"));
    }

    @Test
    void shouldSubmitRescheduleRequestSuccessfully() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 5L);
        session.setAttribute("userRole", "USER");

        User user = new User("Bob", "bob@example.com", "hash", "USER");
        user.setId(5L);

        Venue venue = new Venue("Auditorium", "Building A", 80);
        venue.setId(10L);

        Booking updated = new Booking(user, venue, LocalDate.now().plusDays(7), "AFTERNOON");
        updated.setId(100L);

        when(bookingService.rescheduleBooking(eq(100L), eq(5L), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class), eq("AFTERNOON")))
                .thenReturn(updated);

        mockMvc.perform(post("/bookings/100/reschedule")
                        .session(session)
                        .sessionAttr("org.springframework.security.web.csrf.CsrfToken", csrfToken)
                        .param("_csrf", csrfToken.getToken())
                        .param("bookingDate", LocalDate.now().plusDays(7).toString())
                        .param("startTime", "13:00")
                        .param("endTime", "16:00")
                        .param("slot", "AFTERNOON"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/100"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void shouldSubmitNewBookingWithTimesAndAttendees() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 5L);

        User user = new User("Bob", "bob@example.com", "hash", "USER");
        user.setId(5L);

        Venue venue = new Venue("Auditorium", "Building A", 80);
        venue.setId(10L);

        Booking booking = new Booking(user, venue, LocalDate.now().plusDays(4), "MORNING");
        booking.setId(200L);

        when(bookingService.createBooking(eq(5L), eq(10L), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class), eq(35), eq("MORNING"), anyString()))
                .thenReturn(booking);

        mockMvc.perform(post("/book")
                        .session(session)
                        .sessionAttr("org.springframework.security.web.csrf.CsrfToken", csrfToken)
                        .param("_csrf", csrfToken.getToken())
                        .param("venueId", "10")
                        .param("bookingDate", LocalDate.now().plusDays(4).toString())
                        .param("startTime", "09:00")
                        .param("endTime", "11:30")
                        .param("attendees", "35")
                        .param("slot", "MORNING")
                        .param("purpose", "Quarterly Sync"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-bookings?success"));
    }

    @Test
    void shouldRenderBookingFormWithPathVariableVenueId() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 5L);

        Venue venue = new Venue("Auditorium", "Building A", 80);
        venue.setId(10L);

        when(venueRepository.findById(10L)).thenReturn(java.util.Optional.of(venue));

        mockMvc.perform(get("/book/10").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("book"))
                .andExpect(model().attributeExists("venue", "slots", "minDate"));
    }

    @Test
    void shouldRenderBookingFormWithQueryParamVenueId() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 5L);

        Venue venue = new Venue("Auditorium", "Building A", 80);
        venue.setId(10L);

        when(venueRepository.findById(10L)).thenReturn(java.util.Optional.of(venue));

        mockMvc.perform(get("/book").param("venueId", "10").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("book"))
                .andExpect(model().attributeExists("venue", "slots", "minDate"));
    }

    @Test
    void shouldRedirectToVenuesWhenNoVenueIdProvided() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 5L);

        mockMvc.perform(get("/book").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/venues"));
    }
}
