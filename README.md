# Literal Player

Browse. Play. Done.

A minimalist local audio player for Android.

<p>
  <a href="https://github.com/256x/player/releases/latest"><img src="https://img.shields.io/github/v/release/256x/player?label=GitHub%20Release"></a>&nbsp;<img src="https://img.shields.io/badge/Android-8%2B-blue">&nbsp;<img src="https://img.shields.io/badge/license-MIT-lightgrey">
</p>

[User Guide](./docs/USER_GUIDE.md)

## Features

- Browse by Artist, Album, All tracks, or Playlist
- Favorites playlists — save and organize tracks
- Playback speed control (0.8x – 2.0x)
- Skip forward/backward with short and long press
- Resume position per track
- Next/prev navigation within the current list
- Color customization — background, text, accent
- No internet required. No cloud. No tracking.

## Notes

**First launch is slow.** The app reads metadata (title, artist, album) directly from each audio file using `MediaMetadataRetriever`. With a large library this can take a minute or more. Results are cached locally, so subsequent launches are instant. Tap **Rescan media** in Settings only when you add or remove files.

## Development

- Kotlin / Jetpack Compose / Media3 (ExoPlayer)
- Target: Android 8.0+

This app was built with substantial assistance from [Claude](https://claude.ai) (Anthropic).

## License

MIT
