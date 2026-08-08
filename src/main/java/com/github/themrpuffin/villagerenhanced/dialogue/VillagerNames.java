package com.github.themrpuffin.villagerenhanced.dialogue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.themrpuffin.villagerenhanced.VillagerEnhanced;
import com.github.themrpuffin.villagerenhanced.attachment.VillagerEnhancedAttachments;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;

/**
 * Gives every villager a name.
 *
 * <p>A named villager is a person; "Villager" is furniture. This matters most for rumours, where
 * "Aldric thinks well of you, but Mira hasn't forgiven you" is a story and "a farmer likes you"
 * is a statistic.
 *
 * <p><b>Names are assigned and stored, not derived.</b> Deriving from the villager's UUID would
 * be free, but every villager would roll independently, so avoiding coincidental clashes across
 * a village would need thousands of names — 8 villagers drawing from 20 collide 80% of the time,
 * and 15 collide essentially always. Assigning instead means checking who is nearby and picking
 * something unused, which makes names reliably distinct from a pool barely larger than a village.
 *
 * <p><b>Vanilla's {@code CustomName} is deliberately untouched.</b> Writing to it would collide
 * with player name tags and with other mods that read it, and would render a name above every
 * villager's head whether or not anyone wanted that. A player who has name-tagged a villager has
 * said what they want it called, so that always wins.
 *
 * <p>Pools are keyed by villager type purely so villages sound different from one another. The
 * names are invented and are not meant to represent any real-world culture. Extending a pool is
 * safe at any time — existing villagers keep the name they were given.
 */
@EventBusSubscriber(modid = VillagerEnhanced.MODID)
public final class VillagerNames {

    /**
     * How far to look for names already in use. Roughly a village, so neighbours are distinct
     * without scanning half the world every time a villager is named.
     */
    private static final double VILLAGE_RADIUS = 48.0;

    private static final String[] PLAINS = {
            "Aldric", "Bramwell", "Corin", "Delphie", "Emmet", "Fenna", "Gable", "Hollis",
            "Jorin", "Lowen", "Mirren", "Nesta", "Orrin", "Pell", "Rowena", "Sedwick",
            "Tamsyn", "Verity", "Wilkin", "Yardley", "Alby", "Brindle", "Cassel", "Dorrit",
            "Ewan", "Fairley", "Gorse", "Harrow", "Ivel", "Jessamy", "Kembley", "Linnet",
            "Marlow", "Norrel", "Ottley", "Prue", "Quennel", "Rushton", "Sorrel", "Thurl"
    };

    private static final String[] DESERT = {
            "Ashka", "Dervin", "Hallam", "Kesh", "Nim", "Ordo", "Quill", "Ruka",
            "Sabra", "Sadra", "Tavi", "Torrek", "Vexa", "Zephin", "Calla", "Marn",
            "Othis", "Rhail", "Sunna", "Tiber", "Azrim", "Bekka", "Cassia", "Dral",
            "Emsa", "Farrow", "Gyre", "Hesper", "Ikra", "Jarn", "Kalder", "Lisso",
            "Mazrin", "Nokka", "Orrik", "Pyre", "Rilla", "Sekk", "Tarid", "Vaska"
    };

    private static final String[] JUNGLE = {
            "Anaki", "Iala", "Ilo", "Kaia", "Meru", "Naia", "Ombra", "Oona",
            "Rhen", "Sorel", "Tavane", "Vela", "Yavin", "Zuli", "Ciro", "Enara",
            "Liani", "Petal", "Sova", "Wenna", "Amaru", "Bela", "Cielo", "Duna",
            "Elani", "Faun", "Gilo", "Hana", "Ixel", "Jara", "Kolo", "Lirim",
            "Moya", "Nire", "Olua", "Pavi", "Rasa", "Suri", "Tilo", "Vanya"
    };

    private static final String[] SAVANNA = {
            "Baro", "Deka", "Ekko", "Jarek", "Kova", "Marek", "Nuru", "Onda",
            "Rekka", "Sable", "Tulla", "Vanna", "Zola", "Amsa", "Corvo", "Halla",
            "Odai", "Pemba", "Sirra", "Wekka", "Baku", "Chenna", "Dumo", "Enzo",
            "Falla", "Goro", "Hodi", "Imbe", "Jalo", "Kesta", "Lundo", "Mella",
            "Nyra", "Obu", "Pika", "Rundi", "Sika", "Tovo", "Ubi", "Yara"
    };

    private static final String[] SNOW = {
            "Bryn", "Eira", "Frey", "Halvard", "Kald", "Nix", "Orla", "Runa",
            "Sigrun", "Skadi", "Sten", "Torvald", "Vigga", "Yorik", "Alva", "Gunda",
            "Hesk", "Jorund", "Silla", "Thorne", "Arnvid", "Berg", "Dagny", "Eskil",
            "Fjala", "Gerd", "Hilda", "Isolf", "Ketil", "Lorne", "Mard", "Nessa",
            "Ovin", "Rask", "Sindri", "Tove", "Ulfa", "Vald", "Yrsa", "Zeva"
    };

    private static final String[] SWAMP = {
            "Bogart", "Cobb", "Fenwick", "Grimsby", "Hob", "Lissa", "Marsh", "Mira",
            "Quillon", "Reed", "Sedge", "Tarn", "Wren", "Yarrow", "Dulle", "Everin",
            "Mosley", "Pike", "Sallow", "Thistle", "Alder", "Bracken", "Cress", "Duckett",
            "Eddow", "Fettle", "Gully", "Hollow", "Ivo", "Jarrow", "Keld", "Lurch",
            "Mudge", "Nettle", "Osier", "Peat", "Rushe", "Sump", "Tolley", "Withy"
    };

