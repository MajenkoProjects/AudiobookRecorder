package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import javax.swing.tree.*;

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
}
