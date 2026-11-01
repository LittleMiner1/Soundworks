package dev.soundworks.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Opens a screen independent of where the game keeps setScreen: Minecraft#setScreen in older
 * versions, Minecraft#gui#setScreen in newer ones.
 */
public final class ScreenOpener {
	private ScreenOpener() {}

	public static void open(Screen screen) {
		Minecraft mc = Minecraft.getInstance();
		try {
			Method m = find(mc.getClass(), "setScreen");
			if (m != null) { m.invoke(mc, screen); return; }
			Field f = mc.getClass().getField("gui");
			Object gui = f.get(mc);
			Method g = find(gui.getClass(), "setScreen");
			if (g != null) g.invoke(gui, screen);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Cannot open screen", e);
		}
	}

	private static Method find(Class<?> c, String name) {
		for (Method m : c.getMethods()) {
			if (m.getName().equals(name) && m.getParameterCount() == 1
				&& m.getParameterTypes()[0].isAssignableFrom(Screen.class)) return m;
		}
		return null;
	}
}
