package dev.soundworks.block;

import net.minecraft.server.level.ServerPlayer;

/** A device that accepts actions from the client GUI. */
public interface Controllable {
	int ACTION_TOGGLE = 0, ACTION_SPEED = 1, ACTION_VOLUME = 2, ACTION_EJECT = 3, ACTION_MODE = 4, ACTION_SKIP = 5;

	void handleAction(ServerPlayer player, int action, float value);
}
