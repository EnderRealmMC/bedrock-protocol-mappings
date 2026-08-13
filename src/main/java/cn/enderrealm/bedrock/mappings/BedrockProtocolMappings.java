package cn.enderrealm.bedrock.mappings;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Read-only bidirectional mappings between Bedrock client versions and protocol versions.
 *
 * <p>Unknown versions are never guessed as the nearest version; callers must handle empty results explicitly.</p>
 */
public final class BedrockProtocolMappings {
    private static final String RESOURCE_NAME = "/bedrock-versions.json";
    private static final Pattern VERSION_PATTERN = Pattern.compile("(?:0|[1-9]\\d*)(?:\\.(?:0|[1-9]\\d*))*");
    private static final Pattern CODEC_PATTERN = Pattern.compile("v[1-9]\\d*");
    private static final Catalog CATALOG = loadCatalog();

    private BedrockProtocolMappings() {
    }

    /**
     * Finds metadata by client version.
     *
     * @param version canonical version or registered alias
     * @return matching version metadata
     */
    public static Optional<BedrockVersion> findByClientVersion(String version) {
        return Optional.ofNullable(CATALOG.byClientVersion.get(normalizeVersion(version)));
    }

    /**
     * Finds a protocol version by client version.
     *
     * @param version canonical version or registered alias
     * @return protocol version
     */
    public static OptionalInt findProtocolVersion(String version) {
        return findByClientVersion(version).map(value -> OptionalInt.of(value.protocolVersion()))
                .orElseGet(OptionalInt::empty);
    }

    /**
     * Finds all client versions for a protocol version.
     *
     * @param protocolVersion Bedrock protocol version
     * @return immutable list of matching versions
     */
    public static List<BedrockVersion> findByProtocolVersion(int protocolVersion) {
        return CATALOG.byProtocolVersion.getOrDefault(protocolVersion, List.of());
    }

    /**
     * Returns all registered versions.
     *
     * @return immutable version list
     */
    public static List<BedrockVersion> getAllVersions() {
        return CATALOG.allVersions;
    }

    /**
     * Returns versions from the requested release channel.
     *
     * @param channel release channel
     * @return immutable version list
     */
    public static List<BedrockVersion> getVersions(BedrockReleaseChannel channel) {
        Objects.requireNonNull(channel, "channel");
        return CATALOG.byChannel.getOrDefault(channel, List.of());
    }

    /**
     * Returns all client version names for a protocol version, including canonical names and aliases.
     *
     * @param protocolVersion Bedrock protocol version
     * @return immutable client version set
     */
    public static Set<String> getClientVersions(int protocolVersion) {
        return CATALOG.clientVersionsByProtocol.getOrDefault(protocolVersion, Set.of());
    }

    /**
     * Checks whether a client version is registered.
     *
     * @param version canonical version or registered alias
     * @return whether the version is supported
     */
    public static boolean isSupported(String version) {
        return findByClientVersion(version).isPresent();
    }

    /**
     * Normalizes a version string for lookup.
     *
     * <p>Only surrounding whitespace and an optional {@code v} prefix are removed. No fuzzy matching or version inference is performed.</p>
     *
     * @param version raw version string
     * @return normalized version, or an empty string for null input
     */
    public static String normalizeVersion(String version) {
        if (version == null) {
            return "";
        }
        String normalized = version.trim();
        if (normalized.length() > 1 && (normalized.charAt(0) == 'v' || normalized.charAt(0) == 'V')) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }

    /**
     * Loads and validates mappings from the bundled JSON resource.
     */
    private static Catalog loadCatalog() {
        try (InputStream stream = BedrockProtocolMappings.class.getResourceAsStream(RESOURCE_NAME)) {
            if (stream == null) {
                throw new IllegalStateException("Bedrock mapping resource is missing: " + RESOURCE_NAME);
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                MappingFile file = new Gson().fromJson(reader, MappingFile.class);
                return Catalog.create(file);
            }
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            if (exception instanceof IllegalStateException stateException
                    && stateException.getMessage() != null
                    && stateException.getMessage().startsWith("Bedrock mapping resource is missing")) {
                throw stateException;
            }
            throw new IllegalStateException("Failed to load Bedrock mapping resource: " + RESOURCE_NAME, exception);
        }
    }

    /** JSON root object. */
    private static final class MappingFile {
        private List<MappingEntry> versions;
    }

    /** A single version entry from the JSON resource. */
    private static final class MappingEntry {
        private String canonicalVersion;
        private List<String> aliases;
        private int protocolVersion;
        private String codecId;
        private String releaseChannel;
        @SuppressWarnings("unused")
        private boolean current;
    }

