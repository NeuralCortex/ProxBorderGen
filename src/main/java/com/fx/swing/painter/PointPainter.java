package com.fx.swing.painter;

import com.fx.swing.Globals;
import com.fx.swing.pojo.PositionPOJO;
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
    private final List<PositionPOJO> list;
    private final double lineLength = 50.0; // Length of azimuth line in pixels

    public PointPainter(List<PositionPOJO> list) {
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
        g.setStroke(new BasicStroke(1));

        for (int i = 0; i < list.size(); i++) {
            g.setColor(color);
            PositionPOJO pos = list.get(i);
            Point2D pt = map.getTileFactory().geoToPixel(new GeoPosition(pos.getLat(), pos.getLon()), map.getZoom());
            drawPoint(g, pt, 3);

            // Draw azimuth line if corresponding azimuth exists
            if (pos.getAzi() != Globals.NO_BEARING) {
                g.setColor(Color.BLUE);
                drawAzimuthLine(g, pt, pos.getAzi(), 3);
            }
        }
    }

    private void drawPoint(Graphics2D g, Point2D point2D, int size) {
        int x = (int) point2D.getX();
        int y = (int) point2D.getY();

        x = x + (int) (size / 2.0);
        y = y + (int) (size / 2.0);

        g.fillOval(x, y, size, size);
    }

    private void drawAzimuthLine(Graphics2D g, Point2D startPoint, double azimuth, int pointSize) {
        // Convert azimuth from degrees to radians
        double azimuthRad = Math.toRadians(azimuth);

        // Adjust start point to match the center of the drawn point
        double startX = startPoint.getX() + pointSize / 2.0;
        double startY = startPoint.getY() + pointSize / 2.0;

        // Calculate the end point of the line
        double endX = startX + lineLength * Math.sin(azimuthRad);
        double endY = startY - lineLength * Math.cos(azimuthRad); // Subtract because y increases downward in graphics

        // Draw the line
        g.drawLine((int) startX, (int) startY, (int) endX, (int) endY);
    }
}