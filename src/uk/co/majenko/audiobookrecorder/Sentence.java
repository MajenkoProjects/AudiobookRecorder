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
import davaguine.jeq.spi.EqualizerInputStream;
import davaguine.jeq.core.IIRControls;

public class Sentence extends DefaultMutableTreeNode implements Cacheable {
    
    String text;
    String id;
    int postGap;
    int startOffset = 0;
    int endOffset = 0;
    int crossStartOffset = -1;
    int crossEndOffset = -1;

    int sampleSize = -1;

    boolean locked;

    boolean recording;

    boolean inSample;

    TargetDataLine line;
    AudioInputStream inputStream;

    int[] storedAudioData = null;
    
    RecordingThread recordingThread;

    static class RecordingThread implements Runnable {

        boolean running = false;
        boolean recording = false;

        File tempFile;
        File wavFile;

        AudioFormat format;

        public RecordingThread(File tf, File wf, AudioFormat af) {
            tempFile = tf;
            wavFile = wf;
            format = af;
        }

        public void run() {
            try {
                running = true;
                recording = true;
                byte[] buf = new byte[AudiobookRecorder.window.microphone.getBufferSize()];
                FileOutputStream fos = new FileOutputStream(tempFile);
                int len = 0;
                AudiobookRecorder.window.microphone.flush();
                int nr = 0;
                while (recording) {
                    nr = AudiobookRecorder.window.microphoneStream.read(buf, 0, buf.length);
                    len += nr;
                    fos.write(buf, 0, nr);
                }
                nr = AudiobookRecorder.window.microphoneStream.read(buf, 0, buf.length);
                len += nr;
                fos.write(buf, 0, nr);
                fos.close();

                FileInputStream fis = new FileInputStream(tempFile);
                AudioInputStream ais = new AudioInputStream(fis, format, len / format.getFrameSize());
                fos = new FileOutputStream(wavFile);
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fos);
                fos.close();
                ais.close();
                fis.close();

                tempFile.delete();

                recording = false;
                running = false;
            } catch (Exception e) {
                e.printStackTrace();
                running = false;
                recording = false;
                running = false;
            }
        }

        public boolean isRunning() {
            return running;
        }

