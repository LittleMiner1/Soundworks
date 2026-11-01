package dev.soundworks.client.screen;

import dev.soundworks.PlaybackState;
import dev.soundworks.block.Controllable;
import dev.soundworks.block.TurntableBlockEntity;
import dev.soundworks.net.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Turntable settings: play/stop, eject, speed (75-125 %) and volume (0-200 %). */
public class TurntableScreen extends Screen {
	private static final int W = 220, H = 142;
	private final BlockPos pos;
	private Button playButton;
	private SettingSlider speedSlider, volumeSlider;

	public TurntableScreen(BlockPos pos) {
		super(Component.translatable("block.soundworks.turntable"));
		this.pos = pos;
	}

	private TurntableBlockEntity be() {
		var level = Minecraft.getInstance().level;
		return level != null && level.getBlockEntity(pos) instanceof TurntableBlockEntity t ? t : null;
	}

	private void send(int action, float value) {
		ClientPlayNetworking.send(new Net.DeviceAction(pos, action, value));
	}

	@Override
	protected void init() {
		int cx = width / 2, cy = height / 2;
		int left = cx - 100;
		var t = be();
		playButton = addRenderableWidget(Button.builder(Component.empty(), b -> send(Controllable.ACTION_TOGGLE, 0f))
			.bounds(left, cy - 34, 98, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.soundworks.eject"), b -> send(Controllable.ACTION_EJECT, 0f))
			.bounds(left + 102, cy - 34, 98, 20).build());
		speedSlider = addRenderableWidget(new SettingSlider(left, cy - 8, 200, 20, "gui.soundworks.speed",
			PlaybackState.SPEED_MIN, PlaybackState.SPEED_MAX, 0.01f, 0.02f, t == null ? 1f : t.ps.speed,
			v -> send(Controllable.ACTION_SPEED, v)));
		volumeSlider = addRenderableWidget(new SettingSlider(left, cy + 16, 200, 20, "gui.soundworks.volume",
			PlaybackState.VOLUME_MIN, PlaybackState.VOLUME_MAX, 0.01f, 0.04f, t == null ? 1f : t.ps.volume,
			v -> send(Controllable.ACTION_VOLUME, v)));
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
			.bounds(cx - 50, cy + 44, 100, 20).build());
	}

	@Override
	public void removed() {
		super.removed();
		if (speedSlider != null) speedSlider.flush(true);
		if (volumeSlider != null) volumeSlider.flush(true);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		Gui.panel(g, width / 2 - W / 2, height / 2 - 70, W, H, null);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		TurntableBlockEntity t = be();
		if (t == null) { onClose(); return; }
		playButton.setMessage(Component.translatable(t.ps.playing ? "gui.soundworks.stop" : "gui.soundworks.play"));
		speedSlider.syncFrom(t.ps.speed);
		volumeSlider.syncFrom(t.ps.volume);
		speedSlider.flush(false);
		volumeSlider.flush(false);
		super.extractRenderState(g, mouseX, mouseY, delta);
		int cx = width / 2, cy = height / 2;
		g.centeredText(font, title, cx, cy - 62, Gui.TEXT);
		String name = t.disc.isEmpty() ? Component.translatable("gui.soundworks.no_disc").getString() : t.ps.title;
		g.centeredText(font, Component.literal(Gui.fit(font, name, W - 20)), cx, cy - 49, 0xFF606060);
	}
}