    private static final String[] TAIGA = {
            "Ansgar", "Aspen", "Birk", "Elska", "Freya", "Ilsa", "Konrad", "Lund",
            "Ovar", "Petra", "Rowan", "Solveig", "Thane", "Varr", "Brann", "Hedda",
            "Ingvar", "Norra", "Stenby", "Ulla", "Alrik", "Borg", "Dalla", "Einar",
            "Ferd", "Grimm", "Hulda", "Ivar", "Kelda", "Loft", "Mikkel", "Nordin",
            "Osk", "Ragna", "Sverre", "Tarj", "Unn", "Vidar", "Ylva", "Zorn"
    };

    /** Modded villager types fall back to {@link #PLAINS} rather than failing to name at all. */
    private static final Map<ResourceKey<VillagerType>, String[]> POOLS = Map.of(
            VillagerType.PLAINS, PLAINS,
            VillagerType.DESERT, DESERT,
            VillagerType.JUNGLE, JUNGLE,
            VillagerType.SAVANNA, SAVANNA,
            VillagerType.SNOW, SNOW,
            VillagerType.SWAMP, SWAMP,
            VillagerType.TAIGA, TAIGA);

    private VillagerNames() {}

    /**
     * What this villager is called, naming it if it has not been named yet.
     *
     * <p>Assignment happens server-side. On the client this returns whatever is already known,
     * falling back to the vanilla display name — clients learn names from
     * {@code OpenDialoguePayload} rather than working them out.
     */
    public static Component nameFor(Villager villager) {
        // A player who name-tagged this villager has already said what it is called.
        Component customName = villager.getCustomName();
        if (customName != null) {
            return customName;
        }

        String stored = storedName(villager);
        if (!stored.isEmpty()) {
            return Component.literal(stored);
        }

        if (villager.level().isClientSide()) {
            return villager.getDisplayName();
        }

        String assigned = assign(villager);
        villager.setData(VillagerEnhancedAttachments.VILLAGER_NAME, assigned);
        return Component.literal(assigned);
    }

    /**
     * What this player should see this villager called.
     *
     * <p>A villager who has not introduced themselves is shown by their trade — "Farmer" — not
     * by name. Learning someone's name is something that happens in conversation, so it should
     * cost a question rather than arriving free with the window.
     *
     * <p>A player-applied name tag bypasses this: if you named them yourself, you already know
     * what they are called.
     */
    public static Component displayNameFor(Villager villager, Player player) {
        if (villager.getCustomName() != null || VillagerMemory.isIntroduced(villager, player)) {
            return nameFor(villager);
        }
        return villager.getDisplayName();
    }

    /**
     * The stored name, or empty if this villager has not been named.
     *
     * <p>Checks {@code hasData} first because {@code getData} attaches the default value on its
     * first call, which would make every villager look named the moment anything asked.
     */
    private static String storedName(Villager villager) {
        return villager.hasData(VillagerEnhancedAttachments.VILLAGER_NAME)
                ? villager.getData(VillagerEnhancedAttachments.VILLAGER_NAME)
                : "";
    }

    /**
     * Picks a name no nearby villager is using.
     *
     * <p>Starts at a UUID-derived offset rather than the front of the list, so the first
     * villager in a village is not always called Aldric, then walks forward until it finds
     * something free. If a settlement outgrows its pool, the offset name is reused — duplicates
     * beat refusing to name anyone.
     */
    private static String assign(Villager villager) {
        String[] pool = poolFor(villager);
        Set<String> taken = namesInUseNear(villager);

        int start = Math.floorMod(
                Long.hashCode(villager.getUUID().getLeastSignificantBits()), pool.length);

        for (int i = 0; i < pool.length; i++) {
            String candidate = pool[(start + i) % pool.length];
            if (!taken.contains(candidate)) {
                return candidate;
            }
        }
        return pool[start];
    }

    private static Set<String> namesInUseNear(Villager villager) {
        AABB area = villager.getBoundingBox().inflate(VILLAGE_RADIUS);
        Set<String> taken = new HashSet<>();

        for (Villager neighbour : villager.level().getEntitiesOfClass(Villager.class, area)) {
            if (neighbour == villager) {
                continue;
            }
            String name = storedName(neighbour);
            if (!name.isEmpty()) {
                taken.add(name);
            }
        }
        return taken;
    }

    private static String[] poolFor(Villager villager) {
        Holder<VillagerType> type = villager.getVillagerData().type();
        for (Map.Entry<ResourceKey<VillagerType>, String[]> entry : POOLS.entrySet()) {
            if (type.is(entry.getKey())) {
                return entry.getValue();
            }
        }
        return PLAINS;
    }

    /**
     * Carries a name across zombification and curing.
     *
     * <p>Converting a mob replaces the entity entirely, so without this a villager you knew
     * would come back from being cured as a stranger. Vanilla fires this event in both
     * directions — villager to zombie villager, and back again — so copying whenever the source
     * has a name and the outcome does not covers the whole round trip.
     */
    @SubscribeEvent
    public static void onConversion(LivingConversionEvent.Post event) {
        LivingEntity from = event.getEntity();
        LivingEntity to = event.getOutcome();

        if (!from.hasData(VillagerEnhancedAttachments.VILLAGER_NAME)) {
            return;
        }

        String name = from.getData(VillagerEnhancedAttachments.VILLAGER_NAME);
        if (!name.isEmpty() && !to.hasData(VillagerEnhancedAttachments.VILLAGER_NAME)) {
            to.setData(VillagerEnhancedAttachments.VILLAGER_NAME, name);
        }
    }
}
