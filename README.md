# EdgeSecAI - On-Device AI Cybersecurity Monitor

EdgeSecAI is an offline-first Android cybersecurity monitoring and intrusion detection system. It intercepts device traffic locally using an Android `VpnService` with a high-performance native TUN-to-SOCKS5 proxy, computes multi-dimensional flow metrics, and analyzes network flows using a local machine learning engine (Isolation Forest anomaly detector paired with behavioral heuristics) without exfiltrating telemetry to cloud backends.

---

## Architecture Overview

```mermaid
graph TD
    subgraph Device ["Android Device / Applications"]
        AppTraffic["App Network Traffic (TCP / UDP)"]
    end

    subgraph Kernel ["Android Networking Subsystem"]
        VPNInterface["TUN Interface (VpnService)"]
        NativeTunnel["HevSocks5Tunnel Engine (TProxy / C Library)"]
    end

    subgraph EdgeSecAI ["EdgeSecAI Process (app)"]
        SocksServer["DirectSocks5Server (127.0.0.1:10808)"]
        ProtectedSockets["Protected Outbound Sockets (VpnService.protect)"]
        
        subgraph Pipeline ["Inspection & Analysis Pipeline"]
            SecRepo["SecurityRepository (Flow Aggregation & State)"]
            AIEngine["CybersecurityAIEngine"]
            Features["FeatureExtractor (Entropy, IAT, Rates)"]
            IsoForest["IsolationForestModel (Unsupervised Anomaly)"]
            IntrusionDet["NetworkIntrusionDetector (Port Scan, SYN Flood)"]
            MalwareDet["MalwareBehaviorDetector (C2, Exfil, Mining)"]
            DB[("SQLite Database (edgesec_threats.db)")]
        end

        subgraph Presentation ["UI & Interactions"]
            Dashboard["DashboardFragment (Shield & Counters)"]
            FlowsUI["FlowsFragment (Real-time Flow Inspection)"]
            AlertsUI["AlertsFragment (Active & Historical Threats)"]
            Simulator["SimulatorFragment (Attack Pattern Generator)"]
            Assistant["AssistantFragment (Offline Explainer)"]
        end
    end

    subgraph Internet ["External Network / WAN"]
        TargetServers["Remote Servers & DNS Services"]
    end

    %% Flow connections
    AppTraffic -->|Routed to TUN| VPNInterface
    VPNInterface -->|Intercepts Packets| NativeTunnel
    NativeTunnel -->|Translates to SOCKS5| SocksServer
    SocksServer -->|Streams Metadata & Headers| SecRepo
    SocksServer -->|Forwards Data Outside VPN| ProtectedSockets
    ProtectedSockets -->|TCP / UDP Direct| TargetServers

    SecRepo -->|Aggregate Flow & Window| AIEngine
    AIEngine --> Features
    Features --> IsoForest
    AIEngine --> IntrusionDet
    AIEngine --> MalwareDet
    AIEngine -->|Threat Alerts / Anomaly Scores| SecRepo

    SecRepo -->|Persist Alerts & Flows| DB
    SecRepo -->|State & Event Listeners| Dashboard
    SecRepo -->|Flow Updates| FlowsUI
    SecRepo -->|Alert Updates| AlertsUI
    Simulator -->|Injects Synthetic Attack Vectors| SecRepo
```

---

## Traffic Interception & Forwarding Pipeline

```mermaid
sequenceDiagram
    autonumber
    actor App as Client Application
    participant Tun as Android TUN Interface
    participant Hev as HevSocks5Tunnel Native Engine
    participant Proxy as DirectSocks5Server (Loopback)
    participant Sec as SecurityRepository
    participant AI as CybersecurityAIEngine
    participant Remote as Destination Host / DNS

    App->>Tun: TCP / UDP Outbound Packets
    Tun->>Hev: Intercept Packets
    Hev->>Proxy: SOCKS5 Handshake & Request (CONNECT / ASSOCIATE)
    
    Proxy->>Sec: Check Blocklist (isTrafficBlocked)
    alt Destination IP/Port Blocked
        Proxy-->>Hev: Connection Refused
        Proxy-->>Sec: Record Blocked Threat
    else Allowed
        Proxy->>Remote: Open Socket with VpnService.protect()
        Proxy-->>Hev: SOCKS5 Success Response
        Proxy->>Sec: processPacket(NetworkPacket metadata)
        
        par Traffic Relay
            Proxy<<->>Remote: Bi-directional Raw Stream / Datagrams
        and Edge AI Evaluation (Background Thread)
            Sec->>AI: analyzeFlow(finalFlow, packetSnapshot)
            AI->>Sec: Alert Detected (if anomaly or malicious signature)
            opt Alert Triggered
                Sec->>Sec: Update Statistics & Block Malicious Host
            end
        end
    end
```

---

## On-Device AI & Threat Detection Engine

The AI subsystem operates purely on device with zero cloud latency and no privacy risks.

