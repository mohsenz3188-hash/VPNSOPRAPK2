# VPNSOPR Android

Android app shell based on the supplied Stitch HTML screens, with a native Android VPN service powered by sing-box `libbox`.

## Included

- Stitch screens bundled locally: dashboard, profiles, subscriptions, routing settings, rule inspector, add-override screen, terminal logs and HUD.
- Android WebView bridge for page navigation, profile import, profile selection and VPN connect/disconnect.
- Native Android `VpnService` with a foreground notification and a stop action.
- sing-box engine integration through `com.github.singbox-android:libbox:1.14.0`.
- Import of VLESS, VMess, Trojan, Shadowsocks, Hysteria2 and TUIC share links.
- Import of sing-box JSON configurations (including WireGuard outbounds) and standard WireGuard `.conf` text with one peer.
- HTTPS subscription import for plain or Base64-encoded share-link lists.

## Requirements

- Android Studio with JDK 17
- Android SDK Platform 35 and Build Tools 35.0.0
- Gradle 8.9 (or use the GitHub Actions workflow)
- Internet access on the build machine to download Android dependencies and the sing-box AAR from JitPack/GitHub Releases

## Build locally

1. Install Android Studio and JDK 17.
2. In Android Studio SDK Manager, install **Android SDK Platform 35** and **Build Tools 35.0.0**.
3. Set `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) to your Android SDK directory.
4. Extract this project folder.
5. On Windows, run `build-local.bat`; on macOS/Linux, run `./build-local.sh`.
6. The debug APK will be created at:

   `app/build/outputs/apk/debug/app-debug.apk`

You can also open the folder in Android Studio and build the `app` debug variant. The first build downloads Gradle (if not already installed), Android dependencies and the sing-box native library; it may take several minutes. The resulting APK is larger than a normal WebView app because it includes native VPN engine binaries.

## Build with GitHub Actions

1. Create a new GitHub repository and upload the contents of this folder.
2. Open the repository's **Actions** tab.
3. Select **Build VPNSOPR APK** and press **Run workflow**.
4. When the run finishes, download the `VPNSOPR-debug-apk` artifact. It contains `app-debug.apk`.

The workflow builds a debug APK; it is suitable for testing and sideloading. A release APK should be signed with a private keystore before distribution.

## First run

1. Install the APK on Android 7.0 or later.
2. Open **Profiles** and import a VPN link, subscription URL, sing-box JSON, or WireGuard `.conf` text.
3. Select the profile you want to use.
4. Return to **Home**, tap the power button, and approve Android's VPN permission prompt.
5. Use the persistent notification to stop the tunnel if needed.

A valid server profile is required. The app is a VPN client; it does not create or provide VPN servers or free server credentials.

## Supported profile formats

| Format | Import method |
|---|---|
| VLESS (including TLS / Reality / WS / gRPC) | Share link |
| VMess | Base64 `vmess://` link |
| Trojan | `trojan://` link |
| Shadowsocks | `ss://` / `shadowsocks://` link |
| Hysteria2 | `hysteria2://` / `hy2://` link |
| TUIC | `tuic://` link |
| WireGuard | sing-box JSON endpoint or standard `.conf` text |
| OpenVPN Client | sing-box JSON `openvpn-client` endpoint; direct `.ovpn` text parsing is not included yet |

For the broadest compatibility, import a complete sing-box JSON profile. Some provider-specific URI parameters and subscription formats may need additional mapping.

## Important implementation notes

- The visual Stitch screens originally contained demonstration telemetry and simulated controls. The connection dashboard's status is now driven by the Android VPN service; speed and ping fields remain placeholders until live traffic-statistics and latency sampling are connected.
- The included VPN bridge uses Android `VpnService` and sing-box `libbox`; it is not a visual-only toggle.
- The platform adapter is intentionally minimal and does not yet implement advanced interface monitoring, live rule editing, QR-camera scanning, direct `.ovpn` text parsing, or full traffic counters. Per-app include/exclude fields supplied in a sing-box JSON config are passed to Android's VPN builder.
- The HTML screens load Tailwind and web fonts from their official CDNs. The native app shell and VPN engine are local, but first display of the full design needs an internet connection for those assets.
- Keep imported VPN links and subscription URLs private. They contain server credentials.

## Licensing

The embedded sing-box `libbox` Android library is GPL-3.0-or-later. A distributed build that includes it must comply with that license and include the applicable notices and corresponding source. Review the upstream project license before redistributing or publishing the APK.
