package uk.co.majenko.audiobookrecorder;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.prefs.*;
import java.io.*;

public class Options extends JDialog {

    JTabbedPane tabs;

    GridBagConstraints constraint;

    JComboBox<KVPair> mixerList;
    JComboBox<KVPair> playbackList;
    JComboBox<KVPair> channelList;
    JComboBox<KVPair> rateList;
    JTextField storageFolder;
    JSpinner preChapterGap;
    JSpinner postChapterGap;
    JSpinner postSentenceGap;
    JSpinner postParagraphGap;
    JTextField ffmpegLocation;
    JComboBox<KVPair> bitRate;
    JComboBox<KVPair> exportRate;
    JCheckBox enableParsing;
    JSpinner cacheSize;

    JSliderOb[] sliders;
    JTextFieldOb[] eqvals;


    static HashMap<String, String> defaultPrefs;
    static Preferences prefs = null;

    static class JSliderOb extends JSlider {
        Object object;

        public JSliderOb(int a, int b, int c) {
            super(a, b, c);
        }

        public void setObject(Object o) {
            object = o;
        }

        public Object getObject() {
            return object;
        }
    }

    public class JTextFieldOb extends JTextField {
        Object object;

        public JTextFieldOb(String s) {
            super(s);
        }

        public void setObject(Object o) {
            object = o;
        }

        public Object getObject() {
            return object;
        }
    }
        

    static class KVPair implements Comparable {
        public String key;
        public String value;

        public KVPair(String k, String v) {
            key = k;
            value = v;
        }

        public String toString() {
            return value;
        }

        public int compareTo(Object o) {
            if (o instanceof KVPair) {
                KVPair ko = (KVPair)o;
                return key.compareTo(ko.key);
            }
            return 0;
        }
    }

    class JButtonObject extends JButton {
        Object object;
        public JButtonObject(String s, Object o) {
            super(s);
            object = o;
        }

        public Object getObject() {
            return object;
        }
    }

    JComboBox<KVPair> addDropdown(JPanel panel, String label, KVPair[] options, String def) {
        JLabel l = new JLabel(label);
        constraint.gridx = 0;
        constraint.gridwidth = 1;
        constraint.gridheight = 1;
        constraint.anchor = GridBagConstraints.LINE_START;
        panel.add(l, constraint);

        JComboBox<KVPair> o = new JComboBox<KVPair>(options);
        constraint.gridx = 1;
        panel.add(o, constraint);

        for (KVPair p : options) {
            if (p.key.equals(def)) {
                o.setSelectedItem(p);
            }
        }

        constraint.gridy++;

        return o;
    }

