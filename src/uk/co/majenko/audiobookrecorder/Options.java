package uk.co.majenko.audiobookrecorder;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.prefs.*;
import java.io.*;
import javax.swing.tree.*;

public class Options extends JDialog {

    JTabbedPane tabs;

    GridBagConstraints constraint;

    public static ArrayList<EffectGroup> effectChains;

    JComboBox<KVPair> mixerList;
    JComboBox<KVPair> playbackList;
    JComboBox<KVPair> channelList;
    JComboBox<KVPair> rateList;
    JComboBox<KVPair> bitDepth;
    JComboBox<KVPair> trimMethod;
    JTextField storageFolder;
    JTextField archiveFolder;
    JSpinner preChapterGap;
    JSpinner postChapterGap;
    JSpinner postSentenceGap;
    JSpinner shortSentenceGap;
    JSpinner postParagraphGap;
    JSpinner postSectionGap;
    JTextField ffmpegLocation;
    JComboBox<KVPair> bitRate;
    JComboBox<KVPair> exportRate;
    JCheckBox enableParsing;
    JSpinner cacheSize;

    JSpinner fftThreshold;

    JSpinner etherealIterations;
    JSpinner etherealAttenuation;
    JSpinner etherealOffset;

    JTextField havenApiKey;

    JTextField externalEditor;

    JTextArea startupScript;

    ArrayList<JTextField[]> processorList;

    static HashMap<String, String> defaultPrefs;
    static Preferences prefs = null;

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

    void addTwoLabel(JPanel panel, String label1, String label2) {
        JLabel l1 = new JLabel(label1);
        constraint.gridx = 0;
        constraint.gridwidth = 1;
        constraint.gridheight = 1;
        constraint.anchor = GridBagConstraints.LINE_START;
        panel.add(l1, constraint);

        JLabel l2 = new JLabel(label2);
        constraint.gridx = 1;
        constraint.gridwidth = 1;
        constraint.gridheight = 1;
        constraint.anchor = GridBagConstraints.LINE_START;
        panel.add(l2, constraint);

        constraint.gridy++;
    }
        
    JTextField addTextField(JPanel panel, String label, String def) {
        JLabel l = new JLabel(label);
        constraint.gridx = 0;
        constraint.gridwidth = 1;
        constraint.gridheight = 1;
        constraint.anchor = GridBagConstraints.LINE_START;
        panel.add(l, constraint);

        JTextField a = new JTextField(def);
        constraint.gridx = 1;

        constraint.fill = GridBagConstraints.HORIZONTAL;
        panel.add(a, constraint);

        constraint.fill = GridBagConstraints.NONE;

        constraint.gridy++;
        return a;
    }

