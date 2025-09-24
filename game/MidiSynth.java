package game;

import javax.sound.midi.*;
import java.io.File;

public class MidiSynth {
    private static Synthesizer synth;
    private static MidiChannel channel;

    /**
     * Initialize synthesizer, optionally with a custom SoundFont (.sf2).
     * @param soundFontPath path to a .sf2 file, or null for default soundbank
     */
    public static void init(String soundFontPath) {
        try {
            if (synth == null) {
                synth = MidiSystem.getSynthesizer();
                synth.open();

                // Try to load custom SoundFont if provided
                if (soundFontPath != null) {
                    File sf2File = new File(soundFontPath);
                    if (sf2File.exists()) {
                        Soundbank sb = MidiSystem.getSoundbank(sf2File);
                        if (synth.isSoundbankSupported(sb)) {
                            synth.unloadAllInstruments(synth.getDefaultSoundbank());
                            synth.loadAllInstruments(sb);

                            System.out.println("Loaded custom SoundFont: " + soundFontPath);
                            System.out.println("Instrument count: " + sb.getInstruments().length);
                            System.out.println("Load success? ");
                            Instrument[] instruments = synth.getAvailableInstruments();
                            for (int i = 0; i < instruments.length; i++) {
                                System.out.println(i + ": " + instruments[i].getName());
                            }
                            System.out.println("Loaded custom SoundFont: " + soundFontPath);
                        } else {
                            System.out.println("SoundFont not supported, using default soundbank.");
                        }
                    } else {
                        System.out.println("SoundFont file not found, using default soundbank.");
                    }
                }

                channel = synth.getChannels()[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Overload to init without soundfont
    public static void init() {
        init(null);
    }
 // --- Scale mapping ---
    public static final int[] PHRYGIAN_DOMINANT = {
        65, 67, 68, 70,
        72, 73, 76, 77, 79, 80, 82
    };

    public static final int[] HIJAZ = {
        65, 67, 68, 71,
        72, 73, 76, 77, 79, 80, 83
    };
    

	public static final int[] ARABIC = {
	    65, 67, 68, 71,
	    72, 73, 76, 77, 79, 80, 83
	};


	public enum ScaleType { PHRYGIAN_DOMINANT, HIJAZ, ARABIC }


    public static int snapToScale(int pitch, ScaleType scale) {
        int[] notes = (scale == ScaleType.HIJAZ) ? HIJAZ : PHRYGIAN_DOMINANT;
        int closest = notes[0];
        int minDiff = Math.abs(pitch - closest);
        for (int n : notes) {
            int d = Math.abs(pitch - n);
            if (d < minDiff) {
                minDiff = d;
                closest = n;
            }
        }
        return closest;
    }

    // Optional: print all notes in a scale for debugging
    public static void printScale(ScaleType scale) {
        int[] notes = (scale == ScaleType.HIJAZ) ? HIJAZ : PHRYGIAN_DOMINANT;
        System.out.print(scale + " notes: ");
        for (int n : notes) System.out.print(n + " ");
        System.out.println();
    }


    public static void setInstrument(int program) {
        if (channel != null) {
            channel.programChange(program);
        }
    }

    // 🎚 Brightness control (0–127)
    public static void setBrightness(int brightness) {
        if (channel != null) {
            channel.controlChange(74, Math.max(0, Math.min(127, brightness)));
        }
    }

    public static void playTone(int note, int velocity, int durationMs) {
        if (channel == null) return;
        channel.noteOn(note, velocity);
        new Thread(() -> {
            try {
                Thread.sleep(durationMs);
            } catch (InterruptedException ignored) {}
            channel.noteOff(note);
        }).start();
    }

    // 🎚 Pan control (0 = hard left, 64 = center, 127 = hard right)
    public static void setPan(int pan) {
        if (channel != null) {
            channel.controlChange(10, Math.max(0, Math.min(127, pan)));
        }
    }

    // 🎚 Volume control (0–127)
    public static void setVolume(int volume) {
        if (channel != null) {
            channel.controlChange(7, Math.max(0, Math.min(127, volume)));
        }
    }
}
