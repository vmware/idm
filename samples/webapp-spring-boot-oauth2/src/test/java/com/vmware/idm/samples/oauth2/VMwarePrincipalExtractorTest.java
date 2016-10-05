package com.vmware.idm.samples.oauth2;

import org.junit.Test;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for our special extractor.
 */
public class VMwarePrincipalExtractorTest {

    PrincipalExtractor principalExtractor = new VMwarePrincipalExtractor();

    @Test
    public void testExtractPrincipal() throws Exception {
        Map map = new HashMap<>();
        map.put(VMwarePrincipalExtractor.VMWARE_KEY_SUBJECT, "test");

        assertEquals("test", principalExtractor.extractPrincipal(map));
    }

    @Test
    public void testExtractPrincipalIfMultipleFields() throws Exception {
        Map map = new HashMap<>();
        map.put(VMwarePrincipalExtractor.VMWARE_KEY_SUBJECT, "test");
        map.put("name", "test-other");

        assertEquals("test", principalExtractor.extractPrincipal(map));
    }

    @Test
    public void testExtractPrincipalReturnsNullIfEmpty() throws Exception {
        assertNull(principalExtractor.extractPrincipal(new HashMap<>()));
    }

    @Test
    public void testExtractPrincipalReturnsNullIfNotFound() throws Exception {

        Map map = new HashMap<>();
        map.put("name", "test-other");
        assertNull(principalExtractor.extractPrincipal(new HashMap<>()));
    }
}