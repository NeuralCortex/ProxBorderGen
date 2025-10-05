package com.fx.swing.custom;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import javax.swing.JFormattedTextField;
import javax.swing.text.NumberFormatter;

public class IntegerTextField extends JFormattedTextField {

    public IntegerTextField(int columns) {
        super(NumberFormat.getIntegerInstance(Locale.US));
        setColumns(columns);
        NumberFormatter formatter = new NumberFormatter(NumberFormat.getIntegerInstance(Locale.US));
        formatter.setValueClass(Integer.class);
        formatter.setAllowsInvalid(false); // Only allow valid integers
        formatter.setCommitsOnValidEdit(true); // Update value on valid input
        setFormatter(formatter);
    }

    // Helper method to get the integer value
    public int getIntValue() throws ParseException {
        Object value = getValue();
        if (value instanceof Number) {
            Number num=(Number)value;
            if(num.intValue()==num.doubleValue()){
             return num.intValue();   
            }
        }
        throw new ParseException("No valid integer", 0);
    }
}
