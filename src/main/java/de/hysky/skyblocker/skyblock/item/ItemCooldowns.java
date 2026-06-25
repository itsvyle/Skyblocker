package de.hysky.skyblocker.skyblock.item;

import de.hysky.skyblocker.annotations.Init;
import de.hysky.skyblocker.config.SkyblockerConfigManager;
import de.hysky.skyblocker.utils.ItemUtils;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemCooldowns {
	private static final String JUNGLE_AXE_ID = "JUNGLE_AXE";
	private static final String TREECAPITATOR_ID = "TREECAPITATOR_AXE";
	private static final String FIG_AXE_ID = "FIG_AXE";
	private static final String FIGSTONE_ID = "FIGSTONE_AXE";
	private static final String GRAPPLING_HOOK_ID = "GRAPPLING_HOOK";
	private static final String ROGUE_SWORD_ID = "ROGUE_SWORD";
	private static final String LEAPING_SWORD_ID = "LEAPING_SWORD";
	private static final String SILK_EDGE_SWORD_ID = "SILK_EDGE_SWORD";
	private static final String GREAT_SPOOK_STAFF_ID = "GREAT_SPOOK_STAFF";
	private static final String SPIRIT_LEAP_ID = "SPIRIT_LEAP";
	private static final String GIANTS_SWORD_ID = "GIANTS_SWORD";
	private static final String SHADOW_FURY_ID = "SHADOW_FURY";
	private static final String LIVID_DAGGER_ID = "LIVID_DAGGER";
	private static final String INK_WAND_ID = "INK_WAND";

	private static final String TACTICAL_INSERTION_ID = "TACTICAL_INSERTION"; // 20s
	private static final String RAGNAROCK_AXE_ID = "RAGNAROCK_AXE"; // 20s
	private static final String WAND_OF_ATONEMENT_ID = "WAND_OF_ATONEMENT"; // 7s
	private static final String SOS_FLARE_ID = "SOS_FLARE"; // 3m
	private static final String ATOMSPLIT_KATANA_ID = "ATOMSPLIT_KATANA"; // 4s
	private static final String ICE_SPRAY_WAND_ID = "ICE_SPRAY_WAND"; // 5s
	private static final String SWORD_OF_BAD_HEALTH = "SWORD_OF_BAD_HEALTH"; // 5s



	private static final List<String> BAT_ARMOR_IDS = List.of("BAT_PERSON_HELMET", "BAT_PERSON_CHESTPLATE", "BAT_PERSON_LEGGINGS", "BAT_PERSON_BOOTS");
	private static final Map<String, CooldownEntry> ITEM_COOLDOWNS = new HashMap<>();

	@Init
	public static void init() {
		UseItemCallback.EVENT.register(ItemCooldowns::onItemInteract);
	}

	private static InteractionResult onItemInteract(Player player, Level world, InteractionHand hand) {
		if (!SkyblockerConfigManager.get().uiAndVisuals.itemCooldown.enableItemCooldowns)
			return InteractionResult.PASS;
		String usedItemId = player.getMainHandItem().getSkyblockId();
		switch (usedItemId) {
			case FIG_AXE_ID, FIGSTONE_ID, JUNGLE_AXE_ID, TREECAPITATOR_ID -> handleItemCooldown(usedItemId, 1000);
			case SILK_EDGE_SWORD_ID, LEAPING_SWORD_ID -> handleItemCooldown(usedItemId, 1000);
			case GRAPPLING_HOOK_ID -> handleItemCooldown(GRAPPLING_HOOK_ID, 2000, player.fishing != null && !isWearingBatArmor(player));
			case ROGUE_SWORD_ID, SPIRIT_LEAP_ID, LIVID_DAGGER_ID -> handleItemCooldown(usedItemId, 5000);
			case SHADOW_FURY_ID -> handleItemCooldown(SHADOW_FURY_ID, 15000);
			case INK_WAND_ID, GIANTS_SWORD_ID -> handleItemCooldown(usedItemId, 30000);
			case GREAT_SPOOK_STAFF_ID -> handleItemCooldown(GREAT_SPOOK_STAFF_ID, 60000);
			// Custom items
			case TACTICAL_INSERTION_ID -> handleItemCooldown(TACTICAL_INSERTION_ID, 20000);
			case RAGNAROCK_AXE_ID -> handleItemCooldown(RAGNAROCK_AXE_ID, 20000);
			case WAND_OF_ATONEMENT_ID -> handleItemCooldown(WAND_OF_ATONEMENT_ID, 7000);
			case SOS_FLARE_ID -> handleItemCooldown(SOS_FLARE_ID, 180000);
			case ATOMSPLIT_KATANA_ID -> handleItemCooldown(ATOMSPLIT_KATANA_ID, 4000);
			case ICE_SPRAY_WAND_ID -> handleItemCooldown(ICE_SPRAY_WAND_ID, 5000);
			case SWORD_OF_BAD_HEALTH -> handleItemCooldown(SWORD_OF_BAD_HEALTH, 5000);
			// Handle any unlisted items if necessary
			default -> {}
		}
		return InteractionResult.PASS;
	}

	// Method to handle item cooldowns with optional condition
	private static void handleItemCooldown(String itemId, int cooldownTime, boolean additionalCondition) {
		if ((!isOnCooldown(itemId) && additionalCondition) || itemId.equals(SOS_FLARE_ID)) {
			ITEM_COOLDOWNS.put(itemId, new CooldownEntry(cooldownTime));
		}
	}

	// Overloaded method for cases without additional conditions
	private static void handleItemCooldown(String itemId, int cooldownTime) {
		handleItemCooldown(itemId, cooldownTime, true);
	}

	public static boolean isOnCooldown(ItemStack itemStack) {
		return isOnCooldown(itemStack.getSkyblockId());
	}

	private static boolean isOnCooldown(String itemId) {
		if (ITEM_COOLDOWNS.containsKey(itemId)) {
			CooldownEntry cooldownEntry = ITEM_COOLDOWNS.get(itemId);
			if (cooldownEntry.isOnCooldown()) {
				return true;
			} else {
				ITEM_COOLDOWNS.remove(itemId);
				return false;
			}
		}

		return false;
	}

	public static CooldownEntry getItemCooldownEntry(ItemStack itemStack) {
		return ITEM_COOLDOWNS.get(itemStack.getSkyblockId());
	}

	private static boolean isWearingBatArmor(Player player) {
		for (ItemStack stack : ItemUtils.getArmor(player)) {
			String itemId = stack.getSkyblockId();
			if (!BAT_ARMOR_IDS.contains(itemId)) {
				return false;
			}
		}
		return true;
	}

	public record CooldownEntry(int cooldown, long startTime) {
		public CooldownEntry(int cooldown) {
			this(cooldown, System.currentTimeMillis());
		}

		public boolean isOnCooldown() {
			return (this.startTime + this.cooldown) > System.currentTimeMillis();
		}

		public long getRemainingCooldown() {
			long time = (this.startTime + this.cooldown) - System.currentTimeMillis();
			return Math.max(time, 0);
		}

		public float getRemainingCooldownPercent() {
			return this.isOnCooldown() ? (float) this.getRemainingCooldown() / cooldown : 0.0f;
		}
	}
}
