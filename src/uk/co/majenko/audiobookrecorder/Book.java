package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import javax.swing.tree.*;
import javax.sound.sampled.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class Book extends DefaultMutableTreeNode {
    
    String name;
    String author;
    String genre;
    String comment;
    String ACX;

    String defaultEffect = "none";

    int sampleRate;
    int channels;
    int resolution;

    ImageIcon icon;

    Properties prefs;

    public Book(Properties p, String bookname) {
        super(bookname);

        prefs = p;
        name = bookname;
        AudiobookRecorder.window.setTitle("AudioBook Recorder :: " + name); // This should be in the load routine!!!!
    }

    public Book(Element root) {
        super(getTextNode(root, "title"));

        name = getTextNode(root, "title");
        AudiobookRecorder.window.setTitle("AudioBook Recorder :: " + name); // This should be in the load routine!!!!
    }

    public void loadBookXML(Element root, DefaultTreeModel model) {
        name = getTextNode(root, "title");
        author = getTextNode(root, "author");
        genre = getTextNode(root, "genre");
        comment = getTextNode(root, "comment");
        ACX = getTextNode(root, "acx");

        AudiobookRecorder.window.setNotes(getTextNode(root, "notes"));

        Element settings = getNode(root, "settings");
        Element audioSettings = getNode(settings, "audio");
        Element effectSettings = getNode(settings, "effects");

        sampleRate = Utils.s2i(getTextNode(audioSettings, "samplerate"));
        channels = Utils.s2i(getTextNode(audioSettings, "channels"));
        resolution = Utils.s2i(getTextNode(audioSettings, "resolution"));

        defaultEffect = getTextNode(settings, "default");

        AudiobookRecorder.window.setTitle("AudioBook Recorder :: " + name); // This should be in the load routine!!!!

        Element chapters = getNode(root, "chapters");

        NodeList chapterList = chapters.getElementsByTagName("chapter");

        for (int i = 0; i < chapterList.getLength(); i++) {
            Element chapterElement = (Element)chapterList.item(i);
            Chapter newChapter = new Chapter(chapterElement, model);
            model.insertNodeInto(newChapter, this, getChildCount());
        }
    }

    public static Element getNode(Element r, String n) {
        NodeList nl = r.getElementsByTagName(n);
        if (nl == null) return null;
        if (nl.getLength() == 0) return null;
        return (Element)nl.item(0);
    }

    public static String getTextNode(Element r, String n) {
        Element node = getNode(r, n);
        if (node == null) return "";
        return node.getTextContent();
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

    public File getBookFolder() {
        File dir = new File(Options.get("path.storage"), name);
        return dir;
    }

    public ArrayList<String> getUsedEffects() {

        ArrayList<String> out = new ArrayList<String>();

        for (Enumeration o = children(); o.hasMoreElements();) {
            Object ob = (Object)o.nextElement();
            if (ob instanceof Chapter) {
                Chapter c = (Chapter)ob;
                ArrayList<String> effs = c.getUsedEffects();
                for (String ef : effs) {
                    if (out.indexOf(ef) == -1) {
                        out.add(ef);
                    }
                }
            }
        }

        return out;
    }

    public void purgeBackups() {
        for (Enumeration o = children(); o.hasMoreElements();) {
            Object ob = (Object)o.nextElement();
            if (ob instanceof Chapter) {
                Chapter c = (Chapter)ob;
                c.purgeBackups();
            }
        }
    }

    public Document buildDocument() throws ParserConfigurationException {
        DocumentBuilderFactory dbFactory =
        DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.newDocument();

        Element root = doc.createElement("book");
        doc.appendChild(root);

        root.appendChild(makeTextNode(doc, "title", name));
        root.appendChild(makeTextNode(doc, "author", author));
        root.appendChild(makeTextNode(doc, "comment", comment));
        root.appendChild(makeTextNode(doc, "genre", genre));
        root.appendChild(makeTextNode(doc, "acx", ACX));

        root.appendChild(makeTextNode(doc, "notes", AudiobookRecorder.window.getNotes()));

        Element settingsNode = doc.createElement("settings");
        root.appendChild(settingsNode);
        
        Element audioSettingsNode = doc.createElement("audio");
        settingsNode.appendChild(audioSettingsNode);

        audioSettingsNode.appendChild(makeTextNode(doc, "channels", channels));
        audioSettingsNode.appendChild(makeTextNode(doc, "resolution", resolution));
        audioSettingsNode.appendChild(makeTextNode(doc, "samplerate", sampleRate));
        
        Element effectsNode = doc.createElement("effects");
        settingsNode.appendChild(effectsNode);
        
        effectsNode.appendChild(makeTextNode(doc, "default", defaultEffect));

        Element chaptersNode = doc.createElement("chapters");

        root.appendChild(chaptersNode);

        for (Enumeration o = children(); o.hasMoreElements();) {
            Object ob = (Object)o.nextElement();
            if (ob instanceof Chapter) {
                Chapter c = (Chapter)ob;
                chaptersNode.appendChild(c.getChapterXML(doc));
            }
        }

        return doc;
    }

    public static Element makeTextNode(Document doc, String name, String text) {
        Element node = doc.createElement(name);
        Text tnode = doc.createTextNode(text);
        node.appendChild(tnode);
        return node;
    }

    public static Element makeTextNode(Document doc, String name, Integer text) {
        Element node = doc.createElement(name);
        Text tnode = doc.createTextNode(Integer.toString(text));
        node.appendChild(tnode);
        return node;
    }

    public static Element makeTextNode(Document doc, String name, Double text) {
        Element node = doc.createElement(name);
        Text tnode = doc.createTextNode(String.format("%.8f", text));
        node.appendChild(tnode);
        return node;
    }

    public static Element makeTextNode(Document doc, String name, Boolean text) {
        Element node = doc.createElement(name);
        Text tnode = doc.createTextNode(text ? "true" : "false");
        node.appendChild(tnode);
        return node;
    }

    public String getDefaultEffect() {
        return defaultEffect;
    }

    public void setDefaultEffect(String eff) {
        defaultEffect = eff;
    }

}
