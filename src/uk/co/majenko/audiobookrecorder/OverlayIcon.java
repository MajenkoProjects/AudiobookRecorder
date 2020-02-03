package uk.co.majenko.audiobookrecorder;

import javax.swing.ImageIcon;
import java.util.List;
import java.util.ArrayList;
import java.awt.Component;
import java.awt.Graphics;

public class OverlayIcon extends ImageIcon {
    class IconSpec {
        public ImageIcon icon;
        int location;

        public IconSpec(ImageIcon i, int loc) {
            icon = i;
            location = loc;
        }
    }

    private ImageIcon base;
    private ArrayList<IconSpec> overlays;

    public static final int TOP_LEFT        = 0;
    public static final int TOP_MIDDLE      = 1;
    public static final int TOP_RIGHT       = 2;
    public static final int MIDDLE_LEFT     = 3;
    public static final int MIDDLE_MIDDLE   = 4;
    public static final int MIDDLE_RIGHT    = 5;
    public static final int BOTTOM_LEFT     = 6;
    public static final int BOTTOM_MIDDLE   = 7;
    public static final int BOTTOM_RIGHT    = 8;

    public OverlayIcon(ImageIcon base) {
        super(base.getImage());
        this.base = base;
        this.overlays = new ArrayList<IconSpec>();
    }

    public void add(ImageIcon overlay, int position) {
        overlays.add(new IconSpec(overlay, position));
    }

    @Override

    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
        base.paintIcon(c, g, x, y);

        int bw = base.getIconWidth();
        int bh = base.getIconHeight();

        for(IconSpec icon : overlays) {
            int iw = icon.icon.getIconWidth();
            int ih = icon.icon.getIconHeight();

            int ix = 0;
            int iy = 0;

            switch (icon.location) {
                case TOP_LEFT:
                case MIDDLE_LEFT:
                case BOTTOM_LEFT:
                    ix = 0;
                    break;

                case TOP_MIDDLE:
                case MIDDLE_MIDDLE:
                case BOTTOM_MIDDLE:
                    ix = (bw / 2) - (iw / 2);
                    break;
            
                case TOP_RIGHT:
                case MIDDLE_RIGHT:
                case BOTTOM_RIGHT:
                    ix = bw - iw;
                    break;
            }

            switch (icon.location) {
                case TOP_LEFT:
                case TOP_MIDDLE:
                case TOP_RIGHT:
                    iy = 0;
                    break;

                case MIDDLE_LEFT:
                case MIDDLE_MIDDLE:
                case MIDDLE_RIGHT:
                    iy = (bh / 2) - (ih / 2);
                    break;

                case BOTTOM_LEFT:
                case BOTTOM_MIDDLE:
                case BOTTOM_RIGHT:
                    iy = bh - ih;
                    break;
            }

            icon.icon.paintIcon(c, g, x + ix, y + iy);
        }
    }
}
