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

public class AudiobookRecorder extends JFrame {

    MainToolBar toolBar;

    JMenuBar menuBar;
    JMenu fileMenu;
    JMenu bookMenu;
    JMenu toolsMenu;

    JMenuItem fileNewBook;
    JMenuItem fileOpenBook;
    JMenuItem fileExit;

    JMenuItem bookNewChapter;
    JMenuItem bookExportAudio;

    JMenuItem toolsOptions;

    FlashPanel centralPanel;

    JPanel statusBar;

    JLabel statusLabel;

    JScrollPane mainScroll;

    Book book = null;

    JTree bookTree;
    DefaultTreeModel bookTreeModel;

    Sentence recording = null;
    Sentence playing = null;
    Sentence roomNoise = null;
    Sentence selectedSentence = null;

    JPanel sampleControl;
    Waveform sampleWaveform;

    JSpinner startOffset;
    JSpinner endOffset;
    JSpinner postSentenceGap;

    Thread playingThread = null;

    Random rng = new Random();

    public static AudiobookRecorder window;

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
                createNewBook();
            }
        });
        fileOpenBook = new JMenuItem("Open Book...");
        fileOpenBook.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openBook();
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
                Options o = new Options(AudiobookRecorder.this);
            }
        });
        
        toolsMenu.add(toolsOptions);

        menuBar.add(toolsMenu);

        ob.add(menuBar, BorderLayout.NORTH);

        setPreferredSize(new Dimension(700, 500));
        setLocationRelativeTo(null);

        pack();

        setVisible(true);

    }

    public AudiobookRecorder() {

        window = this;


        Icons.loadIcons();


        try {
//            String clsname = "com.jtattoo.plaf.aluminium.AluminiumLookAndFeel";
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
                }
            }
        });

        endOffset.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSpinner ob = (JSpinner)e.getSource();
                if (selectedSentence != null) {
                    selectedSentence.setEndOffset((Integer)ob.getValue());
                    sampleWaveform.setRightMarker((Integer)ob.getValue());
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

        JPanel controls = new JPanel();
        controls.add(new JLabel("Start Offset:"));

        JButton startFastDown = new JButton("<<");
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
        controls.add(startFastDown);

        JButton startSlowDown = new JButton("<");
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
        controls.add(startSlowDown);

        controls.add(startOffset);

        JButton startSlowUp = new JButton(">");
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
        controls.add(startSlowUp);

        JButton startFastUp = new JButton(">>");
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
        controls.add(startFastUp);


        controls.add(new JLabel("End Offset:"));

        JButton endFastDown = new JButton("<<");
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
        controls.add(endFastDown);

        JButton endSlowDown = new JButton("<");
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
        controls.add(endSlowDown);

        controls.add(endOffset);

        JButton endSlowUp = new JButton(">");
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
        controls.add(endSlowUp);

        JButton endFastUp = new JButton(">>");
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
        controls.add(endFastUp);

        controls.add(new JLabel("Post gap:"));
        controls.add(postSentenceGap);
        controls.add(new JLabel("ms"));

        sampleControl.add(controls, BorderLayout.SOUTH);

        centralPanel.add(sampleControl, BorderLayout.SOUTH);

        statusBar = new JPanel();
        add(statusBar, BorderLayout.SOUTH);

        statusLabel = new JLabel("Noise floor: " + getNoiseFloor());
        statusBar.add(statusLabel);

        buildToolbar(centralPanel);

        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("R"), "startRecord");
        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released R"), "stopRecord");

        centralPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released D"), "deleteLast");

        centralPanel.getActionMap().put("startRecord", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (bookTree.isEditing()) return;
                startRecording();
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

        mainScroll = new JScrollPane();
        centralPanel.add(mainScroll, BorderLayout.CENTER);

        setTitle("AudioBook Recorder");
    }

    public static void main(String args[]) {
        AudiobookRecorder frame = new AudiobookRecorder();
    }

    public void createNewBook() {
        BookInfoPanel info = new BookInfoPanel("", "", "", "");
        int r = JOptionPane.showConfirmDialog(this, info, "New Book", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;

        System.err.println("Name: " + info.getTitle());
        System.err.println("Author: " + info.getAuthor());
        System.err.println("Genre: " + info.getGenre());
        System.err.println("Comment: " + info.getComment());
        
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

    void treePopup(MouseEvent e) {

        int selRow = bookTree.getRowForLocation(e.getX(), e.getY());
        TreePath selPath = bookTree.getPathForLocation(e.getX(), e.getY());
        if (selRow != -1) {

            DefaultMutableTreeNode node = (DefaultMutableTreeNode)selPath.getLastPathComponent();

            if (node instanceof Sentence) {
                Sentence s = (Sentence)node;

                bookTree.setSelectionPath(new TreePath(s.getPath()));

                JPopupMenu menu = new JPopupMenu();
                JMenuObject ins = new JMenuObject("Insert sentence above", s);
                JMenuObject del = new JMenuObject("Delete sentence", s);
                JMenuObject edit = new JMenuObject("Edit text", s);
                JMenuObject process = new JMenuObject("Reprocess audio", s);

                del.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuObject o = (JMenuObject)e.getSource();
                        Sentence s = (Sentence)o.getObject();
                        s.deleteFiles();
                        Chapter c = (Chapter)s.getParent();
                        bookTreeModel.removeNodeFromParent(s);
                    }
                });

                edit.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuObject o = (JMenuObject)e.getSource();
                        Sentence s = (Sentence)o.getObject();
                        s.editText();
                        Chapter c = (Chapter)s.getParent();
                        bookTreeModel.reload(s);
                    }
                });

                process.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuObject o = (JMenuObject)e.getSource();
                        Sentence s = (Sentence)o.getObject();
                        s.autoTrimSample();
                        sampleWaveform.setData(s.getAudioData());
                        sampleWaveform.setMarkers(s.getStartOffset(), s.getEndOffset());
                        startOffset.setValue(s.getStartOffset());
                        endOffset.setValue(s.getEndOffset());
                        postSentenceGap.setValue(s.getPostGap());
                    }
                });

                menu.add(del);
                menu.add(edit);
                menu.add(process);
                menu.show(bookTree, e.getX(), e.getY());
            } else if (node instanceof Chapter) {
                Chapter c = (Chapter)node;

                bookTree.setSelectionPath(new TreePath(c.getPath()));

                JPopupMenu menu = new JPopupMenu();
                JMenuObject ren = new JMenuObject("Rename chapter", c);

                ren.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuObject o = (JMenuObject)e.getSource();
                        Chapter c = (Chapter)o.getObject();
                        c.renameChapter();
                        bookTreeModel.reload(c);
                    }
                });

                menu.add(ren);
                menu.show(bookTree, e.getX(), e.getY());
            }

        }
    }

    public void startRecording() {

        if (recording != null) return;
        if (book == null) return;

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
                i++;
            }
        }

        try {
            FileOutputStream fos = new FileOutputStream(config);
            prefs.storeToXML(fos, "Audiobook Recorder Description");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadBookStructure(File f) {
        try {
            Properties prefs = new Properties();
            FileInputStream fis = new FileInputStream(f);
            prefs.loadFromXML(fis);

            buildBook(prefs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buildBook(Properties prefs) {

        book = new Book(prefs.getProperty("book.name"));

        book.setAuthor(prefs.getProperty("book.author"));
        book.setGenre(prefs.getProperty("book.genre"));
        book.setComment(prefs.getProperty("book.comment"));

        bookTreeModel = new DefaultTreeModel(book);
        bookTree = new JTree(bookTreeModel);
        bookTree.setEditable(true);
        bookTree.setUI(new CustomTreeUI(mainScroll));

        roomNoise = new Sentence("room-noise", "Room Noise");

        bookTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode n = (DefaultMutableTreeNode)bookTree.getLastSelectedPathComponent();
                if (n instanceof Sentence) {
                    Sentence s = (Sentence)n;
                    selectedSentence = s;
                    sampleWaveform.setData(s.getAudioData());
                    sampleWaveform.setMarkers(s.getStartOffset(), s.getEndOffset());
                    startOffset.setValue(s.getStartOffset());
                    endOffset.setValue(s.getEndOffset());
                    postSentenceGap.setValue(s.getPostGap());

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


        Chapter c = new Chapter("open", prefs.getProperty("chapter.open.name"));
        c.setPostGap(s2i(prefs.getProperty("chapter.open.post-gap")));
        c.setPreGap(s2i(prefs.getProperty("chapter.open.pre-gap")));
        bookTreeModel.insertNodeInto(c, book, 0);
        
        for (int i = 0; i < 100000000; i++) {
            String id = prefs.getProperty(String.format("chapter.open.sentence.%08d.id", i));
            String text = prefs.getProperty(String.format("chapter.open.sentence.%08d.text", i));
            int gap = s2i(prefs.getProperty(String.format("chapter.open.sentence.%08d.post-gap", i)));
            if (id == null) break;
            Sentence s = new Sentence(id, text);
            s.setPostGap(gap);
            s.setStartOffset(s2i(prefs.getProperty(String.format("chapter.open.sentence.%08d.start-offset", i))));
            s.setEndOffset(s2i(prefs.getProperty(String.format("chapter.open.sentence.%08d.end-offset", i))));
            bookTreeModel.insertNodeInto(s, c, c.getChildCount());
        }

        for (int cno = 1; cno < 10000; cno++) {
            String cname = prefs.getProperty(String.format("chapter.%04d.name", cno));
            if (cname == null) break;

            c = new Chapter(String.format("%04d", cno), cname);
            c.setPostGap(s2i(prefs.getProperty(String.format("chapter.%04d.post-gap", cno))));
            c.setPreGap(s2i(prefs.getProperty(String.format("chapter.%04d.pre-gap", cno))));
            bookTreeModel.insertNodeInto(c, book, book.getChildCount());

            for (int i = 0; i < 100000000; i++) {
                String id = prefs.getProperty(String.format("chapter.%04d.sentence.%08d.id", cno, i));
                String text = prefs.getProperty(String.format("chapter.%04d.sentence.%08d.text", cno, i));
                int gap = s2i(prefs.getProperty(String.format("chapter.%04d.sentence.%08d.post-gap", cno, i)));
                if (id == null) break;
                Sentence s = new Sentence(id, text);
                s.setPostGap(gap);
                s.setStartOffset(s2i(prefs.getProperty(String.format("chapter.%04d.sentence.%08d.start-offset", cno, i))));
                s.setEndOffset(s2i(prefs.getProperty(String.format("chapter.%04d.sentence.%08d.end-offset", cno, i))));
                bookTreeModel.insertNodeInto(s, c, c.getChildCount());
            }
        }

        c = new Chapter("close", prefs.getProperty("chapter.close.name"));
        c.setPostGap(s2i(prefs.getProperty("chapter.close.post-gap")));
        c.setPreGap(s2i(prefs.getProperty("chapter.close.pre-gap")));
        bookTreeModel.insertNodeInto(c, book, book.getChildCount());

        for (int i = 0; i < 100000000; i++) {
            String id = prefs.getProperty(String.format("chapter.close.sentence.%08d.id", i));
            String text = prefs.getProperty(String.format("chapter.close.sentence.%08d.text", i));
            int gap = s2i(prefs.getProperty(String.format("chapter.close.sentence.%08d.post-gap", i)));
            if (id == null) break;
            Sentence s = new Sentence(id, text);
            s.setPostGap(gap);
            s.setStartOffset(s2i(prefs.getProperty(String.format("chapter.close.sentence.%08d.start-offset", i))));
            s.setEndOffset(s2i(prefs.getProperty(String.format("chapter.close.sentence.%08d.end-offset", i))));
            bookTreeModel.insertNodeInto(s, c, c.getChildCount());
        }

        bookTree.expandPath(new TreePath(book.getPath()));

        toolBar.enableBook();
        statusLabel.setText("Noise floor: " + getNoiseFloor());
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
            
        }

        
    }

    int s2i(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
        }
        return 0;
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

    @SuppressWarnings("unchecked")
    public void exportAudio() {

        try {
            File bookRoot = new File(Options.get("path.storage"), book.getName());
            if (!bookRoot.exists()) {
                bookRoot.mkdirs();
            }

            File export = new File(bookRoot, "export");
            if (!export.exists()) {
                export.mkdirs();
            }

            Encoder encoder = new Encoder();
            EncodingAttributes attributes = new EncodingAttributes();

            AudioAttributes audioAttributes = new AudioAttributes();
            audioAttributes.setCodec("libmp3lame");
            audioAttributes.setBitRate(new Integer(256000));
            audioAttributes.setSamplingRate(new Integer(44100));
            audioAttributes.setChannels(new Integer(2));
            
            attributes.setFormat("mp3");
            attributes.setAudioAttributes(audioAttributes);


            AudioFormat format = roomNoise.getAudioFormat();
            byte[] data;

            for (Enumeration<Chapter> o = book.children(); o.hasMoreElements();) {

                int fullLength = 0;

                Chapter c = o.nextElement();

                String name = c.getName();

                File exportFile = new File(export, name + ".wax");
                File wavFile = new File(export, name + ".wav");
                File mp3File = new File(export, name + "-untagged.mp3");
                File taggedFile = new File(export, name + ".mp3");

                FileOutputStream fos = new FileOutputStream(exportFile);

                data = getRoomNoise(s2i(Options.get("catenation.pre-chapter")));
                fullLength += data.length;
                fos.write(data);

                for (Enumeration<Sentence> s = c.children(); s.hasMoreElements();) {
                    Sentence snt = s.nextElement();
                    data = snt.getRawAudioData();

                    fullLength += data.length;
                    fos.write(data);

                    if (s.hasMoreElements()) {
                        data = getRoomNoise(snt.getPostGap());
                    } else {
                        data = getRoomNoise(s2i(Options.get("catenation.post-chapter")));
                    }
                    fullLength += data.length;
                    fos.write(data);
                }
                fos.close();

                FileInputStream fis = new FileInputStream(exportFile);
                AudioInputStream ais = new AudioInputStream(fis, format, fullLength);
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavFile);
                fis.close();
                exportFile.delete();

                encoder.encode(wavFile, mp3File, attributes);
                Mp3File id3 = new Mp3File(mp3File);

                ID3v2 tags = new ID3v24Tag();
                id3.setId3v2Tag(tags);

                tags.setTrack(Integer.toString(s2i(c.getId()) - 0));
                tags.setTitle(c.getName());
                tags.setArtist(book.getAuthor());

//                ID3v2TextFrameData g = new ID3v2TextFrameData(false, new EncodedText(book.getGenre()));
//                tags.addFrame(tags.createFrame("TCON", g.toBytes(), true));

                tags.setComment(book.getComment());

                id3.save(taggedFile.getAbsolutePath());

                mp3File.delete();
                wavFile.delete();
                
            }

            JOptionPane.showMessageDialog(this, "Book exported.", "Done", JOptionPane.INFORMATION_MESSAGE);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void playFromSelectedSentence() {
        if (selectedSentence == null) return;
        if (playing != null) return;
        playing = selectedSentence;
        toolBar.disableSentence();
        toolBar.enableStop();

        playingThread = new Thread(new Runnable() {
            public void run() {
                Sentence s = playing;
                byte[] data;

                try {

                    AudioFormat format = s.getAudioFormat();
                    SourceDataLine play = AudioSystem.getSourceDataLine(format, Options.getPlaybackMixer());
                    play.open(format);
                    play.start();

                    while (playing != null) {
                        DefaultMutableTreeNode prev = s.getPreviousSibling();
                        boolean first = false;
                        if (prev == null) {
                            first = true;
                        } else if (!(prev instanceof Sentence)) {
                            first = true;
                        }
                        if (first) {
                            data = getRoomNoise(s2i(Options.get("catenation.pre-chapter")));
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
                            data = getRoomNoise(s2i(Options.get("catenation.post-chapter")));
                            play.write(data, 0, data.length);
                            playing = null;
                        } else {
                            data = getRoomNoise(s.getPostGap());
                            play.write(data, 0, data.length);
                        }
                        s = (Sentence)next;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    playing = null;
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
        int frameSize = f.getFrameSize();
        
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
        playing = null;
    }
}
