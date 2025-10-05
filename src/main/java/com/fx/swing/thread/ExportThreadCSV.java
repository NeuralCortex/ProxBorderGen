package com.fx.swing.thread;

import com.fx.swing.dialog.ProgressDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jxmapviewer.viewer.GeoPosition;

public class ExportThreadCSV extends Thread implements ActionListener {

    private static final Logger _log = LogManager.getLogger(ExportThreadCSV.class);
    private final ProgressDialog progressDialog;
    private boolean stop = false;
    private final String filePath;
    private final List<GeoPosition> list;

    public ExportThreadCSV(ProgressDialog progressDialog, List<GeoPosition> list, String filePath) {
        this.progressDialog = progressDialog;
        this.list = list;
        this.filePath = filePath;
    }

    @Override
    public void run() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));

            // Write header
            writer.write("index;latitude;longitude");
            writer.newLine();

            File dir = new File(filePath);
            if (!dir.exists()) {
                dir.mkdir();
            }

            for (int i = 0; i < list.size(); i++) {

                if (stop) {
                    break;
                }

                GeoPosition geoPosition = list.get(i);

                // Write each geoposition
                // Use US locale for consistent decimal point (.)
                String line = String.format(Locale.US, "%d;%f;%f", i, geoPosition.getLatitude(), geoPosition.getLongitude());
                writer.write(line);
                writer.newLine();

                progressDialog.getProgressBar().setValue(i);
            }
            
            writer.close();
        } catch (Exception ex) {
            _log.error(ex.getMessage());
        }

        progressDialog.dispose();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == progressDialog.getButtonAbort()) {
            stop = true;
        }
    }
}
