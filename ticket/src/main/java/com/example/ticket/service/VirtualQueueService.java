package com.example.ticket.service;

import com.example.ticket.dto.VirtualQueueStatusResponse;
import com.example.ticket.exception.ApiException;
import com.example.ticket.repository.EventQueryRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class VirtualQueueService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String KEY_PREFIX = "virtual-queue:event:";
    private static final String PENALTY_COUNT_KEY_PREFIX = "virtual-queue:penalty:count:event:";
    private static final String PENALTY_UNTIL_KEY_PREFIX = "virtual-queue:penalty:until:event:";

    private final StringRedisTemplate redisTemplate;
    private final EventQueryRepository eventQueryRepository;
    private final boolean enabled;
    private final int maxActiveUsers;
    private final int releaseBatchSize;
    private final long accessTtlSeconds;
    private final Duration redisKeyTtl;
    private final boolean reentryPenaltyEnabled;
    private final long penaltyBaseSeconds;
    private final long penaltyMaxSeconds;
    private final long penaltyStrikeTtlSeconds;

    public VirtualQueueService(
            StringRedisTemplate redisTemplate,
            EventQueryRepository eventQueryRepository,
            @Value("${app.virtual-queue.enabled:true}") boolean enabled,
            @Value("${app.virtual-queue.max-active-users:50}") int maxActiveUsers,
            @Value("${app.virtual-queue.release-batch-size:50}") int releaseBatchSize,
            @Value("${app.virtual-queue.access-ttl-seconds:900}") long accessTtlSeconds,
            @Value("${app.virtual-queue.reentry-penalty-enabled:true}") boolean reentryPenaltyEnabled,
            @Value("${app.virtual-queue.reentry-penalty-base-seconds:120}") long penaltyBaseSeconds,
            @Value("${app.virtual-queue.reentry-penalty-max-seconds:3600}") long penaltyMaxSeconds,
            @Value("${app.virtual-queue.reentry-penalty-strike-ttl-seconds:86400}") long penaltyStrikeTtlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.eventQueryRepository = eventQueryRepository;
        this.enabled = enabled;
        this.maxActiveUsers = Math.max(1, maxActiveUsers);
        this.releaseBatchSize = Math.max(1, releaseBatchSize);
        this.accessTtlSeconds = Math.max(60, accessTtlSeconds);
        this.redisKeyTtl = Duration.ofSeconds(Math.max(this.accessTtlSeconds * 4, 3600));
        this.reentryPenaltyEnabled = reentryPenaltyEnabled;
        this.penaltyBaseSeconds = Math.max(30, penaltyBaseSeconds);
        this.penaltyMaxSeconds = Math.max(this.penaltyBaseSeconds, penaltyMaxSeconds);
        this.penaltyStrikeTtlSeconds = Math.max(this.penaltyMaxSeconds, penaltyStrikeTtlSeconds);
    }

    public synchronized VirtualQueueStatusResponse join(UUID eventId, UUID userId) {
        validateUser(userId);
        if (!enabled) {
            return readyResponse(eventId, null, "Virtual queue is disabled");
        }

        validateBookableEvent(eventId);
        long now = nowMillis();
        String member = member(userId);
        long penaltyRemainingMillis = penaltyRemainingMillis(eventId, userId, now);
        if (penaltyRemainingMillis > 0) {
            return cooldownResponse(eventId, penaltyRemainingMillis);
        }

        promoteWaitingUsers(eventId, now);

        Double activeScore = redisTemplate.opsForZSet().score(activeKey(eventId), member);
        if (isActive(activeScore, now)) {
            return touchAndReady(eventId, member, "You can enter seat selection now");
        }
        if (activeScore != null) {
            redisTemplate.opsForZSet().remove(activeKey(eventId), member);
        }

        Long waitingRank = redisTemplate.opsForZSet().rank(waitingKey(eventId), member);
        if (waitingRank == null) {
            long activeUsers = count(activeKey(eventId));
            long waitingUsers = count(waitingKey(eventId));
            if (waitingUsers == 0 && activeUsers < maxActiveUsers) {
                grantAccess(eventId, member, now);
                return readyResponse(
                        eventId,
                        now + accessTtlSeconds * 1000,
                        "You can enter seat selection now"
                );
            }

            Long sequence = redisTemplate.opsForValue().increment(sequenceKey(eventId));
            redisTemplate.opsForZSet().add(waitingKey(eventId), member, sequence == null ? now : sequence.doubleValue());
            refreshKeyTtl(eventId);
        }

        promoteWaitingUsers(eventId, now);
        activeScore = redisTemplate.opsForZSet().score(activeKey(eventId), member);
        if (isActive(activeScore, now)) {
            return touchAndReady(eventId, member, "You can enter seat selection now");
        }

        return waitingResponse(eventId, member);
    }

    public synchronized VirtualQueueStatusResponse status(UUID eventId, UUID userId) {
        validateUser(userId);
        if (!enabled) {
            return readyResponse(eventId, null, "Virtual queue is disabled");
        }

        long now = nowMillis();
        String member = member(userId);
        long penaltyRemainingMillis = penaltyRemainingMillis(eventId, userId, now);
        if (penaltyRemainingMillis > 0) {
            return cooldownResponse(eventId, penaltyRemainingMillis);
        }

        promoteWaitingUsers(eventId, now);

        Double activeScore = redisTemplate.opsForZSet().score(activeKey(eventId), member);
        if (isActive(activeScore, now)) {
            return touchAndReady(eventId, member, "You can enter seat selection now");
        }
        if (activeScore != null) {
            redisTemplate.opsForZSet().remove(activeKey(eventId), member);
        }

        Long waitingRank = redisTemplate.opsForZSet().rank(waitingKey(eventId), member);
        if (waitingRank != null) {
            return waitingResponse(eventId, member);
        }

        return new VirtualQueueStatusResponse(
                eventId,
                "NOT_JOINED",
                null,
                count(activeKey(eventId)),
                count(waitingKey(eventId)),
                null,
                releaseBatchSize,
                "Join the waiting room before seat selection"
        );
    }

    public synchronized void leave(UUID eventId, UUID userId) {
        validateUser(userId);
        if (!enabled) {
            return;
        }

        String member = member(userId);
        redisTemplate.opsForZSet().remove(waitingKey(eventId), member);
        redisTemplate.opsForZSet().remove(activeKey(eventId), member);
        promoteWaitingUsers(eventId, nowMillis());
    }

    public synchronized void requireAccess(UUID eventId, UUID userId) {
        validateUser(userId);
        if (!enabled) {
            return;
        }

        long now = nowMillis();
        String member = member(userId);
        long penaltyRemainingMillis = penaltyRemainingMillis(eventId, userId, now);
        if (penaltyRemainingMillis > 0) {
            long waitSeconds = Math.max(1, penaltyRemainingMillis / 1000);
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait " + waitSeconds + "s before re-entering seat selection"
            );
        }
        cleanupExpiredActive(eventId, now);

        Double activeScore = redisTemplate.opsForZSet().score(activeKey(eventId), member);
        if (!isActive(activeScore, now)) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait in the virtual queue before selecting seats"
            );
        }

        grantAccess(eventId, member, now);
    }

    public void registerExpiredSeatLockStrike(UUID eventId, UUID userId, long seatCount) {
        validateUser(userId);
        if (!enabled || !reentryPenaltyEnabled || eventId == null) {
            return;
        }

        long now = nowMillis();
        long strikeCount = incrementStrike(eventId, userId);
        long cooldownSeconds = penaltySecondsForStrike(strikeCount);
        long cooldownUntil = now + cooldownSeconds * 1000;

        redisTemplate.opsForValue().set(
                penaltyUntilKey(eventId, userId),
                String.valueOf(cooldownUntil),
                Duration.ofSeconds(cooldownSeconds + 60)
        );
        redisTemplate.opsForZSet().remove(activeKey(eventId), member(userId));
        redisTemplate.opsForZSet().remove(waitingKey(eventId), member(userId));
    }

    private void validateBookableEvent(UUID eventId) {
        eventQueryRepository.findPublishedEventForBooking(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event is not available for booking"));
    }

    private void promoteWaitingUsers(UUID eventId, long now) {
        cleanupExpiredActive(eventId, now);

        long availableSlots = maxActiveUsers - count(activeKey(eventId));
        if (availableSlots <= 0) {
            return;
        }

        long usersToPromote = Math.min(availableSlots, releaseBatchSize);
        Set<String> nextUsers = redisTemplate.opsForZSet().range(waitingKey(eventId), 0, usersToPromote - 1);
        if (nextUsers == null || nextUsers.isEmpty()) {
            return;
        }

        for (String waitingUser : nextUsers) {
            redisTemplate.opsForZSet().remove(waitingKey(eventId), waitingUser);
            grantAccess(eventId, waitingUser, now);
        }
    }

    private void cleanupExpiredActive(UUID eventId, long now) {
        redisTemplate.opsForZSet().removeRangeByScore(activeKey(eventId), 0, now);
    }

    private VirtualQueueStatusResponse waitingResponse(UUID eventId, String member) {
        Long rank = redisTemplate.opsForZSet().rank(waitingKey(eventId), member);
        Long position = rank == null ? null : rank + 1;
        long waitSeconds = 5;
        return new VirtualQueueStatusResponse(
                eventId,
                "WAITING",
                position,
                count(activeKey(eventId)),
                count(waitingKey(eventId)),
                null,
                releaseBatchSize,
                "Please wait " + waitSeconds + " seconds and check again"
        );
    }

    private VirtualQueueStatusResponse cooldownResponse(UUID eventId, long penaltyRemainingMillis) {
        long waitSeconds = Math.max(1, penaltyRemainingMillis / 1000);
        return new VirtualQueueStatusResponse(
                eventId,
                "WAITING",
                null,
                count(activeKey(eventId)),
                count(waitingKey(eventId)),
                null,
                releaseBatchSize,
                "Please wait " + waitSeconds + " seconds before booking again"
        );
    }

    private VirtualQueueStatusResponse touchAndReady(UUID eventId, String member, String message) {
        long expiresAt = grantAccess(eventId, member, nowMillis());
        return readyResponse(eventId, expiresAt, message);
    }

    private VirtualQueueStatusResponse readyResponse(UUID eventId, Long expiresAtMillis, String message) {
        return new VirtualQueueStatusResponse(
                eventId,
                "READY",
                null,
                count(activeKey(eventId)),
                count(waitingKey(eventId)),
                expiresAtMillis == null ? null : toOffsetDateTime(expiresAtMillis),
                releaseBatchSize,
                message
        );
    }

    private long grantAccess(UUID eventId, String member, long now) {
        long expiresAt = now + accessTtlSeconds * 1000;
        redisTemplate.opsForZSet().add(activeKey(eventId), member, expiresAt);
        refreshKeyTtl(eventId);
        return expiresAt;
    }

    private void refreshKeyTtl(UUID eventId) {
        redisTemplate.expire(activeKey(eventId), redisKeyTtl);
        redisTemplate.expire(waitingKey(eventId), redisKeyTtl);
        redisTemplate.expire(sequenceKey(eventId), redisKeyTtl);
    }

    private boolean isActive(Double score, long now) {
        return score != null && score.longValue() > now;
    }

    private long penaltyRemainingMillis(UUID eventId, UUID userId, long now) {
        if (!enabled || !reentryPenaltyEnabled || eventId == null || userId == null) {
            return 0;
        }

        String raw = redisTemplate.opsForValue().get(penaltyUntilKey(eventId, userId));
        if (raw == null || raw.isBlank()) {
            return 0;
        }

        try {
            long until = Long.parseLong(raw.trim());
            if (until <= now) {
                redisTemplate.delete(penaltyUntilKey(eventId, userId));
                return 0;
            }
            return until - now;
        } catch (NumberFormatException ignored) {
            redisTemplate.delete(penaltyUntilKey(eventId, userId));
            return 0;
        }
    }

    private long incrementStrike(UUID eventId, UUID userId) {
        Long next = redisTemplate.opsForValue().increment(penaltyCountKey(eventId, userId));
        redisTemplate.expire(penaltyCountKey(eventId, userId), Duration.ofSeconds(penaltyStrikeTtlSeconds));
        return next == null ? 1 : Math.max(1, next);
    }

    private long penaltySecondsForStrike(long strikeCount) {
        long power = Math.min(20, Math.max(0, strikeCount - 1));
        long multiplier = 1L << power;
        long calculated = penaltyBaseSeconds * multiplier;
        return Math.min(penaltyMaxSeconds, calculated);
    }

    private long count(String key) {
        Long count = redisTemplate.opsForZSet().zCard(key);
        return count == null ? 0 : count;
    }

    private long nowMillis() {
        return Instant.now().toEpochMilli();
    }

    private OffsetDateTime toOffsetDateTime(long millis) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), APP_ZONE);
    }

    private void validateUser(UUID userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "You need to sign in");
        }
    }

    private String member(UUID userId) {
        return userId.toString();
    }

    private String activeKey(UUID eventId) {
        return KEY_PREFIX + eventId + ":active";
    }

    private String waitingKey(UUID eventId) {
        return KEY_PREFIX + eventId + ":waiting";
    }

    private String sequenceKey(UUID eventId) {
        return KEY_PREFIX + eventId + ":sequence";
    }

    private String penaltyCountKey(UUID eventId, UUID userId) {
        return PENALTY_COUNT_KEY_PREFIX + eventId + ":user:" + userId;
    }

    private String penaltyUntilKey(UUID eventId, UUID userId) {
        return PENALTY_UNTIL_KEY_PREFIX + eventId + ":user:" + userId;
    }
}
