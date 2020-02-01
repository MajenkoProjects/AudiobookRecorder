package uk.co.majenko.audiobookrecorder;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.border.EmptyBorder;
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
    File configFile;

    boolean highlight = false;

    public BookPanel(Properties p, ImageIcon i) {
        loadBookData(p, i);
    }

    public BookPanel(File r) {
        try {
            root = r;
            Properties props = new Properties();

            configFile = new File(root, "audiobook.abx");

            if (configFile.exists()) {
                loadXMLData(configFile);
            } else {
                configFile = new File(root, "audiobook.abk");
                props.loadFromXML(new FileInputStream(configFile));
                loadBookData(props, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BookPanel(String n, String a, String g, String c, ImageIcon i) {
        name = n;
        author = a;
        genre = g;
        comment = c;
        cover = i;
        if (i != null) {
            cover = i;
            resizedCover = Utils.getScaledImage(cover.getImage(), 75, 75);
            iconLabel = new JLabel(new ImageIcon(resizedCover));
        } else {
            cover = null;
            resizedCover = null;
            iconLabel = new JLabel("");
        }
        populate();
    }

    public void loadXMLData(File inputFile) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            Element rootnode = doc.getDocumentElement();

            name = Book.getTextNode(rootnode, "title");
            author = Book.getTextNode(rootnode, "author");
            genre = Book.getTextNode(rootnode, "genre");
            comment = Book.getTextNode(rootnode, "comment");

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

            populate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void loadBookData(Properties props, ImageIcon i) {
        try {
            name = props.getProperty("book.name");
            author = props.getProperty("book.author");
            genre = props.getProperty("book.genre");
            comment = props.getProperty("book.comment");
            if (i == null) {
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
            } else {
                cover = i;
                resizedCover = Utils.getScaledImage(cover.getImage(), 75, 75);
                iconLabel = new JLabel(new ImageIcon(resizedCover));
            }
            populate();
        } catch (Exception e) {
        }
    }

    void populate() {
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
    }

    public File getConfigFile() {
        return configFile;
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
