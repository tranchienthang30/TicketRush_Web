package com.example.ticket.model.entity;

import com.example.ticket.model.enums.EventStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    private String description;

    private String genre;

    private String country;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "director_name")
    private String directorName;

    @Column(name = "cast_members")
    private String castMembers;

    @Column(name = "performer_names")
    private String performerNames;

    @Column(name = "singer_names")
    private String singerNames;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "listing_type", nullable = false)
    @Builder.Default
    private String listingType = "NOW_SHOWING";

    @Column(name = "location_name")
    private String locationName;

    private String address;
    private String city;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "sale_start_time")
    private Instant saleStartTime;

    @Column(name = "sale_end_time")
    private Instant saleEndTime;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "event_status")
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;

    @Column(name = "seat_provider", nullable = false)
    @Builder.Default
    private String seatProvider = "INTERNAL";

    @Column(name = "external_seat_chart_key")
    private String externalSeatChartKey;

    @Column(name = "payout_bank_name")
    private String payoutBankName;

    @Column(name = "payout_account_name")
    private String payoutAccountName;

    @Column(name = "payout_account_number")
    private String payoutAccountNumber;

    @Column(name = "provider_terms_accepted_at")
    private Instant providerTermsAcceptedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EventSection> sections = new ArrayList<>();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }
}
