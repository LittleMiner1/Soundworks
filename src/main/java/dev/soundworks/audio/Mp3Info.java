package dev.soundworks.audio;

import java.io.ByteArrayInputStream;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Header;

public final class Mp3Info {
	private Mp3Info() {}

	/** Length in seconds (rounded up) or -1 if the data is not a valid MP3. */
	public static int durationSec(byte[] data) {
		double ms = 0;
		int frames = 0;
		Bitstream bs = new Bitstream(new ByteArrayInputStream(data));
		try {
			Header h;
			while ((h = bs.readFrame()) != null && frames < 400_000) {
				ms += h.ms_per_frame();
				frames++;
				bs.closeFrame();
			}
		} catch (Exception ignored) {
			// ignore broken trailing frames
		}
		return frames < 10 ? -1 : (int) Math.ceil(ms / 1000.0);
	}
}
