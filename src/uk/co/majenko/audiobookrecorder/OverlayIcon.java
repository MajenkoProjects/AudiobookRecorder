/*
 * Copyright (c) 2015, Majenko Technologies
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * 
 * * Neither the name of Majenko Technologies nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
