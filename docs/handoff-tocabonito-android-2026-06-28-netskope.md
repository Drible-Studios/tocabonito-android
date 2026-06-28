# Handoff: TocaBonito Android — Netskope Hook + Continued Testing

**Date:** 2026-06-28  
**Repo:** https://github.com/Drible-Studios/tocabonito-android  
**Working directory:** `/Users/JFUJIOK/projects/jeffujioka/tocabonito-android`  
**Primary device:** NVIDIA Shield TV (192.168.2.109:5555)

---

## Focus of Next Session

Create a Claude Code hook that **blocks any attempt to curl/fetch Torrentio or Real-Debrid endpoints from the Mac**. The Mac's network goes through Mercedes-Benz's Netskope corporate proxy which blocks P2P-categorized sites, causing misleading 403 errors.

Then continue E2E testing of the streaming pipeline via `/maria-ssh` or the Shield.

---

## The Problem

The dev Mac routes traffic through Netskope (Mercedes corporate proxy). When any tool (curl, Ktor, wget) tries to access:
- `torrentio.strem.fun` — blocked as "Peer-to-Peer (P2P)"
- `api.real-debrid.com` — blocked as "Peer-to-Peer (P2P)"

The response is a 403 with HTML body containing "Web Page Blocked!" and references to ServiceNOW/Netskope. This was misdiagnosed as "Cloudflare blocking" and wasted debugging time.

### Correct approach
- Use `/maria-ssh` skill to test these endpoints (Maria is on unfiltered network)
- The Shield (192.168.2.109) is on the HOME network and CAN access these directly
- Verify via `adb logcat` that the app on Shield successfully hits the endpoints

---

## What the Hook Should Do

Create a pre-command hook (in `~/.claude/hooks/` or `.claude/settings.json`) that:
1. Intercepts Bash tool calls
2. Checks if the command contains `torrentio.strem.fun` or `api.real-debrid.com` or `real-debrid.com`
3. If it does AND the command is a direct HTTP call (curl, wget, httpie, or similar), **block it** with a message like: "BLOCKED: Torrentio/Real-Debrid are blocked by Netskope on this Mac. Use /maria-ssh instead."
4. Should NOT block `adb` commands that reference these URLs (since the Shield can access them)
5. Should NOT block grep/find/read commands that just mention the URL in code

### Hook reference
- Claude Code hooks docs: `~/.claude/hooks/`
- Hook types: `PreToolUse`, `PostToolUse`, `Notification`
- For this case: `PreToolUse` on the `Bash` tool, inspect the `command` field

---

## Current State of the App

All 8 implementation plans are complete and merged. The app works on Shield:

| Feature | Status |
|---------|--------|
| Home screen (hero + trending + posters) | Working |
| Detail screen (movie + TV) | Working |
| Stream discovery (Torrentio) | Code correct, untested E2E (proxy blocked Mac tests) |
| Stream resolution (Real-Debrid) | Code correct, untested E2E |
| Playback (ExoPlayer) | Untested |
| Settings (theme, API key, Torrentio config) | Working |
| Downloads (WorkManager) | Code complete, untested E2E |
| Firebase sync | Code complete (needs google-services.json for real test) |

### Bugs Fixed This Session (pushed to main)
- `0d77cd0` — Firebase DI graceful degradation without google-services.json
- `5b98903` — TMDB TV detail + numeric IDs (was hardcoded to /movie/)
- `98550c8` — Torrentio config path wired with RD key + providers + language
- `44d9beb` — Pass IMDB ID from TMDB detail to Torrentio stream lookup

---

## Files Updated This Session

- `~/.claude/CLAUDE.md` — Added network restriction section
- `CONTEXT.md` — Added network restriction note to External Services
- Memory: `feedback_never_curl_torrentio_rd.md` — Persisted the rule

---

## Suggested Skills

- `/maria-ssh` — To test Torrentio and Real-Debrid endpoints
- `superpowers:systematic-debugging` — If streaming E2E has issues
- `git-champs` — For any commits/PRs

---

## E2E Testing Plan (After Hook)

1. Use `/maria-ssh` to verify Torrentio returns streams for Fight Club (`tt0137523`)
2. Use `/maria-ssh` to verify RD unrestrict works with a resolver URL
3. Deploy app to Shield, open Fight Club detail, confirm streams load
4. Tap a stream, verify playback starts in ExoPlayer

---

## Build & Deploy

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home gradle :app:assembleDebug
adb connect 192.168.2.109:5555
adb -s 192.168.2.109:5555 install -r app/build/outputs/apk/debug/app-debug.apk
```
