# Changelog

All notable changes to this project are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
uses [Semantic Versioning](https://semver.org/). For a Minecraft mod that means **MAJOR** breaks
saved data, config or the datapack schema; **MINOR** adds features; **PATCH** fixes bugs.

> **Pre-1.0.** The mod targets a beta NeoForge line, and persistence and the dialogue datapack
> schema are not designed yet. Expect breaking changes between minor versions until 1.0.0.

## [Unreleased]

## [0.2.0] - 2026-08-06

Reputation you can see — and change.

Villagers have always formed opinions about you. Minecraft has never shown them to you, and has
never given you a way to influence them other than trading. Now it does both.

### Added

**Seeing where you stand**

- New **"How do you see me?"** option showing how this villager regards you, as a named tier
  rather than a raw number: Reviled, Disliked, Stranger, Acquaintance, Trusted, Honoured.
- Tiers correspond to things you have actually done. Trading regularly makes you Trusted;
  curing a zombie villager is the only route to Honoured; hurting or killing a villager will
  drop you below Stranger — and villagers gossip, so their neighbours will hear about it.
- Reputation decays over time, so standing is something you maintain rather than bank.

**Giving gifts**

- New **"Offer what you're holding"** option. Villagers accept anything they would buy in their
  trades, plus any food — so what a villager wants varies with their profession and trade level.
- Food is welcome from anyone, so nitwits, the unemployed and babies can be befriended too,
  despite having nothing to trade.
- Gifts raise their opinion of you a little, and that goodwill fades more slowly than goodwill
  earned by trading. Gifting cannot take you all the way to Honoured, however generous you are.
- The villager visibly reacts — you can watch it happen behind the open dialogue.

### Changed

- The dialogue is now **multi-page**, so conversations can go somewhere and come back.
- What a villager says is now composed on the server rather than assembled by the client, and
  wraps properly across lines. Groundwork for longer, more varied dialogue.

### Notes

- Network protocol version raised to `2`. Clients and servers must both be on 0.2.0.
- Requires Minecraft 26.2 and NeoForge 26.2.0.48-beta, as before.

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

[Unreleased]: https://github.com/TheMrPuffin/villager-enhanced/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/TheMrPuffin/villager-enhanced/releases/tag/v0.2.0
[0.1.0]: https://github.com/TheMrPuffin/villager-enhanced/releases/tag/v0.1.0
