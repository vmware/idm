package com.vmware.idm.samples.oauth2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the application configuration can load fine (integration test).
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ResourceApplicationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void contextLoads() {
    }

    @Test
    public void canAccessHome() {
        String body = this.restTemplate.getForObject("/", String.class);
        assertThat(body).isEqualTo("Home resource (unprotected)\n");
    }

    @Test
    public void cannotAccessProtectedResource() {
        String body = this.restTemplate.getForObject("/resource", String.class);
        assertThat(body).contains("{\"error\":\"unauthorized\",\"error_description\":\"Full authentication is required to access this resource\"}");
    }

}