# Keycloak Event Listener SHOGun

A Keycloak Service Provider Interface (SPI) implementation listening to a selection of Keycloak events and notifying
a SHOGun instance about it via the appropriate [webhook](https://github.com/terrestris/shogun/blob/main/shogun-lib/src/main/java/de/terrestris/shogun/lib/controller/WebhookController.java).

A (largely) adaptation of https://github.com/jessylenne/keycloak-event-listener-http SPI.

## Compatibility

Currently, the extension has been tested against Keycloak version 25, but it should also work with older (and
probably newer) versions.

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

The plugin can be configured using a set of environment variables:

<table>
  <tr>
    <th>Environment Variable</th>
    <th>Description</th>
    <th>Default</th>
  </tr>
  <tr>
    <td>
      <code>
        SHOGUN_WEBHOOK_EVENT_TYPES
      </code>
    </td>
    <td>
      A comma-separated list of user event types to listen to. See
      <a href="https://github.com/keycloak/keycloak/blob/main/server-spi-private/src/main/java/org/keycloak/events/EventType.java">here</a>
      for a list of available types.
    </td>
    <td>
      <code>
        -
      </code>
    </td>
  </tr>
  <tr>
    <td>
      <code>
        SHOGUN_WEBHOOK_OPERATION_TYPES
      </code>
    </td>
    <td>
      A comma-separated list of admin operation types to listen to. See
      <a href="https://github.com/keycloak/keycloak/blob/main/server-spi-private/src/main/java/org/keycloak/events/admin/OperationType.java">here</a>
      for a list of available types.
    </td>
    <td>
      <code>
        CREATE,DELETE
      </code>
    </td>
  </tr>
  <tr>
    <td>
      <code>
        SHOGUN_WEBHOOK_RESOURCE_TYPES
      </code>
    </td>
    <td>
      A comma-separated list of admin resource types to listen to. See
      <a href="https://github.com/keycloak/keycloak/blob/main/server-spi-private/src/main/java/org/keycloak/events/admin/ResourceType.java">here</a>
      for a list of available types.
    </td>
    <td>
      <code>
        USER,GROUP,GROUP_MEMBERSHIP
      </code>
    </td>
  </tr>
  <tr>
    <td>
      <code>
        SHOGUN_WEBHOOK_URIS
      </code>
    </td>
    <td>
      A comma-separated list of SHOGun webhook URIs to notify. By default, the plugin expects a single SHOGun
      instance running at <code>http://shogun-boot:8080/webhooks/keycloak</code>. This path can be adjusted if the
      instance is available at a different host (e.g. <code>http://my-shogun-boot:8080/webhooks/keycloak</code>) and/or
      if multiple instances of SHOGun should be notified, e.g. in a clustered environment.
   </td>
    <td>
        <code>
          http://shogun-boot:8080/webhooks/keycloak
        </code>
    </td>
  </tr>
  <tr>
    <td>
      <code>
        SHOGUN_WEBHOOK_CLIENT_ID
      </code>
    </td>
    <td>
      The client ID to use for the SHOGun webhook.
    </td>
    <td>
      <code>
        shogun-boot
      </code>
    </td>
  </tr>
  <tr>
    <td>
      <code>
        SHOGUN_WEBHOOK_USE_AUTH
      </code>
    </td>
    <td>
      Whether to use authentication for the webhook or not.
    </td>
    <td>
      <code>
        true
      </code>
    </td>
  </tr>
</table>

### Registration in Keycloak

1. Go to the "Realm settings" in your Keycloak realm.
2. Select the "Events" tab and add `shogun-webhook` to the "Event listeners".
3. Save the settings.
4. Add/Update/Delete a user/group, your webhook should be called.
