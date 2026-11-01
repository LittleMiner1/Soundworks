package dev.soundworks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Data of a pressed record: MP3 hash (SHA-256), title, length in seconds. */
public record DiscData(String hash, String title, int lengthSec) {
	public static final Codec<DiscData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.fieldOf("hash").forGetter(DiscData::hash),
		Codec.STRING.fieldOf("title").forGetter(DiscData::title),
		Codec.INT.fieldOf("length").forGetter(DiscData::lengthSec)
	).apply(i, DiscData::new));

	public static final StreamCodec<ByteBuf, DiscData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, DiscData::hash,
		ByteBufCodecs.STRING_UTF8, DiscData::title,
		ByteBufCodecs.VAR_INT, DiscData::lengthSec,
		DiscData::new
	);
}
