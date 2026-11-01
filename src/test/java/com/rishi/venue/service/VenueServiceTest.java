package com.rishi.venue.service;

import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VenueServiceTest {

    private VenueRepository venueRepository;
    private AuditLogService auditLogService;
    private VenueService venueService;

    @BeforeEach
    void setUp() {
        venueRepository = mock(VenueRepository.class);
        auditLogService = mock(AuditLogService.class);
        venueService = new VenueService(venueRepository, auditLogService);
    }

    @Test
    @DisplayName("Should create venue and log audit event")
    void shouldCreateVenueAndLogAudit() {
        when(venueRepository.findByName("Innovation Hall")).thenReturn(Optional.empty());
        when(venueRepository.save(any(Venue.class))).thenAnswer(inv -> {
            Venue v = inv.getArgument(0);
            v.setId(25L);
            return v;
        });

        Venue created = venueService.createVenue(
                "Innovation Hall",
                "Engineering Wing",
                80,
                true,
                "LAB",
                "Workstations, Wi-Fi",
                "admin@venue.com"
        );

        assertNotNull(created);
        assertEquals(25L, created.getId());
        assertEquals("Innovation Hall", created.getName());
        assertEquals("LAB", created.getVenueType());
        assertTrue(created.isActive());

        verify(venueRepository).save(any(Venue.class));
        verify(auditLogService).log(
                eq("admin@venue.com"),
                eq("VENUE_CREATED"),
                eq("VENUE"),
                eq(25L),
                contains("Innovation Hall")
        );
    }

    @Test
    @DisplayName("Should prevent creating venue with duplicate name")
    void shouldPreventDuplicateVenueName() {
        Venue existing = new Venue("Seminar Hall", "Main", 100);
        when(venueRepository.findByName("Seminar Hall")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () ->
                venueService.createVenue("Seminar Hall", "New Wing", 50, true, "GENERAL", null, "admin"));
    }

    @Test
    @DisplayName("Should update venue and log audit event")
    void shouldUpdateVenueAndLogAudit() {
        Venue existing = new Venue("Old Hall", "Block A", 50, true, "GENERAL", "Basic");
        existing.setId(5L);

        when(venueRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(venueRepository.findByName("Renovated Hall")).thenReturn(Optional.empty());
        when(venueRepository.save(any(Venue.class))).thenAnswer(inv -> inv.getArgument(0));

        Venue updated = venueService.updateVenue(
                5L,
                "Renovated Hall",
                "Block B",
                120,
                true,
                "CONFERENCE",
                "Projector, Mic",
                "admin@venue.com"
        );

        assertEquals("Renovated Hall", updated.getName());
        assertEquals("Block B", updated.getLocation());
        assertEquals(120, updated.getCapacity());

        verify(auditLogService).log(
                eq("admin@venue.com"),
                eq("VENUE_UPDATED"),
                eq("VENUE"),
                eq(5L),
                contains("Renovated Hall")
        );
    }

    @Test
    @DisplayName("Should toggle venue active status and log audit event")
    void shouldToggleVenueActiveStatus() {
        Venue venue = new Venue("Auditorium", "Main", 200, true);
        venue.setId(9L);

        when(venueRepository.findById(9L)).thenReturn(Optional.of(venue));
        when(venueRepository.save(any(Venue.class))).thenAnswer(inv -> inv.getArgument(0));

        Venue toggled = venueService.toggleActiveStatus(9L, "admin@venue.com");
        assertFalse(toggled.isActive());

        verify(auditLogService).log(
                eq("admin@venue.com"),
                eq("VENUE_UPDATED"),
                eq("VENUE"),
                eq(9L),
                contains("INACTIVE")
        );
    }
}
