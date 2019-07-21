package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;

public class BookTreeRenderer extends DefaultTreeCellRenderer {

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        JLabel ret = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if (value instanceof Sentence) {
            Sentence s = (Sentence)value;

            OverlayIcon icn = new OverlayIcon(Icons.sentence);

            if (s.getOverrideText() != null) {
                ret.setText(s.getOverrideText());
            }

            if (s.getAttentionFlag()) {
                ret.setForeground(new Color(0xFF, 0xFF, 0x00));
                icn.add(Overlays.attention, OverlayIcon.TOP_LEFT);
            } 

            if (s.isLocked()) {
                ret.setForeground(new Color(0x00, 0x80, 0xFF));
                icn.add(Overlays.locked, OverlayIcon.BOTTOM_LEFT);
            } 

            if (s.getStartOffset() == 0) {
                icn.add(Overlays.important, OverlayIcon.TOP_RIGHT);
            } 

            if (s.getEffectChain() != null) {
                if (!s.getEffectChain().equals("none")) {
                    icn.add(Overlays.filter, OverlayIcon.BOTTOM_RIGHT);
                }
            }

            ret.setIcon(icn);

        } else if (value instanceof Chapter) {
            ret.setIcon(Icons.chapter);
        } else if (value instanceof Book) {
            ret.setIcon(((Book)value).getIcon());
        }
        return ret;
    }
}