    JTextField addFilePath(JPanel panel, String label, String path, boolean dironly) {
        JLabel l = new JLabel(label);
        constraint.gridx = 0;
        constraint.gridwidth = 1;
        constraint.gridheight = 1;
        constraint.anchor = GridBagConstraints.LINE_START;
        panel.add(l, constraint);

        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        JTextField a = new JTextField(path);
        p.add(a, BorderLayout.CENTER);
        JButtonObject b = new JButtonObject("...", a);

        if (dironly) {
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JButtonObject o = (JButtonObject)e.getSource();
                    JTextField f = (JTextField)o.getObject();
                    JFileChooser fc = new JFileChooser();
                    File d = new File(f.getText());
                    if (d.exists() && d.isDirectory()) {
                        fc.setCurrentDirectory(d);
                    }
                    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int r = fc.showOpenDialog(Options.this);

                    if (r == JFileChooser.APPROVE_OPTION) {
                        f.setText(fc.getSelectedFile().getAbsolutePath());
                    }
                }
            });
        } else {
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JButtonObject o = (JButtonObject)e.getSource();
                    JTextField f = (JTextField)o.getObject();
                    JFileChooser fc = new JFileChooser();
                    File d = new File(f.getText());
                    if (d.exists() && d.isDirectory()) {
                        fc.setCurrentDirectory(d);
                    } else if (d.exists()) {
                        d = d.getParentFile();
                        if (d.exists() && d.isDirectory()) {
                            fc.setCurrentDirectory(d);
                        }
                    }
                    int r = fc.showOpenDialog(Options.this);

                    if (r == JFileChooser.APPROVE_OPTION) {
                        f.setText(fc.getSelectedFile().getAbsolutePath());
                    }
                }
            });
        }
        p.add(b, BorderLayout.EAST);

        constraint.gridx = 1;

        constraint.fill = GridBagConstraints.HORIZONTAL;
        panel.add(p, constraint);

        constraint.fill = GridBagConstraints.NONE;

        constraint.gridy++;
        return a;
    }

    void addSeparator(JPanel panel) {
        constraint.gridx = 0;
        constraint.gridwidth = 2;

        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());

        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setPreferredSize(new Dimension(1, 1));
        p.add(sep, BorderLayout.CENTER);

        constraint.fill = GridBagConstraints.HORIZONTAL;
        constraint.insets = new Insets(10, 2, 10, 2);
        panel.add(p, constraint);
        constraint.insets = new Insets(2, 2, 2, 2);
        constraint.fill = GridBagConstraints.NONE;
        constraint.gridwidth = 1;
        constraint.gridy++;
    }

    JSpinner addSpinner(JPanel panel, String label, int min, int max, int step, int value, String suffix) {
        JLabel l = new JLabel(label);
        constraint.gridx = 0;
        constraint.gridwidth = 1;
        constraint.gridheight = 1;
        constraint.anchor = GridBagConstraints.LINE_START;
        panel.add(l, constraint);

        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        JSpinner a = new JSpinner(new SteppedNumericSpinnerModel(min, max, step, value));

        p.add(a, BorderLayout.CENTER);
        JLabel b = new JLabel(suffix);
        p.add(b, BorderLayout.EAST);

        constraint.gridx = 1;

        constraint.fill = GridBagConstraints.HORIZONTAL;
        panel.add(p, constraint);

        constraint.fill = GridBagConstraints.NONE;

        constraint.gridy++;
        return a;

    }

    JCheckBox addCheckBox(JPanel panel, String label, boolean state) {
        constraint.gridx = 1;
        JCheckBox cb = new JCheckBox(label);
        cb.setSelected(state);
        panel.add(cb, constraint);
        constraint.gridy++;
        return cb;
    }
        

    public Options(JFrame parent) {
        loadPreferences(); // Just in case. It should do nothing.

        setLayout(new BorderLayout());

        tabs = new JTabbedPane();

        JPanel optionsPanel = new JPanel();

        optionsPanel.setLayout(new GridBagLayout());

        constraint = new GridBagConstraints();

        constraint.gridx = 0;
        constraint.gridy = 0;
        constraint.gridwidth = 1;
        constraint.gridheight = 1;

        constraint.insets = new Insets(2, 2, 2, 2);

        addSeparator(optionsPanel);

        mixerList = addDropdown(optionsPanel, "Recording device:", getRecordingMixerList(), get("audio.recording.device"));
        channelList = addDropdown(optionsPanel, "Channels:", getChannelCountList(), get("audio.recording.channels"));
        rateList = addDropdown(optionsPanel, "Sample rate:", getSampleRateList(), get("audio.recording.samplerate"));

        addSeparator(optionsPanel);

        playbackList = addDropdown(optionsPanel, "Playback device:", getPlaybackMixerList(), get("audio.playback.device"));
        addSeparator(optionsPanel);
        storageFolder = addFilePath(optionsPanel, "Storage folder:", get("path.storage"), true);

        addSeparator(optionsPanel);

        preChapterGap = addSpinner(optionsPanel, "Default pre-chapter gap:", 0, 5000, 100, getInteger("catenation.pre-chapter"), "ms");
        postChapterGap = addSpinner(optionsPanel, "Default post-chapter gap:", 0, 5000, 100, getInteger("catenation.post-chapter"), "ms");
        postSentenceGap = addSpinner(optionsPanel, "Default post-sentence gap:", 0, 5000, 100, getInteger("catenation.post-sentence"), "ms");
        postParagraphGap = addSpinner(optionsPanel, "Default post-paragraph gap:", 0, 5000, 100, getInteger("catenation.post-paragraph"), "ms");

        addSeparator(optionsPanel);

        ffmpegLocation = addFilePath(optionsPanel, "FFMPEG location:", get("path.ffmpeg"), false);
        bitRate = addDropdown(optionsPanel, "Export bitrate:", getBitrates(), get("audio.export.bitrate"));
        exportRate = addDropdown(optionsPanel, "Export sample rate:", getSampleRateList(), get("audio.export.samplerate"));
        

        addSeparator(optionsPanel);

        enableParsing = addCheckBox(optionsPanel, "Enable automatic sphinx speech-to-text (**SLOW**)", getBoolean("process.sphinx"));

        addSeparator(optionsPanel);

        cacheSize = addSpinner(optionsPanel, "Cache size:", 0, 5000, 1, getInteger("cache.size"), "");

        addSeparator(optionsPanel);

        tabs.add("Options", optionsPanel);



        JPanel eqPanel = new JPanel();

        eqPanel.setLayout(new GridBagLayout());

        constraint.gridx = 0;
        constraint.gridy = 0;

        sliders = new JSliderOb[31];
        eqvals = new JTextFieldOb[31];

        for (int i = 0; i < 31; i++) {
            sliders[i] = new JSliderOb(-120, 120, (int)(Options.getFloat("audio.eq." + i) * 10));
            sliders[i].setOrientation(SwingConstants.VERTICAL);
            constraint.gridx = i;
            constraint.gridy = 0;
            eqPanel.add(sliders[i], constraint);
            eqvals[i] = new JTextFieldOb(String.format("%.1f", Options.getFloat("audio.eq." + i)));
            constraint.gridy = 1;
            eqPanel.add(eqvals[i], constraint);

            sliders[i].setObject(eqvals[i]);
            eqvals[i].setObject(sliders[i]);

            eqvals[i].setPreferredSize(new Dimension(40, 20));

            sliders[i].addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    JSliderOb o = (JSliderOb)e.getSource();
                    String v = String.format("%.1f", (float)o.getValue() / 10.0f);
                    JTextFieldOb tf = (JTextFieldOb)o.getObject();
                    tf.setText(v);
                }
            });

            eqvals[i].addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent e) {
                    JTextFieldOb o = (JTextFieldOb)e.getSource();
                    o.selectAll();
                }

                public void focusLost(FocusEvent e) {
                    JTextFieldOb o = (JTextFieldOb)e.getSource();
                    String v = o.getText();
                    float f = Utils.s2f(v);
                    if (f < -12f) f = -12f;
                    if (f > 12f) f = 12f;
                    JSliderOb s = (JSliderOb)o.getObject();
                    s.setValue((int)(f * 10));
                    o.setText(String.format("%.1f", f));
                }
            });

            eqvals[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JTextFieldOb o = (JTextFieldOb)e.getSource();
                    String v = o.getText();
                    float f = Utils.s2f(v);
                    if (f < -12f) f = -12f;
                    if (f > 12f) f = 12f;
                    JSliderOb s = (JSliderOb)o.getObject();
                    s.setValue((int)(f * 10));
                    o.setText(String.format("%.1f", f));
                    o.selectAll();
                }
            });
        
        }

        JButton smooth = new JButton("Smooth curve");
        smooth.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Float ave[] = new Float[31];
                for (int i = 1; i < 30; i++) {
                    ave[i] = (Utils.s2f(eqvals[i - 1].getText()) + Utils.s2f(eqvals[i].getText()) + Utils.s2f(eqvals[i + 1].getText())) / 3.0f;
                }

                for (int i = 1; i < 30; i++) {
                    eqvals[i].setText(String.format("%.1f", ave[i]));
                    sliders[i].setValue((int)(ave[i] * 10));
                }
            }
        });

        constraint.gridx = 0;
        constraint.gridy = 2;
        constraint.gridwidth = 4;
        eqPanel.add(smooth, constraint);

        tabs.add("EQ", eqPanel);


        add(tabs, BorderLayout.CENTER);

        setTitle("Options");

        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);

        constraint.gridx = 1;
        constraint.gridwidth = 1;
        constraint.gridheight = 1;

        JPanel box = new JPanel();

        JButton ok = new JButton("OK");

        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                storePreferences();
                Options.this.dispatchEvent(new WindowEvent(Options.this, WindowEvent.WINDOW_CLOSING));
            }
        });

        box.add(ok);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Options.this.dispatchEvent(new WindowEvent(Options.this, WindowEvent.WINDOW_CLOSING));
            }
        });

        box.add(cancel);

        constraint.anchor = GridBagConstraints.LINE_END;

        add(box, BorderLayout.SOUTH);
        
        pack();

        setLocationRelativeTo(parent);

        setIconImage(Icons.appIcon.getImage());
        setVisible(true);
    }

    static KVPair[] getRecordingMixerList() {
        TreeSet<KVPair> list = new TreeSet<KVPair>();

        AudioFormat stereoFormat = new AudioFormat(44100f, 16, 2, true, false);
        AudioFormat monoFormat = new AudioFormat(44100f, 16, 2, true, false);

        DataLine.Info stereoDIF = new DataLine.Info(TargetDataLine.class, stereoFormat);
        DataLine.Info monoDIF = new DataLine.Info(TargetDataLine.class, monoFormat);

        Mixer.Info[] info = AudioSystem.getMixerInfo();
        for (Mixer.Info i : info) {
            Mixer m = AudioSystem.getMixer(i);

            boolean supported = false;

            try {
                m.getLine(stereoDIF);
                supported = true;
            } catch (Exception e) {
            }

            try {
                m.getLine(monoDIF);
                supported = true;
            } catch (Exception e) {
            }

            if (supported) {
                KVPair p = new KVPair(i.getName(), i.getName()); //i.getDescription());
                list.add(p);
            }
        }

        return list.toArray(new KVPair[0]);
    }

    static KVPair[] getPlaybackMixerList() {
        TreeSet<KVPair> list = new TreeSet<KVPair>();

        AudioFormat stereoFormat = new AudioFormat(44100f, 16, 2, true, false);
        AudioFormat monoFormat = new AudioFormat(44100f, 16, 2, true, false);

        DataLine.Info stereoDIF = new DataLine.Info(SourceDataLine.class, stereoFormat);
        DataLine.Info monoDIF = new DataLine.Info(SourceDataLine.class, monoFormat);

        Mixer.Info[] info = AudioSystem.getMixerInfo();
        for (Mixer.Info i : info) {
            Mixer m = AudioSystem.getMixer(i);

            boolean supported = false;

            try { 
                m.getLine(stereoDIF);
                supported = true; 
            } catch (Exception e) {
            }

            try { 
                m.getLine(monoDIF);
                supported = true; 
            } catch (Exception e) {
            }


            if (supported) {
                KVPair p = new KVPair(i.getName(), i.getName()); //i.getDescription());
                list.add(p);
            }
        }

        return list.toArray(new KVPair[0]);
    }

    static KVPair[] getChannelCountList() {
        KVPair[] l = new KVPair[2];
        l[0] = new KVPair("1", "Mono");
        l[1] = new KVPair("2", "Stereo");
        return l;
    }

    static KVPair[] getSampleRateList() {
        KVPair[] l = new KVPair[2];
        l[0] = new KVPair("44100", "44100");
        l[1] = new KVPair("48000", "48000");
        return l;
    }

    public static void loadPreferences() {

        defaultPrefs = new HashMap<String, String>();

        KVPair[] recordingMixers = getRecordingMixerList();
        KVPair[] playbackMixers = getPlaybackMixerList();

        if (recordingMixers.length > 0) {
            defaultPrefs.put("audio.recording.device", recordingMixers[0].key);
        } else {
            defaultPrefs.put("audio.recording.device", "");
        }
        defaultPrefs.put("audio.recording.channels", "2");
        defaultPrefs.put("audio.recording.samplerate", "44100");
        if (playbackMixers.length > 0) {
            defaultPrefs.put("audio.playback.device", playbackMixers[0].key);
        } else {
            defaultPrefs.put("audio.playback.device", "");
        }

        defaultPrefs.put("catenation.pre-chapter", "1000");
        defaultPrefs.put("catenation.post-chapter", "1500");
        defaultPrefs.put("catenation.post-sentence", "1000");
        defaultPrefs.put("catenation.post-paragraph", "2000");
    
        defaultPrefs.put("path.storage", (new File(System.getProperty("user.home"), "Recordings")).toString());
        defaultPrefs.put("path.ffmpeg", "");

        defaultPrefs.put("audio.export.bitrate", "256000");
        defaultPrefs.put("audio.export.samplerate", "44100");

        defaultPrefs.put("process.sphinx", "false");

        defaultPrefs.put("cache.size", "100");

        defaultPrefs.put("audio.eq.0", "0.00");
        defaultPrefs.put("audio.eq.1", "0.00");
        defaultPrefs.put("audio.eq.2", "0.00");
        defaultPrefs.put("audio.eq.3", "0.00");
        defaultPrefs.put("audio.eq.4", "0.00");
        defaultPrefs.put("audio.eq.5", "0.00");
        defaultPrefs.put("audio.eq.6", "0.00");
        defaultPrefs.put("audio.eq.7", "0.00");
        defaultPrefs.put("audio.eq.8", "0.00");
        defaultPrefs.put("audio.eq.9", "0.00");
        defaultPrefs.put("audio.eq.10", "0.00");
        defaultPrefs.put("audio.eq.11", "0.00");
        defaultPrefs.put("audio.eq.12", "0.00");
        defaultPrefs.put("audio.eq.13", "0.00");
        defaultPrefs.put("audio.eq.14", "0.00");
        defaultPrefs.put("audio.eq.15", "0.00");
        defaultPrefs.put("audio.eq.16", "0.00");
        defaultPrefs.put("audio.eq.17", "0.00");
        defaultPrefs.put("audio.eq.18", "0.00");
        defaultPrefs.put("audio.eq.19", "-1.00");
        defaultPrefs.put("audio.eq.20", "-2.00");
        defaultPrefs.put("audio.eq.21", "-3.00");
        defaultPrefs.put("audio.eq.22", "-4.00");
        defaultPrefs.put("audio.eq.23", "-5.00");
        defaultPrefs.put("audio.eq.24", "-6.00");
        defaultPrefs.put("audio.eq.25", "-7.00");
        defaultPrefs.put("audio.eq.26", "-8.00");
        defaultPrefs.put("audio.eq.27", "-9.00");
        defaultPrefs.put("audio.eq.28", "-10.00");
        defaultPrefs.put("audio.eq.29", "-11.00");
        defaultPrefs.put("audio.eq.30", "-12.00");

        if (prefs == null) {
            prefs = Preferences.userNodeForPackage(AudiobookRecorder.class);
        }
    }

    public static void savePreferences() {
        if (prefs != null) {
            try {
                prefs.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String get(String key) {
        if (prefs == null) return null;
        String def = defaultPrefs.get(key);
        String v = prefs.get(key, def);
        return v;
    }

    public static Integer getInteger(String key) {
        try {
            Integer i = Integer.parseInt(get(key));
            return i;
        } catch (Exception e) {
        }
        return 0;
    }

    public static Float getFloat(String key) {
        try {
            Float f = Float.parseFloat(get(key));
            return f;
        } catch (Exception e) {
        }
        return 0.0f;
    }

    public static boolean getBoolean(String key) {
        String v = get(key);
        if (v == null) return false;
        if (v.equals("true")) return true;
        if (v.equals("t")) return true;
        if (v.equals("yes")) return true;
        if (v.equals("y")) return true;
        return false;
    }

    public static void set(String key, String value) {
        if (prefs == null) return;
        prefs.put(key, value);
    }

    public static void set(String key, Integer value) {
        set(key, String.format("%d", value));
    }

    public static void set(String key, Boolean value) {
        if (value) {
            set(key, "true");
        } else {
            set(key, "false");
        }
    }

    public static void set(String key, Float value) {
        set(key, String.format("%.3f", value));
    }

    public static void set(String key, Object value) {
        if (value instanceof Integer) {
            set(key, (Integer)value);
        } else if (value instanceof Float) {
            set(key, (Float)value);
        } else if (value instanceof String) {
            set(key, (String)value);
        } else if (value instanceof Boolean) {
            set(key, (Boolean)value);
        } else {
            System.err.println("Bad type for key " + key);
        }
    }

    void storePreferences() {
        set("audio.recording.device", ((KVPair)mixerList.getSelectedItem()).key);
        set("audio.recording.channels", ((KVPair)channelList.getSelectedItem()).key);
        set("audio.recording.samplerate", ((KVPair)rateList.getSelectedItem()).key);
        set("audio.playback.device", ((KVPair)playbackList.getSelectedItem()).key);
        set("path.storage", storageFolder.getText());
        set("path.ffmpeg", ffmpegLocation.getText());
        set("catenation.pre-chapter", preChapterGap.getValue());
        set("catenation.post-chapter", postChapterGap.getValue());
        set("catenation.post-sentence", postSentenceGap.getValue());
        set("catenation.post-paragraph", postParagraphGap.getValue());
        set("audio.export.bitrate", ((KVPair)bitRate.getSelectedItem()).key);
        set("audio.export.samplerate", ((KVPair)exportRate.getSelectedItem()).key);
        set("process.sphinx", enableParsing.isSelected());
        set("cache.size", cacheSize.getValue());

        for (int i = 0; i < 31; i++) {
            set("audio.eq." + i, eqvals[i].getText());
        }

        savePreferences();
    }

    public static AudioFormat getAudioFormat() {
        String sampleRate = get("audio.recording.samplerate");
        String channels = get("audio.recording.channels");

        float sr = 48000f;
        int chans = 2;

        try {
            sr = Float.parseFloat(sampleRate);
        } catch (Exception e) {
            sr = 48000f;
        }

        try {
            chans = Integer.parseInt(channels);
        } catch (Exception e) {
            chans = 2;
        }
    
        AudioFormat af = new AudioFormat(sr, 16, chans, true, false);
        return af;
    }

    public static Mixer.Info getMixerByName(String name) {

        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixers) {
            if (info.getName().equals(name)) {
                return info;
            }
        }

        return mixers[0];
    }

    public static Mixer.Info getRecordingMixer() {
        return getMixerByName(get("audio.recording.device"));
    }

    public static Mixer.Info getPlaybackMixer() {
        return getMixerByName(get("audio.playback.device"));
    }

    public static KVPair[] getBitrates() {
        KVPair[] pairs = new KVPair[4];
        pairs[0] = new KVPair("128000", "128kbps");
        pairs[1] = new KVPair("192000", "192kbps");
        pairs[2] = new KVPair("256000", "256kbps");
        pairs[3] = new KVPair("320000", "320kbps");
        return pairs;
    }
}
