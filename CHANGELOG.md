# Changelog

All notable changes to this project are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
uses [Semantic Versioning](https://semver.org/). For a Minecraft mod that means **MAJOR** breaks
saved data, config or the datapack schema; **MINOR** adds features; **PATCH** fixes bugs.

> **Pre-1.0.** The mod targets a beta NeoForge line, and persistence and the dialogue datapack
> schema are not designed yet. Expect breaking changes between minor versions until 1.0.0.

## [Unreleased]

## [0.8.0] - 2026-08-09

Find out what happened while you were away.

Losing a villager to a zombie while you are off mining is the sort of thing worth hearing about
at the time, rather than days later when you notice the beds are empty.

### Added

**Villager notifications**

- You are told when a villager near you is **killed**, and by what. "Mira the Librarian was
  killed by a Zombie" means something rather different from "Mira the Librarian has died", and
  the difference is usually the one you needed to know.
- You are told when a villager near you is **born**, named after the parents.
- Villagers you have not been introduced to are described by their trade, just as they are
  everywhere else. Two players standing together may see the same event worded differently,
  depending on who they have met.
- Messages go to chat, so you can scroll back to something you missed.

**Two switches, because the two questions are different**

- Whoever runs the world decides whether these are announced at all, and how far they carry
  (128 blocks by default).
- You decide whether you want to see them, regardless of what anyone else prefers.

### Notes

- Network protocol version raised to `7`. Clients and servers must both be on 0.8.0.
- Requires Minecraft 26.2 and NeoForge 26.2.0.48-beta, as before.

## [0.7.0] - 2026-08-09

Settings, so you can have it your way.

Ten things the mod used to decide for you are now yours to decide. Reachable from
**Mods → Villager Enhanced → Config**, no file editing required.

### Added

**Your settings** — personal, and they affect only you

- **Voice volume.** The mumble was too quiet; it is louder by default and now adjustable. Set
  it to 0 to silence villagers entirely.
- **Dialogue speed.** How quickly lines appear, or 0 to show whole answers at once. Villagers
  still speak when it is off — once per answer rather than once per line.

**World settings** — set by whoever runs the world, and shared by everyone on it

- **Turn the dialogue off entirely.** Right-clicking a villager goes back to plain vanilla
  trading, with the mod installed but dormant. Useful alongside other mods that change villager
  interaction.
- **Sneak to trade**, on or off.
- **Whether a villager is occupied for a whole conversation.** On a busy server you may prefer
  they are not, so several players can talk to the same villager.
- **Conversation range**, how far you can wander before a villager gives up on you.
- **Reputation gained per gift**, how many rumours a villager passes on, and how far their
  acquaintances reach.
- **Whether the reputation page shows the raw score** beside the tier, or just the tier.

**A mod icon**, so the mod is recognisable in the Mods list.

### Notes

- Requires Minecraft 26.2 and NeoForge 26.2.0.48-beta, as before.
- No protocol change this time; 0.7.0 clients and servers talk to 0.6.0 ones.
- Gift value only changes how quickly a villager's opinion tops out, never how high. Reaching
  the highest regard still takes more than generosity.

## [0.6.0] - 2026-08-09

Conversations that look and sound like conversations.

No new things to ask about this time. This is the pass that makes the ones already there feel
like talking to somebody.

### Changed

**The dialogue window was reworked**

- What the villager says is now the clearest thing on screen, set in its own panel below a
  divider, with their trade and level stepped back out of the way. Previously the speech was
  the same shade as the label above it and got lost in the header.
- Speech reads left to right like prose rather than being centred like a heading.
- Long answers wrap inside the panel instead of running to the screen edges.

**Fewer choices at once**

- The greeting had grown to eight buttons. The four questions now sit behind a single
  **"Ask about…"**, leaving the greeting to the things you actually *do* — trade, offer a gift,
  and leave.
- **Back** returns to the list you were in rather than jumping to the start of the conversation.

### Added

**Villagers speak rather than appear**

- Lines arrive one at a time with a quiet mumble, so an answer unfolds instead of being posted.
  Click or press a key to hear the rest at once.
- Longer answers no longer shove the buttons around while they arrive.

**Screen reader support**

- The narrator now reads the villager's name, their trade, and what they actually said.
  Previously it read the name and stopped.
- Options you cannot use are described along with the reason, since they cannot be reached with
  a keyboard to be read any other way.

### Notes

- Network protocol version raised to `6`. Clients and servers must both be on 0.6.0.
- Requires Minecraft 26.2 and NeoForge 26.2.0.48-beta, as before.
- The mumble is on the quiet side. It becomes an adjustable setting in 0.7.0 rather than being
  guessed at again here.

## [0.5.0] - 2026-08-09

Villagers tell you about themselves.

Until now a villager's job was a word under their name. Now you can ask about it — and if they
trust you enough, ask what they are carrying.

### Added

**"What do you do?"**

- Every profession has something to say about their trade, in their own words. Farmers,
  librarians, weaponsmiths, and the rest — including nitwits, who are evasive about it, and the
  unemployed, who have not settled on anything yet.
- Anyone who trades will also tell you how far along their trade they are, and whether there is
  better work in them yet.
- Anyone will answer this, whatever they think of you. Asking someone about their work is not a
  favour.

**"What are you carrying?"**

- Minecraft villagers have always carried a real inventory — bread, seeds, crops they have
  gathered. They eat from it, plant from it, and hand food to villagers who want to breed. You
  have never been able to see it.
- Now you can ask, but only if they properly trust you. This is the first thing that being
  Trusted unlocks.
- Expect most villagers to be carrying nothing much. That in itself tells you something: the
  one with twenty bread is the one about to raise a family.

### Notes

- Network protocol version raised to `5`. Clients and servers must both be on 0.5.0.
- Requires Minecraft 26.2 and NeoForge 26.2.0.48-beta, as before.

## [0.4.0] - 2026-08-06

Villagers remember you — and decide how much to tell you.

Names are no longer handed out with the window. A villager you have never spoken to is just
"the Farmer" until you ask, and whether they answer depends on what they make of you.

### Added

**Introductions**

- Villagers are shown by their trade until they give you their name. Ask, and the window
  changes in front of you.
- A villager who resents you will not introduce themselves. An ordinary stranger will, so
  nothing about names is locked behind grinding reputation.
- Name a villager with a name tag and you skip all of this, as you would expect.

**Memory**

- Villagers remember who they have spoken to, and it survives world reloads and server
  restarts.
- Greetings reflect that: guarded from someone who has not introduced themselves, warmer once
  they know you, and a remark on the gap if you have been away a few days.

**Willingness**

- What a villager will do for you now depends on how they feel about you. Gossip about the
  neighbours is something they share with people they like, not with strangers — trade with
  them a few times first.
- Two things are always available, whatever they think of you: you can always offer a gift, so
  there is a way back from a bad reputation, and you can always ask how they see you, so you
  are never left guessing.
- Options you cannot use yet are shown greyed with an explanation, rather than hidden.

### Notes

- Network protocol version raised to `4`. Clients and servers must both be on 0.4.0.
- Requires Minecraft 26.2 and NeoForge 26.2.0.48-beta, as before.

## [0.3.0] - 2026-08-06

Villagers have names, and they talk about you behind your back.

Minecraft villagers have always gossiped. Each one forms its own opinion of you, and they pass
those opinions around when they meet. You have never been able to see any of it. Now you can ask.

### Added

**Names**

- Every villager has a name. Names are distinct within a village, so you can tell one farmer
  from another.
- Villages in different biomes draw on different names, so a desert settlement sounds unlike a
  taiga one.
- Names are remembered. They survive world reloads, and a villager who is zombified and later
  cured comes back as the person you knew.
- Name a villager with a name tag and that name wins, as you would expect.
- The trade screen is now titled with the villager's name rather than their job.

**Rumours**

- New **"What do the others say about me?"** option. The villager tells you what nearby
  villagers think of you, naming them so you know who to thank — or who to apologise to.
- Up to four opinions, strongest first. Someone who resents you surfaces just as readily as
  someone who admires you.
- The interesting part: villagers you have **never met** can still have an opinion, because
  they heard it from someone who has. Trade fairly with one farmer and word gets round. Hurt
  someone and word gets round just as fast.

### Changed

- Villagers now carry saved data of their own. Existing worlds are fine — villagers are named
  the first time you meet them, with nothing to convert.
- Dialogue can show several lines of speech, which is what makes rumours readable.

### Notes

- Network protocol version raised to `3`. Clients and servers must both be on 0.3.0.
- Requires Minecraft 26.2 and NeoForge 26.2.0.48-beta, as before.

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

[Unreleased]: https://github.com/TheMrPuffin/villager-enhanced/compare/v0.8.0...HEAD
[0.8.0]: https://github.com/TheMrPuffin/villager-enhanced/releases/tag/v0.8.0
[0.7.0]: https://github.com/TheMrPuffin/villager-enhanced/releases/tag/v0.7.0
[0.6.0]: https://github.com/TheMrPuffin/villager-enhanced/releases/tag/v0.6.0
[0.5.0]: https://github.com/TheMrPuffin/villager-enhanced/releases/tag/v0.5.0
[0.4.0]: https://github.com/TheMrPuffin/villager-enhanced/releases/tag/v0.4.0
[0.3.0]: https://github.com/TheMrPuffin/villager-enhanced/releases/tag/v0.3.0
[0.2.0]: https://github.com/TheMrPuffin/villager-enhanced/releases/tag/v0.2.0
[0.1.0]: https://github.com/TheMrPuffin/villager-enhanced/releases/tag/v0.1.0
