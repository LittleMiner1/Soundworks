package dev.soundworks.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

final class Gui {
	private Gui() {}

	static final int TEXT = 0xFF404040;

	/** Vanilla-style gray panel with a bevel and slot frames (no texture needed). */
	static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, AbstractContainerMenu menu) {
		g.fill(x, y, x + w, y + h, 0xFF000000);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
		g.fill(x + 1, y + 1, x + w - 2, y + 3, 0xFFFFFFFF);
		g.fill(x + 1, y + 1, x + 3, y + h - 2, 0xFFFFFFFF);
		g.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
		g.fill(x + w - 3, y + 2, x + w - 1, y + h - 1, 0xFF555555);
		g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFC6C6C6);
		if (menu == null) return;
		for (Slot s : menu.slots) slot(g, x + s.x, y + s.y);
	}

	static void slot(GuiGraphicsExtractor g, int sx, int sy) {
		g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
		g.fill(sx, sy, sx + 17, sy + 17, 0xFFFFFFFF);
		g.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
	}

	/** Recessed dark area (e.g. behind a file list). */
	static void inset(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
		g.fill(x0, y0, x1, y1, 0xFFFFFFFF);
		g.fill(x0, y0, x1 - 1, y1 - 1, 0xFF373737);
		g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFF6B6B6B);
	}

	/** Shortens a text with "..." so that it is never wider than maxWidth pixels. */
	static String fit(Font font, String text, int maxWidth) {
		if (font.width(text) <= maxWidth) return text;
		String dots = "...";
		int avail = Math.max(0, maxWidth - font.width(dots));
		return font.plainSubstrByWidth(text, avail) + dots;
	}

	/** Breaks a text into lines that are at most maxWidth pixels wide (at most maxLines lines, the last one is shortened). */
	static List<String> wrap(Font font, String text, int maxWidth, int maxLines) {
		List<String> lines = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		for (String word : text.split(" ")) {
			String test = cur.length() == 0 ? word : cur + " " + word;
			if (font.width(test) <= maxWidth) {
				cur.setLength(0);
				cur.append(test);
			} else {
				if (cur.length() > 0) lines.add(cur.toString());
				cur.setLength(0);
				cur.append(font.width(word) <= maxWidth ? word : fit(font, word, maxWidth));
			}
		}
		if (cur.length() > 0) lines.add(cur.toString());
		if (lines.size() > maxLines) {
			lines = new ArrayList<>(lines.subList(0, maxLines));
			lines.set(maxLines - 1, fit(font, lines.get(maxLines - 1) + "...", maxWidth));
		}
		return lines;
	}
}
