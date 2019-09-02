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
    JButtonSpacePlay playtoSentence;
    JButtonSpacePlay stopPlaying;
    JButtonSpacePlay eq;
    JToggleButtonSpacePlay mic;

    JToggleButtonSpacePlay disableEffects;

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

        playtoSentence = new JButtonSpacePlay(Icons.playto, "Play sentence to cut", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.playToSelectedSentence();
            }
        });
        add(playtoSentence);
        playtoSentence.setEnabled(false);

        stopPlaying = new JButtonSpacePlay(Icons.stop, "Stop playing", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.stopPlaying();
            }
        });
        add(stopPlaying);

        addSeparator();
        eq = new JButtonSpacePlay(Icons.eq, "Reload Effects", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.loadEffects();
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

        addSeparator();

        disableEffects = new JToggleButtonSpacePlay(Icons.disable, "Disable effects", new ActionListener() {
            Color bgCol = null;
            public void actionPerformed(ActionEvent e) {
                JToggleButton b = (JToggleButton)e.getSource();
                if (b.isSelected()) {
                    root.setEffectsEnabled(false);
                    bgCol = b.getBackground();
                    b.setBackground(Color.RED);
                } else {
                    root.setEffectsEnabled(true);
                    if (bgCol != null) {
                        b.setBackground(bgCol);
                    }
                }
            }
        });

        add(disableEffects);



        setFloatable(false);
    }

    public void enablePlayTo(boolean b) {
        playtoSentence.setEnabled(b);
    }

}
