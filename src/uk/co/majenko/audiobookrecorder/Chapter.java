package uk.co.majenko.audiobookrecorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import it.sauronsoftware.jave.FFMPEGLocator;
import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.EncodingAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.InputFormatException;

import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;

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
    Book parentBook = null;

    public Chapter(String i, String chaptername) {
        super(chaptername);
        Debug.trace();
        id = i;
        name = chaptername;
        preGap = Options.getInteger("catenation.pre-chapter");
        postGap = Options.getInteger("catenation.post-chapter");
    }

    public Chapter(Element root, DefaultTreeModel model) {
        Debug.trace();
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

    public Chapter(Element root) {
        Debug.trace();
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
            add(newSentence);
        }
    }

    public int getSequenceNumber() {
        Book book = getBook();
        int i = 1;
        while (true) {
            Chapter c = book.getChapter(i);
            if (c == null) {
                return -1;
            }
            System.err.println(c.getName());
            if (c.getName().equals(name)) {
                return i;
            }
            i++;
        }
    }

    public String getId() {
        Debug.trace();
        return id;
    }

    public void setId(String i) {
        Debug.trace();
        id = i;
    }

    public Sentence getLastSentence() {
        Debug.trace();
        DefaultMutableTreeNode ls = getLastLeaf();
        if (ls instanceof Sentence) return (Sentence)ls;
        return null;
    }

    public String toString() {
        Debug.trace();
        return name;
    }

    public void setUserObject(Object o) {
        Debug.trace();
        if (o instanceof String) {
            String so = (String)o;
            name = so;
        }
    }

    public String getName() {
        Debug.trace();
        return name;
    }

    public void setName(String n) {
        Debug.trace();
        name = n;
    }

    public void setPreGap(int g) {
        Debug.trace();
        preGap = g;
    }

    public int getPreGap() {
        Debug.trace();
        return preGap;
    }

    public void setPostGap(int g) {
        Debug.trace();
        postGap = g;
    }

    public int getPostGap() {
        Debug.trace();
        return postGap;
    }

    public String createFilename(String format) {
        String out = "";

        char[] chars = format.toCharArray();

        int mode = 0; // nothing
        int len = 0;
        boolean zeros = false;
        boolean first = true;

        Book book = getBook();

        for (char c : chars) {
            switch (mode) {
                case 0:
                    switch (c) {
                        case '%': {
                            mode = 1; 
                            len = 0;
                            zeros = false;
                            first = true;
                        }
                        break;
                        default: out += c; break;
                    }
                    break;

                case 1:
                    switch (c) {
                        case '0': len = len * 10; first = false; break;
                        case '1': len = len * 10 + 1; first = false; break;
                        case '2': len = len * 10 + 2; first = false; break;
                        case '3': len = len * 10 + 3; first = false; break;
                        case '4': len = len * 10 + 4; first = false; break;
                        case '5': len = len * 10 + 5; first = false; break;
                        case '6': len = len * 10 + 6; first = false; break;
                        case '7': len = len * 10 + 7; first = false; break;
                        case '8': len = len * 10 + 8; first = false; break;
                        case '9': len = len * 10 + 9; first = false; break;
                        case 't': 
                            if (len > 0) 
                                out += String.format("%" + len + "s", book.getTitle());
                            else
                                out += book.getTitle();
                            mode = 0;
                            break;
                        case 'n': 
                            if (len > 0)
                                out += String.format("%" + len + "s", name);
                            else
                                out += name;
                            mode = 0;
                            break;

                        case '%':
                            out += '%';
                            mode = 0;
                            break;
                        case 'I':
                            if (len > 0)
                                out += String.format("%" + len + "s", book.getISBN());
                            else
                                out += book.getISBN();
                            mode = 0;
                            break;
                        case 'A':
                            if (len > 0)
                                out += String.format("%" + len + "s", book.getACX());
                            else
                                out += book.getACX();
                            mode = 0;
                            break;
                        case 'i':
                            if (len > 0)
                                out += String.format("%0" + len + "d", getSequenceNumber());
                            else
                                out += getId();
                            mode = 0;
                            break;
                    }
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public void exportChapter(ProgressDialog exportDialog, String fnformat) throws 
                    FileNotFoundException, IOException, InputFormatException, NotSupportedException,
                    EncoderException, UnsupportedTagException, InvalidDataException {
        Debug.trace();

        if (getChildCount() == 0) return;

        Book book = getBook();

        File export = book.getLocation("export");
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


        AudioFormat sampleformat = getBook().getRoomNoiseSentence().getAudioFormat();
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


        File taggedFile = new File(export, createFilename(fnformat) + ".mp3");

        FileOutputStream fos = new FileOutputStream(exportFile);
        data = getBook().getRoomNoise(Utils.s2i(Options.get("catenation.pre-chapter")));
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
                data = getBook().getRoomNoise(snt.getPostGap());
            } else {
                data = getBook().getRoomNoise(Utils.s2i(Options.get("catenation.post-chapter")));
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
        Debug.trace();
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
        Debug.trace();

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
        Debug.trace();
        for (Enumeration s = children(); s.hasMoreElements();) {
            Sentence snt = (Sentence)s.nextElement();
            snt.resetPostGap();
        }
    }

    public void purgeBackups() {
        Debug.trace();
        for (Enumeration o = children(); o.hasMoreElements();) {
            Object ob = (Object)o.nextElement();
            if (ob instanceof Sentence) {
                Sentence s = (Sentence)ob;
                s.purgeBackups();
            }
        }
    }

    public Element getChapterXML(Document doc) {
        Debug.trace();
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
        Debug.trace();
        return notes;
    }

    public void setNotes(String t) {
        Debug.trace();
        notes = t;
    }

    public void onSelect(BookTreeNode target) {
        Debug.trace();
        AudiobookRecorder.setSelectedChapter(this);
        if (target == this) {
            AudiobookRecorder.setSelectedSentence(null);
        }
        AudiobookRecorder.window.setChapterNotes(notes);
        TreeNode p = getParent();
        if (p instanceof BookTreeNode) {
            BookTreeNode btn = (BookTreeNode)p;
            btn.onSelect(target);
        }
    }

    @Override
    public double getLength() {
        Debug.trace();
        double len = 0;
        for (Enumeration o = children(); o.hasMoreElements();) {
            Object ob = (Object)o.nextElement();
            if (ob instanceof Sentence) {
                Sentence s = (Sentence)ob;
                len += s.getLength();
                len += (s.getPostGap() / 1000d);
            }
        }

        if (len > 0) {
            len += (getPreGap() / 1000d);
            len += (getPostGap() / 1000d);
        }
        return len;
    }

    @Override
    public Book getBook() {
        if (parentBook != null) return parentBook;
        if (getParent() == null) return null;
        return (Book)getParent();
    }

    public void setParentBook(Book me) {
        parentBook = me;
        for (Enumeration o = children(); o.hasMoreElements();) {
            Object ob = (Object)o.nextElement();
            if (ob instanceof Sentence) {
                Sentence s = (Sentence)ob;
                s.setParentBook(me);
            }
        }
    }

    public double getRMS() {
        double rms = 0;
        int c = 0;
        for (Enumeration o = children(); o.hasMoreElements();) {
            Object ob = (Object)o.nextElement();
            if (ob instanceof Sentence) {
                Sentence s = (Sentence)ob;
                rms += s.getRMS();
                c++;
            }
        }

        return rms / c;
    }

}
