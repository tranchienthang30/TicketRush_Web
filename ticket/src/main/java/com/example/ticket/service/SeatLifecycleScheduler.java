package com.example.ticket.service;

import com.example.ticket.repository.CheckoutQueryRepository;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeatLifecycleScheduler {
    private static final Logger log = LoggerFactory.getLogger(SeatLifecycleScheduler.class);
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final CheckoutQueryRepository checkoutRepository;

    public SeatLifecycleScheduler(CheckoutQueryRepository checkoutRepository) {
        this.checkoutRepository = checkoutRepository;
    }

    @Scheduled(
            fixedDelayString = "${app.booking.expire-interval-ms:15000}",
            initialDelayString = "${app.booking.expire-initial-delay-ms:15000}"
    )
    @Transactional
    public void expirePendingOrdersAndReleaseSeatLocks() {
        OffsetDateTime now = OffsetDateTime.now(APP_ZONE);
        int expiredOrders = checkoutRepository.expirePendingOrders(now);
        int releasedLocks = checkoutRepository.releaseExpiredSeatLocks(now);

        if (expiredOrders > 0 || releasedLocks > 0) {
            log.info("Seat lifecycle scan: expiredOrders={}, releasedLocks={}", expiredOrders, releasedLocks);
        }
    }
}
