package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;

public class BookTreeRenderer extends DefaultTreeCellRenderer {

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        JLabel ret = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if (value instanceof Sentence) {
            Sentence s = (Sentence)value;

            if (s.getOverrideText() != null) {
                ret.setText(s.getOverrideText());
            }

            if (s.getAttentionFlag()) {
                ret.setForeground(new Color(0xFF, 0xFF, 0x00));
                ret.setIcon(Icons.attention);
            } else if (s.isLocked()) {
                ret.setForeground(new Color(0x00, 0x80, 0xFF));
                ret.setIcon(Icons.locked);
            } else if (s.getStartOffset() == 0) {
                ret.setIcon(Icons.important);
            } else {
                ret.setIcon(Icons.sentence);
            }

            if (s.isInSample()) {
                ret.setIcon(Icons.star);
            }

        } else if (value instanceof Chapter) {
            ret.setIcon(Icons.chapter);
        } else if (value instanceof Book) {
            ret.setIcon(((Book)value).getIcon());
        }
        return ret;
    }
}
