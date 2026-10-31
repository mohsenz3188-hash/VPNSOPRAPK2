---
name: Cyberpunk Neon HUD
colors:
  surface: '#121318'
  surface-dim: '#121318'
  surface-bright: '#38393f'
  surface-container-lowest: '#0d0e13'
  surface-container-low: '#1a1b21'
  surface-container: '#1e1f25'
  surface-container-high: '#292a2f'
  surface-container-highest: '#34343a'
  on-surface: '#e3e1e9'
  on-surface-variant: '#b9cac4'
  inverse-surface: '#e3e1e9'
  inverse-on-surface: '#2f3036'
  outline: '#83948f'
  outline-variant: '#3a4a46'
  surface-tint: '#00dfc1'
  primary: '#d7fff3'
  on-primary: '#00382f'
  primary-container: '#00f5d4'
  on-primary-container: '#006c5c'
  inverse-primary: '#006b5b'
  secondary: '#dbb8ff'
  on-secondary: '#470083'
  secondary-container: '#6807ba'
  on-secondary-container: '#d0a6ff'
  tertiary: '#e4ffd6'
  on-tertiary: '#053900'
  tertiary-container: '#34fc0d'
  on-tertiary-container: '#106f00'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#26fedc'
  primary-fixed-dim: '#00dfc1'
  on-primary-fixed: '#00201a'
  on-primary-fixed-variant: '#005144'
  secondary-fixed: '#efdbff'
  secondary-fixed-dim: '#dbb8ff'
  on-secondary-fixed: '#2b0052'
  on-secondary-fixed-variant: '#6600b7'
  tertiary-fixed: '#79ff5b'
  tertiary-fixed-dim: '#2ae500'
  on-tertiary-fixed: '#022100'
  on-tertiary-fixed-variant: '#095300'
  background: '#121318'
  on-background: '#e3e1e9'
  surface-variant: '#34343a'
typography:
  headline-xl:
    fontFamily: Space Grotesk
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
  headline-xl-mobile:
    fontFamily: Space Grotesk
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  mono-lg:
    fontFamily: JetBrains Mono
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 22px
  mono-md:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
  mono-sm:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
  label-caps:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 12px
    letterSpacing: 0.08em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-sm: 0.75rem
  margin: 1rem
  margin-desktop: 2rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
  space-2xl: 3rem
---

## Brand & Style

This design system channels an advanced, terminal-grade cyber-surveillance countermeasure interface. Designed for privacy-conscious engineers, network specialists, and digital nomads, it combines the tactile discipline of high-performance proxy engines (v2rayNG, Clash, Sing-box) with an immersive cyberpunk aesthetic.

The visual style merges tactical HUD telemetry, crisp glassmorphism, and controlled chromatic luminescences against void-black depth. It feels surgical, defensive, and lightning-fast. Rather than decorative clutter, neon accents strictly represent real-time signal conditions: connection throughput, encrypted socket lifecycles, and millisecond latencies. The interface creates an impression of total control over one’s cryptographic footprint.

## Colors

The palette leverages an ultra-deep obsidian baseline to give neon emission maximum contrast without eye strain:

- **Canvas & Obsidian Bases**: Base background sits at `#0A0B10`, progressing into `#10121A` for mid-plane routing panels, and `#161926` for actionable overlays and modal surfaces.
- **Electric Cyan (`#00F5D4`) & Pulse Blue (`#00B4D8`)**: Primary command accents. Represents the primary tunnel active state, active telemetry streams, and core action triggers.
- **Laser Violet (`#7928CA`) & Neon Magenta (`#B5179E`)**: Secondary hierarchy. Used for cryptographic protocol chips (VMess, VLESS, Trojan, Shadowsocks), rule-routing graphs, and selector states.
- **Signal Green (`#39FF14`) & Emerald (`#10B981`)**: Dedicated network health tokens. Indicates ping sub-80ms, stable handshakes, and live bypass routes.
- **Warning & Hazard (`#FF0055`, `#FFBE0B`)**: Extreme latency (>300ms), DNS leak warnings, and dropped socket indicators.
- **Text & Telemetry Grays**: High-contrast text uses `#F0F4FC`. Neutral telemetry labels use `#626D8A`, with interactive dividers at `#1D2236`.

## Typography

The typographic hierarchy enforces a three-tier operational taxonomy:

1. **Display & Engine Headers (Space Grotesk)**: Angular geometric forms deliver a modern sci-fi authority for screen titles, connection status readouts, and global statistics.
2. **Interface Body & System Messaging (Inter)**: Clean, neutral legibility prevents visual fatigue across dense routing rule lists and complex protocol configurations.
3. **Telemetry & Cryptographic Metadata (JetBrains Mono)**: Pure monospaced precision applied to millisecond pings, upload/download speedmeters, IP addresses, port numbers, hash signatures, and protocol labels. Letter-spacing is slightly widened on micro labels to maintain crisp readability on small mobile viewports.

## Layout & Spacing

The layout model balances high data density with rapid thumb reach on mobile devices:

