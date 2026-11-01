package dev.soundworks.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public abstract class BaseMenu extends AbstractContainerMenu {
	public final BlockPos pos;
	protected final Container container;
	protected final int containerSlots;

	protected BaseMenu(MenuType<?> type, int id, Container container, BlockPos pos) {
		super(type, id);
		this.container = container;
		this.containerSlots = container.getContainerSize();
		this.pos = pos;
	}

	protected void addPlayerSlots(Inventory inv, int x, int y) {
		for (int r = 0; r < 3; r++)
			for (int c = 0; c < 9; c++)
				addSlot(new Slot(inv, c + r * 9 + 9, x + c * 18, y + r * 18));
		for (int c = 0; c < 9; c++)
			addSlot(new Slot(inv, c, x + c * 18, y + 58));
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < containerSlots) {
			if (!moveItemStackTo(stack, containerSlots, slots.size(), true)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, 0, containerSlots, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		return copy;
	}

	@Override
	public boolean stillValid(Player player) {
		return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
	}
}
