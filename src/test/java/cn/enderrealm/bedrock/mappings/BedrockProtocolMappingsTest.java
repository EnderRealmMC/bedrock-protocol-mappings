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
     * Verifies that the registry spans the earliest and latest codecs exposed by Cloudburst Protocol.
     */
    @Test
    void containsTheCompleteCodecRange() {
        assertEquals(291, BedrockProtocolMappings.findProtocolVersion("1.7.0").orElseThrow());
        assertEquals("26.30", BedrockProtocolMappings.findByClientVersion("1.26.31").orElseThrow().canonicalVersion());
        assertEquals(2168, BedrockProtocolMappings.findProtocolVersion("26.40").orElseThrow());
        assertEquals(2168, BedrockProtocolMappings.findProtocolVersion("26.41").orElseThrow());
        assertEquals(2168, BedrockProtocolMappings.findProtocolVersion("26.42").orElseThrow());
        assertEquals(2168, BedrockProtocolMappings.findProtocolVersion("26.43").orElseThrow());
        assertEquals("v2168", BedrockProtocolMappings.findByProtocolVersion(2168).getFirst().codecId());
        assertEquals(2169, BedrockProtocolMappings.findProtocolVersion("26.44").orElseThrow());
        assertEquals(2169, BedrockProtocolMappings.findProtocolVersion("26.45").orElseThrow());
        assertEquals("v2169", BedrockProtocolMappings.findByClientVersion("26.44").orElseThrow().codecId());
        assertEquals("v291", BedrockProtocolMappings.findByProtocolVersion(291).getFirst().codecId());
        assertEquals(1, BedrockProtocolMappings.findByProtocolVersion(2168).size());
        assertEquals(1, BedrockProtocolMappings.findByProtocolVersion(2169).size());
        assertEquals(65, BedrockProtocolMappings.getAllVersions().size());
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
