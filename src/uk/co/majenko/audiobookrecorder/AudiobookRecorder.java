package uk.co.majenko.audiobookrecorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.lang.reflect.Method;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioInputStream;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class AudiobookRecorder extends JFrame implements DocumentListener {

    // Settings - tweakable

    static Properties config = new Properties();

    public final static int IDLE = 0;
    public final static int RECORDING = 1;
    public final static int STOPPING = 2;
    public int state = IDLE;

    public static CommandLine CLI = new CommandLine();

    MainToolBar toolBar;

    JMenuBar menuBar;
    JMenu fileMenu;
    JMenu toolsMenu;
    JMenu helpMenu;

    JMenuItem fileNewBook;
    JMenuItem fileOpenBook;
    JMenuItem fileSave;
    JMenuItem fileExit;
    JMenuItem fileOpenArchive;
    JMenuItem fileOptions;

    JMenuItem toolsCoverArt;
    JMenuItem toolsManuscript;

    FlashPanel centralPanel;

    JPanel statusBar;

    NoiseFloor noiseFloorLabel;

    JScrollPane mainScroll;

    DefaultMutableTreeNode rootNode = null;

    JTree bookTree;
    public DefaultTreeModel bookTreeModel;

    Sentence recording = null;
    Sentence playing = null;

    static Sentence selectedSentence = null;
    static Chapter selectedChapter = null;
    static Book selectedBook = null;

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

    JButtonSpacePlay refreshSentence;

    JComboBox<KVPair<String,String>> effectChain;

    Thread playingThread = null;

    Random rng = new Random();

    SourceDataLine play = null;

    boolean effectsEnabled = true;

    public static AudiobookRecorder window;

    public Queue<Runnable>processQueue = null;
    public QueueMonitor queueMonitor = null;

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
                createNewBook();
            }
        });
        fileOpenBook = new JMenuItem("Open Book...");
        fileOpenBook.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                openBook();
            }
        });
        fileSave = new JMenuItem("Save Book");
        fileSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                if (getBook() != null) {
                    saveBook(getBook());
                }
            }
        });

        fileOpenArchive = new JMenuItem("Open Archive...");
        fileOpenArchive.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                openArchive();
            }
        });

        JMenuItem openOld = new JMenuItem("Import old audiobook...");
        openOld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                importOldStyleBook();
            }
        });

        fileOptions = new JMenuItem("Options");
        fileOptions.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                new Options(AudiobookRecorder.this);
            }
        });

        fileExit = new JMenuItem("Exit");
        fileExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                saveAllBooks();
                System.exit(0);
            }
        });

        fileMenu.add(fileNewBook);
        fileMenu.add(fileOpenBook);
        fileMenu.add(fileOpenArchive);
        fileMenu.add(openOld);
        fileMenu.add(fileSave);
        fileMenu.addSeparator();
        fileMenu.add(fileOptions);
        fileMenu.addSeparator();
        fileMenu.add(fileExit);

        menuBar.add(fileMenu);

        helpMenu = new JMenu("Help");

        helpMenu.add(new JMenuObject("Website", null, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                Utils.browse("https://majenkoprojects.github.io/AudiobookRecorder/index.html");
            }
        }));

        helpMenu.add(new JMenuObject("About AudiobookRecorder", null, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                JOptionPane.showMessageDialog(AudiobookRecorder.this, new AboutPanel(), "About AudiobookRecorder", JOptionPane.PLAIN_MESSAGE);
            }
        }));

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

        processQueue = new ArrayDeque<Runnable>();

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

        queueMonitor = new QueueMonitor(processQueue);

        for (int i = 0; i < Options.getInteger("process.threads"); i++) {
            WorkerThread worker = new WorkerThread(processQueue, queueMonitor);
            queueMonitor.addThread(worker);
            worker.start();
        }


        execScript(Options.get("scripts.startup"));

        CacheManager.setCacheSize(Options.getInteger("cache.size"));

        setLayout(new BorderLayout());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                Debug.trace();
                saveAllBooks();
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
                    queueJob(new SentenceJob(selectedSentence) {
                        public void run() {
                            sentence.autoTrimSampleFFT();
                            updateWaveformMarkers();
                        }
                    });
                }
            }
        });

        reprocessAudioPeak = new JButtonSpacePlay(Icons.peak, "Autotrim Audio (Peak)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.trace();
                if (selectedSentence != null) {
                    queueJob(new SentenceJob(selectedSentence) {
                        public void run() {
                            sentence.autoTrimSamplePeak();
                            updateWaveformMarkers();
                        }
                    });
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

        gainPercent = new JSpinner(new SteppedNumericSpinnerModel(0, 1000, 1, 100));
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
                selectedSentence.reloadTree();
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
                selectedSentence.reloadTree();
            }
        });

        refreshSentence = new JButtonSpacePlay(Icons.refresh, "Refresh Phrase Data", new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                Debug.trace();
                if (selectedSentence == null) return;
                selectedSentence.refreshAllData();
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
    
        controlsTop.add(refreshSentence);


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
        statusBar.setLayout(new FlowLayout(FlowLayout.RIGHT));
        add(statusBar, BorderLayout.SOUTH);

        noiseFloorLabel = new NoiseFloor();

        statusBar.add(noiseFloorLabel);
//        statusBar.add(Box.createHorizontalStrut(2));
        statusBar.add(queueMonitor);

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
                if (getBook().getNoiseFloor() == 0) {
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
                if (getBook().getNoiseFloor() == 0) {
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
                if (getBook().getNoiseFloor() == 0) {
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
                if (getBook().getNoiseFloor() == 0) {
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
                if (getBook().getNoiseFloor() == 0) {
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

        rootNode = new DefaultMutableTreeNode("Books");
        bookTreeModel = new DefaultTreeModel(rootNode);
        bookTree = new JTree(bookTreeModel);
        bookTree.setUI(new CustomTreeUI(mainScroll));
        bookTree.setRootVisible(false);
        mainScroll.setViewportView(bookTree);

        TreeCellRenderer renderer = new BookTreeRenderer();
        try {
            bookTree.setCellRenderer(renderer);
        } catch (Exception ex) {
            bookTree.setCellRenderer(renderer);
        }

        InputMap im = bookTree.getInputMap(JComponent.WHEN_FOCUSED);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "startStopPlayback");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.SHIFT_DOWN_MASK), "startPlaybackFrom");

        bookTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                Debug.trace();

                DefaultMutableTreeNode n = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();

                if (n instanceof BookTreeNode) {
                    BookTreeNode btn = (BookTreeNode)n;
                    btn.onSelect(btn);
                }

                if (n instanceof Sentence) {
                    Sentence s = (Sentence)n;
                    //selectedSentence = s;
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
                    //selectedSentence = null;
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


        bookNotesArea = new JTextArea();
        bookNotesArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        bookNotesArea.getDocument().addDocumentListener(this);
        bookNotesArea.setLineWrap(true);
        bookNotesArea.setWrapStyleWord(true);
        bookNotesScroll = new JScrollPane();
        bookNotesScroll.setViewportView(bookNotesArea);
        bookNotesArea.setCaretColor(new Color(20, 20, 20));
        bookNotesArea.setForeground(new Color(20, 20, 20));
        bookNotesArea.setBackground(new Color(224, 211, 175));

        chapterNotesArea = new JTextArea();
        chapterNotesArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        chapterNotesArea.getDocument().addDocumentListener(this);
        chapterNotesArea.setLineWrap(true);
        chapterNotesArea.setWrapStyleWord(true);
        chapterNotesScroll = new JScrollPane();
        chapterNotesScroll.setViewportView(chapterNotesArea);
        chapterNotesArea.setCaretColor(new Color(20, 20, 20));
        chapterNotesArea.setForeground(new Color(20, 20, 20));
        chapterNotesArea.setBackground(new Color(224, 211, 175));

        sentenceNotesArea = new JTextArea();
        sentenceNotesArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        sentenceNotesArea.getDocument().addDocumentListener(this);
        sentenceNotesArea.setLineWrap(true);
        sentenceNotesArea.setWrapStyleWord(true);
        sentenceNotesScroll = new JScrollPane();
        sentenceNotesScroll.setViewportView(sentenceNotesArea);
        sentenceNotesArea.setCaretColor(new Color(20, 20, 20));
        sentenceNotesArea.setForeground(new Color(20, 20, 20));
        sentenceNotesArea.setBackground(new Color(224, 211, 175));

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
                    Debug.trace();
                    bookTreeModel.reload(rootNode);
                    TreePath path = null;
                    if (selectedSentence != null) {
                        path = new TreePath(selectedSentence.getPath());
                    } else if (selectedChapter != null) {
                        path = new TreePath(selectedChapter.getPath());
                    } else if (selectedBook != null) {
                        path = new TreePath(selectedBook.getPath());
                    }
                    if (path != null) {
                        bookTree.expandPath(path);
                        bookTree.setSelectionPath(path);
                        bookTree.scrollPathToVisible(path);
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

        String books = Options.get("system.open-books");
        if (books != null) {
            String[] paths = books.split("::");
            for (String path : paths) {
                File f = new File(path);
                if (f.exists()) {
                    if (f.getName().equals("audiobook.abx")) {
                        try {
                            Book b = new Book(f);
                            rootNode.add(b);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    bookTreeModel.reload(rootNode);
                    expandAllBooks();
                }
            });
        }

        queueJob(new VersionChecker());

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
        Properties props = System.getProperties();
        props.setProperty("sun.java2d.opengl", "true");
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
            Book newbook = new Book(info.getTitle().trim());
            newbook.setAuthor(info.getAuthor().trim());
            newbook.setGenre(info.getGenre().trim());
            newbook.setComment(info.getComment().trim());
            newbook.setACX(info.getACX().trim());

            Chapter caud = new Chapter("audition", "Audition");
            Chapter copen = new Chapter("open", "Opening Credits");
            Chapter cclose = new Chapter("close", "Closing Credits");
            Chapter cone = new Chapter(UUID.randomUUID().toString(), "Chapter 1");

            newbook.add(caud);
            newbook.add(copen);
            newbook.add(cclose);

            newbook.add(cone);

            newbook.save();

            rootNode.add(newbook);
            bookTreeModel.reload(rootNode);
            bookTree.expandPath(new TreePath(newbook.getPath()));

            updateOpenBookList();
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

    @SuppressWarnings("unchecked")
    void treePopup(MouseEvent e) {
        Debug.trace();
        int selRow = bookTree.getRowForLocation(e.getX(), e.getY());
        TreePath selPath = bookTree.getPathForLocation(e.getX(), e.getY());
        if (selRow != -1) {

            DefaultMutableTreeNode node = (DefaultMutableTreeNode)selPath.getLastPathComponent();
            bookTree.setSelectionPath(new TreePath(node.getPath()));

            if (node instanceof Sentence) {
                Sentence s = (Sentence)node;
                Book book = s.getBook();

                JPopupMenu menu = new JPopupMenu();

                JMenuObject rec = new JMenuObject("Recognise text from audio", s, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Sentence s = (Sentence)o.getObject();
                        if (!s.isLocked()) {
                            queueJob(new SentenceJob(s) {
                                public void run() {
                                    sentence.doRecognition();
                                }
                            });
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
                        sent.reloadTree();
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
                        sent.reloadTree();
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
                        sent.reloadTree();
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
                        sent.reloadTree();
                    }
                });
                setGapType.add(gapTypeSection);

                JMenuObject insa = new JMenuObject("Insert phrase above", s, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Sentence s = (Sentence)o.getObject();
                        Chapter c = (Chapter)s.getParent();
                        Sentence newSentence = new Sentence();
                        int where = bookTreeModel.getIndexOfChild(c, s);
                        bookTreeModel.insertNodeInto(newSentence, c, where);
                        bookTreeModel.reload(newSentence);
                    }
                });

                JMenuObject insb = new JMenuObject("Insert phrase below", s, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Sentence s = (Sentence)o.getObject();
                        Chapter c = (Chapter)s.getParent();
                        Sentence newSentence = new Sentence();
                        int where = bookTreeModel.getIndexOfChild(c, s);
                        bookTreeModel.insertNodeInto(newSentence, c, where + 1);
                        bookTreeModel.reload(newSentence);
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
                            queueJob(new SentenceExternalJob(s, Utils.s2i(o.getActionCommand())));
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
                menu.addSeparator();
                menu.add(insa);
                menu.add(insb);
                menu.addSeparator();
                menu.add(del);
                menu.addSeparator();
                menu.add(dup);
                menu.show(bookTree, e.getX(), e.getY());
            } else if (node instanceof Chapter) {
                Chapter c = (Chapter)node;
                Book book = c.getBook();
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
                        Chapter c = (Chapter)o.getObject();
                        for (Enumeration s = c.children(); s.hasMoreElements();) {
                            Sentence snt = (Sentence)s.nextElement();
                            if (!snt.isProcessed()) {
                                    queueJob(new SentenceJob(snt) {
                                    public void run() {
                                        sentence.autoTrimSampleFFT();
                                        updateWaveformMarkers();
                                    }
                                });
                            }
                        }

                    }
                });

                JMenuObject fftnew = new JMenuObject("Auto-trim new (FFT)", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter c = (Chapter)o.getObject();
                        for (Enumeration s = c.children(); s.hasMoreElements();) {
                            Sentence snt = (Sentence)s.nextElement();
                            if (!snt.isProcessed()) {
                                queueJob(new SentenceJob(snt) {
                                    public void run() {
                                        sentence.autoTrimSampleFFT();
                                        updateWaveformMarkers();
                                    }
                                });
                            }
                        }
                    }
                });

                JMenuObject peak = new JMenuObject("Auto-trim all (Peak)", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter c = (Chapter)o.getObject();
                        for (Enumeration s = c.children(); s.hasMoreElements();) {
                            Sentence snt = (Sentence)s.nextElement();
                            queueJob(new SentenceJob(snt) {
                                public void run() {
                                    sentence.autoTrimSamplePeak();
                                    updateWaveformMarkers();
                                }
                            });
                        }
                    }
                });

                JMenuObject fft = new JMenuObject("Auto-trim all (FFT)", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter c = (Chapter)o.getObject();
                        for (Enumeration s = c.children(); s.hasMoreElements();) {
                            Sentence snt = (Sentence)s.nextElement();
                            queueJob(new SentenceJob(snt) {
                                public void run() {
                                    sentence.autoTrimSampleFFT();
                                    updateWaveformMarkers();
                                }
                            });
                        }
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
                        int pos = bookTreeModel.getIndexOfChild(chap.getBook(), chap);
                        if (pos > 0) pos--;
                        Chapter prevChap = (Chapter)bookTreeModel.getChild(chap.getBook(), pos);
                        bookTreeModel.removeNodeFromParent(chap);
                        bookTreeModel.insertNodeInto(chap, chap.getBook(), pos);
                    }
                });

                JMenuObject moveDown = new JMenuObject("Move Down", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter chap = (Chapter)o.getObject();
                        int pos = bookTreeModel.getIndexOfChild(chap.getBook(), chap);
                        pos++;
                        Chapter nextChap = (Chapter)bookTreeModel.getChild(chap.getBook(), pos);
                        if (nextChap != null) {
                            bookTreeModel.removeNodeFromParent(chap);
                            bookTreeModel.insertNodeInto(chap, chap.getBook(), pos);
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
                            moveSentences(source, target);
                            bookTreeModel.reload(source);
                            bookTreeModel.reload(target);
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
                            snt.reloadTree();
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
                            snt.reloadTree();
                        }
                    }
                });

                JMenuObject importWav = new JMenuObject("Import WAV file...", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter chap = (Chapter)o.getObject();
                        importWavFile(chap);
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
                                    queueJob(new SentenceJob(snt) {
                                        public void run() {
                                            sentence.doRecognition();
                                        }
                                    });
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
                                    queueJob(new SentenceExternalJob(snt, Utils.s2i(o.getActionCommand())));
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
                menu.add(importWav);
                menu.add(exportChapter);
                menu.addSeparator();
                menu.add(deleteChapter);
                menu.addSeparator();
                menu.add(external);

                menu.show(bookTree, e.getX(), e.getY());
            } else if (node instanceof Book) {
                Book book = (Book)node;
                JPopupMenu menu = new JPopupMenu();

                menu.add(new JMenuObject("Add chapter", book, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject src = (JMenuObject)(e.getSource());
                        Book thisBook = (Book)(src.getObject());
                        addChapter(thisBook);
                    }
                }));
            
                menu.addSeparator();

                menu.add(new JMenuObject("Edit Data...", book, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject src = (JMenuObject)(e.getSource());
                        Book thisBook = (Book)(src.getObject());
                        editBookInfo(thisBook);
                    }
                }));
                
                menu.add(new JMenuObject("Reset all post gaps", book, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject src = (JMenuObject)(e.getSource());
                        Book thisBook = (Book)(src.getObject());
                        for (Enumeration ch = book.children(); ch.hasMoreElements();) {
                            Chapter chap = (Chapter)ch.nextElement();
                            chap.resetPostGaps();
                        }
                    }
                }));

                menu.add(new JMenuObject("Merge book...", book, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject src = (JMenuObject)(e.getSource());
                        Book thisBook = (Book)(src.getObject());
                        mergeBook(thisBook);
                    }
                }));

                menu.addSeparator();

                menu.add(new JMenuObject("Export All Audio", book, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject src = (JMenuObject)(e.getSource());
                        Book thisBook = (Book)(src.getObject());
                        exportAudio(thisBook);
                    }
                }));

                JMenu visitACX = new JMenu("Visit ACX");

                visitACX.add(new JMenuObject("Title", book, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject src = (JMenuObject)(e.getSource());
                        Book thisBook = (Book)(src.getObject());
                        Utils.browse("https://www.acx.com/titleview/" + thisBook.getACX() + "?bucket=CREATIVE_BRIEF");
                    }
                }));

                visitACX.add(new JMenuObject("Audition", book, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject src = (JMenuObject)(e.getSource());
                        Book thisBook = (Book)(src.getObject());
                        Utils.browse("https://www.acx.com/titleview/" + thisBook.getACX() + "?bucket=AUDITION_READY");
                    }
                }));

                visitACX.add(new JMenuObject("Produce", book, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject src = (JMenuObject)(e.getSource());
                        Book thisBook = (Book)(src.getObject());
                        Utils.browse("https://www.acx.com/titleview/" + thisBook.getACX() + "?bucket=IN_PRODUCTION");
                    }
                }));

                menu.add(visitACX);

                menu.add(new JMenuObject("Import Cover Art...", book, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject src = (JMenuObject)(e.getSource());
                        Book thisBook = (Book)(src.getObject());
                        loadCoverArt(thisBook);
                    }
                }));

                menu.add(new JMenuObject("Import Manuscript...", book, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject src = (JMenuObject)(e.getSource());
                        Book thisBook = (Book)(src.getObject());
                        loadManuscript(thisBook);
                    }
                }));

                menu.addSeparator();

                menu.add(new JMenuObject("Scan for orphan files", book, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject src = (JMenuObject)(e.getSource());
                        Book thisBook = (Book)(src.getObject());
                        findOrphans(thisBook);
                    }
                }));

                menu.add(new JMenuObject("Purge backups", book, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject src = (JMenuObject)(e.getSource());
                        Book thisBook = (Book)(src.getObject());
                        thisBook.purgeBackups();
                    }
                }));

                menu.add(new JMenuObject("Reload effects", book, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject src = (JMenuObject)(e.getSource());
                        Book thisBook = (Book)(src.getObject());
                        thisBook.loadEffects();
                    }
                }));

                menu.addSeparator();

                menu.add(new JMenuObject("Archive book", book, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject src = (JMenuObject)(e.getSource());
                        Book thisBook = (Book)(src.getObject());
                        archiveBook(thisBook);
                    }
                }));


                menu.add(new JMenuObject("Close book", book, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Debug.trace();
                        JMenuObject src = (JMenuObject)(e.getSource());
                        Book thisBook = (Book)(src.getObject());
                        closeBook(thisBook);
                    }
                }));
                        
                menu.show(bookTree, e.getX(), e.getY());
            }

        }
    }

    public void startReRecording() {
        Debug.trace();

        if (recording != null) return;
        if (getBook() == null) return;

        if (Microphone.getDevice() == null) {
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
        }
    }

    public void startRecordingShort() {
        Debug.trace();

        if (recording != null) return;
        if (getBook() == null) return;

        if (Microphone.getDevice() == null) {
            JOptionPane.showMessageDialog(this, "Microphone not started. Start microphone first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();

        if (selectedNode == null) {
            selectedNode = getBook().getLastChapter();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        if (selectedNode instanceof Book) {
            selectedNode = getBook().getLastChapter();
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
        s.setParentBook(getBook());
        bookTreeModel.insertNodeInto(s, c, c.getChildCount());

        bookTree.expandPath(new TreePath(c.getPath()));
        bookTree.setSelectionPath(new TreePath(s.getPath()));
        bookTree.scrollPathToVisible(new TreePath(s.getPath()));

        if (s.startRecording()) {
            recording = s;
        }
    }


    public void startRecordingNewParagraph() {
        Debug.trace();

        if (recording != null) return;
        if (getBook() == null) return;

        if (Microphone.getDevice() == null) {
            JOptionPane.showMessageDialog(this, "Microphone not started. Start microphone first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();

        if (selectedNode == null) {
            selectedNode = getBook().getLastChapter();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        if (selectedNode instanceof Book) {
            selectedNode = getBook().getLastChapter();
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
        s.setParentBook(getBook());
        bookTreeModel.insertNodeInto(s, c, c.getChildCount());

        bookTree.expandPath(new TreePath(c.getPath()));
        bookTree.setSelectionPath(new TreePath(s.getPath()));
        bookTree.scrollPathToVisible(new TreePath(s.getPath()));

        if (s.startRecording()) {
            recording = s;
        }
    }

    public void startRecordingNewSection() {
        Debug.trace();

        if (recording != null) return;
        if (getBook() == null) return;

        if (Microphone.getDevice() == null) {
            JOptionPane.showMessageDialog(this, "Microphone not started. Start microphone first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();

        if (selectedNode == null) {
            selectedNode = getBook().getLastChapter();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        if (selectedNode instanceof Book) {
            selectedNode = getBook().getLastChapter();
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
        s.setParentBook(getBook());
        bookTreeModel.insertNodeInto(s, c, c.getChildCount());

        bookTree.expandPath(new TreePath(c.getPath()));
        bookTree.setSelectionPath(new TreePath(s.getPath()));
        bookTree.scrollPathToVisible(new TreePath(s.getPath()));

        if (s.startRecording()) {
            recording = s;
        }
    }

    public void startRecording() {
        Debug.trace();

        if (recording != null) return;
        if (getBook() == null) return;

        if (Microphone.getDevice() == null) {
            JOptionPane.showMessageDialog(this, "Microphone not started. Start microphone first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();

        if (selectedNode == null) {
            selectedNode = getBook().getLastChapter();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        if (selectedNode instanceof Book) {
            selectedNode = getBook().getLastChapter();
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
        s.setParentBook(getBook());
        bookTreeModel.insertNodeInto(s, c, c.getChildCount());

        bookTree.expandPath(new TreePath(c.getPath()));
        bookTree.setSelectionPath(new TreePath(s.getPath()));
        bookTree.scrollPathToVisible(new TreePath(s.getPath()));

        if (s.startRecording()) {
            recording = s;
        }
    }

    public void stopRecording() {
        Debug.trace();
        if (recording == null) return;
        recording.stopRecording();

        bookTree.expandPath(new TreePath(((DefaultMutableTreeNode)recording.getParent()).getPath()));
        bookTree.setSelectionPath(new TreePath(recording.getPath()));
        bookTree.scrollPathToVisible(new TreePath(recording.getPath()));

        recording = null;
        saveBook(getBook());
    }

    public void deleteLastRecording() {
        Debug.trace();
        if (recording != null) return;
        if (getBook() == null) return;

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();

        if (selectedNode == null) {
            selectedNode = getBook().getLastChapter();
            bookTree.setSelectionPath(new TreePath(selectedNode.getPath()));
        }

        if (selectedNode instanceof Book) {
            selectedNode = getBook().getLastChapter();
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
//        selectedSentence = s;
        saveBook(getBook());
    }

    public void addChapter() {
        if (getBook() != null) {
            addChapter(getBook());
        }
    }

    public void addChapter(Book book) {
        Debug.trace();
        Chapter last = book.getLastChapter();
        String name = "Chapter " + (book.getChildCount() + 1);
        if (last != null) {
            String lastname = last.getName();
            Pattern p = Pattern.compile("^([^\\d]+)(\\d+)$");
            Matcher m = p.matcher(lastname);
            if (m.find()) {
                String chapname = m.group(1);
                int chapnum = Utils.s2i(m.group(2));
                name = chapname + (chapnum + 1);
            }
        }
        Chapter c = book.addChapter(name);   
        bookTreeModel.reload(book);
        bookTree.setSelectionPath(new TreePath(c.getPath()));
        bookTree.scrollPathToVisible(new TreePath(c.getPath()));
    } 

    public void expandAllBooks() {
        for (Enumeration s = rootNode.children(); s.hasMoreElements();) {
            TreeNode n = (TreeNode)s.nextElement();
            if (n instanceof Book) {
                Book b = (Book)n;
                bookTree.expandPath(new TreePath(b.getPath()));
            }
        }
    }

    public void saveAllBooks() {
        for (Enumeration s = rootNode.children(); s.hasMoreElements();) {
            TreeNode n = (TreeNode)s.nextElement();
            if (n instanceof Book) {
                Book b = (Book)n;
                saveBook(b);
            }
        }
    }

    public void loadXMLBookStructure(File inputFile) {
        Debug.trace();
        try {
            Book book = new Book(inputFile);
            rootNode.add(book);
            bookTreeModel.reload(rootNode);
            bookTree.expandPath(new TreePath(book.getPath()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Chapter convertChapter(String name, String id, Properties data) {
        Chapter c = new Chapter(id, data.getProperty("chapter." + name + ".name"));
        c.setPostGap(Utils.s2i(data.getProperty("chapter." + name + ".post-gap")));
        c.setPreGap(Utils.s2i(data.getProperty("chapter." + name + ".pre-gap")));

        for (int i = 0; i < 100000000; i++) {
            String sid = data.getProperty(String.format("chapter." + name + ".sentence.%08d.id", i));
            String text = data.getProperty(String.format("chapter." + name + ".sentence.%08d.text", i));
            int gap = Utils.s2i(data.getProperty(String.format("chapter." + name + ".sentence.%08d.post-gap", i)));
            if (sid == null) break;
            Sentence s = new Sentence(sid, text);
            s.setPostGap(gap);
            s.setStartOffset(Utils.s2i(data.getProperty(String.format("chapter." + name + ".sentence.%08d.start-offset", i))));
            s.setEndOffset(Utils.s2i(data.getProperty(String.format("chapter. " + name + ".sentence.%08d.end-offset", i))));
            s.setLocked(Utils.s2b(data.getProperty(String.format("chapter." + name + ".sentence.%08d.locked", i))));
            s.setAttentionFlag(Utils.s2b(data.getProperty(String.format("chapter." + name + ".sentence.%08d.attention", i))));
            s.setGain(Utils.s2d(data.getProperty(String.format("chapter." + name + ".sentence.%08d.gain", i))));
            s.setEffectChain(data.getProperty(String.format("chapter." + name + ".sentence.%08d.effect", i)));
            s.setPostGapType(data.getProperty(String.format("chapter." + name + ".sentence.%08d.gaptype", i)));
            c.add(s);
        }

        return c;
    }

    public void importOldStyleBook() {
        Debug.trace();
        JFileChooser jc = new JFileChooser(new File(Options.get("path.storage")));
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Old Audiobooks", "abk");
        jc.addChoosableFileFilter(filter);
        jc.setFileFilter(filter);
        jc.setDialogTitle("Select Old Audiobook");
        int r = jc.showOpenDialog(this);

        if (r == JFileChooser.APPROVE_OPTION) {
            File f = jc.getSelectedFile();
            if (f.exists()) {
                convertOldStyleBook(f);
            }
        }
    }

    public void convertOldStyleBook(File f) {
        Debug.trace();
        try {
            Properties data = new Properties();
            data.loadFromXML(new FileInputStream(f));
            File sourceFolder = f.getParentFile();

            Book book = new Book(data.getProperty("book.name"));
            book.setAuthor(data.getProperty("book.author"));
            book.setGenre(data.getProperty("book.genre"));
            book.setComment(data.getProperty("book.comment"));
            book.setACX(data.getProperty("book.acx"));

            book.setDefaultEffect(data.getProperty("audio.effect.default"));

            book.add(convertChapter("audition", "audition", data));
            book.add(convertChapter("open", "open", data));
            book.add(convertChapter("close", "close", data));
            for (int cno = 1; cno < 10000; cno++) {
                String oldid = String.format("%04d", cno);
                String newid = UUID.randomUUID().toString();
                if (data.getProperty("chapter." + oldid + ".name") == null) break;
                book.add(convertChapter(oldid, newid, data));
            }

            book.save();
        
            File destFolder = book.getLocation();

            // If we are importing from the storage area and nothing changes then we are done.
            if (destFolder.equals(sourceFolder)) return;

            // Otherwise we need to copy everything over.
            Utils.copyFolder(sourceFolder, destFolder);

            // Save again, just to be sure.
            book.save();

            // Add the book to the tree
            loadXMLBookStructure(new File(book.getLocation(), "audiobook.abx"));
            updateOpenBookList();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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

            if (!f.getName().endsWith(".abx")) {
                JOptionPane.showMessageDialog(this, "Not a .abx file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
                
            loadXMLBookStructure(f);
            updateOpenBookList();
        }

        
    }

    public void playSelectedSentence() {
        Debug.trace();
        if (selectedSentence == null) return;
        if (playing != null) return;
        if (getBook().getNoiseFloor() == 0) {
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
                    e.printStackTrace();
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
    public void exportAudio(Book book) {
        Debug.trace();
        
        for (Enumeration o = book.children(); o.hasMoreElements();) {
            Chapter c = (Chapter)o.nextElement();
            if (c.getChildCount() == 0) continue;
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
        if (getBook().getNoiseFloor() == 0) {
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
        if (getBook().getNoiseFloor() == 0) {
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
                            data = getBook().getRoomNoise(Utils.s2i(Options.get("catenation.pre-chapter")));
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
                            data = getBook().getRoomNoise(Utils.s2i(Options.get("catenation.post-chapter")));
                            play.write(data, 0, data.length);
                            playing = null;
                        } else {
                            data = getBook().getRoomNoise(s.getPostGap());
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

    public void mergeBook(Book toBook) {
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

            if (!f.getName().endsWith(".abx")) {
                JOptionPane.showMessageDialog(this, "Not a .abx file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                Book fromBook = new Book(f);
                mergeAllChapters(fromBook, toBook);
                bookTreeModel.reload(toBook);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void moveSentences(Chapter from, Chapter to) {
        while (from.getChildCount() > 0) {
            try {
                Sentence snt = (Sentence)from.getFirstChild();
                File source = snt.getFile();
                from.remove(snt);
                to.add(snt);
                snt.setParentBook(to.getBook());
                File destination = snt.getFile();
                if (from.getBook() != to.getBook()) {
                    Files.copy(source.toPath(), destination.toPath());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public Chapter findChapter(Book from, String id, String name) {
        Chapter c = from.getChapterById(id);
        if (c != null) return c;
        c = from.getChapterByName(name);
        if (c != null) return c;
        return null;
    }

    public void mergeAllChapters(Book from, Book to) {
        if (from.getChildCount() == 0) return;
        while (from.getChildCount() > 0) {
            Chapter fc = (Chapter)from.getFirstChild();
            Chapter tc = findChapter(to, fc.getId(), fc.getName());
            if (tc == null) {
                tc = new Chapter(fc.getId(), fc.getName());
                tc.setParentBook(to);
                to.add(tc);
            }
            moveSentences(fc, tc);
            from.remove(fc);
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
        Book book;

        public ArchiveBookThread(ProgressDialog p, Book b) {
            Debug.trace();
            pd = p;
            book = b;
        }

        public void run() {
            Debug.trace();
            try {
                String name = book.getName();
                File storageDir = new File(Options.get("path.storage"));
                File bookDir = book.getLocation();
                File archiveDir = new File(Options.get("path.archive"));

                ArrayList<File> fileList = gatherFiles(bookDir);

                if (!archiveDir.exists()) {
                    archiveDir.mkdirs();
                }

                File archiveFile = new File(archiveDir, name + ".abz");
                Debug.d("Archiving to", archiveFile.getAbsolutePath());
                if (archiveFile.exists()) {
                    archiveFile.delete();
                }

                FileOutputStream fos = new FileOutputStream(archiveFile);
                ZipOutputStream zos = new ZipOutputStream(fos);
            
                zos.putNextEntry(new ZipEntry(name + "/"));
                zos.closeEntry();

                int numFiles = fileList.size();
                int fileNo = 0;

                String prefix = bookDir.getParentFile().getAbsolutePath();

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

    public void archiveBook(Book book) {
        Debug.trace();
        int r = JOptionPane.showConfirmDialog(this, "This will stash the current book away\nin the archives folder in a compressed\nform. The existing book files will be deleted\nand the book closed.\n\nAre you sure you want to do this?", "Archive Book", JOptionPane.OK_CANCEL_OPTION);

        if (r == JOptionPane.OK_OPTION) {

            ProgressDialog pd = new ProgressDialog("Archiving book...");
            saveBook(book);

            ArchiveBookThread runnable = new ArchiveBookThread(pd, book);
            Thread t = new Thread(runnable);
            t.start();
            pd.setVisible(true);
            closeBook(book);
        }
    }

    public void closeBook(Book b) {
        if (selectedBook == b) {
            setSelectedBook(null);
            setSelectedSentence(null);
            setSelectedChapter(null);
        }
        saveBook(b);
        bookTreeModel.removeNodeFromParent(b);
        updateOpenBookList();
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
                        }
                        updateOpenBookList();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void loadCoverArt(Book book) {
        Debug.trace();

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
                File bookFolder = book.getLocation();
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
                    book.reloadTree();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void updateWaveform() {
        updateWaveform(false);
    }

    synchronized public void updateWaveform(boolean force) {
        Debug.trace();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
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
        });
    }

    synchronized public void updateWaveformMarkers() {
        if (selectedSentence != null) {
            sampleWaveform.setMarkers(selectedSentence.getStartOffset(), selectedSentence.getEndOffset());
            sampleWaveform.setAltMarkers(selectedSentence.getStartCrossing(), selectedSentence.getEndCrossing());
        }
    }

    public void updateEffectChains(TreeMap<String, EffectGroup> effs) {
        Debug.trace();
        int sel = effectChain.getSelectedIndex();
        KVPair<String, String> ent = effectChain.getItemAt(sel);
        while (effectChain.getItemCount() > 0) {
            effectChain.removeItemAt(0);
        }

        KVPair<String, String> none = new KVPair<String, String>("none", "None"); 
        effectChain.addItem(none);
        for (String k : effs.keySet()) {
            Effect e = effs.get(k);
            KVPair<String, String> p = new KVPair<String, String>(k, e.toString());
            effectChain.addItem(p);
        }
        if (ent != null) {
            setEffectChain(ent.getKey());
//        } else {
//            setEffectChain(getBook().getDefaultEffect());
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

        if (getBook().effects.get(getBook().getDefaultEffect()) != null) {
            setEffectChain(getBook().getDefaultEffect());
            updateWaveform(true);
        } else {
            effectChain.setSelectedIndex(0);
            updateWaveform(true);
        }
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
                Debug.d("Selected sentence is NULL in split. That CANNOT happen!");
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
        Debug.d("Effects Enabled:", b);
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
        if (getBook() == null) return;
        File ms = getBook().getManuscript();
        if (ms == null) return;
        try {
            Desktop.getDesktop().open(ms);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void loadManuscript(Book book) {
        Debug.trace();

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

    public void documentContentsChanged(DocumentEvent e) {
        Debug.trace();
        javax.swing.text.Document doc = e.getDocument();
        if (doc == bookNotesArea.getDocument()) {
            if (selectedBook == null) return;
            selectedBook.setNotes(bookNotesArea.getText());
        } else if (doc == chapterNotesArea.getDocument()) {
            if (selectedChapter == null) return;
            selectedChapter.setNotes(chapterNotesArea.getText());
        } else if (doc == sentenceNotesArea.getDocument()) {
            if (selectedSentence == null) return;
            selectedSentence.setNotes(sentenceNotesArea.getText());
        }
    }

    public void changedUpdate(DocumentEvent e) {
        Debug.trace();
        documentContentsChanged(e);
    }

    public void removeUpdate(DocumentEvent e) {
        Debug.trace();
        documentContentsChanged(e);
    }

    public void insertUpdate(DocumentEvent e) {
        Debug.trace();
        documentContentsChanged(e);
    }

    // DocumentListener *//

    public boolean sentenceIdExists(Book book, String id) {
        for (Enumeration c = book.children(); c.hasMoreElements();) {
            Chapter chp = (Chapter)c.nextElement();
            for (Enumeration s = chp.children(); s.hasMoreElements();) {
                Sentence snt = (Sentence)s.nextElement();
                if (snt.getId().equals(id)) return true;
            }
        }
        return false;
    }

    public void findOrphans(Book book) {
        Chapter orphans = book.getChapterById("orphans");
        if (orphans == null) {
            orphans = new Chapter("orphans", "Orphan Files");
            orphans.setParentBook(book);
            book.add(orphans);
        }
        File bookRoot = book.getLocation();
        File[] files = new File(bookRoot, "files").listFiles();
        for (File f : files) {
            String filename = f.getName();
            if (filename.startsWith(".")) continue;
            if (filename.startsWith("backup")) continue;
            if (filename.equals("room-noise.wav")) continue;
            if (filename.endsWith(".wav")) {
                String id = filename.substring(0, filename.length() - 4);
                Debug.d("Testing orphanicity of", id);
                if (!sentenceIdExists(book, id)) {
                    Sentence newSentence = new Sentence(id, id);
                    newSentence.setParentBook(book);
                    orphans.add(newSentence);
                }
            }
        }
        if (orphans.getChildCount() == 0) {
            book.remove(orphans);
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                bookTreeModel.reload(book);
            }
        });
    }

    public void queueJob(Runnable r) {
        synchronized(processQueue) {
            processQueue.add(r);
            if (r instanceof SentenceJob) {
                SentenceJob sj = (SentenceJob)r;
                sj.setQueued();
            }
            processQueue.notify();
        }
    }

    public Book getBook() {
        return selectedBook;
//        if (bookTree == null) return null;
//        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();
//        if (selectedNode == null) return null;
//        if (selectedNode instanceof BookTreeNode) {
//            BookTreeNode btn = (BookTreeNode)selectedNode;
//            return btn.getBook();
//        }
//        return null;
    }


    public static void setSelectedSentence(Sentence s) {
        Debug.trace();
        selectedSentence = s;
        if (selectedSentence == null) window.sentenceNotesArea.setText("");
        Debug.d("Selected sentence", s);
    }

    public static void setSelectedChapter(Chapter c) {
        Debug.trace();
        selectedChapter = c;
        if (selectedChapter == null) window.chapterNotesArea.setText("");
        Debug.d("Selected chapter", c);
    }

    public static void setSelectedBook(Book b) {
        Debug.trace();
        selectedBook = b;
        if (selectedBook == null) window.bookNotesArea.setText("");
        Debug.d("Selected book", b);
    }

    public void saveBook(Book b) {
        try {
            b.save();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(AudiobookRecorder.this, "There was an error saving the book: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void updateOpenBookList() {

        String paths = "";
        for (Enumeration s = rootNode.children(); s.hasMoreElements();) {
            TreeNode n = (TreeNode)s.nextElement();
            if (n instanceof Book) {
                try {
                    Book b = (Book)n;
                    File f = b.getBookFile();
                    if (!paths.equals("")) {
                        paths += "::";
                    }
                    paths += f.getCanonicalPath();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        Options.set("system.open-books", paths);
        Options.savePreferences();
    }

    public void editBookInfo(Book book) {
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
        if (book.effects != null) {
            for (String k : book.effects.keySet()) {
                if (k.equals(book.getDefaultEffect())) {
                    selEff = i;
                }
                KVPair<String, String> p = new KVPair<String, String>(k, book.effects.get(k).toString());
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
//        if (!(book().getName().equals(tit))) {
//            book().renameBook(tit);
//        }

        CacheManager.purgeCache();
    }

    public void importWavFile(Chapter c) {
        JFileChooser jc = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("WAV Files", "wav");
        jc.addChoosableFileFilter(filter);
        jc.setFileFilter(filter);
        jc.setDialogTitle("Select WAV File");
        int r = jc.showOpenDialog(this);

        if (r != JFileChooser.APPROVE_OPTION) return;
        File f = jc.getSelectedFile();
        if (!f.exists()) return;

        try {

            Book book = c.getBook();
            AudioFormat targetFormat = book.getAudioFormat();

            Sentence newSentence = new Sentence();
            newSentence.setText(f.getName());
            newSentence.setParentBook(book);
            c.add(newSentence);

            FileOutputStream fos = new FileOutputStream(newSentence.getFile());
            AudioInputStream source = AudioSystem.getAudioInputStream(f);
            AudioInputStream in = AudioSystem.getAudioInputStream(targetFormat, source);
            AudioSystem.write(in, AudioFileFormat.Type.WAVE, newSentence.getFile());
//            fos.close();
            in.close();
            source.close();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    bookTreeModel.reload(c);
                    bookTree.setSelectionPath(new TreePath(newSentence.getPath()));
                    bookTree.scrollPathToVisible(new TreePath(newSentence.getPath()));
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
