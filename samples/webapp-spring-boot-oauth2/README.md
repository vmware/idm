Log in with VMware Identity Manager
=========================================

This demo application shows the use of Spring Boot and its built-in OAuth2 capabilities to
let a user authenticate with VMware Identity Manager™ and then use the access token to
 access VMware Identity Manager resources (like the user information).

This application is based on the [Spring Demo application](https://spring.io/guides/tutorials/spring-boot-oauth2/#_social_login_github)
and has been modified to integrate Google and VMware Identity Manager as an authorization server.

## Building the project

### Prerequisites

- You need a [VMware Identity Manager organization](http://www.air-watch.com/vmware-identity-manager-free-trial), like https://dev.vmwareidentity.asia, where you have __admin__ access.
- The project requires JDK 8

### Building from IDE

* Clone this project.
* Then import the root folder. You can run the main class `SocialApplication`.

### Building from the Command line

You can run the app by using:
`$ mvn spring-boot:run`

or by building the jar file and running it with `mvn package` and `java -jar target/*.jar` (per the Spring Boot docs and other available documentation).

### Configure the Demo App

* Create an OAuth2.0 client in the VMware Identity Manager admin console.
Go to `Catalog` -> `Settings` -> `Remote App Access`. Click on `Create Client`.
The _redirect URI_ must be set to `http://localhost:8080/login/vmware`

* Edit the file `./src/main/resources/application.yml` to set your own VMware Identity Manager organization URL in the defined endpoints,
and the OAuth2 client id and client secret as they were defined in the previous step:

```yaml
vmware:
  client:
    accessTokenUri: <your organization URL>/SAAS/auth/oauthtoken
    userAuthorizationUri: <your organization URL>/SAAS/auth/oauth2/authorize
    clientId: <client id>
    clientSecret: <client secret>
  resource:
    userInfoUri: <your organization URL>/SAAS/jersey/manager/api/userinfo
```

### Test the application

The web application will be available on `http://localhost:8080`. Click on "Login with WorkspaceONE" to start the OAuth2 flow.
You can use the default values and log in to the demo VMware Identity Manager system using:

* Username: `userN`, where N=1..10
* Password: `vmware`
