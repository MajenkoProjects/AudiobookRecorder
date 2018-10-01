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
import edu.cmu.sphinx.api.*;
import edu.cmu.sphinx.decoder.adaptation.*;
import edu.cmu.sphinx.result.*;
import java.nio.file.Files;
import java.util.zip.*;

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

    JMenuItem bookNewChapter;
    JMenuItem bookExportAudio;

    JMenuItem toolsMerge;
    JMenuItem toolsArchive;
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
    JCheckBox locked;

    JButtonSpacePlay reprocessAudioFFT;
    JButtonSpacePlay reprocessAudioPeak;

    Thread playingThread = null;

    Random rng = new Random();

    SourceDataLine play = null;

    public TargetDataLine microphone = null;
    public AudioInputStream microphoneStream = null;

    public Configuration sphinxConfig;
    public StreamSpeechRecognizer recognizer;


    public static AudiobookRecorder window;

    void initSphinx() {
        sphinxConfig = new Configuration();

        sphinxConfig.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        sphinxConfig.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        sphinxConfig.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

        try {
            recognizer = new StreamSpeechRecognizer(sphinxConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void buildToolbar(Container ob) {
        toolBar = new MainToolBar(this);
        toolBar.disableBook();
        toolBar.disableSentence();
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
        
        toolsOptions = new JMenuItem("Options");
        toolsOptions.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new Options(AudiobookRecorder.this);
            }
        });
        
        toolsMenu.add(toolsMerge);
        toolsMenu.add(toolsArchive);
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
                }
            }
        });

        postSentenceGap = new JSpinner(new SteppedNumericSpinnerModel(0, 5000, 100, 0));
        postSentenceGap.setPreferredSize(new Dimension(75, 20));

        postSentenceGap.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSpinner ob = (JSpinner)e.getSource();
                if (selectedSentence != null) {
                    selectedSentence.setPostGap((Integer)ob.getValue());
                }
            }
        });


        JToolBar controlsTop = new JToolBar(JToolBar.HORIZONTAL);
        JToolBar controlsLeft = new JToolBar(JToolBar.VERTICAL);
        JToolBar controlsRight = new JToolBar(JToolBar.VERTICAL);

        controlsTop.setFloatable(false);
        controlsLeft.setFloatable(false);
        controlsRight.setFloatable(false);

        controlsLeft.add(reprocessAudioFFT);
        controlsLeft.add(reprocessAudioPeak);

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
                bookTreeModel.reload(selectedSentence);
            }
        });

        controlsTop.add(locked);

        controlsTop.add(Box.createHorizontalGlue());
        controlsTop.add(new JLabel("Post gap:"));
        controlsTop.add(postSentenceGap);
        controlsTop.add(new JLabel("ms"));
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

        sampleControl.add(controlsTop, BorderLayout.NORTH);
        sampleControl.add(controlsLeft, BorderLayout.WEST);
        sampleControl.add(controlsRight, BorderLayout.EAST);

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
        BookInfoPanel info = new BookInfoPanel("", "", "", "");
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

        public JMenuObject(String p, Object o) {  
            super(p);
            ob = o;
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

        public JMenuObject2(String p, Object o1, Object o2) {  
            super(p);
            ob1 = o1;
            ob2 = o2;
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
                JMenuObject rec = new JMenuObject("Recognise text from audio", s);
                JMenu moveMenu = new JMenu("Move phrase to...");

                for (Enumeration c = book.children(); c.hasMoreElements();) {
                    Chapter chp = (Chapter)c.nextElement();
                    JMenuObject2 m = new JMenuObject2(chp.getName(), s, chp);
                    m.addActionListener(new ActionListener() {
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

                JMenuObject moveUp = new JMenuObject("Move Up", s);
                JMenuObject moveDown = new JMenuObject("Move Down", s);

                moveUp.addActionListener(new ActionListener() {
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

                moveDown.addActionListener(new ActionListener() {
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


                JMenuObject ins = new JMenuObject("Insert phrase above", s);
                JMenuObject del = new JMenuObject("Delete phrase", s);
                JMenuObject dup = new JMenuObject("Duplicate phrase", s);


                ins.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuObject o = (JMenuObject)e.getSource();
                        Sentence s = (Sentence)o.getObject();
                        Chapter c = (Chapter)s.getParent();
                        Sentence newSentence = new Sentence();
                        int where = bookTreeModel.getIndexOfChild(c, s);
                        bookTreeModel.insertNodeInto(newSentence, c, where);
                    }
                        
                });

                del.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuObject o = (JMenuObject)e.getSource();
                        Sentence s = (Sentence)o.getObject();
                        if (!s.isLocked()) {
                            s.deleteFiles();
                            bookTreeModel.removeNodeFromParent(s);
                        }
                    }
                });

                dup.addActionListener(new ActionListener() {
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

                rec.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuObject o = (JMenuObject)e.getSource();
                        Sentence s = (Sentence)o.getObject();
                        if (!s.isLocked()) {
                            s.setText("[recognising...]");
                            bookTreeModel.reload(s);
                            s.recognise();
                        }
                    }
                });



                menu.add(rec);
                menu.addSeparator();
                menu.add(moveUp);
                menu.add(moveDown);
                menu.add(moveMenu);
                menu.addSeparator();
                menu.add(ins);
                menu.add(del);
                menu.addSeparator();
                menu.add(dup);
                menu.show(bookTree, e.getX(), e.getY());
            } else if (node instanceof Chapter) {
                Chapter c = (Chapter)node;

                bookTree.setSelectionPath(new TreePath(c.getPath()));

                JPopupMenu menu = new JPopupMenu();
                JMenuObject peak = new JMenuObject("Auto-trim all (Peak)", c);
                JMenuObject fft = new JMenuObject("Auto-trim all (FFT)", c);
                JMenuObject moveUp = new JMenuObject("Move Up", c);
                JMenuObject moveDown = new JMenuObject("Move Down", c);
                JMenu mergeWith = new JMenu("Merge chapter with");
                JMenuObject lockAll = new JMenuObject("Lock all phrases", c);
                JMenuObject unlockAll = new JMenuObject("Unlock all phrases", c);
                JMenuObject exportChapter = new JMenuObject("Export chapter", c);
                JMenuObject deleteChapter = new JMenuObject("Delete chapter", c);

                exportChapter.addActionListener(new ActionListener() {
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

                for (Enumeration bc = book.children(); bc.hasMoreElements();) {
                    Chapter chp = (Chapter)bc.nextElement();
                    if (chp.getId().equals(c.getId())) {
                        continue;
                    }
                    JMenuObject2 m = new JMenuObject2(chp.getName(), c, chp);
                    m.addActionListener(new ActionListener() {
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



                int idNumber = Utils.s2i(c.getId());

                moveUp.setEnabled(idNumber > 0);
                moveDown.setEnabled(idNumber > 0);

                moveUp.addActionListener(new ActionListener() {
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
                moveDown.addActionListener(new ActionListener() {
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

                peak.addActionListener(new ActionListener() {
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

                fft.addActionListener(new ActionListener() {
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

                lockAll.addActionListener(new ActionListener() {
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
                unlockAll.addActionListener(new ActionListener() {
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

                deleteChapter.addActionListener(new ActionListener() {
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

        toolBar.disableBook();
        toolBar.disableSentence();

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

        toolBar.disableBook();
        toolBar.disableSentence();

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

        toolBar.disableBook();
        toolBar.disableSentence();

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

        toolBar.disableBook();
        toolBar.disableSentence();

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

        toolBar.enableBook();
        toolBar.enableSentence();
        toolBar.disableStop();

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

        
        prefs.setProperty("book.name", book.getName());
        prefs.setProperty("book.author", book.getAuthor());
        prefs.setProperty("book.genre", book.getGenre());
        prefs.setProperty("book.comment", book.getComment());

        prefs.setProperty("audio.recording.samplerate", "" + book.getSampleRate());
        prefs.setProperty("audio.recording.resolution", "" + book.getResolution());
        prefs.setProperty("audio.recording.channels", "" + book.getChannels());

        for (int i = 0; i < 31; i++) {
            prefs.setProperty("audio.eq." + i, String.format("%.3f", book.equaliser.getChannel(i)));
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

        book = new Book(prefs.getProperty("book.name"));

        book.setAuthor(prefs.getProperty("book.author"));
        book.setGenre(prefs.getProperty("book.genre"));
        book.setComment(prefs.getProperty("book.comment"));

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


        for (int i = 0; i < 31; i++) {
            if (prefs.getProperty("audio.eq." + i) == null) {
                book.equaliser.setChannel(i, Options.getFloat("audio.eq." + i));
            } else {
                book.equaliser.setChannel(i, Utils.s2f(prefs.getProperty("audio.eq." + i)));
            }
        }

        bookTreeModel = new DefaultTreeModel(book);
        bookTree = new JTree(bookTreeModel);
        bookTree.setEditable(true);
        bookTree.setUI(new CustomTreeUI(mainScroll));

        bookTree.setCellRenderer(new BookTreeRenderer());


        InputMap im = bookTree.getInputMap(JComponent.WHEN_FOCUSED);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "startPlayback");

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
                    locked.setSelected(s.isLocked());

                    postSentenceGap.setEnabled(!s.isLocked());
                    reprocessAudioFFT.setEnabled(!s.isLocked());
                    reprocessAudioPeak.setEnabled(!s.isLocked());

                    if (playing == null) {
                        toolBar.enableSentence();
                        toolBar.disableStop();
                    } else {
                        toolBar.disableSentence();
                        toolBar.enableStop();
                    }
                } else {
                    selectedSentence = null;
                    toolBar.disableSentence();
                    sampleWaveform.clearData();
                    toolBar.disableStop();
                    postSentenceGap.setValue(0);
                    locked.setSelected(false);
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
            bookTreeModel.insertNodeInto(s, c, c.getChildCount());
        }

        bookTree.expandPath(new TreePath(book.getPath()));

        toolBar.enableBook();
        statusLabel.setText("Noise floor: " + getNoiseFloorDB() + "dB");
        book.setIcon(Icons.book);
    }

    public void openBook() {

        OpenBookPanel info = new OpenBookPanel();
        int r = JOptionPane.showConfirmDialog(this, info, "Open Book", JOptionPane.OK_CANCEL_OPTION);
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
        toolBar.disableSentence();
        toolBar.enableStop();

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
                } catch (Exception e) {
                    playing = null;
                    if (play != null) {
                        play.drain();
                        play.stop();
                        play.close();
                    }
                    play = null;
                }
                toolBar.enableSentence();
                toolBar.disableStop();
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
        toolBar.disableSentence();
        toolBar.enableStop();

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
                toolBar.enableSentence();
                toolBar.disableStop();
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
            equaliserWindow.add(book.equaliser);
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
                File archiveDir = new File(storageDir, "archive");

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
}
