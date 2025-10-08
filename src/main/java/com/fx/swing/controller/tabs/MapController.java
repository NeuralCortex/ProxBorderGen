package com.fx.swing.controller.tabs;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fx.swing.Globals;
import com.fx.swing.controller.MainController;
import com.fx.swing.controller.PopulateInterface;
import com.fx.swing.model.InfoTableModel;
import com.fx.swing.painter.BorderPainter;
import com.fx.swing.painter.PosPainter;
import com.fx.swing.pojo.InfoPOJO;
import com.fx.swing.pojo.PositionPOJO;
import com.fx.swing.pojo.TableHeaderPOJO;
import com.fx.swing.adapter.GeoSelectionAdapter;
import com.fx.swing.custom.IntegerComboBox;
import com.fx.swing.custom.IntegerTextField;
import com.fx.swing.dialog.ProgressDialog;
import com.fx.swing.listener.MousePositionListener;
import com.fx.swing.model.PositionTableModel;
import com.fx.swing.painter.PointPainter;
import com.fx.swing.renderer.EmptyTableCellRenderer;
import com.fx.swing.thread.AzimuthThread;
import com.fx.swing.thread.ExportThreadCSV;
import com.fx.swing.tools.LayoutFunctions;
import com.mapbox.geojson.MultiPolygon;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.MouseInputListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.CenterMapListener;
import org.jxmapviewer.input.PanKeyListener;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.shape.random.RandomPointsBuilder;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

public class MapController extends JPanel implements PopulateInterface, ActionListener {

    private MainController mainController;
    private ResourceBundle bundle;

    private final double lon = 10.671745101119196;
    private final double lat = 50.661742127393836;

    private IntegerTextField integerTextField;
    private IntegerComboBox cbNumber;
    private JTextField tfDeviation;
    private JCheckBox cbDeviation;
    private JButton btnGenerate;
    private JButton btnReset;
    private JButton btnCsvExport;
    private JTable tableInfo;
    private JTable tableData;

    private final JXMapViewer mapViewer = new JXMapViewer();
    private final List<Painter<JXMapViewer>> painters = new ArrayList<>();
    private GeoPosition marker;

    private final HashMap<Integer, List<PositionPOJO>> mapBorder = new HashMap<>();
    private InfoPOJO infoPOJO;

    private static final GeometryFactory wgsFactory = new GeometryFactory(new PrecisionModel(), Globals.WGS84_SRID);
    private static final CRSFactory crsFactory = new CRSFactory();
    private static final CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
    private static final CoordinateReferenceSystem wgs84 = crsFactory.createFromName("EPSG:4326");

    public MapController(MainController mainController) {
        this.mainController = mainController;
        this.bundle = mainController.getBundle();

        init();
    }

