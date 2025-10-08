package com.fx.swing.controller.tabs;

import com.fx.swing.pojo.PositionPOJO;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.jxmapviewer.viewer.GeoPosition;

/**
 * @author Neural Cortex
 */
public class GeoAzimuthCalculator {

   

    /**
     * Finds the closest border point to a given position using Haversine distance.
     * @param position The observer's position
     * @param borderPoints List of border points
     * @return The closest PositionPOJO on the border
     */
    public static PositionPOJO findClosestBorderPoint(GeoPosition position, List<PositionPOJO> borderPoints) {
        PositionPOJO closest = borderPoints.get(0);
        double minDistance = calculateHaversineDistance(position, closest);

        for (PositionPOJO borderPoint : borderPoints) {
            double distance = calculateHaversineDistance(position, borderPoint);
            if (distance < minDistance) {
                minDistance = distance;
                closest = borderPoint;
            }
        }

        return closest;
    }

    /**
     * Calculates the Haversine distance between two geographic points in kilometers.
     * @param pos1 First position
     * @param pos2 Second position
     * @return Distance in kilometers
     */
    public static double calculateHaversineDistance(GeoPosition pos1, PositionPOJO pos2) {
        final double R = 6371.0; // Earth's radius in kilometers

        double lat1 = Math.toRadians(pos1.getLatitude());
        double lon1 = Math.toRadians(pos1.getLongitude());
        double lat2 = Math.toRadians(pos2.getLat());
        double lon2 = Math.toRadians(pos2.getLon());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Calculates the azimuth (bearing) from pos1 to pos2 in degrees.
     * @param pos1 Starting position
     * @param pos2 Destination position
     * @return Azimuth in degrees (0-360)
     */
    public static double calculateAzimuth(GeoPosition pos1, PositionPOJO pos2) {
        double lat1 = Math.toRadians(pos1.getLatitude());
        double lon1 = Math.toRadians(pos1.getLongitude());
        double lat2 = Math.toRadians(pos2.getLat());
        double lon2 = Math.toRadians(pos2.getLon());

        double dLon = lon2 - lon1;

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                   Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        double azimuth = Math.atan2(y, x);
        azimuth = Math.toDegrees(azimuth);
        azimuth = (azimuth + 360) % 360; // Normalize to 0-360 degrees

        return azimuth;
    }
}