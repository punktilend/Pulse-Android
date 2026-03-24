# Pulse Android — Project Context

## What this is
Pulse is a personal Android music player app that streams FLAC and other audio from a private Backblaze B2 bucket. No Spotify, no streaming service — fully self-hosted personal library.

## B2 Storage Structure
- **Bucket:** `aharveyGoogleDriveBackup`
- **Music library:** `Music/Artist/Year - Album/tracks + cover.jpg`
- **Artist art:** `Music/Artist/artist.jpg`
- **Seedbox (raw torrents):** `crowbox/seedbox/TorrentName/` — kept for seeding, do not touch
- **B2 credentials:** stored in `local.properties` (never committed to git)
  - `B2_KEY_ID`, `B2_APP_KEY`, `B2_BUCKET`, `B2_PREFIX`
  - Key name: PulseKey, Key ID starts with `0055a9c537f296d0`

## Android App Structure
```
app/src/main/java/com/pulse/android/
├── data/
│   ├── B2Repository.kt     — B2 API auth, listFiles(), listAllFiles(), getStreamUrl()
│   └── Models.kt           — Track, Album, Artist, B2Config, PlayerState, NowPlaying
├── player/
│   └── PlaybackService.kt  — Media3/ExoPlayer background playback service
├── ui/
│   ├── PulseApp.kt         — Nav graph, bottom nav (Library, Cloud, Settings)
│   ├── components/
│   │   └── NowPlayingBar.kt — Persistent bottom player bar
│   └── screens/
│       ├── LibraryScreen.kt    — Sidebar: Artists, Albums
│       ├── AlbumsScreen.kt     — Flat list of all albums across all artists
│       ├── CloudScreen.kt      — Browse B2 folders (breadcrumb nav)
│       ├── DashboardScreen.kt  — Main browsing view
│       ├── NowPlayingScreen.kt — Full player screen
│       └── SettingsScreen.kt   — App settings
└── viewmodel/
    └── PlayerViewModel.kt  — Shared state, playback control, queue
```

## Server (23.95.216.131)
- **OS:** Ubuntu 20.04
- **rclone mount:** `crowbox:` mounted at `/crowbox/` via systemd `rclone-crowbox.service`
  - `/crowbox/crowbox/seedbox/` — qBittorrent downloads here, seeds from here
  - `/crowbox/aharveyGoogleDriveBackup/Music/` — organized music library
- **qBittorrent:** systemd service on port 8080, AutoRun enabled, saves to `/crowbox/crowbox/seedbox`
- **pulse-proxy:** Node.js app at `/opt/pulse-proxy/server.js`, PM2 managed, port 3000
  - Streams/transcodes audio from B2 (FLAC passthrough, MP3 320k/192k, AAC 128k)
- **upload-to-b2.sh:** `/root/upload-to-b2.sh` — called by qBittorrent on completion, calls organize-music.sh
- **organize-music.sh:** `/root/organize-music.sh` — parses torrent name, server-side rclone copies to `Music/Artist/Year - Album/`, fetches art from Last.fm
- **Logs:** `/var/log/seedcrow.log`, `/var/log/organize-music.log`, `/var/log/rclone-mount.log`
- **Cron:** nightly 3am full scan via organize-music.sh

## Last.fm API
- Key: `d67dea9be32d3f2510ef5cde2db140fb`
- App name: Pulse
- Used for: artist.jpg and cover.jpg fetched during organize-music.sh

## Pipeline (fully automated, no login needed)
```
Torrent downloads to /crowbox/crowbox/seedbox/ (already in B2 via rclone mount)
  → qBittorrent AutoRun → upload-to-b2.sh
  → organize-music.sh "$TORRENT_NAME"
    → parse artist/album/year from folder name
    → rclone copy (server-side B2) → Music/Artist/Year - Album/
    → Last.fm API → artist.jpg (if missing)
    → Last.fm API → cover.jpg (if missing)
  → qBittorrent keeps seeding from /crowbox/crowbox/seedbox/
```

## Key Design Decisions
- Files live in **both** seedbox (for seeding) and Music/ (for app) — B2 server-side copy, no re-upload bandwidth
- `local.properties` holds all secrets — never committed
- Artwork folders (Scans, Artwork 300 DPI, etc.) are filtered out from file listings
- Image files (artist.jpg, cover.jpg) are filtered from track listings — only audio shown
- Artist names like "Aquabats, The" → rearranged to "The Aquabats" for Last.fm queries

## Target Device
- ATOTO S8 (1024x600) — car head unit running Android
- Virtual AVD in Android Studio for development

## UI Style
- Green accent color on dark/light theme
- Inspired by Vinyls app (macOS) — grid artist view, folder-based album browsing
- Bottom now-playing bar always visible when something is playing
