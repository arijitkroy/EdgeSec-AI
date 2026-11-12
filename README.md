# EdgeSecAI

EdgeSecAI is an Android cybersecurity-monitoring prototype. Its intended operation is offline for analysis: traffic is forwarded directly to its destination, while the local app inspects flow metadata and selected DNS query names and runs local heuristic detection. It does not send traffic to a hosted analysis service.

The project remains an engineering prototype, not production-grade endpoint protection. The current emulator build was manually checked by loading an external HTTPS page through the active VPN; logs showed protected TCP connections and inbound response data. UDP DNS and UDP/QUIC forwarding were also observed. This validates that emulator path only. Physical-device behavior and broad protocol coverage still need testing.

## Current status

Implemented:

- Android dashboard with packet, flow, alert, anomaly, and device-wide throughput views.
- User-approved local VPN service for IPv4 and IPv6 routes.
- A native TUN-to-SOCKS5 bridge using HevSocks5Tunnel, with an in-app SOCKS5 endpoint.
- Direct TCP and UDP forwarding through sockets protected from the VPN route. No external SOCKS server is configured.
- Local flow aggregation, handcrafted intrusion and malware-behavior rules, an Isolation Forest implementation, alerts, and SQLite persistence.
- Independent Clear actions for alert history and recent flow entries; clearing alerts resets threat counters, while clearing flows leaves monitoring and alert history intact.
- A custom adaptive launcher icon using a cyan and mint circuit shield on a dark navy background.
- A local rule-based assistant and synthetic attack simulator.
- Third-party MIT license notice in `app/src/main/assets/THIRD_PARTY_NOTICES.txt`.

Verified on the Android emulator:

- HTTPS browsing through the active VPN using an external test page, with TCP connect and inbound response data observed in Logcat.
- UDP DNS queries and responses, plus UDP/QUIC traffic in both directions.

Still to verify on physical devices and across network conditions:

- IPv4 and IPv6 behavior, multiple DNS configurations, VPN stop/revoke, process death, and network-change recovery.
- Whether UDP association behavior remains reliable across Android versions, networks, and the bundled HevSocks5Tunnel build.

The last clean build command used during development was:

```powershell
.\gradlew.bat app:clean app:assembleDebug
```

## Build requirements

- JDK 17.
- Android SDK Platform 37.
- Android Gradle Plugin 9.5.1 through the version catalog.
- Android Studio / Gradle setup able to resolve the configured Google and Maven Central repositories.
- Network access for the first build to resolve Android dependencies and the HevSocks5Tunnel 2.19.3 AAR from its GitHub release Ivy repository.

The app's minimum Android SDK is 33 and its target SDK is 37. Android Studio creates `local.properties` for the local SDK path; it is intentionally ignored by Git. The adaptive launcher icon is a native vector shield/circuit mark and scales without raster density variants on supported Android versions.

Build a debug APK:

