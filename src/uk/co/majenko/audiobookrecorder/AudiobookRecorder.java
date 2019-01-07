package uk.co.majenko.audiobookrecorder;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.*;
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

public class AudiobookRecorder extends JFrame {

    static Properties config = new Properties();

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
    JMenu bookVisitACX;
    JMenuItem bookVisitTitle;
    JMenuItem bookVisitAudition;
    JMenuItem bookVisitProduce;

    JMenuItem toolsMerge;
    JMenuItem toolsArchive;
    JMenuItem toolsCoverArt;
    JMenuItem toolsOptions;

    JMenuItem helpAbout;

    FlashPanel centralPanel;

    JPanel statusBar;

    JLabel statusLabel;

    JScrollPane mainScroll;

    JDialog equaliserWindow = null;

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

    JSpinner postSentenceGap;
    JSpinner gainPercent;
    JCheckBox locked;
    JCheckBox attention;
    JCheckBox ethereal;

    JButtonSpacePlay reprocessAudioFFT;
    JButtonSpacePlay reprocessAudioPeak;
    JButtonSpacePlay normalizeAudio;

    JComboBox<String> eqProfile;

    Thread playingThread = null;

    Random rng = new Random();

    SourceDataLine play = null;

    public HavenQueue havenQueue = new HavenQueue();


    public TargetDataLine microphone = null;
    public AudioInputStream microphoneStream = null;

    public static AudiobookRecorder window;

    void buildToolbar(Container ob) {
        toolBar = new MainToolBar(this);
        toolBar.addSeparator();
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(havenQueue);
        ob.add(toolBar, BorderLayout.NORTH); 
    }

