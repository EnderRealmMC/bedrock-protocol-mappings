package cn.enderrealm.bedrock.mappings;

import java.util.Objects;
import java.util.Set;

/**
 * A Bedrock client version and its protocol metadata.
 *
 * @param canonicalVersion canonical client version
 * @param aliases registered client version aliases
 * @param protocolVersion Bedrock protocol version
 * @param codecId matching codec identifier, such as {@code v1001}
 * @param releaseChannel release channel
 */
public record BedrockVersion(
        String canonicalVersion,
        Set<String> aliases,
        int protocolVersion,
        String codecId,
        BedrockReleaseChannel releaseChannel
) {
    /**
     * Creates and freezes version metadata.
     */
    public BedrockVersion {
        canonicalVersion = Objects.requireNonNull(canonicalVersion, "canonicalVersion");
        aliases = Set.copyOf(Objects.requireNonNull(aliases, "aliases"));
        codecId = Objects.requireNonNull(codecId, "codecId");
        releaseChannel = Objects.requireNonNull(releaseChannel, "releaseChannel");
    }

    /**
     * Returns the canonical version and all aliases represented by this record.
     *
     * @return immutable client version set
     */
    public Set<String> allClientVersions() {
        java.util.LinkedHashSet<String> versions = new java.util.LinkedHashSet<>();
        versions.add(canonicalVersion);
        versions.addAll(aliases);
        return Set.copyOf(versions);
    }
}
