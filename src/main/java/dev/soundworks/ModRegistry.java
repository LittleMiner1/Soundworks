package dev.soundworks;

import dev.soundworks.block.*;
import dev.soundworks.menu.*;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModRegistry {
	private ModRegistry() {}

	public static DataComponentType<DiscData> DISC_DATA;
	public static SoundEvent SILENT;

	public static Item BLANK_DISC, DISC, PRESS_ITEM, TURNTABLE_ITEM, JUKEBOX_ITEM;
	public static Block PRESS, TURNTABLE, JUKEBOX;
	public static BlockEntityType<PressBlockEntity> PRESS_BE;
	public static BlockEntityType<TurntableBlockEntity> TURNTABLE_BE;
	public static BlockEntityType<JukeboxMachineBlockEntity> JUKEBOX_BE;
	public static MenuType<PressMenu> PRESS_MENU;
	public static MenuType<JukeboxMenu> JUKEBOX_MENU;

	private static Block block(String name, java.util.function.Function<BlockBehaviour.Properties, Block> f) {
		Identifier id = SoundworksMod.id(name);
		Block b = f.apply(BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.WOOD).noOcclusion()
			.setId(ResourceKey.create(Registries.BLOCK, id)));
		Registry.register(BuiltInRegistries.BLOCK, id, b);
		Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(b, new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix()));
		return b;
	}

	public static void init() {
		Identifier compId = SoundworksMod.id("disc_data");
		DISC_DATA = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, compId,
			DataComponentType.<DiscData>builder().persistent(DiscData.CODEC).networkSynchronized(DiscData.STREAM_CODEC).build());

		Identifier silentId = SoundworksMod.id("disc_custom");
		SILENT = Registry.register(BuiltInRegistries.SOUND_EVENT, silentId, SoundEvent.createVariableRangeEvent(silentId));

		Identifier blankId = SoundworksMod.id("blank_disc");
		BLANK_DISC = Registry.register(BuiltInRegistries.ITEM, blankId,
			new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, blankId))));

		Identifier discId = SoundworksMod.id("disc");
		DISC = Registry.register(BuiltInRegistries.ITEM, discId,
			new Item(new Item.Properties().stacksTo(1)
				.jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, SoundworksMod.id("custom")))
				.setId(ResourceKey.create(Registries.ITEM, discId))));

		PRESS = block("press", PressBlock::new);
		TURNTABLE = block("turntable", TurntableBlock::new);
		JUKEBOX = block("jukebox", JukeboxMachineBlock::new);
		PRESS_ITEM = BuiltInRegistries.ITEM.getValue(SoundworksMod.id("press"));
		TURNTABLE_ITEM = BuiltInRegistries.ITEM.getValue(SoundworksMod.id("turntable"));
		JUKEBOX_ITEM = BuiltInRegistries.ITEM.getValue(SoundworksMod.id("jukebox"));

		PRESS_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SoundworksMod.id("press"),
			FabricBlockEntityTypeBuilder.create(PressBlockEntity::new, PRESS).build());
		TURNTABLE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SoundworksMod.id("turntable"),
			FabricBlockEntityTypeBuilder.create(TurntableBlockEntity::new, TURNTABLE).build());
		JUKEBOX_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SoundworksMod.id("jukebox"),
			FabricBlockEntityTypeBuilder.create(JukeboxMachineBlockEntity::new, JUKEBOX).build());

		PRESS_MENU = Registry.register(BuiltInRegistries.MENU, SoundworksMod.id("press"),
			new ExtendedMenuType<>(PressMenu::new, BlockPos.STREAM_CODEC.cast()));
		JUKEBOX_MENU = Registry.register(BuiltInRegistries.MENU, SoundworksMod.id("jukebox"),
			new ExtendedMenuType<>(JukeboxMenu::new, BlockPos.STREAM_CODEC.cast()));

		// Creative inventory: the three blocks appear in the "Building Blocks" tab, the blank record in "Tools & Utilities".
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
			entries.accept(PRESS_ITEM);
			entries.accept(TURNTABLE_ITEM);
			entries.accept(JUKEBOX_ITEM);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(BLANK_DISC));
	}

	/** RGB color of a dye, or -1 if the stack is not a dye. */
	public static int dyeColor(ItemStack stack) {
		if (stack.isEmpty()) return -1;
		Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
		String path = key.getPath();
		if (!key.getNamespace().equals("minecraft") || !path.endsWith("_dye")) return -1;
		String color = path.substring(0, path.length() - 4);
		for (DyeColor d : DyeColor.values()) {
			if (d.getSerializedName().equals(color)) return d.getTextureDiffuseColor() & 0xFFFFFF;
		}
		return -1;
	}
}
