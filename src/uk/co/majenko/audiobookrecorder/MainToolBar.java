package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

public class MainToolBar extends JToolBar {

    JButtonSpacePlay newBook;
    JButtonSpacePlay openBook;
    JButtonSpacePlay saveBook;
    JButtonSpacePlay newChapter;
    JButtonSpacePlay recordRoomNoise;
    JButtonSpacePlay playSentence;
    JButtonSpacePlay playonSentence;
    JButtonSpacePlay stopPlaying;
    JButtonSpacePlay eq;
    JToggleButton mic;

    AudiobookRecorder root;

    public MainToolBar(AudiobookRecorder r) {
        super();

        root = r;

        newBook = new JButtonSpacePlay(Icons.newBook, "New Book", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.createNewBook();
            }
        });
        add(newBook);

        openBook = new JButtonSpacePlay(Icons.openBook, "Open Book", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.openBook();
            }
        });
        add(openBook);

        saveBook = new JButtonSpacePlay(Icons.save, "Save Book", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.saveBookStructure();
            }
        });
        add(saveBook);

        addSeparator();

        newChapter = new JButtonSpacePlay(Icons.newChapter, "New Chapter", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.addChapter();
            }
        });
        add(newChapter);

        recordRoomNoise = new JButtonSpacePlay(Icons.recordRoom, "Record Room Noise", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.recordRoomNoise();
            }
        });
        add(recordRoomNoise);

        addSeparator();

        playSentence = new JButtonSpacePlay(Icons.play, "Play sentence", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.playSelectedSentence();
            }
        });
        add(playSentence);

        playonSentence = new JButtonSpacePlay(Icons.playon, "Play from sentence", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.playFromSelectedSentence();
            }
        });
        add(playonSentence);

        stopPlaying = new JButtonSpacePlay(Icons.stop, "Stop playing", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.stopPlaying();
            }
        });
        add(stopPlaying);

        addSeparator();
        eq = new JButtonSpacePlay(Icons.eq, "Equaliser", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.showEqualiser();
            }
        });

        add(eq);

        addSeparator();

        mic = new JToggleButtonSpacePlay(Icons.mic, "Enable / disable microphone", new ActionListener() {
            Color bgCol = null;
            public void actionPerformed(ActionEvent e) {
                JToggleButton b = (JToggleButton)e.getSource();
                if (b.isSelected()) {
                    if (!root.enableMicrophone()) {
                        b.setSelected(false);
                    } else {
                        if (bgCol == null) {
                            bgCol = b.getBackground();
                        }
                        b.setBackground(Color.RED);
                    }
                } else {
                    root.disableMicrophone();
                    if (bgCol != null) {
                        b.setBackground(bgCol);
                    }
                }
            }
        });
        
        add(mic);


        setFloatable(false);
    }

    public void enableBook() {
        newChapter.setEnabled(true);
        recordRoomNoise.setEnabled(true);
    }

    public void disableBook() {
        newChapter.setEnabled(false);
        recordRoomNoise.setEnabled(false);
    }

    public void enableSentence() {
        playSentence.setEnabled(true);
        playonSentence.setEnabled(true);
    }

    public void disableSentence() {
        playSentence.setEnabled(false);
        playonSentence.setEnabled(false);
    }

    public void enableStop() {
        stopPlaying.setEnabled(true);
    }

    public void disableStop() {
        stopPlaying.setEnabled(false);
    }
}
