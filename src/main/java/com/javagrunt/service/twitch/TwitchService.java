package com.javagrunt.service.twitch;

import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.helix.domain.UserList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;


@Component
public class TwitchService {

    Logger logger = LoggerFactory.getLogger(TwitchService.class);
    private final TwitchClient client;
    private final OAuth2Credential oAuth2Credential;
    private final String accessToken;

    private final String channel;

    public TwitchService(@Value("${twitch.client-id}") String clientId,
                         @Value("${twitch.client-secret}") String clientSecret,
                         @Value("${twitch.channel}") String channel,
                         @Value("${twitch.client-redirect-url}") String clientRedirectUrl,
                         @Value("${twitch.token}") String accessToken) {
        this.accessToken = accessToken;
        this.channel = channel;
        CredentialManager credentialManager = CredentialManagerBuilder.builder().build();
        TwitchIdentityProvider identityProvider = new TwitchIdentityProvider(clientId, clientSecret, clientRedirectUrl);

        client = TwitchClientBuilder.builder()
                .withCredentialManager(credentialManager)
                .withEnableHelix(true)
                .build();

        this.oAuth2Credential = identityProvider.getAppAccessToken();
        logger.info("OAuth2Credential isCredentialValid: {}", identityProvider.isCredentialValid(oAuth2Credential));
    }

    public TwitchClient getClient() {
        return client;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getBroadcasterId() {
        UserList userList = client.getHelix().getUsers(getAccessToken(), null, Collections.singletonList(channel)).execute();
        if (!userList.getUsers().isEmpty()) {
            return userList.getUsers().get(0).getId();
        }
        return "";
    }



}
