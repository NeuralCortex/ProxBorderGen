package com.fx.swing.painter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.List;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

public class PointPainter implements Painter<JXMapViewer> {

    private final Color color = Color.RED;
    private final boolean antiAlias = true;
    private final List<GeoPosition> list;

    public PointPainter(List<GeoPosition> list) {
        this.list = list;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int i, int i1) {
        g = (Graphics2D) g.create();

        Rectangle rect = map.getViewportBounds();
        g.translate(-rect.x, -rect.y);

        if (antiAlias) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        drawList(g, map);

        g.dispose();
    }

    private void drawList(Graphics2D g, JXMapViewer map) {
        g.setColor(color);
        g.setStroke(new BasicStroke(1));

        for (GeoPosition pos : list) {
            Point2D pt = map.getTileFactory().geoToPixel(pos, map.getZoom());
            drawPoint(g, pt, 3);
        }
    }

    private void drawPoint(Graphics2D g, Point2D point2D, int size) {
        int x = (int) point2D.getX();
        int y = (int) point2D.getY();

        x = x + (int) (size / 2.0);
        y = y + (int) (size / 2.0);

        g.fillOval(x, y, size, size);
    }
}
