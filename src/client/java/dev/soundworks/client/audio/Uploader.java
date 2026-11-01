package dev.soundworks.client.audio;

import dev.soundworks.audio.AudioStore;
import dev.soundworks.net.Net;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Reads the MP3, computes its hash and sends it to the server in chunks (a few per tick). */
public final class Uploader {
	private static final ConcurrentLinkedQueue<CustomPacketPayload> QUEUE = new ConcurrentLinkedQueue<>();

	private Uploader() {}

	public static void start(Path file, String title, BlockPos pos, Consumer<String> status) {
		Thread t = new Thread(() -> {
			try {
				long size = Files.size(file);
				if (size > AudioStore.MAX_BYTES) { status.accept("gui.soundworks.msg.too_big"); return; }
				byte[] data = Files.readAllBytes(file);
				String hash = AudioStore.sha256(data);
				// Also cache locally so the presser does not have to download it again
				Files.write(ClientAudio.cacheDir().resolve(hash + ".mp3"), data);
				int total = Math.max(1, (data.length + Net.UPLOAD_CHUNK - 1) / Net.UPLOAD_CHUNK);
				for (int i = 0; i < total; i++) {
					int from = i * Net.UPLOAD_CHUNK;
					int to = Math.min(data.length, from + Net.UPLOAD_CHUNK);
					QUEUE.add(new Net.UploadChunk(hash, i, total, Arrays.copyOfRange(data, from, to)));
				}
				QUEUE.add(new Net.PressRequest(pos, hash, title, false));
				status.accept("gui.soundworks.msg.uploading");
			} catch (Exception e) {
				status.accept("gui.soundworks.msg.read_error");
			}
		}, "Soundworks-Upload");
		t.setDaemon(true);
		t.start();
	}

	/** Call once per client tick. */
	public static void pump() {
		for (int i = 0; i < 6; i++) {
			CustomPacketPayload p = QUEUE.poll();
			if (p == null) return;
			ClientPlayNetworking.send(p);
		}
	}

	public static boolean busy() {
		return !QUEUE.isEmpty();
	}
}
