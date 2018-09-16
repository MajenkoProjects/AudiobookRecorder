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
import edu.cmu.sphinx.api.*;
import edu.cmu.sphinx.decoder.adaptation.*;
import edu.cmu.sphinx.result.*;

public class Sentence extends DefaultMutableTreeNode implements Cacheable {
    
    String text;
    String id;
    int postGap;
    int startOffset = 0;
    int endOffset = 0;

    int sampleSize = -1;

    boolean locked;

    boolean recording;

    boolean inSample;

    TargetDataLine line;
    AudioInputStream inputStream;

    Thread recordingThread = null;

    int[] storedAudioData = null;
    
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
        storedAudioData = null;

        if (!id.equals("room-noise")) {
            autoTrimSampleFFT();
            if (Options.getBoolean("process.sphinx")) {
                recognise();
            }
        }
    }

    public void autoTrimSampleFFT() {
        int[] samples = getAudioData();
        if (samples == null) return;

        int blocks = samples.length / 4096 + 1;

        int[] intens = new int[blocks];
        int block = 0;

        for (int i = 0; i < samples.length; i+= 4096) {
            double[] real = new double[4096];
            double[] imag = new double[4096];

            for (int j = 0; j < 4096; j++) {
                if (i + j < samples.length) {
                    real[j] = samples[i+j] / 32768d;
                    imag[j] = 0;
                } else {
                    real[j] = 0;
                    imag[j] = 0;
                }
            }

            double[] buckets = FFT.fft(real, imag, true);
            double av = 0;
            for (int j = 1; j < 2048; j++) {
                av += Math.abs(buckets[j]);
            }
            av /= 2047d;

            intens[block] = 0;

            for (int j = 1; j < 2048; j++) {
                double d = Math.abs(av - buckets[j]);
                if (d > 0.05) {
                    intens[block]++;
                }
            }
            block++;
            
        }

        // Find first block with > 0 intensity and subtract one.

        int start = 0;
        for (int i = 0; i < blocks; i++) {
            if (intens[i] > 0) break;
            start = i;
        }

        if (start >= blocks) {
            start = 0;
        }

        startOffset = start * 4096;
        if (startOffset < 0) startOffset = 0;
        if (startOffset >= samples.length) startOffset = samples.length;

        int end = blocks - 1;
        // And last block with > 0 intensity and add one.
        for (int i = blocks-1; i >= 0; i--) {
            if (intens[i] > 0) break;
            end = i;
        }

        if (end <= 0) {
            end = blocks - 1;
        }

        endOffset = end * 4096;

        if (endOffset <= startOffset) endOffset = startOffset + 4096;
        if (endOffset < 0) endOffset = 0;
        if (endOffset >= samples.length) endOffset = samples.length;

    }

    public void autoTrimSamplePeak() {
        int[] samples = getAudioData();
        if (samples == null) return;
        int noiseFloor = AudiobookRecorder.window.getNoiseFloor();

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
            startOffset = 0;
        }

        startOffset -= 4096;

        for (int i = samples.length-1; i >= 0; i--) {
            endOffset = i;
            if (Math.abs(samples[i]) > noiseFloor) {
                endOffset ++;
                if (endOffset >= samples.length-1) endOffset = samples.length-1;
                break;
            }
        }

        endOffset += 4096;

        if (endOffset <= startOffset) endOffset = startOffset + 4096;
        if (endOffset <= 0) {
            endOffset = samples.length-1;
        }

        if (startOffset < 0) startOffset = 0;
        if (endOffset >= samples.length) endOffset = samples.length-1;
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
        if (storedAudioData != null) {
            return storedAudioData;
        }
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
            storedAudioData = samples;
            CacheManager.addToCache(this);
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
            play.drain();
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

    public void recognise() {


        Thread t = new Thread(new Runnable() {

            public void run() {
                try {

                    Configuration sphinxConfig = new Configuration();

                    sphinxConfig.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
                    sphinxConfig.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
                    sphinxConfig.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");


                    StreamSpeechRecognizer recognizer;

                    recognizer = new StreamSpeechRecognizer(sphinxConfig);

                    AudioInputStream s = AudioSystem.getAudioInputStream(getFile());
                    AudioFormat format = s.getFormat();
                    int frameSize = format.getFrameSize();
                    int length = (int)s.getFrameLength();
                    byte[] data = new byte[length * frameSize];

                    s.read(data);

                    int channels = format.getChannels();
                    int newLen = (length / 3);
                    byte[] decimated = new byte[newLen * 2];

                    for (int i = 0; i < newLen; i++) {
                        if (channels == 1) {
                            decimated[i * 2] = data[i * 6];
                            decimated[i * 2 + 1] = data[i * 6 + 1];
                        } else {
                            decimated[i * 2] = data[i * 12];
                            decimated[i * 2 + 1] = data[i * 12 + 1];
                        }
                    }


                    System.err.println("Decimated from " + length + " to " + newLen);

                    ByteArrayInputStream bas = new ByteArrayInputStream(decimated);
                    recognizer.startRecognition(bas);
                    SpeechResult result;
                    String res = "";
                    while ((result = recognizer.getResult()) != null) {
                        res += result.getHypothesis();
                        res += " ";
                    }
                    recognizer.stopRecognition(); 

                    text = res;

                    AudiobookRecorder.window.bookTreeModel.reload(Sentence.this);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        t.start();
    }

    public void setLocked(boolean l) {
        locked = l;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setInSample(boolean s) {
        inSample = s;
    }

    public boolean isInSample() { 
        return inSample;
    }

    public void clearCache() {
        storedAudioData = null;
    }

    public boolean lockedInCache() {
        return id.equals("room-noise"); 
    }
}
