package uk.co.majenko.audiobookrecorder;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.UUID;
import java.util.Properties;
import javax.sound.sampled.AudioFormat;
import javax.swing.JOptionPane;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultTreeModel;
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

public class Book extends BookTreeNode {
    
    String name;
    String author;
    String genre;
    String comment;
    String ACX;
    String manuscript;

    String defaultEffect = "none";

    int sampleRate;
    int channels;
    int resolution;

    String notes = null;

    ImageIcon icon;

    Properties prefs;

    File location;

    public Book(Properties p, String bookname) {
        super(bookname);
        Debug.trace();
        prefs = p;
        name = bookname;
        AudiobookRecorder.window.setTitle("AudioBook Recorder :: " + name); // This should be in the load routine!!!!
    }

    public Book(Element root) {
        super(getTextNode(root, "title"));
        Debug.trace();
        name = getTextNode(root, "title");
        AudiobookRecorder.window.setTitle("AudioBook Recorder :: " + name); // This should be in the load routine!!!!
    }

    public void loadBookXML(Element root, DefaultTreeModel model) {
        Debug.trace();
        name = getTextNode(root, "title");
        author = getTextNode(root, "author");
        genre = getTextNode(root, "genre");
        comment = getTextNode(root, "comment");
        ACX = getTextNode(root, "acx");
        manuscript = getTextNode(root, "manuscript");

        AudiobookRecorder.window.setBookNotes(getTextNode(root, "notes"));
        notes = getTextNode(root, "notes");

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
        Debug.trace();
        NodeList nl = r.getElementsByTagName(n);
        if (nl == null) return null;
        if (nl.getLength() == 0) return null;
        return (Element)nl.item(0);
    }

    public static String getTextNode(Element r, String n) {
        Debug.trace();
        return getTextNode(r, n, "");
    }

    public static String getTextNode(Element r, String n, String d) {
        Debug.trace();
        Element node = getNode(r, n);
        if (node == null) return d;
        return node.getTextContent();
    }

    public void setAuthor(String a) { Debug.trace(); author = a; }
    public void setGenre(String g) { Debug.trace(); genre = g; }
    public void setComment(String c) { Debug.trace(); comment = c; }
    public void setACX(String c) { Debug.trace(); ACX = c; }

    public String getAuthor() { Debug.trace(); return author; }
    public String getGenre() { Debug.trace(); return genre; }
    public String getComment() { Debug.trace(); return comment; }
    public String getACX() { Debug.trace(); if (ACX == null) return ""; return ACX; }

    public Chapter getClosingCredits() {
        Debug.trace();
        return getChapterById("close");
    }
    
    public Chapter getOpeningCredits() {
        Debug.trace();
        return getChapterById("open");
    }

    @SuppressWarnings("unchecked")
    public Chapter getChapterById(String id) {
        Debug.trace();
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
        Debug.trace();
        Chapter cc = getClosingCredits();
        if (cc == null) return null;
        Chapter c = (Chapter)getChildBefore(cc);
        if (c == null) return null;
        if (c.getId().equals("open")) return null;
        return c;
    }

    public Chapter getChapter(int n) {
        Debug.trace();
        if (n == 0) return null;
        return (Chapter)getChildAt(n);
    }

    public Chapter addChapter() {
        Debug.trace();
        String uuid = UUID.randomUUID().toString();
        return new Chapter(uuid, uuid);
    }

    public String getName() {
        Debug.trace();
        return name;
    }

    public ImageIcon getIcon() {
        Debug.trace();
        return icon;        
    }

    public void setIcon(ImageIcon i) {
        Debug.trace();
        icon = i;
    }

    public void setUserObject(Object o) {
        Debug.trace();
        if (o instanceof String) {
            String newName = (String)o;
            if (newName.equals(name)) return;
            renameBook(newName);
        }
    }

    public File getBookPath() {
        Debug.trace();
        return new File(Options.get("path.storage"), name);
    }

    public void renameBook(String newName) {
        Debug.trace();
        File oldDir = getBookPath();
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
        Debug.trace();
        return name;
    }

