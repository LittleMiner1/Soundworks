package dev.soundworks;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Playback state of a device. Synced to all clients via block entity updates. */
public class PlaybackState {
	public static final float SPEED_MIN = 0.75f, SPEED_MAX = 1.25f;
	public static final float VOLUME_MIN = 0.0f, VOLUME_MAX = 2.0f;

	public static float clampSpeed(float v) {
		return Float.isNaN(v) ? 1.0f : Math.max(SPEED_MIN, Math.min(SPEED_MAX, v));
	}

	public static float clampVolume(float v) {
		return Float.isNaN(v) ? 1.0f : Math.max(VOLUME_MIN, Math.min(VOLUME_MAX, v));
	}

	public String hash = "";
	public String title = "";
	public int lengthMs = 0;
	public boolean playing = false;
	public int session = 0;       // zaehlt bei jedem (Neu-)Start hoch
	public long startTick = 0;    // Spielzeit-Tick, zu dem basePosMs gilt
	public long basePosMs = 0;
	public float speed = 1.0f;
	public float volume = 1.0f;

	public long posMs(long now) {
		if (!playing) return basePosMs;
		return basePosMs + (long) ((now - startTick) * 50L * (double) speed);
	}

	public void start(DiscData d, long now) {
		hash = d.hash();
		title = d.title();
		lengthMs = d.lengthSec() * 1000;
		basePosMs = 0;
		startTick = now;
		playing = true;
		session++;
	}

	public void stop() {
		playing = false;
		basePosMs = 0;
	}

	public void setSpeed(float newSpeed, long now) {
		if (playing) {
			basePosMs = posMs(now);
			startTick = now;
		}
		speed = newSpeed;
	}

	public void save(ValueOutput out) {
		out.putString("hash", hash);
		out.putString("title", title);
		out.putInt("lengthMs", lengthMs);
		out.putBoolean("playing", playing);
		out.putInt("session", session);
		out.putLong("startTick", startTick);
		out.putLong("basePosMs", basePosMs);
		out.putFloat("speed", speed);
		out.putFloat("volume", volume);
	}

	public void load(ValueInput in) {
		hash = in.getStringOr("hash", "");
		title = in.getStringOr("title", "");
		lengthMs = in.getIntOr("lengthMs", 0);
		playing = in.getBooleanOr("playing", false);
		session = in.getIntOr("session", 0);
		startTick = in.getLongOr("startTick", 0L);
		basePosMs = in.getLongOr("basePosMs", 0L);
		speed = clampSpeed(in.getFloatOr("speed", 1.0f));
		volume = clampVolume(in.getFloatOr("volume", 1.0f));
	}
}
