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

    public class BookCellRenderer implements TableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null) return null;
            BookPanel p = (BookPanel)value;

            if (isSelected) {
                p.highlight();
            } else {
                p.lowlight();
            }

            return p;
        }
    };

    class BookTableModel extends AbstractTableModel {

        ArrayList<BookPanel> books;

        public BookTableModel() {
            super();
            books = new ArrayList<BookPanel>();
        }

        public int getRowCount() {
            return books.size();
        }

        public int getColumnCount() {
            return 1;
        }
    
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public void addBook(BookPanel b) {
            books.add(b);
        }

        public Object getValueAt(int r, int c) {
            return books.get(r);
        }

        public String getColumnName(int i) {
            return "Book";
        }

        public Class getColumnClass(int i) {
            return BookPanel.class;
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
                    BookPanel book = new BookPanel(b);
                    model.addBook(book);
                }
            }


            table = new JTable(model);
            table.setDefaultRenderer(BookPanel.class, new BookCellRenderer());
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setRowHeight(80);
            table.getTableHeader().setUI(null);
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

        BookPanel b = (BookPanel)table.getValueAt(sel, 0);
        return b.getConfigFile();
    }
}
