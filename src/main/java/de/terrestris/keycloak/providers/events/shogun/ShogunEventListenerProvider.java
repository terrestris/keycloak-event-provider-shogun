/*
 * Keycloak Event Listener SHOGun, https://github.com/terrestris/keycloak-event-listener-shogun
 *
 * Copyright © 2020-present terrestris GmbH & Co. KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.terrestris.keycloak.providers.events.shogun;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessTokenResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:info@terrestris.de">terrestris GmbH & Co. KG</a>
 *
 * Credits also got to <a href="mailto:jessy.lenne@stadline.com">Jessy Lenne</a> as this implementation is based on
 * <a href="https://github.com/jessylenne/keycloak-event-listener-http">this</a> implementation.
 */
public class ShogunEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(ShogunEventListenerProvider.class);

    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final KeycloakSession session;

    private final List<String> serverUris;

    private final String clientId;

    private final List<EventType> enabledEventTypes;

    private final List<OperationType> enabledOperationTypes;

    private final List<ResourceType> enabledResourceTypes;

    private final Boolean useAuth;

    public ShogunEventListenerProvider(KeycloakSession session, List<String> serverUris, String clientId,
            List<EventType> enabledEventTypes, List<OperationType> enabledOperationTypes, List<ResourceType> enabledResourceTypes,
            Boolean useAuth) {
        this.session = session;
        this.serverUris = serverUris;
        this.clientId = clientId;
        this.enabledEventTypes = enabledEventTypes;
        this.enabledOperationTypes = enabledOperationTypes;
        this.enabledResourceTypes = enabledResourceTypes;
        this.useAuth = useAuth;
    }

    @Override
    public void onEvent(Event event) {
        log.debug("Received user event: " + event.getType());

        // Continue for observable event types only.
        if (!enabledEventTypes.contains(event.getType())) {
            log.debug("Ignoring event");
            return;
        }

        String stringEvent = toString(event);

        log.debug("Forwarding event");

        this.sendRequest(stringEvent, false);
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        log.debug("Received admin event: " + event.getOperationType() + " on " + event.getResourceType());

        // Continue for observable admin operations / events only.
        if (!(enabledOperationTypes.contains(event.getOperationType()) && enabledResourceTypes.contains(event.getResourceType()))) {
            log.debug("Ignoring event");
            return;
        }

        String stringEvent = toString(event);

        log.debug("Forwarding event");

        this.sendRequest(stringEvent, true);
    }

    private void sendRequest(String stringEvent, boolean isAdminEvent) {
        String accessToken = null;

        if (useAuth) {
            try {
                accessToken = getAccessToken();
            } catch (Exception e) {
                log.error("Error while getting an access token: " + e.getMessage());
                log.debug("Full stack trace ", e);
            }
        }

        String finalAccessToken = accessToken;
        serverUris.forEach(serverUri -> {
            try {
                sendHttpRequest(serverUri, stringEvent, finalAccessToken);
            } catch (Exception e) {
                log.error("Error while requesting the webhook: " + e.getMessage());
                log.debug("Full stack trace ", e);
            }
        });
    }

    private void sendHttpRequest(String uri, String event, String accessToken) throws Exception {
        HttpPost httpPost = new HttpPost(uri);

        httpPost.setHeader(HttpHeaders.ACCEPT, "application/json");
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        httpPost.setHeader(HttpHeaders.USER_AGENT, "KeycloakHttp Bot");

        StringEntity entity = new StringEntity(event);
        httpPost.setEntity(entity);

        if (accessToken != null) {
            httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new Exception("Unexpected status code: " + response);
            }
        }
    }

    /**
     * Gets a service account access token for the current realm and {@link #clientId}.
     *
     * @return The access token.
     * @throws Exception If the access token could not be retrieved.
     */
    private String getAccessToken() throws Exception {
        // Get the realm from the context.
        RealmModel realm = session.getContext().getRealm();

        // Check if the client exists.
        ClientModel client = session.clients().getClientByClientId(realm, clientId);
        if (client == null) {
            throw new Exception("Could not find client '" + clientId + "'");
        }

        // Check if client authentication is enabled for the client.
        if (client.isPublicClient()) {
            throw new Exception("Client '" + clientId + "' must not be a public client");
        }

        // Get the secret for the client.
        String clientSecret = client.getSecret();
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new Exception("No secret available for client '" + clientId + "'");
        }

        // Get the service account user for the client.
        UserModel serviceAccount = session.users().getServiceAccount(client);
        if (serviceAccount == null) {
            throw new Exception("Service account not enabled for client '" + clientId + "'");
        }

        URI authServerUrl = session.getContext().getAuthServerUrl();

        HttpPost httpPost = new HttpPost( authServerUrl + "/realms/" + realm.getName() + "/protocol/openid-connect/token");

        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

        final List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", "client_credentials"));
        params.add(new BasicNameValuePair("client_id", clientId));
        params.add(new BasicNameValuePair("client_secret", clientSecret));
        httpPost.setEntity(new UrlEncodedFormEntity(params));

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            String responseBody = EntityUtils.toString(response.getEntity());

            AccessTokenResponse result = objectMapper.readValue(responseBody, AccessTokenResponse.class);

            return result.getToken();
        }
    }

    private String toString(Event event) {
        try {
            HashMap<String, Object> resultMap = new HashMap<>();
            resultMap.put("type", event.getType());
            resultMap.put("realmId", event.getRealmId());
            resultMap.put("clientId", event.getClientId());
            resultMap.put("userId", event.getUserId());
            resultMap.put("ipAddress", event.getIpAddress());

            String eventError = event.getError();
            if (eventError != null && !eventError.isEmpty()) {
                resultMap.put("error", eventError);
            }
            Map<String, String> details = event.getDetails();
            if (details != null && !details.isEmpty()) {
                resultMap.put("details", details);
            }
            return objectMapper.writeValueAsString(resultMap);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize JSON: " + e.getMessage());
            log.debug("Full stack trace: ", e);
            return "";
        }
    }

    private String toString(AdminEvent adminEvent) {
        try {
            HashMap<String, Object> resultMap = new HashMap<>();
            resultMap.put("type", adminEvent.getOperationType());
            resultMap.put("realmId", adminEvent.getAuthDetails().getRealmId());
            resultMap.put("clientId", adminEvent.getAuthDetails().getClientId());
            resultMap.put("userId", adminEvent.getAuthDetails().getUserId());
            resultMap.put("ipAddress", adminEvent.getAuthDetails().getIpAddress());
            resultMap.put("resourcePath", adminEvent.getResourcePath());
            resultMap.put("resourceType", adminEvent.getResourceType());
            String eventError = adminEvent.getError();
            if (eventError != null && !eventError.isEmpty()) {
                resultMap.put("error", eventError);
            }
            return objectMapper.writeValueAsString(resultMap);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize JSON: " + e.getMessage());
            log.debug("Full stack trace: ", e);
            return "";
        }
    }

    @Override
    public void close() { }

}