    void buildMenu(Container ob) {
        menuBar = new JMenuBar();

        fileMenu = new JMenu("File");

        fileNewBook = new JMenuItem("New Book...");
        fileNewBook.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveBookStructure();
                createNewBook();
            }
        });
        fileOpenBook = new JMenuItem("Open Book...");
        fileOpenBook.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveBookStructure();
                openBook();
            }
        });
        fileSave = new JMenuItem("Save Book");
        fileSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveBookStructure();
            }
        });

        fileOpenArchive = new JMenuItem("Open Archive...");
        fileOpenArchive.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openArchive();
            }
        });

        fileExit = new JMenuItem("Exit");
        fileExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
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
                addChapter();
            }
        });

        bookExportAudio = new JMenuItem("Export Audio");
        bookExportAudio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exportAudio();
            }
        });

        bookMenu.add(bookNewChapter);
        bookMenu.add(bookExportAudio);

        bookVisitACX = new JMenu("Visit ACX");
        bookMenu.add(bookVisitACX);

        bookVisitTitle = new JMenuItem("Title");
        bookVisitTitle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Utils.browse("https://www.acx.com/titleview/" + book.getACX());
            }
        });
        bookVisitACX.add(bookVisitTitle);

        bookVisitAudition = new JMenuItem("Audition");
        bookVisitAudition.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Utils.browse("https://www.acx.com/titleview/" + book.getACX() + "?bucket=AUDITION_READY");
            }
        });
        bookVisitACX.add(bookVisitAudition);

        bookVisitProduce = new JMenuItem("Produce");
        bookVisitProduce.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Utils.browse("https://www.acx.com/titleview/" + book.getACX() + "?bucket=PRODUCE");
            }
        });
        bookVisitACX.add(bookVisitProduce);

        menuBar.add(bookMenu);

        toolsMenu = new JMenu("Tools");

        toolsMerge = new JMenuItem("Merge Book...");
        toolsMerge.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveBookStructure();
                mergeBook();
            }
        });
        
        toolsArchive = new JMenuItem("Archive Book");
        toolsArchive.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveBookStructure();
                archiveBook();
            }
        });

        toolsCoverArt = new JMenuItem("Import Cover Art...");
        toolsCoverArt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadCoverArt();
            }
        });
        
        toolsOptions = new JMenuItem("Options");
        toolsOptions.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new Options(AudiobookRecorder.this);
            }
        });
        
        toolsMenu.add(toolsMerge);
        toolsMenu.add(toolsArchive);
        toolsMenu.add(toolsCoverArt);
        toolsMenu.addSeparator();
        toolsMenu.add(toolsOptions);

        menuBar.add(toolsMenu);


        helpMenu = new JMenu("Help");
        helpAbout = new JMenuItem("About AudiobookRecorder");

        helpAbout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(AudiobookRecorder.this, new AboutPanel(), "About AudiobookRecorder", JOptionPane.PLAIN_MESSAGE);
            }
        });

        helpMenu.add(helpAbout);
        menuBar.add(helpMenu);

        ob.add(menuBar, BorderLayout.NORTH);

        setPreferredSize(new Dimension(700, 500));
        setLocationRelativeTo(null);
    }

    public AudiobookRecorder() {

        window = this;

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

        execScript(Options.get("scripts.startup"));

        CacheManager.setCacheSize(Options.getInteger("cache.size"));

        setLayout(new BorderLayout());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
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
                sampleWaveform.setOffset(sampleScroll.getValue());
            }
        });

        sampleControl.add(sampleScroll, BorderLayout.SOUTH);
        

        sampleWaveform.addMarkerDragListener(new MarkerDragListener() {
            public void leftMarkerMoved(MarkerDragEvent e) {
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
                if (selectedSentence != null) {
                    selectedSentence.autoTrimSampleFFT();
                    sampleWaveform.setData(selectedSentence.getAudioData());
                    sampleWaveform.setMarkers(selectedSentence.getStartOffset(), selectedSentence.getEndOffset());
                    sampleWaveform.setAltMarkers(selectedSentence.getStartCrossing(), selectedSentence.getEndCrossing());
                    postSentenceGap.setValue(selectedSentence.getPostGap());
                    gainPercent.setValue((int)(selectedSentence.getGain() * 100d));
                }
            }
        });

        reprocessAudioPeak = new JButtonSpacePlay(Icons.peak, "Autotrim Audio (Peak)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (selectedSentence != null) {
                    selectedSentence.autoTrimSamplePeak();
                    sampleWaveform.setData(selectedSentence.getAudioData());
                    sampleWaveform.setMarkers(selectedSentence.getStartOffset(), selectedSentence.getEndOffset());
                    sampleWaveform.setAltMarkers(selectedSentence.getStartCrossing(), selectedSentence.getEndCrossing());
                    postSentenceGap.setValue(selectedSentence.getPostGap());
                    gainPercent.setValue((int)(selectedSentence.getGain() * 100d));
                }
            }
        });

        normalizeAudio = new JButtonSpacePlay(Icons.normalize, "Normalize audio", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (selectedSentence != null) {
                    selectedSentence.normalize();
                    sampleWaveform.setData(selectedSentence.getAudioData());
                }
            }
        });
    

        postSentenceGap = new JSpinner(new SteppedNumericSpinnerModel(0, 5000, 100, 0));
        postSentenceGap.setPreferredSize(new Dimension(50, 20));

        postSentenceGap.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
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
                JSpinner ob = (JSpinner)e.getSource();
                if (selectedSentence != null) {
                    selectedSentence.setGain((Integer)ob.getValue() / 100d);
                    sampleWaveform.setData(selectedSentence.getAudioData());
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

        locked = new JCheckBox("Phrase locked");
        locked.setFocusable(false);

        locked.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
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

                bookTreeModel.reload(selectedSentence);
            }
        });
        controlsTop.add(locked);

        attention = new JCheckBox("Flag for attention");
        attention.setFocusable(false);

        attention.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
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

        ethereal = new JCheckBox("Ethereal voice");
        ethereal.setFocusable(false);

        ethereal.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JCheckBox c = (JCheckBox)e.getSource();
                if (c.isSelected()) {
                    if (selectedSentence != null) {
                        selectedSentence.setEthereal(true);
                    }
                } else {
                    if (selectedSentence != null) {
                        selectedSentence.setEthereal(false);
                    }
                }
                bookTreeModel.reload(selectedSentence);
            }
        });

        controlsTop.add(ethereal);

        controlsTop.add(Box.createHorizontalGlue());
        controlsTop.add(new JLabel("Post gap:"));
        controlsTop.add(postSentenceGap);
        controlsTop.add(new JLabel("ms"));

        controlsTop.add(new JLabel("Gain:"));
        controlsTop.add(gainPercent);
        controlsTop.add(new JLabel("%"));

        controlsTop.add(Box.createHorizontalGlue());


        JButtonSpacePlay zoomIn = new JButtonSpacePlay(Icons.zoomIn, "Zoom In", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sampleWaveform.increaseZoom();
            }
        });
        controlsRight.add(zoomIn);

        JButtonSpacePlay zoomOut = new JButtonSpacePlay(Icons.zoomOut, "Zoom Out", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sampleWaveform.decreaseZoom();
            }
        });
        controlsRight.add(zoomOut);

        controlsBottom.add(new JLabel("EQ Profile: "));

        String[] profiles = new String[2];
        profiles[0] = "Default";
        profiles[1] = "Phone";

        eqProfile = new JComboBox<String>(profiles);
        controlsBottom.add(eqProfile);

        eqProfile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (selectedSentence != null) {
                    selectedSentence.setEQProfile(eqProfile.getSelectedIndex());
                }
            }
        });

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

        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F"), "startRecordShort");
        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released F"), "stopRecord");

        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("R"), "startRecord");
        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released R"), "stopRecord");

        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("T"), "startRecordNewPara");
        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released T"), "stopRecord");

        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released D"), "deleteLast");

        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "startStopPlayback");

        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("E"), "startRerecord");
        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released E"), "stopRecord");

        centralPanel.getActionMap().put("startRecord", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (bookTree.isEditing()) return;
                if (getNoiseFloor() == 0) {
                    alertNoRoomNoise();
                    return;
                }
                startRecording();
            }
        });
        centralPanel.getActionMap().put("startRecordShort", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (bookTree.isEditing()) return;
                if (getNoiseFloor() == 0) {
                    alertNoRoomNoise();
                    return;
                }
                startRecordingShort();
            }
        });
        centralPanel.getActionMap().put("startRecordNewPara", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (bookTree.isEditing()) return;
                if (getNoiseFloor() == 0) {
                    alertNoRoomNoise();
                    return;
                }
                startRecordingNewParagraph();
            }
        });
        centralPanel.getActionMap().put("startRerecord", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (bookTree.isEditing()) return;
                if (getNoiseFloor() == 0) {
                    alertNoRoomNoise();
                    return;
                }
                startReRecording();
            }
        });
        centralPanel.getActionMap().put("stopRecord", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (bookTree.isEditing()) return;
                stopRecording();
            }
        });
        centralPanel.getActionMap().put("deleteLast", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (bookTree.isEditing()) return;
                deleteLastRecording();
            }
        });

        centralPanel.getActionMap().put("startStopPlayback", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (bookTree.isEditing()) return;
                if (playing == null) {
                    playSelectedSentence();
                } else {
                    stopPlaying();
                }
            }
        });

        mainScroll = new JScrollPane();
        centralPanel.add(mainScroll, BorderLayout.CENTER);

        setTitle("AudioBook Recorder");

        setIconImage(Icons.appIcon.getImage());


        pack();
        setVisible(true);

        String lastBook = Options.get("path.last-book");

        if (lastBook != null && !lastBook.equals("")) {
            File f = new File(Options.get("path.storage"), lastBook);
            if (f.exists() && f.isDirectory()) {
                File x = new File(f, "audiobook.abk");
                if (x.exists()) {
                    loadBookStructure(x);
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

    public static void main(String args[]) {
        try {
            config.load(AudiobookRecorder.class.getResourceAsStream("config.txt"));
        } catch (Exception e) {
            e.printStackTrace();
        }


        new AudiobookRecorder();
    }

    public void createNewBook() {
        BookInfoPanel info = new BookInfoPanel("", "", "", "", "");
        int r = JOptionPane.showConfirmDialog(this, info, "New Book", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;

        String name = info.getTitle();

        File bookdir = new File(Options.get("path.storage"), name);
        if (bookdir.exists()) {
            JOptionPane.showMessageDialog(this, "File already exists.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }


        Properties prefs = new Properties();

        prefs.setProperty("book.name", info.getTitle());
        prefs.setProperty("book.author", info.getAuthor());
        prefs.setProperty("book.genre", info.getGenre());
        prefs.setProperty("book.comment", info.getComment());
        prefs.setProperty("book.acx", info.getACX());
        prefs.setProperty("chapter.audition.name", "Audition");
        prefs.setProperty("chapter.audition.pre-gap", Options.get("catenation.pre-chapter"));
        prefs.setProperty("chapter.audition.post-gap", Options.get("catenation.post-chapter"));
        prefs.setProperty("chapter.open.name", "Opening Credits");
        prefs.setProperty("chapter.open.pre-gap", Options.get("catenation.pre-chapter"));
        prefs.setProperty("chapter.open.post-gap", Options.get("catenation.post-chapter"));
        prefs.setProperty("chapter.0001.name", "Chapter 1");
        prefs.setProperty("chapter.0001.pre-gap", Options.get("catenation.pre-chapter"));
        prefs.setProperty("chapter.0001.post-gap", Options.get("catenation.post-chapter"));
        prefs.setProperty("chapter.close.name", "Closing Credits");
        prefs.setProperty("chapter.close.pre-gap", Options.get("catenation.pre-chapter"));
        prefs.setProperty("chapter.close.post-gap", Options.get("catenation.post-chapter"));

        buildBook(prefs);

        Options.set("path.last-book", book.getName());
    }

    class JMenuObject extends JMenuItem {
        Object ob;
        
        public JMenuObject(String p) {  
            super(p);
            ob = null;
        }

        public JMenuObject(String p, Object o, ActionListener l) {  
            super(p);
            ob = o;
            addActionListener(l);
        }

        public void setObject(Object o) {
            ob = o;
        }

        public Object getObject() {
            return ob;
        }
    }

    class JMenuObject2 extends JMenuItem {
        Object ob1;
        Object ob2;
        
        public JMenuObject2(String p) {  
            super(p);
            ob1 = null;
            ob2 = null;
        }

        public JMenuObject2(String p, Object o1, Object o2, ActionListener l) {  
            super(p);
            ob1 = o1;
            ob2 = o2;
            addActionListener(l);
        }

        public void setObject1(Object o) {
            ob1 = o;
        }

        public void setObject2(Object o) {
            ob2 = o;
        }

        public Object getObject1() {
            return ob1;
        }

        public Object getObject2() {
            return ob2;
        }
    }

    @SuppressWarnings("unchecked")
    void treePopup(MouseEvent e) {

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
                        JMenuObject o = (JMenuObject)e.getSource();
                        Sentence s = (Sentence)o.getObject();
                        if (!s.isLocked()) {
                            havenQueue.submit(s);
                        }
                    }
                });



                JMenu moveMenu = new JMenu("Move phrase to...");


                for (Enumeration c = book.children(); c.hasMoreElements();) {
                    Chapter chp = (Chapter)c.nextElement();
                    JMenuObject2 m = new JMenuObject2(chp.getName(), s, chp, new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
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
                        JMenuObject o = (JMenuObject)e.getSource();
                        Sentence sent = (Sentence)o.getObject();
                        Chapter chap = (Chapter)sent.getParent();
                        int pos = bookTreeModel.getIndexOfChild(chap, sent);
                        if (pos < chap.getChildCount() - 1) pos++;
                        bookTreeModel.removeNodeFromParent(sent);
                        bookTreeModel.insertNodeInto(sent, chap, pos);
                    }
                });


                JMenuObject ins = new JMenuObject("Insert phrase above", s, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
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
                        try {
                            JMenuObject o = (JMenuObject)e.getSource();
                            Sentence s = (Sentence)o.getObject();
                            Sentence newSentence = s.cloneSentence();
                            Chapter c = (Chapter)s.getParent();
                            int idx = bookTreeModel.getIndexOfChild(c, s);
                            bookTreeModel.insertNodeInto(newSentence, c, idx);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });

                JMenuObject edit = new JMenuObject("Open in external editor", s, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            JMenuObject o = (JMenuObject)e.getSource();
                            Sentence s = (Sentence)o.getObject();
                            s.openInExternalEditor();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });

                menu.add(rec);
                menu.addSeparator();
                menu.add(moveUp);
                menu.add(moveDown);
                menu.add(moveMenu);
                menu.addSeparator();
                menu.add(edit);
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

                JMenuObject peak = new JMenuObject("Auto-trim all (Peak)", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter c = (Chapter)o.getObject();
                        for (Enumeration s = c.children(); s.hasMoreElements();) {
                            Sentence snt = (Sentence)s.nextElement();
                            if (!snt.isLocked()) {
                                snt.autoTrimSamplePeak();
                            }
                        }
                    }
                });

                JMenuObject fft = new JMenuObject("Auto-trim all (FFT)", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter c = (Chapter)o.getObject();
                        for (Enumeration s = c.children(); s.hasMoreElements();) {
                            Sentence snt = (Sentence)s.nextElement();
                            if (!snt.isLocked()) {
                                snt.autoTrimSampleFFT();
                            }
                        }
                    }
                });

                JMenuObject moveUp = new JMenuObject("Move Up", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter chap = (Chapter)o.getObject();
                        int pos = bookTreeModel.getIndexOfChild(book, chap);
                        if (pos > 0) pos--;

                        int id = Utils.s2i(chap.getId());
                        if (id > 0) {
                            Chapter prevChap = (Chapter)bookTreeModel.getChild(book, pos);
                            id = Utils.s2i(prevChap.getId());
                            if (id > 0) {
                                bookTreeModel.removeNodeFromParent(chap);
                                bookTreeModel.insertNodeInto(chap, book, pos);
                            }
                        }
                        book.renumberChapters();
                    }
                });
                moveUp.setEnabled(idNumber > 0);

                JMenuObject moveDown = new JMenuObject("Move Down", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter chap = (Chapter)o.getObject();
                        int pos = bookTreeModel.getIndexOfChild(book, chap);
                        pos++;
                        int id = Utils.s2i(chap.getId());
                        if (id > 0) {
                            Chapter nextChap = (Chapter)bookTreeModel.getChild(book, pos);
                            if (nextChap != null) {
                                id = Utils.s2i(nextChap.getId());
                                if (id > 0) {
                                    bookTreeModel.removeNodeFromParent(chap);
                                    bookTreeModel.insertNodeInto(chap, book, pos);
                                }
                            }
                        }
                        book.renumberChapters();
                    }
                });
                moveDown.setEnabled(idNumber > 0);

                JMenu mergeWith = new JMenu("Merge chapter with");
                for (Enumeration bc = book.children(); bc.hasMoreElements();) {
                    Chapter chp = (Chapter)bc.nextElement();
                    if (chp.getId().equals(c.getId())) {
                        continue;
                    }
                    JMenuObject2 m = new JMenuObject2(chp.getName(), c, chp, new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
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
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter c = (Chapter)o.getObject();
                        for (Enumeration s = c.children(); s.hasMoreElements();) {
                            Sentence snt = (Sentence)s.nextElement();
                            if (snt.getId().equals(snt.getText())) {
                                havenQueue.submit(snt);
                            }
                        }
                    }
                });

                JMenuObject normalizeAll = new JMenuObject("Normalize chapter", c, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter c = (Chapter)o.getObject();
                        for (Enumeration s = c.children(); s.hasMoreElements();) {
                            Sentence snt = (Sentence)s.nextElement();
                            snt.normalize();
                        }
                    }
                });

                menu.add(convertAll);
                menu.add(normalizeAll);
                menu.addSeparator();
                menu.add(moveUp);
                menu.add(moveDown);
                menu.addSeparator();
                menu.add(mergeWith);
                menu.addSeparator();
                menu.add(peak);
                menu.add(fft);
                menu.addSeparator();
                menu.add(lockAll);
                menu.add(unlockAll);
                menu.addSeparator();
                menu.add(exportChapter);
                menu.addSeparator();
                menu.add(deleteChapter);

                menu.show(bookTree, e.getX(), e.getY());
            } else if (node instanceof Book) {
                JPopupMenu menu = new JPopupMenu();

                JMenuItem editData = new JMenuItem("Edit Data...");
                editData.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JTabbedPane tabs = new JTabbedPane();
                        BookInfoPanel info = new BookInfoPanel(
                            book.getName(),
                            book.getAuthor(),
                            book.getGenre(),
                            book.getComment(),
                            book.getACX()
                        );
                        tabs.add("Data", info);

                        JPanel effects = new JPanel();
                        effects.setLayout(new GridBagLayout());
                        GridBagConstraints c = new GridBagConstraints();
                        c.gridx = 0;
                        c.gridy = 0;

                        effects.add(new JLabel("Ethereal Iterations:"), c);
                        c.gridx = 1;
                        JSpinner ethIt = new JSpinner(new SteppedNumericSpinnerModel(1, 10, 1, book.getInteger("effects.ethereal.iterations")));
                        effects.add(ethIt, c);
                        c.gridx = 0;
                        c.gridy++;

                        effects.add(new JLabel("Ethereal Attenuation:"), c);
                        c.gridx = 1;
                        JSpinner ethAt = new JSpinner(new SteppedNumericSpinnerModel(0, 100, 1, book.getInteger("effects.ethereal.attenuation")));
                        effects.add(ethAt, c);
                        c.gridx = 0;
                        c.gridy++;

                        effects.add(new JLabel("Ethereal Offset:"), c);
                        c.gridx = 1;
                        JSpinner ethOf = new JSpinner(new SteppedNumericSpinnerModel(0, 2000, 10, book.getInteger("effects.ethereal.offset")));
                        effects.add(ethOf, c);
                        c.gridx = 0;
                        c.gridy++;

                        tabs.add("Effects", effects);

                        int r = JOptionPane.showConfirmDialog(AudiobookRecorder.this, tabs, "Edit Book", JOptionPane.OK_CANCEL_OPTION);
                        if (r != JOptionPane.OK_OPTION) return;

                        String tit = info.getTitle();
                        String aut = info.getAuthor();
                        String gen = info.getGenre();
                        String com = info.getComment();
                        String acx = info.getACX();

                        book.set("effects.ethereal.iterations", (Integer)ethIt.getValue());
                        book.set("effects.ethereal.attenuation", (Integer)ethAt.getValue());
                        book.set("effects.ethereal.offset", (Integer)ethOf.getValue());

                        book.setAuthor(aut);
                        book.setGenre(gen);
                        book.setComment(com);
                        book.setACX(acx);
                        if (!(book.getName().equals(tit))) {
                            book.renameBook(tit);
                        }
                    }
                });
                menu.add(editData);

                menu.show(bookTree, e.getX(), e.getY());
            }

        }
    }

    public void startReRecording() {

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

        if (s.startRecording()) {
            recording = (Sentence)selectedNode;
            centralPanel.setFlash(true);
        }
    }

    public void startRecordingShort() {

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
        Chapter c = book.addChapter();   
        Chapter lc = book.getLastChapter();
        int i = bookTreeModel.getIndexOfChild(book, lc);
        bookTreeModel.insertNodeInto(c, book, i+1);
        bookTree.scrollPathToVisible(new TreePath(c.getPath()));
    } 

    @SuppressWarnings("unchecked")
    public void saveBookStructure() {
        if (book == null) return;

        File bookRoot = new File(Options.get("path.storage"), book.getName());
        if (!bookRoot.exists()) {
            bookRoot.mkdirs();
        }

        File config = new File(bookRoot, "audiobook.abk");
        Properties prefs = new Properties();

        prefs.setProperty("effects.ethereal.iterations", book.get("effects.ethereal.iterations"));
        prefs.setProperty("effects.ethereal.offset", book.get("effects.ethereal.offset"));
        prefs.setProperty("effects.ethereal.attenuation", book.get("effects.ethereal.attenuation"));
        
        prefs.setProperty("book.name", book.getName());
        prefs.setProperty("book.author", book.getAuthor());
        prefs.setProperty("book.genre", book.getGenre());
        prefs.setProperty("book.comment", book.getComment());
        prefs.setProperty("book.acx", book.getACX());

        prefs.setProperty("audio.recording.samplerate", "" + book.getSampleRate());
        prefs.setProperty("audio.recording.resolution", "" + book.getResolution());
        prefs.setProperty("audio.recording.channels", "" + book.getChannels());

        for (int e = 0; e < book.equaliser.length; e++) {
            for (int i = 0; i < 31; i++) {
                prefs.setProperty(String.format("audio.eq.profiles.%d.%d", e, i), String.format("%.3f", book.equaliser[e].getChannel(i)));
            }
        }

        for (Enumeration o = book.children(); o.hasMoreElements();) {

            Chapter c = (Chapter)o.nextElement();
            String keybase = "chapter." + c.getId();
            prefs.setProperty(keybase + ".name", c.getName());
            prefs.setProperty(keybase + ".pre-gap", Integer.toString(c.getPreGap()));
            prefs.setProperty(keybase + ".post-gap", Integer.toString(c.getPostGap()));

            int i = 0;
            for (Enumeration s = c.children(); s.hasMoreElements();) {
                Sentence snt = (Sentence)s.nextElement();
                prefs.setProperty(String.format("%s.sentence.%08d.id", keybase, i), snt.getId());
                prefs.setProperty(String.format("%s.sentence.%08d.text", keybase, i), snt.getText());
                prefs.setProperty(String.format("%s.sentence.%08d.post-gap", keybase, i), Integer.toString(snt.getPostGap()));
                prefs.setProperty(String.format("%s.sentence.%08d.start-offset", keybase, i), Integer.toString(snt.getStartOffset()));
                prefs.setProperty(String.format("%s.sentence.%08d.end-offset", keybase, i), Integer.toString(snt.getEndOffset()));
                prefs.setProperty(String.format("%s.sentence.%08d.locked", keybase, i), snt.isLocked() ? "true" : "false");
                prefs.setProperty(String.format("%s.sentence.%08d.attention", keybase, i), snt.getAttentionFlag() ? "true" : "false");
                prefs.setProperty(String.format("%s.sentence.%08d.ethereal", keybase, i), snt.getEthereal() ? "true" : "false");
                prefs.setProperty(String.format("%s.sentence.%08d.gain", keybase, i), String.format("%.8f", snt.getGain()));
                prefs.setProperty(String.format("%s.sentence.%08d.eqprofile", keybase, i), Integer.toString(snt.getEQProfile()));
                i++;
            }
        }
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(config);
            prefs.storeToXML(fos, "Audiobook Recorder Description");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (fos != null) {
            try {
                fos.close();
            } catch (Exception e) {
            }
        }
    }

    public void loadBookStructure(File f) {
        try {
            Properties prefs = new Properties();
            FileInputStream fis = new FileInputStream(f);
            prefs.loadFromXML(fis);

            buildBook(prefs);

            File r = f.getParentFile();
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

    public void buildBook(Properties prefs) {

        book = new Book(prefs, prefs.getProperty("book.name"));

        book.setAuthor(prefs.getProperty("book.author"));
        book.setGenre(prefs.getProperty("book.genre"));
        book.setComment(prefs.getProperty("book.comment"));
        book.setACX(prefs.getProperty("book.acx"));

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


        for (int e = 0; e < book.equaliser.length; e++) {
            for (int i = 0; i < 31; i++) {
                if (prefs.getProperty(String.format("audio.eq.profiles.%d.%d", e, i)) == null) {
                    book.equaliser[e].setChannel(i, Options.getFloat("audio.eq." + i));
                } else {
                    book.equaliser[e].setChannel(i, Utils.s2f(prefs.getProperty(String.format("audio.eq.profiles.%d.%d", e, i))));
                }
            }
        }

        bookTreeModel = new DefaultTreeModel(book);
        bookTree = new JTree(bookTreeModel);
        bookTree.setEditable(true);
        bookTree.setUI(new CustomTreeUI(mainScroll));

        bookTree.setCellRenderer(new BookTreeRenderer());


        InputMap im = bookTree.getInputMap(JComponent.WHEN_FOCUSED);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "startStopPlayback");

        roomNoise = new Sentence("room-noise", "Room Noise");

        bookTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode n = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();
                if (n instanceof Sentence) {
                    Sentence s = (Sentence)n;
                    selectedSentence = s;
                    sampleWaveform.setData(s.getAudioData());
                    sampleWaveform.setMarkers(s.getStartOffset(), s.getEndOffset());
                    s.updateCrossings();
                    sampleWaveform.setAltMarkers(s.getStartCrossing(), s.getEndCrossing());
                    postSentenceGap.setValue(s.getPostGap());
                    gainPercent.setValue((int)(s.getGain() * 100d));
                    locked.setSelected(s.isLocked());
                    attention.setSelected(s.getAttentionFlag());
                    ethereal.setSelected(s.getEthereal());
                    eqProfile.setSelectedIndex(s.getEQProfile());

                    postSentenceGap.setEnabled(!s.isLocked());
                    gainPercent.setEnabled(!s.isLocked());
                    reprocessAudioFFT.setEnabled(!s.isLocked());
                    reprocessAudioPeak.setEnabled(!s.isLocked());
                } else {
                    selectedSentence = null;
                    sampleWaveform.clearData();
                    postSentenceGap.setValue(0);
                    gainPercent.setValue(100);
                    eqProfile.setSelectedIndex(0);
                    locked.setSelected(false);
                    attention.setSelected(false);
                    ethereal.setSelected(false);
                }
            }
        });


        bookTree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    treePopup(e);
                }
            }

            public void mouseReleased(MouseEvent e) {
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
            s.setEthereal(Utils.s2b(prefs.getProperty(String.format("chapter.audition.sentence.%08d.ethereal", i))));
            s.setGain(Utils.s2d(prefs.getProperty(String.format("chapter.audition.sentence.%08d.gain", i))));
            s.setEQProfile(Utils.s2i(prefs.getProperty(String.format("chapter.audition.sentence.%08d.eqprofile", i))));
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
            s.setEthereal(Utils.s2b(prefs.getProperty(String.format("chapter.open.sentence.%08d.ethereal", i))));
            s.setGain(Utils.s2d(prefs.getProperty(String.format("chapter.open.sentence.%08d.gain", i))));
            s.setEQProfile(Utils.s2i(prefs.getProperty(String.format("chapter.open.sentence.%08d.eqprofile", i))));
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
                s.setEthereal(Utils.s2b(prefs.getProperty(String.format("chapter.%04d.sentence.%08d.ethereal", cno, i))));
                s.setGain(Utils.s2d(prefs.getProperty(String.format("chapter.%04d.sentence.%08d.gain", cno, i))));
                s.setEQProfile(Utils.s2i(prefs.getProperty(String.format("chapter.%04d.sentence.%08d.eqprofile", cno, i))));
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
            s.setEthereal(Utils.s2b(prefs.getProperty(String.format("chapter.close.sentence.%08d.ethereal", i))));
            s.setGain(Utils.s2d(prefs.getProperty(String.format("chapter.close.sentence.%08d.gain", i))));
            s.setEQProfile(Utils.s2i(prefs.getProperty(String.format("chapter.close.sentence.%08d.eqprofile", i))));
            bookTreeModel.insertNodeInto(s, c, c.getChildCount());
        }

        bookTree.expandPath(new TreePath(book.getPath()));

        statusLabel.setText("Noise floor: " + getNoiseFloorDB() + "dB");
        book.setIcon(Icons.book);
    }

    public void openBook() {

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

            if (!f.getName().endsWith(".abk")) {
                JOptionPane.showMessageDialog(this, "Not a .abk file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
                
            loadBookStructure(f);

            Options.set("path.last-book", book.getName());
            Options.savePreferences();
            
        }

        
    }

    public File getBookFolder() {
        File bf = new File(Options.get("path.storage"), book.getName());
        if (!bf.exists()) {
            bf.mkdirs();
        }
        return bf;
    }

    public int getNoiseFloor() { 
        if (roomNoise == null) return 0;
        int[] samples = roomNoise.getAudioData();
        if (samples == null) {
            return 0;
        }
        int ms = 0;
        for (int i = 0; i < samples.length; i++) {
            if (Math.abs(samples[i]) > ms) {
                ms = Math.abs(samples[i]);
            }
        }

        ms *= 10;
        ms /= 7;
        return ms;
    }

    public int getNoiseFloorDB() {
        int nf = getNoiseFloor();
        if (nf == 0) return 0;
        double r = nf / 32767d;
        double l10 = Math.log10(r);
        double db = 20d * l10;

        return (int)db;
    }

    public void recordRoomNoise() {
        if (roomNoise.startRecording()) {

            centralPanel.setFlash(true);
            java.util.Timer ticker = new java.util.Timer(true);
            ticker.schedule(new TimerTask() {
                public void run() {
                    roomNoise.stopRecording();
                    centralPanel.setFlash(false);
			statusLabel.setText("Noise floor: " + getNoiseFloorDB() + "dB");
                }
            }, 5000); // 5 seconds of recording
        }
    }


    public void playSelectedSentence() {
        if (selectedSentence == null) return;
        if (playing != null) return;
        if (getNoiseFloor() == 0) {
            alertNoRoomNoise();
            return;
        }
        playing = selectedSentence;

        playingThread = new Thread(new Runnable() {
            public void run() {
                Sentence s = playing;
                byte[] data;

                try {

                    AudioFormat format = s.getAudioFormat();
                    play = AudioSystem.getSourceDataLine(format, Options.getPlaybackMixer());
                    play.open(format);
                    play.start();

                    bookTree.scrollPathToVisible(new TreePath(s.getPath()));
                    data = s.getRawAudioData();
                    for (int pos = 0; pos < data.length; pos += 1024) {
                        sampleWaveform.setPlayMarker(pos / format.getFrameSize());
                        int l = data.length - pos;
                        if (l > 1024) l = 1024;
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

    class ExportThread implements Runnable {
        ProgressDialog exportDialog;
        Chapter chapter;

        public ExportThread(Chapter c, ProgressDialog e) {
            super();
            exportDialog = e;
            chapter = c;
        }

        @SuppressWarnings("unchecked")
        public void run() {

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


    public void playFromSelectedSentence() {
        if (selectedSentence == null) return;
        if (playing != null) return;
        if (getNoiseFloor() == 0) {
            alertNoRoomNoise();
            return;
        }
        playing = selectedSentence;

        playingThread = new Thread(new Runnable() {
            public void run() {
                Sentence s = playing;
                byte[] data;

                try {

                    AudioFormat format = s.getAudioFormat();
                    play = AudioSystem.getSourceDataLine(format, Options.getPlaybackMixer());
                    play.open(format);
                    play.start();

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
                        data = s.getRawAudioData();
                        for (int pos = 0; pos < data.length; pos += 1024) {
                            sampleWaveform.setPlayMarker(pos / format.getFrameSize());
                            int l = data.length - pos;
                            if (l > 1024) l = 1024;
                            play.write(data, pos, l);
                        }

                        DefaultMutableTreeNode next = s.getNextSibling();
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
                }
            }
        });

        playingThread.setDaemon(true);
        playingThread.start();
    }


    public byte[] getRoomNoise(int ms) {

        if (roomNoise == null) return null;

        int len = roomNoise.getSampleSize();
        if (len == 0) return null;

        AudioFormat f = roomNoise.getAudioFormat();
        
        float sr = f.getSampleRate();

        int samples = (int)(ms * (sr / 1000f));

        int start = rng.nextInt(len - samples);
        int end = start + samples;

        roomNoise.setStartOffset(start);
        roomNoise.setEndOffset(end);

        byte[] data = roomNoise.getRawAudioData();

        return data;
    }

    public void stopPlaying() {
        if (play != null) {
            play.close();
            play = null;
        }
        playing = null;
    }

    public void alertNoRoomNoise() {
        JOptionPane.showMessageDialog(this, "You must record room noise\nbefore recording or playback", "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void showEqualiser() {
        if (equaliserWindow == null) {
            equaliserWindow = new JDialog();
            equaliserWindow.setTitle("Equaliser");
            JTabbedPane tabs = new JTabbedPane();
            equaliserWindow.add(tabs);
            for (int i = 0; i < book.equaliser.length; i++) {
                tabs.add(book.equaliser[i].getName(), new JScrollPane(book.equaliser[i]));
            }
            equaliserWindow.pack();
        }
        equaliserWindow.setVisible(true);
        equaliserWindow.setLocationRelativeTo(this);
    }

    public boolean enableMicrophone() {
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

            if (!f.getName().endsWith(".abk")) {
                JOptionPane.showMessageDialog(this, "Not a .abk file.", "Error", JOptionPane.ERROR_MESSAGE);
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
            pd = p;
        }

        public void run() {
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

                zos.flush();
                zos.close();

                while (fileList.size() > 0) {
                    File f = fileList.remove(fileList.size() - 1);
                    f.delete();
                }
            } catch (Exception e) {
            }
            pd.closeDialog();
        }
    }

    public void archiveBook() {
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
        JFileChooser jc = new JFileChooser(new File(Options.get("path.archive")));
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Audiobook Archives", "abz");
        jc.addChoosableFileFilter(filter);
        jc.setFileFilter(filter);
        jc.setDialogTitle("Select Audiobook Archive");
        int r = jc.showOpenDialog(this);
        
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = jc.getSelectedFile();
            if (f.exists()) {

                try {
                    ZipInputStream zis = new ZipInputStream(new FileInputStream(f)) {
                        public void close() throws IOException {
                            return;
                        }
                    };
                    ZipEntry entry = null;
                    ImageIcon cover = null;
                    Properties props = new Properties();

                    boolean gotMeta = false;
                    boolean gotCover = false;

                    while ((entry = zis.getNextEntry()) != null) {
                        if (gotMeta && gotCover) break;

                        if (entry.getName().endsWith("/audiobook.abk")) {
                            props.loadFromXML(zis);
                            gotMeta = true;
                        }

                        if (
                                entry.getName().endsWith("/coverart.png") ||
                                entry.getName().endsWith("/coverart.jpg") ||
                                entry.getName().endsWith("/coverart.gif") 
                            ) {
                            cover = new ImageIcon(ImageIO.read(zis));
                            gotCover = true;
                        }
                    }
                    zis.close();

                    BookPanel pan = new BookPanel(props, cover);
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
                        File conf = new File(bookdir, "audiobook.abk");
                        loadBookStructure(conf);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void loadCoverArt() {
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
}
