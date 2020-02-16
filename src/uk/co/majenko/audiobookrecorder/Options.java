package uk.co.majenko.audiobookrecorder;

import java.io.File;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.prefs.Preferences;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Font;
import java.awt.Dialog;
import java.awt.event.WindowEvent;
import javax.swing.border.EmptyBorder;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.JScrollPane;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public class Options extends JDialog {

    JTabbedPane tabs;

    GridBagConstraints constraint;

    JComboBox<KVPair> mixerList;
    JComboBox<KVPair> playbackList;
    JComboBox<KVPair> channelList;
    JComboBox<KVPair> rateList;
    JComboBox<KVPair> bitDepth;
    JComboBox<KVPair> trimMethod;
    JComboBox<KVPair> fftBlockSize;
    JComboBox<KVPair> playbackBlockSize;
    JTextField storageFolder;
    JTextField archiveFolder;
    JSpinner preChapterGap;
    JSpinner postChapterGap;
    JSpinner postSentenceGap;
    JSpinner shortSentenceGap;
    JSpinner postParagraphGap;
    JSpinner postSectionGap;
    JSpinner maxGainVariance;
    JTextField ffmpegLocation;
    JComboBox<KVPair> bitRate;
    JComboBox<KVPair> channels;
    JComboBox<KVPair> exportRate;
    JCheckBox enableParsing;
    JSpinner cacheSize;

    JSpinner fftThreshold;

    JSpinner etherealIterations;
    JSpinner etherealAttenuation;
    JSpinner etherealOffset;

    JTextField externalEditor;

    JTextField speechCommand;
    JSpinner workerThreads;

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

    JComboBox<KVPair> addDropdown(JPanel panel, String label, KVPair[] options, String def, String tip) {
        JLabel l = new JLabel(label);
        constraint.gridx = 0;
        constraint.gridwidth = 1;
        constraint.gridheight = 1;
        constraint.anchor = GridBagConstraints.LINE_START;
        panel.add(l, constraint);

        JComboBox<KVPair> o = new JComboBox<KVPair>(options);
        constraint.gridx = 1;
        panel.add(o, constraint);
        Tip t = new Tip(tip);
        constraint.gridx = 2;
        panel.add(t, constraint);

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
        
    JTextField addTextField(JPanel panel, String label, String def, String tip) {
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

        Tip t = new Tip(tip);
        constraint.gridx = 2;
        panel.add(t, constraint);


        constraint.fill = GridBagConstraints.NONE;

        constraint.gridy++;
        return a;
    }

    JTextField[] addTwoField(JPanel panel, String def1, String def2, String tip) {
        JTextField a = new JTextField(def1);
        constraint.gridx = 0;
        constraint.fill = GridBagConstraints.HORIZONTAL;
        panel.add(a, constraint);

        JTextField b = new JTextField(def2);
        constraint.gridx = 1;
        panel.add(b, constraint);
        constraint.fill = GridBagConstraints.NONE;

        Tip t = new Tip(tip);
        constraint.gridx = 2;
        panel.add(t, constraint);


        constraint.gridy++;
        return new JTextField[] { a, b };
    }

        

    JTextField addFilePath(JPanel panel, String label, String path, boolean dironly, String tip) {
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

        Tip t = new Tip(tip);
        constraint.gridx = 2;
        panel.add(t, constraint);

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

    JSpinner addSpinner(JPanel panel, String label, int min, int max, int step, int value, String suffix, String tip) {
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
        Tip t = new Tip(tip);
        constraint.gridx = 2;
        panel.add(t, constraint);


        constraint.fill = GridBagConstraints.NONE;

        constraint.gridy++;
        return a;

    }

    JCheckBox addCheckBox(JPanel panel, String label, boolean state, String tip) {
        constraint.gridx = 1;
        JCheckBox cb = new JCheckBox(label);
        cb.setSelected(state);
        panel.add(cb, constraint);
        Tip t = new Tip(tip);
        constraint.gridx = 2;
        panel.add(t, constraint);


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

        mixerList = addDropdown(optionsPanel, "Recording device:", getRecordingMixerList(), get("audio.recording.device"), "This is the system device to record through");
        channelList = addDropdown(optionsPanel, "Channels:", getChannelCountList(), get("audio.recording.channels"), "How many channels do you want to record - stereo or mono?");
        rateList = addDropdown(optionsPanel, "Sample rate:", getSampleRateList(), get("audio.recording.samplerate"), "The higher the sample rate the better the quality, but the bigger the files.");
        bitDepth = addDropdown(optionsPanel, "Sample resolution:", getResolutionList(), get("audio.recording.resolution"), "The higher the resolution the better the quality, but the bigger the files.");
        trimMethod = addDropdown(optionsPanel, "Auto-trim method:", getTrimMethods(), get("audio.recording.trim"), "None: don't auto-trim. FFT: Compare the FFT profile of blocks to the room noise profile and trim silent blocks, Peak: Look for the start and end rise and fall points");
        fftThreshold = addSpinner(optionsPanel, "FFT threshold:", 0, 100, 1, getInteger("audio.recording.trim.fft"), "", "This specifies the difference (in hundredths) between the power of FFT buckets in a sample block compared to the overall power of the same FFT bucket in the room noise. Raising this number makes the FFT trimming less sensitive.");
        fftBlockSize = addDropdown(optionsPanel, "FFT Block size:", getFFTBlockSizes(), get("audio.recording.trim.blocksize"), "How large an FFT block should be when processing. Larger values increase sensitivity but at the epxense of resolution.");
        maxGainVariance = addSpinner(optionsPanel, "Maximum gain variance:", 0, 100, 1, getInteger("audio.recording.variance"), "", "This is how much the gain is allowed to vary by from phrase to phrase when normalizing an entire chapter.");

        addSeparator(optionsPanel);

        playbackList = addDropdown(optionsPanel, "Playback device:", getPlaybackMixerList(), get("audio.playback.device"), "Which device to play back through");
        playbackBlockSize = addDropdown(optionsPanel, "Playback Block size:", getPlaybackBlockSizes(), get("audio.playback.blocksize"), "How big the playback buffer should be. Larger is smoother playback but the playback marker in the waveform becomes more out of sync");
        addSeparator(optionsPanel);
        storageFolder = addFilePath(optionsPanel, "Storage folder:", get("path.storage"), true, "This is where all your working audiobooks are stored.");
        archiveFolder = addFilePath(optionsPanel, "Archive folder:", get("path.archive"), true, "This is where audiobooks are archived to.");

        addSeparator(optionsPanel);

        preChapterGap = addSpinner(optionsPanel, "Default pre-chapter gap:", 0, 5000, 100, getInteger("catenation.pre-chapter"), "ms", "How much room noise to add at the beginning of a chapter.");
        postChapterGap = addSpinner(optionsPanel, "Default post-chapter gap:", 0, 5000, 100, getInteger("catenation.post-chapter"), "ms", "How much room noise to add to the end of a chapter.");
        postSentenceGap = addSpinner(optionsPanel, "Default post-sentence gap:", 0, 5000, 100, getInteger("catenation.post-sentence"), "ms", "How much room noise to add between normal sentences.");
        shortSentenceGap = addSpinner(optionsPanel, "Short post-sentence gap:", 0, 5000, 100, getInteger("catenation.short-sentence"), "ms", "How much room noise to add between 'continuations'.");
        postParagraphGap = addSpinner(optionsPanel, "Default post-paragraph gap:", 0, 5000, 100, getInteger("catenation.post-paragraph"), "ms", "How much room noise to add between paragraphs.");
        postSectionGap = addSpinner(optionsPanel, "Default post-section gap:", 0, 5000, 100, getInteger("catenation.post-section"), "ms", "How much room noise to add between sections.");

        addSeparator(optionsPanel);

        ffmpegLocation = addFilePath(optionsPanel, "FFMPEG location:", get("path.ffmpeg"), false, "Path to your ffmpeg executable.");
        bitRate = addDropdown(optionsPanel, "Export bitrate:", getBitrates(), get("audio.export.bitrate"), "The MP3 bitrate to produce");
        channels = addDropdown(optionsPanel, "Export channels:", getChannelCountList(), get("audio.export.channels"), "Mono or stereo MP3 production");
        exportRate = addDropdown(optionsPanel, "Export sample rate:", getSampleRateList(), get("audio.export.samplerate"), "Sample frequency of the produced MP3");
        

        addSeparator(optionsPanel);

        enableParsing = addCheckBox(optionsPanel, "Enable automatic speech-to-text (**SLOW**)", getBoolean("process.sphinx"), "This will automatically start recognising the speech in every sentence you record. This can really slow down recording though so it's recommended to leave it turned off and do your recognition afterwards as a batch operation.");
        speechCommand = addTextField(optionsPanel, "Speech to text command (must take 1 filename parameter):", get("process.command"), "This specifies what command to run to recognize the speech. This command must take only one parameter, which is the full path of the WAV file. It should return (on standard output) the recognised speech.");
        workerThreads = addSpinner(optionsPanel, "Worker threads:", 1, 100, 1, getInteger("process.threads"), "", "How many concurrent threads to run when processing speech. This should ideally be no more than the number of CPU cores you have in your computer minus one.");
    
        addSeparator(optionsPanel);

        externalEditor = addTextField(optionsPanel, "External Editor Command", get("editor.external"), "The program to run when you select 'Open in external editor'.");

        addSeparator(optionsPanel);

        cacheSize = addSpinner(optionsPanel, "Cache size:", 2, 100, 1, getInteger("cache.size"), "", "How many phrases to keep cached in memory at once. More gives a smoother editing experience, but you can easily run out of memory if you are not careful.");

        addSeparator(optionsPanel);
        tabs.add("Options", new JScrollPane(optionsPanel));




        JPanel startScript = new JPanel();
        startScript.setLayout(new BorderLayout());
        startScript.setBorder(new EmptyBorder(15, 15, 15, 15));
        startupScript = new JTextArea(get("scripts.startup"));
        startupScript.setBorder(new EmptyBorder(5, 5, 5, 5));
        startupScript.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        startScript.add(startupScript, BorderLayout.CENTER);
        tabs.add("Startup Script", startScript);




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
            JTextField[] f = addTwoField(processorListPanel, name, command, "Specify the name of the operation (which will appear in context menus) and the command to run for that operation. The command should have parameters separated with :: and %f for the input filename and %o for the output filename.");
            processorList.add(f);
        }

        JTextField[] f = addTwoField(processorListPanel, "", "", "Specify the name of the operation (which will appear in context menus) and the command to run for that operation. The command should have parameters separated with :: and %f for the input filename and %o for the output filename.");
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
        return getMixerList(TargetDataLine.class);
    }

    static KVPair[] getPlaybackMixerList() {
        return getMixerList(SourceDataLine.class);
    }

    static KVPair[] getMixerList(Class<?> cl) {
        ArrayList<KVPair<String, String>> list = new ArrayList<KVPair<String, String>>();

        AudioFormat stereoFormat = new AudioFormat(44100f, 16, 2, true, false);
        AudioFormat monoFormat = new AudioFormat(44100f, 16, 1, true, false);

        ArrayList<AudioFormat> validFormats = new ArrayList<AudioFormat>();

        validFormats.add(new AudioFormat(44100f, 16, 1, true, false));
        validFormats.add(new AudioFormat(44100f, 16, 2, true, false));
        validFormats.add(new AudioFormat(44100f, 24, 1, true, false));
        validFormats.add(new AudioFormat(44100f, 24, 2, true, false));
        validFormats.add(new AudioFormat(48000f, 16, 1, true, false));
        validFormats.add(new AudioFormat(48000f, 16, 2, true, false));
        validFormats.add(new AudioFormat(48000f, 24, 1, true, false));
        validFormats.add(new AudioFormat(48000f, 24, 2, true, false));
        validFormats.add(new AudioFormat(96000f, 16, 1, true, false));
        validFormats.add(new AudioFormat(96000f, 16, 2, true, false));
        validFormats.add(new AudioFormat(96000f, 24, 1, true, false));
        validFormats.add(new AudioFormat(96000f, 24, 2, true, false));

        Mixer.Info[] info = AudioSystem.getMixerInfo();
        for (Mixer.Info i : info) {
            Mixer m = AudioSystem.getMixer(i);

            boolean supported = false;

            for (AudioFormat valid : validFormats) {
                try { 
                    m.getLine(new DataLine.Info(cl, valid));
                    supported = true; 
                } catch (Exception e) {
                }
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
        defaultPrefs.put("audio.recording.trim.blocksize", "4096");
        defaultPrefs.put("audio.playback.blocksize", "4096");

        defaultPrefs.put("catenation.pre-chapter", "1000");
        defaultPrefs.put("catenation.post-chapter", "1500");
        defaultPrefs.put("catenation.post-sentence", "1000");
        defaultPrefs.put("catenation.short-sentence", "100");
        defaultPrefs.put("catenation.post-paragraph", "2000");
        defaultPrefs.put("catenation.post-section", "3000");

        defaultPrefs.put("audio.recording.trim.fft", "10");
        defaultPrefs.put("audio.recording.variance", "10");
    
        defaultPrefs.put("path.storage", (new File(System.getProperty("user.home"), "Recordings")).toString());
        defaultPrefs.put("path.archive", (new File(new File(System.getProperty("user.home"), "Recordings"),"archive")).toString());
        defaultPrefs.put("path.ffmpeg", "");

        defaultPrefs.put("audio.export.bitrate", "256000");
        defaultPrefs.put("audio.export.channels", "2");
        defaultPrefs.put("audio.export.samplerate", "44100");
        defaultPrefs.put("process.sphinx", "false");
        defaultPrefs.put("process.command", "speech-to-text \"%f\"");
        defaultPrefs.put("process.threads", "10");

        defaultPrefs.put("editor.external", "");

        defaultPrefs.put("cache.size", "100");

        defaultPrefs.put("scripts.startup", "");

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
            Debug.d("Bad type for key", key);
        }
    }

    void storePreferences() {
        if (mixerList.getSelectedItem() != null) set("audio.recording.device", ((KVPair)mixerList.getSelectedItem()).key);
        if (channelList.getSelectedItem() != null) set("audio.recording.channels", ((KVPair)channelList.getSelectedItem()).key);
        if (rateList.getSelectedItem() != null) set("audio.recording.samplerate", ((KVPair)rateList.getSelectedItem()).key);
        if (bitDepth.getSelectedItem() != null) set("audio.recording.resolution", ((KVPair)bitDepth.getSelectedItem()).key);
        if (trimMethod.getSelectedItem() != null) set("audio.recording.trim", ((KVPair)trimMethod.getSelectedItem()).key);
        if (playbackList.getSelectedItem() != null) set("audio.playback.device", ((KVPair)playbackList.getSelectedItem()).key);
        set("path.storage", storageFolder.getText());
        set("path.archive", archiveFolder.getText());
        set("path.ffmpeg", ffmpegLocation.getText());
        set("catenation.pre-chapter", preChapterGap.getValue());
        set("catenation.post-chapter", postChapterGap.getValue());
        set("catenation.post-sentence", postSentenceGap.getValue());
        set("catenation.short-sentence", shortSentenceGap.getValue());
        set("catenation.post-paragraph", postParagraphGap.getValue());
        set("catenation.post-section", postSectionGap.getValue());
        if (bitRate.getSelectedItem() != null) set("audio.export.bitrate", ((KVPair)bitRate.getSelectedItem()).key);
        if (channels.getSelectedItem() != null) set("audio.export.channels", ((KVPair)channels.getSelectedItem()).key);
        if (exportRate.getSelectedItem() != null) set("audio.export.samplerate", ((KVPair)exportRate.getSelectedItem()).key);
        set("process.sphinx", enableParsing.isSelected());
        set("process.command", speechCommand.getText());
        set("process.threads", workerThreads.getValue());
        set("editor.external", externalEditor.getText());
        set("cache.size", cacheSize.getValue());
        set("audio.recording.trim.fft", fftThreshold.getValue());
        set("audio.recording.variance", maxGainVariance.getValue());
        if (fftBlockSize.getSelectedItem() != null) set("audio.recording.trim.blocksize", ((KVPair)fftBlockSize.getSelectedItem()).key);
        if (playbackBlockSize.getSelectedItem() != null) set("audio.playback.blocksize", ((KVPair)playbackBlockSize.getSelectedItem()).key);

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
        KVPair[] pairs = new KVPair[3];
        pairs[0] = new KVPair<String, String>("none", "None");
        pairs[1] = new KVPair<String, String>("peak", "Peak Amplitude");
        pairs[2] = new KVPair<String, String>("fft", "FFT Analysis");
        return pairs;
    }

    public static KVPair[] getFFTBlockSizes() {
        KVPair[] pairs = new KVPair[8];
        pairs[0] = new KVPair<String, String>("1024", "1024");
        pairs[1] = new KVPair<String, String>("2048", "2048");
        pairs[2] = new KVPair<String, String>("4096", "4096");
        pairs[3] = new KVPair<String, String>("8192", "8192");
        pairs[4] = new KVPair<String, String>("16384", "16384");
        pairs[5] = new KVPair<String, String>("32768", "32768");
        pairs[6] = new KVPair<String, String>("65536", "65537");
        pairs[7] = new KVPair<String, String>("131072", "131072");
        return pairs;
    }

    public static KVPair[] getPlaybackBlockSizes() {
        KVPair[] pairs = new KVPair[8];
        pairs[0] = new KVPair<String, String>("1024", "1024");
        pairs[1] = new KVPair<String, String>("2048", "2048");
        pairs[2] = new KVPair<String, String>("4096", "4096");
        pairs[3] = new KVPair<String, String>("8192", "8192");
        pairs[4] = new KVPair<String, String>("16384", "16384");
        pairs[5] = new KVPair<String, String>("32768", "32768");
        pairs[6] = new KVPair<String, String>("65536", "65537");
        pairs[7] = new KVPair<String, String>("131072", "131072");
        return pairs;
    }
}
