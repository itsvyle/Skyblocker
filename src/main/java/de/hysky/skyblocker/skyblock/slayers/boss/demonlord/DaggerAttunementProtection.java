package de.hysky.skyblocker.skyblock.slayers.boss.demonlord;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;
import de.hysky.skyblocker.utils.Constants;


import de.hysky.skyblocker.annotations.Init;
import de.hysky.skyblocker.config.SkyblockerConfigManager;
import de.hysky.skyblocker.utils.Utils;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import de.hysky.skyblocker.skyblock.slayers.SlayerManager;
import de.hysky.skyblocker.skyblock.slayers.SlayerType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;


public class DaggerAttunementProtection {
	private static final Set<String> BLAZE_DAGGERS = Set.of("HEARTMAW_DAGGER", "BURSTMAW_DAGGER", "MAWDUST_DAGGER", "HEARTFIRE_DAGGER", "BURSTFIRE_DAGGER", "FIREDUST_DAGGER");

	private static final Pattern BOSS_ATTUNEMENT_PATTERN = Pattern.compile("ASHEN|SPIRIT|CRYSTAL|AURIC");
	private static final Pattern LORE_ATTUNEMENT_PATTERN = Pattern.compile("Attuned:\\s*(ASHEN|SPIRIT|CRYSTAL|AURIC)", Pattern.CASE_INSENSITIVE);

	@Init
	public static void init() {
		UseItemCallback.EVENT.register(DaggerAttunementProtection::onInteract);
	}

	private static InteractionResult onInteract(Player player, Level world, InteractionHand hand) {
		if (world.isClientSide() &&
			SkyblockerConfigManager.get().slayers.blazeSlayer.blockIncorrectDaggerSwitch &&
			SlayerManager.isFightingSlayerType(SlayerType.DEMONLORD) &&
			Utils.isInCrimson()) {
			// get the boss from the slayer manager, to get the entities we're looking for!
			// I'm not sure exactly how it works though, so maybe look a bit more at the mob glow stuff
			ItemStack stack = player.getItemInHand(hand);
			String skyblockId = stack.getSkyblockId();

			if (!skyblockId.isEmpty() && BLAZE_DAGGERS.contains(skyblockId)) {
				String bossAttunement = getNearbyBossAttunement(player, world);

				if (bossAttunement != null) {
					String daggerAttunement = getDaggerAttunementFromLore(player, stack);

					if (daggerAttunement != null && daggerAttunement.equals(bossAttunement)) {
						player.displayClientMessage(Constants.PREFIX.get().append("§c[Skyblocker] Blocked attunement switch! Already matching " + daggerAttunement), false);
						player.playSound(SoundEvents.VILLAGER_NO, 100f, 0.1f);
						return InteractionResult.FAIL;
					}
				}
			}
		}

		return InteractionResult.PASS;
	}

	/**
	 * Scans the item's visual tooltip/lore text line-by-line for the active attunement state.
	 */
	private static String getDaggerAttunementFromLore(Player player, ItemStack stack) {
		for (Component line : stack.getTooltipLines(Item.TooltipContext.EMPTY, player, TooltipFlag.NORMAL)) {
			String plainLine = line.getString();
			Matcher matcher = LORE_ATTUNEMENT_PATTERN.matcher(plainLine);
			if (matcher.find()) {
				return matcher.group(1).toUpperCase(Locale.ENGLISH);
			}
		}
		return null;
	}

	/**
	 * Scans the surrounding area for active Inferno Demonlord attribute displays via ArmorStands
	 */
	private static String getNearbyBossAttunement(Player player, Level world) {
		if (!SlayerManager.isFightingSlayerType(SlayerType.DEMONLORD)) return null;

		ArmorStand armorStand = null;

		if (SlayerManager.isSlayerArmorStandAlive()) {
			var armorStands = SlayerManager.getEntityArmorStands(SlayerManager.getBossFight().armorStand, 2.5f);
			for (ArmorStand as : armorStands) {
				if (as.getName() != null && BOSS_ATTUNEMENT_PATTERN.matcher(as.getName().getString()).find()) {
					armorStand = as;
					break;
				}
			}
			player.displayClientMessage(Constants.PREFIX.get().append("Found active attunement!"), false);
		} else {
			// search for the closest WitherSkeleton and ZombifiedPiglin, and then for each get the entity armor stands around them, and check for attunement armor stands among those. This is a fallback for the demons phase
			var witherSkeletons = world.getEntitiesOfClass(WitherSkeleton.class, player.getBoundingBox().inflate(24.0));
			var piglins = world.getEntitiesOfClass(ZombifiedPiglin.class, player.getBoundingBox().inflate(24.0));
			armorStand = witherSkeletons.stream()
					.flatMap(ws -> SlayerManager.getEntityArmorStands(ws, 2.5f).stream())
					.filter(as -> as.getName() != null && BOSS_ATTUNEMENT_PATTERN.matcher(as.getName().getString()).find())
					.findFirst()
					.orElseGet(() -> piglins.stream().flatMap(pg -> SlayerManager.getEntityArmorStands(pg, 2.5f).stream())
							.filter(as -> as.getName() != null && BOSS_ATTUNEMENT_PATTERN.matcher(as.getName().getString()).find())
							.findFirst()
							.orElse(null)); // if no attunement armor stands are found around the demons, fallback to scanning nearby armor stands for attunement (e.g. if the boss armor stand isn't loaded due to spawn protection or something)
		}

		if (armorStand == null) {
			player.displayClientMessage(Constants.PREFIX.get().append("Couldn't find boss armor stand"), false);
			return null;
		}

		Matcher matcher = BOSS_ATTUNEMENT_PATTERN.matcher(armorStand.getName().getString());
		if (matcher.find()) {
			return matcher.group();
		}
		return null;
	}
}
