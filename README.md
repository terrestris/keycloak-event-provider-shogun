# Keycloak Event Listener SHOGun

A Keycloak Service Provider Interface (SPI) implementation listening to a selection of Keycloak events (all events except 
`EventType.LOGIN`, `EventType.LOGOUT` and `EventType.SEND_RESET_PASSWORD`) and notifying a SHOGun instance 
about it via the appropriate [Webhook](https://github.com/terrestris/shogun/blob/main/shogun-lib/src/main/java/de/terrestris/shogun/lib/controller/WebhookController.java).

A (largely) adaptation of https://github.com/jessylenne/keycloak-event-listener-http SPI.

## Compatibility

Currently, the extension has been tested against Keycloak version 21, but it should also work with older versions.

## Development

### Requirements

To build the extension the following tools are required:

- Java 17
- mvn

### Build

To build the extension just execute:

```bash
mvn clean package
```

## Usage

### Installation

Copy the `target/event-listener-shogun-jar-with-dependencies.jar` (available after it has been built) file to your
`{KEYCLOAK_HOME}/providers` directory.

If you are working in a [Docker environment](https://quay.io/repository/keycloak/keycloak) you might want to mount 
the `/opt/keycloak/providers` folder as a volume and copy the target to the host directory instead, e.g.:

```yaml
(…)
volumes:
  - ./my-keycloak/providers/event-listener-shogun-jar-with-dependencies.jar:/opt/keycloak/providers/event-listener-shogun-jar-with-dependencies.jar
(…)
```

### Configuration

By default, the plugin expects a single SHOGun instance running at `http://shogun-boot:8080/`. This path can be adjusted
if the instance is available at a different host (e.g. `http://my-shogun-boot:8080/`) and/or if multiple instances of
SHOGun should be notified, e.g. in a clustered environment.
If needed, set the environment variable `SHOGUN_WEBHOOK_URIS` to your host(s), multiple entries can be separated by `;`:

```
SHOGUN_WEBHOOK_URIS=http://http://my-shogun-boot:8080/webhooks/keycloak;http://http://my-shogun-interceptor:8080/webhooks/keycloak
```

### Registration in Keycloak

1. Go to the "Realm settings" in your Keycloak realm.
2. Select the "Events" tab and add `shogun-webhook` to the "Event listeners".
3. Save the settings.
4. Add/Update/Delete a user/group, your webhook should be called.
