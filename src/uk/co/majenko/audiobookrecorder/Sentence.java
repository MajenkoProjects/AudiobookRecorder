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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class Sentence extends BookTreeNode implements Cacheable {

    String text;
    String id;
    String notes;
    int postGap;
    int startOffset = 0;
    int endOffset = 0;
    int crossStartOffset = -1;
    int crossEndOffset = -1;
    String postGapType = "none";

    int sampleSize = -1;
    double peak = -1;

    boolean locked;

    boolean recording;

    boolean inSample;
    boolean attention = false;
    boolean processed = false;

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

    double runtime = -1d;

    double[][] audioData = null;

    double[][] processedAudio = null;

    double[] fftProfile = null;

    RecordingThread recordingThread;

    boolean effectEthereal = false;

    public void setSampleSize(int s) {
        Debug.trace();
        sampleSize = s;
    }

    static class RecordingThread implements Runnable {

        boolean running = false;
        boolean recording = false;
        Sentence sent = null;

        File tempFile;
        File wavFile;

        AudioFormat format;

        public RecordingThread(File tf, File wf, AudioFormat af, Sentence s) {
            Debug.trace();
            tempFile = tf;
            wavFile = wf;
            format = af;
            sent = s;
        }

        public void run() {
            Debug.trace();
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

                sent.setSampleSize(len / format.getFrameSize());

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
            Debug.trace();
            return running;
        }

        public void stopRecording() {
            Debug.trace();
            recording = false;
        }
    }

    public Sentence() {
        super("");
        Debug.trace();
        id = UUID.randomUUID().toString();
        text = id;
        setUserObject(text);
        postGap = Options.getInteger("catenation.post-sentence");
    }

    public Sentence(String i, String t) {
        super("");
        Debug.trace();
        id = i;
        text = t;
        setUserObject(text);
        postGap = Options.getInteger("catenation.post-sentence");
    }

    public Sentence(Element root) {
        super("");
        Debug.trace();
        id = root.getAttribute("id");
        text = Book.getTextNode(root, "text");
        notes = Book.getTextNode(root, "notes");
        setUserObject(text);
        setPostGap(Utils.s2i(Book.getTextNode(root, "post-gap")));
        setStartOffset(Utils.s2i(Book.getTextNode(root, "start-offset")));
        setEndOffset(Utils.s2i(Book.getTextNode(root, "end-offset")));
        crossStartOffset = Utils.s2i(Book.getTextNode(root, "cross-start-offset", "-1"));
        crossEndOffset = Utils.s2i(Book.getTextNode(root, "end-offset", "-1"));
        setLocked(Utils.s2b(Book.getTextNode(root, "locked")));
        setAttentionFlag(Utils.s2b(Book.getTextNode(root, "attention")));
        gain = Utils.s2d(Book.getTextNode(root, "gain"));
        setEffectChain(Book.getTextNode(root, "effect"));
        setPostGapType(Book.getTextNode(root, "gaptype"));
        sampleSize = Utils.s2i(Book.getTextNode(root, "samples"));
        processed = Utils.s2b(Book.getTextNode(root, "processed"));
        runtime = Utils.s2d(Book.getTextNode(root, "time", "-1.000"));
        peak = Utils.s2d(Book.getTextNode(root, "peak", "-1.000"));

        if ((crossStartOffset == -1) || (crossEndOffset == -1)) {
            updateCrossings(true);
        }

        if (runtime <= 0.01d) getLength();
    }

    public boolean startRecording() {
        Debug.trace();
        if (AudiobookRecorder.window.microphone == null) {
            JOptionPane.showMessageDialog(AudiobookRecorder.window, "Microphone not started. Start the microphone first.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        CacheManager.removeFromCache(this);

        recordingThread = new RecordingThread(getTempFile(), getFile(), Options.getAudioFormat(), this);

        Thread rc = new Thread(recordingThread);
        rc.setDaemon(true);
        rc.start();

        return true;
    }

    public void stopRecording() {
        Debug.trace();
        recordingThread.stopRecording();
        while (recordingThread.isRunning()) {
            try {
                Thread.sleep(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        CacheManager.removeFromCache(this);

        if (!id.equals("room-noise")) {
            autoTrimSample(true);
            if (Options.getBoolean("process.sphinx")) {
                recognise();
            }
        }

    }

    public void autoTrimSample() {
        Debug.trace();
        autoTrimSample(false);
    }

    public void autoTrimSample(boolean useRaw) {
        Debug.trace();
        String tm = Options.get("audio.recording.trim");
        if (tm.equals("peak")) {
            autoTrimSamplePeak(useRaw);
        } else if (tm.equals("fft")) {
            autoTrimSampleFFT(useRaw);
        } else {
            startOffset = 0;
            crossStartOffset = 0;
            endOffset = sampleSize - 1;
            crossEndOffset = sampleSize - 1;
            processed = false;
//            peak = -1d;
        }
    }

    public static final int FFTBuckets = 1024;

    public void autoTrimSampleFFT() {
        Debug.trace();
        autoTrimSampleFFT(false);
    }
    
    public double bucketDifference(double[] a, double[] b) {
        double diff = 0d;
        int l = Math.min(a.length, b.length);
        for (int i = 0; i < l; i++) {
            if ((a[i] - b[i]) > diff) {
                diff = (a[i] - b[i]);
            }
        }
        return diff;
    }

    public void autoTrimSampleFFT(boolean useRaw) {
        Debug.trace();
        crossStartOffset = -1;
        crossEndOffset = -1;
        double[][] samples;
        if (useRaw) {
            samples = getRawAudioData();
        } else {
            samples = getProcessedAudioData();
        }
        if (samples == null) {
            return;
        }

        double[] roomNoiseProfile = AudiobookRecorder.window.getRoomNoiseSentence().getFFTProfile();

        int fftSize = Options.getInteger("audio.recording.trim.blocksize");

        int blocks = samples[LEFT].length / fftSize + 1;

        double[] intens = new double[blocks];
        int block = 0;

        for (int i = 0; i < samples[LEFT].length; i+= fftSize) {
            double[] real = new double[fftSize];
            double[] imag = new double[fftSize];

            for (int j = 0; j < fftSize; j++) {
                if (i + j < samples[LEFT].length) {
                    real[j] = (samples[LEFT][i+j] + samples[RIGHT][i+j]) / 2d;
                    imag[j] = 0;
                } else {
                    real[j] = 0;
                    imag[j] = 0;
                }
            }

            double[] buckets = FFT.fft(real, imag, true);


            intens[block] = bucketDifference(buckets, roomNoiseProfile);
            block++;
        }


        double limit = (double)(Options.getInteger("audio.recording.trim.fft"));

        // Find first block with > 1 intensity and subtract one.

        int start = 0;
        for (int i = 0; i < blocks; i++) {
            if (intens[i] >= (limit / 100.0)) break;
            start = i;
        }

        if (start >= blocks) {
            start = 0;
        }

        startOffset = start * fftSize;
        if (startOffset < 0) startOffset = 0;
        if (startOffset >= samples[LEFT].length) startOffset = samples[LEFT].length;

        int end = blocks - 1;
        // And last block with > 1 intensity and add one.
        for (int i = blocks-1; i >= 0; i--) {
            if (intens[i] >= (limit / 100)) break;
            end = i;
        }

        end++;

        if (end <= 0) {
            end = blocks - 1;
        }

        endOffset = (end+1) * fftSize;

        if (endOffset <= startOffset) endOffset = startOffset + fftSize;
        if (endOffset < 0) endOffset = 0;
        if (endOffset >= samples[LEFT].length) endOffset = samples[LEFT].length;
        updateCrossings(useRaw);
        intens = null;
        samples = null;
        processed = true;
        reloadTree();
    }

    public double[] getFFTProfile() {
        Debug.trace();
        if (fftProfile != null) return fftProfile;

        double[][] samples = getProcessedAudioData();
        if (samples == null) {
            return null;
        }

        int fftSize = Options.getInteger("audio.recording.trim.blocksize");

        fftProfile = new double[fftSize / 2];
        for (int j = 1; j < fftSize/2; j++) {
            fftProfile[j] = 0d;
        }

        for (int i = 0; i < samples[LEFT].length; i+= fftSize) {
            double[] real = new double[fftSize];
            double[] imag = new double[fftSize];

            for (int j = 0; j < fftSize; j++) {
                if (i + j < samples[LEFT].length) {
                    real[j] = (samples[LEFT][i+j] + samples[RIGHT][i+j]) / 2d;
                    imag[j] = 0;
                } else {
                    real[j] = 0;
                    imag[j] = 0;
                }
            }

            double[] buckets = FFT.fft(real, imag, true);

            for (int j = 1; j < fftSize/2; j++) {
                fftProfile[j] += Math.abs(buckets[j]);
            }
        }

        for (int j = 1; j < fftSize/2; j++) {
            fftProfile[j] /= (double)(fftSize / 2d);
        }
        return fftProfile;
    }


    public void autoTrimSamplePeak() {
        Debug.trace();
        autoTrimSamplePeak(false);
    }

    public void autoTrimSamplePeak(boolean useRaw) {
        Debug.trace();
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
        for (int i = 0; i < samples[LEFT].length; i++) {
            startOffset = i;
            if (Math.abs((samples[LEFT][i] + samples[RIGHT][i])/2d) > noiseFloor) {
                startOffset --;
                if (startOffset < 0) startOffset = 0;
                break;
            }
        }

        if (startOffset >= samples[LEFT].length-1) { // Failed! Silence?
            startOffset = 0;
        }

        int fftSize = Options.getInteger("audio.recording.trim.blocksize");
        startOffset -= fftSize;

        for (int i = samples[LEFT].length-1; i >= 0; i--) {
            endOffset = i;
            if (Math.abs((samples[LEFT][i] + samples[RIGHT][i])/2d) > noiseFloor) {
                endOffset ++;
                if (endOffset >= samples[LEFT].length-1) endOffset = samples[LEFT].length-1;
                break;
            }
        }

        endOffset += fftSize;

        if (endOffset <= startOffset) endOffset = startOffset + fftSize;
        if (endOffset <= 0) {
            endOffset = samples[LEFT].length-1;
        }

        if (startOffset < 0) startOffset = 0;
        if (endOffset >= samples[LEFT].length) endOffset = samples[LEFT].length-1;
        updateCrossings(useRaw);
        processed = true;
        reloadTree();
    }

    public String getId() {
        Debug.trace();
        return id;
    }

    public void setText(String t) {
        Debug.trace();
        overrideText = null;
        text = t;
        reloadTree();
    }

    public String getText() {
        Debug.trace();
        return text;
    }

    public File getFile() {
        Debug.trace();
        File b = new File(AudiobookRecorder.window.getBookFolder(), "files");
        if (!b.exists()) {
            b.mkdirs();
        }
        return new File(b, id + ".wav");
    }

    public File getTempFile() {
        Debug.trace();
        File b = new File(AudiobookRecorder.window.getBookFolder(), "files");
        if (!b.exists()) {
            b.mkdirs();
        }
        return new File(b, id + ".wax");
    }

    public void editText() {
        Debug.trace();
        String t = JOptionPane.showInputDialog(null, "Edit Text", text);

        if (t != null) {
            text = t;
        }

    }

    public String toString() {
        Debug.trace();
        return text;
/*
        if (effectChain == null) return text;
        if (effectChain.equals("none")) return text;
        Effect e = AudiobookRecorder.window.effects.get(effectChain);
        if (e == null) return text;
        return text + " (" + e.toString() + ")";
*/
    }

    public boolean isRecording() {
        Debug.trace();
        return recording;
    }

    public void setUserObject(Object o) {
        Debug.trace();
        if (o instanceof String) {
            String so = (String)o;
            text = so;
            reloadTree();
        }
    }

    public int getPostGap() {
        Debug.trace();
        return postGap;
    }

    public void setPostGap(int g) {
        Debug.trace();
        postGap = g;
    }

    public void deleteFiles() {
        Debug.trace();
        File audioFile = getFile();
        if (audioFile.exists()) {
            audioFile.delete();
        }
    }

    public int getStartCrossing() {
        Debug.trace();
        return crossStartOffset;
    }

    public int getStartOffset() {
        Debug.trace();
        return startOffset;
    }

    public void updateCrossings() {
        Debug.trace();
        updateCrossings(false);
    }

    public void updateCrossings(boolean useRaw) {
        Debug.trace();
        updateStartCrossing(useRaw);
        updateEndCrossing(useRaw);
        runtime = -1d;
        getLength();
    }

    public void updateStartCrossing() {
        Debug.trace();
        updateStartCrossing(false);
    }

    public void updateStartCrossing(boolean useRaw) {
        Debug.trace();
        if (crossStartOffset == -1) {
            crossStartOffset = findNearestZeroCrossing(useRaw, startOffset, 4096);
        }
    }

    public void updateEndCrossing() {
        Debug.trace();
        updateEndCrossing(false);
    }

    public void updateEndCrossing(boolean useRaw) {
        Debug.trace();
        if (crossEndOffset == -1) {
            crossEndOffset = findNearestZeroCrossing(useRaw, endOffset, 4096);
        }
    }

    public void setStartOffset(int o) {
        Debug.trace();
        if (startOffset != o) {
            startOffset = o;
            crossStartOffset = -1;
            reloadTree();
        }
    }

    public int getEndCrossing() {
        Debug.trace();
        return crossEndOffset;
    }

    public int getEndOffset() {
        Debug.trace();
        return endOffset;
    }

    public void setEndOffset(int o) {
        Debug.trace();
        if (endOffset != o) {
            endOffset = o;
            crossEndOffset = -1;
            reloadTree();
        }
    }

    public void setStartCrossing(int o) {
        Debug.trace();
        crossStartOffset = o;
    }

    public void setEndCrossing(int o) {
        Debug.trace();
        crossEndOffset = o;
    }

    public int getSampleSize() {
        Debug.trace();
        if (sampleSize == -1) {
            loadFile();
        }
        return sampleSize;
    }

    public AudioFormat getAudioFormat() {
        Debug.trace();
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
        Debug.trace();
        try {
            setText("[recognising...]");
            reloadTree();

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
            reloadTree();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void recognise() {
        Debug.trace();
        Thread t = new Thread(new Runnable() {
            public void run() {
                Debug.trace();
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
        Debug.trace();
        if (locked == l) return;
        locked = l;
        reloadTree();
    }

    public boolean isLocked() {
        Debug.trace();
        return locked;
    }

    public void setInSample(boolean s) {
        Debug.trace();
        inSample = s;
    }

    public boolean isInSample() { 
        Debug.trace();
        return inSample;
    }

    public void clearCache() {
        Debug.trace();
        audioData = null;
        processedAudio = null;
        storedFormat = null;
    }

    public boolean lockedInCache() {
        Debug.trace();
        return id.equals("room-noise"); 
    }

    public int findNearestZeroCrossing(int pos, int range) {
        Debug.trace();
        return findNearestZeroCrossing(false, pos, range);
    }

    public int findNearestZeroCrossing(boolean useRaw, int pos, int range) {
        Debug.trace();
        double[][] data = null;
        if (useRaw) {
            data = getRawAudioData();
        } else {
            data = getProcessedAudioData();
        }
        if (data == null) return 0;
        if (data[LEFT].length == 0) return 0;

        if (pos < 0) pos = 0;
        if (pos >= data[LEFT].length) pos = data[LEFT].length-1;

        int backwards = pos;
        int forwards = pos;

        double backwardsPrev = (data[LEFT][backwards] + data[RIGHT][backwards]) / 2d;
        double forwardsPrev = (data[LEFT][forwards] + data[RIGHT][forwards]) / 2d;

        while (backwards > 0 || forwards < data[LEFT].length-2) {

            if (forwards < data[LEFT].length-2) forwards++;
            if (backwards > 0) backwards--;

            if (backwardsPrev >= 0 && ((data[LEFT][backwards] + data[RIGHT][backwards]) / 2d) < 0) { // Found one!
                return backwards;
            }

            if (forwardsPrev < 0 && ((data[LEFT][forwards] + data[RIGHT][forwards]) / 2d) >= 0) {
                return forwards;
            }

            range--;
            if (range == 0) {
                return pos;
            }

            backwardsPrev = (data[LEFT][backwards] + data[RIGHT][backwards]) / 2d;
            forwardsPrev = (data[LEFT][forwards] + data[RIGHT][forwards]) / 2d;
        }
        return pos;
    }

    /* Get the length of the sample in seconds */
    public double getLength() {
        Debug.trace();
        if (runtime > 0.01d) return runtime;
        File f = getFile();
        if (!f.exists()) { // Not recorded yet!
            return 0;
        }
        AudioFormat format = getAudioFormat();
        float sampleFrequency = format.getFrameRate();
        int length = crossEndOffset - crossStartOffset;
        runtime = (double)length / (double)sampleFrequency;
        return runtime;
    }

    public Sentence cloneSentence() throws IOException {
        Debug.trace();
        Sentence sentence = new Sentence();
        sentence.setPostGap(getPostGap());
        if (!id.equals(text)) {
            sentence.setText(text);
        }
        sentence.setStartOffset(getStartOffset());
        sentence.setEndOffset(getEndOffset());
        sentence.setStartCrossing(getStartCrossing());
        sentence.setEndCrossing(getEndCrossing());

        File from = getFile();
        File to = sentence.getFile();
        Files.copy(from.toPath(), to.toPath());

//        sentence.updateCrossings();
        return sentence;
    }

    public void setAttentionFlag(boolean f) {
        Debug.trace();
        if (attention == f) return;
        attention = f;
        reloadTree();
    }

    public boolean getAttentionFlag() {
        Debug.trace();
        return attention;
    }

    public double getPeakValue() {
        Debug.trace();
        return getPeakValue(false, true);
    }

    public double getPeakValue(boolean useRaw) {
        Debug.trace();
        return getPeakValue(useRaw, true);
    }

    public double getPeakValue(boolean useRaw, boolean applyGain) {
        Debug.trace();
        double oldGain = gain;
        gain = 1.0d;
        double[][] samples = null;
        if (useRaw) {
            samples = getRawAudioData();
        } else {
            samples = getProcessedAudioData(true, applyGain);
        }
        gain = oldGain;
        if (samples == null) {
            return 0;
        }
        double ms = 0;
        for (int i = 0; i < samples[LEFT].length; i++) {
            if (Math.abs((samples[LEFT][i] + samples[RIGHT][i]) / 2d) > ms) {
                ms = Math.abs((samples[LEFT][i] + samples[RIGHT][i]) / 2d);
            }
        }
        return ms;
    }

    public int getHeadroom() {
        Debug.trace();
        double r = getPeakValue();
        if (r == 0) return 0;
        double l10 = Math.log10(r);
        double db = 20d * l10;

        return (int)db;
    }

    public void setGain(double g) {
        Debug.trace();
        if (g <= 0.0001d) g = 1.0d;
        if (g == gain) return;

        int gint = (int)(g * 100d);
        int gainint = (int)(gain * 100d);
        gain = g;
        if (gint != gainint) {
            CacheManager.removeFromCache(this);
            peak = -1;
            reloadTree();
        }
    }

    public double getGain() {
        Debug.trace();
        return gain;
    }

    public double normalize(double low, double high) {
        Debug.trace();
        if (locked) return gain;
        double max = getPeakValue(true, false);
        double d = 0.708 / max;
        if (d > 1d) d = 1d;
        if (d < low) d = low;
        if (d > high) d = high;
        setGain(d);
        peak = -1;
        getPeak();
        reloadTree();
        return d;
    }

    public double normalize() {
        Debug.trace();
        if (locked) return gain;
        double max = getPeakValue(true, false);
        double d = 0.708 / max;
        if (d > 1d) d = 1d;
        setGain(d);
        peak = -1;
        getPeak();
        reloadTree();
        return d;
    }

    class ExternalEditor implements Runnable {
        Sentence sentence;
        ExternalEditor(Sentence s) {
            sentence = s;
        }

        public void run() {
            Debug.trace();
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
            CacheManager.removeFromCache(Sentence.this);
            AudiobookRecorder.window.updateWaveform(true);
        }
    }

    public void openInExternalEditor() {
        Debug.trace();
        ExternalEditor ed = new ExternalEditor(this);
        Thread t = new Thread(ed);
        t.start();
    }

    public void backup() throws IOException {
        Debug.trace();
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
            debug("Out of backup space!");
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
            Debug.trace();
            sentence = s;
            number = num;
        }

        public void run() {
            Debug.trace();
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

            CacheManager.removeFromCache(Sentence.this);
            AudiobookRecorder.window.updateWaveform(true);
        }
    }

    public void runExternalProcessor(int num) {
        Debug.trace();
        if (isLocked()) return;
        ExternalProcessor ed = new ExternalProcessor(this, num);
        Thread t = new Thread(ed);
        t.start();
    }

    public void undo() {
        Debug.trace();
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
        
        CacheManager.removeFromCache(this);
        AudiobookRecorder.window.updateWaveform(true);
    }

    public double[][] getDoubleDataS16LE(AudioInputStream s, AudioFormat format) throws IOException {
        Debug.trace();
        long len = s.getFrameLength();
        int frameSize = format.getFrameSize();
        int chans = format.getChannels();

        byte[] frame = new byte[frameSize];
        double[][] samples = new double[2][(int)len];

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
                samples[LEFT][(int)fno] = (double)left / 32767d;
                samples[RIGHT][(int)fno] = (double)right / 32767d;
            } else {
                int l = frame[0] >= 0 ? frame[0] : 256 + frame[0];
                int h = frame[1] >= 0 ? frame[1] : 256 + frame[1];
                int mono = (h << 8) | l;
                if ((mono & 0x8000) == 0x8000) mono |= 0xFFFF0000;
                samples[LEFT][(int)fno] = (double)mono / 32767d;
                samples[RIGHT][(int)fno] = (double)mono / 32767d;
            }
        }

        return samples;
    }

    public void writeDoubleDataS16LE(double[][] samples, AudioFormat format) throws IOException {
        Debug.trace();
        int chans = format.getChannels();

        int frames = samples[LEFT].length;

        byte[] buffer;
        
        if (chans == 2) {
            int buflen = frames * 4;
            buffer = new byte[buflen];

            for (int i = 0; i < frames; i++) {
                double left = samples[LEFT][i];
                double right = samples[RIGHT][i];
                int off = i * 4;
                left *= 32767d;
                right *= 32767d;
                int li = (int)left;
                int ri = (int)right;

                if (li > 32767) li = 32767;
                if (li < -32767) li = -32767;
                if (ri > 32767) ri = 32767;
                if (ri < -32767) ri = -32767;

                buffer[off + 0] = (byte)(li & 0xFF);
                buffer[off + 1] = (byte)((li >> 8) & 0xFF);
                buffer[off + 2] = (byte)(ri & 0xFF);
                buffer[off + 3] = (byte)((ri >> 8) & 0xFF);
            }
        } else {
            int buflen = frames * 2;
            buffer = new byte[buflen];

            for (int i = 0; i < frames; i++) {
                double left = samples[LEFT][i];
                double right = samples[RIGHT][i];
                double mono = (left + right) / 2d;
                int off = i * 2;
                mono *= 32767d;
                int mi = (int)mono;
     
                if (mi > 32767) mi = 32767;
                if (mi < -32767) mi = -32767;

                buffer[off + 0] = (byte)(mi & 0xFF);
                buffer[off + 1] = (byte)((mi >> 8) & 0xFF);
            }
        }

        backup();
        ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
        AudioInputStream ais = new AudioInputStream(bis, format, frames);
        File wavFile = getFile();
        FileOutputStream fos = new FileOutputStream(wavFile);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fos);
        fos.close();
        ais.close();
    }

    public void writeDoubleDataS24LE(double[][] samples, AudioFormat format) throws IOException {
        Debug.trace();
        int chans = format.getChannels();

        int frames = samples[LEFT].length;

        byte[] buffer;

        if (chans == 2) {
            int buflen = frames * 6;
            buffer = new byte[buflen];

            for (int i = 0; i < frames; i++) {
                double left = samples[LEFT][i];
                double right = samples[RIGHT][i];
                int off = i * 6;
                left *= 8388607d;
                right *= 8388607d;
                int li = (int)left;
                int ri = (int)right;

                if (li > 8388607) li = 8388607;
                if (li < -8388607) li = -8388607;
                if (ri > 8388607) ri = 8388607;
                if (ri < -8388607) ri = -8388607;

                buffer[off + 0] = (byte)(li & 0xFF);
                buffer[off + 1] = (byte)((li >> 8) & 0xFF);
                buffer[off + 2] = (byte)((li >> 16) & 0xFF);
                buffer[off + 3] = (byte)(ri & 0xFF);
                buffer[off + 4] = (byte)((ri >> 8) & 0xFF);
                buffer[off + 5] = (byte)((ri >> 16) & 0xFF);
            }
        } else {
            int buflen = frames * 3;
            buffer = new byte[buflen];

            for (int i = 0; i < frames; i++) {
                double left = samples[LEFT][i];
                double right = samples[RIGHT][i];
                double mono = (left + right) / 2d;
                int off = i * 3;
                mono *= 8388607d;
                int mi = (int)mono;
    
                if (mi > 8388607) mi = 8388607;
                if (mi < -8388607) mi = -8388607;

                buffer[off + 0] = (byte)(mi & 0xFF);
                buffer[off + 1] = (byte)((mi >> 8) & 0xFF);
                buffer[off + 2] = (byte)((mi >> 16) & 0xFF);
            }
        }

        backup();
        ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
        AudioInputStream ais = new AudioInputStream(bis, format, frames);
        File wavFile = getFile();
        FileOutputStream fos = new FileOutputStream(wavFile);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fos);
        fos.close();
        ais.close();
    }

    public void writeAudioData(double[][] samples) throws IOException {
        Debug.trace();
        AudioFormat format = getAudioFormat();

        switch (format.getSampleSizeInBits()) {
            case 16:
                writeDoubleDataS16LE(samples, format);
                break;
            case 24:
                writeDoubleDataS24LE(samples, format);
                break;
        }

        CacheManager.removeFromCache(this);
    }

    public double[][] getDoubleDataS24LE(AudioInputStream s, AudioFormat format) throws IOException {
        Debug.trace();
        long len = s.getFrameLength();
        int frameSize = format.getFrameSize();
        int chans = format.getChannels();

        byte[] frame = new byte[frameSize];
        double[][] samples = new double[2][(int)len];

        int pos = 0;

        Debug.d("Starting processing");
        if (chans == 2) { // Stereo
            for (long fno = 0; fno < len; fno++) {
                s.read(frame);
                int sample = 0;
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
                samples[LEFT][(int)fno] = (double)left / 8388607d;
                samples[RIGHT][(int)fno] = (double)right / 8388607d;
            }
        } else {
            for (long fno = 0; fno < len; fno++) {
                s.read(frame);
                int l = frame[0] >= 0 ? frame[0] : 256 + frame[0];
                int m = frame[1] >= 0 ? frame[1] : 256 + frame[1];
                int h = frame[2] >= 0 ? frame[2] : 256 + frame[2];
                int mono = (h << 16) | (m << 8) | l;
                if ((mono & 0x800000) == 0x800000) mono |= 0xFF000000;
                samples[LEFT][(int)fno] = (double)mono / 8388607d;
                samples[RIGHT][(int)fno] = (double)mono / 8388607d;
            }
        }
        Debug.d("Finished processing");

        return samples;
    }

    public void loadFile() {
        Debug.trace();
        if (audioData != null) {
            return;
        }

        File f = getFile();
        try {
            if (!f.exists()) {
                debug("TODO: Race condition: wav file doesn't exist yet");
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
            sampleSize = samples[LEFT].length;
            audioData = samples;
            getPeak();
            CacheManager.addToCache(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized public double[][] getRawAudioData() {
        Debug.trace();
        loadFile();
        return audioData;
    }

    synchronized public double[][] getProcessedAudioData() {
        Debug.trace();
        return getProcessedAudioData(true, true);
    }

    synchronized public double[][] getProcessedAudioData(boolean effectsEnabled) {
        Debug.trace();
        return getProcessedAudioData(effectsEnabled, true);
    }

    synchronized public double[][] getProcessedAudioData(boolean effectsEnabled, boolean applyGain) {
        Debug.trace();
        loadFile();
        if (processedAudio != null) {
            return processedAudio;
        }

        if (audioData == null) return null;
        processedAudio = new double[2][audioData[LEFT].length];
        for (int i = 0; i < audioData[LEFT].length; i++) {
            processedAudio[LEFT][i] = audioData[LEFT][i];
            processedAudio[RIGHT][i] = audioData[RIGHT][i];
        }
        // Add processing in here.


        String def = AudiobookRecorder.window.getDefaultEffectsChain();
        Effect eff = AudiobookRecorder.window.effects.get(def);
    
        if (effectsEnabled) {
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
        }

        if (applyGain) {
            // Add final master gain stage
            for (int i = 0; i < processedAudio[LEFT].length; i++) {
                processedAudio[LEFT][i] *= gain;
                processedAudio[RIGHT][i] *= gain;
            }
        }

        return processedAudio;
    }

    public double[][] getDoubleAudioData() {
        Debug.trace();
        return getDoubleAudioData(true);
    }

    public double[][] getDoubleAudioData(boolean effectsEnabled) {
        Debug.trace();
        return getProcessedAudioData(effectsEnabled);
    }

    public double[][] getCroppedAudioData() {
        Debug.trace();
        return getCroppedAudioData(true);
    }

    public double[][] getCroppedAudioData(boolean effectsEnabled) {
        Debug.trace();
        double[][] inSamples = getDoubleAudioData(effectsEnabled);
        if (inSamples == null) return null;
        updateCrossings();

        int length = crossEndOffset - crossStartOffset;

        double[][] samples = new double[2][length];
        for (int i = 0; i < length; i++) {
            samples[LEFT][i] = inSamples[LEFT][crossStartOffset + i];
            samples[RIGHT][i] = inSamples[RIGHT][crossStartOffset + i];
        }
        return samples;
    }

    public byte[] getPCMData() {
        Debug.trace();
        return getPCMData(true);
    }

    public byte[] getPCMData(boolean effectsEnabled) {
        Debug.trace();
        double[][] croppedData = getCroppedAudioData(effectsEnabled);
        if (croppedData == null) return null;
        int length = croppedData[LEFT].length;
        byte[] pcmData = new byte[length * 4];
        for (int i = 0; i < length; i++) {
            double sd = croppedData[LEFT][i] * 32767d;
            int si = (int)sd;
            if (si > 32767) si = 32767;
            if (si < -32767) si = -32767;
            pcmData[i * 4] = (byte)(si & 0xFF);
            pcmData[(i * 4) + 1] = (byte)((si & 0xFF00) >> 8);
            sd = croppedData[RIGHT][i] * 32767d;
            si = (int)sd;
            if (si > 32767) si = 32767;
            if (si < -32767) si = -32767;
            pcmData[(i * 4) + 2] = (byte)(si & 0xFF);
            pcmData[(i * 4) + 3] = (byte)((si & 0xFF00) >> 8);
        }
        return pcmData;
    }

    public void setEffectChain(String key) {
        Debug.trace();
        if ((effectChain != null) && (effectChain.equals(key))) {
            return;
        }
        if ((effectChain != null) && (!effectChain.equals(key))) {
            CacheManager.removeFromCache(this);
        }
        effectChain = key;
        reloadTree();
    }

    public String getEffectChain() {
        Debug.trace();
        if (effectChain == null) return "none";
        return effectChain;
    }

    public String getPostGapType() {
        Debug.trace();
        return postGapType;
    }

    public void setPostGapType(String t) {
        Debug.trace();
        if (t == null || t.equals("none")) {
            if (getPostGap() == Options.getInteger("catenation.short-sentence")) {
                t = "continuation";
            } else if (getPostGap() == Options.getInteger("catenation.post-paragraph")) {
                t = "paragraph";
            } else if (getPostGap() == Options.getInteger("catenation.post-section")) {
                t = "section";
            } else if (getPostGap() == Options.getInteger("catenation.post-sentence")) {
                t = "sentence";
            } else {
                t = "sentence";
            }
        }
        postGapType = t;
        reloadTree();
    }

    public void resetPostGap() {
        Debug.trace();
        if (postGapType == null) {
            postGapType = "sentence";
        }
        if (postGapType.equals("continuation")) {
            setPostGap(Options.getInteger("catenation.short-sentence"));
        } else if (postGapType.equals("paragraph")) {
            setPostGap(Options.getInteger("catenation.post-paragraph"));
        } else if (postGapType.equals("section")) {
            setPostGap(Options.getInteger("catenation.post-section"));
        } else {
            setPostGap(Options.getInteger("catenation.post-sentence"));
        } 

    }

    public void debug(String txt) {
        Debug.trace();
        Debug.debug(String.format("%s: %s", id, txt));
    }

    public TreeMap<String, String> getSentenceData() {
        Debug.trace();

        TreeMap<String, String> out = new TreeMap<String, String>();

        out.put("id", getId());
        out.put("text", getText());
        out.put("post-gap", Integer.toString(getPostGap()));
        out.put("start-offset", Integer.toString(getStartOffset()));
        out.put("end-offset", Integer.toString(getEndOffset()));
        out.put("locked", isLocked() ? "true" : "false");
        out.put("attention", getAttentionFlag() ? "true" : "false");
        out.put("gain", String.format("%.8f", getGain()));
        out.put("effect", getEffectChain());
        out.put("gaptype", getPostGapType());
//        out.put("samples", Integer.toString(getSampleSize()));

        return out;
    }

    public void purgeBackups() {
        Debug.trace();
        File whereto = getFile().getParentFile();
        String name = getFile().getName();

        File[] files = whereto.listFiles();
        for (File f : files) {
            String fn = f.getName();
            if (fn.startsWith("backup-") && fn.endsWith("-" + name)) {
                f.delete();
            }
        }
    }

    public Element getSentenceXML(Document doc) {
        Debug.trace();
        Element sentenceNode = doc.createElement("sentence");
        sentenceNode.setAttribute("id", getId());
        sentenceNode.appendChild(Book.makeTextNode(doc, "text", getText()));
        sentenceNode.appendChild(Book.makeTextNode(doc, "post-gap", getPostGap()));
        sentenceNode.appendChild(Book.makeTextNode(doc, "start-offset", getStartOffset()));
        sentenceNode.appendChild(Book.makeTextNode(doc, "end-offset", getEndOffset()));
        sentenceNode.appendChild(Book.makeTextNode(doc, "cross-start-offset", crossStartOffset));
        sentenceNode.appendChild(Book.makeTextNode(doc, "cross-end-offset", crossEndOffset));
        sentenceNode.appendChild(Book.makeTextNode(doc, "locked", isLocked()));
        sentenceNode.appendChild(Book.makeTextNode(doc, "attention", getAttentionFlag()));
        sentenceNode.appendChild(Book.makeTextNode(doc, "gain", getGain()));
        sentenceNode.appendChild(Book.makeTextNode(doc, "effect", getEffectChain()));
        sentenceNode.appendChild(Book.makeTextNode(doc, "gaptype", getPostGapType()));
        sentenceNode.appendChild(Book.makeTextNode(doc, "samples", getSampleSize()));
        sentenceNode.appendChild(Book.makeTextNode(doc, "processed", isProcessed()));
        sentenceNode.appendChild(Book.makeTextNode(doc, "notes", getNotes()));
        sentenceNode.appendChild(Book.makeTextNode(doc, "time", getLength()));
        sentenceNode.appendChild(Book.makeTextNode(doc, "peak", getPeak()));
        return sentenceNode;
    }

    public boolean isProcessed() {
        Debug.trace();
        return processed;
    }

    public void setProcessed(boolean p) {
        Debug.trace();
        processed = p;
        reloadTree();
    }

    public void setNotes(String n) {
        Debug.trace();
        notes = n;
    }

    public String getNotes() {
        Debug.trace();
        return notes;
    }

    public void onSelect() {
        Debug.trace();
        AudiobookRecorder.window.setSentenceNotes(notes);
    }

    void reloadTree() {
        Debug.trace();
        if (id.equals("room-noise")) return;
        if (getParent() == null) return;
        AudiobookRecorder.window.bookTreeModel.reload(this);
    }

    public double getPeak() {
        Debug.trace();
        if (peak > -1) return peak;
        double[][] samples = getDoubleAudioData();
        if (samples == null) {
            peak = -1;
            return 0;
        }
        double ms = 0;
        for (int i = 0; i < samples[LEFT].length; i++) {
            double n = Math.abs((samples[LEFT][i] + samples[RIGHT][i]) / 2d);
            if (n > ms) {
                ms = n;
            }
        }

        ms *= 10d;
        ms /= 7d;
        peak = ms;
        return ms;
    }

    public int getPeakDB() {
        Debug.trace();
        double r = getPeak();
        if (r == 0) return 0;
        double l10 = Math.log10(r);
        double db = 20d * l10;

        return (int)db;
    }


}
