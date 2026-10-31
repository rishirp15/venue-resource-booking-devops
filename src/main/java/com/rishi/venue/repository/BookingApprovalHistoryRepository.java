package com.rishi.venue.repository;

import com.rishi.venue.model.BookingApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingApprovalHistoryRepository extends JpaRepository<BookingApprovalHistory, Long> {

    @Query("SELECT h FROM BookingApprovalHistory h WHERE h.booking.id = :bookingId ORDER BY h.timestamp DESC")
    List<BookingApprovalHistory> findByBookingIdOrderByTimestampDesc(@Param("bookingId") Long bookingId);

    @Query("SELECT h FROM BookingApprovalHistory h WHERE h.booking.id = :bookingId ORDER BY h.timestamp ASC")
    List<BookingApprovalHistory> findByBookingIdOrderByTimestampAsc(@Param("bookingId") Long bookingId);
}
