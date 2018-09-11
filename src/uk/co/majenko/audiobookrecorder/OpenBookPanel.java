package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class OpenBookPanel extends JPanel {

    JScrollPane scroll;

    JTable table;

    class BookInfo {
        public String name;
        public String author;
        public String genre;
        public String comment;
        
        public BookInfo(String n, String a, String g, String c) {
            name = n;
            author = a;
            genre = g;
            comment = c;
        }
    }

    class BookTableModel extends AbstractTableModel {

        ArrayList<BookInfo> books;

        public BookTableModel() {
            super();
            books = new ArrayList<BookInfo>();
        }

        public int getRowCount() {
            return books.size();
        }

        public int getColumnCount() {
            return 4;
        }
    
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public void addBook(BookInfo b) {
            books.add(b);
        }

        public Object getValueAt(int r, int c) {
            if (c > 3) return null;
            if (r > books.size()) return null;
            BookInfo b = books.get(r);
            switch (c) {
                case 0: return b.name;
                case 1: return b.author;
                case 2: return b.genre;
                case 4: return b.comment;
            }
            return null;
        }

        public String getColumnName(int i) {
            switch(i) {
                case 0: return "Name";
                case 1: return "Author";
                case 2: return "Genre";
                case 3: return "Comment";
            }
            return null;
        }
    }

    BookTableModel model;
    
    public OpenBookPanel() {
        super();

        model = new BookTableModel();

        setLayout(new BorderLayout());

        scroll = new JScrollPane();
        add(scroll, BorderLayout.CENTER);


        try {
            File dir = new File(Options.get("path.storage"));

            for (File b : dir.listFiles()) {
                if (!b.isDirectory()) continue;
                File xml = new File(b, "audiobook.abk");
                if (xml.exists()) {
                    Properties props = new Properties();
                    props.loadFromXML(new FileInputStream(xml));

                    BookInfo book = new BookInfo(
                        props.getProperty("book.name"),
                        props.getProperty("book.author"),
                        props.getProperty("book.genre"),
                        props.getProperty("book.comment")
                    );

                    model.addBook(book);
                }
            }

            table = new JTable(model);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            scroll.setViewportView(table);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public File getSelectedFile() {
        int sel = table.getSelectedRow();
        if (sel == -1) {
            return null;
        }

        String name = (String)table.getValueAt(sel, 0);
        File d = new File(Options.get("path.storage"), name);
        File f = new File(d, "audiobook.abk");
        return f;
    }
}
