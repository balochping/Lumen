## What's Changed

- feat: add a main core choice in Settings → Network — Auto keeps picking the right core for each server (sing-box, Xray or a hybrid), Xray runs everything on Xray only, including TUN on Xray's own TUN inbound, and sing-box extended runs everything on sing-box only; servers the chosen core cannot run are dimmed and explain what to change
- perf: start TUN in about a second — server addresses for the route exclusions are resolved in parallel with one shared deadline instead of one by one, which took 10–20 s once the DNS cache had expired
- perf: keep the interface smooth while Zapret and TUN start or stop — Zapret stops instantly instead of freezing for about 2 s, background waits no longer block rendering, the connection spinner runs on the render thread and large log bursts are processed in small chunks
- perf: keep window resizing and the sidebar animation smooth on every page — hidden pages no longer relayout on every frame
- fix: ship wintun.dll with Xray so TUN on the Xray core works in release builds

Full Changelog: https://github.com/krambovic/Lumen/compare/v2.1.0...v2.1.1
