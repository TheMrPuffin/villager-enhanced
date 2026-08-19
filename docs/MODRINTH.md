## Villager Enhanced

Minecraft's villagers are vending machines that happen to have legs. This mod makes them people.

Right-click a villager and you get a conversation instead of a shop window. Trading is still
right there — it is just no longer the *only* thing a villager is for.

![Talking to a villager](https://raw.githubusercontent.com/TheMrPuffin/villager-enhanced/main/docs/images/greeting.png)

### They have names

Every villager has one, and keeps it. Names differ by region, so a desert settlement sounds
nothing like a taiga one, and no two villagers in the same village share a name.

You do not get told it for free. A villager you have never spoken to is just "the Farmer" until
you ask — and if you have given them reason to dislike you, they will not tell you at all.

Cure a zombie villager and they come back as the person you knew, not a stranger.

### They have opinions about you

Every villager forms their own view of you, shown as where you stand rather than as a number:

**Reviled → Disliked → Stranger → Acquaintance → Trusted → Honoured**

Trade with someone regularly and they warm to you. Hit one and word gets round. Cure a zombie
villager and you have done something the whole village will remember.

You can also simply be generous. Offer a villager something they would buy, or any food, and
their opinion improves — though generosity alone will never earn you the highest regard.

Gifts a villager can actually use go **into their inventory** rather than disappearing. Villagers
live off what they carry — they eat from it, plant from it, and pass food to neighbours who want
to raise a family. So a gift of bread is not a gesture. It is dinner.

![Where you stand with a villager](https://raw.githubusercontent.com/TheMrPuffin/villager-enhanced/main/docs/images/regards.png)

### They talk about each other

This is the part Minecraft has been hiding from you.

Villagers have always gossiped. Each one forms opinions and passes them to the others when they
meet. You have never been able to see any of it. Now you can ask.

> *Mira the Librarian speaks well of you.*
> *Aldric the Farmer has been complaining about you.*

A villager you have **never met** can already have an opinion of you, because they heard it from
someone who has. Deal fairly with one farmer and in time the whole village knows you are good
for it. Hurt someone and word travels just as fast.

![What the other villagers say about you](https://raw.githubusercontent.com/TheMrPuffin/villager-enhanced/main/docs/images/gossip.png)

### They remember you

Villagers know whether they have met you, and greet a stranger differently from someone they know
— or from someone who has not been round in a while.

Look at someone who has given you their name and it appears above their head, the way it does for
a mob you have name-tagged. Strangers stay anonymous. On a server every player sees only the
names they have earned, so two people looking at the same villager can see different things.

The better they know you, the more they will do. A stranger will give you their name. Someone who
likes you will gossip. Someone who properly trusts you will show you what they are carrying —
which, since Minecraft villagers really do carry bread and seeds and use them to eat, plant and
raise families, tells you more than you might expect.

### They tell you why the village stopped growing

Ask a villager who knows you well enough whether they see themselves starting a family, and they
will tell you what is standing in the way.

Minecraft has always required two things before villagers will breed — enough food, and a spare
bed for the child — and has never once told you which is missing. So a village quietly stops
growing and you are left guessing.

> *I'd like to, and I've food enough put by for it.*
> *But there's nowhere to put a child — not a spare bed in the whole village.*

Hand them a few bread and ask again.

### They tell you what happened

Off mining while a zombie gets into your village? You will hear about it.

> *Mira the Librarian was killed by a Zombie.*
> *A child was born to Aldric the Farmer and Fenna the Shepherd.*
> *The village's iron golem was struck down by a Zombie.*

Golem deaths only count when something actually killed one, so an iron farm stays quiet.

### You can build them a bell

Minecraft has never had a bell recipe. If your village generated without one, or you built the
village yourself, you cannot have one at all.

That matters more here than it does in vanilla. The bell is the meeting point — it is where
villagers gather at dusk and pass their opinions to one another. All the gossip above is read
from that. A village with no bell quietly runs everything this mod does at a fraction of
strength, and nothing tells you.

So now you can make one. Six stone, a stick and a gold block, arranged as a bell — note the empty
slot beneath the gold, which is the bell's open mouth.

![Crafting a bell](https://raw.githubusercontent.com/TheMrPuffin/villager-enhanced/main/docs/images/bell-recipe.png)

Nine gold ingots is not cheap, and it is not meant to be.

### You can make them better at their trade

Villagers get better by trading, slowly, and nitwits never get better at all. The **diamond
apple** changes both.

Offer one to a villager in conversation and they gain a trade level — new stock, better prices,
one step closer to mastering their craft. Offer one to a **nitwit** and something quietly
remarkable happens: they stop being a nitwit, and can take up a trade like anyone else.

![Crafting a diamond apple](https://raw.githubusercontent.com/TheMrPuffin/villager-enhanced/main/docs/images/diamond-apple-recipe.png)

Eight diamonds around a golden apple. That is deliberate — an apple is expensive to make once,
and levelling villagers is the thing the whole trade economy rests on.

They will not take one from just anyone, either. A villager who does not already **trust** you
hands it straight back and says why, rather than eating your diamonds. Earn their trust first.

And if you would rather just eat it, you can. It is worth twice a golden apple.

### You get warning before a raid

A raid does not simply start. Carrying Bad Omen into a village begins a **thirty second**
countdown, and only then does the raid break. Minecraft has never told you this is happening.

> *An omen hangs over the village. It breaks in 30 seconds.*

Villagers notice too. Walk in carrying an omen and they are afraid of *you* — and they have
rather more urgent things to say while a raid is on than they did that morning.

### And a way to find the last raider

You know the one. The raid is nearly over, one pillager spawned badly and is stuck in a ravine
somewhere, and you are walking in circles.

Ring a bell during a raid and **every raider in it lights up for thirty seconds** — at any
distance, through walls and rock. The bell also tells you how many are left, which is what helps
when one is too far away to draw at all.

Vanilla does technically do this: for three seconds, within 48 blocks, and only when a raider is
already within 32 — which is the one you had already found.

### Still in a hurry?

**Sneak + right-click** goes straight to trading, exactly as before.

And if you would rather have your plain right-click back, one setting turns the whole dialogue
off and leaves the mod dormant.

## Settings

Everything is adjustable from **Mods → Villager Enhanced → Config**.

Your own settings cover voice volume, how fast dialogue appears, whether names show above
villagers' heads, and whether you see notifications. World settings — for whoever runs the
server — cover whether the dialogue is enabled at all, how far conversations reach, how generous
gifts are, how much villagers gossip, which events are announced, how far news travels, and how
long the bell lights up raiders.

## Compatibility

- Works in single-player and on dedicated servers. **Both sides need the mod.**
- Uses no mixins, so conflicts are limited to other mods that also hook villager interaction —
  and sneak-to-trade plus the master switch are there as escape hatches if one does.
- Vanilla name tags still work and take priority.
- Wandering Traders are untouched.

## A note on versions

Needs Minecraft **26.2** and NeoForge **26.2.0.62 or newer**.

**1.0 is a promise, not a milestone.** Your worlds keep working — villager names and everything a
villager remembers about you keep their stored shape. Settings keep their names, so nothing you
have tuned gets quietly reset. And `dialogueEnabled = false` will always give you plain vanilla
trading back, with the mod installed but dormant. Breaking any of those means 2.0.

One thing is deliberately outside that promise: the connection between client and server. It is
internal, it carries its own version, and a mismatch is refused at the door rather than
misunderstood. **Clients and servers need matching versions**, and that will keep being true.
