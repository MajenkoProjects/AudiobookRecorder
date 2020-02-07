package uk.co.majenko.audiobookrecorder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioInputStream;

import javax.swing.JOptionPane;

public class Microphone {

    public static TargetDataLine device = null;
    public static AudioInputStream stream = null;

    public static boolean start() {
        Debug.trace();
        AudioFormat format = Options.getAudioFormat();
        Mixer.Info mixer = Options.getRecordingMixer();

        device = null;

        try {
            device = AudioSystem.getTargetDataLine(format, mixer);
        } catch (Exception e) {
            e.printStackTrace();
            device = null;
            return false;
        }

        if (device == null) {
            JOptionPane.showMessageDialog(AudiobookRecorder.window, "Sample format not supported", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        stream = new AudioInputStream(device);

        try {
            device.open();
        } catch (Exception e) {
            e.printStackTrace();
            device = null;
            return false;
        }

        device.start();
        return true;
    }

    public static void stop() {
        Debug.trace();
        try {
            stream.close();
            device.stop();
            device.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        device = null;
        stream = null;
    } 

    public static AudioInputStream getStream() {
        return stream;
    }

    public static TargetDataLine getDevice() {
        return device;
    }

    public static void flush() {
        if (device != null) {
            device.flush();
        }
    }
}
