package dev.soundworks.client;

import dev.soundworks.Hooks;
import dev.soundworks.ModRegistry;
import dev.soundworks.client.audio.ClientAudio;
import dev.soundworks.client.audio.Uploader;
import dev.soundworks.client.screen.JukeboxScreen;
import dev.soundworks.client.screen.PressScreen;
import dev.soundworks.client.screen.SoundworksScreen;
import dev.soundworks.client.screen.TurntableScreen;
import dev.soundworks.net.Net;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;

public class SoundworksClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Hooks.audio = ClientAudio.INSTANCE;
		Hooks.openTurntable = pos -> ScreenOpener.open(new TurntableScreen(pos));

		MenuScreens.register(ModRegistry.PRESS_MENU, PressScreen::new);
		MenuScreens.register(ModRegistry.JUKEBOX_MENU, JukeboxScreen::new);

		ClientPlayNetworking.registerGlobalReceiver(Net.AudioChunk.TYPE, (payload, ctx) -> ClientAudio.INSTANCE.onChunk(payload));
		ClientPlayNetworking.registerGlobalReceiver(Net.JukeboxSync.TYPE, (payload, ctx) -> ClientAudio.INSTANCE.onVanillaSync(payload));

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			Uploader.pump();
			if (mc.level != null) ClientAudio.INSTANCE.tickVanilla(mc.level.getGameTime());
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> ClientAudio.INSTANCE.stopAll());

		// "Soundworks" button in Minecraft's Music & Sounds settings
		ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
			if (screen.getClass().getSimpleName().equals("SoundOptionsScreen")) {
				Screens.getWidgets(screen).add(Button.builder(Component.translatable("gui.soundworks.title"),
					b -> ScreenOpener.open(new SoundworksScreen(screen))).bounds(w - 106, 6, 100, 20).build());
			}
		});
	}
}
