package dev.soundworks.client.screen;

import dev.soundworks.client.ScreenOpener;
import dev.soundworks.client.audio.ClientAudio;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Soundworks section of the sound settings: lists all MP3 files and opens the music folder. */
public class SoundworksScreen extends Screen {
	private static final int MAX_ROWS = 10;
	private static final int ROW_W = 280, ROW_H = 22;
	private final Screen parent;
	private final List<Path> files = new ArrayList<>();
	private final Button[] rows = new Button[MAX_ROWS];
	private Button upButton, downButton;
	private int visibleRows = MAX_ROWS;
	private int offset = 0;

	public SoundworksScreen(Screen parent) {
		super(Component.translatable("gui.soundworks.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cx = width / 2;
		int top = 40;
		// the list adapts to the window height so the buttons below are always visible
		visibleRows = Math.max(3, Math.min(MAX_ROWS, (height - top - 66) / ROW_H));
		reload();
		for (int i = 0; i < MAX_ROWS; i++) {
			rows[i] = addRenderableWidget(Button.builder(Component.empty(), b -> {})
				.bounds(cx - 150, top + i * ROW_H, ROW_W, 20).build());
			rows[i].active = false;
		}
		upButton = addRenderableWidget(Button.builder(Component.literal("^"), b -> {
			offset = Math.max(0, offset - Math.max(1, visibleRows - 1)); update(); })
			.bounds(cx + 134, top, 16, 20).build());
		downButton = addRenderableWidget(Button.builder(Component.literal("v"), b -> {
			offset = Math.max(0, Math.min(files.size() - visibleRows, offset + Math.max(1, visibleRows - 1))); update(); })
			.bounds(cx + 134, top + ROW_H, 16, 20).build());
		int by = top + visibleRows * ROW_H + 6;
		addRenderableWidget(Button.builder(Component.translatable("gui.soundworks.open_folder"), b -> openFolder())
			.bounds(cx - 150, by, 148, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.soundworks.refresh"), b -> { reload(); update(); })
			.bounds(cx + 2, by, 148, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
			.bounds(cx - 100, by + 24, 200, 20).build());
		update();
	}

	private void reload() {
		files.clear();
		offset = 0;
		try (Stream<Path> s = Files.list(ClientAudio.musicDir())) {
			s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".mp3")).sorted().forEach(files::add);
		} catch (IOException ignored) {}
	}

	/** Only songs that really exist get a row; the rest of the rows are hidden. */
	private void update() {
		int rowWidth = ROW_W - 12;
		for (int i = 0; i < MAX_ROWS; i++) {
			int idx = offset + i;
			boolean has = i < visibleRows && idx < files.size();
			rows[i].visible = has;
			if (has) {
				Path f = files.get(idx);
				String size;
				try { size = String.format(" (%.1f MB)", Files.size(f) / 1048576.0); } catch (IOException e) { size = ""; }
				String name = Gui.fit(font, f.getFileName().toString(), rowWidth - font.width(size));
				rows[i].setMessage(Component.literal(name + size));
			} else {
				rows[i].setMessage(Component.empty());
			}
		}
		boolean scroll = files.size() > visibleRows;
		upButton.visible = scroll;
		downButton.visible = scroll;
		upButton.active = scroll && offset > 0;
		downButton.active = scroll && offset < files.size() - visibleRows;
	}

	private void openFolder() {
		Path dir = ClientAudio.musicDir();
		String os = System.getProperty("os.name", "").toLowerCase();
		String[] cmd = os.contains("win") ? new String[] {"explorer", dir.toString()}
			: os.contains("mac") ? new String[] {"open", dir.toString()}
			: new String[] {"xdg-open", dir.toString()};
		try { new ProcessBuilder(cmd).start(); } catch (IOException ignored) {}
	}

	@Override
	public void onClose() {
		ScreenOpener.open(parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.centeredText(font, title, width / 2, 14, 0xFFFFFFFF);
		if (files.isEmpty()) {
			int y = 44;
			for (String line : Gui.wrap(font, Component.translatable("gui.soundworks.no_files").getString(), ROW_W, 3)) {
				g.centeredText(font, Component.literal(line), width / 2, y, 0xFFB0B0B0);
				y += 11;
			}
		} else {
			g.centeredText(font, Component.translatable("gui.soundworks.count", files.size()), width / 2, 26, 0xFFB0B0B0);
		}
	}
}
