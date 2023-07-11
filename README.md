# Keycloak Event Listener SHOGun

A Keycloak SPI that publishes events (except EventType.LOGIN, EventType.LOGOUT and EventType.SEND_RESET_PASSWORD) 
to the [SHOGun Webhook](https://github.com/terrestris/shogun/blob/main/shogun-lib/src/main/java/de/terrestris/shogun/lib/controller/WebhookController.java).

A (largely) adaptation of https://github.com/jessylenne/keycloak-event-listener-http SPI.

Currently the extension has been tested against Keycloak version 21.

# Requirements

To build the extension the following tools are required:

* Java 17
* mvn

# Build

To build the extension just execute:

```bash
mvn clean install
```

# Installation

Copy `target/event-listener-shogun-jar-with-dependencies.jar` to `{KEYCLOAK_HOME}/providers`.

If you are working in a [Docker environment](https://quay.io/repository/keycloak/keycloak) you might want to mount 
the `/opt/keycloak/providers` folder as a volume and copy the target to the host directory instead, e.g.:

```yaml
(…)
volumes:
  - ./my-keycloak/providers/event-listener-shogun-jar-with-dependencies.jar:/opt/keycloak/providers/event-listener-shogun-jar-with-dependencies.jar
  (…)
```

# Configuration

If working in a project environment, use the environment variable `SHOGUN_WEBHOOK_URIS` to configure a list of
webhook URIs. Multiple entries can be separated by `;`, e.g.:

```
http://my-app:8080/webhooks/keycloak;http://my-interceptor:8080/webhooks/keycloak
```

# Registration in Keycloak

1. Go to the "Realm settings" in your Keycloak realm.
2. Select the "Events" tab and add `shogun-webhook` to the "Event listeners".
3. Save the settings.
4. Add/Update/Delete a user/group, your webhook should be called.
