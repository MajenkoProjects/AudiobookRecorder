package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import javax.swing.tree.*;
import javax.sound.sampled.*;

public class Sentence extends DefaultMutableTreeNode {
    
    String text;
    String id;
    int postGap;
    int startOffset = 0;
    int endOffset = 0;

    int sampleSize = -1;

    boolean isSilence = false;

    boolean recording;

    TargetDataLine line;
    AudioInputStream inputStream;

    Thread recordingThread = null;
    

    public Sentence() {
        super("");
        id = UUID.randomUUID().toString();
        text = id;
        setUserObject(text);
        postGap = Options.getInteger("catenation.post-sentence");
    }

    public Sentence(String i, String t) {
        super("");
        id = i;
        text = t;
        setUserObject(text);
        postGap = Options.getInteger("catenation.post-sentence");
    }

    public boolean startRecording() {
        AudioFormat format = new AudioFormat(
            Options.getInteger("audio.recording.samplerate"),
            16,
            Options.getInteger("audio.recording.channels"),
            true,
            false
        );

        if (format == null) {
            JOptionPane.showMessageDialog(AudiobookRecorder.window, "Sample format not supported", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        Mixer.Info mixer = Options.getRecordingMixer();

        line = null;

        try {
            line = AudioSystem.getTargetDataLine(format, mixer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (line == null) {
            JOptionPane.showMessageDialog(AudiobookRecorder.window, "Sample format not supported", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        inputStream = new AudioInputStream(line);

        try {
            line.open();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        line.start();

        File audioFile = getFile();

        recordingThread = new Thread(new Runnable() {
            public void run() {
                try {
                    AudioSystem.write(inputStream, AudioFileFormat.Type.WAVE, audioFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        recordingThread.setDaemon(true);

        recordingThread.start();

        recording = true;
        return true;
    }

    public void stopRecording() {
        try {
            inputStream.close();
            line.stop();
            line.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        recording = false;

        if (!id.equals("room-noise")) {
            autoTrimSample();
        }
    }

    public void autoTrimSample() {
        int[] samples = getAudioData();
        int noiseFloor = AudiobookRecorder.window.getNoiseFloor();

        isSilence = false;
        // Find start
        for (int i = 0; i < samples.length; i++) {
            startOffset = i;
            if (Math.abs(samples[i]) > noiseFloor) {
                startOffset --;
                if (startOffset < 0) startOffset = 0;
                break;
            }
        }

        if (startOffset >= samples.length-1) { // Failed! Silence?
            isSilence = true;
            startOffset = 0;
        }

        for (int i = samples.length-1; i >= 0; i--) {
            endOffset = i;
            if (Math.abs(samples[i]) > noiseFloor) {
                endOffset ++;
                if (endOffset >= samples.length-1) endOffset = samples.length-1;
                break;
            }
        }
        if (endOffset <= 0) {
            isSilence = true;
            endOffset = samples.length-1;
        }
        
    }

    public String getId() {
        return id;
    }

    public void setText(String t) {
        text = t;
    }

    public String getText() {
        return text;
    }

    public File getFile() {
        File b = new File(AudiobookRecorder.window.getBookFolder(), "files");
        if (!b.exists()) {
            b.mkdirs();
        }
        return new File(b, id + ".wav");
    }

    public void editText() {
        String t = JOptionPane.showInputDialog(null, "Edit Text", text);

        if (t != null) {
            text = t;
        }

    }

    public String toString() {
        return text;
    }

    public boolean isRecording() {
        return recording;
    }

    public void setUserObject(Object o) {
        if (o instanceof String) {
            String so = (String)o;
            text = so;
        }
    }

    public int getPostGap() {
        return postGap;
    }

    public void setPostGap(int g) {
        postGap = g;
    }

    public void deleteFiles() {
        File audioFile = getFile();
        if (audioFile.exists()) {
            audioFile.delete();
        }
    }

    public int[] getAudioData() {
        File f = getFile();
        try {
            AudioInputStream s = AudioSystem.getAudioInputStream(f);
            AudioFormat format = s.getFormat();

            long len = s.getFrameLength();
            int frameSize = format.getFrameSize();
            int chans = format.getChannels();
            int bytes = frameSize / chans;

            byte[] frame = new byte[frameSize];
            int[] samples = new int[(int)len];

            if (bytes != 2) return null;

            for (long fno = 0; fno < len; fno++) {
                s.read(frame);
                int sample = 0;
                if (chans == 2) { // Stereo
                    int left = (frame[1] << 8) | frame[0];
                    int right = (frame[3] << 8) | frame[2];
                    sample = (left + right) / 2;
                } else {
                    sample = (frame[1] << 8) | frame[0];
                }
                samples[(int)fno] = sample;
            }
            s.close();
            sampleSize = samples.length;
            return samples;
        } catch (Exception e) {
        }
        return null;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(int o) {
        startOffset = o;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(int o) {
        endOffset = o;
    }

    public int getSampleSize() {
        if (sampleSize == -1) {
            getAudioData();
        }
        return sampleSize;
    }

    // Open the audio file as an AudioInputStream and automatically
    // skip the first startOffset frames.
    public AudioInputStream getAudioStream() {
        File f = getFile();
        try {
            AudioInputStream s = AudioSystem.getAudioInputStream(f);
            AudioFormat format = s.getFormat();
            long len = s.getFrameLength();
            int frameSize = format.getFrameSize();

            s.skip(frameSize * startOffset);
             
            return s;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public AudioFormat getAudioFormat() {
        File f = getFile();
        try {
            AudioInputStream s = AudioSystem.getAudioInputStream(f);
            AudioFormat format = s.getFormat();
            return format;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public void play() {
        File f = getFile();
        try {
            AudioInputStream s = AudioSystem.getAudioInputStream(f);
            AudioFormat format = s.getFormat();
            long len = s.getFrameLength();
            int frameSize = format.getFrameSize();

            int pos = startOffset * frameSize;

            SourceDataLine play = AudioSystem.getSourceDataLine(format, Options.getPlaybackMixer());
            play.open(format);
    
            play.start();

            byte[] buffer = new byte[1024];

            s.skip(pos);
            
            while (pos < endOffset * frameSize) {
                int nr = s.read(buffer);
                pos += nr;

                play.write(buffer, 0, nr);
            };
            play.close(); 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getRawAudioData() {
        File f = getFile();
        try {
            AudioInputStream s = AudioSystem.getAudioInputStream(f);
            AudioFormat format = s.getFormat();
            int frameSize = format.getFrameSize();
            int length = endOffset - startOffset;
            byte[] data = new byte[length * frameSize];

            s.skip(startOffset * frameSize);

            s.read(data);

            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
