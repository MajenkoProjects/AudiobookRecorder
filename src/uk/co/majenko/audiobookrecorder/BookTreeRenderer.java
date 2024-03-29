package uk.co.majenko.audiobookrecorder;

import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.SwingConstants;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;

public class BookTreeRenderer extends DefaultTreeCellRenderer {

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        JLabel ret = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        ret.setIconTextGap(5);
        ret.setBorder(new EmptyBorder(0, 0, 0, 0));
        if (value instanceof Sentence) {
            Sentence s = (Sentence)value;

            JPanel p = new JPanel();
            p.setLayout(new GridBagLayout());
            GridBagConstraints ctx = new GridBagConstraints();

            OverlayIcon icn = new OverlayIcon(Icons.sentence);

            if (s.getOverrideText() != null) {
                ret.setText(s.getOverrideText());
            }

            if (!s.isProcessed()) {
                ret.setForeground(new Color(0x88, 0x88, 0x88));
            }

            if (s.getAttentionFlag()) {
                ret.setForeground(new Color(0xFF, 0xFF, 0x00));
                icn.add(Overlays.attention, OverlayIcon.TOP_LEFT);
            } 

            if (s.isClipping()) {
                ret.setForeground(new Color(0xe0, 0x00, 0x00));
            }

            if (s.isLocked()) {
                ret.setForeground(new Color(0x30, 0xb0, 0xFF));
                icn.add(Overlays.locked, OverlayIcon.BOTTOM_LEFT);
            } 

            if (s.getStartOffset() == 0) {
                icn.add(Overlays.important, OverlayIcon.TOP_RIGHT);
            } 

            if (s.getEndOffset() == s.getSampleSize() - 1) {
                icn.add(Overlays.important, OverlayIcon.TOP_RIGHT);
            }

            if (s.getEffectChain() != null) {
                if (!s.getEffectChain().equals("none")) {
                    icn.add(Overlays.filter, OverlayIcon.BOTTOM_RIGHT);
                }
            }

            ret.setIcon(icn);

            String gaptype = s.getPostGapType();
            DefaultMutableTreeNode prev = s.getPreviousSibling();
            String prevtype = "sentence";
            if (prev instanceof Sentence) {
                Sentence s2 = (Sentence)prev;
                prevtype = s2.getPostGapType();
            }

            if (prevtype.equals("continuation")) {
                ret.setIconTextGap(20);
            }

            if (gaptype.equals("sentence")) {
                p.setBorder(new EmptyBorder(0, 0, 0, 0));
            } else if (gaptype.equals("continuation")) {
                p.setBorder(new EmptyBorder(0, 0, 0, 0));
            } else if (gaptype.equals("paragraph")) {
                p.setBorder(new EmptyBorder(0, 0, 7, 0));
            } else if (gaptype.equals("section")) {
                p.setBorder(new EmptyBorder(0, 0, 15, 0));
            }


            JLabel time = new JLabelFixedWidth(75, " " + Utils.secToTime(s.getStartTime(), "mm.ss.SSS") + "     ");
            time.setHorizontalAlignment(SwingConstants.RIGHT);

            ctx.gridx = 0;
            ctx.gridy = 0;
            ctx.weightx = 1.0d;
            ctx.fill = GridBagConstraints.HORIZONTAL;
            ctx.anchor = GridBagConstraints.LINE_START;
            p.add(ret, ctx);

            String effectChain = s.getEffectChain();
            if ((effectChain != null) && (!effectChain.equals("none"))) {
                Effect e = s.getBook().effects.get(effectChain);
                if (e != null) {
                    JLabel eff = new JLabel(e.toString() + " ");
                    ctx.weightx = 0.0d;
                    ctx.gridx = 1;
                    p.add(eff);
                }
            }


            if (s.isProcessing()) {
                JLabel eff = new JLabel();
                eff.setIcon(Icons.processing);
                ctx.weightx = 0.0d;
                ctx.gridx = 2;
                p.add(eff);
            } else if (s.isQueued()) {
                JLabel eff = new JLabel();
                eff.setIcon(Icons.queued);
                ctx.weightx = 0.0d;
                ctx.gridx = 2;
                p.add(eff);
            }

            ctx.weightx = 0.0d;
            ctx.gridx = 3;
            ctx.anchor = GridBagConstraints.LINE_END;
            int peak = (int)s.getRMS();
            JLabel peakLabel = new JLabelFixedWidth(50, peak + "dB ");
            peakLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            if ((peak < -23) || (peak > -18)) {
                peakLabel.setForeground(new Color(0xCC, 0x00, 0x00));
            }
            p.add(peakLabel, ctx);

            ctx.weightx = 0.0d;
            ctx.gridx = 4;
            ctx.anchor = GridBagConstraints.LINE_END;
            p.add(time, ctx);

            p.setOpaque(false);

            return p;

        } else if (value instanceof Chapter) {
            Chapter c = (Chapter)value;

            ret.setIcon(Icons.chapter);

            JPanel p = new JPanel();
            p.setLayout(new GridBagLayout());
            GridBagConstraints ctx = new GridBagConstraints();

            JLabel time = new JLabel(Utils.secToTime(c.getLength(), "mm:ss") + "     ");

            ctx.gridx = 0;
            ctx.gridy = 0;
            ctx.fill = GridBagConstraints.HORIZONTAL;
            ctx.anchor = GridBagConstraints.LINE_START;
            ctx.weightx = 1.0d;
            p.add(ret, ctx);


            ctx.weightx = 0.0d;
            ctx.gridx = 1;
            ctx.anchor = GridBagConstraints.LINE_END;
            int peak = (int)c.getRMS();
            JLabel peakLabel = new JLabelFixedWidth(50, peak + "dB ");
            peakLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            if ((peak < -23) || (peak > -18)) {
                peakLabel.setForeground(new Color(0xCC, 0x00, 0x00));
            }
            p.add(peakLabel, ctx);

            ctx.weightx = 0.0d;
            ctx.gridx = 2;
            ctx.anchor = GridBagConstraints.LINE_END;
            p.add(time, ctx);
            p.setOpaque(false);
            return p;
        } else if (value instanceof Book) {
            Book b = (Book)value;

            JPanel p = new JPanel();
            p.setLayout(new GridBagLayout());
            GridBagConstraints ctx = new GridBagConstraints();

            ctx.gridx = 0;
            ctx.gridy = 0;
            ctx.fill = GridBagConstraints.HORIZONTAL;
            ctx.anchor = GridBagConstraints.LINE_START;
            ctx.weightx = 1.0d;

            ret.setIcon(b.getIcon());
            p.add(ret, ctx);

            JLabel author = new JLabel(b.getAuthor() + " - " + Utils.secToTime(b.getLength(), "HH:mm:ss"));
            ctx.gridy++;
            author.setBorder(new EmptyBorder(0, 27, 0, 0));
            Font f = author.getFont();
            Font nf = f.deriveFont(Font.ITALIC, (int)(f.getSize() * 0.75));
            author.setFont(nf);
            author.setForeground(author.getForeground().darker());
            p.add(author, ctx);

            p.setOpaque(false);
            return p;
            
        }
        return ret;
    }
}
