package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;

public class BookInfoPanel extends JPanel {

    JTextField title;
    JTextField author;
    JTextField genre;
    JTextField comment;
    JTextField acx;

    public BookInfoPanel(String t, String a, String g, String c, String x) {
        super();
        Debug.trace();
        setLayout(new GridBagLayout());
        GridBagConstraints con = new GridBagConstraints();

        con.gridx = 0;
        con.gridy = 0;
        
        add(new JLabel("Title:"), con);
        con.gridx = 1;
        title = new JTextField(t);
        title.setPreferredSize(new Dimension(200, 20));
        add(title, con);

        con.gridx = 0;
        con.gridy++;

        add(new JLabel("Author:"), con);
        con.gridx = 1;
        author = new JTextField(a);
        author.setPreferredSize(new Dimension(200, 20));
        add(author, con);

        con.gridx = 0;
        con.gridy++;

        add(new JLabel("Genre:"), con);
        con.gridx = 1;
        genre = new JTextField(g);
        genre.setPreferredSize(new Dimension(200, 20));
        add(genre, con);

        con.gridx = 0;
        con.gridy++;

        add(new JLabel("Comment:"), con);
        con.gridx = 1;
        comment = new JTextField(c);
        comment.setPreferredSize(new Dimension(200, 20));
        add(comment, con);

        con.gridx = 0;
        con.gridy++;

        add(new JLabel("AXC Code:"), con);
        con.gridx = 1;
        acx = new JTextField(x);
        acx.setPreferredSize(new Dimension(200, 20));
        add(acx, con);

        con.gridx = 0;
        con.gridy++;

    }

    public String getTitle() { Debug.trace(); return title.getText(); }
    public String getAuthor() { Debug.trace(); return author.getText(); }
    public String getGenre() { Debug.trace(); return genre.getText(); }
    public String getComment() { Debug.trace(); return comment.getText(); }

    public String getACX() { 
        Debug.trace();
        Pattern p = Pattern.compile("\\/titleview\\/([A-Z0-9]{14})");
        Matcher m = p.matcher(acx.getText());
        if (m.find()) {
            System.err.println(m);
            return m.group(1);
        }
        return acx.getText();
    }

    public void setTitle(String t) { Debug.trace(); title.setText(t); }
    public void setAuthor(String a) { Debug.trace(); author.setText(a); }
    public void setGenre(String g) { Debug.trace(); genre.setText(g); }
    public void setComment(String c) { Debug.trace(); comment.setText(c); }
    public void setACX(String a) { Debug.trace(); acx.setText(a); }

}
