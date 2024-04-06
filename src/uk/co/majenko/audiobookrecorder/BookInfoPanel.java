package uk.co.majenko.audiobookrecorder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JComboBox;

public class BookInfoPanel extends JPanel {

    JTextField title;
	JTextField shortTitle;
    JTextField author;
	JTextField shortAuthor;
    JTextField genre;
    JTextField comment;
    JTextField acx;
    JTextField isbn;
	JComboBox<ExportProfile> exportProfile;

    public BookInfoPanel(String t, String st, String a, String sa, String g, String c, String x, String i, String epc) {
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

        add(new JLabel("Short Title:"), con);
        con.gridx = 1;
        shortTitle = new JTextField(st);
        shortTitle.setPreferredSize(new Dimension(200, 20));
        add(shortTitle, con);

        con.gridx = 0;
        con.gridy++;

        add(new JLabel("Author:"), con);
        con.gridx = 1;
        author = new JTextField(a);
        author.setPreferredSize(new Dimension(200, 20));
        add(author, con);

        con.gridx = 0;
        con.gridy++;

        add(new JLabel("Short Author:"), con);
        con.gridx = 1;
        shortAuthor = new JTextField(sa);
        shortAuthor.setPreferredSize(new Dimension(200, 20));
        add(shortAuthor, con);

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

        add(new JLabel("ACX Code:"), con);
        con.gridx = 1;
        acx = new JTextField(x);
        acx.setPreferredSize(new Dimension(200, 20));
        add(acx, con);

        con.gridx = 0;
        con.gridy++;

        add(new JLabel("ISBN:"), con);
        con.gridx = 1;
        isbn = new JTextField(i);
        isbn.setPreferredSize(new Dimension(200, 20));
        add(isbn, con);

        con.gridx = 0;
        con.gridy++;

        add(new JLabel("Export Profile:"), con);
        con.gridx = 1;
		exportProfile = new JComboBox<ExportProfile>();
		for (ExportProfile profile : AudiobookRecorder.exportProfiles.values()) {
			exportProfile.addItem(profile);
			if (profile.getCode() == epc) {
				exportProfile.setSelectedItem(profile);
			}
		}
        add(exportProfile, con);

        con.gridx = 0;
        con.gridy++;

    }

    public String getTitle() { Debug.trace(); return title.getText(); }
    public String getAuthor() { Debug.trace(); return author.getText(); }
    public String getGenre() { Debug.trace(); return genre.getText(); }
    public String getComment() { Debug.trace(); return comment.getText(); }
    public String getISBN() { Debug.trace(); return isbn.getText(); }
    public String getACX() { 
        Debug.trace();
        Pattern p = Pattern.compile("\\/titleview\\/([A-Z0-9]{14})");
        Matcher m = p.matcher(acx.getText());
        if (m.find()) {
            return m.group(1);
        }
        return acx.getText();
    }
	public String getShortTitle() { Debug.trace(); return shortTitle.getText(); }
	public String getShortAuthor() { Debug.trace(); return shortAuthor.getText(); }
	public ExportProfile getExportProfile() { Debug.trace(); return (ExportProfile)exportProfile.getSelectedItem(); }

    public void setTitle(String t) { Debug.trace(); title.setText(t); }
    public void setAuthor(String a) { Debug.trace(); author.setText(a); }
    public void setGenre(String g) { Debug.trace(); genre.setText(g); }
    public void setComment(String c) { Debug.trace(); comment.setText(c); }
    public void setACX(String a) { Debug.trace(); acx.setText(a); }
    public void setISBN(String i) { Debug.trace(); isbn.setText(i); }
	public void setShortTitle(String t) { Debug.trace(); shortTitle.setText(t); }
	public void setShortAuthor(String a) { Debug.trace(); shortAuthor.setText(a); }
	public void setExportProfile(ExportProfile p) { Debug.trace(); exportProfile.setSelectedItem(p); }
}