    private void init() {
        setLayout(new BorderLayout());

        Integer number[] = {500, 1000, 5000, 10000};
        cbNumber = new IntegerComboBox(number);

        integerTextField = new IntegerTextField(10);
        integerTextField.setMaximumSize(new Dimension(40, cbNumber.getPreferredSize().height));
        integerTextField.setText("50");

        tfDeviation = new JTextField();
        tfDeviation.setMaximumSize(new Dimension(20, cbNumber.getPreferredSize().height));
        tfDeviation.setText("25");

        cbDeviation = new JCheckBox(bundle.getString("lb.azi"));

        btnGenerate = new JButton(bundle.getString("btn.generate"));
        btnGenerate.addActionListener(this);
        btnReset = new JButton(bundle.getString("btn.reset"));
        btnReset.addActionListener(this);
        JSeparator jSeparator = new JSeparator(JSeparator.VERTICAL);
        JSeparator jSeparator2 = new JSeparator(JSeparator.VERTICAL);
        JPanel panelTop = LayoutFunctions.createOptionPanelX(Globals.COLOR_BLUE, new JLabel(""), new JLabel(bundle.getString("lb.width")), integerTextField, new JLabel(bundle.getString("lb.number")), cbNumber, btnGenerate, jSeparator, new JLabel(bundle.getString("lb.dev")), tfDeviation, cbDeviation, jSeparator2, btnReset);
        add(panelTop, BorderLayout.NORTH);

        JPanel panelInfo = LayoutFunctions.createOptionPanelX(Globals.COLOR_BLUE, new JLabel(bundle.getString("lb.geoinfo")));

        List<TableHeaderPOJO> headerList = new ArrayList<>();
        headerList.add(new TableHeaderPOJO(bundle.getString("lb.param"), String.class));
        headerList.add(new TableHeaderPOJO(bundle.getString("lb.value"), String.class));

        InfoTableModel infoTableModel = new InfoTableModel(headerList, new ArrayList<>());
        tableInfo = new JTable(infoTableModel);
        tableInfo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tableInfo.getSelectionModel().addListSelectionListener(e -> {

            if (!e.getValueIsAdjusting()) {

                int idx = tableInfo.getSelectedRow();
                if (idx != -1) {
                    infoPOJO = ((InfoTableModel) tableInfo.getModel()).getList().get(idx);

                    painters.clear();

                    mapBorder.clear();

                    CompoundPainter<JXMapViewer> painter = new CompoundPainter<>(painters);
                    mapViewer.setOverlayPainter(painter);
                    mapViewer.repaint();

                    loadPolygon(infoPOJO);
                }
            }
        });

        btnCsvExport = new JButton(bundle.getString("btn.csv.export"));
        btnCsvExport.addActionListener(this);

        JPanel panelExport = LayoutFunctions.createOptionPanelX(Globals.COLOR_BLUE, new JLabel(bundle.getString("lb.random")), btnCsvExport);

        headerList = new ArrayList<>();
        headerList.add(new TableHeaderPOJO(bundle.getString("col.idx"), String.class));
        headerList.add(new TableHeaderPOJO(bundle.getString("col.lat"), String.class));
        headerList.add(new TableHeaderPOJO(bundle.getString("col.lon"), String.class));
        headerList.add(new TableHeaderPOJO(bundle.getString("col.azi"), String.class));

        PositionTableModel positionTableModel = new PositionTableModel(headerList, new ArrayList<>());
        tableData = new JTable(positionTableModel);
        // Apply custom renderer to the Azimuth column (index 3)
        tableData.getColumnModel().getColumn(3).setCellRenderer(new EmptyTableCellRenderer());
        tableData.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        double heightPercentages[] = {10.0, 15.0, 10.0, 85.0};
        JPanel panelRight = LayoutFunctions.createVerticalGridbag(heightPercentages, panelInfo, tableInfo, panelExport, tableData);
        panelRight.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        add(panelRight, BorderLayout.EAST);

        initOSM();
    }

