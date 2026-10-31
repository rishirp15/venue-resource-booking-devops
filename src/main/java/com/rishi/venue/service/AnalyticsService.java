package com.rishi.venue.service;

import com.rishi.venue.dto.analytics.*;
import com.rishi.venue.model.Booking;
import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.BookingRepository;
import com.rishi.venue.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;
    private final BookingService bookingService;

    public AnalyticsService(
            BookingRepository bookingRepository,
            VenueRepository venueRepository,
            BookingService bookingService) {

        this.bookingRepository = bookingRepository;
        this.venueRepository = venueRepository;
        this.bookingService = bookingService;
    }

    public AdminAnalyticsData getAdminAnalytics(LocalDate startDate, LocalDate endDate) {
        // Automatically ensure any completed bookings are updated
        bookingService.autoMarkCompletedBookings();

        List<Booking> allBookings = bookingRepository.findAll();
        List<Venue> allVenues = venueRepository.findAll();

        // Apply date filtering if provided
        List<Booking> filteredBookings = allBookings.stream()
                .filter(b -> startDate == null || !b.getBookingDate().isBefore(startDate))
                .filter(b -> endDate == null || !b.getBookingDate().isAfter(endDate))
                .toList();

        long totalBookings = filteredBookings.size();

        // 1. Status distribution
        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        statusDistribution.put("PENDING", 0L);
        statusDistribution.put("APPROVED", 0L);
        statusDistribution.put("REJECTED", 0L);
        statusDistribution.put("CANCELLED", 0L);
        statusDistribution.put("COMPLETED", 0L);

        for (Booking b : filteredBookings) {
            String status = b.getStatus();
            if ("CONFIRMED".equalsIgnoreCase(status)) {
                status = "APPROVED";
            }
            statusDistribution.put(status, statusDistribution.getOrDefault(status, 0L) + 1L);
        }

        long approvedBookings = statusDistribution.getOrDefault("APPROVED", 0L);
        long pendingBookings = statusDistribution.getOrDefault("PENDING", 0L);
        long cancelledBookings = statusDistribution.getOrDefault("CANCELLED", 0L);
        long rejectedBookings = statusDistribution.getOrDefault("REJECTED", 0L);
        long completedBookings = statusDistribution.getOrDefault("COMPLETED", 0L);

        // 2. Monthly Trends (Chronological YearMonths)
        List<MonthlyTrend> monthlyTrends = calculateMonthlyTrends(filteredBookings);

        // 3. Most Booked & Least Utilized Venues
        Map<Long, Long> venueBookingCounts = filteredBookings.stream()
                .filter(b -> b.getVenue() != null && b.getVenue().getId() != null)
                .collect(Collectors.groupingBy(b -> b.getVenue().getId(), Collectors.counting()));

        List<VenueBookingMetric> venueMetrics = allVenues.stream()
                .map(venue -> {
                    long count = venueBookingCounts.getOrDefault(venue.getId(), 0L);
                    double share = totalBookings > 0 ? (count * 100.0) / totalBookings : 0.0;
                    return new VenueBookingMetric(
                            venue.getId(),
                            venue.getName(),
                            venue.getLocation(),
                            venue.getCapacity(),
                            count,
                            Math.round(share * 10.0) / 10.0
                    );
                })
                .sorted(Comparator.comparingLong(VenueBookingMetric::bookingCount).reversed())
                .toList();

        List<VenueBookingMetric> mostBookedVenues = venueMetrics.stream().limit(5).toList();
        List<VenueBookingMetric> leastUtilizedVenues = new ArrayList<>(venueMetrics);
        Collections.reverse(leastUtilizedVenues);
        if (leastUtilizedVenues.size() > 5) {
            leastUtilizedVenues = leastUtilizedVenues.subList(0, 5);
        }

        // 4. Peak Booking Days (Monday through Sunday)
        List<DayOfWeekMetric> peakBookingDays = calculatePeakDays(filteredBookings, totalBookings);

        // 5. Peak Booking Hours (Hour of Day)
        List<HourMetric> peakBookingHours = calculatePeakHours(filteredBookings, totalBookings);

        // 6. Venue Utilization Percentage
        List<VenueUtilizationMetric> venueUtilizations = calculateVenueUtilizations(allVenues, filteredBookings, totalBookings);

        return new AdminAnalyticsData(
                totalBookings,
                approvedBookings,
                pendingBookings,
                cancelledBookings,
                rejectedBookings,
                completedBookings,
                monthlyTrends,
                mostBookedVenues,
                leastUtilizedVenues,
                statusDistribution,
                peakBookingDays,
                peakBookingHours,
                venueUtilizations,
                startDate,
                endDate
        );
    }

    public UserAnalyticsData getUserAnalytics(Long userId) {
        bookingService.autoMarkCompletedBookings();

        List<Booking> userBookings = bookingRepository.findByUserIdOrderByBookingDateDesc(userId);
        long totalBookings = userBookings.size();

        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        statusDistribution.put("PENDING", 0L);
        statusDistribution.put("APPROVED", 0L);
        statusDistribution.put("REJECTED", 0L);
        statusDistribution.put("CANCELLED", 0L);
        statusDistribution.put("COMPLETED", 0L);

        for (Booking b : userBookings) {
            String status = b.getStatus();
            if ("CONFIRMED".equalsIgnoreCase(status)) {
                status = "APPROVED";
            }
            statusDistribution.put(status, statusDistribution.getOrDefault(status, 0L) + 1L);
        }

        long completedBookings = statusDistribution.getOrDefault("COMPLETED", 0L);
        long approvedBookings = statusDistribution.getOrDefault("APPROVED", 0L);
        long pendingBookings = statusDistribution.getOrDefault("PENDING", 0L);
        long cancelledBookings = statusDistribution.getOrDefault("CANCELLED", 0L);
        long rejectedBookings = statusDistribution.getOrDefault("REJECTED", 0L);

        double completionRate = totalBookings > 0
                ? Math.round(((completedBookings * 100.0) / totalBookings) * 10.0) / 10.0
                : 0.0;

        LocalDate today = LocalDate.now();
        List<Booking> upcomingBookings = userBookings.stream()
                .filter(b -> !b.getBookingDate().isBefore(today)
                        && BookingService.ACTIVE_STATUSES.contains(b.getStatus()))
                .sorted(Comparator.comparing(Booking::getBookingDate))
                .toList();

        long upcomingCount = upcomingBookings.size();

        // Frequently used venues for this user
        Map<Long, Long> venueCounts = userBookings.stream()
                .filter(b -> b.getVenue() != null)
                .collect(Collectors.groupingBy(b -> b.getVenue().getId(), Collectors.counting()));

        List<VenueBookingMetric> frequentlyUsedVenues = venueCounts.entrySet().stream()
                .map(entry -> {
                    Venue venue = userBookings.stream()
                            .filter(b -> b.getVenue().getId().equals(entry.getKey()))
                            .map(Booking::getVenue)
                            .findFirst()
                            .orElse(null);

                    long count = entry.getValue();
                    double share = totalBookings > 0 ? (count * 100.0) / totalBookings : 0.0;

                    return new VenueBookingMetric(
                            entry.getKey(),
                            venue != null ? venue.getName() : "Venue #" + entry.getKey(),
                            venue != null ? venue.getLocation() : "Unknown",
                            venue != null ? venue.getCapacity() : 0,
                            count,
                            Math.round(share * 10.0) / 10.0
                    );
                })
                .sorted(Comparator.comparingLong(VenueBookingMetric::bookingCount).reversed())
                .limit(5)
                .toList();

        return new UserAnalyticsData(
                totalBookings,
                completedBookings,
                approvedBookings,
                pendingBookings,
                cancelledBookings,
                rejectedBookings,
                completionRate,
                upcomingCount,
                upcomingBookings,
                statusDistribution,
                frequentlyUsedVenues
        );
    }

    private List<MonthlyTrend> calculateMonthlyTrends(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            YearMonth current = YearMonth.now();
            List<MonthlyTrend> emptyTrends = new ArrayList<>();
            for (int i = 5; i >= 0; i--) {
                YearMonth ym = current.minusMonths(i);
                emptyTrends.add(new MonthlyTrend(
                        ym.toString(),
                        ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + ym.getYear(),
                        0L
                ));
            }
            return emptyTrends;
        }

        Map<YearMonth, Long> countsByMonth = bookings.stream()
                .map(b -> YearMonth.from(b.getBookingDate()))
                .collect(Collectors.groupingBy(ym -> ym, Collectors.counting()));

        YearMonth minMonth = countsByMonth.keySet().stream().min(Comparator.naturalOrder()).orElse(YearMonth.now().minusMonths(5));
        YearMonth maxMonth = countsByMonth.keySet().stream().max(Comparator.naturalOrder()).orElse(YearMonth.now());

        if (minMonth.isAfter(maxMonth.minusMonths(5))) {
            minMonth = maxMonth.minusMonths(5);
        }

        List<MonthlyTrend> trends = new ArrayList<>();
        YearMonth cursor = minMonth;
        while (!cursor.isAfter(maxMonth)) {
            long count = countsByMonth.getOrDefault(cursor, 0L);
            String label = cursor.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + cursor.getYear();
            trends.add(new MonthlyTrend(cursor.toString(), label, count));
            cursor = cursor.plusMonths(1);
        }

        return trends;
    }

    private List<DayOfWeekMetric> calculatePeakDays(List<Booking> bookings, long totalBookings) {
        Map<DayOfWeek, Long> countsByDay = bookings.stream()
                .collect(Collectors.groupingBy(b -> b.getBookingDate().getDayOfWeek(), Collectors.counting()));

        List<DayOfWeekMetric> list = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            long count = countsByDay.getOrDefault(day, 0L);
            double pct = totalBookings > 0 ? (count * 100.0) / totalBookings : 0.0;
            list.add(new DayOfWeekMetric(
                    day.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                    day.getValue(),
                    count,
                    Math.round(pct * 10.0) / 10.0
            ));
        }
        return list;
    }

    private List<HourMetric> calculatePeakHours(List<Booking> bookings, long totalBookings) {
        Map<Integer, Long> hourCounts = new TreeMap<>();
        for (int h = 8; h <= 20; h++) {
            hourCounts.put(h, 0L);
        }

        for (Booking b : bookings) {
            LocalTime start = b.getEffectiveStartTime();
            if (start != null) {
                int hour = start.getHour();
                hourCounts.put(hour, hourCounts.getOrDefault(hour, 0L) + 1L);
            }
        }

        List<HourMetric> list = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : hourCounts.entrySet()) {
            int h = entry.getKey();
            long count = entry.getValue();
            double pct = totalBookings > 0 ? (count * 100.0) / totalBookings : 0.0;
            String label = String.format("%02d:00", h);
            list.add(new HourMetric(h, label, count, Math.round(pct * 10.0) / 10.0));
        }
        return list;
    }

    private List<VenueUtilizationMetric> calculateVenueUtilizations(
            List<Venue> venues, List<Booking> bookings, long totalBookings) {

        return venues.stream().map(venue -> {
            List<Booking> venueBookings = bookings.stream()
                    .filter(b -> b.getVenue() != null && venue.getId().equals(b.getVenue().getId()))
                    .toList();

            long totalForVenue = venueBookings.size();
            long approvedForVenue = venueBookings.stream()
                    .filter(b -> bookingService.isApprovedStatus(b.getStatus()) || "COMPLETED".equalsIgnoreCase(b.getStatus()))
                    .count();

            // Utilization percentage as share of system activity
            double utilizationPercent = totalBookings > 0
                    ? Math.round(((totalForVenue * 100.0) / totalBookings) * 10.0) / 10.0
                    : 0.0;

            return new VenueUtilizationMetric(
                    venue.getId(),
                    venue.getName(),
                    venue.getLocation(),
                    venue.getCapacity(),
                    totalForVenue,
                    approvedForVenue,
                    utilizationPercent
            );
        }).sorted(Comparator.comparingDouble(VenueUtilizationMetric::utilizationPercentage).reversed()).toList();
    }
}

