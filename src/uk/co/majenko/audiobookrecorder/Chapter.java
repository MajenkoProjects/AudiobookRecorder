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


public class Chapter extends DefaultMutableTreeNode {
    
    String name;
    String id;

    int preGap;
    int postGap;

    public Chapter(String i, String chaptername) {
        super(chaptername);

        id = i;
        name = chaptername;
        preGap = Options.getInteger("catenation.pre-chapter");
        postGap = Options.getInteger("catenation.post-chapter");

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
    public void exportChapter(ExportDialog exportDialog) throws 
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
        audioAttributes.setChannels(2); //new Integer(2));
        attributes.setFormat("mp3");
        attributes.setAudioAttributes(audioAttributes);


        AudioFormat format = AudiobookRecorder.window.roomNoise.getAudioFormat();
        byte[] data;

        int fullLength = 0;

        int kids = getChildCount();
        
        String name = getName();
        if (exportDialog != null) exportDialog.setMessage("Exporting " + name);
        if (exportDialog != null) exportDialog.setProgress(0);

        File exportFile = new File(export, name + ".wax");
        File wavFile = new File(export, name + ".wav");
        File mp3File = new File(export, name + "-untagged.mp3");
        File taggedFile = new File(export, name + ".mp3");

        FileOutputStream fos = new FileOutputStream(exportFile);
        data = AudiobookRecorder.window.getRoomNoise(Utils.s2i(Options.get("catenation.pre-chapter")));
        fullLength += data.length;
        fos.write(data);

        int kidno = 0;


        for (Enumeration s = children(); s.hasMoreElements();) {
            kidno++;
            if (exportDialog != null) exportDialog.setProgress(kidno * 1000 / kids);
            Sentence snt = (Sentence)s.nextElement();
            data = snt.getRawAudioData();

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

}
