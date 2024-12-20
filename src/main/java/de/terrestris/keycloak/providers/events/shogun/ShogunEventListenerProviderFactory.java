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

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:info@terrestris.de">terrestris GmbH & Co. KG</a>
 * Credits also got to <a href="mailto:jessy.lenne@stadline.com">Jessy Lenne</a> as this implementation is based on
 * https://github.com/jessylenne/keycloak-event-listener-http
 */
public class ShogunEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger log = Logger.getLogger(ShogunEventListenerProviderFactory.class);

    /**
     * User events, configurable via environment variable SHOGUN_WEBHOOK_EVENT_TYPES.
     */
    private List<EventType> enabledEventTypes = List.of();

    /**
     * Admin events, configurable via environment variable SHOGUN_WEBHOOK_OPERATION_TYPES.
     */
    private List<OperationType> enabledOperationTypes = List.of(
        OperationType.CREATE,
        OperationType.DELETE
    );

    /**
     * Admin resource types, configurable via environment variable SHOGUN_WEBHOOK_RESOURCE_TYPES.
     */
    private List<ResourceType> enabledResourceTypes = List.of(
        ResourceType.USER,
        ResourceType.GROUP,
        ResourceType.GROUP_MEMBERSHIP
    );

    /**
     * Whether to use authentication for the webhook or not, configurable via environment variable SHOGUN_WEBHOOK_USE_AUTH.
     */
    private Boolean useAuth = true;

    /**
     * The list of target URIs. Configurable via environment variable SHOGUN_WEBHOOK_URIS.
     */
    private List<String> serverUris = List.of("http://shogun-boot:8080/webhooks/keycloak");

    /**
     * The id of the client the target URI is defined in. Configurable via environment variable SHOGUN_WEBHOOK_CLIENT_ID.
     */
    private String clientId = "shogun-boot";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new ShogunEventListenerProvider(session, serverUris, clientId, enabledEventTypes,
            enabledOperationTypes, enabledResourceTypes, useAuth);
    }

    @Override
    public void init(Config.Scope config) {
        String eventTypes = System.getenv("SHOGUN_WEBHOOK_EVENT_TYPES");
        if (eventTypes != null) {
            log.info("Picked up environment variable SHOGUN_WEBHOOK_EVENT_TYPES: " + eventTypes);

            this.enabledEventTypes = Arrays.stream(eventTypes.split(","))
                .map(EventType::valueOf)
                .collect(Collectors.toList());
        } else {
            log.info("Environment variable SHOGUN_WEBHOOK_EVENT_TYPES not set, using the default: " + this.enabledEventTypes);
        }

        String operationTypes = System.getenv("SHOGUN_WEBHOOK_OPERATION_TYPES");
        if (operationTypes != null) {
            log.info("Picked up environment variable SHOGUN_WEBHOOK_OPERATION_TYPES: " + operationTypes);

            this.enabledOperationTypes = Arrays.stream(operationTypes.split(","))
                .map(OperationType::valueOf)
                .collect(Collectors.toList());
        } else {
            log.info("Environment variable SHOGUN_WEBHOOK_OPERATION_TYPES not set, using the default: " + this.enabledOperationTypes);
        }

        String resourceTypes = System.getenv("SHOGUN_WEBHOOK_RESOURCE_TYPES");
        if (resourceTypes != null) {
            log.info("Picked up environment variable SHOGUN_WEBHOOK_RESOURCE_TYPES: " + resourceTypes);

            this.enabledResourceTypes = Arrays.stream(resourceTypes.split(","))
                .map(ResourceType::valueOf)
                .collect(Collectors.toList());
        } else {
            log.info("Environment variable SHOGUN_WEBHOOK_RESOURCE_TYPES not set, using the default: " + this.enabledResourceTypes);
        }

        String webhookUriString = System.getenv("SHOGUN_WEBHOOK_URIS");
        if (webhookUriString != null) {
            log.info("Picked up environment variable SHOGUN_WEBHOOK_URIS: " + webhookUriString);

            this.serverUris = Arrays.stream(webhookUriString.split(",")).collect(Collectors.toList());
        } else {
            log.info("Environment variable SHOGUN_WEBHOOK_URIS not set, using the default: " + this.serverUris);
        }

        String shogunClientId = System.getenv("SHOGUN_WEBHOOK_CLIENT_ID");
        if (shogunClientId != null) {
            log.info("Picked up environment variable SHOGUN_WEBHOOK_CLIENT_ID: " + shogunClientId);
            this.clientId = shogunClientId;
        } else {
            log.info("Environment variable SHOGUN_WEBHOOK_CLIENT_ID not set, using the default: " + this.clientId);
        }

        String shogunUseAuth = System.getenv("SHOGUN_WEBHOOK_USE_AUTH");
        if (shogunUseAuth != null) {
            log.info("Picked up environment variable SHOGUN_WEBHOOK_USE_AUTH: " + shogunUseAuth);
            this.useAuth = Boolean.parseBoolean(shogunUseAuth);
        } else {
            log.info("Environment variable SHOGUN_WEBHOOK_USE_AUTH not set, using the default: " + this.useAuth);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) { }

    @Override
    public void close() { }

    @Override
    public String getId() {
        return "shogun-webhook";
    }

}
