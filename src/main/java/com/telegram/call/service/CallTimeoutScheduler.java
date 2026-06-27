package com.telegram.call.service;

import com.telegram.call.entity.Call;
import com.telegram.call.repository.CallRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class CallTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(CallTimeoutScheduler.class);
    private static final int RING_TIMEOUT_SECONDS = 30;

    private final CallRepo callRepo;
    private final CallService callService;

    public CallTimeoutScheduler(CallRepo callRepo, CallService callService) {
        this.callRepo = callRepo;
        this.callService = callService;
    }

    @Scheduled(fixedRate = 5000)
    public void expireTimedOutCalls() {
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(RING_TIMEOUT_SECONDS);
        List<Call> timedOut = callRepo.findTimedOutRingingCalls(cutoff);

        for (Call call : timedOut) {
            try {
                callService.markAsMissed(call.getId());
                log.info("Call {} marked as missed (timed out after {}s)", call.getId(), RING_TIMEOUT_SECONDS);
            } catch (Exception e) {
                log.error("Failed to mark call {} as missed: {}", call.getId(), e.getMessage());
            }
        }
    }
}