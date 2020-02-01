package uk.co.majenko.audiobookrecorder;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import javax.swing.tree.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.prefs.*;
import java.io.*;
import it.sauronsoftware.jave.*;
import com.mpatric.mp3agic.*;
import java.nio.file.Files;
import java.util.zip.*;
import javax.swing.filechooser.*;
import javax.imageio.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import org.w3c.dom.Node;
import java.util.concurrent.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class AudiobookRecorder extends JFrame implements DocumentListener {

    // Settings - tweakable

    static Properties config = new Properties();
    TreeMap<String, EffectGroup> effects;

    public final static int IDLE = 0;
    public final static int RECORDING = 1;
    public final static int STOPPING = 2;
    public int state = IDLE;

    public static CommandLine CLI = new CommandLine();

    MainToolBar toolBar;

    JMenuBar menuBar;
    JMenu fileMenu;
    JMenu bookMenu;
    JMenu toolsMenu;
    JMenu helpMenu;

    JMenuItem fileNewBook;
    JMenuItem fileOpenBook;
    JMenuItem fileSave;
    JMenuItem fileExit;
    JMenuItem fileOpenArchive;

    JMenuItem bookNewChapter;
    JMenuItem bookExportAudio;
    JMenuItem bookPurgeBackups;
    JMenu bookVisitACX;
    JMenuItem bookVisitTitle;
    JMenuItem bookVisitAudition;
    JMenuItem bookVisitProduce;

    JMenuItem toolsMerge;
    JMenuItem toolsArchive;
    JMenuItem toolsCoverArt;
    JMenuItem toolsManuscript;
    JMenuItem toolsOptions;

    JMenuItem helpAbout;

    FlashPanel centralPanel;

    JPanel statusBar;

    JLabel statusLabel;

    JScrollPane mainScroll;

    Book book = null;

    JTree bookTree;
    public DefaultTreeModel bookTreeModel;

    Sentence recording = null;
    Sentence playing = null;
    Sentence roomNoise = null;
    Sentence selectedSentence = null;

    JPanel sampleControl;
    public Waveform sampleWaveform;
    JScrollBar sampleScroll;
    JSplitPane mainSplit;

    JTabbedPane notesTabs;

    JTextArea bookNotesArea;
    JScrollPane bookNotesScroll;

    JTextArea chapterNotesArea;
    JScrollPane chapterNotesScroll;

    JTextArea sentenceNotesArea;
    JScrollPane sentenceNotesScroll;

    JSpinner postSentenceGap;
    JSpinner gainPercent;
    JCheckBox locked;
    JCheckBox attention;
    JCheckBox rawAudio;

    JButtonSpacePlay reprocessAudioFFT;
    JButtonSpacePlay reprocessAudioPeak;
    JButtonSpacePlay normalizeAudio;
    JToggleButtonSpacePlay selectSplitMode;
    JToggleButtonSpacePlay selectCutMode;
    JButtonSpacePlay doCutSplit;

    JComboBox<KVPair<String,String>> effectChain;

    Thread playingThread = null;

    Random rng = new Random();

    SourceDataLine play = null;

    boolean effectsEnabled = true;

    public TargetDataLine microphone = null;
    public AudioInputStream microphoneStream = null;

    public static AudiobookRecorder window;

    public Queue<Runnable>speechProcessQueue = null;

    void buildToolbar(Container ob) {
        Debug.trace();
        toolBar = new MainToolBar(this);
        toolBar.addSeparator();
        toolBar.add(Box.createHorizontalGlue());
        ob.add(toolBar, BorderLayout.NORTH); 
    }

    void buildMenu(Container ob) {
        Debug.trace();
        menuBar = new JMenuBar();

        fileMenu = new JMenu("File");

        fileNewBook = new JMenuItem("New Book...");
        fileNewBook.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                saveBookStructure();
                createNewBook();
            }
        });
        fileOpenBook = new JMenuItem("Open Book...");
        fileOpenBook.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                saveBookStructure();
                openBook();
            }
        });
        fileSave = new JMenuItem("Save Book");
        fileSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                saveBookStructure();
            }
        });

        fileOpenArchive = new JMenuItem("Open Archive...");
        fileOpenArchive.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                openArchive();
            }
        });

        fileExit = new JMenuItem("Exit");
        fileExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                saveBookStructure();
                System.exit(0);
            }
        });

        fileMenu.add(fileNewBook);
        fileMenu.add(fileOpenBook);
        fileMenu.add(fileSave);
        fileMenu.addSeparator();
        fileMenu.add(fileOpenArchive);
        fileMenu.addSeparator();
        fileMenu.add(fileExit);

        menuBar.add(fileMenu);


        bookMenu = new JMenu("Book");
        
        bookNewChapter = new JMenuItem("New Chapter");
        bookNewChapter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                addChapter();
            }
        });

        bookExportAudio = new JMenuItem("Export Audio");
        bookExportAudio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                exportAudio();
            }
        });

        bookPurgeBackups = new JMenuItem("Purge Backups");
        bookPurgeBackups.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                book.purgeBackups();
            }
        });

        bookMenu.add(bookNewChapter);
        bookMenu.add(bookExportAudio);
        bookMenu.add(bookPurgeBackups);

        bookVisitACX = new JMenu("Visit ACX");
        bookMenu.add(bookVisitACX);

        bookVisitTitle = new JMenuItem("Title");
        bookVisitTitle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                Utils.browse("https://www.acx.com/titleview/" + book.getACX());
            }
        });
        bookVisitACX.add(bookVisitTitle);

        bookVisitAudition = new JMenuItem("Audition");
        bookVisitAudition.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                Utils.browse("https://www.acx.com/titleview/" + book.getACX() + "?bucket=AUDITION_READY");
            }
        });
        bookVisitACX.add(bookVisitAudition);

        bookVisitProduce = new JMenuItem("Produce");
        bookVisitProduce.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                Utils.browse("https://www.acx.com/titleview/" + book.getACX() + "?bucket=PRODUCE");
            }
        });
        bookVisitACX.add(bookVisitProduce);

        menuBar.add(bookMenu);

        toolsMenu = new JMenu("Tools");

        toolsMerge = new JMenuItem("Merge Book...");
        toolsMerge.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                saveBookStructure();
                mergeBook();
            }
        });
        
        toolsArchive = new JMenuItem("Archive Book");
        toolsArchive.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                saveBookStructure();
                archiveBook();
            }
        });

        toolsCoverArt = new JMenuItem("Import Cover Art...");
        toolsCoverArt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                loadCoverArt();
            }
        });
        
        toolsManuscript = new JMenuItem("Import Manuscript...");
        toolsManuscript.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                loadManuscript();
            }
        });
        
        toolsOptions = new JMenuItem("Options");
        toolsOptions.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                new Options(AudiobookRecorder.this);
            }
        });
        
        toolsMenu.add(toolsMerge);
        toolsMenu.add(toolsArchive);
        toolsMenu.add(toolsCoverArt);
        toolsMenu.add(toolsManuscript);
        toolsMenu.addSeparator();
        toolsMenu.add(toolsOptions);

        menuBar.add(toolsMenu);


        helpMenu = new JMenu("Help");
        helpAbout = new JMenuItem("About AudiobookRecorder");

        helpAbout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                JOptionPane.showMessageDialog(AudiobookRecorder.this, new AboutPanel(), "About AudiobookRecorder", JOptionPane.PLAIN_MESSAGE);
            }
        });

        helpMenu.add(helpAbout);
        menuBar.add(helpMenu);

        ob.add(menuBar, BorderLayout.NORTH);

        setPreferredSize(new Dimension(700, 500));
        setLocationRelativeTo(null);
    }

    public AudiobookRecorder(String[] args) {
        Debug.trace();

        window = this;

        CLI.addParameter("debug", "", Boolean.class, "Enable debug output");
        CLI.addParameter("trace", "", Boolean.class, "Enable function tracing");

        String[] argv = CLI.process(args);

        Debug.debugEnabled = CLI.isSet("debug");
        Debug.traceEnabled = CLI.isSet("trace");

        speechProcessQueue = new ArrayDeque<Runnable>();

        try {

            String clsname = "com.jtattoo.plaf.hifi.HiFiLookAndFeel";
            UIManager.setLookAndFeel(clsname);

            Properties p = new Properties();
            p.put("windowDecoration", "off");
            p.put("logoString", "Audiobook");
            p.put("textAntiAliasing", "on");

            Class<?> cls = Class.forName(clsname);
            Class[] cArg = new Class[1];
            cArg[0] = Properties.class;
            Method mth = cls.getMethod("setCurrentTheme", cArg);
            mth.invoke(cls, p);

        } catch (Exception e) {
            e.printStackTrace();
        }


        Options.loadPreferences();

        for (int i = 0; i < Options.getInteger("process.threads"); i++) {
            WorkerThread worker = new WorkerThread(speechProcessQueue);
            worker.start();
        }


        execScript(Options.get("scripts.startup"));

        CacheManager.setCacheSize(Options.getInteger("cache.size"));

        setLayout(new BorderLayout());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                Debug.trace();
                saveBookStructure();
                System.exit(0);
            }
        });

        buildMenu(this);

        centralPanel = new FlashPanel();
        centralPanel.setLayout(new BorderLayout());
        add(centralPanel, BorderLayout.CENTER);

        sampleControl = new JPanel();
        sampleControl.setLayout(new BorderLayout());
        sampleControl.setPreferredSize(new Dimension(400, 150));
        sampleWaveform = new Waveform();

        sampleScroll = new JScrollBar(JScrollBar.HORIZONTAL);
        sampleScroll.setMinimum(0);
        sampleScroll.setMaximum(1000);
        sampleScroll.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                Debug.trace();
                sampleWaveform.setOffset(sampleScroll.getValue());
            }
        });

        sampleWaveform.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (selectedSentence == null) return;
                if (selectedSentence.isLocked()) return;
                int val = ((int)gainPercent.getValue()) - e.getWheelRotation();
                if (val < 1) val = 1;
                gainPercent.setValue(val);
            }
        });

        sampleWaveform.addMarkerDragListener(new MarkerDragListener() {
            public void leftMarkerMoved(MarkerDragEvent e) {
                Debug.trace();
                if (selectedSentence != null) {
                    if (!selectedSentence.isLocked()) {
                        selectedSentence.setStartOffset(e.getPosition());
                        selectedSentence.updateCrossings();
                        sampleWaveform.setAltMarkers(selectedSentence.getStartCrossing(), selectedSentence.getEndCrossing());
                    } else {
                        sampleWaveform.setLeftMarker(selectedSentence.getStartOffset());
                    }
                }
            }

            public void rightMarkerMoved(MarkerDragEvent e) {
                Debug.trace();
                if (selectedSentence != null) {
                    if (!selectedSentence.isLocked()) {
                        selectedSentence.setEndOffset(e.getPosition());
                        selectedSentence.updateCrossings();
                        sampleWaveform.setAltMarkers(selectedSentence.getStartCrossing(), selectedSentence.getEndCrossing());
                    } else {
                        sampleWaveform.setRightMarker(selectedSentence.getEndOffset());
                    }
                }
            }
        });
    
        sampleControl.add(sampleWaveform, BorderLayout.CENTER);

        reprocessAudioFFT = new JButtonSpacePlay(Icons.fft, "Autotrim Audio (FFT)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                if (selectedSentence != null) {
                    selectedSentence.autoTrimSampleFFT();
                    sampleWaveform.setMarkers(selectedSentence.getStartOffset(), selectedSentence.getEndOffset());
                    sampleWaveform.setAltMarkers(selectedSentence.getStartCrossing(), selectedSentence.getEndCrossing());
                    postSentenceGap.setValue(selectedSentence.getPostGap());
                    gainPercent.setValue((int)(selectedSentence.getGain() * 100d));
                }
            }
        });

        reprocessAudioPeak = new JButtonSpacePlay(Icons.peak, "Autotrim Audio (Peak)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                if (selectedSentence != null) {
                    selectedSentence.autoTrimSamplePeak();
                    sampleWaveform.setMarkers(selectedSentence.getStartOffset(), selectedSentence.getEndOffset());
                    sampleWaveform.setAltMarkers(selectedSentence.getStartCrossing(), selectedSentence.getEndCrossing());
                    postSentenceGap.setValue(selectedSentence.getPostGap());
                    gainPercent.setValue((int)(selectedSentence.getGain() * 100d));
                }
            }
        });

        normalizeAudio = new JButtonSpacePlay(Icons.normalize, "Normalize audio", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                if (selectedSentence != null) {
                    selectedSentence.normalize();
                    gainPercent.setValue((int)(selectedSentence.getGain() * 100d));
                    updateWaveform(true);
                }
            }
        });
    
        selectSplitMode = new JToggleButtonSpacePlay(Icons.split, "Toggle split mode", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                toggleSplitMode();
            }
        });

        selectCutMode = new JToggleButtonSpacePlay(Icons.cut, "Toggle cut mode", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                toggleCutMode();
            }
        });

        doCutSplit = new JButtonSpacePlay(Icons.docut, "Perform cut or split", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                executeCutOrSplit();
            }
        });

        postSentenceGap = new JSpinner(new SteppedNumericSpinnerModel(0, 5000, 100, 0));
        postSentenceGap.setPreferredSize(new Dimension(50, 20));

        postSentenceGap.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Debug.trace();
                JSpinner ob = (JSpinner)e.getSource();
                if (selectedSentence != null) {
                    selectedSentence.setPostGap((Integer)ob.getValue());
                }
            }
        });

        gainPercent = new JSpinner(new SteppedNumericSpinnerModel(0, 500, 1, 100));
        gainPercent.setPreferredSize(new Dimension(50, 20));

        gainPercent.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Debug.trace();
                JSpinner ob = (JSpinner)e.getSource();
                if (selectedSentence != null) {
                    selectedSentence.setGain((Integer)ob.getValue() / 100d);
                    updateWaveform(true);
                }
            }
        });

        JToolBar controlsTop = new JToolBar(JToolBar.HORIZONTAL);
        JToolBar controlsLeft = new JToolBar(JToolBar.VERTICAL);
        JToolBar controlsRight = new JToolBar(JToolBar.VERTICAL);
        JToolBar controlsBottom = new JToolBar(JToolBar.HORIZONTAL);

        controlsTop.setFloatable(false);
        controlsLeft.setFloatable(false);
        controlsRight.setFloatable(false);
        controlsBottom.setFloatable(false);

        controlsLeft.add(reprocessAudioFFT);
        controlsLeft.add(reprocessAudioPeak);
        controlsLeft.add(normalizeAudio);

        rawAudio = new JCheckBox("Raw Audio");
        rawAudio.setFocusable(false);

        controlsTop.add(rawAudio);

        locked = new JCheckBox("Phrase locked");
        locked.setFocusable(false);

        locked.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                JCheckBox c = (JCheckBox)e.getSource();
                if (c.isSelected()) {
                    if (selectedSentence != null) {
                        selectedSentence.setLocked(true);
                    }
                } else {
                    if (selectedSentence != null) {
                        selectedSentence.setLocked(false);
                    }
                }
                postSentenceGap.setEnabled(!selectedSentence.isLocked());
                gainPercent.setEnabled(!selectedSentence.isLocked());
                reprocessAudioFFT.setEnabled(!selectedSentence.isLocked());
                reprocessAudioPeak.setEnabled(!selectedSentence.isLocked());
                selectCutMode.setEnabled(!selectedSentence.isLocked());
                selectSplitMode.setEnabled(!selectedSentence.isLocked());
                doCutSplit.setEnabled(false);
                selectCutMode.setSelected(false);
                selectSplitMode.setSelected(false);

                bookTreeModel.reload(selectedSentence);
            }
        });
        controlsTop.add(locked);

        attention = new JCheckBox("Flag for attention");
        attention.setFocusable(false);

        attention.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                JCheckBox c = (JCheckBox)e.getSource();
                if (c.isSelected()) {
                    if (selectedSentence != null) {
                        selectedSentence.setAttentionFlag(true);
                    }
                } else {
                    if (selectedSentence != null) {
                        selectedSentence.setAttentionFlag(false);
                    }
                }
                bookTreeModel.reload(selectedSentence);
            }
        });

        controlsTop.add(attention);

        controlsTop.add(Box.createHorizontalGlue());
        controlsTop.add(new JLabel(" Post gap:"));
        controlsTop.add(postSentenceGap);
        controlsTop.add(new JLabel("ms "));

        controlsTop.add(new JLabel(" Gain:"));
        controlsTop.add(gainPercent);
        controlsTop.add(new JLabel("%"));

        controlsTop.add(Box.createHorizontalGlue());


        JButtonSpacePlay zoomIn = new JButtonSpacePlay(Icons.zoomIn, "Zoom In", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                sampleWaveform.increaseZoom();
            }
        });
        controlsRight.add(zoomIn);

        JButtonSpacePlay zoomOut = new JButtonSpacePlay(Icons.zoomOut, "Zoom Out", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                sampleWaveform.decreaseZoom();
            }
        });
        controlsRight.add(zoomOut);

        controlsBottom.add(new JLabel("Effects Chain: "));

        effectChain = new JComboBox<KVPair<String, String>>();
        effectChain.setFocusable(false);
        controlsBottom.add(effectChain);

        controlsBottom.add(sampleScroll);

        effectChain.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                Debug.d(e);
                if (selectedSentence != null) {
                    int i = effectChain.getSelectedIndex();
                    KVPair<String, String> p = effectChain.getItemAt(i);
                    if (p == null) return;
                    selectedSentence.setEffectChain(p.getKey());
                    updateWaveform(true);
                }
            }
        });

        controlsBottom.add(selectSplitMode);
        controlsBottom.add(selectCutMode);
        controlsBottom.add(doCutSplit);

        sampleControl.add(controlsTop, BorderLayout.NORTH);
        sampleControl.add(controlsLeft, BorderLayout.WEST);
        sampleControl.add(controlsRight, BorderLayout.EAST);
        sampleControl.add(controlsBottom, BorderLayout.SOUTH);

        centralPanel.add(sampleControl, BorderLayout.SOUTH);

        statusBar = new JPanel();
        add(statusBar, BorderLayout.SOUTH);

        statusLabel = new JLabel("Noise floor: " + getNoiseFloorDB() + "dB");
        statusBar.add(statusLabel);

        buildToolbar(centralPanel);


        centralPanel.getActionMap().put("startRecord", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                if (!getLock()) return;
                if (getFocusOwner() == bookNotesArea) { freeLock(); return; }
                if (getFocusOwner() == chapterNotesArea) { freeLock(); return; }
                if (getFocusOwner() == sentenceNotesArea) { freeLock(); return; }
                if (bookTree.isEditing()) {
                    freeLock();
                    return;
                }
                if (getNoiseFloor() == 0) {
                    freeLock();
                    alertNoRoomNoise();
                    return;
                }
                startRecording();
            }
        });
        centralPanel.getActionMap().put("startRecordShort", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                if (!getLock()) return;
                if (getFocusOwner() == bookNotesArea) { freeLock(); return; }
                if (getFocusOwner() == chapterNotesArea) { freeLock(); return; }
                if (getFocusOwner() == sentenceNotesArea) { freeLock(); return; }
                if (bookTree.isEditing()) {
                    freeLock();
                    return;
                }
                if (getNoiseFloor() == 0) {
                    freeLock();
                    alertNoRoomNoise();
                    return;
                }
                startRecordingShort();
            }
        });
        centralPanel.getActionMap().put("startRecordNewPara", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                if (!getLock()) return;
                if (getFocusOwner() == bookNotesArea) { freeLock(); return; }
                if (getFocusOwner() == chapterNotesArea) { freeLock(); return; }
                if (getFocusOwner() == sentenceNotesArea) { freeLock(); return; }
                if (bookTree.isEditing()) {
                    freeLock();
                    return;
                }
                if (getNoiseFloor() == 0) {
                    alertNoRoomNoise();
                    freeLock();
                    return;
                }
                startRecordingNewParagraph();
            }
        });
        centralPanel.getActionMap().put("startRecordNewSection", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                if (!getLock()) return;
                if (getFocusOwner() == bookNotesArea) { freeLock(); return; }
                if (getFocusOwner() == chapterNotesArea) { freeLock(); return; }
                if (getFocusOwner() == sentenceNotesArea) { freeLock(); return; }
                if (bookTree.isEditing()) {
                    freeLock();
                    return;
                }
                if (getNoiseFloor() == 0) {
                    freeLock();
                    alertNoRoomNoise();
                    return;
                }
                startRecordingNewSection();
            }
        });
        centralPanel.getActionMap().put("startRerecord", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                if (!getLock()) return;
                if (getFocusOwner() == bookNotesArea) { freeLock(); return; }
                if (getFocusOwner() == chapterNotesArea) { freeLock(); return; }
                if (getFocusOwner() == sentenceNotesArea) { freeLock(); return; }
                if (bookTree.isEditing()) {
                    freeLock();
                    return;
                }
                if (getNoiseFloor() == 0) {
                    freeLock();
                    alertNoRoomNoise();
                    return;
                }
                startReRecording();
            }
        });
        centralPanel.getActionMap().put("stopRecord", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                if (getFocusOwner() == bookNotesArea) { return; }
                if (getFocusOwner() == chapterNotesArea) { return; }
                if (getFocusOwner() == sentenceNotesArea) { return; }
                if (bookTree.isEditing()) return;
                stopLock();
                stopRecording();
                freeLock();
            }
        });
        centralPanel.getActionMap().put("deleteLast", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                if (getFocusOwner() == bookNotesArea) { return; }
                if (getFocusOwner() == chapterNotesArea) { return; }
                if (getFocusOwner() == sentenceNotesArea) { return; }
                if (bookTree.isEditing()) return;
                deleteLastRecording();
            }
        });

        centralPanel.getActionMap().put("startStopPlayback", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                if (getFocusOwner() == bookNotesArea) { return; }
                if (getFocusOwner() == chapterNotesArea) { return; }
                if (getFocusOwner() == sentenceNotesArea) { return; }
                if (bookTree.isEditing()) return;
                if (playing == null) {
                    playSelectedSentence();
                } else {
                    stopPlaying();
                }
            }
        });

        centralPanel.getActionMap().put("startPlaybackFrom", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                if (getFocusOwner() == bookNotesArea) { return; }
                if (getFocusOwner() == chapterNotesArea) { return; }
                if (getFocusOwner() == sentenceNotesArea) { return; }
                if (bookTree.isEditing()) return;
                if (playing == null) {
                    playFromSelectedSentence();
                }
            }
        });

        mainScroll = new JScrollPane();

        bookNotesArea = new JTextArea();
        bookNotesArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        bookNotesScroll = new JScrollPane();
        bookNotesScroll.setViewportView(bookNotesArea);

        chapterNotesArea = new JTextArea();
        chapterNotesArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        chapterNotesArea.getDocument().addDocumentListener(this);
        chapterNotesScroll = new JScrollPane();
        chapterNotesScroll.setViewportView(chapterNotesArea);

        sentenceNotesArea = new JTextArea();
        sentenceNotesArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        sentenceNotesArea.getDocument().addDocumentListener(this);
        sentenceNotesScroll = new JScrollPane();
        sentenceNotesScroll.setViewportView(sentenceNotesArea);

        notesTabs = new JTabbedPane();

        notesTabs.add("Book", bookNotesScroll);
        notesTabs.add("Chapter", chapterNotesScroll);
        notesTabs.add("Phrase", sentenceNotesScroll);

        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainScroll, notesTabs);
        centralPanel.add(mainSplit, BorderLayout.CENTER);

        mainSplit.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
                Debug.trace();
                if (ev.getPropertyName().equals("dividerLocation")) {
                    if ((bookTreeModel != null) && (book != null)) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                Debug.trace();
                                bookTreeModel.reload(book);
                            }
                        });
                    }
                }
            }
        });


        setTitle("AudioBook Recorder");

        setIconImage(Icons.appIcon.getImage());

        bindKeys(centralPanel);

        mainSplit.setResizeWeight(0.8d);

        pack();
        setVisible(true);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Debug.trace();
                mainSplit.setDividerLocation(0.8d);
            }
        });

        String lastBook = Options.get("path.last-book");

        if (lastBook != null && !lastBook.equals("")) {
            File f = new File(Options.get("path.storage"), lastBook);
            if (f.exists() && f.isDirectory()) {
                File y = new File(f, "audiobook.abx");
                if (y.exists()) {
                    loadXMLBookStructure(y);
                } else {
                    File x = new File(f, "audiobook.abk");
                    if (x.exists()) {
                        loadBookStructure(x);
                    }
                }
            }
        }

        if (!Options.getBoolean("interface.donations.hide")) {
            int numruns = Options.getInteger("interface.donations.count");
            numruns++;
            if (numruns == 10) {
                numruns = 0;
                JOptionPane.showMessageDialog(this, new DonationPanel(), "Thank you", JOptionPane.PLAIN_MESSAGE);
            }
            Options.set("interface.donations.count", numruns);
            Options.savePreferences();
        }

    }

    void bindKeys(JComponent component) {
        Debug.trace();
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F"), "startRecordShort");
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released F"), "stopRecord");

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("R"), "startRecord");
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released R"), "stopRecord");

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("T"), "startRecordNewPara");
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released T"), "stopRecord");

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("Y"), "startRecordNewSection");
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released Y"), "stopRecord");

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released D"), "deleteLast");

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "startStopPlayback");
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.SHIFT_DOWN_MASK), "startPlaybackFrom");

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("E"), "startRerecord");
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released E"), "stopRecord");
    }

    void unbindKeys(JComponent component) {
        Debug.trace();
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F"), "ignore");
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released F"), "ignore");

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("R"), "ignore");
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released R"), "ignore");

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("T"), "ignore");
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released T"), "ignore");

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("Y"), "ignore");
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released Y"), "ignore");

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released D"), "ignore");

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "ignore");
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.SHIFT_DOWN_MASK), "ignore");

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("E"), "ignore");
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released E"), "ignore");
    }

    public static void main(String args[]) {
        Debug.trace();
        try {
            config.load(AudiobookRecorder.class.getResourceAsStream("config.txt"));
        } catch (Exception e) {
            e.printStackTrace();
        }


        new AudiobookRecorder(args);
    }

    public void createNewBook() {
        Debug.trace();
        BookInfoPanel info = new BookInfoPanel("", "", "", "", "");
        int r = JOptionPane.showConfirmDialog(this, info, "New Book", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;

        String name = info.getTitle();

        File bookdir = new File(Options.get("path.storage"), name);
        if (bookdir.exists()) {
            JOptionPane.showMessageDialog(this, "File already exists.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Properties prefs = new Properties();
            Book newbook = new Book(prefs, info.getTitle().trim());
            newbook.setAuthor(info.getAuthor().trim());
            newbook.setGenre(info.getGenre().trim());
            newbook.setComment(info.getComment().trim());
            newbook.setACX(info.getACX().trim());

            Chapter caud = new Chapter(UUID.randomUUID().toString(), "Audition");
            Chapter copen = new Chapter(UUID.randomUUID().toString(), "Opening Credits");
            Chapter cclose = new Chapter(UUID.randomUUID().toString(), "Closing Credits");
            Chapter cone = new Chapter(UUID.randomUUID().toString(), "Chapter 1");

            newbook.add(caud);
            newbook.add(copen);
            newbook.add(cclose);

            newbook.add(cone);

            File bookRoot = new File(Options.get("path.storage"), newbook.getName());
            if (!bookRoot.exists()) {
                bookRoot.mkdirs();
            }

            File xml = new File(bookRoot, "audiobook.abx");
            Document doc = newbook.buildDocument();

            File backup = new File(bookRoot, "audiobook.bak");
            if (xml.exists()) {
                xml.renameTo(backup);
            }

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(xml);
            transformer.transform(source, result);

            loadXMLBookStructure(xml);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    class JMenuObject extends JMenuItem {
        Object ob;
        
        public JMenuObject(String p) {  
            super(p);
            Debug.trace();
            ob = null;
        }

        public JMenuObject(String p, Object o, ActionListener l) {  
            super(p);
            Debug.trace();
            ob = o;
            addActionListener(l);
        }

        public void setObject(Object o) {
            Debug.trace();
            ob = o;
        }

        public Object getObject() {
            Debug.trace();
            return ob;
        }
    }

    class JMenuObject2 extends JMenuItem {
        Object ob1;
        Object ob2;
        
        public JMenuObject2(String p) {  
            super(p);
            Debug.trace();
            ob1 = null;
            ob2 = null;
        }

        public JMenuObject2(String p, Object o1, Object o2, ActionListener l) {  
            super(p);
            Debug.trace();
            ob1 = o1;
            ob2 = o2;
            addActionListener(l);
        }

        public void setObject1(Object o) {
            Debug.trace();
            ob1 = o;
        }

        public void setObject2(Object o) {
            Debug.trace();
            ob2 = o;
        }

        public Object getObject1() {
            Debug.trace();
            return ob1;
        }

        public Object getObject2() {
            Debug.trace();
            return ob2;
        }
    }

    class BatchConversionThread implements Runnable {
        Chapter chapter;

        public BatchConversionThread(Chapter c) {
            Debug.trace();
            chapter = c;
        }
        public void run() {
            Debug.trace();
            try {
                for (Enumeration s = chapter.children(); s.hasMoreElements();) {
                    Sentence snt = (Sentence)s.nextElement();
                    if (!snt.isLocked()) {
                        if (snt.getId().equals(snt.getText())) {
                            Runnable r = snt.getRecognitionRunnable();
                            snt.setQueued();
                            speechProcessQueue.add(r);
                            speechProcessQueue.notify();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @SuppressWarnings("unchecked")
    void treePopup(MouseEvent e) {
        Debug.trace();
        int selRow = bookTree.getRowForLocation(e.getX(), e.getY());
        TreePath selPath = bookTree.getPathForLocation(e.getX(), e.getY());
        if (selRow != -1) {

            DefaultMutableTreeNode node = (DefaultMutableTreeNode)selPath.getLastPathComponent();

            if (node instanceof Sentence) {
                Sentence s = (Sentence)node;

                bookTree.setSelectionPath(new TreePath(s.getPath()));

                JPopupMenu menu = new JPopupMenu();

                JMenuObject rec = new JMenuObject("Recognise text from audio", s, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Sentence s = (Sentence)o.getObject();
                        if (!s.isLocked()) {
                            s.recognise();
                        }
                    }
                });



                JMenu moveMenu = new JMenu("Move phrase to...");


                for (Enumeration c = book.children(); c.hasMoreElements();) {
                    Chapter chp = (Chapter)c.nextElement();
                    JMenuObject2 m = new JMenuObject2(chp.getName(), s, chp, new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            Debug.trace();
                            JMenuObject2 ob = (JMenuObject2)e.getSource();
                            Sentence sentence = (Sentence)ob.getObject1();
                            Chapter target = (Chapter)ob.getObject2();

                            bookTreeModel.removeNodeFromParent(sentence);
                            bookTreeModel.insertNodeInto(sentence, target, target.getChildCount());
                        }
                    });
                    moveMenu.add(m);
                }

                JMenuObject moveUp = new JMenuObject("Move Up", s, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Sentence sent = (Sentence)o.getObject();
                        Chapter chap = (Chapter)sent.getParent();
                        int pos = bookTreeModel.getIndexOfChild(chap, sent);
                        if (pos > 0) pos--;
                        bookTreeModel.removeNodeFromParent(sent);
                        bookTreeModel.insertNodeInto(sent, chap, pos);
                    }
                });

                JMenuObject moveDown = new JMenuObject("Move Down", s, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Sentence sent = (Sentence)o.getObject();
                        Chapter chap = (Chapter)sent.getParent();
                        int pos = bookTreeModel.getIndexOfChild(chap, sent);
                        if (pos < chap.getChildCount() - 1) pos++;
                        bookTreeModel.removeNodeFromParent(sent);
                        bookTreeModel.insertNodeInto(sent, chap, pos);
                    }
                });

                JMenu setGapType = new JMenu("Post-gap Type...");

                String sentenceText = "  Sentence";
                String continuationText = "  Continuation";
                String paragraphText = "  Paragraph";
                String sectionText = "  Section";

                if (s.getPostGapType().equals("sentence")) {
                    sentenceText = "\u2713 Sentence";
                } else if (s.getPostGapType().equals("continuation")) {
                    continuationText = "\u2713 Continuation";
                } else if (s.getPostGapType().equals("paragraph")) {
                    paragraphText = "\u2713 Paragraph";
                } else if (s.getPostGapType().equals("section")) {
                    sectionText = "\u2713 Section";
                }

                JMenuObject2 gapTypeSentence = new JMenuObject2(sentenceText, s, "sentence", new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject2 o = (JMenuObject2)e.getSource();
                        Sentence sent = (Sentence)o.getObject1();
                        String type = (String)o.getObject2();
                        sent.setPostGapType(type);
                        sent.setPostGap(Options.getInteger("catenation.post-sentence"));
                        bookTreeModel.reload(sent);
                    }
                });
                setGapType.add(gapTypeSentence);
                
                JMenuObject2 gapTypeContinuation = new JMenuObject2(continuationText, s, "continuation", new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject2 o = (JMenuObject2)e.getSource();
                        Sentence sent = (Sentence)o.getObject1();
                        String type = (String)o.getObject2();
                        sent.setPostGapType(type);
                        sent.setPostGap(Options.getInteger("catenation.short-sentence"));
                        bookTreeModel.reload(sent);
                    }
                });
                setGapType.add(gapTypeContinuation);

                JMenuObject2 gapTypeParagraph = new JMenuObject2(paragraphText, s, "paragraph", new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject2 o = (JMenuObject2)e.getSource();
                        Sentence sent = (Sentence)o.getObject1();
                        String type = (String)o.getObject2();
                        sent.setPostGapType(type);
                        sent.setPostGap(Options.getInteger("catenation.post-paragraph"));
                        bookTreeModel.reload(sent);
                    }
                });
                setGapType.add(gapTypeParagraph);
                
                JMenuObject2 gapTypeSection = new JMenuObject2(sectionText, s, "section", new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject2 o = (JMenuObject2)e.getSource();
                        Sentence sent = (Sentence)o.getObject1();
                        String type = (String)o.getObject2();
                        sent.setPostGapType(type);
                        sent.setPostGap(Options.getInteger("catenation.post-section"));
                        bookTreeModel.reload(sent);
                    }
                });
                setGapType.add(gapTypeSection);
                


                JMenuObject ins = new JMenuObject("Insert phrase above", s, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Sentence s = (Sentence)o.getObject();
                        Chapter c = (Chapter)s.getParent();
                        Sentence newSentence = new Sentence();
                        int where = bookTreeModel.getIndexOfChild(c, s);
                        bookTreeModel.insertNodeInto(newSentence, c, where);
                    }
                        
                });

                JMenuObject del = new JMenuObject("Delete phrase", s, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Sentence s = (Sentence)o.getObject();
                        if (!s.isLocked()) {
                            s.deleteFiles();
                            bookTreeModel.removeNodeFromParent(s);
                        }
                    }
                });

                JMenuObject dup = new JMenuObject("Duplicate phrase", s, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        try {
                            JMenuObject o = (JMenuObject)e.getSource();
                            Sentence s = (Sentence)o.getObject();
                            Sentence newSentence = s.cloneSentence();
                            Chapter c = (Chapter)s.getParent();
                            int idx = bookTreeModel.getIndexOfChild(c, s);
                            bookTreeModel.insertNodeInto(newSentence, c, idx);
                            bookTree.setSelectionPath(new TreePath(newSentence.getPath()));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });

                JMenuObject edit = new JMenuObject("Open in external editor", s, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        try {
                            JMenuObject o = (JMenuObject)e.getSource();
                            Sentence s = (Sentence)o.getObject();
                            s.openInExternalEditor();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });

                JMenu external = new JMenu("Run external processor");

                for (int i = 0; i < 999; i++) {
                    String name = Options.get("editor.processor." + i + ".name");
                    if (name == null) break;
                    if (name.equals("")) break;
                    JMenuObject ob = new JMenuObject(name, s, new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            Debug.trace();
                            JMenuObject o = (JMenuObject)e.getSource();
                            Sentence s = (Sentence)o.getObject();
                            s.runExternalProcessor(Utils.s2i(o.getActionCommand()));
                        }
                    });
                    ob.setActionCommand(Integer.toString(i));
                    external.add(ob);
                }

                JMenuObject undo = new JMenuObject("Undo", s, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Sentence s = (Sentence)o.getObject();
                        s.undo();
                    }
                });

                menu.add(undo);
                menu.addSeparator();
                menu.add(rec);
                menu.addSeparator();
                menu.add(moveUp);
                menu.add(moveDown);
                menu.add(moveMenu);
                menu.add(setGapType);
                menu.addSeparator();
                menu.add(edit);
                menu.add(external);
                menu.add(ins);
                menu.add(del);
                menu.addSeparator();
                menu.add(dup);
                menu.show(bookTree, e.getX(), e.getY());
            } else if (node instanceof Chapter) {
                Chapter c = (Chapter)node;
                int idNumber = Utils.s2i(c.getId());

                bookTree.setSelectionPath(new TreePath(c.getPath()));

                JPopupMenu menu = new JPopupMenu();

                JMenuObject undo = new JMenuObject("Undo all", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter c = (Chapter)o.getObject();
                        for (Enumeration s = c.children(); s.hasMoreElements();) {
                            Sentence snt = (Sentence)s.nextElement();
                            snt.undo();
                        }
                    }
                });

                JMenuObject peaknew = new JMenuObject("Auto-trim new (Peak)", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter chap = (Chapter)o.getObject();

                        ProgressDialog ed = new ProgressDialog("Auto-trimming " + chap.getName());

                        AutoTrimThread t = new AutoTrimThread(chap, ed, AutoTrimThread.Peak, AutoTrimThread.NewOnly);
                        Thread nt = new Thread(t);
                        nt.start();
                        ed.setVisible(true);
                    }
                });

                JMenuObject fftnew = new JMenuObject("Auto-trim new (FFT)", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter chap = (Chapter)o.getObject();

                        ProgressDialog ed = new ProgressDialog("Auto-trimming " + chap.getName());

                        AutoTrimThread t = new AutoTrimThread(chap, ed, AutoTrimThread.FFT, AutoTrimThread.NewOnly);
                        Thread nt = new Thread(t);
                        nt.start();
                        ed.setVisible(true);
                    }
                });

                JMenuObject peak = new JMenuObject("Auto-trim all (Peak)", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter chap = (Chapter)o.getObject();

                        ProgressDialog ed = new ProgressDialog("Auto-trimming " + chap.getName());

                        AutoTrimThread t = new AutoTrimThread(chap, ed, AutoTrimThread.Peak, AutoTrimThread.All);
                        Thread nt = new Thread(t);
                        nt.start();
                        ed.setVisible(true);
                    }
                });

                JMenuObject fft = new JMenuObject("Auto-trim all (FFT)", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter chap = (Chapter)o.getObject();

                        ProgressDialog ed = new ProgressDialog("Auto-trimming " + chap.getName());

                        AutoTrimThread t = new AutoTrimThread(chap, ed, AutoTrimThread.FFT, AutoTrimThread.All);
                        Thread nt = new Thread(t);
                        nt.start();
                        ed.setVisible(true);
                    }
                });

                JMenuObject resetChapterGaps = new JMenuObject("Reset post gaps", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter c = (Chapter)o.getObject();
                        c.resetPostGaps();
                    }
                });

                JMenuObject moveUp = new JMenuObject("Move Up", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter chap = (Chapter)o.getObject();
                        int pos = bookTreeModel.getIndexOfChild(book, chap);
                        if (pos > 0) pos--;
                        Chapter prevChap = (Chapter)bookTreeModel.getChild(book, pos);
                        bookTreeModel.removeNodeFromParent(chap);
                        bookTreeModel.insertNodeInto(chap, book, pos);
                    }
                });

                JMenuObject moveDown = new JMenuObject("Move Down", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter chap = (Chapter)o.getObject();
                        int pos = bookTreeModel.getIndexOfChild(book, chap);
                        pos++;
                        Chapter nextChap = (Chapter)bookTreeModel.getChild(book, pos);
                        if (nextChap != null) {
                            bookTreeModel.removeNodeFromParent(chap);
                            bookTreeModel.insertNodeInto(chap, book, pos);
                        }
                    }
                });

                JMenu mergeWith = new JMenu("Merge chapter with");
                for (Enumeration bc = book.children(); bc.hasMoreElements();) {
                    Chapter chp = (Chapter)bc.nextElement();
                    if (chp.getId().equals(c.getId())) {
                        continue;
                    }
                    JMenuObject2 m = new JMenuObject2(chp.getName(), c, chp, new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            Debug.trace();
                            JMenuObject2 ob = (JMenuObject2)e.getSource();
                            Chapter source = (Chapter)ob.getObject1();
                            Chapter target = (Chapter)ob.getObject2();

                            DefaultMutableTreeNode n = source.getFirstLeaf();
                            while (n instanceof Sentence) {
                                bookTreeModel.removeNodeFromParent(n);
                                bookTreeModel.insertNodeInto(n, target, target.getChildCount());
                                n = source.getFirstLeaf();
                            }
                        }
                    });
                    mergeWith.add(m);
                }

                JMenuObject lockAll = new JMenuObject("Lock all phrases", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter c = (Chapter)o.getObject();
                        for (Enumeration s = c.children(); s.hasMoreElements();) {
                            Sentence snt = (Sentence)s.nextElement();
                            snt.setLocked(true);
                            bookTreeModel.reload(snt);
                        }
                    }
                });

                JMenuObject unlockAll = new JMenuObject("Unlock all phrases", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter c = (Chapter)o.getObject();
                        for (Enumeration s = c.children(); s.hasMoreElements();) {
                            Sentence snt = (Sentence)s.nextElement();
                            snt.setLocked(false);
                            bookTreeModel.reload(snt);
                        }
                    }
                });

                JMenuObject exportChapter = new JMenuObject("Export chapter", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter chap = (Chapter)o.getObject();

                        ProgressDialog ed = new ProgressDialog("Exporting " + chap.getName());

                        ExportThread t = new ExportThread(chap, ed);
                        Thread nt = new Thread(t);
                        nt.start();
                        ed.setVisible(true);
                    }
                });

                JMenuObject deleteChapter = new JMenuObject("Delete chapter", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter c = (Chapter)o.getObject();
                        int rv = JOptionPane.showConfirmDialog(AudiobookRecorder.this, "Are you sure you want to delete this chapter?", "Are you sure?", JOptionPane.OK_CANCEL_OPTION);
                        if (rv == JOptionPane.OK_OPTION) {
                            while (c.getChildCount() > 0) {
                                Sentence s = (Sentence)c.getFirstChild();
                                s.deleteFiles();
                                bookTreeModel.removeNodeFromParent(s);
                            }
                        }
                        bookTreeModel.removeNodeFromParent(c);
                        book.renumberChapters();
                    }
                });
                        
                JMenuObject convertAll = new JMenuObject("Detect all text", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter c = (Chapter)o.getObject();

                        for (Enumeration s = c.children(); s.hasMoreElements();) {
                            Sentence snt = (Sentence)s.nextElement();
                            if (!snt.isLocked()) {
                                if (!snt.beenDetected()) {
                                    Debug.d("Queueing recognition of", snt.getId());
                                    synchronized(speechProcessQueue) {
                                        Runnable r = snt.getRecognitionRunnable();
                                        snt.setQueued();
                                        speechProcessQueue.add(r);
                                        speechProcessQueue.notify();
                                    }
                                }
                            }
                        }
                    }
                });

                JMenuObject normalizeAll = new JMenuObject("Normalize chapter", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();

                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter chap = (Chapter)o.getObject();

                        ProgressDialog ed = new ProgressDialog("Normalizing " + chap.getName());

                        NormalizeThread t = new NormalizeThread(chap, ed);
                        Thread nt = new Thread(t);
                        nt.start();
                        ed.setVisible(true);
                    }
                });

                JMenu external = new JMenu("Run external processor");

                for (int i = 0; i < 999; i++) {
                    String name = Options.get("editor.processor." + i + ".name");
                    if (name == null) break;
                    if (name.equals("")) break;
                    JMenuObject ob = new JMenuObject(name, c, new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            Debug.trace();
                            JMenuObject o = (JMenuObject)e.getSource();
                            Chapter c = (Chapter)o.getObject();
                            for (Enumeration s = c.children(); s.hasMoreElements();) {
                                Sentence snt = (Sentence)s.nextElement();
                                if (!snt.isLocked()) {
                                    snt.runExternalProcessor(Utils.s2i(o.getActionCommand()));
                                }
                            }
                        }
                    });
                    ob.setActionCommand(Integer.toString(i));
                    external.add(ob);
                }


                menu.add(convertAll);
                menu.add(normalizeAll);
                menu.add(resetChapterGaps);
                menu.addSeparator();
                menu.add(moveUp);
                menu.add(moveDown);
                menu.addSeparator();
                menu.add(mergeWith);
                menu.addSeparator();
                menu.add(peaknew);
                menu.add(fftnew);
                menu.add(peak);
                menu.add(fft);
                menu.addSeparator();
                menu.add(lockAll);
                menu.add(unlockAll);
                menu.addSeparator();
                menu.add(exportChapter);
                menu.addSeparator();
                menu.add(deleteChapter);
                menu.addSeparator();
                menu.add(external);

                menu.show(bookTree, e.getX(), e.getY());
            } else if (node instanceof Book) {
                JPopupMenu menu = new JPopupMenu();

                JMenuItem editData = new JMenuItem("Edit Data...");
                editData.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JTabbedPane tabs = new JTabbedPane();
                        BookInfoPanel info = new BookInfoPanel(
                            book.getName(),
                            book.getAuthor(),
                            book.getGenre(),
                            book.getComment(),
                            book.getACX()
                        );
                        tabs.add("Data", info);

                        JPanel effectsPanel = new JPanel();
                        effectsPanel.setLayout(new GridBagLayout());
                        GridBagConstraints c = new GridBagConstraints();
                        c.gridx = 0;
                        c.gridy = 0;

                        effectsPanel.add(new JLabel("Default Effect:"), c);
                        c.gridx = 1;

                        JComboBox<KVPair<String, String>> defEff = new JComboBox<KVPair<String, String>>();
                        int selEff = -1;
                        int i = 0;
                        if (effects != null) {
                            for (String k : effects.keySet()) {
                                if (k.equals(book.getDefaultEffect())) {
                                    selEff = i;
                                }
                                KVPair<String, String> p = new KVPair<String, String>(k, effects.get(k).toString());
                                defEff.addItem(p);
                                i++;
                            }
                        }

                        defEff.setSelectedIndex(selEff);
                        
                        effectsPanel.add(defEff, c);

                        tabs.add("Effects", effectsPanel);

                        int r = JOptionPane.showConfirmDialog(AudiobookRecorder.this, tabs, "Edit Book", JOptionPane.OK_CANCEL_OPTION);
                        if (r != JOptionPane.OK_OPTION) return;

                        String tit = info.getTitle();
                        String aut = info.getAuthor();
                        String gen = info.getGenre();
                        String com = info.getComment();
                        String acx = info.getACX();

                        i = defEff.getSelectedIndex();
                        KVPair<String, String> de = defEff.getItemAt(i);
                        book.setDefaultEffect(de.getKey());

                        book.setAuthor(aut);
                        book.setGenre(gen);
                        book.setComment(com);
                        book.setACX(acx);
                        if (!(book.getName().equals(tit))) {
                            book.renameBook(tit);
                        }

                        CacheManager.purgeCache();
                    }
                });
                menu.add(editData);

                JMenuObject resetBookGaps = new JMenuObject("Reset all post gaps", book, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        for (Enumeration ch = book.children(); ch.hasMoreElements();) {
                            Chapter chap = (Chapter)ch.nextElement();
                            chap.resetPostGaps();
                        }
                    }
                });

                menu.add(resetBookGaps);

                menu.show(bookTree, e.getX(), e.getY());
            }

        }
    }

    public void startReRecording() {
        Debug.trace();

        if (recording != null) return;
        if (book == null) return;

        if (microphone == null) {
            JOptionPane.showMessageDialog(this, "Microphone not started. Start microphone first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();

        if (selectedNode == null) {
            return;
        }

        if (!(selectedNode instanceof Sentence)) {
            return;
        }

        Sentence s = (Sentence)selectedNode;

        if (s.isLocked()) return;

        try {
            s.backup();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (s.startRecording()) {
            recording = (Sentence)selectedNode;
            centralPanel.setFlash(true);
        }
    }

    public void startRecordingShort() {
        Debug.trace();

        if (recording != null) return;
        if (book == null) return;

        if (microphone == null) {
            JOptionPane.showMessageDialog(this, "Microphone not started. Start microphone first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();

        if (selectedNode == null) {
            selectedNode = book.getLastChapter();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        if (selectedNode instanceof Book) {
            selectedNode = book.getLastChapter();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        if (selectedNode instanceof Sentence) {
            selectedNode = (DefaultMutableTreeNode)selectedNode.getParent();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        Chapter c = (Chapter)selectedNode;

        DefaultMutableTreeNode lastLeaf = c.getLastLeaf();

        if (lastLeaf instanceof Sentence) {
            Sentence lastSentence = (Sentence)lastLeaf;
            lastSentence.setPostGap(Options.getInteger("catenation.short-sentence"));
            lastSentence.setPostGapType("continuation");
        }

        Sentence s = new Sentence();
        bookTreeModel.insertNodeInto(s, c, c.getChildCount());

        bookTree.expandPath(new TreePath(c.getPath()));
        bookTree.setSelectionPath(new TreePath(s.getPath()));
        bookTree.scrollPathToVisible(new TreePath(s.getPath()));

        if (s.startRecording()) {
            recording = s;
            centralPanel.setFlash(true);
        }
    }


    public void startRecordingNewParagraph() {
        Debug.trace();

        if (recording != null) return;
        if (book == null) return;

        if (microphone == null) {
            JOptionPane.showMessageDialog(this, "Microphone not started. Start microphone first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();

        if (selectedNode == null) {
            selectedNode = book.getLastChapter();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        if (selectedNode instanceof Book) {
            selectedNode = book.getLastChapter();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        if (selectedNode instanceof Sentence) {
            selectedNode = (DefaultMutableTreeNode)selectedNode.getParent();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        Chapter c = (Chapter)selectedNode;

        DefaultMutableTreeNode lastLeaf = c.getLastLeaf();

        if (lastLeaf instanceof Sentence) {
            Sentence lastSentence = (Sentence)lastLeaf;
            lastSentence.setPostGap(Options.getInteger("catenation.post-paragraph"));
            lastSentence.setPostGapType("paragraph");
        }

        Sentence s = new Sentence();
        bookTreeModel.insertNodeInto(s, c, c.getChildCount());

        bookTree.expandPath(new TreePath(c.getPath()));
        bookTree.setSelectionPath(new TreePath(s.getPath()));
        bookTree.scrollPathToVisible(new TreePath(s.getPath()));

        if (s.startRecording()) {
            recording = s;
            centralPanel.setFlash(true);
        }
    }

    public void startRecordingNewSection() {
        Debug.trace();

        if (recording != null) return;
        if (book == null) return;

        if (microphone == null) {
            JOptionPane.showMessageDialog(this, "Microphone not started. Start microphone first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();

        if (selectedNode == null) {
            selectedNode = book.getLastChapter();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        if (selectedNode instanceof Book) {
            selectedNode = book.getLastChapter();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        if (selectedNode instanceof Sentence) {
            selectedNode = (DefaultMutableTreeNode)selectedNode.getParent();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        Chapter c = (Chapter)selectedNode;

        DefaultMutableTreeNode lastLeaf = c.getLastLeaf();

        if (lastLeaf instanceof Sentence) {
            Sentence lastSentence = (Sentence)lastLeaf;
            lastSentence.setPostGap(Options.getInteger("catenation.post-section"));
            lastSentence.setPostGapType("section");
        }

        Sentence s = new Sentence();
        bookTreeModel.insertNodeInto(s, c, c.getChildCount());

        bookTree.expandPath(new TreePath(c.getPath()));
        bookTree.setSelectionPath(new TreePath(s.getPath()));
        bookTree.scrollPathToVisible(new TreePath(s.getPath()));

        if (s.startRecording()) {
            recording = s;
            centralPanel.setFlash(true);
        }
    }

    public void startRecording() {
        Debug.trace();

        if (recording != null) return;
        if (book == null) return;

        if (microphone == null) {
            JOptionPane.showMessageDialog(this, "Microphone not started. Start microphone first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();

        if (selectedNode == null) {
            selectedNode = book.getLastChapter();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        if (selectedNode instanceof Book) {
            selectedNode = book.getLastChapter();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        if (selectedNode instanceof Sentence) {
            selectedNode = (DefaultMutableTreeNode)selectedNode.getParent();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        Chapter c = (Chapter)selectedNode;

        DefaultMutableTreeNode lastLeaf = c.getLastLeaf();

        if (lastLeaf instanceof Sentence) {
            Sentence lastSentence = (Sentence)lastLeaf;
            lastSentence.setPostGap(Options.getInteger("catenation.post-sentence"));
            lastSentence.setPostGapType("sentence");
        }

        Sentence s = new Sentence();
        bookTreeModel.insertNodeInto(s, c, c.getChildCount());

        bookTree.expandPath(new TreePath(c.getPath()));
        bookTree.setSelectionPath(new TreePath(s.getPath()));
        bookTree.scrollPathToVisible(new TreePath(s.getPath()));

        if (s.startRecording()) {
            recording = s;
            centralPanel.setFlash(true);
        }
    }

    public void stopRecording() {
        Debug.trace();
        if (recording == null) return;
        recording.stopRecording();

        bookTreeModel.reload(book);

        bookTree.expandPath(new TreePath(((DefaultMutableTreeNode)recording.getParent()).getPath()));
        bookTree.setSelectionPath(new TreePath(recording.getPath()));
        bookTree.scrollPathToVisible(new TreePath(recording.getPath()));

        centralPanel.setFlash(false);
        recording = null;
        saveBookStructure();
    }

    public void deleteLastRecording() {
        Debug.trace();
        if (recording != null) return;
        if (book == null) return;

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();

        if (selectedNode == null) {
            selectedNode = book.getLastChapter();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        if (selectedNode instanceof Book) {
            selectedNode = book.getLastChapter();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        if (selectedNode instanceof Sentence) {
            selectedNode = (DefaultMutableTreeNode)selectedNode.getParent();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        Chapter c = (Chapter)selectedNode;

        Sentence s = c.getLastSentence();
        if (s == null) return;

        if (s.isLocked()) return;

        s.deleteFiles();
        bookTreeModel.removeNodeFromParent(s);
        s = c.getLastSentence();

        bookTree.expandPath(new TreePath(selectedNode.getPath()));
        if (s != null) {
            bookTree.setSelectionPath(new TreePath(s.getPath()));
            bookTree.scrollPathToVisible(new TreePath(s.getPath()));
        }  else {
            sampleWaveform.clearData();
        }
        selectedSentence = s;
        saveBookStructure();
    }

    public void addChapter() {
        Debug.trace();
        Chapter c = book.addChapter();   
        Chapter lc = book.getLastChapter();
        int i = bookTreeModel.getIndexOfChild(book, lc);
        bookTreeModel.insertNodeInto(c, book, i+1);
        bookTree.scrollPathToVisible(new TreePath(c.getPath()));
    } 

    @SuppressWarnings("unchecked")
    public void saveBookStructure() {
        Debug.trace();
        if (book == null) return;

        File bookRoot = new File(Options.get("path.storage"), book.getName());
        if (!bookRoot.exists()) {
            bookRoot.mkdirs();
        }

        try {
            File xml = new File(bookRoot, "audiobook.abx");
            Document doc = book.buildDocument();


            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(xml);
            transformer.transform(source, result);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void loadXMLBookStructure(File inputFile) {
        Debug.trace();

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            book = new Book(root);
            
            bookTreeModel = new DefaultTreeModel(book);

            book.loadBookXML(root, bookTreeModel);

            loadEffects();

            bookTree = new JTree(bookTreeModel);
            bookTree.setEditable(true);
            bookTree.setUI(new CustomTreeUI(mainScroll));

            bookTree.setCellRenderer(new BookTreeRenderer());


            InputMap im = bookTree.getInputMap(JComponent.WHEN_FOCUSED);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "startStopPlayback");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.SHIFT_DOWN_MASK), "startPlaybackFrom");

            roomNoise = new Sentence("room-noise", "Room Noise");

            bookTree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    Debug.trace();

                    DefaultMutableTreeNode n = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();

                    if (n instanceof BookTreeNode) {
                        BookTreeNode btn = (BookTreeNode)n;
                        btn.onSelect();
                    }

                    if (n instanceof Sentence) {
                        Sentence s = (Sentence)n;
                        selectedSentence = s;
                        sampleWaveform.setData(s.getDoubleAudioData(effectsEnabled));
                        sampleWaveform.setMarkers(s.getStartOffset(), s.getEndOffset());
                        s.updateCrossings();
                        sampleWaveform.setAltMarkers(s.getStartCrossing(), s.getEndCrossing());
                        postSentenceGap.setValue(s.getPostGap());
                        gainPercent.setValue((int)(s.getGain() * 100d));
                        locked.setSelected(s.isLocked());
                        attention.setSelected(s.getAttentionFlag());

                        setEffectChain(s.getEffectChain());

                        postSentenceGap.setEnabled(!s.isLocked());
                        gainPercent.setEnabled(!s.isLocked());
                        reprocessAudioFFT.setEnabled(!s.isLocked());
                        reprocessAudioPeak.setEnabled(!s.isLocked());
                        selectCutMode.setEnabled(!s.isLocked());
                        selectSplitMode.setEnabled(!s.isLocked());
                        doCutSplit.setEnabled(false);
                        selectCutMode.setSelected(false);
                        selectSplitMode.setSelected(false);
                    } else {
                        selectedSentence = null;
                        sampleWaveform.clearData();
                        postSentenceGap.setValue(0);
                        gainPercent.setValue(100);
                        locked.setSelected(false);
                        attention.setSelected(false);
                        selectCutMode.setEnabled(false);
                        selectSplitMode.setEnabled(false);
                        doCutSplit.setEnabled(false);
                        selectCutMode.setSelected(false);
                        selectSplitMode.setSelected(false);
                    }
                }
            });


            bookTree.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    Debug.trace();
                    if (e.isPopupTrigger()) {
                        treePopup(e);
                    }
                }

                public void mouseReleased(MouseEvent e) {
                    Debug.trace();
                    if (e.isPopupTrigger()) {
                        treePopup(e);
                    }
                }

            });

            mainScroll.setViewportView(bookTree);
            File r = inputFile.getParentFile();

            File cf = new File(r, "coverart.png");
            if (!cf.exists()) {
                cf = new File(r, "coverart.jpg");
                if (!cf.exists()) {
                    cf = new File(r, "coverart.gif");
                }
            }

            if (cf.exists()) {
                ImageIcon i = new ImageIcon(cf.getAbsolutePath());
                Image ri = Utils.getScaledImage(i.getImage(), 22, 22);
                book.setIcon(new ImageIcon(ri));
            } else {
                book.setIcon(Icons.book);
            }
            bookTreeModel.reload(book);

            bookTree.expandPath(new TreePath(book.getPath()));

            statusLabel.setText("Noise floor: " + getNoiseFloorDB() + "dB");

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        gatherOrphans();
    }

    public void loadBookStructure(File f) {
        Debug.trace();
        try {
            Properties prefs = new Properties();
            FileInputStream fis = new FileInputStream(f);
            prefs.loadFromXML(fis);

            File r = f.getParentFile();

            buildBook(prefs);

            File cf = new File(r, "coverart.png");
            if (!cf.exists()) {
                cf = new File(r, "coverart.jpg");
                if (!cf.exists()) {
                    cf = new File(r, "coverart.gif");
                }
            }

            if (cf.exists()) {
                ImageIcon i = new ImageIcon(cf.getAbsolutePath());
                Image ri = Utils.getScaledImage(i.getImage(), 22, 22);
                book.setIcon(new ImageIcon(ri));
                bookTreeModel.reload(book);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Retained for legacy use...! */
    public void buildBook(Properties prefs) {
        Debug.trace();

        book = new Book(prefs, prefs.getProperty("book.name"));

        book.setAuthor(prefs.getProperty("book.author"));
        book.setGenre(prefs.getProperty("book.genre"));
        book.setComment(prefs.getProperty("book.comment"));
        book.setACX(prefs.getProperty("book.acx"));

        loadEffects();

        String defaultEffectChain = prefs.getProperty("audio.effect.default");

        if (defaultEffectChain == null) {
            defaultEffectChain = "none";
        }

        book.setDefaultEffect(defaultEffectChain);

        int sr = Utils.s2i(prefs.getProperty("audio.recording.samplerate"));
        if (sr == 0) {
            sr = Options.getInteger("audio.recording.samplerate");
        }
        book.setSampleRate(sr);

        int chans = Utils.s2i(prefs.getProperty("audio.recording.channels"));
        if (chans == 0) {
            chans = Options.getInteger("audio.recording.channels");
        }
        book.setChannels(chans);

        int res = Utils.s2i(prefs.getProperty("audio.recording.resolution"));
        if (res == 0) {
            res = Options.getInteger("audio.recording.resolution");
        }
        book.setResolution(res);

        bookTreeModel = new DefaultTreeModel(book);
        bookTree = new JTree(bookTreeModel);
        bookTree.setEditable(true);
        bookTree.setUI(new CustomTreeUI(mainScroll));

        bookTree.setCellRenderer(new BookTreeRenderer());


        InputMap im = bookTree.getInputMap(JComponent.WHEN_FOCUSED);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "startStopPlayback");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.SHIFT_DOWN_MASK), "startPlaybackFrom");

        roomNoise = new Sentence("room-noise", "Room Noise");

        bookTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                Debug.trace();
                DefaultMutableTreeNode n = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();
                if (n instanceof BookTreeNode) {
                    BookTreeNode btn = (BookTreeNode)n;
                    btn.onSelect();
                }

                if (n instanceof Sentence) {
                    Sentence s = (Sentence)n;
                    selectedSentence = s;
                    sampleWaveform.setData(s.getDoubleAudioData(effectsEnabled));
                    sampleWaveform.setMarkers(s.getStartOffset(), s.getEndOffset());
                    s.updateCrossings();
                    sampleWaveform.setAltMarkers(s.getStartCrossing(), s.getEndCrossing());
                    postSentenceGap.setValue(s.getPostGap());
                    gainPercent.setValue((int)(s.getGain() * 100d));
                    locked.setSelected(s.isLocked());
                    attention.setSelected(s.getAttentionFlag());

                    setEffectChain(s.getEffectChain());

                    postSentenceGap.setEnabled(!s.isLocked());
                    gainPercent.setEnabled(!s.isLocked());
                    reprocessAudioFFT.setEnabled(!s.isLocked());
                    reprocessAudioPeak.setEnabled(!s.isLocked());
                    selectCutMode.setEnabled(!s.isLocked());
                    selectSplitMode.setEnabled(!s.isLocked());
                    doCutSplit.setEnabled(false);
                    selectCutMode.setSelected(false);
                    selectSplitMode.setSelected(false);
                } else {
                    selectedSentence = null;
                    sampleWaveform.clearData();
                    postSentenceGap.setValue(0);
                    gainPercent.setValue(100);
                    locked.setSelected(false);
                    attention.setSelected(false);
                    selectCutMode.setEnabled(false);
                    selectSplitMode.setEnabled(false);
                    doCutSplit.setEnabled(false);
                    selectCutMode.setSelected(false);
                    selectSplitMode.setSelected(false);
                }
            }
        });


        bookTree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                Debug.trace();
                if (e.isPopupTrigger()) {
                    treePopup(e);
                }
            }

            public void mouseReleased(MouseEvent e) {
                Debug.trace();
                if (e.isPopupTrigger()) {
                    treePopup(e);
                }
            }

        });

        mainScroll.setViewportView(bookTree);


        Chapter c = new Chapter("audition", prefs.getProperty("chapter.audition.name"));
        c.setPostGap(Utils.s2i(prefs.getProperty("chapter.audition.post-gap")));
        c.setPreGap(Utils.s2i(prefs.getProperty("chapter.audition.pre-gap")));
        bookTreeModel.insertNodeInto(c, book, 0);
        
        for (int i = 0; i < 100000000; i++) {
            String id = prefs.getProperty(String.format("chapter.audition.sentence.%08d.id", i));
            String text = prefs.getProperty(String.format("chapter.audition.sentence.%08d.text", i));
            int gap = Utils.s2i(prefs.getProperty(String.format("chapter.audition.sentence.%08d.post-gap", i)));
            if (id == null) break;
            Sentence s = new Sentence(id, text);
            s.setPostGap(gap);
            s.setStartOffset(Utils.s2i(prefs.getProperty(String.format("chapter.audition.sentence.%08d.start-offset", i))));
            s.setEndOffset(Utils.s2i(prefs.getProperty(String.format("chapter.audition.sentence.%08d.end-offset", i))));
            s.setLocked(Utils.s2b(prefs.getProperty(String.format("chapter.audition.sentence.%08d.locked", i))));
            s.setAttentionFlag(Utils.s2b(prefs.getProperty(String.format("chapter.audition.sentence.%08d.attention", i))));
            s.setGain(Utils.s2d(prefs.getProperty(String.format("chapter.audition.sentence.%08d.gain", i))));
            s.setEffectChain(prefs.getProperty(String.format("chapter.audition.sentence.%08d.effect", i)));
            s.setPostGapType(prefs.getProperty(String.format("chapter.audition.sentence.%08d.gaptype", i)));
            bookTreeModel.insertNodeInto(s, c, c.getChildCount());
        }

        c = new Chapter("open", prefs.getProperty("chapter.open.name"));
        c.setPostGap(Utils.s2i(prefs.getProperty("chapter.open.post-gap")));
        c.setPreGap(Utils.s2i(prefs.getProperty("chapter.open.pre-gap")));
        bookTreeModel.insertNodeInto(c, book, 0);
        
        for (int i = 0; i < 100000000; i++) {
            String id = prefs.getProperty(String.format("chapter.open.sentence.%08d.id", i));
            String text = prefs.getProperty(String.format("chapter.open.sentence.%08d.text", i));
            int gap = Utils.s2i(prefs.getProperty(String.format("chapter.open.sentence.%08d.post-gap", i)));
            if (id == null) break;
            Sentence s = new Sentence(id, text);
            s.setPostGap(gap);
            s.setStartOffset(Utils.s2i(prefs.getProperty(String.format("chapter.open.sentence.%08d.start-offset", i))));
            s.setEndOffset(Utils.s2i(prefs.getProperty(String.format("chapter.open.sentence.%08d.end-offset", i))));
            s.setLocked(Utils.s2b(prefs.getProperty(String.format("chapter.open.sentence.%08d.locked", i))));
            s.setAttentionFlag(Utils.s2b(prefs.getProperty(String.format("chapter.open.sentence.%08d.attention", i))));
            s.setGain(Utils.s2d(prefs.getProperty(String.format("chapter.open.sentence.%08d.gain", i))));
            s.setEffectChain(prefs.getProperty(String.format("chapter.open.sentence.%08d.effect", i)));
            s.setPostGapType(prefs.getProperty(String.format("chapter.open.sentence.%08d.gaptype", i)));
            bookTreeModel.insertNodeInto(s, c, c.getChildCount());
        }



        for (int cno = 1; cno < 10000; cno++) {
            String cname = prefs.getProperty(String.format("chapter.%04d.name", cno));
            if (cname == null) break;

            c = new Chapter(String.format("%04d", cno), cname);
            c.setPostGap(Utils.s2i(prefs.getProperty(String.format("chapter.%04d.post-gap", cno))));
            c.setPreGap(Utils.s2i(prefs.getProperty(String.format("chapter.%04d.pre-gap", cno))));
            bookTreeModel.insertNodeInto(c, book, book.getChildCount());

            for (int i = 0; i < 100000000; i++) {
                String id = prefs.getProperty(String.format("chapter.%04d.sentence.%08d.id", cno, i));
                String text = prefs.getProperty(String.format("chapter.%04d.sentence.%08d.text", cno, i));
                int gap = Utils.s2i(prefs.getProperty(String.format("chapter.%04d.sentence.%08d.post-gap", cno, i)));
                if (id == null) break;
                Sentence s = new Sentence(id, text);
                s.setPostGap(gap);
                s.setStartOffset(Utils.s2i(prefs.getProperty(String.format("chapter.%04d.sentence.%08d.start-offset", cno, i))));
                s.setEndOffset(Utils.s2i(prefs.getProperty(String.format("chapter.%04d.sentence.%08d.end-offset", cno, i))));
                s.setLocked(Utils.s2b(prefs.getProperty(String.format("chapter.%04d.sentence.%08d.locked", cno, i))));
                s.setAttentionFlag(Utils.s2b(prefs.getProperty(String.format("chapter.%04d.sentence.%08d.attention", cno, i))));
                s.setGain(Utils.s2d(prefs.getProperty(String.format("chapter.%04d.sentence.%08d.gain", cno, i))));
                s.setEffectChain(prefs.getProperty(String.format("chapter.%04d.sentence.%08d.effect", cno, i)));
                s.setPostGapType(prefs.getProperty(String.format("chapter.%04d.sentence.%08d.gaptype", cno, i)));
                bookTreeModel.insertNodeInto(s, c, c.getChildCount());
            }
        }

        c = new Chapter("close", prefs.getProperty("chapter.close.name"));
        c.setPostGap(Utils.s2i(prefs.getProperty("chapter.close.post-gap")));
        c.setPreGap(Utils.s2i(prefs.getProperty("chapter.close.pre-gap")));
        bookTreeModel.insertNodeInto(c, book, book.getChildCount());

        for (int i = 0; i < 100000000; i++) {
            String id = prefs.getProperty(String.format("chapter.close.sentence.%08d.id", i));
            String text = prefs.getProperty(String.format("chapter.close.sentence.%08d.text", i));
            int gap = Utils.s2i(prefs.getProperty(String.format("chapter.close.sentence.%08d.post-gap", i)));
            if (id == null) break;
            Sentence s = new Sentence(id, text);
            s.setPostGap(gap);
            s.setStartOffset(Utils.s2i(prefs.getProperty(String.format("chapter.close.sentence.%08d.start-offset", i))));
            s.setEndOffset(Utils.s2i(prefs.getProperty(String.format("chapter.close.sentence.%08d.end-offset", i))));
            s.setLocked(Utils.s2b(prefs.getProperty(String.format("chapter.close.sentence.%08d.locked", i))));
            s.setAttentionFlag(Utils.s2b(prefs.getProperty(String.format("chapter.close.sentence.%08d.attention", i))));
            s.setGain(Utils.s2d(prefs.getProperty(String.format("chapter.close.sentence.%08d.gain", i))));
            s.setEffectChain(prefs.getProperty(String.format("chapter.close.sentence.%08d.effect", i)));
            s.setPostGapType(prefs.getProperty(String.format("chapter.close.sentence.%08d.gaptype", i)));
            bookTreeModel.insertNodeInto(s, c, c.getChildCount());
        }

        bookTree.expandPath(new TreePath(book.getPath()));

        statusLabel.setText("Noise floor: " + getNoiseFloorDB() + "dB");
        book.setIcon(Icons.book);
    }

    public void openBook() {
        Debug.trace();

        OpenBookPanel info = new OpenBookPanel();
        int r = JOptionPane.showConfirmDialog(this, info, "Open Book", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            File f = info.getSelectedFile();
            if (f == null) return;
            if (!f.exists()) {
                JOptionPane.showMessageDialog(this, "File not found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (f.isDirectory()) {
                JOptionPane.showMessageDialog(this, "File is directory.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!(f.getName().endsWith(".abk") || f.getName().endsWith(".abx"))) {
                JOptionPane.showMessageDialog(this, "Not a .abk or .abx file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
                
            if (f.getName().endsWith(".abx")) {
                loadXMLBookStructure(f);
            } else {
                loadBookStructure(f);
            }

            Options.set("path.last-book", book.getName());
            Options.savePreferences();
            
        }

        
    }

    public File getBookFolder() {
        Debug.trace();
        File bf = new File(Options.get("path.storage"), book.getName());
        if (!bf.exists()) {
            bf.mkdirs();
        }
        return bf;
    }

    public double getNoiseFloor() { 
        Debug.trace();
        if (roomNoise == null) return 0;
        return roomNoise.getPeak();
    }

    public int getNoiseFloorDB() {
        Debug.trace();
        if (roomNoise == null) return 0;
        return roomNoise.getPeakDB();
    }

    public void recordRoomNoise() {
        Debug.trace();
        if (roomNoise.startRecording()) {

            centralPanel.setFlash(true);
            java.util.Timer ticker = new java.util.Timer(true);
            ticker.schedule(new TimerTask() {
                public void run() {
                    Debug.trace();
                    roomNoise.stopRecording();
                    centralPanel.setFlash(false);
                    statusLabel.setText("Noise floor: " + getNoiseFloorDB() + "dB");
                }
            }, 5000); // 5 seconds of recording
        }
    }


    public void playSelectedSentence() {
        Debug.trace();
        if (selectedSentence == null) return;
        if (playing != null) return;
        if (getNoiseFloor() == 0) {
            alertNoRoomNoise();
            return;
        }
        playing = selectedSentence;

        playingThread = new Thread(new Runnable() {
            public void run() {
                Debug.trace();
                Sentence s = playing;
                byte[] data;

                try {

                    int blockSize = Options.getInteger("audio.playback.blocksize");

                    AudioFormat sampleformat = s.getAudioFormat();
                    float sampleRate = sampleformat.getSampleRate();
//                    sampleRate *= toolBar.getPlaybackSpeed();
                    AudioFormat format = new AudioFormat(sampleRate, 16, 2, true, false);
            
                    play = AudioSystem.getSourceDataLine(format, Options.getPlaybackMixer());
                    play.open(format);
                    play.start();
                    play.drain();

                    bookTree.scrollPathToVisible(new TreePath(s.getPath()));
                    data = s.getPCMData(effectsEnabled);
                    for (int pos = 0; pos < data.length; pos += blockSize) {
                        sampleWaveform.setPlayMarker(pos / format.getFrameSize());
                        int l = data.length - pos;
                        if (l > blockSize) l = blockSize;
                        play.write(data, pos, l);
                    }

                    play.drain();
                    play.stop();
                    play.close();
                    play = null;
                    playing = null;
                } catch (Exception e) {
                    playing = null;
                    if (play != null) {
                        play.drain();
                        play.stop();
                        play.close();
                    }
                    play = null;
                }
            }
        });

        playingThread.setDaemon(true);
        playingThread.start();
    }

    class NormalizeThread implements Runnable {
        ProgressDialog dialog;
        Chapter chapter;
    
        public NormalizeThread(Chapter c, ProgressDialog e) {
            super();
            Debug.trace();
            dialog = e;
            chapter = c;
        }

        @SuppressWarnings("unchecked")
        public void run() {
            Debug.trace();

            int numKids = chapter.getChildCount();
            int kidCount = 0;
            double lastGain = -1;
            double variance = Options.getInteger("audio.recording.variance") / 100d;
            for (Enumeration s = chapter.children(); s.hasMoreElements();) {
                kidCount++;
                dialog.setProgress(kidCount * 2000 / numKids);
                Sentence snt = (Sentence)s.nextElement();
                if (lastGain == -1) {
                    lastGain = snt.normalize();
                } else {
                    lastGain = snt.normalize(lastGain - variance, lastGain + variance);
                }
            }

            dialog.closeDialog();
        }
    }

    class AutoTrimThread implements Runnable {
        ProgressDialog dialog;
        Chapter chapter;
        int type;
        int scope;

        public final static int FFT = 0;
        public final static int Peak = 1;
        public final static int NewOnly = 0;
        public final static int All = 1;
   
        public AutoTrimThread(Chapter c, ProgressDialog e, int t, int sc) {
            super();
            Debug.trace();
            dialog = e;
            chapter = c;
            type = t;
            scope = sc;
        }

        @SuppressWarnings("unchecked")
        public void run() {
            Debug.trace();

            int numKids = chapter.getChildCount();
            int kidCount = 0;
            for (Enumeration s = chapter.children(); s.hasMoreElements();) {
                kidCount++;
                dialog.setProgress(kidCount * 2000 / numKids);
                Sentence snt = (Sentence)s.nextElement();
                if (scope == NewOnly) {
                    if (snt.isProcessed()) continue;
                }
                switch (type) {
                    case FFT:
                        snt.autoTrimSampleFFT();
                        break;
                    case Peak:
                        snt.autoTrimSamplePeak();
                        break;
                } 
            }

            dialog.closeDialog();
        }
    }



    class ExportThread implements Runnable {
        ProgressDialog exportDialog;
        Chapter chapter;

        public ExportThread(Chapter c, ProgressDialog e) {
            super();
            Debug.trace();
            exportDialog = e;
            chapter = c;
        }

        @SuppressWarnings("unchecked")
        public void run() {
            Debug.trace();

            try {
                chapter.exportChapter(exportDialog);
            } catch (Exception e) {
                e.printStackTrace();
            }

            exportDialog.closeDialog();
        }
    }

    @SuppressWarnings("unchecked")
    public void exportAudio() {
        Debug.trace();

        
        for (Enumeration o = book.children(); o.hasMoreElements();) {
            Chapter c = (Chapter)o.nextElement();
            ProgressDialog ed = new ProgressDialog("Exporting " + c.getName());

            ExportThread t = new ExportThread(c, ed);
            Thread nt = new Thread(t);
            nt.start();
            ed.setVisible(true);
        }

        JOptionPane.showMessageDialog(this, "Book export finished", "Export finished", JOptionPane.PLAIN_MESSAGE);
    }


    public void playToSelectedSentence() {
        Debug.trace();
        if (selectedSentence == null) return;
        if (playing != null) return;
        if (getNoiseFloor() == 0) {
            alertNoRoomNoise();
            return;
        }
        playing = selectedSentence;

        playingThread = new Thread(new Runnable() {
            public void run() {
                Debug.trace();
                Sentence s = playing;
                byte[] data;

                try {

                    int blockSize = Options.getInteger("audio.playback.blocksize");

                    AudioFormat sampleformat = s.getAudioFormat();
                    AudioFormat format = new AudioFormat(sampleformat.getSampleRate(), 16, 2, true, false);

                    play = AudioSystem.getSourceDataLine(format, Options.getPlaybackMixer());
                    play.open(format);
                    play.start();
                    play.drain();

                    bookTree.scrollPathToVisible(new TreePath(s.getPath()));
                    data = s.getPCMData(effectsEnabled);

                    int startPos = 0;
                    int endPos = data.length / format.getFrameSize();

//foobar
                    if (selectSplitMode.isSelected()) {
                        endPos = sampleWaveform.getCutStart() - selectedSentence.getStartCrossing();
                        if (endPos < 0) endPos = 0;
                    } else if (selectCutMode.isSelected()) {
                        startPos = sampleWaveform.getCutStart() - selectedSentence.getStartCrossing();;
                        endPos = sampleWaveform.getCutEnd() - selectedSentence.getStartCrossing();;
                        if (startPos < 0) startPos = 0;
                        if (endPos < 0) endPos = 0;
                    }

                    startPos *= format.getFrameSize();
                    endPos *= format.getFrameSize();

                    if (startPos > data.length) startPos = data.length;
                    if (endPos > data.length) endPos = data.length;

                    for (int pos = startPos; pos < endPos; pos += blockSize) {
                        sampleWaveform.setPlayMarker(pos / format.getFrameSize());
                        int l = data.length - pos;
                        if (l > blockSize) l = blockSize;
                        play.write(data, pos, l);
                    }

                    play.drain();
                    play.stop();
                    play.close();
                    play = null;
                    playing = null;
                } catch (Exception e) {
                    playing = null;
                    if (play != null) {
                        play.drain();
                        play.stop();
                        play.close();
                    }
                    play = null;
                    e.printStackTrace();
                }
            }
        });

        playingThread.setDaemon(true);
        playingThread.start();
    }

    public void playFromSelectedSentence() {
        Debug.trace();
        if (selectedSentence == null) return;
        if (playing != null) return;
        if (getNoiseFloor() == 0) {
            alertNoRoomNoise();
            return;
        }
        playing = selectedSentence;

        playingThread = new Thread(new Runnable() {
            public void run() {
                Debug.trace();
                Sentence s = playing;
                byte[] data;

                try {

                    int blockSize = Options.getInteger("audio.playback.blocksize");

                    AudioFormat sampleformat = s.getAudioFormat();
                    float sampleRate = sampleformat.getSampleRate();
                    sampleRate *= toolBar.getPlaybackSpeed();
                    AudioFormat format = new AudioFormat(sampleRate, 16, 2, true, false);
                    play = AudioSystem.getSourceDataLine(format, Options.getPlaybackMixer());
                    play.open(format);
                    play.start();
                    play.drain();

                    while (playing != null) {
                        bookTree.scrollPathToVisible(new TreePath(s.getPath()));
                        DefaultMutableTreeNode prev = s.getPreviousSibling();
                        boolean first = false;
                        if (prev == null) {
                            first = true;
                        } else if (!(prev instanceof Sentence)) {
                            first = true;
                        }
                        if (first) {
                            data = getRoomNoise(Utils.s2i(Options.get("catenation.pre-chapter")));
                            play.write(data, 0, data.length);
                        }
                        data = s.getPCMData(effectsEnabled);
                        DefaultMutableTreeNode next = s.getNextSibling();
                        if (next != null) {
                            Thread t = new Thread(new Runnable() {
                                public void run() {
                                    Debug.trace();
                                    Sentence ns = (Sentence)next;
                                    ns.getProcessedAudioData(effectsEnabled); // Cache it
                                }
                            });
                            t.start();
                        }
                        for (int pos = 0; pos < data.length; pos += blockSize) {
                            sampleWaveform.setPlayMarker(pos / format.getFrameSize());
                            int l = data.length - pos;
                            if (l > blockSize) l = blockSize;
                            play.write(data, pos, l);
                        }

                        boolean last = false;
                        if (next == null) {
                            last = true;
                        } else if (!(next instanceof Sentence)) {
                            last = true;
                        }

                        if (last) {
                            data = getRoomNoise(Utils.s2i(Options.get("catenation.post-chapter")));
                            play.write(data, 0, data.length);
                            playing = null;
                        } else {
                            data = getRoomNoise(s.getPostGap());
                            play.write(data, 0, data.length);
                        }
                        s = (Sentence)next;
                        if (s != null) {
                            bookTree.setSelectionPath(new TreePath(s.getPath()));
                        }
                    }
                    play.drain();
                    play.stop();
                    play.close();
                    play = null;
                } catch (Exception e) {
                    playing = null;
                    if (play != null) {
                        play.drain();
                        play.stop();
                        play.close();
                    }
                    play = null;
//                    e.printStackTrace();
                }
            }
        });

        playingThread.setDaemon(true);
        playingThread.start();
    }


    public Sentence getRoomNoiseSentence() {
        return roomNoise;
    }

    public byte[] getRoomNoise(int ms) {
        Debug.trace();

        if (roomNoise == null) return null;

        roomNoise.setEffectChain(book.getDefaultEffect());

        int len = roomNoise.getSampleSize();
        if (len == 0) return null;

        AudioFormat f = roomNoise.getAudioFormat();
        
        float sr = f.getSampleRate();

        int samples = (int)(ms * (sr / 1000f));

        int start = rng.nextInt(len - samples);
        int end = start + samples;

        roomNoise.setStartOffset(start);
        roomNoise.setEndOffset(end);

        byte[] data = roomNoise.getPCMData();

        return data;
    }

    public void stopPlaying() {
        Debug.trace();
        if (play != null) {
            play.close();
            play = null;
        }
        playing = null;
    }

    public void alertNoRoomNoise() {
        Debug.trace();
        JOptionPane.showMessageDialog(this, "You must record room noise\nbefore recording or playback", "Error", JOptionPane.ERROR_MESSAGE);
    }

    public boolean enableMicrophone() {
        Debug.trace();
        AudioFormat format = Options.getAudioFormat();

        Mixer.Info mixer = Options.getRecordingMixer();

        microphone = null;

        try {
            microphone = AudioSystem.getTargetDataLine(format, mixer);
        } catch (Exception e) {
            e.printStackTrace();
            microphone = null;
            return false;
        }

        if (microphone == null) {
            JOptionPane.showMessageDialog(AudiobookRecorder.window, "Sample format not supported", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        microphoneStream = new AudioInputStream(microphone);

        try {
            microphone.open();
        } catch (Exception e) {
            e.printStackTrace();
            microphone = null;
            return false;
        }

        microphone.start();
        return true;
    }

    public void disableMicrophone() {
        Debug.trace();
        try {
            microphoneStream.close();
            microphone.stop();
            microphone.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        microphone = null;
        microphoneStream = null;
    } 

    public void execScript(String s) {
        Debug.trace();
        if (s == null) return;
        String[] lines = s.split("\n");

        for (String line : lines) {
            line = line.trim();
            try {
                if (!line.equals("")) {
                    Process p = Runtime.getRuntime().exec(line);
                    p.waitFor();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void mergeBook() {
        Debug.trace();
        OpenBookPanel info = new OpenBookPanel();
        int r = JOptionPane.showConfirmDialog(this, info, "Merge Book", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            File f = info.getSelectedFile();
            if (!f.exists()) {
                JOptionPane.showMessageDialog(this, "File not found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (f.isDirectory()) {
                JOptionPane.showMessageDialog(this, "File is directory.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!(f.getName().endsWith(".abk") || f.getName().endsWith(".abx"))) {
                JOptionPane.showMessageDialog(this, "Not a .abk or .abx file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                Properties prefs = new Properties();
                FileInputStream fis = new FileInputStream(f);
                prefs.loadFromXML(fis);

                // Merge the opening credits if they exist
                if (prefs.getProperty("chapter.open.name") != null) {
                    mergeChapter(prefs, "open");
                }

                // Merge the audition if it exists
                if (prefs.getProperty("chapter.audition.name") != null) {
                    mergeChapter(prefs, "audition");
                }

                for (int i = 0; i < 9999; i++) {
                    String chid = String.format("%04d", i);
                    if (prefs.getProperty("chapter." + chid + ".name") != null) {
                        mergeChapter(prefs, chid);
                    }
                }

                // Merge the opening credits if they exist
                if (prefs.getProperty("chapter.open.name") != null) {
                    mergeChapter(prefs, "open");
                }

                // Merge the closing credits if they exist
                if (prefs.getProperty("chapter.close.name") != null) {
                    mergeChapter(prefs, "close");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }



        }
    }

    public void mergeChapter(Properties prefs, String chid) {
        Debug.trace();
        Chapter c = book.addChapter();
        c.setName("Merged-" + prefs.getProperty("chapter." + chid + ".name"));
        c.setPostGap(Utils.s2i(prefs.getProperty("chapter." + chid + ".post-gap")));
        c.setPreGap(Utils.s2i(prefs.getProperty("chapter." + chid + ".pre-gap")));

        Chapter lc = book.getLastChapter();
        int idx = bookTreeModel.getIndexOfChild(book, lc);
        bookTreeModel.insertNodeInto(c, book, idx+1);


        File srcBook = new File(Options.get("path.storage"), prefs.getProperty("book.name"));
        File srcFolder = new File(srcBook, "files");

        File dstBook = new File(Options.get("path.storage"), book.getName());
        File dstFolder = new File(dstBook, "files");

        for (int i = 0; i < 100000000; i++) {
            String id = prefs.getProperty(String.format("chapter." + chid + ".sentence.%08d.id", i));
            String text = prefs.getProperty(String.format("chapter." + chid + ".sentence.%08d.text", i));
            int gap = Utils.s2i(prefs.getProperty(String.format("chapter." + chid + ".sentence.%08d.post-gap", i)));
            if (id == null) break;
            Sentence s = new Sentence(id, text);
            s.setPostGap(gap);
            s.setStartOffset(Utils.s2i(prefs.getProperty(String.format("chapter." + chid + ".sentence.%08d.start-offset", i))));
            s.setEndOffset(Utils.s2i(prefs.getProperty(String.format("chapter." + chid + ".sentence.%08d.end-offset", i))));
            s.setLocked(Utils.s2b(prefs.getProperty(String.format("chapter." + chid + ".sentence.%08d.locked", i))));
            bookTreeModel.insertNodeInto(s, c, c.getChildCount());

            File srcFile = new File(srcFolder, s.getId() + ".wav");
            File dstFile = new File(dstFolder, s.getId() + ".wav");

            if (srcFile.exists()) {
                try {
                    Files.copy(srcFile.toPath(), dstFile.toPath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

    }

    ArrayList<File> gatherFiles(File root) {
        ArrayList<File> fileList = new ArrayList<File>();
        File[] files = root.listFiles();
        for (File f : files) {
            if (f.getName().startsWith(".")) continue; 
            if (!f.isDirectory()) {
                fileList.add(f);
            }
        }

        for (File f : files) {
            if (f.getName().startsWith(".")) continue; 
            if (f.isDirectory()) {
                fileList.add(f);
                fileList.addAll(gatherFiles(f));
            }
        }
        return fileList;
    }

    public class ArchiveBookThread implements Runnable {
        ProgressDialog pd;

        public ArchiveBookThread(ProgressDialog p) {
            Debug.trace();
            pd = p;
        }

        public void run() {
            Debug.trace();
            try {
                String name = AudiobookRecorder.this.book.getName();
                File storageDir = new File(Options.get("path.storage"));
                File bookDir = new File(storageDir, name);
                File archiveDir = new File(Options.get("path.archive"));

                ArrayList<File> fileList = gatherFiles(bookDir);

                if (!archiveDir.exists()) {
                    archiveDir.mkdirs();
                }

                File archiveFile = new File(archiveDir, name + ".abz");
                System.err.println("Archiving to " + archiveFile.getAbsolutePath());
                if (archiveFile.exists()) {
                    archiveFile.delete();
                }

                FileOutputStream fos = new FileOutputStream(archiveFile);
                ZipOutputStream zos = new ZipOutputStream(fos);
            
                zos.putNextEntry(new ZipEntry(name + "/"));
                zos.closeEntry();

                int numFiles = fileList.size();
                int fileNo = 0;

                String prefix = storageDir.getAbsolutePath();

                for (File f : fileList) {
                    fileNo++;
                    int pct = fileNo * 2000 / numFiles;
                    pd.setProgress(pct);

                    String path = f.getAbsolutePath().substring(prefix.length() + 1);

                    if (f.isDirectory()) {
                        ZipEntry entry = new ZipEntry(path + "/");
                        zos.putNextEntry(entry);
                        zos.closeEntry();
                    } else {
                        ZipEntry entry = new ZipEntry(path);
                        entry.setSize(f.length());
                        entry.setTime(f.lastModified());
                        zos.putNextEntry(entry);

                        FileInputStream fis = new FileInputStream(f);
                        byte[] buffer = new byte[1024];
                        int bytesRead = 0;
                        while ((bytesRead = fis.read(buffer, 0, 1024)) != -1) {
                            zos.write(buffer, 0, bytesRead);
                        }
                        fis.close();
                        zos.closeEntry();
                    }
                }

                // Now grab any used effects that aren't already part of the book folder
                ArrayList<String> usedEffects = book.getUsedEffects();
                for (String ef : usedEffects) {
                    File inBook = new File(bookDir, ef + ".eff");
                    if (!inBook.exists()) {
                        File sys = new File(storageDir, "System");
                        File sysFile = new File(sys, ef + ".eff");
                        if (sysFile.exists()) {
                            ZipEntry entry = new ZipEntry(name + "/" + ef + ".eff");
                            entry.setSize(sysFile.length());
                            entry.setTime(sysFile.lastModified());
                            zos.putNextEntry(entry);

                            FileInputStream fis = new FileInputStream(sysFile);
                            byte[] buffer = new byte[1024];
                            int bytesRead = 0;
                            while ((bytesRead = fis.read(buffer, 0, 1024)) != -1) {
                                zos.write(buffer, 0, bytesRead);
                            }
                            fis.close();
                            zos.closeEntry();
                            
                        }
                    }
                }


                zos.flush();
                zos.close();

                while (fileList.size() > 0) {
                    File f = fileList.remove(fileList.size() - 1);
                    f.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            pd.closeDialog();
        }
    }

    public void archiveBook() {
        Debug.trace();
        int r = JOptionPane.showConfirmDialog(this, "This will stash the current book away\nin the archives folder in a compressed\nform. The existing book files will be deleted\nand the book closed.\n\nAre you sure you want to do this?", "Archive Book", JOptionPane.OK_CANCEL_OPTION);

        if (r == JOptionPane.OK_OPTION) {

            ProgressDialog pd = new ProgressDialog("Archiving book...");
            saveBookStructure();

            ArchiveBookThread runnable = new ArchiveBookThread(pd);
            Thread t = new Thread(runnable);
            t.start();
            pd.setVisible(true);
            book = null;
            bookTree = null;
            mainScroll.setViewportView(null);
        }
    }

    public void openArchive() {
        Debug.trace();
        JFileChooser jc = new JFileChooser(new File(Options.get("path.archive")));
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Audiobook Archives", "abz");
        jc.addChoosableFileFilter(filter);
        jc.setFileFilter(filter);
        jc.setDialogTitle("Select Audiobook Archive");
        int r = jc.showOpenDialog(this);
        
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = jc.getSelectedFile();
            if (f.exists()) {

                BookPanel pan = null;

                try {

                    String bookName = null;
                    String bookAuthor = null;
                    String bookGenre = null;
                    String bookComment = null;
                    ImageIcon bookCover = null;

                    ZipInputStream zis = new ZipInputStream(new FileInputStream(f)) {
                        public void close() throws IOException {
                            Debug.trace();
                            return;
                        }
                    };
                    ZipEntry entry = null;
                    ImageIcon cover = null;
                    Properties props = new Properties();

                    while ((entry = zis.getNextEntry()) != null) {
                        if (entry.getName().endsWith("/audiobook.abk")) {
                            props.loadFromXML(zis);

                            if (bookName == null) bookName = props.getProperty("book.name");
                            if (bookAuthor == null) bookAuthor = props.getProperty("book.author");
                            if (bookGenre == null) bookGenre = props.getProperty("book.genre");
                            if (bookComment == null) bookComment = props.getProperty("book.comment");
                        }

                        if (entry.getName().endsWith("/audiobook.abx")) {
                            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                            Document doc = dBuilder.parse(zis);
                            doc.getDocumentElement().normalize();

                            Element rootnode = doc.getDocumentElement();

                            bookName = Book.getTextNode(rootnode, "title");
                            bookAuthor = Book.getTextNode(rootnode, "author");
                            bookGenre = Book.getTextNode(rootnode, "genre");
                            bookComment = Book.getTextNode(rootnode, "comment");
                        }

                        if (
                                entry.getName().endsWith("/coverart.png") ||
                                entry.getName().endsWith("/coverart.jpg") ||
                                entry.getName().endsWith("/coverart.gif") 
                            ) {
                            bookCover = new ImageIcon(ImageIO.read(zis));
                        }
                    }
                    zis.close();

                    pan = new BookPanel(bookName, bookAuthor, bookGenre, bookComment, bookCover);

                    int okToImport = JOptionPane.showConfirmDialog(this, pan, "Import this book?", JOptionPane.OK_CANCEL_OPTION);
                    if (okToImport == JOptionPane.OK_OPTION) {
                        zis = new ZipInputStream(new FileInputStream(f));
                        while ((entry = zis.getNextEntry()) != null) {
                            File out = new File(Options.get("path.storage"), entry.getName());
                            if (entry.isDirectory()) {
                                out.mkdirs();
                            } else {
                                byte[] buffer = new byte[1024];
                                int nr;
                                FileOutputStream fos = new FileOutputStream(out);
                                while ((nr = zis.read(buffer, 0, 1024)) > 0) {
                                    fos.write(buffer, 0, nr);
                                }
                                fos.close();
                            }
                        }
                        zis.close();

                        File bookdir = new File(Options.get("path.storage"), props.getProperty("book.name"));
                        File conf = new File(bookdir, "audiobook.abx");
                        if (conf.exists()) {
                            loadXMLBookStructure(conf);
                        } else {
                            conf = new File(bookdir, "audiobook.abk");
                            loadBookStructure(conf);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void loadCoverArt() {
        Debug.trace();
        if (book == null) return;

        JFileChooser jc = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", "png", "jpg", "gif");
        jc.addChoosableFileFilter(filter);
        jc.setFileFilter(filter);
        jc.setDialogTitle("Select coverart image");
        int r = jc.showOpenDialog(this);

        if (r == JFileChooser.APPROVE_OPTION) {
            File src = jc.getSelectedFile();
            if (src.exists()) {
                File dest = null;
                File bookFolder = new File(Options.get("path.storage"), book.getName());
                if (src.getName().endsWith(".png")) {
                    dest = new File(bookFolder, "coverart.png");
                } else if (src.getName().endsWith(".jpg")) {
                    dest = new File(bookFolder, "coverart.jpg");
                } else if (src.getName().endsWith(".gif")) {
                    dest = new File(bookFolder, "coverart.gif");
                }

                if (dest == null) {
                    JOptionPane.showMessageDialog(this, "Invalid image format or filename", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    new File(bookFolder, "coverart.png").delete();
                    new File(bookFolder, "coverart.jpg").delete();
                    new File(bookFolder, "coverart.gif").delete();

                    Files.copy(src.toPath(), dest.toPath());

                    ImageIcon i = new ImageIcon(dest.getAbsolutePath());
                    Image ri = Utils.getScaledImage(i.getImage(), 22, 22);
                    book.setIcon(new ImageIcon(ri));
                    bookTreeModel.reload(book);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void updateWaveform() {
        updateWaveform(false);
    }

    public void updateWaveform(boolean force) {
        Debug.trace();
        if (selectedSentence != null) {
            if ((!force) && (sampleWaveform.getId() != null) && (sampleWaveform.getId().equals(selectedSentence.getId()))) return;
            sampleWaveform.setId(selectedSentence.getId());
            if (rawAudio.isSelected()) {
                sampleWaveform.setData(selectedSentence.getRawAudioData());
            } else {
                sampleWaveform.setData(selectedSentence.getDoubleAudioData(effectsEnabled));
            }
        }
    }

    public void loadEffects() {
        Debug.trace();
        effects = new TreeMap<String,EffectGroup>();
        loadEffectsFromFolder(new File(Options.get("path.storage"), "System"));
        if (book != null) {
            loadEffectsFromFolder(new File(Options.get("path.storage"), book.getName()));
        }
        updateEffectChains();
    }

    public void loadEffectsFromFolder(File dir) {
        Debug.trace();
        if (dir == null) return;
	if (!dir.exists()) return;
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.getName().endsWith(".eff")) {
                EffectGroup g = loadEffect(f);
                if (g != null) {
                    String fn = f.getName().replace(".eff","");
                    effects.put(fn, g);
                }
            }
        }
    }

    public EffectGroup loadEffect(File xml) {
        Debug.trace();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xml);

            Element root = document.getDocumentElement();
            if (root.getTagName().equals("effect")) {
                EffectGroup g = loadEffectGroup(root);
                return g;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public EffectGroup loadEffectGroup(Element root) {
        Debug.trace();
        EffectGroup group = new EffectGroup(root.getAttribute("name"));
        NodeList kids = root.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node kid = kids.item(i);
            if (kid instanceof Element) {
                Element e = (Element)kid;
                if (e.getTagName().equals("biquad")) {
                    Effect eff = (Effect)loadBiquad(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                } else if (e.getTagName().equals("delayline")) {
                    Effect eff = (Effect)loadDelayLine(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                } else if (e.getTagName().equals("pan")) {
                    Effect eff = (Effect)loadPan(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                } else if (e.getTagName().equals("amplifier")) {
                    Effect eff = (Effect)loadAmplifier(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                } else if (e.getTagName().equals("chain")) {
                    Effect eff = (Effect)loadChain(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                } else if (e.getTagName().equals("group")) {
                    Effect eff = (Effect)loadEffectGroup(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                } else if (e.getTagName().equals("lfo")) {
                    Effect eff = (Effect)loadLFO(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                } else if (e.getTagName().equals("agc")) {
                    Effect eff = (Effect)loadAGC(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                } else if (e.getTagName().equals("clipping")) {
                    Effect eff = (Effect)loadClipping(e);
                    if (eff != null) {
                        group.addEffect(eff);
                    }
                }
            }
        }
        return group;
    }

    public Biquad loadBiquad(Element root) {
        Debug.trace();
        String type = root.getAttribute("type").toLowerCase();
        Biquad bq = new Biquad();

        if (type.equals("lowpass")) {
            bq.setType(Biquad.Lowpass);
        } else if (type.equals("highpass")) {
            bq.setType(Biquad.Highpass);
        } else if (type.equals("bandpass")) {
            bq.setType(Biquad.Bandpass);
        } else if (type.equals("notch")) {
            bq.setType(Biquad.Notch);
        }  else if (type.equals("peak")) {
            bq.setType(Biquad.Peak);
        } else if (type.equals("lowshelf")) {
            bq.setType(Biquad.Lowshelf);
        } else if (type.equals("highshelf")) {
            bq.setType(Biquad.Highshelf);
        } else {
            System.err.println("Bad Biquad type: " + type);
            return null;
        }

        bq.setQ(Utils.s2d(root.getAttribute("q")));
        bq.setFc(Utils.s2d(root.getAttribute("fc")));
        bq.setPeakGain(Utils.s2d(root.getAttribute("gain")));
        return bq;
    }

    public DelayLine loadDelayLine(Element root) {
        Debug.trace();
        DelayLine line = new DelayLine();
    
        NodeList list = root.getChildNodes();
        if (Utils.s2b(root.getAttribute("wetonly"))) {
            line.setWetOnly(true);
        }

        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n instanceof Element) {
                Element e = (Element)n;
                if (e.getTagName().equals("delay")) {
                    int samples = Utils.s2i(e.getAttribute("samples"));
                    double gain = Utils.s2d(e.getAttribute("gain"));
                    double pan = Utils.s2d(e.getAttribute("pan"));
                    DelayLineStore store = line.addDelayLine(samples, gain, pan);


                    NodeList inner = e.getChildNodes();
                    for (int j = 0; j < inner.getLength(); j++) {
                        Node in = inner.item(j);
                        if (in instanceof Element) {
                            Element ie = (Element)in;

                            if (ie.getTagName().equals("biquad")) {
                                Effect eff = (Effect)loadBiquad(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            } else if (ie.getTagName().equals("delayline")) {
                                Effect eff = (Effect)loadDelayLine(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            } else if (ie.getTagName().equals("pan")) {
                                Effect eff = (Effect)loadPan(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            } else if (ie.getTagName().equals("amplifier")) {
                                Effect eff = (Effect)loadAmplifier(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            } else if (ie.getTagName().equals("chain")) {
                                Effect eff = (Effect)loadChain(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            } else if (ie.getTagName().equals("group")) {
                                Effect eff = (Effect)loadEffectGroup(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            } else if (ie.getTagName().equals("lfo")) {
                                Effect eff = (Effect)loadLFO(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            } else if (ie.getTagName().equals("agc")) {
                                Effect eff = (Effect)loadAGC(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            } else if (ie.getTagName().equals("clipping")) {
                                Effect eff = (Effect)loadClipping(ie);
                                if (eff != null) {
                                    store.addEffect(eff);
                                }
                            }
                        }
                    }
                }
            }
        }

        return line;
    }

    public Amplifier loadAmplifier(Element root) {
        Debug.trace();
        Amplifier a = new Amplifier(Utils.s2d(root.getAttribute("gain")));
        return a;
    }

    public Chain loadChain(Element root) {
        Debug.trace();
        Chain c = new Chain(root.getAttribute("src"));
        return c;
    }

    public Pan loadPan(Element root) {
        Debug.trace();
        Pan p = new Pan(Utils.s2d(root.getAttribute("pan")));
        return p;
    }

    public Clipping loadClipping(Element root) {
        Debug.trace();
        Clipping c = new Clipping(Utils.s2d(root.getAttribute("clip")));
        return c;
    }

    public LFO loadLFO(Element root) {
        Debug.trace();
        double f = Utils.s2d(root.getAttribute("frequency"));
        double d = Utils.s2d(root.getAttribute("depth"));
        double p = Utils.s2d(root.getAttribute("phase"));
        double dty = Math.PI;
        String waveform = root.getAttribute("waveform");
        if (waveform == null) {
            waveform = "sine";
        }

        int w = LFO.SINE;

        switch (waveform.toLowerCase()) {
            case "sine": w = LFO.SINE; break;
            case "cosine": w = LFO.COSINE; break;
            case "square": w = LFO.SQUARE; break;
            case "triangle": w = LFO.TRIANGLE; break;
            case "sawtooth": w = LFO.SAWTOOTH; break;
        }

        int m = LFO.ADD;

        String mode = root.getAttribute("mode");

        if (mode == null) {
            mode = "add";
        }

        switch (mode.toLowerCase()) {
            case "add": m = LFO.ADD; break;
            case "replace": m = LFO.REPLACE; break;
        }

        if (root.getAttribute("duty") != null) {
            int di = Utils.s2i(root.getAttribute("duty")); // 0-100;
            dty = (Math.PI * 2) * ((double)di / 100d);
        }
        return new LFO(f, d, p, w, dty, m);
    }

    public AGC loadAGC(Element root) {
        Debug.trace();
        double ceiling = Utils.s2d(root.getAttribute("ceiling"));
        double limit = Utils.s2d(root.getAttribute("limit"));
        double attack = Utils.s2d(root.getAttribute("attack"));
        double decay = Utils.s2d(root.getAttribute("decay"));
        if (ceiling < 0.0001d) {
            ceiling = 0.708d; // -3dB
        }
        if (limit < 0.0001d) {
            limit = 1d; // No gain
        }
        AGC agc = new AGC(ceiling, attack, decay, limit);
        return agc;
    }

    public void updateEffectChains() {
        Debug.trace();
        int sel = effectChain.getSelectedIndex();
        KVPair<String, String> ent = effectChain.getItemAt(sel);
        while (effectChain.getItemCount() > 0) {
            effectChain.removeItemAt(0);
        }

        KVPair<String, String> none = new KVPair<String, String>("none", "None"); 
        effectChain.addItem(none);
        for (String k : effects.keySet()) {
            Effect e = effects.get(k);
            KVPair<String, String> p = new KVPair<String, String>(k, e.toString());
            effectChain.addItem(p);
        }
        if (ent != null) {
            setEffectChain(ent.getKey());
        } else {
            setEffectChain(book.getDefaultEffect());
        }
    }

    public void setEffectChain(String key) {
        Debug.trace();
        for (int i = 0; i < effectChain.getItemCount(); i++) {
            KVPair<String, String> p = effectChain.getItemAt(i);
            if (p.getKey().equals(key)) {
                effectChain.setSelectedIndex(i);
                updateWaveform(true);
                return;
            }
        }

        if (effects.get(book.getDefaultEffect()) != null) {
            setEffectChain(book.getDefaultEffect());
            updateWaveform(true);
        } else {
            effectChain.setSelectedIndex(0);
            updateWaveform(true);
        }
    }

    public String getDefaultEffectsChain() {
        Debug.trace();
        return book.getDefaultEffect();
    }

    public synchronized boolean getLock() {
        Debug.trace();
        if (state == RECORDING) return false;

        int counts = 0;
        while (state == STOPPING) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            counts++;
            if (counts > 100) return false;
        }

        state = RECORDING;
        
        return true;
    }

    public void freeLock() {
        Debug.trace();
        state = IDLE;
    }

    public void stopLock() {
        Debug.trace();
        state = STOPPING;
    }

    public void toggleSplitMode() {
        Debug.trace();
        selectCutMode.setSelected(false);
        if (selectedSentence != null) {
            sampleWaveform.setDisplaySplit(selectSplitMode.isSelected());
        }
        doCutSplit.setEnabled(selectSplitMode.isSelected());
        toolBar.enablePlayTo(selectSplitMode.isSelected());
    }

    public void toggleCutMode() {
        Debug.trace();
        selectSplitMode.setSelected(false);
        if (selectedSentence != null) {
            sampleWaveform.setDisplayCut(selectCutMode.isSelected());
        }
        doCutSplit.setEnabled(selectCutMode.isSelected());
        toolBar.enablePlayTo(selectCutMode.isSelected());
    }

    public void doCut(int start, int end) {
        Debug.trace();
        try {
            double[][] samples = selectedSentence.getRawAudioData();
            double[][] croppedSamples = new double[2][samples[Sentence.LEFT].length - (end - start)];

            int a = 0;
            for (int i = 0; i < samples[Sentence.LEFT].length; i++) {
                if ((i < start) || (i > end)) {
                    croppedSamples[Sentence.LEFT][a] = samples[Sentence.LEFT][i];
                    croppedSamples[Sentence.RIGHT][a] = samples[Sentence.RIGHT][i];
                    a++;
                }
            }
            selectedSentence.writeAudioData(croppedSamples);
            selectedSentence.autoTrimSample();
            updateWaveform(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void doSplit(int at) {
        Debug.trace();
        try {
            if (selectedSentence == null) {
                System.err.println("Selected sentence is NULL in split. That CANNOT happen!");
                return;
            }
            Chapter c = (Chapter)selectedSentence.getParent();
            Sentence newSentence = selectedSentence.cloneSentence();
            int idx = bookTreeModel.getIndexOfChild(c, selectedSentence);
            bookTreeModel.insertNodeInto(newSentence, c, idx);

            double[][] samples = selectedSentence.getRawAudioData();
            double[][] startSamples = new double[2][at];
            double[][] endSamples = new double[2][samples[Sentence.LEFT].length - at];

            int a = 0; 
            int b = 0;

            for (int i = 0; i < samples[Sentence.LEFT].length; i++) {
                if (i < at) {
                    startSamples[Sentence.LEFT][a] = samples[Sentence.LEFT][i];
                    startSamples[Sentence.RIGHT][a] = samples[Sentence.RIGHT][i];
                    a++;
                } else {
                    endSamples[Sentence.LEFT][b] = samples[Sentence.LEFT][i];
                    endSamples[Sentence.RIGHT][b] = samples[Sentence.RIGHT][i];
                    b++;
                }
            }

            newSentence.writeAudioData(startSamples);
            newSentence.setPostGapType("continuation");
            newSentence.setPostGap(Options.getInteger("catenation.short-sentence"));

            selectedSentence.writeAudioData(endSamples);
            selectedSentence.autoTrimSample();
            newSentence.autoTrimSample();
            updateWaveform(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void executeCutOrSplit() {
        Debug.trace();
        int start = sampleWaveform.getCutStart();
        int end = sampleWaveform.getCutEnd();
        if (selectCutMode.isSelected()) {
            doCut(start, end);
        } else if (selectSplitMode.isSelected()) {
            doSplit(start);
        }
        selectCutMode.setSelected(false);
        selectSplitMode.setSelected(false);
        toolBar.enablePlayTo(false);
        doCutSplit.setEnabled(false);
    }

    public void setEffectsEnabled(boolean b) {
        Debug.trace();
        effectsEnabled = b;
        System.err.println("Effects Enabled: " + b);
    }

    public void setBookNotes(String text) {
        Debug.trace();
        bookNotesArea.setText(text);
    }

    public void setChapterNotes(String text) {
        Debug.trace();
        chapterNotesArea.setText(text);
    }

    public void setSentenceNotes(String text) {
        Debug.trace();
        sentenceNotesArea.setText(text);
    }

    public String getBookNotes() {
        Debug.trace();
        return bookNotesArea.getText();
    }

    public String getChapterNotes() {
        Debug.trace();
        return chapterNotesArea.getText();
    }

    public String getSentenceNotes() {
        Debug.trace();
        return sentenceNotesArea.getText();
    }

    public void openManuscript() {
        Debug.trace();
        if (book == null) return;
        File ms = book.getManuscript();
        if (ms == null) return;
        try {
            Desktop.getDesktop().open(ms);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void loadManuscript() {
        Debug.trace();
        if (book == null) return;

        JFileChooser jc = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Document Files", "doc", "docx", "pdf", "odt");
        jc.addChoosableFileFilter(filter);
        jc.setFileFilter(filter);
        jc.setDialogTitle("Select manuscript");
        int r = jc.showOpenDialog(this);

        if (r == JFileChooser.APPROVE_OPTION) {
            File src = jc.getSelectedFile();
            if (src.exists()) {
                book.setManuscript(src);
            }
        }
    }

    //* DocumentListener

    public void changedUpdate(DocumentEvent e) {
        Debug.trace();
        javax.swing.text.Document doc = e.getDocument();
        if (doc == chapterNotesArea.getDocument()) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();
            if (selectedNode instanceof Sentence) {
                selectedNode = (DefaultMutableTreeNode)selectedNode.getParent();
            }
            if (! (selectedNode instanceof Chapter)) {
                return;
            }
            Chapter c = (Chapter)selectedNode;
            c.setNotes(chapterNotesArea.getText());
        } else if (doc == sentenceNotesArea.getDocument()) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();
            if (! (selectedNode instanceof Sentence)) {
                return;
            }
            Sentence s = (Sentence)selectedNode;
            s.setNotes(sentenceNotesArea.getText());
        }
    }

    public void removeUpdate(DocumentEvent e) {
        Debug.trace();
        javax.swing.text.Document doc = e.getDocument();
        if (doc == chapterNotesArea.getDocument()) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();
            if (selectedNode instanceof Sentence) {
                selectedNode = (DefaultMutableTreeNode)selectedNode.getParent();
            }
            if (! (selectedNode instanceof Chapter)) {
                return;
            }
            Chapter c = (Chapter)selectedNode;
            c.setNotes(chapterNotesArea.getText());
        } else if (doc == sentenceNotesArea.getDocument()) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();
            if (! (selectedNode instanceof Sentence)) {
                return;
            }
            Sentence s = (Sentence)selectedNode;
            s.setNotes(sentenceNotesArea.getText());
        }
    }

    public void insertUpdate(DocumentEvent e) {
        Debug.trace();
        javax.swing.text.Document doc = e.getDocument();
        if (doc == chapterNotesArea.getDocument()) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();
            if (selectedNode instanceof Sentence) {
                selectedNode = (DefaultMutableTreeNode)selectedNode.getParent();
            }
            if (! (selectedNode instanceof Chapter)) {
                return;
            }
            Chapter c = (Chapter)selectedNode;
            c.setNotes(chapterNotesArea.getText());
        } else if (doc == sentenceNotesArea.getDocument()) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();
            if (! (selectedNode instanceof Sentence)) {
                return;
            }
            Sentence s = (Sentence)selectedNode;
            s.setNotes(sentenceNotesArea.getText());
        }
    }

    // DocumentListener *//

    public boolean sentenceIdExists(String id) {
        for (Enumeration c = book.children(); c.hasMoreElements();) {
            Chapter chp = (Chapter)c.nextElement();
            for (Enumeration s = chp.children(); s.hasMoreElements();) {
                Sentence snt = (Sentence)s.nextElement();
                if (snt.getId().equals(id)) return true;
            }
        }
        return false;
    }

    public void gatherOrphans() {
        Chapter orphans = getChapterById("orphans");
        if (orphans == null) {
            orphans = new Chapter("orphans", "Orphan Files");
            bookTreeModel.insertNodeInto(orphans, book, book.getChildCount());
        }
        File bookRoot = new File(Options.get("path.storage"), book.getName());
        File[] files = new File(bookRoot, "files").listFiles();
        for (File f : files) {
            String filename = f.getName();
            if (filename.startsWith(".")) continue;
            if (filename.startsWith("backup")) continue;
            if (filename.equals("room-noise.wav")) continue;
            if (filename.endsWith(".wav")) {
                String id = filename.substring(0, filename.length() - 4);
                Debug.d("Testing orphanicity of", id);
                if (!sentenceIdExists(id)) {
                    Sentence newSentence = new Sentence(id, id);
                    bookTreeModel.insertNodeInto(newSentence, orphans, orphans.getChildCount());
                }
            }
        }
    }

    public Chapter getChapterById(String id) {
        for (Enumeration c = book.children(); c.hasMoreElements();) {
            Chapter chp = (Chapter)c.nextElement();
            if (chp.getId().equals(id)) return chp;
        }
        return null;
    }

}
