#!/usr/bin/env bash
set -euo pipefail
# Existing cloud checkouts are isolated; do not create another worktree.
cd /workspace/No
mkdir -p /workspace/tools /workspace/.gradle-cache
if [ ! -x /workspace/tools/jdk-17.0.17+10/bin/javac ]; then
  archive=$(mktemp /tmp/gyro-jdk-XXXXXX.tar.gz)
  trap 'rm -f "$archive"' EXIT
  curl --fail --location --retry 2 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.17%2B10/OpenJDK17U-jdk_x64_linux_hotspot_17.0.17_10.tar.gz' -o "$archive"
  printf '%s  %s\n' '992f96e7995075ac7636bb1a8de52b0c61d71ed3137fafc979ab96b4ab78dd75' "$archive" | sha256sum --check --status
  tar -xzf "$archive" -C /workspace/tools
fi
export JAVA_HOME=/workspace/tools/jdk-17.0.17+10
export GRADLE_USER_HOME=/workspace/.gradle-cache
# Reuse the platform's trusted CA store; never disable TLS verification.
export GRADLE_OPTS="${GRADLE_OPTS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
# Gradle does not automatically consume HTTPS_PROXY. Save only proxy host/port,
# retaining unrelated Gradle user properties and never copying credentials.
python3 - <<'PROXY'
import os, pathlib, urllib.parse
proxy = urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY', ''))
if proxy.hostname:
    path = pathlib.Path('/workspace/.gradle-cache/gradle.properties')
    updates = {
        'systemProp.https.proxyHost': proxy.hostname,
        'systemProp.https.proxyPort': str(proxy.port or 80),
        'systemProp.http.proxyHost': proxy.hostname,
        'systemProp.http.proxyPort': str(proxy.port or 80),
        'systemProp.http.nonProxyHosts': 'localhost|127.*',
    }
    lines = path.read_text().splitlines() if path.exists() else []
    lines = [line for line in lines if line.partition('=')[0].strip() not in updates]
    path.write_text('\n'.join(lines + [f'{k}={v}' for k, v in updates.items()]) + '\n')
PROXY
./gradlew --no-daemon build runGametest
