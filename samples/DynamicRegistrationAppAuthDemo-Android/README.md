# Mobile SSO Sample Application For Android

## Overview

This application is an Android native demo application to show you how to achieve mobile single sign-on using the [AppAuth library](https://github.com/openid/AppAuth-Android) with VMware Identity Manager as your Authorization Server.
It is based on the [demo application](https://github.com/openid/AppAuth-Android) provided by the OpenID Foundation.

VMware Identity Manager supports the OAuth2.0 Authorization Code Grant for mobile apps but requires each application instance to register a unique __Client ID__ and __Client Secret__ for additional security.
This application shows how to set up mobile single sign-on using the AppAuth standard library.

## Building the project

### Prerequisites

- You need a [VMware Identity Manager organization](http://www.air-watch.com/vmware-identity-manager-free-trial), like https://dev.vmwareidentity.asia, where you have __admin__ access.
- The project requires the Android SDK for API level 23 (Marshmallow) to build, though the produced binaries only require API level 16 (Jellybean) to be used.

### Building from Android Studio

* Clone this project.
* Then in AndroidStudio, use File -> New -> Import project. Select the root folder.

### Building from the Command line

DynamicRegistrationAppAuthDemo for Android uses Gradle as its build system. In order to build the library and app binaries, run `./gradlew assemble`
The demo app is output to _app/build/outputs/apk_. In order to run the tests and code analysis, run `./gradlew check`.

### Configure the Demo App

You will need to edit the file `./app/src/main/res/values/idp_configs.xml` and edit the following values:

* the organization URL: this is the full URL of your VMware Identity Manager organization:

```xml
    <!-- Your VMware Identity Manager organization URL. -->
    <string name="vmware_url" translatable="false">https://dev.vmwareidentity.asia</string>
```

* the application template and redirection URI: this is the template you defined in the VMware Identity Manager admin console under
`Catalog` -> `Settings` -> `Remote App Access`. Click on `Templates` and then `Create Template`.
The `vmware_auth_redirect_scheme` and `vmware_auth_redirect_uri` must match what you defined in the previous application template.

```xml
    <!-- The information below must match the template information defined in the admin interface. -->
    <string name="vmware_template" translatable="false">VMware-AppAuth-Samples-Template</string>
    <string name="vmware_auth_redirect_scheme" translatable="false">com.vmware.idm.samples.mobilesso</string>
    <string name="vmware_auth_redirect_uri" translatable="false">com.vmware.idm.samples.mobilesso://oauth2redirect</string>
```

### Test the application

You can use the default values and log in to the demo VMware Identity Manager system using:

* Username: `userN`, where N=1..10
* Password: `vmware`
