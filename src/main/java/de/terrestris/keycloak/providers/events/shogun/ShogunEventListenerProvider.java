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
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:info@terrestris.de">terrestris GmbH & Co. KG</a>
 *
 * Credits also got to <a href="mailto:jessy.lenne@stadline.com">Jessy Lenne</a> as this implementation is based on
 * <a href="https://github.com/jessylenne/keycloak-event-listener-http">this</a> implementation.
 */
public class ShogunEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(ShogunEventListenerProvider.class);

    private final CloseableHttpClient client = HttpClients.createDefault();
    private final Set<EventType> excludedEvents;
    private final Set<OperationType> excludedAdminOperations;
    private final List<String> serverUris;
    private final String username;
    private final String password;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ShogunEventListenerProvider(Set<EventType> excludedEvents, Set<OperationType> excludedAdminOperations, List<String> serverUris, String username, String password) {
        this.excludedEvents = excludedEvents;
        this.excludedAdminOperations = excludedAdminOperations;
        this.serverUris = serverUris;
        this.username = username;
        this.password = password;
    }

    @Override
    public void onEvent(Event event) {
        // Ignore excluded events
        if (excludedEvents == null || !excludedEvents.contains(event.getType())) {
            String stringEvent = toString(event);
            log.debug(stringEvent);
            this.sendRequest(stringEvent, false);
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Ignore excluded operations
        if (excludedAdminOperations == null || !excludedAdminOperations.contains(event.getOperationType())) {
            String stringEvent = toString(event);
            log.debug(stringEvent);
            this.sendRequest(stringEvent, true);
        }
    }

    private void sendRequest(String stringEvent, boolean isAdminEvent) {
        serverUris.forEach(serverUri -> {
            try {
                HttpPost httpPost = new HttpPost(serverUri);
                StringEntity entity = new StringEntity(stringEvent);
                httpPost.setEntity(entity);
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");
                httpPost.setHeader("User-Agent", "KeycloakHttp Bot");

                if (this.username != null && this.password != null) {
                    UsernamePasswordCredentials creds = new UsernamePasswordCredentials(this.username, this.password);
                    httpPost.addHeader(new BasicScheme().authenticate(creds, httpPost, null));
                }

                CloseableHttpResponse response = client.execute(httpPost);

                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IOException("Unexpected code " + response);
                }

                // Get response body
                log.debug(response.getStatusLine());
            } catch(Exception e) {
                log.error("Error while requesting the SHOGun webhook " + e);
                log.trace("Full stack trace: " + e.getMessage());
            }
        });
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
            if (eventError != null && eventError.length() > 0) {
                resultMap.put("error", eventError);
            }
            Map<String, String> details = event.getDetails();
            if (details != null && !details.isEmpty()) {
                resultMap.put("details", details);
            }
            return objectMapper.writeValueAsString(resultMap);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize JSON: " + e.getMessage());
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
            if (eventError != null && eventError.length() > 0) {
                resultMap.put("error", eventError);
            }
            return objectMapper.writeValueAsString(resultMap);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize JSON: " + e.getMessage());
            return "";
        }
    }

    @Override
    public void close() { }

}
