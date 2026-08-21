# bedrock-protocol-mappings

Pure Java 21 library for bidirectional Bedrock client-version and protocol-version lookup.

The bundled registry supports Bedrock codecs from `Bedrock_v291` (1.7.0) through `Bedrock_v2169` (26.44, latest with 26.45 alias).

## Coordinates

```text
cn.enderrealm:bedrock-protocol-mappings:1.0.0
```

The library has no dependency on Bukkit, Floodgate, Geyser, or Cloudburst Protocol. Version data is loaded from the bundled `bedrock-versions.json` resource and validated when the mapping class is initialized.

## Usage

```java
OptionalInt protocol = BedrockProtocolMappings.findProtocolVersion("1.26.31");
List<BedrockVersion> versions = BedrockProtocolMappings.findByProtocolVersion(1001);
Set<String> clientVersions = BedrockProtocolMappings.getClientVersions(1001);
```

Queries trim surrounding whitespace and accept the registered aliases. Unknown versions return an empty optional; the library never guesses the nearest protocol.

The registry intentionally stores one record per codec. When several client releases use the same wire protocol, those releases are listed as aliases of the same record, so reverse lookups retain every registered client version without duplicating codec metadata.

## Build and publish

```text
gradle test
gradle build
gradle publish
```

`publish` targets GitHub Packages. A classic personal access token is required for publishing and for downloading Maven/Gradle packages, including public packages. Credentials can be supplied with `-PgprUser` and `-PgprToken`, or with `GITHUB_ACTOR` and `GITHUB_TOKEN` environment variables. Cumuline uses a Gradle composite build during local development, so publishing locally is not required.
A version registry for Minecraft Bedrock Edition, mapping client versions to network protocol versions and codec identifiers.
