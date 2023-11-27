package com.javagrunt.service.twitch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.credentialmanager.identityprovider.OAuth2IdentityProvider;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MyIdentityProvider extends OAuth2IdentityProvider {
    private final Logger logger = LoggerFactory.getLogger(MyIdentityProvider.class);
    private static final Logger log = LoggerFactory.getLogger(TwitchIdentityProvider.class);
    public static final String PROVIDER_NAME = "twitch";

    public MyIdentityProvider(String clientId, String clientSecret, String redirectUrl) {
        super("twitch", "oauth2", clientId, clientSecret, "https://id.twitch.tv/oauth2/authorize", "https://id.twitch.tv/oauth2/token", redirectUrl);
        this.tokenEndpointPostType = "QUERY";
        this.scopeSeperator = "+";
    }

    public Optional<Boolean> isCredentialValid(OAuth2Credential credential) {
        if (credential != null && credential.getAccessToken() != null && !credential.getAccessToken().isEmpty()) {
            Request request = (new Request.Builder()).url("https://id.twitch.tv/oauth2/validate").header("Authorization", "OAuth " + credential.getAccessToken()).build();

            try {
                Response response = this.httpClient.newCall(request).execute();

                Optional var4;
                label73: {
                    label79: {
                        try {
                            if (response.isSuccessful()) {
                                var4 = Optional.of(true);
                                break label79;
                            }

                            if (response.code() >= 400 && response.code() < 500) {
                                var4 = Optional.of(false);
                                break label73;
                            }
                        } catch (Throwable var7) {
                            if (response != null) {
                                try {
                                    response.close();
                                } catch (Throwable var6) {
                                    var7.addSuppressed(var6);
                                }
                            }

                            throw var7;
                        }

                        if (response != null) {
                            response.close();
                        }

                        return Optional.empty();
                    }

                    if (response != null) {
                        response.close();
                    }

                    return var4;
                }

                if (response != null) {
                    response.close();
                }

                return var4;
            } catch (Exception var8) {
                return Optional.empty();
            }
        } else {
            return Optional.of(false);
        }
    }

    public Optional<OAuth2Credential> getAdditionalCredentialInformation(OAuth2Credential credential) {
        try {
            Request request = (new Request.Builder()).url("https://id.twitch.tv/oauth2/validate").header("Authorization", "OAuth " + credential.getAccessToken()).build();
            Response response = this.httpClient.newCall(request).execute();
            String responseBody = response.body().string();
            if (response.isSuccessful()) {
                ObjectMapper objectMapper = new ObjectMapper();
                HashMap<String, Object> tokenInfo = (HashMap)objectMapper.readValue(responseBody, new TypeReference<HashMap<String, Object>>() {
                });
                String userId = (String)tokenInfo.get("user_id");
                String userName = (String)tokenInfo.get("login");
                List<String> scopes = (List)tokenInfo.get("scopes");
                int expiresIn = (Integer)tokenInfo.get("expires_in");
                OAuth2Credential newCredential = new OAuth2Credential(credential.getIdentityProvider(), credential.getAccessToken(), credential.getRefreshToken(), userId, userName, expiresIn, scopes);
                newCredential.getContext().put("client_id", tokenInfo.get("client_id"));
                return Optional.of(newCredential);
            } else {
                throw new RuntimeException("Request Failed! Code: " + response.code() + " - " + responseBody);
            }
        } catch (Exception var12) {
            return Optional.empty();
        }
    }

    public boolean revokeCredential(OAuth2Credential credential) {
        HttpUrl url = HttpUrl.parse("https://id.twitch.tv/oauth2/revoke").newBuilder().addQueryParameter("client_id", (String)credential.getContext().getOrDefault("client_id", this.clientId)).addQueryParameter("token", credential.getAccessToken()).build();
        Request request = (new Request.Builder()).url(url).post(RequestBody.create("", (MediaType)null)).build();

        try {
            Response response = this.httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                return true;
            }

            assert response.body() != null;
            log.warn("Unable to revoke access token! Code: " + response.code() + " - " + response.body().string());
        } catch (Exception var5) {
        }

        return false;
    }
    public OAuth2Credential getScopedAppAccessToken(String scope) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", this.clientId);
        parameters.put("client_secret", this.clientSecret);
        parameters.put("grant_type", "client_credentials");
        if (StringUtils.isNotBlank(scope)) {
            parameters.put("scope", scope);
        }

        try {
            Request request = getTokenRequest(parameters, Collections.emptyMap());
            try (Response response = httpClient.newCall(request).execute()) {
                assert response.body() != null;
                String responseBody = response.body().string();
                if (response.isSuccessful()) {
                    Map<String, Object> resultMap = OBJECTMAPPER.readValue(responseBody, new TypeReference<HashMap<String, Object>>() {});
                    logger.info("getCredentialByClientCredentials scope: {}", resultMap.get("scope"));
                    ArrayList<String> scopes;
                    scopes = (ArrayList<String>)resultMap.get("scope");
                    return new OAuth2Credential(this.providerName, (String) resultMap.get("access_token"), (String) resultMap.get("refresh_token"), null, null, (Integer) resultMap.get("expires_in"), scopes);
                } else {
                    throw new RuntimeException("getCredentialByClientCredentials request failed! " + response.code() + ": " + responseBody);
                }
            }

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    Request getTokenRequest(Map<String, String> parameters, Map<String, String> headers) {
        Request request;

        switch (tokenEndpointPostType.toUpperCase()) {
            case "QUERY":
                HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(this.tokenUrl)).newBuilder();
                parameters.forEach(urlBuilder::addQueryParameter);

                request = new Request.Builder()
                        .url(urlBuilder.build().toString())
                        .post(RequestBody.create(new byte[]{}, null))
                        .headers(Headers.of(headers))
                        .build();
                break;
            case "BODY":
                FormBody.Builder requestBody = new FormBody.Builder();
                parameters.forEach(requestBody::add);

                request = new Request.Builder()
                        .url(this.tokenUrl)
                        .post(requestBody.build())
                        .headers(Headers.of(headers))
                        .build();
                break;
            default:
                throw new UnsupportedOperationException("Unknown tokenEndpointPostType: " + tokenEndpointPostType);
        }

        return request;
    }
}