```powershell
.\gradlew.bat app:assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install from Android Studio, or use:

```powershell
.\gradlew.bat app:installDebug
```

### Release APK

Build the optimized release variant with:

```powershell
.\gradlew.bat app:assembleRelease
```

This repository has no release signing key or signing configuration. Gradle therefore produces an **unsigned release APK** at:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

An APK must be signed before normal installation or distribution. In Android Studio, choose **Build > Generate Signed Bundle / APK > APK**, select or create a release keystore, and save the signed artifact outside the repository. Keep the keystore and passwords backed up securely; do not commit them. Friends can build an unsigned release with `app:assembleRelease` or sign their own APK with a keystore they control.

The VPN must be explicitly approved by the user in Android's VPN consent dialog. The app requests notification permission on Android 13 and later.

## Project layout

Production sources are under `app/src/main/java/com/example/edgesecai/`.

- `MainActivity.java`: Activity and bottom-navigation fragment host.
- `ui/`: Dashboard, alerts, flows, simulator, and assistant UI and adapters.
- `engine/VpnInspectorService.java`: Foreground Android `VpnService`, TUN setup, native forwarder lifecycle, and shutdown.
- `engine/DirectSocks5Server.java`: Loopback SOCKS5 endpoint. It handles TCP CONNECT and UDP ASSOCIATE and opens protected direct sockets outside the VPN.
- `engine/PacketParser.java`: IPv4 packet parser retained from the earlier TUN-reader prototype. The active native forwarder owns the TUN descriptor, so this parser is not the current full-route forwarding path.
- `engine/AttackSimulator.java`: Synthetic traffic generation for detector/UI demonstrations.
- `ai/CybersecurityAIEngine.java`: Feature extraction, Isolation Forest scoring, and detector orchestration.
- `ai/IsolationForestModel.java`: In-process forest built from generated nominal examples. This is a handcrafted prototype model, not a downloaded or trained production model.
- `ai/NetworkIntrusionDetector.java`: Port-scan, SYN-rate, and traffic-outlier heuristics.
- `ai/MalwareBehaviorDetector.java`: Stratum signature, periodic-beacon, DNS tunneling, and high-volume asymmetric-traffic heuristics.
- `ai/AIAssistantExplainer.java`: Offline fixed knowledge base and rule-based responses. It is not a generative language model.
- `storage/SecurityRepository.java`: In-memory active state, background analysis, listener notifications, and block lists.
- `storage/SecurityDatabaseHelper.java`: Local SQLite persistence for alerts and flows.
- `model/`: Flow, packet, alert, severity, threat type, and summary data structures.
- `app/src/main/res/`: Android layouts, strings, themes, the adaptive EdgeSecAI shield icon, and navigation resources.

## Live traffic path

1. The dashboard starts `VpnInspectorService` after Android grants VPN consent.
2. The service starts `DirectSocks5Server` on loopback port 10808.
3. `VpnService.Builder` routes IPv4 and IPv6 traffic into the TUN interface and excludes the EdgeSecAI app itself to prevent a proxy loop.
4. HevSocks5Tunnel reads the TUN descriptor and translates intercepted TCP and UDP into SOCKS5 requests to the local endpoint.
5. For TCP CONNECT, the local endpoint opens a socket to the requested destination. For UDP ASSOCIATE, it sends datagrams directly to destinations. The service calls `VpnService.protect` on these outbound sockets so they use the underlying network instead of re-entering the VPN.
6. The SOCKS endpoint reports destination, protocol, byte count, timing, and classic UDP DNS query names to `SecurityRepository`. Analysis and persistence run on a background executor.
7. The Dashboard separately samples Android `TrafficStats` counters once per second while visible. Those are device-wide throughput counters, not per-app flow attribution.

The foreground notification and dashboard protection state should only indicate active once TUN and native forwarding startup report success. If the native tunnel cannot start, the service tears down the VPN interface. The Android emulator has completed an external HTTPS request through the protected TCP path; this should not be treated as validation for all devices or networks.

## Detection and data limits

The forwarding library handles packet transport. EdgeSecAI's Java layer sees SOCKS connection metadata and stream byte chunks; it does not independently inspect every raw IP packet.

- App/UID attribution is currently unavailable for proxied flows. They are labeled `Device traffic` with UID 0.
- HTTPS/TLS application payloads are encrypted. The app does not install a certificate authority or perform TLS interception. Stratum strings inside TLS cannot be inspected.
- DNS tunneling analysis only receives classic DNS query names carried through UDP port 53. DNS over HTTPS, DNS over TLS, encrypted client hello, and other encrypted DNS are not decoded.
- Destination hostnames may be resolved by Android's resolver in the EdgeSecAI process. Validate whether those resolution requests are visible to the current inspection path; do not claim complete DNS coverage.
- The project rules and Isolation Forest baseline are prototypes. Detection thresholds are not calibrated against a representative traffic dataset and can produce false positives or misses.
- Blocking is local to the SOCKS forwarding path. An alert is analyzed asynchronously, so bytes may pass before a new block is applied. Verify per-endpoint and per-port behavior under concurrency.
- The detector's simulated port scans and SYN floods model traffic that is not necessarily meaningful as an attack against a phone acting as a VPN client. Treat simulator alerts as rule-path demonstrations.
- The local forwarding service depends on an active underlying network for internet access. “Offline-first” means local detection and storage do not require a cloud model; it does not mean remote internet destinations work offline.
- ICMP and non-TCP/UDP protocols are not supported by the in-app SOCKS endpoint. Validate IPv6 and UDP relay behavior before relying on the VPN for general use.
- Device-wide throughput uses Android network byte counters and should not be presented as inspected packet volume.

## Native tunnel dependency

The app resolves `hev:hev-socks5-tunnel:2.19.3@aar` using the `HevTunnelReleases` Ivy repository in `settings.gradle.kts`. The AAR ships the JNI binding class `hev.htproxy.TProxyService` and native libraries for armeabi-v7a, arm64-v8a, x86, and x86_64. The service starts and stops the native tunnel through this binding.

Upstream project and documentation:

- [HevSocks5Tunnel repository](https://github.com/heiher/hev-socks5-tunnel)
- [HevSocks5Tunnel README and Android AAR API](https://github.com/heiher/hev-socks5-tunnel#prebuilt-android-aar)
- The upstream project is MIT licensed; preserve its copyright and license notice when redistributing the APK.

If dependency resolution fails, first check that GitHub release downloads are reachable and that Gradle is not in offline mode. The Android NDK is not required when using the prebuilt AAR.

## Guidance for the next agent

Before extending detection logic:

1. Run `app:assembleDebug` and inspect the resolved native AAR and APK ABI contents.
2. Test on an emulator/device with access to a known network. Verify normal TCP browsing, UDP DNS, HTTPS, IPv6, UDP applications, and app behavior while stopping/revoking the VPN.
3. Inspect Logcat for `VpnInspectorService`, `EdgeSec-Socks`, and native Hev tunnel errors. Confirm that failed startup removes routes and reports the shield inactive.
4. Add automated tests for SOCKS5 negotiation, IPv4/domain/IPv6 address parsing, TCP CONNECT, UDP framing, DNS query extraction, malformed packets, and blocked destinations.
5. Fix any forwarding or lifecycle defects before treating full-device monitoring as reliable. Avoid routing all device traffic through an unverified forwarder.
6. Improve flow identity and per-app attribution before tuning the anomaly model. Evaluate rules against labeled benign and malicious traffic and document false-positive rates.
7. Keep assistant descriptions, dashboard labels, and documentation aligned with what the active capture path can actually observe.

## Data handling

Alerts and flow records are stored in the app's private SQLite database. The current design does not upload telemetry to a server. Network sockets are created to forward user-requested traffic to its destination; the app must have an underlying network for those connections to succeed.
