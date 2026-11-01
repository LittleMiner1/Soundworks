package dev.soundworks.block;

import dev.soundworks.DiscData;
import dev.soundworks.ModRegistry;
import dev.soundworks.audio.AudioStore;
import dev.soundworks.net.Net;
import dev.soundworks.menu.PressMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Slots: 0 = blank record, 1 = dye, 2 = source to copy, 3 = output. */
public class PressBlockEntity extends RandomizableContainerBlockEntity implements ExtendedMenuProvider<BlockPos> {
	public static final int BLANK = 0, DYE = 1, SOURCE = 2, OUTPUT = 3;
	private NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);

	public PressBlockEntity(BlockPos pos, BlockState state) {
		super(ModRegistry.PRESS_BE, pos, state);
	}

	@Override protected NonNullList<ItemStack> getItems() { return items; }
	@Override protected void setItems(NonNullList<ItemStack> list) { this.items = list; }
	@Override protected Component getDefaultName() { return Component.translatable("block.soundworks.press"); }
	@Override public int getContainerSize() { return 4; }
	@Override public BlockPos getScreenOpeningData(ServerPlayer player) { return worldPosition; }

	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory inv) {
		return new PressMenu(id, inv, this, worldPosition);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		items = NonNullList.withSize(4, ItemStack.EMPTY);
		if (!tryLoadLootTable(in)) ContainerHelper.loadAllItems(in, items);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		if (!trySaveLootTable(out)) ContainerHelper.saveAllItems(out, items);
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return switch (slot) {
			case BLANK -> stack.is(ModRegistry.BLANK_DISC);
			case DYE -> ModRegistry.dyeColor(stack) >= 0;
			case SOURCE -> stack.is(ModRegistry.DISC);
			default -> false;
		};
	}

	/** Press a new record from an MP3 already stored on the server. */
	public void press(ServerPlayer player, String hash, String title) {
		if (!ready(player)) return;
		var server = player.level().getServer();
		if (!AudioStore.isValidHash(hash) || !AudioStore.has(server, hash)) {
			Net.overlay(player, Component.translatable("gui.soundworks.msg.no_audio"));
			return;
		}
		String t = title == null || title.isBlank() ? "Untitled" : title.strip();
		if (t.length() > 48) t = t.substring(0, 48);
		ItemStack out = new ItemStack(ModRegistry.DISC);
		out.set(ModRegistry.DISC_DATA, new DiscData(hash, t, AudioStore.lengthSec(server, hash)));
		out.set(DataComponents.ITEM_NAME, Component.literal(t));
		finish(out);
	}

	/** Duplicate the record in slot 2. */
	public void copy(ServerPlayer player) {
		if (!ready(player)) return;
		ItemStack src = items.get(SOURCE);
		if (!src.is(ModRegistry.DISC) || src.get(ModRegistry.DISC_DATA) == null) {
			Net.overlay(player, Component.translatable("gui.soundworks.msg.no_source"));
			return;
		}
		finish(src.copyWithCount(1));
	}

	private boolean ready(ServerPlayer player) {
		if (!items.get(OUTPUT).isEmpty()) {
			Net.overlay(player, Component.translatable("gui.soundworks.msg.output_full"));
			return false;
		}
		if (!items.get(BLANK).is(ModRegistry.BLANK_DISC)) {
			Net.overlay(player, Component.translatable("gui.soundworks.msg.no_blank"));
			return false;
		}
		return true;
	}

	private void finish(ItemStack out) {
		ItemStack dye = items.get(DYE);
		int color = ModRegistry.dyeColor(dye);
		if (color >= 0) {
			out.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
			dye.shrink(1);
		}
		items.get(BLANK).shrink(1);
		items.set(OUTPUT, out);
		setChanged();
		if (level != null) level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.4f);
	}
}
