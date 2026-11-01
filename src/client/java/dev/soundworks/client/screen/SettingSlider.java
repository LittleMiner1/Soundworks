package dev.soundworks.client.screen;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/**
 * Percent slider (speed / volume). The value is sent to the server at most every 100 ms while dragging,
 * and snaps to 100 % (the default) when it gets close to it.
 */
final class SettingSlider extends AbstractSliderButton {
	private final String key;
	private final float min, max, step, snapRadius;
	private final Consumer<Float> sender;
	private float real;
	private boolean dirty;
	private long lastSend, lastChange;

	SettingSlider(int x, int y, int width, int height, String key, float min, float max, float step, float snapRadius,
			float initial, Consumer<Float> sender) {
		super(x, y, width, height, Component.empty(), (initial - min) / (max - min));
		this.key = key;
		this.min = min;
		this.max = max;
		this.step = step;
		this.snapRadius = snapRadius;
		this.sender = sender;
		this.real = initial;
		updateMessage();
	}

	@Override
	protected void updateMessage() {
		if (key == null) return;
		setMessage(Component.translatable(key, Math.round(real * 100) + "%"));
	}

	@Override
	protected void applyValue() {
		float r = min + (float) value * (max - min);
		r = Math.round(r / step) * step;
		if (Math.abs(r - 1.0f) <= snapRadius) r = 1.0f;
		r = Math.max(min, Math.min(max, r));
		real = r;
		value = (r - min) / (max - min);
		dirty = true;
		lastChange = System.currentTimeMillis();
		updateMessage();
	}

	/** Called every frame by the screen: sends the changed value (throttled). */
	void flush(boolean force) {
		long now = System.currentTimeMillis();
		if (dirty && (force || now - lastSend >= 100)) {
			dirty = false;
			lastSend = now;
			sender.accept(Math.round(real * 100f) / 100f);
		}
	}

	/** Takes over the value from the server, unless the player is currently changing it. */
	void syncFrom(float serverValue) {
		if (dirty || System.currentTimeMillis() - lastChange < 700) return;
		if (Math.abs(serverValue - real) > 0.004f) {
			real = Math.max(min, Math.min(max, serverValue));
			value = (real - min) / (max - min);
			updateMessage();
		}
	}
}
