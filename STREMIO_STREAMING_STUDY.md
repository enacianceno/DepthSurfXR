# How Stremio plays torrents, and what DepthSurf adopts

Study of [Stremio on GitHub](https://github.com/Stremio) (Oct 2026) to answer:
why does tapping a magnet on a web page fail with "video not supported", and
how does Stremio make the same magnet play.

## 1. Stremio's architecture: torrent -> local HTTP server -> native player

Stremio never plays a torrent file directly and never plays it inside a web
page. The chain is always:

```
magnet / infoHash
  -> streaming engine (torrent client, sequential download)
  -> local HTTP server (http://127.0.0.1:PORT/<infoHash>/<fileIndex>)
  -> platform player (mpv / libVLC / HTML5 video)
```

Key repos (all github.com/Stremio):

- `server.js` — the streaming server (Node): resolves infoHash to files,
  serves pieces over HTTP with Range support, remuxes/transcodes when the
  player needs it. The desktop shell spawns one per session (see
  `stremio-shell` README: `--streaming-server`, `--development` skips it).
- `enginefs` — "simple layer to use and manage torrent-stream engines and
  access them through HTTP": same pattern, documented example is
  `vlc http://localhost:10000/<hash>/0`. Engines die after 2 min idle.
- `stremio-video` — player abstraction with one backend per platform:
  `ShellVideo` (mpv, desktop), `HTMLVideo` (web), `ChromecastSenderVideo`,
  `TizenVideo`, `TitanVideo`, ...
- `stremio-core` (+ `stremio-core-kotlin` for Android) — catalog, addons,
  library; playback itself stays in the platform player.
- `vlc-android-sdk` (JitPack `com.github.stremio:vlc-android-sdk:8.0.0`,
  libvlc from VLC-Android 3.4.3) — Stremio's Android player: full codec
  coverage, hardware accel, plays HTTP(S) progressively.
- `rar-http` — streams scene RAR sets over HTTP (many torrents are RARed).
- `ffmpeg-android-maker` / `ffmpeg-kit` — FFmpeg builds for remux/transcode.

Why HTTP and not the file: the download is incomplete and growing. A local
server answers Range requests from whatever is already fetched, prioritizes
the header + the playhead, and keeps the length story consistent. A player
pointed at a raw partial file sees a fixed length and chokes on seeks past
it; pointed at the server URL it just sees a (slow) web video.

## 2. Why the web page says "video not supported"

Torrent/trailer pages that play in-page use WebTorrent (WebRTC peers) +
`<video>`. That fails on real scene files for two boring reasons:

1. The browser/WebView build lacks the codec (HEVC, AC3/DTS audio, PGS
   subs) or the container path it needs.
2. WebTorrent needs WebRTC peers; most public swarms have none, so nothing
   arrives at all.

"Use an external player" on such pages means: hand the magnet/stream URL to
VLC/MX Player via intent — which leaves our app and loses XR/3D. DepthSurf
must take over the tap instead (section 4).

## 3. What DepthSurf does today (Fix6-8) vs Stremio

| Step | Stremio | DepthSurf |
|---|---|---|
| Fetch | streaming engine + DHT | TorrentStream 3.0.0 (libtorrent4j, sequential, largest file) |
| Serve | local HTTP, ranges, idle GC | direct partial file (gap, see 5) |
| Play (Android) | libVLC | ExoPlayer 1.11 + TextureView (VD-safe) |
| Magnet from page | n/a (native UI) | shouldOverrideUrlLoading + JS click hook (Fix9) |
| RAR sets | rar-http | not supported (documented) |
| Transcode | server.js FFmpeg | not supported (documented) |

ExoPlayer covers H.264/HEVC/VP9 in MP4/MKV/WebM progressively; that is
enough for direct (non-RAR) scene files. If a file still fails, the
documented fallback is `vlc-android-sdk:8.0.0` — verify its COPYING first:
VLC is GPL-family and our Moonlight base is GPLv3, so redistribution terms
must be checked before adopting, not after.

## 4. DepthSurf-only problem: the page eats the tap

Stremio has a native UI, so every tap is already in-app. A browser must
steal magnet taps back from pages whose JS intercepts them (embedded player
-> "not supported"). Fix9 does it at two levels:

1. Navigation level: `shouldOverrideUrlLoading` catches `magnet:`,
   `intent://...scheme=magnet` and `.torrent` navigations.
2. DOM level: injected capture-phase click listener on `a[href]` —
   `preventDefault + stopPropagation + DepthSurf.playMagnet(href)` — runs
   before page handlers, so the embedded player never starts. Delegated at
   `document`, so dynamically added links are covered; iframes are not (v1).

Direct video links (`.mp4/.mkv/.m3u8/.mpd/...`) are also taken over into
the in-app overlay, so a page's "external player" option is never needed
and playback stays in XR/3D.

## 5. Known gaps (honest)

- No local HTTP server yet: seeks past the downloaded prefix and some
  containers (moov-at-end MP4) can stall on partial files. If real magnets
  prove this out, the fix is a minimal Range-capable localhost server over
  the sequential file (Stremio's shape), not a bigger player.
- No RAR support (`rar-http` equivalent): RARed scene sets will not play.
- No transcode path: exotic codecs depend on the Quest hardware decoders.
- First magnet fetch needs DHT bootstrap (seconds to ~2 min); the overlay
  shows progress/seeds meanwhile.