    private void initOSM() {
        TileFactoryInfo tileFactoryInfo = new OSMTileFactoryInfo();
        DefaultTileFactory defaultTileFactory = new DefaultTileFactory(tileFactoryInfo);
        defaultTileFactory.setThreadPoolSize(Runtime.getRuntime().availableProcessors());
        mapViewer.setTileFactory(defaultTileFactory);

        final JLabel labelAttr = new JLabel();
        mapViewer.setLayout(new BorderLayout());
        mapViewer.add(labelAttr, BorderLayout.SOUTH);
        labelAttr.setText(defaultTileFactory.getInfo().getAttribution() + " - " + defaultTileFactory.getInfo().getLicense());

        // Set the focus
        GeoPosition city = new GeoPosition(lat, lon);

        mapViewer.setZoom(14);
        mapViewer.setAddressLocation(city);

        // Add interactions
        MouseInputListener mil = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mil);
        mapViewer.addMouseMotionListener(mil);
        mapViewer.addMouseListener(new CenterMapListener(mapViewer));
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));
        mapViewer.addKeyListener(new PanKeyListener(mapViewer));

        MousePositionListener mousePositionListener = new MousePositionListener(mapViewer);
        mousePositionListener.setGeoPosListener((GeoPosition geoPosition) -> {
            mainController.getLabelStatus().setText(bundle.getString("col.lat") + ": " + geoPosition.getLatitude() + " " + bundle.getString("col.lon") + ": " + geoPosition.getLongitude());
        });
        mapViewer.addMouseMotionListener(mousePositionListener);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(mapViewer, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 10));

        add(panel, BorderLayout.CENTER);

        initPainter();
    }

    private void loadPolygon(InfoPOJO address) {
        if (address != null) {
            mapBorder.clear();

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            String state = URLEncoder.encode(address.getValue(), StandardCharsets.UTF_8);
            String baseURL = "https://nominatim.openstreetmap.org/search?" + address.getParam().toLowerCase() + "=" + state + "&polygon_geojson=1&format=geojson";

            JsonNode root;
            try {
                root = objectMapper.readTree(new URL(baseURL));
                //System.out.println(root.toPrettyString());

                JsonNode feat = root.get("features");
                JsonNode geo = feat.get(0).get("geometry");
                JsonNode type = geo.get("type");

                //System.out.println("type: " + type.toString());
                if (type.toString().replace("\"", "").startsWith("Multi")) {
                    MultiPolygon multiPolygon = MultiPolygon.fromJson(geo.toString());
                    int idx = 0;
                    for (List<List<com.mapbox.geojson.Point>> points : multiPolygon.coordinates()) {
                        int size = points.size();
                        //System.out.println("size: "+size);
                        for (int i = 0; i < size; i++) {
                            List<PositionPOJO> posList = new ArrayList<>();
                            //System.out.println("idx: "+idx+" size: "+points.get(i).size());
                            for (int j = 0; j < points.get(i).size(); j++) {
                                com.mapbox.geojson.Point p = points.get(i).get(j);
                                posList.add(new PositionPOJO(p.longitude(), p.latitude()));
                            }
                            mapBorder.put(idx, posList);

                            idx++;
                        }
                    }
                } else {
                    com.mapbox.geojson.Polygon polygon = com.mapbox.geojson.Polygon.fromJson(geo.toString());
                    int idx = 0;
                    for (List<com.mapbox.geojson.Point> points : polygon.coordinates()) {
                        List<PositionPOJO> posList = new ArrayList<>();
                        for (com.mapbox.geojson.Point point : points) {
                            posList.add(new PositionPOJO(point.longitude(), point.latitude()));
                        }
                        mapBorder.put(idx, posList);

                        idx++;
                    }
                }

                int idx = 0;

                for (Integer key : mapBorder.keySet()) {
                    List<PositionPOJO> list = mapBorder.get(key);

                    if (isGeoPositionInsidePolygon(marker, list)) {
                        idx = key;
                        break;
                    }
                }

                BorderPainter borderPainter = new BorderPainter(mapBorder.get(idx), Color.BLACK);
                painters.add(borderPainter);

                CompoundPainter<JXMapViewer> painter = new CompoundPainter<>(painters);
                mapViewer.setOverlayPainter(painter);
                mapViewer.repaint();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private boolean isGeoPositionInsidePolygon(GeoPosition point, List<PositionPOJO> polygonPoints) {
        // Validate input
        if (polygonPoints == null || polygonPoints.size() < 3) {
            System.out.println("Error: Invalid polygon (needs at least 3 points).");
            return false;
        }

        // Create JTS GeometryFactory
        GeometryFactory factory = new GeometryFactory();

        // Convert List<PositionPOJO> to JTS Coordinate array
        Coordinate[] coordinates = new Coordinate[polygonPoints.size() + 1];
        for (int i = 0; i < polygonPoints.size(); i++) {
            PositionPOJO pojo = polygonPoints.get(i);
            coordinates[i] = new Coordinate(pojo.getLon(), pojo.getLat());
        }
        // Close the polygon by repeating the first point
        coordinates[polygonPoints.size()] = coordinates[0];

        // Create a LinearRing and Polygon
        LinearRing ring = factory.createLinearRing(coordinates);
        Polygon polygon = factory.createPolygon(ring, null);

        // Convert GeoPosition to JTS Point
        Point jtsPoint = factory.createPoint(new Coordinate(point.getLongitude(), point.getLatitude()));

        // Check if the point is inside the polygon
        return jtsPoint.within(polygon);
    }

    private void initPainter() {
        GeoSelectionAdapter geoSelectionAdapter = new GeoSelectionAdapter(mapViewer, painters);
        geoSelectionAdapter.setGeoSelectionAdapterListener((GeoPosition geoPosition) -> {
            PosPainter posPainter = new PosPainter(geoPosition);
            painters.clear();
            painters.add(posPainter);

            ((InfoTableModel) tableInfo.getModel()).getList().clear();
            ((InfoTableModel) tableInfo.getModel()).fireTableDataChanged();

            ((PositionTableModel) tableData.getModel()).getList().clear();
            ((PositionTableModel) tableData.getModel()).fireTableDataChanged();

            getGeoInfos(geoPosition);
            marker = geoPosition;

            CompoundPainter<JXMapViewer> painter = new CompoundPainter<>(painters);
            mapViewer.setOverlayPainter(painter);
            mapViewer.repaint();
        });
        mapViewer.addMouseListener(geoSelectionAdapter);
    }

    private void getGeoInfos(GeoPosition geoPosition) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        String baseURL = "https://nominatim.openstreetmap.org/reverse?lat=" + geoPosition.getLatitude() + "&lon=" + geoPosition.getLongitude() + "&format=json&addressdetails=1&accept-language=en";
        JsonNode info;
        try {
            info = objectMapper.readTree(new URL(baseURL));
            JsonNode address = info.get("address");

            List<InfoPOJO> list = new ArrayList<>();
            ((InfoTableModel) tableInfo.getModel()).getList().clear();
            ((InfoTableModel) tableInfo.getModel()).fireTableDataChanged();

            for (Iterator<Map.Entry<String, JsonNode>> iter = address.fields(); iter.hasNext();) {
                Map.Entry entry = iter.next();
                String key = String.valueOf(entry.getKey());
                key = key.substring(0, 1).toUpperCase() + key.substring(1);
                String value = String.valueOf(entry.getValue());
                if (key.equalsIgnoreCase("country") || key.equalsIgnoreCase("state")) {
                    list.add(new InfoPOJO(key, value.replace("\"", "")));
                }
            }

            ((InfoTableModel) tableInfo.getModel()).setList(list);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void populate() {

    }

    @Override
    public void reset() {
        resetAll();
    }

    @Override
    public void clear() {

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton) {
            if (e.getSource() == btnGenerate) {
                int idx = tableInfo.getSelectedRow();
                if (idx == -1) {
                    JOptionPane.showMessageDialog(mainController, bundle.getString("msg.sel"), "Information", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    generate();
                }
            }
            if (e.getSource() == btnCsvExport) {
                int rowCount = tableData.getModel().getRowCount();
                if (rowCount == 0) {
                    JOptionPane.showMessageDialog(mainController, bundle.getString("msg.data"), "Information", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    exportCsv();
                }

            }
            if (e.getSource() == btnReset) {
                resetAll();
            }
        }
    }

    private void generate() {
        BorderPainter borderPainter = null;
        for (Painter painter : painters) {
            if (painter instanceof BorderPainter) {
                borderPainter = (BorderPainter) painter;
            }
        }

        try {
            int width = integerTextField.getIntValue();
            int number = cbNumber.getSelectedInt();

            if (borderPainter != null) {
                List<GeoPosition> posList = createPolygonFromGeoPositionsAndGenerateRandomPoints(borderPainter.getBorder(), width, number);
                List<Double> aziList = null;

                if (cbDeviation.isSelected()) {
                    ProgressDialog progressDialog = new ProgressDialog(mainController.getFrame(), bundle.getString("lb.azi"), Dialog.ModalityType.APPLICATION_MODAL, bundle.getString("lb.cancel"), 0, posList.size() - 1);
                    String text = tfDeviation.getText().trim();
                    double dev = Double.parseDouble(text);
                    AzimuthThread azimuthThread = new AzimuthThread(progressDialog, posList, borderPainter.getBorder(), dev);
                    azimuthThread.start();
                    progressDialog.setVisible(true);
                    try {
                        azimuthThread.join();
                    } catch (InterruptedException ex) {
                        System.out.println(ex.getLocalizedMessage());
                    }
                    aziList = azimuthThread.getAzimuths();
                }

                List<PositionPOJO> list = new ArrayList<>();
                for (int i = 0; i < posList.size(); i++) {
                    GeoPosition pos = posList.get(i);
                    if (aziList != null) {
                        list.add(new PositionPOJO(pos.getLongitude(), pos.getLatitude(), i, aziList.get(i)));
                    } else {
                        list.add(new PositionPOJO(pos.getLongitude(), pos.getLatitude(), i, Globals.NO_BEARING));
                    }
                }

                PointPainter pointPainter = new PointPainter(list);
                painters.add(pointPainter);

                CompoundPainter<JXMapViewer> painter = new CompoundPainter<>(painters);
                mapViewer.setOverlayPainter(painter);
                mapViewer.repaint();

                ((PositionTableModel) tableData.getModel()).getList().clear();
                ((PositionTableModel) tableData.getModel()).setList(list);
            }
        } catch (NumberFormatException | ParseException ex) {
            String msg = "";
            if (ex instanceof NumberFormatException) {
                msg = bundle.getString("msg.width");
            }
            if (ex instanceof ParseException) {
                msg = bundle.getString("msg.number");
            }
            JOptionPane.showMessageDialog(mainController, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCsv() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(Globals.propman.getProperty(Globals.DIR_CSV_OUTPUT, System.getProperty("user.dir"))));
        // Set file filter for .csv files
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        InfoPOJO selInfo = ((InfoTableModel) tableInfo.getModel()).get(tableInfo.getSelectedRow());
        String fileName = selInfo.getValue() + " KM" + integerTextField.getText() + " W" + cbNumber.getSelectedItem() + ".csv";
        fileChooser.setSelectedFile(new File(fileName));

        // Show save dialog
        int result = fileChooser.showSaveDialog(mainController);

        List<PositionPOJO> list = ((PositionTableModel) tableData.getModel()).getList();

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            ProgressDialog progressDialog = new ProgressDialog(mainController.getFrame(), bundle.getString("btn.csv"), Dialog.ModalityType.APPLICATION_MODAL, bundle.getString("lb.cancel"), 0, list.size() - 1);
            ExportThreadCSV exportThreadCSV = new ExportThreadCSV(progressDialog, list, file.getAbsolutePath(), cbDeviation.isSelected());
            exportThreadCSV.start();
            progressDialog.setVisible(true);

            Globals.propman.put(Globals.DIR_CSV_OUTPUT, file.getParent());
            Globals.propman.save();
        }
    }

    private List<GeoPosition> createPolygonFromGeoPositionsAndGenerateRandomPoints(
            List<PositionPOJO> geoPositions, double inwardDistanceKm, int numPoints) {
        if (geoPositions == null || geoPositions.size() < 3) {
            throw new IllegalArgumentException("At least 3 GeoPosition points are required to form a polygon.");
        }
        if (numPoints <= 0) {
            throw new IllegalArgumentException("Number of points must be positive.");
        }

        // Create original polygon in WGS84
        List<Coordinate> coordinates = new ArrayList<>();
        for (PositionPOJO gp : geoPositions) {
            coordinates.add(new Coordinate(gp.getLon(), gp.getLat()));
        }
        // Close the ring
        coordinates.add(new Coordinate(geoPositions.get(0).getLon(), geoPositions.get(0).getLat()));

        LinearRing ring = wgsFactory.createLinearRing(coordinates.toArray(new Coordinate[0]));
        Polygon originalPolygon = wgsFactory.createPolygon(ring, null); // Assume no holes

        // Compute centroid for UTM zone selection
        org.locationtech.jts.geom.Point centroid = originalPolygon.getCentroid();
        double centLon = centroid.getX();
        double centLat = centroid.getY();
        int zone = (int) Math.floor((centLon + 180) / 6) + 1;
        String epsgCode = (centLat >= 0) ? "326" + String.format("%02d", zone) : "327" + String.format("%02d", zone);
        String epsg = "EPSG:" + epsgCode;
        CoordinateReferenceSystem utm = crsFactory.createFromName(epsg);
        GeometryFactory utmFactory = new GeometryFactory(new PrecisionModel(), Integer.parseInt(epsgCode));

        // Project to UTM
        CoordinateTransform toUtm = ctFactory.createTransform(wgs84, utm);
        Polygon utmPolygon = (Polygon) projectGeometry(originalPolygon, toUtm, utmFactory);

        // Create inward buffer (negative distance in meters)
        double distanceMeters = inwardDistanceKm * 1000;
        Geometry utmInset = utmPolygon.buffer(-distanceMeters);

        // If inset is empty (polygon too small), use original as buffer zone
        Geometry utmBufferZone = utmInset.isEmpty() ? utmPolygon : utmPolygon.difference(utmInset);

        // Generate random points in the UTM buffer zone (uniform in area)
        RandomPointsBuilder builder = new RandomPointsBuilder(utmFactory);
        builder.setExtent(utmBufferZone);
        builder.setNumPoints(numPoints);
        Geometry utmPointsGeom = builder.getGeometry(); // MultiPoint

        // Project random points back to WGS84
        CoordinateTransform toWgs = ctFactory.createTransform(utm, wgs84);
        List<Point> wgsPoints = new ArrayList<>();
        for (int i = 0; i < utmPointsGeom.getNumGeometries(); i++) {
            Point utmPoint = (Point) utmPointsGeom.getGeometryN(i);
            Point wgsPoint = (Point) projectGeometry(utmPoint, toWgs, wgsFactory);
            wgsPoints.add(wgsPoint);
        }

        // Convert to GeoPosition
        return convertPointsToGeoPositions(wgsPoints);
    }

    private Geometry projectGeometry(Geometry geom, CoordinateTransform transform, GeometryFactory targetFactory) {
        if (geom instanceof Point) {
            Coordinate coord = geom.getCoordinate();
            ProjCoordinate src = new ProjCoordinate(coord.x, coord.y);
            ProjCoordinate dst = new ProjCoordinate();
            transform.transform(src, dst);
            return targetFactory.createPoint(new Coordinate(dst.x, dst.y));
        } else if (geom instanceof LinearRing) {
            Coordinate[] projected = projectCoordinates(geom.getCoordinates(), transform);
            return targetFactory.createLinearRing(projected);
        } else if (geom instanceof LineString) {
            Coordinate[] projected = projectCoordinates(geom.getCoordinates(), transform);
            return targetFactory.createLineString(projected);
        } else if (geom instanceof Polygon) {
            Polygon poly = (Polygon) geom;
            LinearRing shell = (LinearRing) projectGeometry(poly.getExteriorRing(), transform, targetFactory);
            LinearRing[] holes = new LinearRing[poly.getNumInteriorRing()];
            for (int i = 0; i < poly.getNumInteriorRing(); i++) {
                holes[i] = (LinearRing) projectGeometry(poly.getInteriorRingN(i), transform, targetFactory);
            }
            return targetFactory.createPolygon(shell, holes);
        } else if (geom instanceof GeometryCollection) {
            GeometryCollection coll = (GeometryCollection) geom;
            Geometry[] geoms = new Geometry[coll.getNumGeometries()];
            for (int i = 0; i < coll.getNumGeometries(); i++) {
                geoms[i] = projectGeometry(coll.getGeometryN(i), transform, targetFactory);
            }
            return targetFactory.createGeometryCollection(geoms);
        }
        throw new UnsupportedOperationException("Unsupported geometry type: " + geom.getGeometryType());
    }

    private static Coordinate[] projectCoordinates(Coordinate[] coords, CoordinateTransform transform) {
        Coordinate[] projected = new Coordinate[coords.length];
        for (int i = 0; i < coords.length; i++) {
            ProjCoordinate src = new ProjCoordinate(coords[i].x, coords[i].y);
            ProjCoordinate dst = new ProjCoordinate();
            transform.transform(src, dst);
            projected[i] = new Coordinate(dst.x, dst.y);
        }
        return projected;
    }

    private List<GeoPosition> convertPointsToGeoPositions(List<Point> points) {
        List<GeoPosition> geoPositions = new ArrayList<>();
        for (Point p : points) {
            geoPositions.add(new GeoPosition(p.getY(), p.getX())); // lat, lon
        }
        return geoPositions;
    }

    private void resetAll() {
        ((InfoTableModel) tableInfo.getModel()).getList().clear();
        ((InfoTableModel) tableInfo.getModel()).fireTableDataChanged();

        ((PositionTableModel) tableData.getModel()).getList().clear();
        ((PositionTableModel) tableData.getModel()).fireTableDataChanged();

        painters.clear();

        CompoundPainter<JXMapViewer> painter = new CompoundPainter<>(painters);
        mapViewer.setOverlayPainter(painter);
        mapViewer.repaint();
    }
}
