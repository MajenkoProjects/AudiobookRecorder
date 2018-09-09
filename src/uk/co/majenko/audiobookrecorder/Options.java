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

    GridBagConstraints constraint;

    JComboBox<KVPair> mixerList;
    JComboBox<KVPair> playbackList;
    JComboBox<KVPair> channelList;
    JComboBox<KVPair> rateList;
    JTextField storageFolder;
    JSpinner preChapterGap;
    JSpinner postChapterGap;
    JSpinner postSentenceGap;


    static HashMap<String, String> defaultPrefs;
    static Preferences prefs = null;

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

    JComboBox<KVPair> addDropdown(String label, KVPair[] options, String def) {
        JLabel l = new JLabel(label);
        constraint.gridx = 0;
        constraint.gridwidth = 1;
        constraint.gridheight = 1;
        constraint.anchor = GridBagConstraints.LINE_START;
        add(l, constraint);

        JComboBox<KVPair> o = new JComboBox<KVPair>(options);
        constraint.gridx = 1;
        add(o, constraint);

        for (KVPair p : options) {
            if (p.key.equals(def)) {
                o.setSelectedItem(p);
            }
        }

        constraint.gridy++;

        return o;
    }

    JTextField addFilePath(String label, String path) {
        JLabel l = new JLabel(label);
        constraint.gridx = 0;
        constraint.gridwidth = 1;
        constraint.gridheight = 1;
        constraint.anchor = GridBagConstraints.LINE_START;
        add(l, constraint);

        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        JTextField a = new JTextField(path);
        p.add(a, BorderLayout.CENTER);
        JButton b = new JButton("...");
        p.add(b, BorderLayout.EAST);

        constraint.gridx = 1;

        constraint.fill = GridBagConstraints.HORIZONTAL;
        add(p, constraint);

        constraint.fill = GridBagConstraints.NONE;

        constraint.gridy++;
        return a;
    }

    void addSeparator() {
        constraint.gridx = 0;
        constraint.gridwidth = 2;

        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());

        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setPreferredSize(new Dimension(1, 1));
        p.add(sep, BorderLayout.CENTER);

        constraint.fill = GridBagConstraints.HORIZONTAL;
        constraint.insets = new Insets(10, 2, 10, 2);
        add(p, constraint);
        constraint.insets = new Insets(2, 2, 2, 2);
        constraint.fill = GridBagConstraints.NONE;
        constraint.gridwidth = 1;
        constraint.gridy++;
    }

    JSpinner addSpinner(String label, int min, int max, int step, int value, String suffix) {
        JLabel l = new JLabel(label);
        constraint.gridx = 0;
        constraint.gridwidth = 1;
        constraint.gridheight = 1;
        constraint.anchor = GridBagConstraints.LINE_START;
        add(l, constraint);

        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        JSpinner a = new JSpinner(new SteppedNumericSpinnerModel(min, max, step, value));

        p.add(a, BorderLayout.CENTER);
        JLabel b = new JLabel(suffix);
        p.add(b, BorderLayout.EAST);

        constraint.gridx = 1;

        constraint.fill = GridBagConstraints.HORIZONTAL;
        add(p, constraint);

        constraint.fill = GridBagConstraints.NONE;

        constraint.gridy++;
        return a;

    }

    public Options(JFrame parent) {
        loadPreferences(); // Just in case. It should do nothing.

        setLayout(new GridBagLayout());

        constraint = new GridBagConstraints();

        constraint.gridx = 0;
        constraint.gridy = 0;
        constraint.gridwidth = 1;
        constraint.gridheight = 1;

        constraint.insets = new Insets(2, 2, 2, 2);

        addSeparator();

        mixerList = addDropdown("Recording device:", getMixerList(), get("audio.recording.device"));
        channelList = addDropdown("Channels:", getChannelCountList(), get("audio.recording.channels"));
        rateList = addDropdown("Sample rate:", getSampleRateList(), get("audio.recording.samplerate"));

        addSeparator();

        playbackList = addDropdown("Playback device:", getMixerList(), get("audio.playback.device"));
        addSeparator();
        storageFolder = addFilePath("Storage folder:", get("path.storage"));

        addSeparator();

        preChapterGap = addSpinner("Default pre-chapter gap:", 0, 5000, 100, getInteger("catenation.pre-chapter"), "ms");
        postChapterGap = addSpinner("Default post-chapter gap:", 0, 5000, 100, getInteger("catenation.post-chapter"), "ms");
        postSentenceGap = addSpinner("Default post-sentence gap:", 0, 5000, 100, getInteger("catenation.post-sentence"), "ms");

        addSeparator();




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

        add(box, constraint);
        
        pack();

        setLocationRelativeTo(parent);

        setVisible(true);
    }

    static KVPair[] getMixerList() {
        TreeSet<KVPair> list = new TreeSet<KVPair>();

        Mixer.Info[] info = AudioSystem.getMixerInfo();
        for (Mixer.Info i : info) {
            KVPair p = new KVPair(i.getName(), i.getDescription());
            list.add(p);
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

        KVPair[] mixers = getMixerList();

        defaultPrefs.put("audio.recording.device", mixers[0].key);
        defaultPrefs.put("audio.recording.channels", "2");
        defaultPrefs.put("audio.recording.samplerate", "48000");
        defaultPrefs.put("audio.playback.device", mixers[0].key);
        defaultPrefs.put("path.storage", (new File(System.getProperty("user.home"), "Recordings")).toString());

        defaultPrefs.put("catenation.pre-chapter", "2000");
        defaultPrefs.put("catenation.post-chapter", "2000");
        defaultPrefs.put("catenation.post-sentence", "1000");

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

    public static void set(String key, String value) {
        if (prefs == null) return;
        prefs.put(key, value);
    }

    void storePreferences() {
        set("audio.recording.device", ((KVPair)mixerList.getSelectedItem()).key);
        set("audio.recording.channels", ((KVPair)channelList.getSelectedItem()).key);
        set("audio.recording.samplerate", ((KVPair)rateList.getSelectedItem()).key);
        set("audio.playback.device", ((KVPair)playbackList.getSelectedItem()).key);
        set("path.storage", storageFolder.getText());
        set("catenation.pre-chapter", "" + preChapterGap.getValue());
        set("catenation.post-chapter", "" + postChapterGap.getValue());
        set("catenation.post-sentence", "" + postSentenceGap.getValue());

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
}
