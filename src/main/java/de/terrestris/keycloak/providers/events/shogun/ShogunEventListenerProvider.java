/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.terrestris.keycloak.providers.events.shogun;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:info@terrestris.de">terrestris GmbH & Co. KG</a>
 *
 * Credits also got to <a href="mailto:jessy.lenne@stadline.com">Jessy Lenne</a> as this implementation is based on
 * https://github.com/jessylenne/keycloak-event-listener-http
 */
public class ShogunEventListenerProvider implements EventListenerProvider {
    private final CloseableHttpClient client = HttpClients.createDefault();
    private Set<EventType> excludedEvents;
    private Set<OperationType> excludedAdminOperations;
    private String serverUri;
    private String username;
    private String password;
    public static final String publisherId = "keycloak";

    public ShogunEventListenerProvider(Set<EventType> excludedEvents, Set<OperationType> excludedAdminOperations, String serverUri, String username, String password) {
        this.excludedEvents = excludedEvents;
        this.excludedAdminOperations = excludedAdminOperations;
        this.serverUri = serverUri;
        this.username = username;
        this.password = password;
    }

    @Override
    public void onEvent(Event event) {
        // Ignore excluded events
        if (excludedEvents != null && excludedEvents.contains(event.getType())) {
            return;
        } else {
            String stringEvent = toString(event);
            // TODO: Replace with logger
            System.out.println(stringEvent);
            this.sendRequest(stringEvent, false);
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Ignore excluded operations
        if (excludedAdminOperations != null && excludedAdminOperations.contains(event.getOperationType())) {
            return;
        } else {
            String stringEvent = toString(event);
            // TODO: Replace with logger
            System.out.println(stringEvent);
            this.sendRequest(stringEvent, true);
        }
    }

    private void sendRequest(String stringEvent, boolean isAdminEvent) {
        try {
            HttpPost httpPost = new HttpPost(this.serverUri);
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
            // TODO: Replace with logger
            System.out.println(response.getStatusLine());
        } catch(Exception e) {
            // TODO: Replace with logger
            System.out.println("UH OH!! " + e.toString());
            e.printStackTrace();
            return;
        }
    }


    private String toString(Event event) {
        StringBuilder sb = new StringBuilder();

        sb.append("{\"type\": \"");
        sb.append(event.getType());
        sb.append("\", \"realmId\": \"");
        sb.append(event.getRealmId());
        sb.append("\", \"clientId\": \"");
        sb.append(event.getClientId());
        sb.append("\", \"userId\": \"");
        sb.append(event.getUserId());
        sb.append("\", \"ipAddress\": \"");
        sb.append(event.getIpAddress());
        sb.append("\"");

        if (event.getError() != null) {
            sb.append(", \"error\": \"");
            sb.append(event.getError());
            sb.append("\"");
        }
        sb.append(", \"details\": {");
        if (event.getDetails() != null) {
            for (Map.Entry<String, String> e : event.getDetails().entrySet()) {
                sb.append("\"");
                sb.append(e.getKey());
                sb.append("\": \"");
                sb.append(e.getValue());
                sb.append("\", ");
            }
        sb.append("}}");
        }

        return sb.toString();
    }


    private String toString(AdminEvent adminEvent) {
        StringBuilder sb = new StringBuilder();

        sb.append("{\"type\": \"");
        sb.append(adminEvent.getOperationType());
        sb.append("\", \"realmId\": \"");
        sb.append(adminEvent.getAuthDetails().getRealmId());
        sb.append("\", \"clientId\": \"");
        sb.append(adminEvent.getAuthDetails().getClientId());
        sb.append("\", \"userId\": \"");
        sb.append(adminEvent.getAuthDetails().getUserId());
        sb.append("\", \"ipAddress\": \"");
        sb.append(adminEvent.getAuthDetails().getIpAddress());
        sb.append("\", \"resourcePath\": \"");
        sb.append(adminEvent.getResourcePath());
        sb.append("\", \"resourceType\": \"");
        sb.append(adminEvent.getResourceType());
        sb.append("\"");

        if (adminEvent.getError() != null) {
            sb.append(", \"error\": \"");
            sb.append(adminEvent.getError());
            sb.append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public void close() {
    }

}
