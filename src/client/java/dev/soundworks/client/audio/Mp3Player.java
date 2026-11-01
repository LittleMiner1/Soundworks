package dev.soundworks.client.audio;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

/**
 * Plays an MP3 file on its own thread (Java Sound). Output is always stereo: stereo MP3s keep their
 * channels, mono MP3s are duplicated, and the left/right gains place the sound in the world.
 * Speed changes also change the pitch, like on a real turntable.
 */
public final class Mp3Player implements Runnable {
	public final int session;
	public final String hash;
	private final Path file;
	private final long startMs;
	public volatile float gainL = 0f;
	public volatile float gainR = 0f;
	public volatile float speed = 1f;
	private volatile boolean stopped = false;

	public Mp3Player(Path file, String hash, int session, long startMs) {
		this.file = file;
		this.hash = hash;
		this.session = session;
		this.startMs = startMs;
		Thread thread = new Thread(this, "Soundworks-Audio");
		thread.setDaemon(true);
		thread.start();
	}

	public void stop() {
		stopped = true;
	}

	@Override
	public void run() {
		SourceDataLine line = null;
		try (InputStream in = new BufferedInputStream(Files.newInputStream(file), 1 << 16)) {
			Bitstream bs = new Bitstream(in);
			Decoder dec = new Decoder();
			double skipped = 0;
			while (skipped < startMs && !stopped) {
				Header h = bs.readFrame();
				if (h == null) return;
				skipped += h.ms_per_frame();
				bs.closeFrame();
			}
			byte[] out = new byte[16384];
			while (!stopped) {
				Header h = bs.readFrame();
				if (h == null) break;
				SampleBuffer sb = (SampleBuffer) dec.decodeFrame(h, bs);
				bs.closeFrame();
				if (line == null) {
					AudioFormat fmt = new AudioFormat(dec.getOutputFrequency(), 16, 2, true, false);
					line = AudioSystem.getSourceDataLine(fmt);
					line.open(fmt, 1 << 15);
					line.start();
				}
				int ch = Math.max(1, dec.getOutputChannels());
				short[] pcm = sb.getBuffer();
				int frames = sb.getBufferLength() / ch;
				if (frames < 2) continue;
				float s = Math.max(0.25f, Math.min(4f, speed));
				float gl = gainL, gr = gainR;
				int outFrames = (int) (frames / s);
				int needed = outFrames * 4;
				if (out.length < needed) out = new byte[needed];
				int o = 0;
				for (int j = 0; j < outFrames; j++) {
					double src = j * (double) s;
					int i0 = Math.min((int) src, frames - 1);
					int i1 = Math.min(i0 + 1, frames - 1);
					float frac = (float) (src - i0);
					float l = lerp(pcm[i0 * ch], pcm[i1 * ch], frac);
					float r = ch > 1 ? lerp(pcm[i0 * ch + 1], pcm[i1 * ch + 1], frac) : l;
					o = put(out, o, l * gl);
					o = put(out, o, r * gr);
				}
				line.write(out, 0, o);
			}
			if (line != null && !stopped) line.drain();
		} catch (Throwable t) {
			dev.soundworks.SoundworksMod.LOGGER.warn("Playback failed: {}", t.toString());
		} finally {
			if (line != null) {
				line.stop();
				line.flush();
				line.close();
			}
		}
	}

	private static float lerp(float a, float b, float f) {
		return a + (b - a) * f;
	}

	private static int put(byte[] out, int o, float sample) {
		int v = Math.round(sample);
		if (v > 32767) v = 32767;
		else if (v < -32768) v = -32768;
		out[o++] = (byte) (v & 0xFF);
		out[o++] = (byte) ((v >> 8) & 0xFF);
		return o;
	}
}
