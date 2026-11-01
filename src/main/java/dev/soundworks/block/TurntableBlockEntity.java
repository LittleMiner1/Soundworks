package dev.soundworks.block;

import dev.soundworks.DiscData;
import dev.soundworks.Hooks;
import dev.soundworks.ModRegistry;
import dev.soundworks.PlaybackState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TurntableBlockEntity extends BlockEntity implements Controllable {
	public ItemStack disc = ItemStack.EMPTY;
	public final PlaybackState ps = new PlaybackState();

	public TurntableBlockEntity(BlockPos pos, BlockState state) {
		super(ModRegistry.TURNTABLE_BE, pos, state);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		ps.save(out);
		if (!disc.isEmpty()) out.store("disc", ItemStack.CODEC, disc);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		ps.load(in);
		disc = in.read("disc", ItemStack.CODEC).orElse(ItemStack.EMPTY);
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

	public static void tick(Level level, BlockPos pos, BlockState state, TurntableBlockEntity be) {
		if (level.isClientSide()) {
			Hooks.audio.tick(pos.asLong(), pos, be.ps, level.getGameTime());
		} else if (be.ps.playing && be.ps.posMs(level.getGameTime()) >= be.ps.lengthMs) {
			be.ps.stop();
			be.sync();
		}
	}

	public void insert(ItemStack stack, Player player) {
		DiscData d = stack.get(ModRegistry.DISC_DATA);
		if (d == null || level == null) return;
		disc = stack.copyWithCount(1);
		if (!player.isCreative()) stack.shrink(1);
		ps.start(d, level.getGameTime());
		level.setBlock(worldPosition, getBlockState().setValue(TurntableBlock.HAS_DISC, true), Block.UPDATE_ALL);
		sync();
	}

	public void eject() {
		if (level == null || disc.isEmpty()) return;
		Block.popResource(level, worldPosition, disc);
		disc = ItemStack.EMPTY;
		ps.stop();
		ps.hash = "";
		ps.title = "";
		level.setBlock(worldPosition, getBlockState().setValue(TurntableBlock.HAS_DISC, false), Block.UPDATE_ALL);
		sync();
	}

	@Override
	public void handleAction(ServerPlayer player, int action, float value) {
		if (level == null) return;
		long now = level.getGameTime();
		switch (action) {
			case ACTION_TOGGLE -> {
				DiscData d = disc.get(ModRegistry.DISC_DATA);
				if (d == null) return;
				if (ps.playing) ps.stop();
				else ps.start(d, now);
			}
			case ACTION_SPEED -> ps.setSpeed(PlaybackState.clampSpeed(value), now);
			case ACTION_VOLUME -> ps.volume = PlaybackState.clampVolume(value);
			case ACTION_EJECT -> { eject(); return; }
			default -> { return; }
		}
		sync();
	}
}
