package uk.co.majenko.audiobookrecorder;

import javax.swing.JPanel;
import javax.swing.table.TableCellRenderer;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import javax.swing.JScrollPane;
import java.io.File;
import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.ListSelectionModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JOptionPane;

public class OpenBookPanel extends JPanel {

    JScrollPane scroll;

    JTable table;

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

            if (dir.exists() && dir.isDirectory()) {
                for (File b : dir.listFiles()) {
                    if (b == null) continue;
                    if (!b.isDirectory()) continue;
                    File xml = new File(b, "audiobook.abx");
                    if (xml.exists()) {
                        BookPanel book = new BookPanel(b);
                        model.addBook(book);
                    } else {
                        xml = new File(b, "audiobook.abk");
                        if (xml.exists()) {
                            BookPanel book = new BookPanel(b);
                            model.addBook(book);
                        }
                    }
                }
            }


            table = new JTable(model);
            table.setDefaultRenderer(BookPanel.class, new BookCellRenderer());
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setRowHeight(80);
            table.getTableHeader().setUI(null);

            table.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                        Component c = (Component)OpenBookPanel.this;
                        while ((c != null) && (!(c instanceof JOptionPane))) {
                            c = c.getParent();
                        }
                        if (c == null) {
                            Debug.d("Could not get option pane!");
                        } else {
                            JOptionPane op = (JOptionPane)c;
                            op.setValue(JOptionPane.OK_OPTION);
                        }
                    }
                }
            });




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