    JTextField[] addTwoField(JPanel panel, String def1, String def2) {
        JTextField a = new JTextField(def1);
        constraint.gridx = 0;
        constraint.fill = GridBagConstraints.HORIZONTAL;
        panel.add(a, constraint);

        JTextField b = new JTextField(def2);
        constraint.gridx = 1;
        panel.add(b, constraint);
        constraint.fill = GridBagConstraints.NONE;

        constraint.gridy++;
        return new JTextField[] { a, b };
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
        bitDepth = addDropdown(optionsPanel, "Sample resolution:", getResolutionList(), get("audio.recording.resolution"));
        trimMethod = addDropdown(optionsPanel, "Auto-trim method:", getTrimMethods(), get("audio.recording.trim"));
        fftThreshold = addSpinner(optionsPanel, "FFT threshold:", 0, 100, 1, getInteger("audio.recording.trim.fft"), "");

        addSeparator(optionsPanel);

        playbackList = addDropdown(optionsPanel, "Playback device:", getPlaybackMixerList(), get("audio.playback.device"));
        addSeparator(optionsPanel);
        storageFolder = addFilePath(optionsPanel, "Storage folder:", get("path.storage"), true);
        archiveFolder = addFilePath(optionsPanel, "Archive folder:", get("path.archive"), true);

        addSeparator(optionsPanel);

        preChapterGap = addSpinner(optionsPanel, "Default pre-chapter gap:", 0, 5000, 100, getInteger("catenation.pre-chapter"), "ms");
        postChapterGap = addSpinner(optionsPanel, "Default post-chapter gap:", 0, 5000, 100, getInteger("catenation.post-chapter"), "ms");
        postSentenceGap = addSpinner(optionsPanel, "Default post-sentence gap:", 0, 5000, 100, getInteger("catenation.post-sentence"), "ms");
        shortSentenceGap = addSpinner(optionsPanel, "Short post-sentence gap:", 0, 5000, 100, getInteger("catenation.short-sentence"), "ms");
        postParagraphGap = addSpinner(optionsPanel, "Default post-paragraph gap:", 0, 5000, 100, getInteger("catenation.post-paragraph"), "ms");
        postSectionGap = addSpinner(optionsPanel, "Default post-section gap:", 0, 5000, 100, getInteger("catenation.post-section"), "ms");

        addSeparator(optionsPanel);

        ffmpegLocation = addFilePath(optionsPanel, "FFMPEG location:", get("path.ffmpeg"), false);
        bitRate = addDropdown(optionsPanel, "Export bitrate:", getBitrates(), get("audio.export.bitrate"));
        exportRate = addDropdown(optionsPanel, "Export sample rate:", getSampleRateList(), get("audio.export.samplerate"));
        

        addSeparator(optionsPanel);

        enableParsing = addCheckBox(optionsPanel, "Enable automatic speech-to-text submission", getBoolean("process.haven.auto"));
        havenApiKey = addTextField(optionsPanel, "Haven OnDemand API Key", get("process.haven.apikey"));

        addSeparator(optionsPanel);
        
        externalEditor = addTextField(optionsPanel, "External Editor Command", get("editor.external"));

        addSeparator(optionsPanel);

        cacheSize = addSpinner(optionsPanel, "Cache size:", 0, 5000, 1, getInteger("cache.size"), "");

        addSeparator(optionsPanel);
        tabs.add("Options", new JScrollPane(optionsPanel));

        JPanel effectChains = new JPanel();
        effectChains.setLayout(new BorderLayout());

        JPanel effectDetails = new JPanel() {
            public Dimension getPreferredSize() {
                return new Dimension(200, 400);
            }
        };

        DefaultTreeModel m = new DefaultTreeModel(new DefaultMutableTreeNode("Effect Chains"));

        JTree effectChainTree = new JTree(m);
        effectChains.add(effectChainTree, BorderLayout.CENTER);
        effectChains.add(effectDetails, BorderLayout.EAST);

        tabs.add("Effects Chains", new JScrollPane(effectChains));

        JPanel startScript = new JPanel();
        startScript.setLayout(new BorderLayout());
        startupScript = new JTextArea(get("scripts.startup"));
        startScript.add(startupScript, BorderLayout.CENTER);
        tabs.add("Startup Script", startScript);



        JPanel effects = new JPanel();
        effects.setLayout(new GridBagLayout());
        constraint.gridx = 0;
        constraint.gridy = 0;
        constraint.gridwidth = 1;
        constraint.gridheight = 1;

        etherealOffset = addSpinner(effects, "Ethereal Voice Offset", 0, 2000, 10, getInteger("effects.ethereal.offset"), "ms");
        etherealIterations = addSpinner(effects, "Ethereal Voice Iterations", 1, 10, 1, getInteger("effects.ethereal.iterations"), "");
        etherealAttenuation = addSpinner(effects, "Ethereal Voice Attenuation", 0, 100, 1, getInteger("effects.ethereal.attenuation"), "%");

        tabs.add("Effects", effects);


        JPanel processors = new JPanel();
        processors.setLayout(new BorderLayout());
        JPanel processorListPanel = new JPanel();

        JScrollPane psp = new JScrollPane(processorListPanel);

        processors.add(psp, BorderLayout.CENTER);
        processorListPanel.setLayout(new GridBagLayout());

        constraint.gridx = 0;
        constraint.gridy = 0;
        constraint.gridwidth = 1;
        constraint.gridheight = 1;

        addTwoLabel(processorListPanel, "Name", "Command");

        processorList = new ArrayList<JTextField[]>();

        for (int i = 0; i < 999; i++) {
            String name = get("editor.processor." + i + ".name");
            String command = get("editor.processor." + i + ".command");
            if (name == null || command == null) break;
            if (name.equals("") || command.equals("")) break;
            JTextField[] f = addTwoField(processorListPanel, name, command);
            processorList.add(f);
        }

        JTextField[] f = addTwoField(processorListPanel, "", "");
        processorList.add(f);

        tabs.add("Processors", processors);
        




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

    @SuppressWarnings("unchecked")
    static KVPair<String, String>[] getRecordingMixerList() {
        TreeSet<KVPair<String, String>> list = new TreeSet<KVPair<String, String>>();

        AudioFormat stereoFormat = new AudioFormat(44100f, 16, 2, true, false);
        AudioFormat monoFormat = new AudioFormat(44100f, 16, 1, true, false);

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
                KVPair<String, String> p = new KVPair<String, String>(i.getName(), i.getName()); //i.getDescription());
                list.add(p);
            }
        }

