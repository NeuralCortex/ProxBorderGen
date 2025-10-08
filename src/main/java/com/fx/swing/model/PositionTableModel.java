package com.fx.swing.model;

import com.fx.swing.pojo.PositionPOJO;
import com.fx.swing.pojo.TableHeaderPOJO;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PositionTableModel extends AbstractTableModel {

    private static final Logger _log = LogManager.getLogger(PositionTableModel.class);
    private final List<TableHeaderPOJO> headerList;
    private List<PositionPOJO> list;

    public PositionTableModel(List<TableHeaderPOJO> headerList, List<PositionPOJO> list) {
        this.headerList = headerList;
        this.list = list;
    }

    @Override
    public int getRowCount() {
        return list.size();
    }

    @Override
    public int getColumnCount() {
        return headerList.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        PositionPOJO positionPOJO = list.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return rowIndex;
            case 1:
                return positionPOJO.getLat();
            case 2:
                return positionPOJO.getLon();
            case 3:
                return positionPOJO.getAzi();
            default:
                throw new AssertionError();
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return headerList.get(columnIndex).getKlasse();
    }

    @Override
    public String getColumnName(int column) {
        return headerList.get(column).getName();
    }

    public void add(PositionPOJO positionPOJO) {
        list.add(positionPOJO);
        fireTableDataChanged();
    }

    public List<PositionPOJO> getList() {
        return list;
    }

    public void setList(List<PositionPOJO> list) {
        this.list = list;
        fireTableDataChanged();
    }
}