        public void stopRecording() {
            recording = false;
        }
    }

    
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
        if (AudiobookRecorder.window.microphone == null) {
            JOptionPane.showMessageDialog(AudiobookRecorder.window, "Microphone not started. Start the microphone first.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }


        recordingThread = new RecordingThread(getTempFile(), getFile(), Options.getAudioFormat());

        Thread rc = new Thread(recordingThread);
        rc.setDaemon(true);
        rc.start();

        return true;
    }

    public void stopRecording() {
        recordingThread.stopRecording();
        while (recordingThread.isRunning()) {
            try {
                Thread.sleep(10);
            } catch (Exception e) {
            }
        }

        storedAudioData = null;

        if (!id.equals("room-noise")) {
            autoTrimSamplePeak();
            if (Options.getBoolean("process.sphinx")) {
                recognise();
            }
        }
    }

    public void autoTrimSampleFFT() {
        crossStartOffset = -1;
        crossEndOffset = -1;
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
        updateCrossings();

    }

    public void autoTrimSamplePeak() {
        crossStartOffset = -1;
        crossEndOffset = -1;
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
        updateCrossings();
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

    public File getTempFile() {
        File b = new File(AudiobookRecorder.window.getBookFolder(), "files");
        if (!b.exists()) {
            b.mkdirs();
        }
        return new File(b, id + ".wax");
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

    public int[] getAudioDataS16LE(AudioInputStream s, AudioFormat format) throws IOException {
        long len = s.getFrameLength();
        int frameSize = format.getFrameSize();
        int chans = format.getChannels();
        int bytes = frameSize / chans;

        byte[] frame = new byte[frameSize];
        int[] samples = new int[(int)len];

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

        return samples;
    }

    public int[] getAudioData() {
        if (storedAudioData != null) {
            return storedAudioData;
        }
        File f = getFile();
        try {
            AudioInputStream s = AudioSystem.getAudioInputStream(f);
            AudioFormat format = s.getFormat();

            int[] samples = null;

            switch (format.getSampleSizeInBits()) {
                case 16:
                    samples = getAudioDataS16LE(s, format);
                    break;
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

    public int getStartCrossing() {
        return crossStartOffset;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public void updateCrossings() {
        updateStartCrossing();
        updateEndCrossing();
    }

    public void updateStartCrossing() {
        if (crossStartOffset == -1) {
            crossStartOffset = findNearestZeroCrossing(startOffset, 4096);
        }
    }

    public void updateEndCrossing() {
        if (crossEndOffset == -1) {
            crossEndOffset = findNearestZeroCrossing(endOffset, 4096);
        }
    }

    public void setStartOffset(int o) {
        if (startOffset != o) {
            startOffset = o;
            crossStartOffset = -1;
        }
    }

    public int getEndCrossing() {
        return crossEndOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(int o) {
        if (endOffset != o) {
            endOffset = o;
            crossEndOffset = -1;
        }
    }

    public int getSampleSize() {
        if (sampleSize == -1) {
            getAudioData();
        }
        return sampleSize;
    }

    // Open the audio file as an AudioInputStream and automatically
    // skip the first startOffset frames.
    public EqualizerInputStream getAudioStream() {
        File f = getFile();
        try {
            AudioInputStream s = AudioSystem.getAudioInputStream(f);
            EqualizerInputStream eq = new EqualizerInputStream(s, 31);
            AudioFormat format = eq.getFormat();
            IIRControls controls = eq.getControls();
            AudiobookRecorder.window.book.equaliser.apply(controls, format.getChannels());

            int frameSize = format.getFrameSize();

            eq.skip(frameSize * startOffset);
             
            return eq;
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
            EqualizerInputStream eq = new EqualizerInputStream(s, 31);

            AudioFormat format = eq.getFormat();

            IIRControls controls = eq.getControls();
            AudiobookRecorder.window.book.equaliser.apply(controls, format.getChannels());

            int frameSize = format.getFrameSize();

            updateCrossings();

            int pos = crossStartOffset * frameSize;

            SourceDataLine play = AudioSystem.getSourceDataLine(format, Options.getPlaybackMixer());
            play.open(format);
    
            play.start();

            byte[] buffer = new byte[1024];

            eq.skip(pos);
            
            while (pos < crossEndOffset * frameSize) {
                int nr = eq.read(buffer);
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
            updateCrossings();
            AudioInputStream s = AudioSystem.getAudioInputStream(f);
            EqualizerInputStream eq = new EqualizerInputStream(s, 31);


            AudioFormat format = eq.getFormat();
            IIRControls controls = eq.getControls();
            AudiobookRecorder.window.book.equaliser.apply(controls, format.getChannels());

            int frameSize = format.getFrameSize();
            int length = crossEndOffset - crossStartOffset;

            int bytesToRead = length * frameSize;
            byte[] data = new byte[bytesToRead];
            byte[] buf = new byte[65536];

            int pos = 0;

            eq.skip(crossStartOffset * frameSize);

            while (bytesToRead > 0) {
                int r = eq.read(buf, 0, bytesToRead > 65536 ? 65536 : bytesToRead);
                for (int i = 0; i < r; i++) {
                    data[pos++] = buf[i];
                    bytesToRead--;
                }
            }
                
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
        System.gc();
    }

    public boolean lockedInCache() {
        return id.equals("room-noise"); 
    }

    public int findNearestZeroCrossing(int pos, int range) {
        int[] data = getAudioData();
        if (data == null) return 0;
        if (data.length == 0) return 0;

        if (pos < 0) pos = 0;
        if (pos >= data.length) pos = data.length-1;

        int backwards = pos;
        int forwards = pos;

        int backwardsPrev = data[backwards];
        int forwardsPrev = data[forwards];

        while (backwards > 0 || forwards < data.length-2) {

            if (forwards < data.length-2) forwards++;
            if (backwards > 0) backwards--;

            if (backwardsPrev >= 0 && data[backwards] < 0) { // Found one!
                return backwards;
            }

            if (forwardsPrev < 0 && data[forwards] >= 0) {
                return forwards;
            }

            range--;
            if (range == 0) {
                return pos;
            }

            backwardsPrev = data[backwards];
            forwardsPrev = data[forwards];
        }
        return pos;
    }
}
