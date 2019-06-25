# VMware Identity Manager

Identity Manager is an Identity as a Service (IDaaS) offering, providing application provisioning, self-service catalog, conditional access controls and Single Sign-On (SSO) for SaaS, web, cloud and native mobile applications.

## Overview

If you don't know where to start, read the introduction on existing protocols to achieve Single-Sign On with your application:

* [Choosing the right authentication protocol](https://github.com/vmware/idm/wiki/Choosing-The-Right-Auth)

## How-To Guides

The wiki contains a lot of documentation to help you achieve your task. Whether you want to have end-users login to your web application or mobile application, or have your service be able to call VMware Identity Manager API, read through the following guides:

* [Integrating your web app with OAuth2.0 (end users log in)](https://github.com/vmware/idm/wiki/Integrating-Webapp-with-OAuth2)
* [Integrating your mobile app with OAuth2.0 (end users log in)](https://github.com/vmware/idm/wiki/Single-sign-on-for-Mobile)
* [Integrating your backend app with OAuth2.0 (service to service)](https://github.com/vmware/idm/wiki/Integrating-Client-Credentials-app-with-OAuth2)
* [Validate the tokens](https://github.com/vmware/idm/wiki/Validating-Access-or-ID-Token)
* [Managing Users and Groups using SCIM API](https://github.com/vmware/idm/wiki/SCIM-guide)


## Samples
[![Build Status](https://travis-ci.org/vmware/idm.svg?branch=master)](https://travis-ci.org/vmware/idm/)
Sample applications are provided - as is - to demonstrate how to integrate your application with VMware Identity Manager:

 * [Android application using the AppAuth library](https://github.com/vmware/idm/tree/master/samples/DynamicRegistrationAppAuthDemo-Android)
 * [SpringBoot web application using OAuth2.0](https://github.com/vmware/idm/tree/master/samples/webapp-spring-boot-oauth2)
 * [SpringBoot web application acting as a resource server using OAuth2.0](https://github.com/vmware/idm/tree/master/samples/webapp-spring-boot-oauth2-resource-server)
 * [SpringBoot web application using SAML2](https://github.com/vmware/idm/tree/master/samples/webapp-spring-boot-saml2)

## Resources
* [Reference API File](https://github.com/vmware/idm/blob/master/apidocs/swagger.json)
* [Identity Manager API Documentation](https://vmware.github.io/idm/api-docs)
