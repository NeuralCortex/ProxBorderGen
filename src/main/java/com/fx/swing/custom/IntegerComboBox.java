package com.fx.swing.custom;

import javax.swing.JComboBox;

public class IntegerComboBox extends JComboBox<Integer> {

    public IntegerComboBox(Integer[] items) {
        super(items);
        setSelectedIndex(0); // Select first item by default
    }

    public int getSelectedInt() throws NumberFormatException {
        Object selected = getSelectedItem();
        if (selected instanceof Integer) {
            return (Integer) selected;
        }
        throw new NumberFormatException("Selected item is not an integer.");
    }
}
