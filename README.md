# Villager Enhanced

A NeoForge mod for Minecraft that replaces the vanilla villager trade screen with a dialogue
window, laying the groundwork for a branching, reputation-aware conversation system.

> **Status: in development.** The dialogue system works end to end in single-player and on
> dedicated servers, but the mod has not had a tagged release yet.

## What it does

Right-clicking a villager opens a conversation instead of the trade GUI:

- The villager's name, profession and trade level are shown.
- **Trade** hands off to the real vanilla merchant screen, reputation discounts included.
- **Leave** ends the conversation.
- Villagers with nothing to sell — nitwits, the unemployed, babies — show a greyed-out Trade
  option with an explanation rather than an empty trade window.

**Sneak + right-click** skips the dialogue and opens the trade screen directly. Useful as a
shortcut, and as a fallback if the dialogue system misbehaves.

While a player is in conversation, the villager is marked busy and other players are told so,
mirroring how vanilla treats a villager mid-trade.

## Requirements

| | |
|---|---|
| Minecraft | 26.2 |
| NeoForge | 26.2.0.48-beta |
| Java | 25 |

NeoForge 26.2 is a **beta** line and its APIs change without deprecation cycles. Expect
breakage when bumping the loader version.

## Building

```bash
./gradlew build
```

The jar is written to `build/libs/`.

## Development

Run configurations, all available from the Gradle tool window or your IDE after a sync:

| Task | Purpose |
|---|---|
| `runClient` | Client, playing as `Dev` |
| `runServer` | Dedicated server |
| `runClientDev01` | Second client, playing as `dev01` |
| `runClientDev02` | Third client, playing as `dev02` |

The two numbered clients exist for multiplayer testing. Each needs its own username *and* its
own game directory — clients sharing one directory fight over `options.txt`, the logs and the
session lock. Start `runServer`, then connect both to `localhost`.

### Architecture

The dialogue is **server-authoritative**. The server decides when a conversation starts, what
options it offers, and whether to act on a choice; the client renders what it is told and sends
choices back. Every inbound message is re-validated against the server's own view of the world
before anything happens.

Code is split along the client/server boundary, which NeoForge enforces at class-load time:

| Package | Loaded on | Contains |
|---|---|---|
| `dialogue` | both | Interaction handling, sessions, trade handoff |
| `network` | both | Payload definitions, serverbound handlers |
| `client` | client only | Screens and clientbound handlers |

Nothing in `dialogue` or `network` may reference `client` or any `net.minecraft.client` type —
a dedicated server would crash the moment such a class loaded.

`src/main/resources/META-INF/accesstransformer.cfg` widens exactly one vanilla method,
`Villager#updateSpecialPrices`, which applies the reputation discount before trading.

## Licence

[MIT](LICENSE).

`TEMPLATE_LICENSE.txt` covers the NeoForge MDK files this project was generated from — the
Gradle build, the wrapper and the CI workflow — and is retained for that reason.
