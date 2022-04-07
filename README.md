# keycloak-event-listener-shogun

A Keycloak SPI that publishes events to the SHOGun Webhook.
A (largely) adaptation of https://github.com/jessylenne/keycloak-event-listener-http SPI

# Build
If working in a project environment: Use the environment variable `SHOGUN_WEBHOOK_URI` to configure the webhook uri.

```
mvn clean install
```

# Deploy

* Copy target/event-listener-shogun-jar-with-dependencies.jar to {KEYCLOAK_HOME}/standalone/deployments

If you are working in a docker environment you might want to mount the `deployments` folder as a volume and copy the
target to the host directory instead.

# Use

1. Go to the "Manage -> Events" in your Keycloak realm.
2. Select the "Config" tab and add `shogun-webhook` to the "Event listeners".
3. Save the settings
4. Add/Update/Delete a user/group, your webhook should be called.

Request example
```
{
    "type": "REGISTER",
    "realmId": "myrealm",
    "clientId": "heva",
    "userId": "bcee5034-c65f-4d7c-9036-034042f0a054",
    "ipAddress": "172.21.0.1", 
    "details": {
        "auth_method": "openid-connect",
        "auth_type": "code",
        "register_method": "form",
        "redirect_uri": "http://nginx:8000/",
        "code_id": "98bfe6b2-b8c2-4b82-bc85-9cd033324ec9",
        "email": "fake.email@service.com",
        "username": "username"
    }
}
```