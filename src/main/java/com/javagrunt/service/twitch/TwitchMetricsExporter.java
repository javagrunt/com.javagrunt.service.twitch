package com.javagrunt.service.twitch;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.helix.domain.*;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class TwitchMetricsExporter {

    Logger logger = LoggerFactory.getLogger(TwitchService.class);
    private final TwitchClient twitchClient;
    private final String authToken;
    private final String broadcasterId;
    private final String channel;


    public TwitchMetricsExporter(TwitchService twitchService, MeterRegistry meterRegistry,
                                 @Value("${twitch.channel}") String channel) {
        this.channel = channel;
        this.twitchClient = twitchService.getClient();
        this.authToken = twitchService.getAccessToken();
        this.broadcasterId = twitchService.getBroadcasterId();
        Gauge
                .builder("twitch_live_viewer_count", this, TwitchMetricsExporter::getLiveViewerCount)
                .register(meterRegistry);
        Gauge
                .builder("twitch_inbound_followers", this, TwitchMetricsExporter::getInboundFollowersCount)
                .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 60000) // Run every minute
    public void exportMetrics() {
        getLiveViewerCount();
        getInboundFollowersCount();
    }

    Integer getLiveViewerCount() {
        Stream stream = getStream();
        if (stream != null) {
            return stream.getViewerCount();
        }
        return 0;
    }

    Stream getStream() {
        try {
            StreamList resultList = twitchClient
                    .getHelix()
                    .getStreams(authToken, null, null, null, null, null, null, Collections.singletonList(channel)).execute();
            Stream stream = resultList.getStreams().get(0);
            logger.info("Stream: {}", stream);
            return stream;
        } catch (Exception e) {
            logger.error("TwitchMetricsExporter.getStream:", e);
        }
        return null;
    }

    Integer getInboundFollowersCount() {
        InboundFollowers inboundFollowers = getInboundFollowers();
        if (inboundFollowers != null) {
            return inboundFollowers.getTotal();
        }
        return 0;
    }

    InboundFollowers getInboundFollowers() {
        try {
            InboundFollowers inboundFollowers = twitchClient
                    .getHelix()
                    .getChannelFollowers(authToken, broadcasterId, null, null, null)
                    .execute();
            logger.info("InboundFollowers: {}", inboundFollowers);
            return inboundFollowers;
        } catch (Exception e) {
            logger.error("TwitchMetricsExporter.getFollowers:", e);
        }
        return null;
    }
    
}
