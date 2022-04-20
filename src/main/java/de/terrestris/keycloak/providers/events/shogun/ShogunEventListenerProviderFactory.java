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

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.*;

/**
 * @author <a href="mailto:info@terrestris.de">terrestris GmbH & Co. KG</a>
 *
 * Credits also got to <a href="mailto:jessy.lenne@stadline.com">Jessy Lenne</a> as this implementation is based on
 * https://github.com/jessylenne/keycloak-event-listener-http
 */
public class ShogunEventListenerProviderFactory implements EventListenerProviderFactory {

    private Set<EventType> eventBlacklist = new HashSet<>(Arrays.asList(
            EventType.LOGIN,
            EventType.LOGOUT,
            EventType.SEND_RESET_PASSWORD
    ));
    private Set<OperationType> excludedAdminOperations;

    private List<String> serverUris;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new ShogunEventListenerProvider(eventBlacklist, excludedAdminOperations, serverUris, null, null);
    }

    @Override
    public void init(Config.Scope config) {
        String[] excludes = config.getArray("exclude-events");
        if (excludes != null) {
            eventBlacklist = new HashSet<>();
            for (String e : excludes) {
                eventBlacklist.add(EventType.valueOf(e));
            }
        }

        String[] excludesOperations = config.getArray("excludesOperations");
        if (excludesOperations != null) {
            excludedAdminOperations = new HashSet<>();
            for (String e : excludesOperations) {
                excludedAdminOperations.add(OperationType.valueOf(e));
            }
        }

        String envUriShogun = System.getenv("SHOGUN_WEBHOOK_URI");
        String envUriInterceptor = System.getenv("INTERCEPTOR_WEBHOOK_URI");
        if (envUriShogun == null) {
            // shogun-boot url is not defined -> use shogun default url, do not notify interceptor
            System.out.println("ServerURI: Using default shogun webhook URI http://shogun-boot:8080/webhooks/keycloak. Configure it with env SHOGUN_WEBHOOK_URI");
            serverUris = Collections.singletonList("http://shogun-boot:8080/webhooks/keycloak");
        } else if (envUriInterceptor != null) {
            // both urls are defined -> notify both hosts
            System.out.println("ServerURI shogun: " + envUriShogun);
            System.out.println("ServerURI interceptor: " + envUriInterceptor);
            serverUris = Arrays.asList(envUriShogun, envUriInterceptor);
        } else {
            // only shogun-boot url is defined -> notify only shogun-boot
            System.out.println("ServerURI shogun: " + envUriShogun);
            serverUris = Collections.singletonList(envUriShogun);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }
    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "shogun-webhook";
    }

}