```mermaid
flowchart TD
    PacketIn["Incoming Packet Metadata (NetworkPacket)"] --> Agg["Flow Aggregation (NetworkFlow)"]
    Agg --> Cache["Sliding Window (recentPackets)"]
    
    Agg --> Extractor["Feature Extractor"]
    Cache --> Extractor
    
    subgraph Features ["Feature Vector (12 Dimensions)"]
        F1["Packet & Byte Rates"]
        F2["Inter-Arrival Time (Mean, Var, CV)"]
        F3["Shannon Entropy (Payload & DNS Domains)"]
        F4["SYN / ACK Flag Ratios"]
        F5["Outbound / Inbound Byte Ratio"]
        F6["Port Distribution Entropy"]
    end
    
    Extractor --> Features
    
    Features --> IsoModel["Isolation Forest Model (Unsupervised Anomaly)"]
    Features --> Baseline["BaselineProfiler (Dynamic EMA Z-Scores)"]
    
    IsoModel --> Score["Calculate Anomaly Score (0.0 - 1.0)"]
    
    Score --> Decision{"Evaluate Threat Logic"}
    Features --> Decision
    
    Decision -->|Low CV, Regular Interval, High Mean IAT| C2["MALWARE_C2_BEACONING (T1071.001)"]
    Decision -->|High Shannon Entropy > 4.2 in Subdomain| DNS["DNS_TUNNELING (T1071.004)"]
    Decision -->|Rapid Probes across >= 5 Ports| PortScan["PORT_SCAN (T1046)"]
    Decision -->|Packet Rate > 35 pkt/s, SYN/ACK > 4.0, SYN >= 10| SynFlood["SYN_FLOOD_DOS (T1498)"]
    Decision -->|Stratum JSON-RPC Mining Signatures| Miner["CRYPTO_MINING (T1496)"]
    Decision -->|Z-Score > 3σ & Isolation Score >= 0.78| AnomVol["ANOMALOUS_TRAFFIC_VOLUME (T1030)"]
    Decision -->|Outbound/Inbound > 20:1 & High Outbound Vol| Exfil["DATA_EXFILTRATION (T1048)"]
    Decision -->|No Thresholds Exceeded| Normal["Classification: Normal Traffic"]
```

---

## Attack Simulator

The built-in Attack Simulator allows testing the detection engine, dashboard counters, flow list, and alert feeds on demand without external offensive tools:

```mermaid
graph LR
    subgraph AttackSimulator ["AttackSimulator"]
        PS["Port Scan Simulation"]
        C2["Malware C2 Beaconing"]
        DT["DNS Tunneling Simulation"]
        SF["SYN Flood Simulation"]
        CM["Cryptomining RPC Simulation"]
        NT["Benign Normal Traffic"]
    end

    subgraph SecurityCore ["Core Engine"]
        Reset["Session Unblock & Deduplication Reset"]
        Ingest["SecurityRepository.processPacket()"]
        Detect["Intrusion & Malware Evaluators"]
        Alert["ThreatAlert & Live Flow Broadcast"]
    end

    PS --> Reset
    C2 --> Reset
    DT --> Reset
    SF --> Reset
    CM --> Reset
    NT --> Reset

    Reset --> Ingest --> Detect --> Alert
```

- **Port Scan**: Simulates rapid vertical reconnaissance probes across 10 common service ports (21, 22, 23, 80, 443, 8080, 4444, 3389, 5900, 8443) to trigger `PORT_SCAN` alerts and auto-block the attacking host.
- **C2 Beaconing**: Injects clock-regular heartbeat transmissions (inter-arrival time CV < 0.15) to simulate Command & Control agent telemetry (e.g., Cobalt Strike, Mythic).
- **DNS Tunneling**: Transmits high-entropy base64/hex payload subdomains to simulate exfiltration utilities (iodine, dnscat2).
- **SYN Flood**: Sends a burst of 35 SYN packets at sub-15ms intervals to validate Denial of Service detection.
- **Cryptominer**: Emits Stratum JSON-RPC subscription payloads (`mining.subscribe`) on pool ports (3333) to trigger malware resource-hijacking alarms.
- **Benign Traffic**: Simulates clean DNS queries and HTTPS flows to calibrate the dynamic baseline profiler.

---

## Current Status & Verification

- **Real-Time Monitoring**: Direct SOCKS5 proxy running on loopback forwards all TCP and UDP traffic with zero external dependencies.
- **Edge AI Evaluation**: Isolation Forest and rule heuristics evaluate every flow in background executors without dropping frame rates.
- **Interactive UI**:
  - **Dashboard**: Live shield toggle, packet counter, active flow tally, threat counters, anomaly meter, and device throughput gauges.
  - **Alerts**: Real-time threat feed with MITRE ATT&CK mappings, AI forensic diagnosis, and one-tap IP blocking.
  - **Flows**: Live flow monitor with classification status, port endpoints, and packet volume.
  - **Simulator**: Test interface with immediate feedback.
  - **Assistant**: Offline cybersecurity assistant and incident response advisor.

---

## Build & Run

### Prerequisites
- JDK 17
- Android SDK Platform 37 (Min SDK: 33)
- Android Gradle Plugin 9.5.2+
- Android Studio Ladybug / Meerkat or later

### Build Debug APK
```powershell
.\gradlew.bat app:assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Install to Connected Device / Emulator
```powershell
.\gradlew.bat app:installDebug
```

---

## License & Third-Party Credits
- **HevSocks5Tunnel**: Native TUN/SOCKS5 forwarder engine, licensed under the MIT License. See [app/src/main/assets/THIRD_PARTY_NOTICES.txt](app/src/main/assets/THIRD_PARTY_NOTICES.txt).