        return list.toArray(new KVPair[0]);
    }

    static KVPair[] getPlaybackMixerList() {
        TreeSet<KVPair<String, String>> list = new TreeSet<KVPair<String, String>>();

        AudioFormat stereoFormat = new AudioFormat(44100f, 16, 2, true, false);
        AudioFormat monoFormat = new AudioFormat(44100f, 16, 1, true, false);

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
                KVPair<String, String> p = new KVPair<String, String>(i.getName(), i.getName()); //i.getDescription());
                list.add(p);
            }
        }

        return list.toArray(new KVPair[0]);
    }

    static KVPair[] getChannelCountList() {
        KVPair[] l = new KVPair[2];
        l[0] = new KVPair<String, String>("1", "Mono");
        l[1] = new KVPair<String, String>("2", "Stereo");
        return l;
    }

    static KVPair[] getSampleRateList() {
        KVPair[] l = new KVPair[3];
        l[0] = new KVPair<String, String>("44100", "44100");
        l[1] = new KVPair<String, String>("48000", "48000");
        l[2] = new KVPair<String, String>("96000", "96000");
        return l;
    }

    public static void loadPreferences() {

        defaultPrefs = new HashMap<String, String>();

        KVPair[] recordingMixers = getRecordingMixerList();
        KVPair[] playbackMixers = getPlaybackMixerList();

        if (recordingMixers.length > 0) {
            defaultPrefs.put("audio.recording.device", (String)recordingMixers[0].key);
        } else {
            defaultPrefs.put("audio.recording.device", "");
        }
        defaultPrefs.put("audio.recording.channels", "2");
        defaultPrefs.put("audio.recording.samplerate", "44100");
        defaultPrefs.put("audio.recording.resolution", "16");
        defaultPrefs.put("audio.recording.trim", "peak");
        if (playbackMixers.length > 0) {
            defaultPrefs.put("audio.playback.device", (String)playbackMixers[0].key);
        } else {
            defaultPrefs.put("audio.playback.device", "");
        }

        defaultPrefs.put("catenation.pre-chapter", "1000");
        defaultPrefs.put("catenation.post-chapter", "1500");
        defaultPrefs.put("catenation.post-sentence", "1000");
        defaultPrefs.put("catenation.short-sentence", "100");
        defaultPrefs.put("catenation.post-paragraph", "2000");
        defaultPrefs.put("catenation.post-section", "3000");

        defaultPrefs.put("audio.recording.trim.fft", "10");
    
        defaultPrefs.put("path.storage", (new File(System.getProperty("user.home"), "Recordings")).toString());
        defaultPrefs.put("path.archive", (new File(new File(System.getProperty("user.home"), "Recordings"),"archive")).toString());
        defaultPrefs.put("path.ffmpeg", "");

        defaultPrefs.put("audio.export.bitrate", "256000");
        defaultPrefs.put("audio.export.samplerate", "44100");

        defaultPrefs.put("process.sphinx", "false");
        defaultPrefs.put("process.haven.apikey", "");

        defaultPrefs.put("editor.external", "");

        defaultPrefs.put("cache.size", "100");

        defaultPrefs.put("scripts.startup", "");

        defaultPrefs.put("effects.ethereal.offset", "50");
        defaultPrefs.put("effects.ethereal.iterations", "3");
        defaultPrefs.put("effects.ethereal.attenuation", "50");

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

    public static Double getDouble(String key) {
        try {
            Double f = Double.parseDouble(get(key));
            return f;
        } catch (Exception e) {
        }
        return 0.0d;
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
        set("audio.recording.resolution", ((KVPair)bitDepth.getSelectedItem()).key);
        set("audio.recording.trim", ((KVPair)trimMethod.getSelectedItem()).key);
        set("audio.playback.device", ((KVPair)playbackList.getSelectedItem()).key);
        set("path.storage", storageFolder.getText());
        set("path.archive", archiveFolder.getText());
        set("path.ffmpeg", ffmpegLocation.getText());
        set("catenation.pre-chapter", preChapterGap.getValue());
        set("catenation.post-chapter", postChapterGap.getValue());
        set("catenation.post-sentence", postSentenceGap.getValue());
        set("catenation.short-sentence", shortSentenceGap.getValue());
        set("catenation.post-paragraph", postParagraphGap.getValue());
        set("catenation.post-section", postSectionGap.getValue());
        set("audio.export.bitrate", ((KVPair)bitRate.getSelectedItem()).key);
        set("audio.export.samplerate", ((KVPair)exportRate.getSelectedItem()).key);
        set("process.sphinx", enableParsing.isSelected());
        set("process.haven.apikey", havenApiKey.getText());
        set("editor.external", externalEditor.getText());
        set("cache.size", cacheSize.getValue());
        set("audio.recording.trim.fft", fftThreshold.getValue());

        set("effects.ethereal.offset", etherealOffset.getValue());
        set("effects.ethereal.iterations", etherealIterations.getValue());
        set("effects.ethereal.attenuation", etherealAttenuation.getValue());

        set("scripts.startup", startupScript.getText());

        int procNo = 0;
        for (JTextField[] proc : processorList) {
            String name = proc[0].getText();
            String command = proc[1].getText();
            if (name.equals("") || command.equals("")) {
                continue;
            }
            set("editor.processor." + procNo + ".name", name);
            set("editor.processor." + procNo + ".command", command);
            procNo++;
        }

        savePreferences();
    }

    public static AudioFormat getAudioFormat() {
        AudioFormat af = new AudioFormat(
            getInteger("audio.recording.samplerate"), 
            getInteger("audio.recording.resolution"), 
            getInteger("audio.recording.channels"), true, false);
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
        pairs[0] = new KVPair<String, String>("128000", "128kbps");
        pairs[1] = new KVPair<String, String>("192000", "192kbps");
        pairs[2] = new KVPair<String, String>("256000", "256kbps");
        pairs[3] = new KVPair<String, String>("320000", "320kbps");
        return pairs;
    }

    public static KVPair[] getResolutionList() {
        KVPair[] pairs = new KVPair[2];
        pairs[0] = new KVPair<String, String>("16", "16 Bit");
        pairs[1] = new KVPair<String, String>("24", "24 Bit");
        return pairs;
    }

    public static KVPair[] getTrimMethods() {
        KVPair[] pairs = new KVPair[2];
        pairs[0] = new KVPair<String, String>("peak", "Peak Amplitude");
        pairs[1] = new KVPair<String, String>("fft", "FFT Analysis");
        return pairs;
    }


    public static void createEffectChains() {
        effectChains = new ArrayList<EffectGroup>();

        for (int i = 0; i < 999999; i++) {
            if (get("effects." + i + ".name") == null) {
                EffectGroup e = new EffectGroup(get("effects." + i + ".name"));
                for (int j = 0; i < 999999; j++) {
                    String type = get("effects." + i + ".children." + j + ".type");
                    if (type == null) break;

                    if (type.equals("biquad")) {
                        int bqt = getInteger("effects." + i + ".children." + j + ".filtertype");
                        double fc = getDouble("effects." + i + ".children." + j + ".fc");
                        double q = getDouble("effects." + i + ".children." + j + ".q");
                        double gain = getDouble("effects." + i + ".children." + j + ".gain");
                        Biquad b = new Biquad(bqt, fc, q, gain);
                        e.addEffect(b);
                        continue;
                    } 

                    if (type.equals("amplifier")) {
                        double gain = getDouble("effects." + i + ".children." + j + ".gain");
                        Amplifier a = new Amplifier(gain);
                        e.addEffect(a);
                        continue;
                    }

                    if (type.equals("delayline")) {
                        DelayLine l = new DelayLine();
                        for (int c = 0; c < 999999; c++) {
                            if (get("effects." + i + ".children." + j + ".lines." + c + ".samples") == null) break;
                            int samples = getInteger("effects." + i + ".children." + j + ".lines." + c + ".samples");
                            double gain = getDouble("effects." + i + ".children." + j + ".lines." + c + ".gain");
                            l.addDelayLine(samples, gain);
                        }
                        e.addEffect(l);
                        continue;
                    }


                }
                effectChains.add(e);
                break;
            }
        }
    }
}