    @SuppressWarnings("unchecked")
    public void renumberChapters() {
        Debug.trace();
        int id = 1;

        for (Enumeration c = children(); c.hasMoreElements();) {
            Chapter chp = (Chapter)c.nextElement();
            if (Utils.s2i(chp.getId()) > 0) {
                chp.setId(String.format("%04d", id));
                id++;
            }
        }
    }

    public int getSampleRate() { Debug.trace(); return sampleRate; }
    public void setSampleRate(int sr) { Debug.trace(); sampleRate = sr; }
    public int getChannels() { Debug.trace(); return channels; }
    public void setChannels(int c) { Debug.trace(); channels = c; }
    public int getResolution() { Debug.trace(); return resolution; }
    public void setResolution(int r) { Debug.trace(); resolution = r; }

    public AudioFormat getAudioFormat() {
        Debug.trace();
        return new AudioFormat(getSampleRate(), getResolution(), getChannels(), true, false);
    }

    public String get(String key) {
        Debug.trace();
        if (prefs.getProperty(key) == null) { return Options.get(key); }
        return prefs.getProperty(key);
    }

    public Integer getInteger(String key) {
        Debug.trace();
        if (prefs.getProperty(key) == null) { return Options.getInteger(key); }
        return Utils.s2i(prefs.getProperty(key));
    }

    public void set(String key, String value) {
        Debug.trace();
        prefs.setProperty(key, value);
    }

    public void set(String key, Integer value) {
        Debug.trace();
        prefs.setProperty(key, "" + value);
    }

    public File getBookFolder() {
        Debug.trace();
        File dir = new File(Options.get("path.storage"), name);
        return dir;
    }

    public ArrayList<String> getUsedEffects() {
        Debug.trace();

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
        Debug.trace();
        for (Enumeration o = children(); o.hasMoreElements();) {
            Object ob = (Object)o.nextElement();
            if (ob instanceof Chapter) {
                Chapter c = (Chapter)ob;
                c.purgeBackups();
            }
        }
    }

    public Document buildDocument() throws ParserConfigurationException {
        Debug.trace();
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
        root.appendChild(makeTextNode(doc, "manuscript", manuscript));

        root.appendChild(makeTextNode(doc, "notes", AudiobookRecorder.window.getBookNotes()));

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
        Debug.trace();
        Element node = doc.createElement(name);
        Text tnode = doc.createTextNode(text == null ? "" : text);
        node.appendChild(tnode);
        return node;
    }

    public static Element makeTextNode(Document doc, String name, Integer text) {
        Debug.trace();
        Element node = doc.createElement(name);
        Text tnode = doc.createTextNode(Integer.toString(text));
        node.appendChild(tnode);
        return node;
    }

    public static Element makeTextNode(Document doc, String name, Double text) {
        Debug.trace();
        Element node = doc.createElement(name);
        Text tnode = doc.createTextNode(String.format("%.8f", text));
        node.appendChild(tnode);
        return node;
    }

    public static Element makeTextNode(Document doc, String name, Boolean text) {
        Debug.trace();
        Element node = doc.createElement(name);
        Text tnode = doc.createTextNode(text ? "true" : "false");
        node.appendChild(tnode);
        return node;
    }

    public String getDefaultEffect() {
        Debug.trace();
        return defaultEffect;
    }

    public void setDefaultEffect(String eff) {
        Debug.trace();
        defaultEffect = eff;
    }

    public void setManuscript(File f) {
        Debug.trace();
        manuscript = f.getName();
        File dst = new File(getBookPath(), manuscript);

        try {
            Files.copy(f.toPath(), dst.toPath());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public File getManuscript() {
        Debug.trace();
        if (manuscript == null) return null;
        if (manuscript.equals("")) return null;
        File f = new File(getBookPath(), manuscript);
        if (f.exists()) { 
            return f;
        }
        return null;
    }

    public void onSelect() {
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String n) {
        notes = n;
    }

    public File getLocation() { 
        return location;
    }

    public void setLocation(File l) {
        location = l;
    }
}
