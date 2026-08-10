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

So now you can make one. Six stone, a stick and a gold block, arranged as a bell:

```
stone  stick  stone
stone  gold   stone
stone         stone
```

Nine gold ingots is not cheap, and it is not meant to be.

### Still in a hurry?

**Sneak + right-click** goes straight to trading, exactly as before.

And if you would rather have your plain right-click back, one setting turns the whole dialogue
off and leaves the mod dormant.

## Settings

Everything is adjustable from **Mods → Villager Enhanced → Config**.

Your own settings cover voice volume, how fast dialogue appears, whether names show above
villagers' heads, and whether you see notifications. World settings — for whoever runs the
server — cover whether the dialogue is enabled at all, how far conversations reach, how generous
gifts are, how much villagers gossip, which events are announced, and how far news travels.

## Compatibility

- Works in single-player and on dedicated servers. **Both sides need the mod.**
- Uses no mixins, so conflicts are limited to other mods that also hook villager interaction —
  and sneak-to-trade plus the master switch are there as escape hatches if one does.
- Vanilla name tags still work and take priority.
- Wandering Traders are untouched.

## A note on versions

This targets a **beta** NeoForge line, and the mod is pre-1.0. Expect changes between versions,
and check the changelog before updating a world you care about.
