package com.fx.swing.renderer;

import com.fx.swing.Globals;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Neural Cortex
 */
public class EmptyTableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column) {
        // Check if the value is the sentinel (-9999)
        if (value instanceof Number && ((Number) value).doubleValue() == Globals.NO_BEARING) {
            // Return an empty component to display nothing
            setText("");
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            return this;
        }

        // Use default rendering for other values
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}
