package com.rishi.venue.service;

import com.rishi.venue.dto.analytics.VenueRecommendation;
import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private RecommendationService recommendationService;

    private Venue smallRoom;
    private Venue mediumRoom;
    private Venue largeHall;

    @BeforeEach
    void setUp() {
        smallRoom = new Venue("Huddle Room", "Ground Floor", 20, true, "MEETING", "Whiteboard, Display");
        smallRoom.setId(1L);

        mediumRoom = new Venue("Executive Boardroom", "2nd Floor", 50, true, "CONFERENCE", "Video Conferencing, Projector, Sound, Wi-Fi");
        mediumRoom.setId(2L);

        largeHall = new Venue("Main Auditorium", "West Wing", 500, true, "AUDITORIUM", "Stage, Lighting, Sound System, Wi-Fi");
        largeHall.setId(3L);
    }

    @Test
    void shouldRecommendAvailableVenuesMatchingCapacityWithBestFitRankedFirst() {
        when(venueRepository.findAll()).thenReturn(List.of(smallRoom, mediumRoom, largeHall));
        when(bookingService.hasTimeConflict(any(), any(), any(), any(), any())).thenReturn(false);

        LocalDate date = LocalDate.now().plusDays(5);

        // Requested for 45 attendees
        List<VenueRecommendation> recs = recommendationService.recommendVenues(
                45, date, LocalTime.of(9, 0), LocalTime.of(12, 0), "MORNING", null, null
        );

        // smallRoom has capacity 20 < 45, so it MUST be excluded
        assertEquals(2, recs.size());
        assertTrue(recs.stream().noneMatch(r -> r.venue().getId().equals(1L)));

        // mediumRoom (capacity 50, ratio 45/50 = 90%) should score higher than largeHall (capacity 500, ratio 45/500 = 9%)
        assertEquals(2L, recs.get(0).venue().getId(), "Executive Boardroom should be recommended #1 due to optimal capacity fit");
        assertEquals(3L, recs.get(1).venue().getId(), "Main Auditorium should be ranked #2");

        assertTrue(recs.get(0).matchScore() > recs.get(1).matchScore());
    }

    @Test
    void shouldExcludeConflictedVenues() {
        when(venueRepository.findAll()).thenReturn(List.of(mediumRoom, largeHall));
        LocalDate date = LocalDate.now().plusDays(3);

        // mediumRoom is conflicted
        when(bookingService.hasTimeConflict(eq(mediumRoom), eq(date), any(), any(), any())).thenReturn(true);
        // largeHall is free
        when(bookingService.hasTimeConflict(eq(largeHall), eq(date), any(), any(), any())).thenReturn(false);

        List<VenueRecommendation> recs = recommendationService.recommendVenues(
                30, date, LocalTime.of(9, 0), LocalTime.of(12, 0), "MORNING", null, null
        );

        assertEquals(1, recs.size());
        assertEquals(3L, recs.get(0).venue().getId(), "Only unconflicted largeHall should be returned");
    }

    @Test
    void shouldExcludeInactiveVenues() {
        mediumRoom.setActive(false);
        when(venueRepository.findAll()).thenReturn(List.of(mediumRoom, largeHall));

        List<VenueRecommendation> recs = recommendationService.recommendVenues(
                30, LocalDate.now().plusDays(2), LocalTime.of(9, 0), LocalTime.of(12, 0), "MORNING", null, null
        );

        assertEquals(1, recs.size());
        assertEquals(3L, recs.get(0).venue().getId());
    }

    @Test
    void shouldBoostScoreForTypeAndFacilityMatch() {
        when(venueRepository.findAll()).thenReturn(List.of(mediumRoom, largeHall));
        when(bookingService.hasTimeConflict(any(), any(), any(), any(), any())).thenReturn(false);

        LocalDate date = LocalDate.now().plusDays(4);

        // Request specifically for CONFERENCE with "Video" facility
        List<VenueRecommendation> recs = recommendationService.recommendVenues(
                40, date, LocalTime.of(13, 0), LocalTime.of(16, 0), "AFTERNOON", "CONFERENCE", "Video"
        );

        assertEquals(2, recs.size());
        VenueRecommendation topMatch = recs.get(0);
        assertEquals(2L, topMatch.venue().getId());
        assertTrue(topMatch.matchHighlights().stream().anyMatch(h -> h.contains("CONFERENCE")));
        assertTrue(topMatch.matchHighlights().stream().anyMatch(h -> h.contains("Video")));
    }
}

