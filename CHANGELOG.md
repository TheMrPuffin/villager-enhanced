# Changelog

All notable changes to this project are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
uses [Semantic Versioning](https://semver.org/). For a Minecraft mod that means **MAJOR** breaks
saved data, config or the datapack schema; **MINOR** adds features; **PATCH** fixes bugs.

> **Pre-1.0.** The mod targets a beta NeoForge line, and persistence and the dialogue datapack
> schema are not designed yet. Expect breaking changes between minor versions until 1.0.0.

## [Unreleased]

## [0.1.0] - 2026-08-05

First playable release. Requires Minecraft 26.2 and NeoForge 26.2.0.48-beta.

### Added

- Right-clicking a villager opens a dialogue window instead of the vanilla trade screen,
  showing the villager's name, profession and trade level.
- **Trade** hands off to the real vanilla merchant screen, applying the reputation-based
  discount vanilla would normally apply.
- **Leave** ends the conversation.
- Villagers with nothing to sell — nitwits, the unemployed, babies — show a greyed-out Trade
  option with an explanatory tooltip instead of an empty trade window.
- **Sneak + right-click** skips the dialogue and opens the trade screen directly.
- A villager is marked busy for the duration of a conversation. Other players are told so
  rather than silently interrupting.
- Conversations end cleanly when the player walks out of range, changes dimension, dies or
  disconnects, or when the villager dies or unloads — with an on-screen explanation.

### Notes

- The dialogue is server-authoritative. The server decides when a conversation starts, which
  options it offers and whether to act on a choice; every message from the client is
  re-validated against the server's own view of the world.
- Verified on both single-player and dedicated servers, including two simultaneous players.
- Wandering Traders are deliberately out of scope.

[Unreleased]: https://github.com/TheMrPuffin/villager-enhanced/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/TheMrPuffin/villager-enhanced/releases/tag/v0.1.0
