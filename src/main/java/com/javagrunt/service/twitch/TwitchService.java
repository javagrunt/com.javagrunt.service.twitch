package com.javagrunt.service.twitch;

import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
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
    
    private final String channel;
    
    public TwitchService(@Value("${twitch.client-id}") String clientId,
                         @Value("${twitch.client-secret}") String clientSecret,
                         @Value("${twitch.channel}") String channel) {
        this.channel = channel;
        CredentialManager credentialManager = CredentialManagerBuilder.builder().build();
        MyIdentityProvider identityProvider = new MyIdentityProvider(clientId, clientSecret, "http://localhost:8080");

        client = TwitchClientBuilder.builder()
                .withCredentialManager(credentialManager)
                .withEnableHelix(true)
                .build();
        
        this.oAuth2Credential = identityProvider.getScopedAppAccessToken("channel:read:subscriptions");
        logger.info("OAuth2Credential isCredentialValid: {}", identityProvider.isCredentialValid(oAuth2Credential));
    }
    
    public TwitchClient getClient() {
        return client;
    }
    
    public String getAccessToken() {
        for(String scope : oAuth2Credential.getScopes()) {
            logger.info("OAuth2Credential scope: {}", scope);
        }
        return oAuth2Credential.getAccessToken();
    }
    
    public String getBroadcasterId() {
        UserList userList = client.getHelix().getUsers(getAccessToken(),null, Collections.singletonList(channel)).execute();
        if (!userList.getUsers().isEmpty()) {
            return userList.getUsers().get(0).getId();
        }
        return "";
    }

}
