package uk.co.majenko.audiobookrecorder;

import javax.swing.*;

public class Icons {
    static public ImageIcon book;
    static public ImageIcon chapter;
    static public ImageIcon sentence;
    static public ImageIcon play;  
    static public ImageIcon playon;
    static public ImageIcon stop;
    static public ImageIcon record;

    static public ImageIcon openBook;
    static public ImageIcon newBook;
    static public ImageIcon newChapter;
    static public ImageIcon recordRoom;
    static public ImageIcon save;

    static void loadIcons() {
        book = new ImageIcon(Icons.class.getResource("icons/book.png"));
        chapter = new ImageIcon(Icons.class.getResource("icons/chapter.png"));
        sentence = new ImageIcon(Icons.class.getResource("icons/sentence.png"));
        play = new ImageIcon(Icons.class.getResource("icons/play.png"));
        playon = new ImageIcon(Icons.class.getResource("icons/playon.png"));
        stop = new ImageIcon(Icons.class.getResource("icons/stop.png"));
        record = new ImageIcon(Icons.class.getResource("icons/record.png"));

        openBook = new ImageIcon(Icons.class.getResource("icons/open.png"));
        newBook = new ImageIcon(Icons.class.getResource("icons/new.png"));
        newChapter = new ImageIcon(Icons.class.getResource("icons/new-chapter.png"));
        recordRoom = new ImageIcon(Icons.class.getResource("icons/record-room.png"));
        save = new ImageIcon(Icons.class.getResource("icons/save.png"));

    }
}


