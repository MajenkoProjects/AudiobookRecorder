package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.border.*;
import java.util.*;
import java.io.*;

public class BookPanel extends JPanel {
    String name;
    String author;
    String genre;
    String comment;
    ImageIcon cover;
    Image resizedCover;

    JLabel iconLabel;
    JLabel titleLabel;
    JLabel authorLabel;
    JLabel otherLabel;

    JPanel panel;

    File root;

    boolean highlight = false;

    public BookPanel(File r) {
        try {
            root = r;
            Properties props = new Properties();
            props.loadFromXML(new FileInputStream(new File(root, "audiobook.abk")));
            name = props.getProperty("book.name");
            author = props.getProperty("book.author");
            genre = props.getProperty("book.genre");
            comment = props.getProperty("book.comment");
            File icon = new File(root, "coverart.png");
            if (!icon.exists()) {
                icon = new File(root, "coverart.jpg");
            }
            if (!icon.exists()) {
                icon = new File(root, "coverart.gif");
            }
            if (icon.exists()) {
                cover = new ImageIcon(icon.getAbsolutePath());
                resizedCover = Utils.getScaledImage(cover.getImage(), 75, 75);
                iconLabel = new JLabel(new ImageIcon(resizedCover));
            } else {
                cover = null;
                resizedCover = null;
                iconLabel = new JLabel("");
            }

            iconLabel.setSize(new Dimension(75, 75));
            iconLabel.setPreferredSize(new Dimension(75, 75));

            titleLabel = new JLabel(name);
            authorLabel = new JLabel(author);
            otherLabel = new JLabel(genre + " :: " + comment);

            authorLabel.setForeground(new Color(0x80, 0x80, 0x80));
            otherLabel.setForeground(new Color(0x80, 0x80, 0x80));

            setLayout(new BorderLayout());

            panel = new JPanel();

            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            panel.setBorder(new EmptyBorder(10, 10, 10, 10));

            panel.add(titleLabel);
            panel.add(authorLabel);
            panel.add(otherLabel);

            add(iconLabel, BorderLayout.WEST);
            add(panel, BorderLayout.CENTER);
            panel.setBackground(new Color(0x20, 0x20, 0x20));
            panel.setOpaque(true);
            setBackground(new Color(0x20, 0x20, 0x20));
            setOpaque(true);
        } catch (Exception e) {
        }
    }

    public File getConfigFile() {
        return new File(root, "audiobook.abk");
    }

    public void highlight() {
        setBackground(new Color(0x00, 0x20, 0x40));
        panel.setBackground(new Color(0x00, 0x20, 0x40));
    }

    public void lowlight() {
        setBackground(new Color(0x20, 0x20, 0x20));
        panel.setBackground(new Color(0x20, 0x20, 0x20));
    }
}
