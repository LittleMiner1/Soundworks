package dev.soundworks.client.screen;

import dev.soundworks.PlaybackState;
import dev.soundworks.block.Controllable;
import dev.soundworks.block.JukeboxMachineBlockEntity;
import dev.soundworks.menu.JukeboxMenu;
import dev.soundworks.net.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class JukeboxScreen extends AbstractContainerScreen<JukeboxMenu> {
	private Button playButton, modeButton;
	private SettingSlider speedSlider, volumeSlider;

	public JukeboxScreen(JukeboxMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, 176, 222);
	}

	private JukeboxMachineBlockEntity be() {
		var level = Minecraft.getInstance().level;
		return level != null && level.getBlockEntity(menu.pos) instanceof JukeboxMachineBlockEntity j ? j : null;
	}

	private void send(int action, float value) {
		ClientPlayNetworking.send(new Net.DeviceAction(menu.pos, action, value));
	}

	@Override
	protected void init() {
		super.init();
		titleLabelY = 10000;
		inventoryLabelY = 10000;
		var j = be();
		playButton = addRenderableWidget(Button.builder(Component.empty(), b -> send(Controllable.ACTION_TOGGLE, 0f))
			.bounds(leftPos + 8, topPos + 17, 54, 16).build());
		modeButton = addRenderableWidget(Button.builder(Component.empty(), b -> send(Controllable.ACTION_MODE, 0f))
			.bounds(leftPos + 66, topPos + 17, 66, 16).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.soundworks.skip"), b -> send(Controllable.ACTION_SKIP, 0f))
			.bounds(leftPos + 136, topPos + 17, 32, 16).build());
		speedSlider = addRenderableWidget(new SettingSlider(leftPos + 8, topPos + 36, 78, 16, "gui.soundworks.speed",
			PlaybackState.SPEED_MIN, PlaybackState.SPEED_MAX, 0.01f, 0.02f, j == null ? 1f : j.ps.speed,
			v -> send(Controllable.ACTION_SPEED, v)));
		volumeSlider = addRenderableWidget(new SettingSlider(leftPos + 90, topPos + 36, 78, 16, "gui.soundworks.volume",
			PlaybackState.VOLUME_MIN, PlaybackState.VOLUME_MAX, 0.01f, 0.04f, j == null ? 1f : j.ps.volume,
			v -> send(Controllable.ACTION_VOLUME, v)));
	}

	@Override
	public void removed() {
		super.removed();
		if (speedSlider != null) speedSlider.flush(true);
		if (volumeSlider != null) volumeSlider.flush(true);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		Gui.panel(g, leftPos, topPos, imageWidth, imageHeight, menu);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		JukeboxMachineBlockEntity be = be();
		boolean playing = be != null && be.ps.playing;
		playButton.setMessage(Component.translatable(playing ? "gui.soundworks.stop" : "gui.soundworks.play"));
		modeButton.setMessage(Component.translatable("gui.soundworks.mode." + (be == null ? 0 : be.mode)));
		if (be != null) {
			speedSlider.syncFrom(be.ps.speed);
			volumeSlider.syncFrom(be.ps.volume);
		}
		speedSlider.flush(false);
		volumeSlider.flush(false);
		extractBackground(g, mouseX, mouseY, delta);
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.text(font, title, leftPos + 8, topPos + 6, Gui.TEXT, false);
		if (playing) {
			String s = Component.translatable("gui.soundworks.now_playing", be.ps.title).getString();
			g.text(font, Component.literal(Gui.fit(font, s, imageWidth - 16)), leftPos + 8, topPos + 58, Gui.TEXT, false);
		}
		extractTooltip(g, mouseX, mouseY);
	}
}
