package dev.soundworks;

import dev.soundworks.net.Net;
import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

/**
 * Tracks vanilla jukeboxes on the server so that every player (including late joiners) hears a Soundworks
 * record at the same position in the song. The state is re-broadcast every 5 seconds.
 */
public final class VanillaJukeboxSync {
	private VanillaJukeboxSync() {}

	private record Entry(String hash, int lengthMs, long startTick, int session) {}

	private static final Map<JukeboxBlockEntity, Entry> ACTIVE = new IdentityHashMap<>();
	private static final Map<JukeboxBlockEntity, ServerLevel> LEVELS = new IdentityHashMap<>();
	private static int sessionCounter = 0;

	public static void init() {
		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((be, level) -> {
			if (be instanceof JukeboxBlockEntity j) LEVELS.put(j, level);
		});
		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((be, level) -> {
			if (be instanceof JukeboxBlockEntity j) {
				LEVELS.remove(j);
				if (ACTIVE.remove(j) != null) broadcast(level, j, null, false);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			boolean resend = server.getTickCount() % 100 == 0;
			for (var e : LEVELS.entrySet()) {
				JukeboxBlockEntity j = e.getKey();
				ServerLevel level = e.getValue();
				ItemStack stack = j.getTheItem();
				DiscData d = stack.get(ModRegistry.DISC_DATA);
				boolean has = d != null && j.getBlockState().hasProperty(JukeboxBlock.HAS_RECORD)
					&& j.getBlockState().getValue(JukeboxBlock.HAS_RECORD);
				Entry cur = ACTIVE.get(j);
				if (!has) {
					if (cur != null) {
						ACTIVE.remove(j);
						broadcast(level, j, null, false);
					}
				} else if (cur == null || !cur.hash().equals(d.hash())) {
					Entry n = new Entry(d.hash(), d.lengthSec() * 1000, level.getGameTime(), ++sessionCounter);
					ACTIVE.put(j, n);
					broadcast(level, j, n, true);
				} else if (resend) {
					broadcast(level, j, cur, true);
				}
			}
		});
	}

	private static void broadcast(ServerLevel level, JukeboxBlockEntity j, Entry e, boolean playing) {
		Net.JukeboxSync p = new Net.JukeboxSync(j.getBlockPos(), e == null ? "" : e.hash(), e == null ? 0 : e.lengthMs(),
			e == null ? 0 : e.startTick(), e == null ? 0 : e.session(), playing);
		for (ServerPlayer player : level.players()) ServerPlayNetworking.send(player, p);
	}
}
