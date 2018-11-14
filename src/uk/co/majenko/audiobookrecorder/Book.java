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
    String ACX;

    int sampleRate;
    int channels;
    int resolution;

    ImageIcon icon;

    public Equaliser[] equaliser = new Equaliser[2];

    float[] eqChannels = new float[31];

    Properties prefs;

    public Book(Properties p, String bookname) {
        super(bookname);

        prefs = p;
        name = bookname;
        equaliser[0] = new Equaliser("Default");
        equaliser[1] = new Equaliser("Phone");
        AudiobookRecorder.window.setTitle("AudioBook Recorder :: " + name);
    }

    public void setAuthor(String a) { author = a; }
    public void setGenre(String g) { genre = g; }
    public void setComment(String c) { comment = c; }
    public void setACX(String c) { ACX = c; }

    public String getAuthor() { return author; }
    public String getGenre() { return genre; }
    public String getComment() { return comment; }
    public String getACX() { if (ACX == null) return ""; return ACX; }

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
            if (newName.equals(name)) return;
            renameBook(newName);
        }
    }

    public void renameBook(String newName) {
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

    public String get(String key) {
        if (prefs.getProperty(key) == null) { return Options.get(key); }
        return prefs.getProperty(key);
    }

    public Integer getInteger(String key) {
        if (prefs.getProperty(key) == null) { return Options.getInteger(key); }
        return Utils.s2i(prefs.getProperty(key));
    }

    public void set(String key, String value) {
        prefs.setProperty(key, value);
    }

    public void set(String key, Integer value) {
        prefs.setProperty(key, "" + value);
    }
}
