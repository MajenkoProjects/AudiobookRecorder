package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import javax.swing.tree.*;
import it.sauronsoftware.jave.*;
import com.mpatric.mp3agic.*;
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class Chapter extends BookTreeNode {
    
    String name;
    String id;

    int preGap;
    int postGap;

    String notes;

    public Chapter(String i, String chaptername) {
        super(chaptername);

        id = i;
        name = chaptername;
        preGap = Options.getInteger("catenation.pre-chapter");
        postGap = Options.getInteger("catenation.post-chapter");
    }

    public Chapter(Element root, DefaultTreeModel model) {

        name = Book.getTextNode(root, "name");
        id = root.getAttribute("id");
        preGap = Utils.s2i(Book.getTextNode(root, "pre-gap"));
        postGap = Utils.s2i(Book.getTextNode(root, "post-gap"));

        notes = Book.getTextNode(root, "notes");
    
        Element sentencesNode = Book.getNode(root, "sentences");
        NodeList sentences = sentencesNode.getElementsByTagName("sentence");

        for (int i = 0; i < sentences.getLength(); i++) {
            Element sentenceElement = (Element)sentences.item(i);
            Sentence newSentence = new Sentence(sentenceElement);
            model.insertNodeInto(newSentence, this, getChildCount());
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String i) {
        id = i;
    }

    public Sentence getLastSentence() {
        DefaultMutableTreeNode ls = getLastLeaf();
        if (ls instanceof Sentence) return (Sentence)ls;
        return null;
    }

    public String toString() {
        return name;
    }

    public void setUserObject(Object o) {
        if (o instanceof String) {
            String so = (String)o;
            name = so;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        name = n;
    }

    public void setPreGap(int g) {
        preGap = g;
    }

    public int getPreGap() {
        return preGap;
    }

    public void setPostGap(int g) {
        postGap = g;
    }

    public int getPostGap() {
        return postGap;
    }

    @SuppressWarnings("unchecked")
    public void exportChapter(ProgressDialog exportDialog) throws 
                    FileNotFoundException, IOException, InputFormatException, NotSupportedException,
                    EncoderException, UnsupportedTagException, InvalidDataException {

        if (getChildCount() == 0) return;

        Book book = AudiobookRecorder.window.book;

        File bookRoot = new File(Options.get("path.storage"), book.getName());
        if (!bookRoot.exists()) {
            bookRoot.mkdirs();
        }

        File export = new File(bookRoot, "export");
        if (!export.exists()) {
            export.mkdirs();
        }
        Encoder encoder;

        String ffloc = Options.get("path.ffmpeg");
        if (ffloc != null && !ffloc.equals("")) {
            encoder = new Encoder(new FFMPEGLocator() {
                public String getFFMPEGExecutablePath() {
                    return Options.get("path.ffmpeg");
                }
            });
        } else {
            encoder = new Encoder();
        }
        EncodingAttributes attributes = new EncodingAttributes();

        AudioAttributes audioAttributes = new AudioAttributes();
        audioAttributes.setCodec("libmp3lame");
        audioAttributes.setBitRate(Options.getInteger("audio.export.bitrate"));
        audioAttributes.setSamplingRate(Options.getInteger("audio.export.samplerate"));
        audioAttributes.setChannels(Options.getInteger("audio.export.channels")); //new Integer(2));
        attributes.setFormat("mp3");
        attributes.setAudioAttributes(audioAttributes);


        AudioFormat sampleformat = AudiobookRecorder.window.roomNoise.getAudioFormat();
        AudioFormat format = new AudioFormat(sampleformat.getSampleRate(), 16, 2, true, false);
        byte[] data;

        int fullLength = 0;

        int kids = getChildCount();
        
        String name = getName();
        if (exportDialog != null) exportDialog.setMessage("Exporting " + name);
        if (exportDialog != null) exportDialog.setProgress(0);

        File exportFile = new File(export, name + ".wax");
        File wavFile = new File(export, name + ".wav");
        File mp3File = new File(export, name + "-untagged.mp3");
        File taggedFile = new File(export, book.getName() + " - " + name + ".mp3");

        FileOutputStream fos = new FileOutputStream(exportFile);
        data = AudiobookRecorder.window.getRoomNoise(Utils.s2i(Options.get("catenation.pre-chapter")));
        fullLength += data.length;
        fos.write(data);

        int kidno = 0;


        for (Enumeration s = children(); s.hasMoreElements();) {
            kidno++;
            if (exportDialog != null) exportDialog.setProgress(kidno * 1000 / kids);
            Sentence snt = (Sentence)s.nextElement();
            data = snt.getPCMData();

            fullLength += data.length;
            fos.write(data);

            if (s.hasMoreElements()) {
                data = AudiobookRecorder.window.getRoomNoise(snt.getPostGap());
            } else {
                data = AudiobookRecorder.window.getRoomNoise(Utils.s2i(Options.get("catenation.post-chapter")));
            }
            fullLength += data.length;
            fos.write(data);
        }
        fos.close();
        FileInputStream fis = new FileInputStream(exportFile);
        AudioInputStream ais = new AudioInputStream(fis, format, fullLength);
        fos = new FileOutputStream(wavFile);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fos);
        fos.flush();
        fos.close();
        fis.close();
        exportFile.delete();



        if (exportDialog != null) exportDialog.setMessage("Converting " + name);


        if (exportDialog != null) {
            encoder.encode(wavFile, mp3File, attributes, exportDialog);
        } else {
            encoder.encode(wavFile, mp3File, attributes);
        }

        Mp3File id3 = new Mp3File(mp3File);

        ID3v2 tags = new ID3v24Tag();
        id3.setId3v2Tag(tags);

        tags.setTrack(Integer.toString(Utils.s2i(getId()) - 0));
        tags.setTitle(name);
        tags.setAlbum(book.getName());
        tags.setArtist(book.getAuthor());

        tags.setComment(book.getComment());

        id3.save(taggedFile.getAbsolutePath());
        mp3File.delete();
        wavFile.delete();
    }

    public double getChapterLength() {
        double totalTime = Options.getInteger("audio.recording.pre-chapter") / 1000d;
        for (Enumeration s = children(); s.hasMoreElements();) {
            Sentence sentence = (Sentence)s.nextElement();
            totalTime += sentence.getLength();
            if (s.hasMoreElements()) {
                totalTime += (sentence.getPostGap() / 1000d);
            } else {
                totalTime += Options.getInteger("audio.recording.post-chapter") / 1000d;
            }
        }
        return totalTime;
    }

    public ArrayList<String> getUsedEffects() {

        ArrayList<String> out = new ArrayList<String>();

        for (Enumeration o = children(); o.hasMoreElements();) {
            Object ob = (Object)o.nextElement();
            if (ob instanceof Sentence) {
                Sentence s = (Sentence)ob;
                String ec = s.getEffectChain();
                if (out.indexOf(ec) == -1) {
                    out.add(ec);
                }
            }
        }
        return out;
    }

    public void resetPostGaps() {
        for (Enumeration s = children(); s.hasMoreElements();) {
            Sentence snt = (Sentence)s.nextElement();
            snt.resetPostGap();
        }
    }

    public void purgeBackups() {
        for (Enumeration o = children(); o.hasMoreElements();) {
            Object ob = (Object)o.nextElement();
            if (ob instanceof Sentence) {
                Sentence s = (Sentence)ob;
                s.purgeBackups();
            }
        }
    }

    public Element getChapterXML(Document doc) {
        Element chapterNode = doc.createElement("chapter");
        chapterNode.setAttribute("id", id);
        chapterNode.appendChild(Book.makeTextNode(doc, "name", name));
        chapterNode.appendChild(Book.makeTextNode(doc, "pre-gap", preGap));
        chapterNode.appendChild(Book.makeTextNode(doc, "post-gap", postGap));
        chapterNode.appendChild(Book.makeTextNode(doc, "notes", notes));

        Element sentencesNode = doc.createElement("sentences");
        chapterNode.appendChild(sentencesNode);

        for (Enumeration o = children(); o.hasMoreElements();) {
            Object ob = (Object)o.nextElement();
            if (ob instanceof Sentence) {
                Sentence s = (Sentence)ob;
                sentencesNode.appendChild(s.getSentenceXML(doc));
            }
        }

        return chapterNode;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String t) {
        notes = t;
    }

    public void onSelect() {
        AudiobookRecorder.window.setChapterNotes(notes);
    }

    public double getLength() {
        double len = 0;
        for (Enumeration o = children(); o.hasMoreElements();) {
            Object ob = (Object)o.nextElement();
            if (ob instanceof Sentence) {
                Sentence s = (Sentence)ob;
                len += s.getLength();
            }
        }
        return len;
    }

}
