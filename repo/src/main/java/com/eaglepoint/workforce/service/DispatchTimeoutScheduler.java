package com.eaglepoint.workforce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DispatchTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(DispatchTimeoutScheduler.class);

    private final DispatchService dispatchService;

    public DispatchTimeoutScheduler(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Scheduled(fixedRate = 10000)
    public void processExpiredOffers() {
        int count = dispatchService.processExpiredOffers();
        if (count > 0) {
            log.info("Processed {} expired dispatch offers, auto-redispatched", count);
        }
    }
}