- **Mobile Grid**: A flexible 4-column structure spanning edge-to-edge with standard `16px` (`1rem`) outer margins. Dense server lists collapse inner gutter distances to `12px` (`0.75rem`) to fit multiple data points (flag, node name, protocol, ping, packet loss) in a single card row without truncation.
- **Vertical Flow**: A 4px baseline sub-rhythm governs compact technical components. Vertical gap tokens:
  - Micro-stacking (Latency to Server Title): `space-xs` (4px).
  - Intra-card padding: `space-md` (16px).
  - Section separation (e.g., Active Node to Routing Mode Switcher): `space-lg` (24px).
  - Main trigger clearance: `space-xl` (32px).
- **Tablet & Desktop Reflow**: Above 768px, layout expands into an 8-to-12 column multi-pane layout: connection status with HUD rings on the left; node selection, routing maps, and live socket logs pinned side-by-side.

## Elevation & Depth

Visual hierarchy uses dark glass translucency accented by targeted chromatic edge lights and neon backdrops:

- **Level 0 (Abyssal Substrate)**: `#0A0B10`. Flat canvas for system background.
- **Level 1 (Telemetry Panels & Cards)**: Background `#10121A` at 85% opacity, paired with `backdrop-filter: blur(16px)` and a subtle `1px` inner boundary of `rgba(255, 255, 255, 0.05)`.
- **Level 2 (Active/Selected Containers)**: Background `#161926` at 90% opacity, bordered with a 1px directional stroke transitioning from `rgba(0, 245, 212, 0.4)` to `rgba(121, 40, 202, 0.1)`.
- **Level 3 (Overlays, Bottom Sheets & Command Palettes)**: Surface `#121520` with `backdrop-filter: blur(24px)`, framed by a crisp `1px` border of `rgba(0, 245, 212, 0.25)` and a soft ambient glow: `box-shadow: 0 0 30px rgba(0, 245, 212, 0.08)`.
- **Neon Emission Shadows**: Interactive triggers utilize vibrant, concentrated drop glows rather than muddy shadows:
  - Active Switch Glow: `0 0 20px rgba(0, 245, 212, 0.45), 0 0 40px rgba(0, 245, 212, 0.2)`.
  - Disconnect / Alert Glow: `0 0 20px rgba(255, 0, 85, 0.45)`.

## Shapes

The design language favors precision engineering over soft consumer roundness. A base index of `1` (Soft, 4px - 12px) provides crisp, structural contours:

- **HUD Outer Panels & Server Cards**: `rounded-md` (`8px`) or `rounded-lg` (`12px`) with micro-notched or chamfered cues on visual dividers.
- **Data Tags, Monospace Badges & Chips**: `rounded-sm` (`4px`) to preserve a tactile hardware feel.
- **Power Button / Core Node Orbital**: A strict circle (`50%` radius), enclosed by dual concentric glowing telemetry rings, serving as the interface's primary focal anchor.
- **Sliders & Indicators**: Squarish or pill caps with crisp endpoints for definite spatial tracking.

## Components

### Master Connection Orb & Rings
The primary mobile control. A centered circular actuator surrounded by concentric SVG rings displaying real-time handshake activity:
- **Idle State**: Muted dark boundary (`#1D2236`) with slow pulse.
- **Connecting State**: Clockwise rotating gradient trace (`#7928CA` to `#00F5D4`).
- **Secured State**: High-intensity outer ring `#00F5D4` with internal radial glow (`rgba(0, 245, 212, 0.15)`), displaying upload/download vector speeds directly below the orb.

### Server & Proxy Node Cards
- Compact horizontal rows with `8px` corner radius.
- Left edge features a 2px vertical neon status track: `#39FF14` (ping < 100ms), `#FFBE0B` (100–250ms), `#FF0055` (> 250ms or timeout).
- Displays country code flag, server alias (`Space Grotesk`, 14px), protocol pill tag (VMess, Trojan, VLESS in laser violet), and numeric ping tag (`JetBrains Mono`, 12px).
- Selected node gains an electric cyan border outline (`#00F5D4`) with `0 0 12px rgba(0, 245, 212, 0.2)`.

### Buttons & Quick Actions
- **Primary Cyber Button**: High-visibility `#00F5D4` fill with jet-black typography (`#0A0B10`), bold weight, with an external cyan shadow.
- **Secondary Ghost Button**: Transparent core, 1px stroke of `rgba(0, 245, 212, 0.4)`, hover/press transitions into laser purple hue with neon edge illumination.
- **Danger Button**: Carbon black background with hot crimson stroke (`#FF0055`) and text.

### Monospace Latency Chips
- Micro tags (`JetBrains Mono`, 10px uppercase).
- Encased in a `4px` rounded badge with semi-transparent tinted backgrounds matching their severity: `rgba(57, 255, 20, 0.1)` for green, `rgba(255, 0, 85, 0.1)` for red.

### Network Traffic Speedometer & Log Ticker
- Real-time chart backed by a subtle neon-grid raster.
- Downlink rendered in `#00F5D4` wave line with area fill gradient (`opacity 0.25` to `0.0`).
- Uplink rendered in `#B5179E` stroke.
- Live socket connections feature auto-scrolling terminal logs set in `JetBrains Mono` sm (`#626D8A`), highlighting IP handshakes in white and encrypted domains in primary cyan.

### Form Inputs & Config Fields
- Input backgrounds use `#10121A` with a `1px` subtle rim of `#1D2236`.
- Focus state instantly activates a sharp `#00F5D4` border and a mini cyan HUD bracket at the top-left label marker.
- Monospace font input for subscription URLs, public keys, and routing regex rules.