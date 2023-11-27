package com.javagrunt.service.twitch;

import com.github.twitch4j.helix.domain.ModeratorList;
import com.github.twitch4j.helix.domain.SubscriptionList;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TwitchMetricsExporter {

    Logger logger = LoggerFactory.getLogger(TwitchService.class);
    private final TwitchService twitchService;
    private final String authToken;
    private final String broadcasterId;


    public TwitchMetricsExporter(TwitchService twitchService, MeterRegistry meterRegistry) {
        this.twitchService = twitchService;
        this.authToken = twitchService.getAccessToken();
        this.broadcasterId = twitchService.getBroadcasterId();
        Gauge subscriptionsGauge = Gauge
                .builder("twitch_subscriptions", this, TwitchMetricsExporter::getSubscriptionsCount)
                .register(meterRegistry);
        Gauge moderatorsGauge = Gauge
                .builder("twitch_moderators", this, TwitchMetricsExporter::getModeratorsCount)
                .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 60000) // Run every minute
    public void exportMetrics() {
        getSubscriptionsCount();
        getModeratorsCount();
    }

    Integer getSubscriptionsCount() {
        SubscriptionList subscriptionList = twitchService
                .getClient()
                .getHelix()
                .getSubscriptions(authToken, broadcasterId, null, null, null)
                .execute();
        logger.info("TwitchMetricsExporter.getSubscriptionsCount: {}", subscriptionList.getSubscriptions().size());
        return subscriptionList.getSubscriptions().size();
    }
    
    Integer getModeratorsCount() {
        ModeratorList resultList = twitchClient.getHelix().getModerators(authToken, broadcasterId, null, null).execute();
        resultList.getModerators().forEach(moderator -> {
            logger.info("Moderator: {}", moderator);
        });
    }
}
