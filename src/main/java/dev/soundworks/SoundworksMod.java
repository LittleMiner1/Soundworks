package dev.soundworks;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoundworksMod implements ModInitializer {
	public static final String MOD_ID = "soundworks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModRegistry.init();
		dev.soundworks.net.Net.initCommon();
		dev.soundworks.net.Net.initServer();
		VanillaJukeboxSync.init();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
