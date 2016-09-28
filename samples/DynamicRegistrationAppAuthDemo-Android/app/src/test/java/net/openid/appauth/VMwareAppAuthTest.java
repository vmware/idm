package net.openid.appauth;

import android.net.Uri;

/**
 * Common class to hold test values.
 */
public class VMwareAppAuthTest {

    public static final Uri TEST_IDP_TOKEN_ENDPOINT =
            Uri.parse("https://testidp.example.com/authorize");
    public static final Uri TEST_IDP_AUTH_ENDPOINT = Uri.parse("https://testidp.example.com/token");
    public static final Uri TEST_IDP_ACTIVATION_ENDPOINT =
            Uri.parse("https://testidp.example.com/activate");
    public static final Uri TEST_IDP_REGISTRATION_ENDPOINT =
            Uri.parse("https://testidp.example.com/register");

    public static final String TEST_ACTIVATION_CODE = "activation-code";
    public static final String TEST_AUTHORIZATION_CODE = "authorization-code";

    public static final Uri TEST_APP_REDIRECT_URI = Uri.parse("test://my-redirect-uri");
    public static final String TEST_APP_SCHEME = "test";

    public static final String TEST_DEVICE_NAME = "device name";
    public static final String TEST_USER_DEVICE_JSON = "{ \"info\" : \"user_device\" }";
    public static final String TEST_APP_TEMPLATE = "app_template";
    public static final String TEST_SCOPE = "scope";
    public static final String TEST_STATE = "state";
    public static final String TEST_CLIENT_ID = "client_id";
    public static final String TEST_CLIENT_SECRET = "the_client_secret";


    public static AuthorizationServiceConfiguration getTestServiceConfig() {
        return new AuthorizationServiceConfiguration(
                TEST_IDP_AUTH_ENDPOINT,
                TEST_IDP_TOKEN_ENDPOINT,
                TEST_IDP_REGISTRATION_ENDPOINT);
    }

    public static DeviceActivationRequest getTestDeviceActivationRequest() {
        return new DeviceActivationRequest.Builder(TEST_IDP_ACTIVATION_ENDPOINT,
                getTestServiceConfig(), TEST_ACTIVATION_CODE).build();

    }

    public static DeviceRegistrationRequest.Builder getTestDeviceRegistrationRequestBuilder() {
        return new DeviceRegistrationRequest.Builder(getTestServiceConfig(), TEST_APP_REDIRECT_URI,
                TEST_DEVICE_NAME, TEST_USER_DEVICE_JSON, TEST_APP_TEMPLATE,
                TEST_IDP_ACTIVATION_ENDPOINT).setState(TEST_STATE);

    }
}
