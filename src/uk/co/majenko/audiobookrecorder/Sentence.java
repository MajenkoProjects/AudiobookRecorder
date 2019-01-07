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
import davaguine.jeq.spi.EqualizerInputStream;
import davaguine.jeq.core.IIRControls;

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

    int eqProfile = 0;

    double gain = 1.0d;

    String havenJobId = "";

    // 0: Not processed
    // 1: Submitted
    // 2: Procesisng finished
    // 3: Processing failed
    int havenStatus = 0;

    String overrideText = null;

    public void setOverrideText(String s) { overrideText = s; }
    public String getOverrideText() { return overrideText; }

    TargetDataLine line;
    AudioInputStream inputStream;
    AudioFormat storedFormat = null;
    double storedLength = -1d;

    int[] storedAudioData = null;
    
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


        recordingThread = new RecordingThread(getTempFile(), getFile(), AudiobookRecorder.window.book.getAudioFormat());

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
        storedFormat = null;
        storedLength = -1;

        if (!id.equals("room-noise")) {
            String tm = Options.get("audio.recording.trim");
            if (tm.equals("peak")) {
                autoTrimSamplePeak();
            } else if (tm.equals("fft")) {
                autoTrimSampleFFT();
            }
            if (Options.getBoolean("process.haven.auto")) {
                recognise();
            }
        }

    }

    public static final int FFTBuckets = 1024;

    public double[][] getFFTProfile() {
        double[] real = new double[FFTBuckets];
        double[] imag = new double[FFTBuckets];

        int[] samples = getAudioData();
        int slices = (samples.length / FFTBuckets) + 1;

        double[][] out = new double[slices][];

        int slice = 0;

        for (int i = 0; i < samples.length; i += FFTBuckets) {
            for (int j = 0; j < FFTBuckets; j++) {
                if (i + j < samples.length) {
                    real[j] = samples[i+j] / 32768d;
                    imag[j] = 0;
                } else {
                    real[j] = 0;
                    imag[j] = 0;
                }
            }

            out[slice++] = FFT.fft(real, imag, true);
        }

        return out;
        
        
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

            for (int j = 2; j < 4096; j += 2) {
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

        end++;

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
        return getAudioDataS16LE(s, format, true);
    }

    public int[] getAudioDataS16LE(AudioInputStream s, AudioFormat format, boolean amplify) throws IOException {
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
            if (amplify) {
                double amped = (double)sample * gain;
                samples[(int)fno] = (int)amped;
            } else {
                samples[(int)fno] = sample;
            }
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
            AudioFormat format = getAudioFormat();

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

    public int[] getUnprocessedAudioData() {
        File f = getFile();
        try {
            AudioInputStream s = AudioSystem.getAudioInputStream(f);
            AudioFormat format = getAudioFormat();

            int[] samples = null;

            switch (format.getSampleSizeInBits()) {
                case 16:
                    samples = getAudioDataS16LE(s, format, false);
                    break;
            }

            s.close();
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
            AudioFormat format = getAudioFormat();
            IIRControls controls = eq.getControls();
            AudiobookRecorder.window.book.equaliser[eqProfile].apply(controls, format.getChannels());

            int frameSize = format.getFrameSize();

            eq.skip(frameSize * startOffset);
             
            return eq;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
    
    public byte[] getRawAudioData() {
        File f = getFile();
        try {
            updateCrossings();
            AudioInputStream s = AudioSystem.getAudioInputStream(f);
            EqualizerInputStream eq = new EqualizerInputStream(s, 31);


            AudioFormat format = getAudioFormat();
            IIRControls controls = eq.getControls();
            AudiobookRecorder.window.book.equaliser[eqProfile].apply(controls, format.getChannels());

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

            data = postProcessData(data);
                
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    byte[] adjustGain(byte[] data) {
        AudioFormat format = getAudioFormat();
        int frameSize = format.getFrameSize();
        int channels = format.getChannels();
        int bytesPerChannel = frameSize / channels;

        int frames = data.length / frameSize;

        int byteNo = 0;

        byte[] out = new byte[data.length];

        for (int i = 0; i < frames; i++) {
            if (channels == 1) {
                int l = data[i * frameSize] >= 0 ? data[i * frameSize] : 256 + data[i * frameSize];
                int h = data[(i * frameSize) + 1] >= 0 ? data[(i * frameSize) + 1] : 256 + data[(i * frameSize) + 1];

                int sample = (h << 8) | l;
                if ((sample & 0x8000) == 0x8000) sample |= 0xFFFF0000;

                double sampleDouble = (double)sample;
                sampleDouble *= gain;
                sample = (int)sampleDouble;

                if (sample > 32767) sample = 32767;
                if (sample < -32768) sample = -32768;
                out[i * frameSize] = (byte)(sample & 0xFF);
                out[(i * frameSize) + 1] = (byte)((sample & 0xFF00) >> 8);

            } else {
                return data;
            }
        }

        return out;
    }

    byte[] postProcessData(byte[] data) {
        data = adjustGain(data);

        if (effectEthereal) {
            data = processEtherealEffect(data);
        }
        return data;
    }

    byte[] processEtherealEffect(byte[] data) {
        AudioFormat format = getAudioFormat();
        int frameSize = format.getFrameSize();
        int channels = format.getChannels();
        int bytesPerChannel = frameSize / channels;

        int frames = data.length / frameSize;

        int byteNo = 0;

        double fpms = (double)format.getFrameRate() / 1000d;
        double doubleOffset = fpms * (double) AudiobookRecorder.window.book.getInteger("effects.ethereal.offset");
        int offset = (int)doubleOffset;
        double attenuation = 1d - ((double)AudiobookRecorder.window.book.getInteger("effects.ethereal.attenuation") / 100d);

        int copies = AudiobookRecorder.window.book.getInteger("effects.ethereal.iterations");

        byte[] out = new byte[data.length];

        for (int i = 0; i < frames; i++) {
            if (channels == 1) {
                int l = data[i * frameSize] >= 0 ? data[i * frameSize] : 256 + data[i * frameSize];
                int h = data[(i * frameSize) + 1] >= 0 ? data[(i * frameSize) + 1] : 256 + data[(i * frameSize) + 1];
                
                int sample = (h << 8) | l;
                if ((sample & 0x8000) == 0x8000) sample |= 0xFFFF0000;

                double sampleDouble = (double)sample;

                int used = 0;
                for (int j = 0; j < copies; j++) {
                    if (i + (j * offset) < frames) {
                        used++;
                        int lx = data[(i + (j * offset)) * frameSize] >= 0 ? data[(i + (j * offset)) * frameSize] : 256 + data[(i + (j * offset)) * frameSize];
                        int hx = data[((i + (j * offset)) * frameSize) + 1] >= 0 ? data[((i + (j * offset)) * frameSize) + 1] : 256 + data[((i + (j * offset)) * frameSize) + 1];
                        int futureSample = (hx << 8) | lx;
                        if ((futureSample & 0x8000) == 0x8000) futureSample |= 0xFFFF0000;
                        double futureDouble = (double)futureSample;
                        for (int k = 0; k < copies; k++) {
                            futureDouble *= attenuation;
                        }
                        sampleDouble  = mix(sampleDouble, futureDouble);
                    }
                }
                sample = (int)sampleDouble;
                if (sample > 32767) sample = 32767;
                if (sample < -32768) sample = -32768;
                out[i * frameSize] = (byte)(sample & 0xFF); 
                out[(i * frameSize) + 1] = (byte)((sample & 0xFF00) >> 8);

            } else {
                return data;
            }
        }

        return out;
    }

    public void recognise() {
        AudiobookRecorder.window.havenQueue.submit(Sentence.this);
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
//        System.gc();
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

    public String getHavenJobId() {
        return havenJobId;
    }

    public void setHavenJobId(String i) {
        havenJobId = i;
    }

    public int getHavenStatus() {
        return havenStatus;
    }

    public void setHavenStatus(int i) {
        havenStatus = i;
    }

    public boolean postHavenData() {
        String apiKey = Options.get("process.haven.apikey");
        if (apiKey == null || apiKey.equals("")) return false;

        CloseableHttpClient httpclient = HttpClients.createDefault();

        setOverrideText("[submitting...]");
        AudiobookRecorder.window.bookTreeModel.reload(this);

        try {
            HttpPost httppost = new HttpPost("https://api.havenondemand.com/1/api/async/recognizespeech/v2?apikey=" + apiKey);

            FileBody bin = new FileBody(getFile());
            StringBody language = new StringBody("en-GB");

            HttpEntity reqEntity = MultipartEntityBuilder.create()
                .addPart("language_model", language)
                .addPart("file", bin)
                .build();

            httppost.setEntity(reqEntity);

            CloseableHttpResponse response = httpclient.execute(httppost);
            try {
                if (response.getStatusLine().getStatusCode() != 200) {
                    System.err.println("Error posting data: " + response.getStatusLine().getStatusCode());
                    return false;
                }

                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    JSONObject obj = new JSONObject(EntityUtils.toString(resEntity));
                    havenJobId = obj.getString("jobID");
                    System.err.println("Submitted new Haven OnDemand job #" + havenJobId);
                    havenStatus = 1;
                }
                EntityUtils.consume(resEntity);
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }


    return true;
// eddec91c-6018-4dcd-bd8d-5e96b23e334c --form "language_model=en-US" --form "file=@3e67460c-f298-4e2c-a412-d375d489e1b3.wav" 
    }

    public void processPendingHaven() {
        if (havenStatus != 1) return;

        
        String apiKey = Options.get("process.haven.apikey");
        if (apiKey == null || apiKey.equals("")) return;

        CloseableHttpClient httpclient = HttpClients.createDefault();


        try {
            HttpPost httppost = new HttpPost("https://api.havenondemand.com/1/job/status/" + havenJobId + "?apikey=" + apiKey);

            HttpEntity reqEntity = MultipartEntityBuilder.create().build();
            httppost.setEntity(reqEntity);

            CloseableHttpResponse response = httpclient.execute(httppost);
            try {
                if (response.getStatusLine().getStatusCode() != 200) {
                    havenStatus = 3; 
                    return;
                }

                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    JSONObject obj = new JSONObject(EntityUtils.toString(resEntity));

                    System.err.println(havenJobId + ": " + obj.getString("status"));

                    if (obj.getString("status").equals("finished")) {
                        havenStatus = 2;
                        JSONArray textItems = obj.getJSONArray("actions").getJSONObject(0).getJSONObject("result").getJSONArray("items");

                        StringBuilder out = new StringBuilder();

                        for (int i = 0; i < textItems.length(); i++) {
                            out.append(textItems.getJSONObject(i).getString("text"));
                            out.append(" ");
                        }
                        String result = out.toString();
                        setText(result.trim());
                        AudiobookRecorder.window.bookTreeModel.reload(Sentence.this);
                        System.err.println(result);
                    } else if (obj.getString("status").equals("queued")) {
                        havenStatus = 1;
                        setOverrideText("[processing...]");
                        AudiobookRecorder.window.bookTreeModel.reload(Sentence.this);
                    } else {
                        text = id;
                        AudiobookRecorder.window.bookTreeModel.reload(Sentence.this);
                        havenStatus = 3;
                        return;
                    }
                }
                EntityUtils.consume(resEntity);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setEthereal(boolean e) {
        effectEthereal = e;
    }

    public boolean getEthereal() {
        return effectEthereal;
    }

    public double mix(double a, double b) {
        double z;
        double fa, fb, fz;
        fa = a + 32768d;
        fb = b + 32768d;

        if (fa < 32768d && fb < 32768d) {
            fz = (fa * fb) / 32768d;
        } else {
            fz = (2d * (fa + fb)) - ((fa * fb) / 32768d) - 65536d;
        }

        z = fz - 32768d;
        return z;
    }

   public int getPeakValue() {
        int[] samples = getUnprocessedAudioData();
        if (samples == null) {
            return 0;
        }
        int ms = 0;
        for (int i = 0; i < samples.length; i++) {
            if (Math.abs(samples[i]) > ms) {
                ms = Math.abs(samples[i]);
            }
        }
        return ms;
    }

    public int getHeadroom() {
        int nf = getPeakValue();
        if (nf == 0) return 0;
        double r = nf / 32767d;
        double l10 = Math.log10(r);
        double db = 20d * l10;

        return (int)db;
    }

    public void setGain(double g) {
        if (g <= 0.0001d) g = 1.0d;
        if (g == gain) return;
        gain = g;
        clearCache();
    }

    public double getGain() {
        return gain;
    }

    public void normalize() {
        if (locked) return;
        int max = getPeakValue();
        double d = 23192d / max;
        if (d > 1.1d) d = 1.1d;
        setGain(d);
    }

    public int getEQProfile() {
        return eqProfile;
    }

    public void setEQProfile(int e) {
        eqProfile = e;
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

}
