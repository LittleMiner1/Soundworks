package dev.soundworks.menu;

import dev.soundworks.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class JukeboxMenu extends BaseMenu {
	public JukeboxMenu(int id, Inventory inv, BlockPos pos) {
		this(id, inv, new SimpleContainer(27), pos);
	}

	public JukeboxMenu(int id, Inventory inv, Container c, BlockPos pos) {
		super(ModRegistry.JUKEBOX_MENU, id, c, pos);
		for (int r = 0; r < 3; r++)
			for (int col = 0; col < 9; col++)
				addSlot(new Slot(c, col + r * 9, 8 + col * 18, 72 + r * 18) {
					@Override public boolean mayPlace(ItemStack s) { return s.is(ModRegistry.DISC); }
				});
		addPlayerSlots(inv, 8, 140);
	}
}
