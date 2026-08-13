package cn.enderrealm.bedrock.mappings;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the public Bedrock protocol mapping API.
 */
class BedrockProtocolMappingsTest {
    /**
     * Verifies canonical versions, aliases, and whitespace normalization.
     */
    @Test
    void findsCanonicalVersionAndAlias() {
        assertEquals(1001, BedrockProtocolMappings.findProtocolVersion(" v1.26.31 ").orElseThrow());
        assertEquals("26.30", BedrockProtocolMappings.findByClientVersion("1.26.31").orElseThrow().canonicalVersion());
        assertEquals("1.21.130", BedrockProtocolMappings.normalizeVersion(" 1.21.130 "));
    }

    /**
     * Verifies that unknown versions are not fuzzy-matched.
     */
    @Test
    void rejectsUnknownVersion() {
        assertFalse(BedrockProtocolMappings.isSupported("1.21.999"));
        assertTrue(BedrockProtocolMappings.findProtocolVersion("1.21.999").isEmpty());
    }

    /**
     * Verifies that reverse lookup retains canonical versions and all aliases.
     */
    @Test
    void reverseLookupKeepsAllClientVersions() {
        Set<String> versions = BedrockProtocolMappings.getClientVersions(1001);
        assertTrue(versions.contains("26.30"));
        assertTrue(versions.contains("1.26.34"));
        assertEquals(1, BedrockProtocolMappings.findByProtocolVersion(1001).size());
        assertThrows(UnsupportedOperationException.class, () -> versions.add("invalid"));
    }

    /**
     * Verifies release-channel filtering and immutable return lists.
     */
    @Test
    void filtersReleaseChannel() {
        assertEquals(BedrockProtocolMappings.getAllVersions().size(),
                BedrockProtocolMappings.getVersions(BedrockReleaseChannel.RELEASE).size());
        assertThrows(UnsupportedOperationException.class,
                () -> BedrockProtocolMappings.getAllVersions().add(null));
    }
}
