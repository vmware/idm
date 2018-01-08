package com.vmware.idm.samples.oauth2;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

/**
 * Unit tests for our token validation class.
 */
@RunWith(SpringRunner.class)
public class VMwareValidateTokenServicesTest {

    private static final String AN_ACCESS_TOKEN_STRING = "an-access-token";
    private static final String VALIDATE_TOKEN_URL = "https://test.test.test/SAAS/my/url/to/validate";
    private Calendar cal = Calendar.getInstance();

    @MockBean
    private JwtTokenStore mockTokenStore;
    @MockBean
    private RestOperations mockRestTemplate;

    private VMwareValidateTokenServices tokenServices;
    private OAuth2AccessToken aValidAccessToken;
    private org.springframework.security.oauth2.provider.OAuth2Authentication aValidOAuth2Authentication;
    private OAuth2Request aValidOAuth2Request;
    private UsernamePasswordAuthenticationToken aValidAuth;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void throwsExceptionWithSpecificType() {
        thrown.expect(NullPointerException.class);
        throw new NullPointerException();
    }

    @Before
    public void setUp() throws URISyntaxException {
        tokenServices = new VMwareValidateTokenServices(VALIDATE_TOKEN_URL, mockTokenStore);
        aValidAccessToken = aValidOAuth2AccessToken();
        aValidOAuth2Request = new OAuth2Request(null, null, null, false, null, new HashSet<>(Arrays.asList("test-resource-id")), null, null, null);
        aValidAuth = new UsernamePasswordAuthenticationToken("test-username", null);
        aValidOAuth2Authentication = new OAuth2Authentication(aValidOAuth2Request, aValidAuth);
        cal.setTime(new Date());
    }

    @Test
    public void testLoadAuthenticationCanValidateLocally() throws Exception {
        willSuccessfullyDecodeAccessToken(aValidAccessToken);
        willSuccessfullyReadAuthInfo();

        OAuth2Authentication auth = tokenServices.loadAuthentication(AN_ACCESS_TOKEN_STRING);
        assertAuthIsValid(auth);
    }

