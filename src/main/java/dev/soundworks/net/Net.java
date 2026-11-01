package dev.soundworks.net;

import dev.soundworks.SoundworksMod;
import dev.soundworks.audio.AudioStore;
import dev.soundworks.block.Controllable;
import dev.soundworks.block.PressBlockEntity;
import io.netty.buffer.ByteBuf;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;

public final class Net {
	private Net() {}

	/** C2S packets are limited to 32 KiB, so chunks are small. */
	public static final int UPLOAD_CHUNK = 30_000;
	/** S2C packets may be up to 1 MiB. */
	public static final int DOWNLOAD_CHUNK = 500_000;

	public record UploadChunk(String hash, int index, int total, byte[] data) implements CustomPacketPayload {
		public static final Type<UploadChunk> TYPE = new Type<>(SoundworksMod.id("upload"));
		public static final StreamCodec<ByteBuf, UploadChunk> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, UploadChunk::hash,
			ByteBufCodecs.VAR_INT, UploadChunk::index,
			ByteBufCodecs.VAR_INT, UploadChunk::total,
			ByteBufCodecs.BYTE_ARRAY, UploadChunk::data,
			UploadChunk::new);
		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record PressRequest(BlockPos pos, String hash, String title, boolean copy) implements CustomPacketPayload {
		public static final Type<PressRequest> TYPE = new Type<>(SoundworksMod.id("press"));
		public static final StreamCodec<ByteBuf, PressRequest> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, PressRequest::pos,
			ByteBufCodecs.STRING_UTF8, PressRequest::hash,
			ByteBufCodecs.STRING_UTF8, PressRequest::title,
			ByteBufCodecs.BOOL, PressRequest::copy,
			PressRequest::new);
		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record DeviceAction(BlockPos pos, int action, float value) implements CustomPacketPayload {
		public static final Type<DeviceAction> TYPE = new Type<>(SoundworksMod.id("action"));
		public static final StreamCodec<ByteBuf, DeviceAction> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, DeviceAction::pos,
			ByteBufCodecs.VAR_INT, DeviceAction::action,
			ByteBufCodecs.FLOAT, DeviceAction::value,
			DeviceAction::new);
		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record AudioRequest(String hash) implements CustomPacketPayload {
		public static final Type<AudioRequest> TYPE = new Type<>(SoundworksMod.id("audio_request"));
		public static final StreamCodec<ByteBuf, AudioRequest> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, AudioRequest::hash, AudioRequest::new);
		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record AudioChunk(String hash, int index, int total, byte[] data) implements CustomPacketPayload {
		public static final Type<AudioChunk> TYPE = new Type<>(SoundworksMod.id("audio_chunk"));
		public static final StreamCodec<ByteBuf, AudioChunk> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, AudioChunk::hash,
			ByteBufCodecs.VAR_INT, AudioChunk::index,
			ByteBufCodecs.VAR_INT, AudioChunk::total,
			ByteBufCodecs.BYTE_ARRAY, AudioChunk::data,
			AudioChunk::new);
		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	/** Server -> clients: state of a vanilla jukebox that plays a Soundworks record. */
	public record JukeboxSync(BlockPos pos, String hash, int lengthMs, long startTick, int session, boolean playing) implements CustomPacketPayload {
		public static final Type<JukeboxSync> TYPE = new Type<>(SoundworksMod.id("jukebox_sync"));
		public static final StreamCodec<ByteBuf, JukeboxSync> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, JukeboxSync::pos,
			ByteBufCodecs.STRING_UTF8, JukeboxSync::hash,
			ByteBufCodecs.VAR_INT, JukeboxSync::lengthMs,
			ByteBufCodecs.VAR_LONG, JukeboxSync::startTick,
			ByteBufCodecs.VAR_INT, JukeboxSync::session,
			ByteBufCodecs.BOOL, JukeboxSync::playing,
			JukeboxSync::new);
		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	/** Shows a short message above the hotbar (replaces the removed Player#displayClientMessage). */
	public static void overlay(ServerPlayer player, Component message) {
		player.connection.send(new ClientboundSetActionBarTextPacket(message));
	}

