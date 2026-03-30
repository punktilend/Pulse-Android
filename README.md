# AtomicBlast

Personal self-hosted music streaming system. Streams FLAC and other audio from a private Backblaze B2 bucket across three clients — a car head unit (Android), a Windows desktop (Electron), and a browser (Firefox extension). No streaming services, no subscriptions.

---

## Projects

| Project | Path | Platform |
|---------|------|----------|
| **AtomicBlast-Android** | `C:\Users\adamm\AndroidStudioProjects\AtomicBlast-Android\` | Android (ATOTO S8 car head unit) |
| **AtomicBlast-Win** | `C:\Users\adamm\AtomicBlast-Win\` | Windows (Electron desktop app) |
| **AtomicBlast-Extension** | `C:\Users\adamm\AtomicBlast-Extension\` | Firefox browser extension |

---

## Shared Infrastructure

All three clients talk to the same backend:

### Backblaze B2
- **Bucket:** `aharveyGoogleDriveBackup`
- **Music prefix:** `Music/`
- **Structure:** `Music/Artist/Year - Album/tracks + cover.jpg`
- **Artist art:** `Music/Artist/artist.jpg`
- **Key name:** PulseKey (ID starts with `0055a9c537f296d0`)
- Credentials stored locally — never committed

### Proxy Server (`23.95.216.131`)
- Node.js app at `/opt/pulse-proxy/server.js`, managed by PM2 on port 3000
- **Transcodes** audio on the fly: FLAC passthrough, MP3 320k/192k, AAC 128k
- **Favorites** API — shared across all clients (`GET/POST/DELETE /favorites`)
- Stream endpoint: `GET /stream?file=<path>&quality=<flac|high|medium|low>`

### Stream Quality Options
| Label | Format | Bitrate |
|-------|--------|---------|
| FLAC (Direct) | FLAC | Lossless |
| High | MP3 | 320 kbps |
| Medium | MP3 | 192 kbps |
| Low | AAC | 128 kbps |

### Automation Pipeline
```
Torrent downloads to /crowbox/crowbox/seedbox/ (rclone-mounted B2)
  → qBittorrent AutoRun → /root/upload-to-b2.sh
  → /root/organize-music.sh "$TORRENT_NAME"
    → Parse artist/album/year from folder name
    → rclone copy (server-side B2) → Music/Artist/Year - Album/
    → Last.fm API → artist.jpg + cover.jpg (if missing)
  → qBittorrent keeps seeding from seedbox/
```

**Last.fm API key:** `d67dea9be32d3f2510ef5cde2db140fb`
**Server logs:** `/var/log/seedcrow.log`, `/var/log/organize-music.log`

---

## AtomicBlast-Android

Primary client — runs on an **ATOTO S8** car head unit (1024×600, landscape).

### Stack
- Kotlin + Jetpack Compose (Material3)
- Media3 / ExoPlayer with `MediaSession` (car controls, AVRCP, audio focus)
- Coil for image loading
- OkHttp for B2 API and proxy calls

### Structure
```
app/src/main/java/com/pulse/android/
├── data/
│   ├── B2Repository.kt       — B2 auth, listFiles(), listAllFiles(), stream URLs
│   ├── FavoritesRepository.kt — Favorites CRUD via proxy API
│   └── Models.kt              — Track, Album, Artist, NowPlaying, StreamQuality
├── player/
│   └── PlaybackService.kt    — (reserved, not currently active)
├── ui/
│   ├── PulseApp.kt           — Nav graph, sidebar (Artists/Albums/Favorites/Cloud/Settings)
│   ├── components/
│   │   └── NowPlayingBar.kt  — Persistent bottom player bar
│   └── screens/
│       ├── DashboardScreen.kt   — Artist grid + Shuffle All
│       ├── AlbumsScreen.kt      — Flat album list + Shuffle All
│       ├── CloudScreen.kt       — Browse B2 folders (breadcrumb nav, favorites, shuffle)
│       ├── FavoritesScreen.kt   — Favorited tracks list
│       ├── NowPlayingScreen.kt  — Full player (seek, queue, favorites, car controls)
│       └── SettingsScreen.kt    — Theme, stream quality
└── viewmodel/
    └── PlayerViewModel.kt    — Shared state, ExoPlayer, MediaSession, queue management
```

### Build Config (`local.properties`)
```
B2_KEY_ID=...
B2_APP_KEY=...
B2_BUCKET=aharveyGoogleDriveBackup
B2_PREFIX=Music/
PROXY_URL=http://23.95.216.131:3000
```

### Key Design Notes
- `MediaSession` wraps ExoPlayer in `PlayerViewModel` — exposes playback to car AVRCP controls, notification, and headset buttons
- Full queue loaded into ExoPlayer at once so native skip (including Bluetooth controls) works
- Artwork folders (`scans`, `artwork`, `covers`, etc.) filtered from folder listings
- `OkHttpClient` shared between B2Repository and FavoritesRepository via ViewModel

---

## AtomicBlast-Win

Windows desktop client built with **Electron**. Supports both local music files and B2 streaming.

### Stack
- Electron (Node.js + Chromium)
- `music-metadata` for local file tag reading
- `hls.js` for streaming
- Internal HTTP API server for renderer ↔ main process communication

### Run
```bash
cd C:\Users\adamm\AtomicBlast-Win
npm start
```

### Features
- B2 cloud browsing (same bucket/prefix as Android)
- Local music library support (`config.json → musicPaths`)
- Media key support (play/pause/next/prev via keyboard media keys)
- Favorites synced with proxy server
- Stream quality selection (FLAC / MP3 320k / MP3 192k / AAC 128k)
- yt-dlp integration (`config.json → ytdlpPath`)

### Config (`config.json`)
```json
{
  "musicPaths": [],
  "ytdlpPath": "yt-dlp"
}
```
B2 credentials are hardcoded in `main.js` — keep the file private.

---

## AtomicBlast-Extension

**Firefox** browser extension. Popup player for browsing and streaming the B2 library without leaving the browser.

### Install (development)
1. Open Firefox → `about:debugging`
2. Click **Load Temporary Add-on**
3. Select `C:\Users\adamm\AtomicBlast-Extension\manifest.json`

### Features
- B2 folder browser with breadcrumb navigation
- Now-playing bar with seek, prev/next, shuffle
- Stream quality selector
- Favorites (♥) synced with proxy server
- Search/filter within current folder

### Files
```
AtomicBlast-Extension/
├── manifest.json     — Firefox WebExtension manifest (v2)
├── background.js     — Service worker: B2 auth, playback state, proxy calls
├── popup.html/css/js — UI rendered in the extension popup
└── icons/            — Extension icon assets
```

### Permissions
- `https://api.backblazeb2.com/*` — B2 API auth and file listing
- `https://*.backblazeb2.com/*` — Direct FLAC downloads
- `http://23.95.216.131:3000/*` — Proxy transcoding + favorites API
- `storage` — Persisting auth tokens between sessions

---

## Server Reference

| Service | Details |
|---------|---------|
| OS | Ubuntu 20.04 |
| rclone mount | `crowbox:` → `/crowbox/` via systemd `rclone-crowbox.service` |
| qBittorrent | systemd, port 8080, saves to `/crowbox/crowbox/seedbox/` |
| pulse-proxy | Node.js, PM2, port 3000 — `/opt/pulse-proxy/server.js` |
| Cron | Nightly 3am full scan via `organize-music.sh` |
