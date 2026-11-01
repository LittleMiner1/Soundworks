package dev.soundworks;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;

/** Bridge between common code and client code (set by the client entrypoint). */
public final class Hooks {
	private Hooks() {}

	public interface Audio {
		/** Called every client tick for a device. */
		void tick(long key, BlockPos pos, PlaybackState state, long gameTime);
		void stop(long key);
	}

	public static Audio audio = new Audio() {
		@Override public void tick(long key, BlockPos pos, PlaybackState state, long gameTime) {}
		@Override public void stop(long key) {}
	};
	public static Consumer<BlockPos> openTurntable = p -> {};
}
