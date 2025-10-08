package com.fx.swing.thread;

import com.fx.swing.controller.tabs.GeoAzimuthCalculator;
import com.fx.swing.dialog.ProgressDialog;
import com.fx.swing.pojo.PositionPOJO;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jxmapviewer.viewer.GeoPosition;

public class AzimuthThread extends Thread implements ActionListener {

    private static final Logger _log = LogManager.getLogger(AzimuthThread.class);
    private final ProgressDialog progressDialog;
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final List<GeoPosition> list;
    private final List<PositionPOJO> border;
    private final double deviationPercent;
    private static final Random random = new Random(); // Random number generator for deviation
    private List<Double> azimuths = new ArrayList<>(); // Store calculated localAzimuths

    public AzimuthThread(ProgressDialog progressDialog, List<GeoPosition> list, List<PositionPOJO> border, double deviationPercent) {
        this.progressDialog = progressDialog;
        this.list = list;
        this.border = border;
        this.deviationPercent = Math.max(0, Math.min(100, deviationPercent)); // Clamp between 0 and 100
    }

    @Override
    public void run() {
        azimuths = calculateAzimuthsToBorder(list, border, deviationPercent);
        progressDialog.dispose();
    }

    /**
     * Returns the list of calculated localAzimuths.
     * @return List of localAzimuths in degrees (0-360)
     */
    public List<Double> getAzimuths() {
        return new ArrayList<>(azimuths); // Return a copy to prevent external modification
    }

    /**
     * Calculates localAzimuths from a list of positions to the nearest points on a
country border with a random deviation using multiple threads.
     *
     * @param positions List of GeoPosition objects representing observer locations
     * @param borderPoints List of PositionPOJO objects representing country border points
     * @param deviationPercent Percentage deviation to apply to each azimuth (±deviationPercent%)
     * @return List of localAzimuths in degrees (0-360) from each position to the nearest border point with deviation
     */
    private List<Double> calculateAzimuthsToBorder(List<GeoPosition> positions, List<PositionPOJO> borderPoints, double deviationPercent) {
        List<Double> localAzimuths = new ArrayList<>(positions.size());
        // Initialize list with nulls to maintain order
        for (int i = 0; i < positions.size(); i++) {
            localAzimuths.add(null);
        }

        if (borderPoints == null || positions.isEmpty() || borderPoints.isEmpty()) {
            return localAzimuths; // Return empty list if inputs are invalid
        }

        // Determine number of threads based on available CPUs
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicInteger progress = new AtomicInteger(0);

        // Divide positions into chunks
        int chunkSize = Math.max(1, positions.size() / numThreads);
        for (int i = 0; i < positions.size(); i += chunkSize) {
            int start = i;
            int end = Math.min(i + chunkSize, positions.size());
            List<GeoPosition> chunk = positions.subList(start, end);

            executor.submit(() -> {
                for (int j = 0; j < chunk.size(); j++) {
                    if (stop.get()) {
                        break;
                    }

                    GeoPosition position = chunk.get(j);
                    // Find the closest border point
                    PositionPOJO closestBorderPoint = GeoAzimuthCalculator.findClosestBorderPoint(position, borderPoints);
                    // Calculate azimuth to the closest border point
                    double azimuth = GeoAzimuthCalculator.calculateAzimuth(position, closestBorderPoint);
                    // Apply random deviation
                    double maxDeviation = azimuth * (deviationPercent / 100.0);
                    double deviation = (random.nextDouble() * 2 - 1) * maxDeviation; // Random value between -maxDeviation and +maxDeviation
                    double adjustedAzimuth = (azimuth + deviation + 360) % 360; // Normalize to 0-360 degrees

                    // Store result at correct index
                    synchronized (localAzimuths) {
                        localAzimuths.set(start + j, adjustedAzimuth);
                    }

                    // Update progress bar in a thread-safe manner
                    int currentProgress = progress.incrementAndGet();
                    EventQueue.invokeLater(() -> {
                        progressDialog.getProgressBar().setValue(currentProgress - 1);
                    });
                }
            });
        }

        // Shutdown executor and wait for tasks to complete
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                _log.warn("Azimuth calculation timed out");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            _log.error("Azimuth calculation interrupted", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt(); // Preserve interrupt status
        }

        return localAzimuths;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == progressDialog.getButtonAbort()) {
            stop.set(true);
        }
    }
}