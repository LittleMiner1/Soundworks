package dev.soundworks.client.screen;

import dev.soundworks.client.audio.ClientAudio;
import dev.soundworks.client.audio.Uploader;
import dev.soundworks.menu.PressMenu;
import dev.soundworks.net.Net;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PressScreen extends AbstractContainerScreen<PressMenu> {
	private static final int ROWS = 6;
	private static final int LIST_X = 104, LIST_Y = 22, LIST_W = 146, ROW_H = 14;
	private static final int LABEL_X = 30, LABEL_W = 70;
	private final List<Path> files = new ArrayList<>();
	private final Button[] rows = new Button[ROWS];
	private Button upButton, downButton;
	private int offset = 0;
	private int selected = -1;
	private volatile Component status = Component.empty();

	public PressScreen(PressMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, 280, 232);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelY = 10000;
		inventoryLabelY = 10000;
		for (int i = 0; i < ROWS; i++) {
			final int idx = i;
			rows[i] = addRenderableWidget(Button.builder(Component.empty(), b -> {
				if (offset + idx < files.size()) selected = offset + idx;
				updateRows();
			}).bounds(leftPos + LIST_X, topPos + LIST_Y + i * ROW_H, LIST_W, ROW_H - 1).build());
		}
		upButton = addRenderableWidget(Button.builder(Component.literal("^"), b -> { offset = Math.max(0, offset - 1); updateRows(); })
			.bounds(leftPos + 254, topPos + LIST_Y, 16, 13).build());
		downButton = addRenderableWidget(Button.builder(Component.literal("v"), b -> {
			offset = Math.max(0, Math.min(files.size() - ROWS, offset + 1)); updateRows(); })
			.bounds(leftPos + 254, topPos + LIST_Y + ROW_H, 16, 13).build());
		addRenderableWidget(Button.builder(Component.literal("R"), b -> { refreshFiles(); updateRows(); })
			.bounds(leftPos + 254, topPos + LIST_Y + 2 * ROW_H, 16, 13).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.soundworks.press"), b -> onPress())
			.bounds(leftPos + LIST_X, topPos + 110, 70, 16).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.soundworks.copy"), b -> {
			ClientPlayNetworking.send(new Net.PressRequest(menu.pos, "", "", true));
			status = Component.empty();
		}).bounds(leftPos + LIST_X + 76, topPos + 110, 70, 16).build());
		refreshFiles();
		updateRows();
	}

	private void refreshFiles() {
		files.clear();
		try (Stream<Path> s = Files.list(ClientAudio.musicDir())) {
			s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".mp3"))
				.sorted().forEach(files::add);
		} catch (IOException ignored) {}
		selected = -1;
		offset = 0;
	}

	/** Only rows that really have a song are visible; empty rows disappear completely. */
	private void updateRows() {
		for (int i = 0; i < ROWS; i++) {
			int idx = offset + i;
			boolean has = idx < files.size();
			rows[i].visible = has;
			rows[i].active = has;
			if (has) {
				String name = files.get(idx).getFileName().toString();
				String prefix = idx == selected ? "> " : "";
				rows[i].setMessage(Component.literal(prefix + Gui.fit(font, name, LIST_W - 12 - font.width(prefix))));
			} else {
				rows[i].setMessage(Component.empty());
			}
		}
		boolean scroll = files.size() > ROWS;
		upButton.visible = scroll;
		downButton.visible = scroll;
		upButton.active = scroll && offset > 0;
		downButton.active = scroll && offset < files.size() - ROWS;
	}

	private void onPress() {
		if (selected < 0 || selected >= files.size()) {
			status = Component.translatable("gui.soundworks.msg.select_file");
			return;
		}
		if (Uploader.busy()) return;
		Path f = files.get(selected);
		String name = f.getFileName().toString();
		String title = name.substring(0, Math.max(1, name.lastIndexOf('.')));
		status = Component.translatable("gui.soundworks.msg.reading");
		Uploader.start(f, title, menu.pos, key -> status = Component.translatable(key));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		Gui.panel(g, leftPos, topPos, imageWidth, imageHeight, menu);
		if (files.isEmpty()) Gui.inset(g, leftPos + LIST_X - 2, topPos + LIST_Y - 2, leftPos + LIST_X + LIST_W + 2, topPos + LIST_Y + ROWS * ROW_H + 1);
	}

	private void label(GuiGraphicsExtractor g, String key, int slotY) {
		String s = Gui.fit(font, Component.translatable(key).getString(), LABEL_W);
		g.text(font, Component.literal(s), leftPos + LABEL_X, topPos + slotY + 4, Gui.TEXT, false);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		extractBackground(g, mouseX, mouseY, delta);
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.text(font, Component.literal(Gui.fit(font, title.getString(), 90)), leftPos + 8, topPos + 8, Gui.TEXT, false);
		label(g, "gui.soundworks.slot.blank", 24);
		label(g, "gui.soundworks.slot.dye", 46);
		label(g, "gui.soundworks.slot.source", 68);
		label(g, "gui.soundworks.slot.output", 104);
		if (files.isEmpty()) {
			int y = topPos + LIST_Y + 4;
			for (String line : Gui.wrap(font, Component.translatable("gui.soundworks.no_files").getString(), LIST_W - 8, 6)) {
				g.text(font, Component.literal(line), leftPos + LIST_X + 4, y, 0xFFFFFFFF, false);
				y += 10;
			}
		}
		int y = topPos + 131;
		for (String line : Gui.wrap(font, status.getString(), 166, 2)) {
			g.text(font, Component.literal(line), leftPos + LIST_X, y, Gui.TEXT, false);
			y += 10;
		}
		extractTooltip(g, mouseX, mouseY);
	}
}