    @Test
    public void testCreationFailsIfValidationUrlIsInvalid() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Can not infer expected issuer URL from validation URL");
        new VMwareValidateTokenServices("https://an.invalid.url.without.SAAS.context", mockTokenStore);
    }

    @Test
    public void testLoadAuthenticationFailsIfAuthInfoCanNotBeExtracted() throws Exception {
        willSuccessfullyDecodeAccessToken(aValidAccessToken);
        expectInvalidToken("Invalid access token, could not extract authentication information: " + AN_ACCESS_TOKEN_STRING);

        given(mockTokenStore.readAuthentication(AN_ACCESS_TOKEN_STRING)).willReturn(null);
        tokenServices.loadAuthentication(AN_ACCESS_TOKEN_STRING);
    }

    @Test
    public void testLoadAuthenticationCanValidateRemotely() throws Exception {
        willSuccessfullyDecodeAccessToken(aValidAccessToken);
        willSuccessfullyReadAuthInfo();
        testLocalValidationWithResult("true", mockRestTemplate);
    }

    @Test
    public void testLoadAuthenticationFailsIfRemoteValidationFails() throws Exception {
        willSuccessfullyDecodeAccessToken(aValidAccessToken);
        expectInvalidToken("The token is not valid: " + AN_ACCESS_TOKEN_STRING);
        testLocalValidationWithResult("false", mockRestTemplate);
    }

    @Test
    public void testLoadAuthenticationFailsIfRemoteValidationUrlIsFailing() throws Exception {
        willSuccessfullyDecodeAccessToken(aValidAccessToken);
        given(mockRestTemplate.getForObject(any(URI.class), any())).willThrow(new RestClientException("test"));
        thrown.expect(RestClientException.class);
        thrown.expectMessage("test");

        tokenServices.setValidateLocally(false);
        tokenServices.setRestTemplate(mockRestTemplate);
        tokenServices.loadAuthentication(AN_ACCESS_TOKEN_STRING);
    }

    @Test
    public void testLoadAuthenticationDoesNotValidatesTokenWithMissingIssuer() throws Exception {
        DefaultOAuth2AccessToken token = aValidOAuth2AccessToken();
        token.getAdditionalInformation().remove("iss");

        expectInvalidToken("Invalid issuer: 'null', expected: 'https://test.test.test/SAAS/auth'");
        willSuccessfullyDecodeAccessToken(token);
        tokenServices.loadAuthentication(AN_ACCESS_TOKEN_STRING);
    }

    @Test
    public void testLoadAuthenticationDoesNotValidatesTokenWithInvalidIssuer() throws Exception {
        DefaultOAuth2AccessToken token = aValidOAuth2AccessToken();
        token.getAdditionalInformation().put("iss", "wrong!");

        expectInvalidToken("Invalid issuer: 'wrong!', expected: 'https://test.test.test/SAAS/auth'");
        willSuccessfullyDecodeAccessToken(token);
        tokenServices.loadAuthentication(AN_ACCESS_TOKEN_STRING);
    }

    @Test
    public void testLoadAuthenticationFailsIfInvalidToken() throws Exception {
        given(mockTokenStore.readAccessToken(AN_ACCESS_TOKEN_STRING)).willReturn(null);
        expectInvalidToken("Invalid access token: " + AN_ACCESS_TOKEN_STRING);
        tokenServices.loadAuthentication(AN_ACCESS_TOKEN_STRING);
    }

    @Test
    public void testLoadAuthenticationFailsIfExpiredToken() throws Exception {
        DefaultOAuth2AccessToken token = aValidOAuth2AccessToken();


        cal.add(Calendar.HOUR_OF_DAY, -2); // remove 2 hours
        token.setExpiration(cal.getTime());

        willSuccessfullyDecodeAccessToken(token);
        expectInvalidToken("Access token expired: " + cal.getTime());
        tokenServices.loadAuthentication(AN_ACCESS_TOKEN_STRING);
    }

    @Test
    public void testLoadAuthenticationFailsIfMissingIssuedAt() throws Exception {
        DefaultOAuth2AccessToken token = aValidOAuth2AccessToken();
        token.getAdditionalInformation().remove("iat");

        willSuccessfullyDecodeAccessToken(token);
        expectInvalidToken("Missing or invalid 'iat' key, expecting a valid timestamp value.");
        tokenServices.loadAuthentication(AN_ACCESS_TOKEN_STRING);
    }

    @Test
    public void testLoadAuthenticationFailsIfInvalidIssuedAt() throws Exception {
        DefaultOAuth2AccessToken token = aValidOAuth2AccessToken();
        token.getAdditionalInformation().put("iat", "invalid-timestamp");

        willSuccessfullyDecodeAccessToken(token);
        expectInvalidToken("Missing or invalid 'iat' key, expecting a valid timestamp value.");
        tokenServices.loadAuthentication(AN_ACCESS_TOKEN_STRING);
    }

    @Test
    public void testLoadAuthenticationFailsIfIssuedAtIsInTheFuture() throws Exception {
        DefaultOAuth2AccessToken token = aValidOAuth2AccessToken();

        cal.setTime(new Date());
        cal.add(Calendar.HOUR_OF_DAY, 2); // add 2 hours

        token.getAdditionalInformation().put("iat", (int) (cal.getTime().getTime() / 1000));

        willSuccessfullyDecodeAccessToken(token);
        expectInvalidToken("Token has been issued in the future:");
        tokenServices.loadAuthentication(AN_ACCESS_TOKEN_STRING);
    }

    @Test
    public void testLoadAuthenticationSucceedsIfIssuedAtTimeIsWithinTheAllowedSkew() throws Exception {
        DefaultOAuth2AccessToken token = aValidOAuth2AccessToken();

        cal.setTime(new Date());
        // hopefully the time it takes to execute is lower than the allowed skew time
        cal.add(Calendar.MILLISECOND, (int) (VMwareValidateTokenServices.ALLOWED_SKEW_IN_MS - 1));
        token.getAdditionalInformation().put("iat", (int) (cal.getTime().getTime() / 1000));

        willSuccessfullyDecodeAccessToken(token);
        willSuccessfullyReadAuthInfo();

        OAuth2Authentication auth = tokenServices.loadAuthentication(AN_ACCESS_TOKEN_STRING);
        assertAuthIsValid(auth);
    }

    @Test
    public void canReadAccessToken() {
        willSuccessfullyDecodeAccessToken(aValidAccessToken);
        OAuth2AccessToken result = tokenServices.readAccessToken(AN_ACCESS_TOKEN_STRING);
        assertThat(result).isEqualTo(aValidAccessToken);
    }
    
    /**
     * Helper to test the actual error message returned in the exception.
     */
    private void expectInvalidToken(String message) {
        thrown.expect(InvalidTokenException.class);
        thrown.expectMessage(message);
    }

    private void testLocalValidationWithResult(String expectedResult, RestOperations mockRestTemplate) {
        given(mockRestTemplate.getForObject(any(URI.class), any())).willReturn(expectedResult);
        tokenServices.setValidateLocally(false);
        tokenServices.setRestTemplate(mockRestTemplate);
        OAuth2Authentication auth = tokenServices.loadAuthentication(AN_ACCESS_TOKEN_STRING);
        if (Boolean.getBoolean(expectedResult)) {
            assertAuthIsValid(auth);
        }
    }

    private void assertAuthIsValid(OAuth2Authentication auth) {
        assertThat(auth.getPrincipal()).isEqualTo("test-username");
    }

    private void willSuccessfullyReadAuthInfo() {
        given(mockTokenStore.readAuthentication(AN_ACCESS_TOKEN_STRING)).willReturn(aValidOAuth2Authentication);
    }

    private void willSuccessfullyDecodeAccessToken(OAuth2AccessToken token) {
        given(mockTokenStore.readAccessToken(AN_ACCESS_TOKEN_STRING)).willReturn(token);
    }

    private DefaultOAuth2AccessToken aValidOAuth2AccessToken() {
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(AN_ACCESS_TOKEN_STRING);
        Map<String, Object> info = new HashMap<>();
        info.put("iss", "https://test.test.test/SAAS/auth");
        info.put("iat", (int) (cal.getTime().getTime() / 1000));
        token.setAdditionalInformation(info);

        cal.add(Calendar.HOUR_OF_DAY, 1); // adds one hour
        token.setExpiration(cal.getTime());
        return token;
    }

}