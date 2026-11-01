package dev.soundworks.block;

import dev.soundworks.DiscData;
import dev.soundworks.Hooks;
import dev.soundworks.ModRegistry;
import dev.soundworks.PlaybackState;
import dev.soundworks.menu.JukeboxMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Jukebox with 27 slots (like a chest): playlist, shuffle or play once. */
public class JukeboxMachineBlockEntity extends RandomizableContainerBlockEntity implements ExtendedMenuProvider<BlockPos>, Controllable {
	public static final int MODE_PLAYLIST = 0, MODE_SHUFFLE = 1, MODE_ONCE = 2;
	private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
	public final PlaybackState ps = new PlaybackState();
	public int mode = MODE_PLAYLIST;
	public int current = -1;

	public JukeboxMachineBlockEntity(BlockPos pos, BlockState state) {
		super(ModRegistry.JUKEBOX_BE, pos, state);
	}

	@Override protected NonNullList<ItemStack> getItems() { return items; }
	@Override protected void setItems(NonNullList<ItemStack> list) { this.items = list; }
	@Override protected Component getDefaultName() { return Component.translatable("block.soundworks.jukebox"); }
	@Override public int getContainerSize() { return 27; }
	@Override public BlockPos getScreenOpeningData(ServerPlayer player) { return worldPosition; }
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.is(ModRegistry.DISC); }

	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory inv) {
		return new JukeboxMenu(id, inv, this, worldPosition);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		items = NonNullList.withSize(27, ItemStack.EMPTY);
		if (!tryLoadLootTable(in)) ContainerHelper.loadAllItems(in, items);
		ps.load(in);
		mode = in.getIntOr("mode", MODE_PLAYLIST);
		current = in.getIntOr("current", -1);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		if (!trySaveLootTable(out)) ContainerHelper.saveAllItems(out, items);
		ps.save(out);
		out.putInt("mode", mode);
		out.putInt("current", current);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level != null && level.isClientSide()) Hooks.audio.stop(worldPosition.asLong());
	}

	private void sync() {
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, JukeboxMachineBlockEntity be) {
		if (level.isClientSide()) {
			Hooks.audio.tick(pos.asLong(), pos, be.ps, level.getGameTime());
			return;
		}
		if (!be.ps.playing) return;
		boolean ended = be.ps.posMs(level.getGameTime()) >= be.ps.lengthMs;
		boolean removed = true;
		if (be.current >= 0 && be.current < 27) {
			DiscData d = be.items.get(be.current).get(ModRegistry.DISC_DATA);
			removed = d == null || !d.hash().equals(be.ps.hash);
		}
		if (ended || removed) be.advance(level.getGameTime());
	}

	private DiscData dataAt(int slot) {
		return slot < 0 || slot >= 27 ? null : items.get(slot).get(ModRegistry.DISC_DATA);
	}

	private int nextSlot(boolean forceDifferent) {
		java.util.List<Integer> filled = new java.util.ArrayList<>();
		for (int i = 0; i < 27; i++) if (dataAt(i) != null) filled.add(i);
		if (filled.isEmpty()) return -1;
		if (mode == MODE_SHUFFLE) {
			if (filled.size() > 1 && forceDifferent) filled.remove(Integer.valueOf(current));
			return filled.get(level.getRandom().nextInt(filled.size()));
		}
		for (int s : filled) if (s > current) return s;
		return filled.get(0);
	}

	private void play(int slot, long now) {
		DiscData d = dataAt(slot);
		if (d == null) { ps.stop(); current = -1; return; }
		current = slot;
		ps.start(d, now);
	}

	/** Track ended (or record removed): pick the next track or stop. */
	private void advance(long now) {
		boolean wasEnded = ps.posMs(now) >= ps.lengthMs;
		if (mode == MODE_ONCE && wasEnded) {
			ps.stop();
		} else {
			int next = nextSlot(true);
			if (next < 0) { ps.stop(); current = -1; }
			else play(next, now);
		}
		sync();
	}

	@Override
	public void handleAction(ServerPlayer player, int action, float value) {
		if (level == null) return;
		long now = level.getGameTime();
		switch (action) {
			case ACTION_TOGGLE -> {
				if (ps.playing) ps.stop();
				else {
					int slot = dataAt(current) != null ? current : nextSlot(false);
					if (slot < 0) return;
					play(slot, now);
				}
			}
			case ACTION_MODE -> mode = (mode + 1) % 3;
			case ACTION_SPEED -> ps.setSpeed(PlaybackState.clampSpeed(value), now);
			case ACTION_VOLUME -> ps.volume = PlaybackState.clampVolume(value);
			case ACTION_SKIP -> {
				int next = nextSlot(true);
				if (next < 0) return;
				play(next, now);
			}
			default -> { return; }
		}
		sync();
	}
}
