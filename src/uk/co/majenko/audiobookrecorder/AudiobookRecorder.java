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
    Waveform sampleWaveform;

    JSpinner startOffset;
    JSpinner endOffset;
    JSpinner postSentenceGap;
    JCheckBox locked;

    JButton reprocessAudioFFT;
    JButton reprocessAudioPeak;

    JButton startSlowDown;
    JButton startSlowUp;
    JButton startFastDown;
    JButton startFastUp;

    JButton endSlowDown;
    JButton endSlowUp;
    JButton endFastDown;
    JButton endFastUp;

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
        
        toolsOptions = new JMenuItem("Options");
        toolsOptions.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new Options(AudiobookRecorder.this);
            }
        });
        
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
    
        sampleControl.add(sampleWaveform, BorderLayout.CENTER);

        reprocessAudioFFT = new JButton(Icons.fft);
        reprocessAudioFFT.setToolTipText("Autotrim Audio (FFT)");
        reprocessAudioFFT.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (selectedSentence != null) {
                    selectedSentence.autoTrimSampleFFT();
                    sampleWaveform.setData(selectedSentence.getAudioData());
                    sampleWaveform.setMarkers(selectedSentence.getStartOffset(), selectedSentence.getEndOffset());
                    sampleWaveform.setAltMarkers(selectedSentence.getStartCrossing(), selectedSentence.getEndCrossing());
                    startOffset.setValue(selectedSentence.getStartOffset());
                    endOffset.setValue(selectedSentence.getEndOffset());
                    postSentenceGap.setValue(selectedSentence.getPostGap());
                }
            }
        });

        reprocessAudioPeak = new JButton(Icons.peak);
        reprocessAudioPeak.setToolTipText("Autotrim Audio (Peak)");
        reprocessAudioPeak.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (selectedSentence != null) {
                    selectedSentence.autoTrimSamplePeak();
                    sampleWaveform.setData(selectedSentence.getAudioData());
                    sampleWaveform.setMarkers(selectedSentence.getStartOffset(), selectedSentence.getEndOffset());
                    sampleWaveform.setAltMarkers(selectedSentence.getStartCrossing(), selectedSentence.getEndCrossing());
                    startOffset.setValue(selectedSentence.getStartOffset());
                    endOffset.setValue(selectedSentence.getEndOffset());
                    postSentenceGap.setValue(selectedSentence.getPostGap());
                }
            }
        });

        startOffset = new JSpinner(new SteppedNumericSpinnerModel(0, 0, 1, 0));
        startOffset.setPreferredSize(new Dimension(100, 20));
        endOffset = new JSpinner(new SteppedNumericSpinnerModel(0, 0, 1, 0));
        endOffset.setPreferredSize(new Dimension(100, 20));
        postSentenceGap = new JSpinner(new SteppedNumericSpinnerModel(0, 5000, 100, 0));
        postSentenceGap.setPreferredSize(new Dimension(75, 20));

        startOffset.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSpinner ob = (JSpinner)e.getSource();
                if (selectedSentence != null) {
                    selectedSentence.setStartOffset((Integer)ob.getValue());
                    sampleWaveform.setLeftMarker((Integer)ob.getValue());
                    selectedSentence.updateStartCrossing();
                    sampleWaveform.setLeftAltMarker(selectedSentence.getStartCrossing());
                }
            }
        });

        endOffset.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSpinner ob = (JSpinner)e.getSource();
                if (selectedSentence != null) {
                    selectedSentence.setEndOffset((Integer)ob.getValue());
                    sampleWaveform.setRightMarker((Integer)ob.getValue());
                    selectedSentence.updateEndCrossing();
                    sampleWaveform.setRightAltMarker(selectedSentence.getEndCrossing());
                }
            }
        });

        postSentenceGap.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSpinner ob = (JSpinner)e.getSource();
                if (selectedSentence != null) {
                    selectedSentence.setPostGap((Integer)ob.getValue());
                }
            }
        });



        JPanel controlsTop = new JPanel();
        JPanel controlsBottom = new JPanel();
        JToolBar controlsLeft = new JToolBar(JToolBar.VERTICAL);
        JToolBar controlsRight = new JToolBar(JToolBar.VERTICAL);

        controlsLeft.setFloatable(false);
        controlsRight.setFloatable(false);

        controlsLeft.add(reprocessAudioFFT);
        controlsLeft.add(reprocessAudioPeak);

        controlsBottom.add(new JLabel("Start Offset:"));

        startFastDown = new JButton("<<");
        startFastDown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SteppedNumericSpinnerModel m = (SteppedNumericSpinnerModel)startOffset.getModel();
                int f = (Integer)startOffset.getValue();
                int max = m.getMaximum();
                f -= (max / 10);
                if (f < 0) f = 0;
                startOffset.setValue(f);
            }
        });
        controlsBottom.add(startFastDown);

        startSlowDown = new JButton("<");
        startSlowDown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SteppedNumericSpinnerModel m = (SteppedNumericSpinnerModel)startOffset.getModel();
                int f = (Integer)startOffset.getValue();
                int max = m.getMaximum();
                f -= (max / 100);
                if (f < 0) f = 0;
                startOffset.setValue(f);
            }
        });
        controlsBottom.add(startSlowDown);

        controlsBottom.add(startOffset);

        startSlowUp = new JButton(">");
        startSlowUp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SteppedNumericSpinnerModel m = (SteppedNumericSpinnerModel)startOffset.getModel();
                int f = (Integer)startOffset.getValue();
                int max = m.getMaximum();
                f += (max / 100);
                if (f > max) f = max;
                startOffset.setValue(f);
            }
        });
        controlsBottom.add(startSlowUp);

        startFastUp = new JButton(">>");
        startFastUp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SteppedNumericSpinnerModel m = (SteppedNumericSpinnerModel)startOffset.getModel();
                int f = (Integer)startOffset.getValue();
                int max = m.getMaximum();
                f += (max / 10);
                if (f > max) f = max;
                startOffset.setValue(f);
            }
        });
        controlsBottom.add(startFastUp);


        controlsBottom.add(new JLabel("End Offset:"));

        endFastDown = new JButton("<<");
        endFastDown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SteppedNumericSpinnerModel m = (SteppedNumericSpinnerModel)endOffset.getModel();
                int f = (Integer)endOffset.getValue();
                int max = m.getMaximum();
                f -= (max / 10);
                if (f < 0) f = 0;
                endOffset.setValue(f);
            }
        });
        controlsBottom.add(endFastDown);

        endSlowDown = new JButton("<");
        endSlowDown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SteppedNumericSpinnerModel m = (SteppedNumericSpinnerModel)endOffset.getModel();
                int f = (Integer)endOffset.getValue();
                int max = m.getMaximum();
                f -= (max / 100);
                if (f < 0) f = 0;
                endOffset.setValue(f);
            }
        });
        controlsBottom.add(endSlowDown);

        controlsBottom.add(endOffset);

        endSlowUp = new JButton(">");
        endSlowUp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SteppedNumericSpinnerModel m = (SteppedNumericSpinnerModel)endOffset.getModel();
                int f = (Integer)endOffset.getValue();
                int max = m.getMaximum();
                f += (max / 100);
                if (f > max) f = max;
                endOffset.setValue(f);
            }
        });
        controlsBottom.add(endSlowUp);

        endFastUp = new JButton(">>");
        endFastUp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SteppedNumericSpinnerModel m = (SteppedNumericSpinnerModel)endOffset.getModel();
                int f = (Integer)endOffset.getValue();
                int max = m.getMaximum();
                f += (max / 10);
                if (f > max) f = max;
                endOffset.setValue(f);
            }
        });
        controlsBottom.add(endFastUp);

        locked = new JCheckBox("Sentence locked");

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

        controlsTop.add(new JLabel("Post gap:"));
        controlsTop.add(postSentenceGap);
        controlsTop.add(new JLabel("ms"));

        sampleControl.add(controlsTop, BorderLayout.NORTH);
        sampleControl.add(controlsBottom, BorderLayout.SOUTH);
        sampleControl.add(controlsLeft, BorderLayout.WEST);
        sampleControl.add(controlsRight, BorderLayout.EAST);

        centralPanel.add(sampleControl, BorderLayout.SOUTH);

        statusBar = new JPanel();
        add(statusBar, BorderLayout.SOUTH);

        statusLabel = new JLabel("Noise floor: " + getNoiseFloor());
        statusBar.add(statusLabel);

        buildToolbar(centralPanel);

        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("R"), "startRecord");
        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released R"), "stopRecord");

        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("T"), "startRecordNewPara");
        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released T"), "stopRecord");

        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released D"), "deleteLast");

        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "startPlayback");

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

        centralPanel.getActionMap().put("startPlayback", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (bookTree.isEditing()) return;
                playSelectedSentence();
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
                JMenu moveMenu = new JMenu("Move sentence to...");

                for (Enumeration<Chapter> c = book.children(); c.hasMoreElements();) {
                    Chapter chp = c.nextElement();
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


                JMenuObject ins = new JMenuObject("Insert sentence above", s);
                JMenuObject del = new JMenuObject("Delete sentence", s);


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
                menu.show(bookTree, e.getX(), e.getY());
            } else if (node instanceof Chapter) {
                Chapter c = (Chapter)node;

                bookTree.setSelectionPath(new TreePath(c.getPath()));

                JPopupMenu menu = new JPopupMenu();
                JMenuObject peak = new JMenuObject("Auto-trim all (Peak)", c);
                JMenuObject moveUp = new JMenuObject("Move Up", c);
                JMenuObject moveDown = new JMenuObject("Move Down", c);
                JMenu mergeWith = new JMenu("Merge chapter with");
                JMenuObject lockAll = new JMenuObject("Lock all sentences", c);
                JMenuObject unlockAll = new JMenuObject("Unlock all sentences", c);
                JMenuObject exportChapter = new JMenuObject("Export chapter", c);

                exportChapter.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter chap = (Chapter)o.getObject();

                        ExportDialog ed = new ExportDialog("Exporting " + chap.getName());

                        ExportThread t = new ExportThread(chap, ed);
                        Thread nt = new Thread(t);
                        nt.start();
                        ed.setVisible(true);
                    }
                });

                for (Enumeration<Chapter> bc = book.children(); bc.hasMoreElements();) {
                    Chapter chp = bc.nextElement();
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
                    }
                });

                peak.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter c = (Chapter)o.getObject();
                        for (Enumeration<Sentence> s = c.children(); s.hasMoreElements();) {
                            Sentence snt = s.nextElement();
                            if (!snt.isLocked()) {
                                snt.autoTrimSamplePeak();
                            }
                        }
                    }
                });

                lockAll.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter c = (Chapter)o.getObject();
                        for (Enumeration<Sentence> s = c.children(); s.hasMoreElements();) {
                            Sentence snt = s.nextElement();
                            snt.setLocked(true);
                            bookTreeModel.reload(snt);
                        }
                    }
                });
                unlockAll.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter c = (Chapter)o.getObject();
                        for (Enumeration<Sentence> s = c.children(); s.hasMoreElements();) {
                            Sentence snt = s.nextElement();
                            snt.setLocked(false);
                            bookTreeModel.reload(snt);
                        }
                    }
                });
                        

                menu.add(moveUp);
                menu.add(moveDown);

                menu.addSeparator();
                menu.add(mergeWith);

                menu.addSeparator();

                menu.add(peak);

                menu.addSeparator();

                menu.add(lockAll);
                menu.add(unlockAll);
    
                menu.addSeparator();

                menu.add(exportChapter);

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

        for (int i = 0; i < 31; i++) {
            prefs.setProperty("audio.eq." + i, String.format("%.3f", book.equaliser.getChannel(i)));
        }

        for (Enumeration<Chapter> o = book.children(); o.hasMoreElements();) {

            Chapter c = o.nextElement();
            String keybase = "chapter." + c.getId();
            prefs.setProperty(keybase + ".name", c.getName());
            prefs.setProperty(keybase + ".pre-gap", Integer.toString(c.getPreGap()));
            prefs.setProperty(keybase + ".post-gap", Integer.toString(c.getPostGap()));

            int i = 0;
            for (Enumeration<Sentence> s = c.children(); s.hasMoreElements();) {
                Sentence snt = s.nextElement();
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
                    startOffset.setValue(s.getStartOffset());
                    endOffset.setValue(s.getEndOffset());
                    postSentenceGap.setValue(s.getPostGap());
                    locked.setSelected(s.isLocked());

                    postSentenceGap.setEnabled(!s.isLocked());
                    startOffset.setEnabled(!s.isLocked());
                    endOffset.setEnabled(!s.isLocked());
                    reprocessAudioFFT.setEnabled(!s.isLocked());
                    reprocessAudioPeak.setEnabled(!s.isLocked());

                    startSlowDown.setEnabled(!s.isLocked());
                    startSlowUp.setEnabled(!s.isLocked());
                    startFastDown.setEnabled(!s.isLocked());
                    startFastUp.setEnabled(!s.isLocked());

                    endSlowDown.setEnabled(!s.isLocked());
                    endSlowUp.setEnabled(!s.isLocked());
                    endFastDown.setEnabled(!s.isLocked());
                    endFastUp.setEnabled(!s.isLocked());

                    int samples = s.getSampleSize();

                    ((SteppedNumericSpinnerModel)startOffset.getModel()).setMaximum(samples);
                    ((SteppedNumericSpinnerModel)endOffset.getModel()).setMaximum(samples);

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
                    startOffset.setValue(0);
                    endOffset.setValue(0);
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
        statusLabel.setText("Noise floor: " + getNoiseFloor());
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
        ms /= 9;
        return ms;
    }

    public void recordRoomNoise() {
        if (roomNoise.startRecording()) {

            centralPanel.setFlash(true);
            java.util.Timer ticker = new java.util.Timer(true);
            ticker.schedule(new TimerTask() {
                public void run() {
                    roomNoise.stopRecording();
                    centralPanel.setFlash(false);
                    statusLabel.setText("Noise floor: " + getNoiseFloor());
                }
            }, 5000); // 5 seconds of recording
        }
    }

    public void playSelectedSentence() {
        if (selectedSentence == null) return;

        if (playing != null) return;

        playing = selectedSentence;

        toolBar.disableSentence();
        toolBar.enableStop();


        playingThread = new Thread(new Runnable() {
            public void run() {
                playing.play();
                playing = null;
                toolBar.enableSentence();
                toolBar.disableStop();
            }
        });

        playingThread.setDaemon(true);
        playingThread.start();

    }

    class ExportThread implements Runnable {
        ExportDialog exportDialog;
        Chapter chapter;

        public ExportThread(Chapter c, ExportDialog e) {
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

        
        for (Enumeration<Chapter> o = book.children(); o.hasMoreElements();) {
            Chapter c = o.nextElement();
            ExportDialog ed = new ExportDialog("Exporting " + c.getName());

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
                        play.write(data, 0, data.length);

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
            try {
                Process p = Runtime.getRuntime().exec(line);
                p.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
