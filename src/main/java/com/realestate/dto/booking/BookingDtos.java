package com.realestate.dto.booking;

import com.realestate.entity.SiteVisitBooking;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

public class BookingDtos {

    @Data
    public static class BookSiteVisitRequest {
        @NotBlank(message = "Name is required")
        @Size(max = 150)
        private String contactName;

        @Size(max = 15)
        private String contactPhone;

        @Size(max = 255)
        private String contactEmail;

        @Size(max = 40)
        private String preferredDate;

        @Size(max = 60)
        private String preferredWindow;

        @Size(max = 2000)
        private String notes;
    }

    @Data
    public static class CancelBookingRequest {
        @Size(max = 1000)
        private String reason;
    }

    @Data @Builder
    public static class SiteVisitBookingResponse {
        private UUID   id;
        private UUID   propertyId;
        private String propertyTitle;
        private String propertyImageUrl;
        private String propertyLocality;
        private String propertyCity;

        private String contactName;
        private String contactPhone;
        private String contactEmail;

        private String preferredDate;
        private String preferredWindow;
        private String notes;

        private String status;
        private String cancelReason;
        private String cancelledBy;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static SiteVisitBookingResponse from(SiteVisitBooking b, String primaryImageUrl) {
            return SiteVisitBookingResponse.builder()
                .id(b.getId())
                .propertyId(b.getProperty().getId())
                .propertyTitle(b.getProperty().getTitle())
                .propertyImageUrl(primaryImageUrl)
                .propertyLocality(b.getProperty().getLocality().getName())
                .propertyCity(b.getProperty().getLocality().getCity().getName())
                .contactName(b.getContactName())
                .contactPhone(b.getContactPhone())
                .contactEmail(b.getContactEmail())
                .preferredDate(b.getPreferredDate())
                .preferredWindow(b.getPreferredWindow())
                .notes(b.getNotes())
                .status(b.getStatus().name())
                .cancelReason(b.getCancelReason())
                .cancelledBy(b.getCancelledBy() != null ? b.getCancelledBy().name() : null)
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
        }
    }
}
