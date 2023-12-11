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

import static com.github.twitch4j.helix.domain.Video.*;

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
        Gauge
                .builder("twitch_moderator_count", this, TwitchMetricsExporter::getModeratorCount)
                .register(meterRegistry);
        Gauge
                .builder("twitch_subscription_count", this, TwitchMetricsExporter::getSubscriptionCount)
                .register(meterRegistry);
        Gauge
                .builder("twitch_video_count",this, TwitchMetricsExporter::getVideoCount)
                .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 600000)
    public void exportMetrics() {
        getInboundFollowersCount();
        getLiveViewerCount();
        getModeratorCount();
        getSubscriptionCount();
        getVideoCount();
    }

    Integer getInboundFollowersCount() {
        InboundFollowers inboundFollowers = getInboundFollowers();
        if (inboundFollowers != null) {
            return inboundFollowers.getTotal();
        }
        return 0;
    }

    Integer getLiveViewerCount() {
        Stream stream = getStream();
        if (stream != null) {
            return stream.getViewerCount();
        }
        return 0;
    }

    Integer getModeratorCount() {
        return getModeratorList().getModerators().isEmpty() ? 0 : getModeratorList().getModerators().size();
    }

    Integer getSubscriptionCount() {
        return getSubscriptionList().getSubscriptions().size();
    }

    Integer getVideoCount() {
        return getVideoList().getVideos().size();
    }

    private Stream getStream() {
        try {
            StreamList resultList = twitchClient
                    .getHelix()
                    .getStreams(authToken, null, null, null, null, null, null, Collections.singletonList(channel)).execute();
            return resultList.getStreams().isEmpty() ? null : resultList.getStreams().getFirst();
        } catch (Exception e) {
            logger.error("TwitchMetricsExporter.getStream:", e);
        }
        return null;
    }

    private InboundFollowers getInboundFollowers() {
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

    private ModeratorList getModeratorList() {
        ModeratorList moderatorList = twitchClient
                .getHelix()
                .getModerators(authToken, broadcasterId, null, null, null)
                .execute();

        moderatorList.getModerators().forEach((moderator -> logger.info(moderator.toString())));

        return moderatorList;
    }

    private SubscriptionList getSubscriptionList() {
        SubscriptionList subscriptionList = twitchClient
                .getHelix().getSubscriptions(authToken, broadcasterId, null, null, null)
                .execute();

        subscriptionList.getSubscriptions().forEach(subscription -> logger.info(subscription.getUserName()));

        return subscriptionList;
    }

    private VideoList getVideoList() {
        VideoList resultList = (VideoList) twitchClient
                .getHelix()
                .getVideos(authToken, null, broadcasterId, null, null, SearchPeriod.ALL, SearchOrder.TIME, Type.ALL, 100, null, null)
                .execute();

        resultList.getVideos().forEach(video -> logger.info(video.getTitle()));

        return resultList;
    }

}