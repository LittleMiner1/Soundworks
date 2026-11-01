package dev.soundworks.menu;

import dev.soundworks.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PressMenu extends BaseMenu {
	/** Client constructor. */
	public PressMenu(int id, Inventory inv, BlockPos pos) {
		this(id, inv, new SimpleContainer(4), pos);
	}

	public PressMenu(int id, Inventory inv, Container c, BlockPos pos) {
		super(ModRegistry.PRESS_MENU, id, c, pos);
		addSlot(new Slot(c, 0, 10, 24) {
			@Override public boolean mayPlace(ItemStack s) { return s.is(ModRegistry.BLANK_DISC); }
		});
		addSlot(new Slot(c, 1, 10, 46) {
			@Override public boolean mayPlace(ItemStack s) { return ModRegistry.dyeColor(s) >= 0; }
		});
		addSlot(new Slot(c, 2, 10, 68) {
			@Override public boolean mayPlace(ItemStack s) { return s.is(ModRegistry.DISC); }
		});
		addSlot(new Slot(c, 3, 10, 104) {
			@Override public boolean mayPlace(ItemStack s) { return false; }
		});
		addPlayerSlots(inv, 59, 152);
	}
}
