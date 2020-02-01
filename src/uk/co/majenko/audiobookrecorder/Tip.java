package uk.co.majenko.audiobookrecorder;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.font.FontRenderContext;
import java.awt.FontMetrics;
import java.awt.Dimension;
import org.apache.commons.text.WordUtils;

public class Tip extends JLabel implements MouseListener {
    String tip = null;

    public Tip(String text) {
        tip = WordUtils.wrap(text, 50);

        setToolTipText(text);
        setIcon(Icons.tooltip);
        addMouseListener(this);
    }

    @Override
    public JToolTip createToolTip() {
        JToolTip tt = new JToolTip() {
            public String getTipText() {
                return "*" + tip + "*";
            }

            public void paintComponent(Graphics g) {

                Rectangle r = g.getClipBounds();

                JLabel l = new JLabel();

                AffineTransform affinetransform = new AffineTransform();     
                FontRenderContext frc = new FontRenderContext(affinetransform,true,true);
                Font f = l.getFont().deriveFont(14f);

                g.setColor(new Color(200, 200, 180));
                g.fillRect(0, 0, r.width, r.height);

                g.setColor(new Color(10, 10, 10));
                g.setFont(f);
                int y = 3;
                String[] lines = tip.split("\n");
                for (String line : lines) {
                    Rectangle2D bounds = f.getStringBounds(line, frc);
                    y += bounds.getHeight();
                    g.drawString(line, 5, y);
                }
            
            }

            public Dimension getPreferredSize() {

                JLabel l = new JLabel();
                AffineTransform affinetransform = new AffineTransform();     
                FontRenderContext frc = new FontRenderContext(affinetransform,true,true);  
                Font f = l.getFont().deriveFont(14f);

                String[] lines = tip.split("\n");
                int w = 0;
                int h = 0;
                for (String line : lines) {
                    Rectangle2D bounds = f.getStringBounds(line, frc);
                    double fw = bounds.getWidth();
                    if (fw > w) w = (int)fw;
                    h += bounds.getHeight();
                }

                Dimension s = new Dimension(w + 10, h + 10);
                return s;
            }
        };

        tt.removeAll();
        JPanel p = new JPanel();
        tt.add(p);
        JLabel l = new JLabel(tip);
        l.setBackground(new Color(200, 200, 180));
        l.setForeground(new Color(0, 0, 0));
        p.add(l);
        return tt;
    }

    public void mouseEntered(MouseEvent evt) {
//        showTipWindow();
    }

    public void mouseExited(MouseEvent evt) {
//        hideTipWindow();
    }

    public void mousePressed(MouseEvent evt) {
    }

    public void mouseReleased(MouseEvent evt) {
    }

    public void mouseClicked(MouseEvent evt) {
    }

}
