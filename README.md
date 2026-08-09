# Villager Enhanced

A NeoForge mod that turns Minecraft's villagers from vending machines into people. Right-clicking
a villager opens a conversation instead of a trade window — and they have names, opinions about
you, and things to say about each other.

![Talking to a villager](docs/images/greeting.png)

| | |
|---|---|
| **Minecraft** | 26.2 |
| **NeoForge** | 26.2.0.48-beta |
| **Java** | 25 |
| **Licence** | [MIT](LICENSE) |

> NeoForge 26.2 is a **beta** line whose APIs change without deprecation cycles. Expect breakage
> when bumping the loader version.

## What it does

**Villagers have names.** Assigned per villager and remembered, distinct within a village, and
drawn from different pools by biome so a desert settlement sounds unlike a taiga one. They keep
their name through zombification and curing. A name tag still wins.

**They decide how much to tell you.** A villager you have never spoken to is "the Farmer" until
you ask their name — and someone who resents you will not tell you. What else they will do rises
with how they feel about you:

| Standing | They will |
|---|---|
| Reviled / Disliked | trade, accept gifts, tell you exactly what they think of you |
| Stranger | …give you their name |
| Acquaintance | …gossip about the neighbours |
| Trusted | …show you what they are carrying |

**They have opinions, and you can change them.** Reputation shows as a named tier rather than a
number, and you can shift it by trading or by giving gifts — anything they would buy, or any
food. Gifts cannot buy the highest regard; that still takes curing a zombie villager.

**They talk about each other.** Ask what the others say about you and a villager relays their
neighbours' opinions, by name. Minecraft has always run this gossip network — villagers form
opinions and pass them around when they meet — and it has never been visible. A villager you have
never met can already have an opinion of you, because they heard it from someone who has.

**They remember you.** Greetings differ for a stranger, a returning acquaintance, and someone who
has been away a few days.

**They tell you when something happens.** Nearby players hear when a villager is born or killed,
and by what.

**Sneak + right-click** skips the conversation and trades directly.

|  |  |
|---|---|
| ![Where you stand with a villager](docs/images/regards.png) | ![What the other villagers say](docs/images/gossip.png) |
| Where you stand with one villager | What the rest of the village says about you |

## Configuration

**Mods → Villager Enhanced → Config**, or edit the files.

Client settings are personal — voice volume, dialogue speed, whether to see notifications. Server
settings are shared by everyone on the world: whether the dialogue is enabled at all, sneak-to-
trade, whether a villager is occupied for a whole conversation, conversation range, gift value,
rumour count and radius, and the notification radius.

The split is deliberate: anything affecting what a player can *do* is a server setting, because a
client-side value would simply be a switch to change the rules.

Setting `dialogueEnabled` to false restores vanilla trading entirely, leaving the mod installed
but dormant.

## Building

```bash
./gradlew build
```

The jar lands in `build/libs/`.

## Development

Run configurations, available from the Gradle tool window or your IDE after a sync:

| Task | Purpose |
|---|---|
| `runClient` | Client, playing as `Dev` |
| `runServer` | Dedicated server |
| `runClientDev01` | Second client, playing as `dev01` |
| `runClientDev02` | Third client, playing as `dev02` |

The numbered clients are for multiplayer testing. Each needs its own username *and* its own game
directory — clients sharing one fight over `options.txt`, the logs and the session lock. Start
`runServer`, then connect both to `localhost`.

### Architecture

The dialogue is **server-authoritative**. The server decides when a conversation starts, which
options it offers, and whether to act on a choice; the client renders what it is told and sends
choices back. Every inbound message is re-validated against the server's own view of the world.

Code is split along the client/server boundary, which NeoForge enforces at class-load time:

| Package | Loaded on | Contains |
|---|---|---|
| `dialogue` | both | Interaction, sessions, names, memory, reputation, notifications |
| `network` | both | Payload definitions and serverbound handlers |
| `config` | both | The two config specs |
| `attachment` | both | Saved per-villager data |
| `client` | client only | Screens and clientbound handlers |

Nothing in the common packages may reference `client` or any `net.minecraft.client` type — a
dedicated server would crash the moment such a class loaded.

Villagers store two pieces of saved data via NeoForge attachments: their name, and what they
remember about each player who has spoken to them.

`src/main/resources/META-INF/accesstransformer.cfg` widens two vanilla members: the method that
applies reputation discounts before trading, and the gossip container so gifts can affect it.

## Licence

[MIT](LICENSE).

`TEMPLATE_LICENSE.txt` covers the NeoForge MDK files this project was generated from — the Gradle
build, the wrapper and the CI workflow — and is retained for that reason.
