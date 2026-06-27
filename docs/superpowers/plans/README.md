# iOS Parity Implementation Plans

Execute in order. Each plan is a self-contained vertical slice.

| Plan | Slice | Issues | Description |
|------|-------|--------|-------------|
| [00-foundation](2026-06-28-plan-00-foundation.md) | 0 | #2-5, #11-14 | Streaming pipeline + theme fixes — MUST DO FIRST |
| [01-02-ui](2026-06-28-plan-01-02-ui.md) | 1-2 | #6-9 | Home screen + stream detail UX |
| [03-downloads](2026-06-28-plan-03-downloads.md) | 3 | #10 | Download pipeline |
| [04-api-settings](2026-06-28-plan-04-api-settings.md) | 4 | #20 | API key management + Torrentio config UI |
| [05-subtitles-player](2026-06-28-plan-05-subtitles-player.md) | 5 | #16, #21 | OpenSubtitles + player enhancements |
| [06-firebase-sync](2026-06-28-plan-06-firebase-sync.md) | 6 | #17 | Google Sign-In + Firestore sync |
| [07-polish](2026-06-28-plan-07-polish.md) | 7 | #15, #18, #19 | Splash + SilhouetteBackground + Format Guide |

## Dependencies
- Slice 0 must complete before anything else (streaming pipeline is a blocker)
- Slices 1-7 can run in any order after Slice 0
- Slice 5 benefits from Slice 4 (API key needed for OpenSubtitles optional login)
- Slice 6 benefits from Slice 3 (progress sync works better with downloads)

## How to execute
Use `superpowers:subagent-driven-development` skill for each plan.
