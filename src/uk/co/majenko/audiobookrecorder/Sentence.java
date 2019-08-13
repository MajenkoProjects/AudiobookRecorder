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

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import edu.cmu.sphinx.api.*;
import edu.cmu.sphinx.decoder.adaptation.*;
import edu.cmu.sphinx.result.*;

import org.json.*;

import java.util.Timer;


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
    boolean attention = false;

    String effectChain = null;

    double gain = 1.0d;

    String overrideText = null;

    public void setOverrideText(String s) { overrideText = s; }
    public String getOverrideText() { return overrideText; }

    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    TargetDataLine line;
    AudioInputStream inputStream;
    AudioFormat storedFormat = null;
    double storedLength = -1d;

    double[][] audioData = null;

    double[][] processedAudio = null;

    RecordingThread recordingThread;

    boolean effectEthereal = false;

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
                byte[] buf = new byte[1024]; //AudiobookRecorder.window.microphone.getBufferSize()];
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

        CacheManager.removeFromCache(this);

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
                e.printStackTrace();
            }
        }

        CacheManager.removeFromCache(this);

        audioData = null;
        processedAudio = null;

        if (!id.equals("room-noise")) {
            String tm = Options.get("audio.recording.trim");
            if (tm.equals("peak")) {
                autoTrimSamplePeak(true);
            } else if (tm.equals("fft")) {
                autoTrimSampleFFT(true);
            }
            if (Options.getBoolean("process.sphinx")) {
                recognise();
            }
        }

    }

    public static final int FFTBuckets = 1024;

    public void autoTrimSampleFFT() {
        autoTrimSampleFFT(false);
    }

    public void autoTrimSampleFFT(boolean useRaw) {
        crossStartOffset = -1;
        crossEndOffset = -1;
        double[][] samples;
        if (useRaw) {
            samples = getRawAudioData();
        } else {
            samples = getProcessedAudioData();
        }
        if (samples == null) {
            System.err.println("Error: loading data failed!");
            return;
        }


        int blocks = samples.length / 4096 + 1;

        int[] intens = new int[blocks];
        int block = 0;

        for (int i = 0; i < samples.length; i+= 4096) {
            double[] real = new double[4096];
            double[] imag = new double[4096];

            for (int j = 0; j < 4096; j++) {
                if (i + j < samples.length) {
                    real[j] = (samples[i+j][LEFT] + samples[i+j][RIGHT]) / 2d;
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

            for (int j = 2; j < 4096; j += 2) {
                double d = Math.abs(av - buckets[j]);
                if (d > 0.05) {
                    intens[block]++;
                }
            }
            block++;
        }


        int limit = Options.getInteger("audio.recording.trim.fft");

        // Find first block with > 1 intensity and subtract one.

        int start = 0;
        for (int i = 0; i < blocks; i++) {
            if (intens[i] > limit) break;
            start = i;
        }

        if (start >= blocks) {
            start = 0;
        }

        startOffset = start * 4096;
        if (startOffset < 0) startOffset = 0;
        if (startOffset >= samples.length) startOffset = samples.length;

        int end = blocks - 1;
        // And last block with > 1 intensity and add one.
        for (int i = blocks-1; i >= 0; i--) {
            if (intens[i] > limit) break;
            end = i;
        }

        end++;

        if (end <= 0) {
            end = blocks - 1;
        }

        endOffset = end * 4096;

        if (endOffset <= startOffset) endOffset = startOffset + 4096;
        if (endOffset < 0) endOffset = 0;
        if (endOffset >= samples.length) endOffset = samples.length;
        updateCrossings(useRaw);
        intens = null;
        samples = null;
        System.gc();

    }

    public void autoTrimSamplePeak() {
        autoTrimSamplePeak(false);
    }

    public void autoTrimSamplePeak(boolean useRaw) {
        crossStartOffset = -1;
        crossEndOffset = -1;
        double[][] samples;
        if (useRaw) {
            samples = getRawAudioData();
        } else {
            samples = getProcessedAudioData();
        }
        if (samples == null) return;
        double noiseFloor = AudiobookRecorder.window.getNoiseFloor();
        noiseFloor *= 1.1;

        // Find start
        for (int i = 0; i < samples.length; i++) {
            startOffset = i;
            if (Math.abs((samples[i][LEFT] + samples[i][RIGHT])/2d) > noiseFloor) {
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
            if (Math.abs((samples[i][LEFT] + samples[i][RIGHT])/2d) > noiseFloor) {
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
        updateCrossings(useRaw);
    }

    public String getId() {
        return id;
    }

    public void setText(String t) {
        overrideText = null;
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
        if (effectChain == null) return text;
        if (effectChain.equals("none")) return text;
        Effect e = AudiobookRecorder.window.effects.get(effectChain);
        if (e == null) return text;
        return text + " (" + e.toString() + ")";
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

    public int getStartCrossing() {
        return crossStartOffset;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public void updateCrossings() {
        updateCrossings(false);
    }

    public void updateCrossings(boolean useRaw) {
        updateStartCrossing(useRaw);
        updateEndCrossing(useRaw);
    }

    public void updateStartCrossing() {
        updateStartCrossing(false);
    }

    public void updateStartCrossing(boolean useRaw) {
        if (crossStartOffset == -1) {
            crossStartOffset = findNearestZeroCrossing(useRaw, startOffset, 4096);
        }
    }

    public void updateEndCrossing() {
        updateEndCrossing(false);
    }

    public void updateEndCrossing(boolean useRaw) {
        if (crossEndOffset == -1) {
            crossEndOffset = findNearestZeroCrossing(useRaw, endOffset, 4096);
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
            loadFile();
        }
        return sampleSize;
    }

    public AudioFormat getAudioFormat() {
        if (storedFormat != null) return storedFormat; 

        File f = getFile();
        try {
            AudioInputStream s = AudioSystem.getAudioInputStream(f);
            storedFormat = s.getFormat();
            s.close();
            return storedFormat;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void doRecognition(StreamSpeechRecognizer recognizer) {
        try {
            setText("[recognising...]");
            AudiobookRecorder.window.bookTreeModel.reload(this);

            byte[] inData = getPCMData();

            ByteArrayInputStream bas = new ByteArrayInputStream(inData);
            recognizer.startRecognition(bas);
            SpeechResult result;
            String res = "";
            while ((result = recognizer.getResult()) != null) {
                res += result.getHypothesis();
                res += " ";
            }
            recognizer.stopRecognition();

            setText(res);
            AudiobookRecorder.window.bookTreeModel.reload(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void recognise() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Configuration sphinxConfig = new Configuration();

                    sphinxConfig.setAcousticModelPath(AudiobookRecorder.SPHINX_MODEL);
                    sphinxConfig.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
                    sphinxConfig.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

                    AudioInputStream s = AudioSystem.getAudioInputStream(getFile());
                    AudioFormat format = getAudioFormat();

                    sphinxConfig.setSampleRate((int)(format.getSampleRate()));

                    StreamSpeechRecognizer recognizer;

                    recognizer = new StreamSpeechRecognizer(sphinxConfig);

                    doRecognition(recognizer);
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
        audioData = null;
        processedAudio = null;
        storedFormat = null;
        storedLength = -1;
    }

    public boolean lockedInCache() {
        return id.equals("room-noise"); 
    }

    public int findNearestZeroCrossing(int pos, int range) {
        return findNearestZeroCrossing(false, pos, range);
    }

    public int findNearestZeroCrossing(boolean useRaw, int pos, int range) {
        double[][] data = null;
        if (useRaw) {
            data = getRawAudioData();
        } else {
            data = getProcessedAudioData();
        }
        if (data == null) return 0;
        if (data.length == 0) return 0;

        if (pos < 0) pos = 0;
        if (pos >= data.length) pos = data.length-1;

        int backwards = pos;
        int forwards = pos;

        double backwardsPrev = (data[backwards][LEFT] + data[backwards][RIGHT]) / 2d;
        double forwardsPrev = (data[forwards][LEFT] + data[forwards][RIGHT]) / 2d;

        while (backwards > 0 || forwards < data.length-2) {

            if (forwards < data.length-2) forwards++;
            if (backwards > 0) backwards--;

            if (backwardsPrev >= 0 && ((data[backwards][LEFT] + data[backwards][RIGHT]) / 2d) < 0) { // Found one!
                return backwards;
            }

            if (forwardsPrev < 0 && ((data[forwards][LEFT] + data[forwards][RIGHT]) / 2d) >= 0) {
                return forwards;
            }

            range--;
            if (range == 0) {
                return pos;
            }

            backwardsPrev = (data[backwards][LEFT] + data[backwards][RIGHT]) / 2d;
            forwardsPrev = (data[forwards][LEFT] + data[forwards][RIGHT]) / 2d;
        }
        return pos;
    }

    /* Get the length of the sample in seconds */
    public double getLength() {
        if (storedLength > -1d) return storedLength;
        AudioFormat format = getAudioFormat();
        float sampleFrequency = format.getFrameRate();
        int length = crossEndOffset - crossStartOffset;
        double time = (double)length / (double)sampleFrequency;
        storedLength = time;
        return time;
    }

    public Sentence cloneSentence() throws IOException {
        Sentence sentence = new Sentence();
        sentence.setPostGap(getPostGap());
        if (!id.equals(text)) {
            sentence.setText(text);
        }
        sentence.setStartOffset(getStartOffset());
        sentence.setEndOffset(getEndOffset());

        File from = getFile();
        File to = sentence.getFile();
        Files.copy(from.toPath(), to.toPath());

        sentence.updateCrossings();
        return sentence;
    }

    public void setAttentionFlag(boolean f) {
        attention = f;
    }

    public boolean getAttentionFlag() {
        return attention;
    }

    public double getPeakValue() {
        return getPeakValue(false);
    }

    public double getPeakValue(boolean useRaw) {
        double oldGain = gain;
        gain = 1.0d;
        double[][] samples = null;
        if (useRaw) {
            samples = getRawAudioData();
        } else {
            samples = getProcessedAudioData();
        }
        gain = oldGain;
        if (samples == null) {
            return 0;
        }
        double ms = 0;
        for (int i = 0; i < samples.length; i++) {
            if (Math.abs((samples[i][LEFT] + samples[i][RIGHT]) / 2d) > ms) {
                ms = Math.abs((samples[i][LEFT] + samples[i][RIGHT]) / 2d);
            }
        }
        return ms;
    }

    public int getHeadroom() {
        double r = getPeakValue();
        if (r == 0) return 0;
        double l10 = Math.log10(r);
        double db = 20d * l10;

        return (int)db;
    }

    public void setGain(double g) {
        if (g <= 0.0001d) g = 1.0d;
        if (g == gain) return;

        if (gain != g) {
            clearCache();
        }
        gain = g;
    }

    public double getGain() {
        return gain;
    }

    public void normalize() {
        if (locked) return;
        double max = getPeakValue();
        double d = 0.708 / max;
        if (d > 1d) d = 1d;
        setGain(d);
    }

    class ExternalEditor implements Runnable {
        Sentence sentence;
        ExternalEditor(Sentence s) {
            sentence = s;
        }

        public void run() {
            String command = Options.get("editor.external");
            if (command == null) return;
            if (command.equals("")) return;

            String[] parts = command.split("::");

            ArrayList<String> args = new ArrayList<String>();

            for (String part : parts) {
                if (part.equals("%f")) {
                    args.add(getFile().getAbsolutePath());
                } else {
                    args.add(part);
                }
            }

            try {
                ProcessBuilder process = new ProcessBuilder(args);
                Process proc = process.start();
                proc.waitFor();
            } catch (Exception e) { 
            }
            clearCache();
            AudiobookRecorder.window.updateWaveform();
        }
    }

    public void openInExternalEditor() {
        ExternalEditor ed = new ExternalEditor(this);
        Thread t = new Thread(ed);
        t.start();
    }

    public void backup() throws IOException {
        File whereto = getFile().getParentFile();
        String name = getFile().getName();

        int backupNumber = -1;
        for (int i = 1; i < 999999; i++) {

            String fn = String.format("backup-%08d-%s", i, name);
            File testFile = new File(whereto, fn);
            if (!testFile.exists()) {
                backupNumber = i;
                break;
            }
        }
        if (backupNumber == -1) {
            System.err.println("Out of backup space!");
            return;
        }

        String fn = String.format("backup-%08d-%s", backupNumber, getFile().getName());
        File bak = new File(getFile().getParentFile(), fn);
        Files.copy(getFile().toPath(), bak.toPath());
    }

    class ExternalProcessor implements Runnable {
        Sentence sentence;
        int number;
        
        public ExternalProcessor(Sentence s, int num) {
            sentence = s;
            number = num;
        }

        public void run() {
            String command = Options.get("editor.processor." + number + ".command");
            if (command == null) return;
            if (command.equals("")) return;

            String[] parts = command.split("::");
        
            ArrayList<String> args = new ArrayList<String>();

            try {
                backup();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            File out = new File(getFile().getParentFile(), "proc-" + getFile().getName());

            for (String part : parts) {
                if (part.equals("%f")) {
                    args.add(getFile().getAbsolutePath());
                } else if (part.equals("%o")) {
                    args.add(out.getAbsolutePath());
                } else {
                    args.add(part);
                }
            }

            try {
                ProcessBuilder process = new ProcessBuilder(args);
                Process proc = process.start();
                proc.waitFor();
            } catch (Exception e) {
            }

            if (out.exists()) {
                try {
                    File in = getFile();
                    in.delete();
                    Files.copy(out.toPath(), in.toPath());
                    out.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            clearCache();
            AudiobookRecorder.window.updateWaveform();
        }
    }

    public void runExternalProcessor(int num) {
        if (isLocked()) return;
        ExternalProcessor ed = new ExternalProcessor(this, num);
        Thread t = new Thread(ed);
        t.start();
    }

    public void undo() {
        File whereto = getFile().getParentFile();
        String name = getFile().getName();

        int backupNumber = -1;
        for (int i = 1; i < 999999; i++) {
            String fn = String.format("backup-%08d-%s", i, name);
            File testFile = new File(whereto, fn);
            if (testFile.exists()) {
                backupNumber = i;
            } else {
                break;
            }
        }

        if (backupNumber == -1) {
            return;
        }

        String fn = String.format("backup-%08d-%s", backupNumber, getFile().getName());
        File bak = new File(getFile().getParentFile(), fn);

        try {
            File in = getFile();
            in.delete();
            Files.copy(bak.toPath(), in.toPath());
            bak.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        clearCache();
        AudiobookRecorder.window.updateWaveform();
    }

    public double[][] getDoubleDataS16LE(AudioInputStream s, AudioFormat format) throws IOException {
        long len = s.getFrameLength();
        int frameSize = format.getFrameSize();
        int chans = format.getChannels();
        int bytes = frameSize / chans;

        byte[] frame = new byte[frameSize];
        double[][] samples = new double[(int)len][2];

        for (long fno = 0; fno < len; fno++) {

            s.read(frame);
            if (chans == 2) { // Stereo
                int ll = frame[0] >= 0 ? frame[0] : 256 + frame[0];
                int lh = frame[1] >= 0 ? frame[1] : 256 + frame[1];
                int rl = frame[2] >= 0 ? frame[2] : 256 + frame[2];
                int rh = frame[3] >= 0 ? frame[3] : 256 + frame[3];
                int left = (lh << 8) | ll;
                int right = (rh << 8) | rl;
                if ((left & 0x8000) == 0x8000) left |= 0xFFFF0000;
                if ((right & 0x8000) == 0x8000) right |= 0xFFFF0000;
                samples[(int)fno][LEFT] = (double)left / 32768d;
                samples[(int)fno][RIGHT] = (double)right / 32768d;
            } else {
                int l = frame[0] >= 0 ? frame[0] : 256 + frame[0];
                int h = frame[1] >= 0 ? frame[1] : 256 + frame[1];
                int mono = (h << 8) | l;
                if ((mono & 0x8000) == 0x8000) mono |= 0xFFFF0000;
                samples[(int)fno][LEFT] = (double)mono / 32768d;
                samples[(int)fno][RIGHT] = (double)mono / 32768d;
            }
        }

        return samples;
    }

    public double[][] getDoubleDataS24LE(AudioInputStream s, AudioFormat format) throws IOException {
        long len = s.getFrameLength();
        int frameSize = format.getFrameSize();
        int chans = format.getChannels();
        int bytes = frameSize / chans;

        byte[] frame = new byte[frameSize];
        double[][] samples = new double[(int)len][2];

        for (long fno = 0; fno < len; fno++) {

            s.read(frame);
            int sample = 0;
            if (chans == 2) { // Stereo
                int ll = frame[0] >= 0 ? frame[0] : 256 + frame[0];
                int lm = frame[1] >= 0 ? frame[1] : 256 + frame[1];
                int lh = frame[2] >= 0 ? frame[2] : 256 + frame[2];
                int rl = frame[3] >= 0 ? frame[3] : 256 + frame[3];
                int rm = frame[4] >= 0 ? frame[4] : 256 + frame[4];
                int rh = frame[5] >= 0 ? frame[5] : 256 + frame[5];
                int left = (lh << 16) | (lm << 8) | ll;
                int right = (rh << 16) | (rm << 8) | rl;
                if ((left & 0x800000) == 0x800000) left |= 0xFF000000;
                if ((right & 0x800000) == 0x800000) right |= 0xFF000000;
                samples[(int)fno][LEFT] = (double)left / 8388608d;
                samples[(int)fno][RIGHT] = (double)right / 8388608d;
            } else {
                int l = frame[0] >= 0 ? frame[0] : 256 + frame[0];
                int m = frame[1] >= 0 ? frame[1] : 256 + frame[1];
                int h = frame[2] >= 0 ? frame[2] : 256 + frame[2];
                int mono = (h << 16) | (m << 8) | l;
                if ((mono & 0x800000) == 0x800000) mono |= 0xFF000000;
                samples[(int)fno][LEFT] = (double)mono / 8388608d;
                samples[(int)fno][RIGHT] = (double)mono / 8388608d;
            }
        }

        return samples;
    }

    public void loadFile() {
        if (audioData != null) {
            return;
        }

        File f = getFile();
        try {
            if (!f.exists()) {
                System.err.println("TODO: Race condition: wav file doesn't exist yet");
                return;
            }
            AudioInputStream s = AudioSystem.getAudioInputStream(f);
            AudioFormat format = getAudioFormat();

            double[][] samples = null;

            switch (format.getSampleSizeInBits()) {
                case 16:
                    samples = getDoubleDataS16LE(s, format);
                    break;
                case 24:
                    samples = getDoubleDataS24LE(s, format);
                    break;
            }

            s.close();
            sampleSize = samples.length;
            audioData = samples;
            CacheManager.addToCache(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized public double[][] getRawAudioData() {
        loadFile();
        return audioData;
    }

    synchronized public double[][] getProcessedAudioData() {
        loadFile();
        if (processedAudio != null) return processedAudio;

        if (audioData == null) return null;
        processedAudio = new double[audioData.length][2];
        for (int i = 0; i < audioData.length; i++) {
            processedAudio[i][LEFT] = audioData[i][LEFT];
            processedAudio[i][RIGHT] = audioData[i][RIGHT];
        }
        // Add processing in here.


        String def = AudiobookRecorder.window.getDefaultEffectsChain();
        Effect eff = AudiobookRecorder.window.effects.get(def);
    
        if (eff != null) {
            eff.init(getAudioFormat().getFrameRate());
            eff.process(processedAudio);
        }

        if (effectChain != null) {
            // Don't double up the default chain
            if (!effectChain.equals(def)) {
                eff = AudiobookRecorder.window.effects.get(effectChain);
                if (eff != null) {
                    eff.init(getAudioFormat().getFrameRate());
                    eff.process(processedAudio);
                }
            }
        }
        
        // Add final master gain stage
        for (int i = 0; i < processedAudio.length; i++) {
            processedAudio[i][LEFT] *= gain;
            processedAudio[i][RIGHT] *= gain;
        }

        return processedAudio;
    }

    public double[][] getDoubleAudioData() {
        return getProcessedAudioData();
    }

    public double[][] getCroppedAudioData() {
        double[][] inSamples = getDoubleAudioData();
        if (inSamples == null) return null;
        updateCrossings();

        int length = crossEndOffset - crossStartOffset;

        double[][] samples = new double[length][2];
        for (int i = 0; i < length; i++) {
            samples[i][LEFT] = inSamples[crossStartOffset + i][LEFT];
            samples[i][RIGHT] = inSamples[crossStartOffset + i][RIGHT];
        }
        return samples;
    }

    public byte[] getPCMData() {
        double[][] croppedData = getCroppedAudioData();
        if (croppedData == null) return null;
        int length = croppedData.length;
        byte[] pcmData = new byte[length * 4];
        for (int i = 0; i < length; i++) {
            double sd = croppedData[i][LEFT] * 32768d;
            int si = (int)sd;
            if (si > 32767) si = 32767;
            if (si < -32768) si = -32768;
            pcmData[i * 4] = (byte)(si & 0xFF);
            pcmData[(i * 4) + 1] = (byte)((si & 0xFF00) >> 8);
            sd = croppedData[i][RIGHT] * 32768d;
            si = (int)sd;
            if (si > 32767) si = 32767;
            if (si < -32768) si = -32768;
            pcmData[(i * 4) + 2] = (byte)(si & 0xFF);
            pcmData[(i * 4) + 3] = (byte)((si & 0xFF00) >> 8);
        }
        return pcmData;
    }

    public void setEffectChain(String key) {
        if ((effectChain != null) && (!effectChain.equals(key))) {
            clearCache();
        }
        effectChain = key;
    }

    public String getEffectChain() {
        if (effectChain == null) return "none";
        return effectChain;
    }
}
