package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import javax.swing.tree.*;
import davaguine.jeq.core.IIRControls;
import javax.sound.sampled.*;

public class Book extends DefaultMutableTreeNode {
    
    String name;
    String author;
    String genre;
    String comment;

    int sampleRate;
    int channels;
    int resolution;

    ImageIcon icon;

    public Equaliser equaliser;

    float[] eqChannels = new float[31];

    public Book(String bookname) {
        super(bookname);
        name = bookname;
        equaliser = new Equaliser();
        AudiobookRecorder.window.setTitle("AudioBook Recorder :: " + name);
    }

    public void setAuthor(String a) {
        author = a;
    }

    public void setGenre(String g) {
        genre = g;
    }

    public void setComment(String c) {
        comment = c;
    }

    public String getAuthor() {
        return author;
    }

    public String getGenre() {
        return genre;
    }

    public String getComment() {
        return comment;
    }

    public Chapter getClosingCredits() {
        return getChapterById("close");
    }
    
    public Chapter getOpeningCredits() {
        return getChapterById("open");
    }

    @SuppressWarnings("unchecked")
    public Chapter getChapterById(String id) {
        for (Enumeration o = children(); o.hasMoreElements();) {
            Object ob = (Object)o.nextElement();
            if (ob instanceof Chapter) {
                Chapter c = (Chapter)ob;
                if (c.getId().equals(id)) {
                    return c;
                }
            }
        }
        return null;
    }

    public Chapter getLastChapter() {
        Chapter cc = getClosingCredits();
        if (cc == null) return null;
        Chapter c = (Chapter)getChildBefore(cc);
        if (c == null) return null;
        if (c.getId().equals("open")) return null;
        return c;
    }

    public Chapter getChapter(int n) {
        if (n == 0) return null;
        return (Chapter)getChildAt(n);
    }

    public Chapter addChapter() {
        Chapter lc = getLastChapter();
        if (lc == null) return new Chapter("1", "Chapter 1");
        try {
            int lcid = Integer.parseInt(lc.getId());
            lcid++;

            Chapter nc = new Chapter(String.format("%04d", lcid), "Chapter " + lcid);
            return nc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public ImageIcon getIcon() {
        return icon;        
    }

    public void setIcon(ImageIcon i) {
        icon = i;
    }

    public void setUserObject(Object o) {
        if (o instanceof String) {
            String newName = (String)o;

            File oldDir = new File(Options.get("path.storage"), name);
            File newDir = new File(Options.get("path.storage"), newName);

            if (newDir.exists()) {
                JOptionPane.showMessageDialog(AudiobookRecorder.window, "Book already exists", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (oldDir.exists() && oldDir.isDirectory()) {
                oldDir.renameTo(newDir);
                name = newName;
                AudiobookRecorder.window.saveBookStructure();
                AudiobookRecorder.window.bookTreeModel.reload(this);
                Options.set("path.last-book", name);
                Options.savePreferences();
                AudiobookRecorder.window.setTitle("AudioBook Recorder :: " + name);
            }
        }
    }

    public String toString() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public void renumberChapters() {
        int id = 1;

        for (Enumeration c = children(); c.hasMoreElements();) {
            Chapter chp = (Chapter)c.nextElement();
            if (Utils.s2i(chp.getId()) > 0) {
                chp.setId(String.format("%04d", id));
                id++;
            }
        }
    }

    public int getSampleRate() { return sampleRate; }
    public void setSampleRate(int sr) { sampleRate = sr; }
    public int getChannels() { return channels; }
    public void setChannels(int c) { channels = c; }
    public int getResolution() { return resolution; }
    public void setResolution(int r) { resolution = r; }

    public AudioFormat getAudioFormat() {
        return new AudioFormat(getSampleRate(), getResolution(), getChannels(), true, false);
    }
}