    /**
     * Immutable indexes built from the mapping resource.
     *
     * @param allVersions all canonical version records
     * @param byClientVersion forward client-version index
     * @param byProtocolVersion reverse protocol-version index
     * @param clientVersionsByProtocol reverse client-name index
     * @param byChannel release-channel index
     */
    private record Catalog(
            List<BedrockVersion> allVersions,
            Map<String, BedrockVersion> byClientVersion,
            Map<Integer, List<BedrockVersion>> byProtocolVersion,
            Map<Integer, Set<String>> clientVersionsByProtocol,
            Map<BedrockReleaseChannel, List<BedrockVersion>> byChannel
    ) {
        /**
         * Builds and validates all forward and reverse indexes.
         *
         * @param file JSON mapping data
         * @return frozen mapping catalog
         */
        private static Catalog create(MappingFile file) {
            if (file == null || file.versions == null || file.versions.isEmpty()) {
                throw new IllegalStateException("Bedrock mapping resource contains no versions");
            }

            List<BedrockVersion> versions = new ArrayList<>();
            Map<String, BedrockVersion> byClient = new LinkedHashMap<>();
            Map<Integer, List<BedrockVersion>> byProtocol = new LinkedHashMap<>();
            Map<BedrockReleaseChannel, List<BedrockVersion>> byChannel = new EnumMap<>(BedrockReleaseChannel.class);

            for (MappingEntry entry : file.versions) {
                BedrockVersion version = toVersion(entry);
                if (byClient.putIfAbsent(normalizeVersion(version.canonicalVersion()), version) != null) {
                    throw new IllegalStateException("Duplicate canonical Bedrock version: " + version.canonicalVersion());
                }
                versions.add(version);
                byProtocol.computeIfAbsent(version.protocolVersion(), ignored -> new ArrayList<>()).add(version);
                byChannel.computeIfAbsent(version.releaseChannel(), ignored -> new ArrayList<>()).add(version);
                for (String alias : version.aliases()) {
                    String normalizedAlias = normalizeVersion(alias);
                    BedrockVersion previous = byClient.putIfAbsent(normalizedAlias, version);
                    if (previous != null && previous != version) {
                        throw new IllegalStateException("Conflicting Bedrock version alias: " + alias);
                    }
                }
            }

            Map<Integer, Set<String>> clientVersionsByProtocol = new LinkedHashMap<>();
            for (Map.Entry<Integer, List<BedrockVersion>> entry : byProtocol.entrySet()) {
                java.util.LinkedHashSet<String> clientVersions = new java.util.LinkedHashSet<>();
                for (BedrockVersion version : entry.getValue()) {
                    clientVersions.add(version.canonicalVersion());
                    clientVersions.addAll(version.aliases());
                    if (version.protocolVersion() != entry.getKey()) {
                        throw new IllegalStateException("Protocol reverse index mismatch for " + version.canonicalVersion());
                    }
                }
                clientVersionsByProtocol.put(entry.getKey(), Set.copyOf(clientVersions));
            }

            for (Map.Entry<String, BedrockVersion> entry : byClient.entrySet()) {
                List<BedrockVersion> reverseVersions = byProtocol.get(entry.getValue().protocolVersion());
                if (reverseVersions == null || !reverseVersions.contains(entry.getValue())) {
                    throw new IllegalStateException("Protocol reverse index is incomplete for " + entry.getKey());
                }
            }

            return new Catalog(
                    List.copyOf(versions),
                    Map.copyOf(byClient),
                    freezeLists(byProtocol),
                    Map.copyOf(clientVersionsByProtocol),
                    freezeLists(byChannel)
            );
        }

        /**
         * Validates and converts one JSON entry.
         *
         * @param entry raw JSON entry
         * @return validated public version record
         */
        private static BedrockVersion toVersion(MappingEntry entry) {
            if (entry == null || !isVersion(entry.canonicalVersion) || entry.protocolVersion <= 0
                    || entry.aliases == null || entry.codecId == null || !CODEC_PATTERN.matcher(entry.codecId).matches()
                    || entry.releaseChannel == null) {
                throw new IllegalStateException("Invalid Bedrock mapping entry");
            }
            BedrockReleaseChannel channel;
            try {
                channel = BedrockReleaseChannel.valueOf(entry.releaseChannel);
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Invalid Bedrock release channel: " + entry.releaseChannel, exception);
            }
            Set<String> normalizedAliases = new HashSet<>();
            if (entry.aliases.stream().anyMatch(alias -> !isVersion(alias)
                    || normalizeVersion(alias).equals(normalizeVersion(entry.canonicalVersion))
                    || !normalizedAliases.add(normalizeVersion(alias)))) {
                throw new IllegalStateException("Invalid Bedrock version alias for: " + entry.canonicalVersion);
            }
            return new BedrockVersion(entry.canonicalVersion, Set.copyOf(entry.aliases), entry.protocolVersion, entry.codecId, channel);
        }

        /**
         * Checks whether a value is a valid dotted numeric version.
         *
         * @param value version value
         * @return whether the value is valid
         */
        private static boolean isVersion(String value) {
            return value != null && VERSION_PATTERN.matcher(normalizeVersion(value)).matches();
        }

        /**
         * Freezes every list in an index map.
         *
         * @param source mutable index map
         * @param <K> index key type
         * @return immutable index map
         */
        private static <K> Map<K, List<BedrockVersion>> freezeLists(Map<K, List<BedrockVersion>> source) {
            Map<K, List<BedrockVersion>> frozen = new HashMap<>();
            source.forEach((key, value) -> frozen.put(key, List.copyOf(value)));
            return Map.copyOf(frozen);
        }
    }
}
