
package com.rishi.venue.service;

import com.rishi.venue.model.Booking;
import com.rishi.venue.model.User;
import com.rishi.venue.model.Venue;

import com.rishi.venue.repository.BookingRepository;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.repository.VenueRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VenueRepository venueRepository;

    @InjectMocks
    private BookingService bookingService;

    private User user;
    private Venue venue;
    private LocalDate bookingDate;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setRole("USER");

        venue = new Venue();
        venue.setName("Seminar Hall");
        venue.setLocation("Main Building");
        venue.setCapacity(120);

        bookingDate = LocalDate.now().plusDays(7);
    }

    // TEST 1: Booking creation

    @Test
    void shouldCreateBookingSuccessfully() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(venueRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(venue));

        when(bookingRepository
                .existsByVenueAndBookingDateAndSlotAndStatusIn(
                        eq(venue),
                        eq(bookingDate),
                        eq("MORNING"),
                        anySet()
                ))
                .thenReturn(false);

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Booking result = bookingService.createBooking(
                1L,
                1L,
                bookingDate,
                "MORNING"
        );

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals("MORNING", result.getSlot());
        assertEquals(bookingDate, result.getBookingDate());

        verify(bookingRepository, times(1))
                .save(any(Booking.class));
    }

    // TEST 2: Duplicate booking rejection

    @Test
    void shouldRejectDuplicateBooking() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(venueRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(venue));

        when(bookingRepository
                .existsByVenueAndBookingDateAndSlotAndStatusIn(
                        eq(venue),
                        eq(bookingDate),
                        eq("MORNING"),
                        anySet()
                ))
                .thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> bookingService.createBooking(
                                1L,
                                1L,
                                bookingDate,
                                "MORNING"
                        )
                );

        assertTrue(exception.getMessage()
                .contains("already booked"));

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    // TEST 3: Reject past booking dates

    @Test
    void shouldRejectPastBookingDate() {

        LocalDate pastDate = LocalDate.now().minusDays(1);

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(
                        1L,
                        1L,
                        pastDate,
                        "MORNING"
                )
        );

        verifyNoInteractions(bookingRepository);
    }

    // TEST 4: Reject invalid time slots

    @Test
    void shouldRejectInvalidSlot() {

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(
                        1L,
                        1L,
                        bookingDate,
                        "MIDNIGHT"
                )
        );
    }

    // TEST 5: Confirm pending booking

    @Test
    void shouldConfirmPendingBooking() {

        Booking booking = new Booking(
                user,
                venue,
                bookingDate,
                "MORNING"
        );

        booking.setStatus("PENDING");

        when(bookingRepository.findById(10L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Booking result = bookingService.confirmBooking(10L);

        assertEquals("CONFIRMED", result.getStatus());
    }

    // TEST 6: Reject confirmation of cancelled booking

    @Test
    void shouldRejectConfirmationOfCancelledBooking() {

        Booking booking = new Booking(
                user,
                venue,
                bookingDate,
                "MORNING"
        );

        booking.setStatus("CANCELLED");

        when(bookingRepository.findById(10L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.confirmBooking(10L)
        );

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    // TEST 7: Cancel own booking

    @Test
    void shouldCancelOwnBooking() {

        Booking booking = new Booking(
                user,
                venue,
                bookingDate,
                "AFTERNOON"
        );

        booking.setStatus("PENDING");

        when(bookingRepository.findById(10L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Booking result = bookingService.cancelBooking(
                10L,
                1L
        );

        assertEquals("CANCELLED", result.getStatus());
    }

    // TEST 8: Reject cancellation by another user

    @Test
    void shouldRejectUnauthorizedCancellation() {

        Booking booking = new Booking(
                user,
                venue,
                bookingDate,
                "AFTERNOON"
        );

        booking.setStatus("PENDING");

        when(bookingRepository.findById(10L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                SecurityException.class,
                () -> bookingService.cancelBooking(
                        10L,
                        999L
                )
        );

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    // TEST 9: Reject repeated cancellation

    @Test
    void shouldRejectRepeatedCancellation() {

        Booking booking = new Booking(
                user,
                venue,
                bookingDate,
                "AFTERNOON"
        );

        booking.setStatus("CANCELLED");

        when(bookingRepository.findById(10L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.cancelBooking(
                        10L,
                        1L
                )
        );
    }

    // TEST 10: Fetch user booking history

    @Test
    void shouldFetchUserBookings() {

        when(bookingRepository
                .findByUserIdOrderByBookingDateDesc(1L))
                .thenReturn(List.of());

        List<Booking> bookings =
                bookingService.getUserBookings(1L);

        assertNotNull(bookings);
        assertTrue(bookings.isEmpty());

        verify(bookingRepository)
                .findByUserIdOrderByBookingDateDesc(1L);
    }
}