	/** True if the player is within 8 blocks of the block (squared distance <= 64). */
	public static boolean inReach(ServerPlayer player, BlockPos pos) {
		return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
	}

	public static void initCommon() {
		PayloadTypeRegistry.serverboundPlay().register(UploadChunk.TYPE, UploadChunk.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PressRequest.TYPE, PressRequest.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DeviceAction.TYPE, DeviceAction.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(AudioRequest.TYPE, AudioRequest.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(AudioChunk.TYPE, AudioChunk.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(JukeboxSync.TYPE, JukeboxSync.CODEC);
	}

	private static final Map<String, ByteArrayOutputStream> UPLOADS = new ConcurrentHashMap<>();

	public static void initServer() {
		// MP3 upload from a client (in chunks)
		ServerPlayNetworking.registerGlobalReceiver(UploadChunk.TYPE, (p, ctx) -> {
			ServerPlayer player = ctx.player();
			var server = ctx.server();
			if (!AudioStore.isValidHash(p.hash()) || p.total() <= 0
				|| (long) p.total() * UPLOAD_CHUNK > AudioStore.MAX_BYTES + UPLOAD_CHUNK) return;
			String key = player.getUUID() + ":" + p.hash();
			if (AudioStore.has(server, p.hash())) { UPLOADS.remove(key); return; }
			if (p.index() == 0) UPLOADS.put(key, new ByteArrayOutputStream());
			ByteArrayOutputStream buf = UPLOADS.get(key);
			if (buf == null) return;
			if (p.data().length > UPLOAD_CHUNK) { UPLOADS.remove(key); return; }
			buf.writeBytes(p.data());
			if (buf.size() > AudioStore.MAX_BYTES) {
				UPLOADS.remove(key);
				overlay(player, Component.translatable("gui.soundworks.msg.too_big"));
				return;
			}
			if (p.index() == p.total() - 1) {
				UPLOADS.remove(key);
				if (!AudioStore.store(server, p.hash(), buf.toByteArray())) {
					overlay(player, Component.translatable("gui.soundworks.msg.bad_mp3"));
				}
			}
		});

		// Operate the press (press / copy)
		ServerPlayNetworking.registerGlobalReceiver(PressRequest.TYPE, (p, ctx) -> {
			ServerPlayer player = ctx.player();
			if (!inReach(player, p.pos())) return;
			if (player.level().getBlockEntity(p.pos()) instanceof PressBlockEntity be) {
				if (p.copy()) be.copy(player);
				else be.press(player, p.hash(), p.title());
			}
		});

		// Operate turntable / jukebox
		ServerPlayNetworking.registerGlobalReceiver(DeviceAction.TYPE, (p, ctx) -> {
			ServerPlayer player = ctx.player();
			if (!inReach(player, p.pos())) return;
			if (player.level().getBlockEntity(p.pos()) instanceof Controllable c) {
				c.handleAction(player, p.action(), p.value());
			}
		});

		// Client is missing an MP3 -> send it in chunks
		ServerPlayNetworking.registerGlobalReceiver(AudioRequest.TYPE, (p, ctx) -> {
			ServerPlayer player = ctx.player();
			if (!AudioStore.has(ctx.server(), p.hash())) return;
			try {
				byte[] data = AudioStore.read(ctx.server(), p.hash());
				int total = Math.max(1, (data.length + DOWNLOAD_CHUNK - 1) / DOWNLOAD_CHUNK);
				for (int i = 0; i < total; i++) {
					int from = i * DOWNLOAD_CHUNK;
					int to = Math.min(data.length, from + DOWNLOAD_CHUNK);
					ServerPlayNetworking.send(player, new AudioChunk(p.hash(), i, total, java.util.Arrays.copyOfRange(data, from, to)));
				}
			} catch (IOException e) {
				SoundworksMod.LOGGER.warn("Could not read {}", p.hash(), e);
			}
		});
	}
}
