package io.github.ezequiel24123z.grindless.assetgen;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Synthesises the mod's industrial sound effects.
 *
 * <p>Sound was written off as impossible before being tried, and that was wrong: the JDK's
 * {@code javax.sound.sampled} writes WAV with no third-party library, and industrial noise — hums,
 * relays, charge-ups, alarms — is exactly the family that synthesises well (ADR-0048).
 *
 * <p>What does <em>not</em> work this way is music and anything recorded. The limit is tonal and
 * organic sound, not sound as such.
 *
 * <h2>Looping without a click</h2>
 *
 * <p>A looping sound whose waveform does not meet itself at the join produces an audible tick once
 * per loop, which is maddening on a machine the player stands next to for hours. Every looping
 * sound here is built only from harmonics of its own loop frequency, so the waveform is
 * periodic over exactly the loop length and the join is seamless by construction rather than by
 * fading.
 */
public final class SoundSynth {

    /** Minecraft resamples anyway, so 44.1 kHz mono is plenty and keeps files small. */
    public static final float SAMPLE_RATE = 44_100f;

    private SoundSynth() {
    }

    /**
     * A seamless machine hum.
     *
     * @param seconds     loop length; every partial is a harmonic of {@code 1/seconds}
     * @param fundamental base frequency in Hz, rounded to a harmonic of the loop
     * @param grit        0 to 1, how much filtered noise rides on top
     */
    public static double[] hum(double seconds, double fundamental, double grit) {
        int frames = (int) (SAMPLE_RATE * seconds);
        double[] samples = new double[frames];
        Random random = new Random(Double.hashCode(fundamental));
        // Snap every partial onto a harmonic of the loop frequency so the loop is exact.
        double loopHz = 1.0 / seconds;
        double root = Math.max(1, Math.round(fundamental / loopHz)) * loopHz;
        double filtered = 0;
        for (int i = 0; i < frames; i++) {
            double t = i / (double) SAMPLE_RATE;
            double value = Math.sin(2 * Math.PI * root * t) * 0.45
                    + Math.sin(2 * Math.PI * root * 1.5 * t) * 0.18
                    + Math.sin(2 * Math.PI * root * 3 * t) * 0.07;
            // A one-pole low-pass on white noise: mechanical rumble rather than hiss.
            filtered = filtered * 0.97 + (random.nextDouble() * 2 - 1) * 0.03;
            samples[i] = value + filtered * grit;
        }
        return samples;
    }

    /**
     * A rising chirp with a bright tail: something connecting, charging or completing.
     *
     * @param seconds duration
     * @param from    starting frequency in Hz
     * @param to      ending frequency in Hz
     */
    public static double[] chirp(double seconds, double from, double to) {
        int frames = (int) (SAMPLE_RATE * seconds);
        double[] samples = new double[frames];
        for (int i = 0; i < frames; i++) {
            double t = i / (double) SAMPLE_RATE;
            double progress = i / (double) frames;
            // Quadratic sweep: most of the movement happens late, which reads as "building up"
            // rather than as a siren.
            double freq = from + (to - from) * progress * progress;
            double envelope = Math.min(1, progress * 18) * Math.pow(1 - progress, 1.6);
            double tone = Math.sin(2 * Math.PI * freq * t);
            // A slightly detuned octave adds shimmer without sounding like a pure test tone.
            double shimmer = Math.sin(2 * Math.PI * freq * 2.02 * t) * 0.3;
            samples[i] = (tone + shimmer) * envelope * 0.6;
        }
        return samples;
    }

    /** A short mechanical click: a relay, a switch, a card slotting home. */
    public static double[] click(double seconds, double brightness) {
        int frames = (int) (SAMPLE_RATE * seconds);
        double[] samples = new double[frames];
        Random random = new Random(Double.hashCode(brightness));
        double resonator = 0;
        double velocity = 0;
        for (int i = 0; i < frames; i++) {
            double progress = i / (double) frames;
            double impulse = i < 3 ? 1.0 : 0.0;
            // A damped resonator struck once: the physical model of something small and hard
            // being hit, which is what a relay is.
            velocity += impulse - resonator * brightness * 0.6 - velocity * 0.08;
            resonator += velocity * 0.4;
            samples[i] = (resonator + (random.nextDouble() * 2 - 1) * 0.05)
                    * Math.pow(1 - progress, 3.0);
        }
        return samples;
    }

    /** A two-tone alarm, for a brownout or a process running out of band. */
    public static double[] alarm(double seconds, double low, double high) {
        int frames = (int) (SAMPLE_RATE * seconds);
        double[] samples = new double[frames];
        for (int i = 0; i < frames; i++) {
            double t = i / (double) SAMPLE_RATE;
            double progress = i / (double) frames;
            double freq = progress < 0.5 ? low : high;
            // A square-ish wave: urgent without being piercing.
            double tone = Math.signum(Math.sin(2 * Math.PI * freq * t)) * 0.4
                    + Math.sin(2 * Math.PI * freq * t) * 0.3;
            double gate = Math.min(1, Math.min(progress, 1 - progress) * 30);
            samples[i] = tone * gate * 0.5;
        }
        return samples;
    }

    /**
     * Writes samples as a 16-bit mono WAV.
     *
     * @param fadeEnds whether to taper the first and last few milliseconds; correct for one-shot
     *                 sounds and <em>wrong</em> for loops, where it would create the very
     *                 discontinuity the harmonic construction avoids
     */
    public static void write(File file, double[] samples, boolean fadeEnds) throws IOException {
        byte[] pcm = new byte[samples.length * 2];
        int fade = 64;
        for (int i = 0; i < samples.length; i++) {
            double value = samples[i];
            if (fadeEnds) {
                if (i < fade) {
                    value *= i / (double) fade;
                }
                if (i > samples.length - fade) {
                    value *= (samples.length - i) / (double) fade;
                }
            }
            int sample = (int) Math.max(Short.MIN_VALUE,
                    Math.min(Short.MAX_VALUE, value * Short.MAX_VALUE));
            pcm[i * 2] = (byte) (sample & 0xFF);
            pcm[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        try (AudioInputStream stream = new AudioInputStream(
                new ByteArrayInputStream(pcm), format, pcm.length / 2)) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file);
        }
    }
}
