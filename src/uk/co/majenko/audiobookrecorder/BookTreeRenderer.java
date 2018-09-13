package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;

public class BookTreeRenderer extends DefaultTreeCellRenderer {

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        JLabel ret = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if (value instanceof Sentence) {
            Sentence s = (Sentence)value;
            if (s.isLocked()) {
                ret.setForeground(new Color(0x20, 0x00, 0x00));
                ret.setIcon(Icons.locked);
            } else {
                ret.setIcon(Icons.sentence);
            }
        } else if (value instanceof Chapter) {
            ret.setIcon(Icons.chapter);
        } else if (value instanceof Book) {
            ret.setIcon(((Book)value).getIcon());
        }
        return ret;
    }
}
