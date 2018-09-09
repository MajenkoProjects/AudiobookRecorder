package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import java.awt.*;

public class CustomTreeUI extends BasicTreeUI {

        JScrollPane pane;

        public CustomTreeUI(JScrollPane p) {
            super();
            pane = p;
        }

        @Override
        protected AbstractLayoutCache.NodeDimensions createNodeDimensions() {
            return new NodeDimensionsHandler() {
                @Override
                public Rectangle getNodeDimensions(
                        Object value, int row, int depth, boolean expanded,
                        Rectangle size) {
                    Rectangle dimensions = super.getNodeDimensions(value, row,
                            depth, expanded, size);
                    dimensions.width =
                            pane.getWidth() - getRowX(row, depth);
                    return dimensions;
                }
            };
        }

        @Override
        protected void paintHorizontalLine(Graphics g, JComponent c,
                                           int y, int left, int right) {
            // do nothing.
        }

        @Override
        protected void paintVerticalPartOfLeg(Graphics g, Rectangle clipBounds,
                                              Insets insets, TreePath path) {
            // do nothing.
        }
}
