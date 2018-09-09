package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

public class MainToolBar extends JToolBar {

    JButton newBook;
    JButton openBook;
    JButton saveBook;
    JButton newChapter;
    JButton recordRoomNoise;
    JButton playSentence;
    JButton playonSentence;
    JButton recordSentence;

    AudiobookRecorder root;

    public MainToolBar(AudiobookRecorder r) {
        super();

        root = r;

        newBook = new JButton(Icons.newBook);
        newBook.setToolTipText("New Book");
        newBook.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.createNewBook();
            }
        });
        add(newBook);

        openBook = new JButton(Icons.openBook);
        openBook.setToolTipText("Open Book");
        openBook.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.openBook();
            }
        });
        add(openBook);

        saveBook = new JButton(Icons.save);
        saveBook.setToolTipText("Save Book");
        saveBook.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.saveBookStructure();
            }
        });
        add(saveBook);

        addSeparator();

        newChapter = new JButton(Icons.newChapter);
        newChapter.setToolTipText("New Chapter");
        newChapter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.addChapter();
            }
        });
        add(newChapter);

        recordRoomNoise = new JButton(Icons.recordRoom);
        recordRoomNoise.setToolTipText("Record Room Noise");
        recordRoomNoise.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.recordRoomNoise();
            }
        });
        add(recordRoomNoise);

        addSeparator();

        playSentence = new JButton(Icons.play);
        playSentence.setToolTipText("Play sentence");
        playSentence.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                root.playSelectedSentence();
            }
        });
        add(playSentence);

        playonSentence = new JButton(Icons.playon);
        playonSentence.setToolTipText("Play from sentence");
        add(playonSentence);

        recordSentence = new JButton(Icons.record);
        recordSentence.setToolTipText("Re-record sentence");
        add(recordSentence);

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
        recordSentence.setEnabled(true);
    }

    public void disableSentence() {
        playSentence.setEnabled(false);
        playonSentence.setEnabled(false);
        recordSentence.setEnabled(false);
    }
}
