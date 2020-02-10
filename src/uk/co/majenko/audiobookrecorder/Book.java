package uk.co.majenko.audiobookrecorder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.UUID;
import java.util.Random;
import java.util.TimerTask;
import java.util.TreeMap;
import java.awt.Image;
import javax.sound.sampled.AudioFormat;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.ImageIcon;
import javax.swing.tree.TreeNode;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public class Book extends BookTreeNode {
    
    String name;
    String author;
    String genre;
    String comment;
    String ACX;
    String manuscript;
    String defaultEffect = "none";
    Sentence roomNoise = null;
    String notes = null;
    ImageIcon icon;
    File location;
    Random rng = new Random();
    TreeMap<String, EffectGroup> effects;
    AudioFormat cachedFormat = null;

    public Book(String bookname) {
        super(bookname);
        Debug.trace();
        name = bookname;
        location = new File(Options.get("path.storage"), sanitize(name));
        AudiobookRecorder.window.setTitle("AudioBook Recorder :: " + name); // This should be in the load routine!!!!
        setIcon(Icons.book);
        roomNoise = new Sentence("room-noise", "Room Noise");
        roomNoise.setParentBook(this);
        loadEffects();
    }

    public Book(File inputFile) throws SAXException, IOException, ParserConfigurationException {
        Debug.trace();
        Debug.d("Loading book from", inputFile.getCanonicalPath());
        if (inputFile.getName().endsWith(".abx")) {
            location = inputFile.getParentFile();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();

            name = getTextNode(root, "title");
            author = getTextNode(root, "author");
            genre = getTextNode(root, "genre");
            comment = getTextNode(root, "comment");
            ACX = getTextNode(root, "acx");
            manuscript = getTextNode(root, "manuscript");
            notes = getTextNode(root, "notes");

            Element settings = getNode(root, "settings");
            Element audioSettings = getNode(settings, "audio");
            Element effectSettings = getNode(settings, "effects");

            defaultEffect = getTextNode(settings, "default");

            AudiobookRecorder.window.setTitle("AudioBook Recorder :: " + name); // This should be in the load routine!!!!

            loadEffects();

            File cf = new File(location, "coverart.png");
            if (!cf.exists()) {
                cf = new File(location, "coverart.jpg");
                if (!cf.exists()) {
                    cf = new File(location, "coverart.gif");
                }
            }

            if (cf.exists()) {
                ImageIcon i = new ImageIcon(cf.getAbsolutePath());
                Image ri = Utils.getScaledImage(i.getImage(), 22, 22);
                setIcon(new ImageIcon(ri));
            } else {
                setIcon(Icons.book);
            }

            roomNoise = new Sentence("room-noise", "Room Noise");
            roomNoise.setParentBook(this);

            Element chapters = getNode(root, "chapters");
            NodeList chapterList = chapters.getElementsByTagName("chapter");
            for (int i = 0; i < chapterList.getLength(); i++) {
                Element chapterElement = (Element)chapterList.item(i);
                Chapter newChapter = new Chapter(chapterElement);
                newChapter.setParentBook(this);
                add(newChapter);
            }
    
            AudiobookRecorder.window.updateEffectChains(effects);
        }
    }

    public void loadBookXML(Element root, DefaultTreeModel model) {
        Debug.trace();
        name = getTextNode(root, "title");
        author = getTextNode(root, "author");
        genre = getTextNode(root, "genre");
        comment = getTextNode(root, "comment");
        ACX = getTextNode(root, "acx");
        manuscript = getTextNode(root, "manuscript");
        notes = getTextNode(root, "notes");

        Element settings = getNode(root, "settings");
        Element audioSettings = getNode(settings, "audio");
        Element effectSettings = getNode(settings, "effects");

        defaultEffect = getTextNode(settings, "default");

        AudiobookRecorder.window.setTitle("AudioBook Recorder :: " + name); // This should be in the load routine!!!!

        Element chapters = getNode(root, "chapters");

        NodeList chapterList = chapters.getElementsByTagName("chapter");

        roomNoise = new Sentence("room-noise", "Room Noise");
        roomNoise.setParentBook(this);

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

    public Chapter getChapterByName(String name) {
        Debug.trace();
        for (Enumeration o = children(); o.hasMoreElements();) {
            Object ob = (Object)o.nextElement();
            if (ob instanceof Chapter) {
                Chapter c = (Chapter)ob;
                if (c.getName().equals(name)) {
                    return c;
                }
            }
        }
        return null;
    }

    public Chapter getLastChapter() {
        Debug.trace();
        DefaultMutableTreeNode leaf = getLastLeaf();
        if (leaf instanceof Sentence) {
            Sentence s = (Sentence)leaf;
            return (Chapter)s.getParent();
        }
        if (leaf instanceof Chapter) {
            return (Chapter)getLastLeaf();
        } 
        return null;
    }

    public Chapter getChapter(int n) {
        Debug.trace();
        if (n == 0) return null;
        return (Chapter)getChildAt(n);
    }

    public Chapter addChapter() {
        Debug.trace();
        String uuid = UUID.randomUUID().toString();
        Chapter c = new Chapter(uuid, uuid);
        add(c);
        c.setParentBook(this);
        return c;
    }

    public Chapter addChapter(String name) {
        Debug.trace();
        String uuid = UUID.randomUUID().toString();
        Chapter c = new Chapter(uuid, name);
        add(c);
        c.setParentBook(this);
        return c;
    }

    public Chapter addChapter(String id, String name) {
        Debug.trace();
        Chapter c = new Chapter(id, name);
        add(c);
        c.setParentBook(this);
        return c;
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

    public void renameBook(String newName) {
        Debug.trace();
        File oldDir = location;
        File newDir = new File(Options.get("path.storage"), newName);

        if (newDir.exists()) {
            JOptionPane.showMessageDialog(AudiobookRecorder.window, "Book already exists", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (oldDir.exists() && oldDir.isDirectory()) {
            oldDir.renameTo(newDir);
            name = newName;
            try {
                save();
            } catch (Exception e) {
                e.printStackTrace();
            }
            reloadTree();
            Options.set("path.last-book", name);
            Options.savePreferences();
            AudiobookRecorder.window.setTitle("AudioBook Recorder :: " + name);
        }
    }

    public String toString() {
        Debug.trace();
        return name;
    }

    public AudioFormat getAudioFormat() {
        Debug.trace();
        if (cachedFormat != null) {
            return cachedFormat;
        }
        cachedFormat = roomNoise.getAudioFormat();
        return cachedFormat;
    }

    public File getBookFolder() {
        return location;
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
        root.appendChild(makeTextNode(doc, "notes", notes));

        Element settingsNode = doc.createElement("settings");
        root.appendChild(settingsNode);
        
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
        File dst = new File(location, manuscript);

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
        File f = new File(location, manuscript);
        if (f.exists()) { 
            return f;
        }
        return null;
    }

    public void onSelect(BookTreeNode target) {
        Debug.trace();
        AudiobookRecorder.setSelectedBook(this);
        if (target == this) {
            AudiobookRecorder.setSelectedChapter(null);
            AudiobookRecorder.setSelectedSentence(null);
        }
        AudiobookRecorder.window.setBookNotes(notes);
        AudiobookRecorder.window.noiseFloorLabel.setNoiseFloor(getNoiseFloorDB());
//        AudiobookRecorder.window.updateEffectChains(effects);
        TreeNode p = getParent();
        if (p instanceof BookTreeNode) {
            BookTreeNode btn = (BookTreeNode)p;
            btn.onSelect(target);
        }
    }

    public String getNotes() {
        Debug.trace();
        return notes;
    }

    public void setNotes(String n) {
        Debug.trace();
        notes = n;
    }

    public File getLocation() { 
        Debug.trace();
        return location;
    }

    public void setLocation(File l) {
        Debug.trace();
        location = l;
    }

    public void reloadTree() {
        Debug.trace();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Debug.trace();
                if (AudiobookRecorder.window == null) return;
                if (AudiobookRecorder.window.bookTreeModel == null) return;
                try {
                    AudiobookRecorder.window.bookTreeModel.reload(Book.this);
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    public Book getBook() {
        Debug.trace();
        return this;
    }

    public byte[] getRoomNoise(int ms) {
        Debug.trace();

        if (roomNoise == null) return null;

//        roomNoise.setEffectChain(getDefaultEffect());
        int len = roomNoise.getSampleSize();
        if (len == 0) return null;

        AudioFormat f = roomNoise.getAudioFormat();

        float sr = f.getSampleRate();

        int samples = (int)(ms * (sr / 1000f));

        int start = rng.nextInt(len - samples);
        int end = start + samples;

        roomNoise.setStartOffset(start);
        roomNoise.setEndOffset(end);

        byte[] data = roomNoise.getPCMData();

        return data;
    }

    public double getNoiseFloor() {
        Debug.trace();
        if (roomNoise == null) return 0;
        return roomNoise.getPeak();
    }

    public int getNoiseFloorDB() {
        Debug.trace();
        if (roomNoise == null) return 0;
        return roomNoise.getPeakDB();
    }

    public Sentence getRoomNoiseSentence() {
        Debug.trace();
        return roomNoise;
    }

    public void recordRoomNoise() {
        Debug.trace();
        if (roomNoise.startRecording()) {

            java.util.Timer ticker = new java.util.Timer(true);
            ticker.schedule(new TimerTask() {
                public void run() {
                    Debug.trace();
                    roomNoise.stopRecording();
                }
            }, 5000); // 5 seconds of recording
        }
    }

    public void loadEffects() {
        Debug.trace();
        effects = new TreeMap<String,EffectGroup>();
        loadEffectsFromFolder(new File(Options.get("path.storage"), "System"));
        if (location != null) {
            loadEffectsFromFolder(location);
        }
    }

    public void loadEffectsFromFolder(File dir) {
        Debug.trace();
        if (dir == null) return;
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.getName().endsWith(".eff")) {
                EffectGroup g = loadEffect(f);
                if (g != null) {
                    String fn = f.getName().replace(".eff","");
                    effects.put(fn, g);
                }
            }
        }
    }

    public EffectGroup loadEffect(File xml) {
        Debug.trace();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xml);

            Element root = document.getDocumentElement();
            if (root.getTagName().equals("effect")) {
                EffectGroup g = EffectGroup.loadEffectGroup(root);
                return g;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void save() throws ParserConfigurationException, TransformerConfigurationException, TransformerException {
        Debug.trace();
        if (location == null) {
            location = new File(Options.get("path.storage"), getName());
        }

        if (!location.exists()) {
            location.mkdirs();
        }

        File xml = new File(location, "audiobook.abx");
        Document doc = buildDocument();


        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(xml);
        transformer.transform(source, result);
    }

    public File getBookFile() {
        return new File(location, "audiobook.abx");
    }

    final static int[] illegalChars = {34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};

    static {
        Arrays.sort(illegalChars);
    }

    public static String sanitize(String badFileName) {
        StringBuilder cleanName = new StringBuilder();
        int len = badFileName.codePointCount(0, badFileName.length());
        for (int i=0; i<len; i++) {
            int c = badFileName.codePointAt(i);
            if (Arrays.binarySearch(illegalChars, c) < 0) {
                cleanName.appendCodePoint(c);
            }
        }
        return cleanName.toString();
    }

    @Override
    public double getLength() {
        Debug.trace();
        double len = 0;
        for (Enumeration o = children(); o.hasMoreElements();) {
            Object ob = (Object)o.nextElement();
            if (ob instanceof Chapter) {
                Chapter c = (Chapter)ob;
                len += c.getLength();
            }
        }
        return len;
    }

}
