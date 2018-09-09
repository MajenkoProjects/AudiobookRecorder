package uk.co.majenko.audiobookrecorder;

import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class SteppedNumericSpinnerModel implements SpinnerModel {
    int min;
    int max;
    int step;
    int value;

    ArrayList<ChangeListener> listeners;

    public SteppedNumericSpinnerModel(int amin, int amax, int astep, int avalue) {
        min = amin;
        max = amax;
        step = astep;
        value = avalue;

        listeners = new ArrayList<ChangeListener>();
    }

    public Object getNextValue() {
        Integer v = value;
        v += step;
        if (v > max) return null;
        return v;
    }

    public Object getPreviousValue() {
        Integer v = value;
        v -= step;
        if (v < min) return null;
        return v;
    }

    public Object getValue() {
        Integer v = value;
        return v;
    }

    public void setValue(Object v) {
        if (v instanceof Integer) {
            Integer i = (Integer)v;
            value = i;
            ChangeEvent e = new ChangeEvent(this);
            for (ChangeListener l : listeners) {
                l.stateChanged(e);
            }
        }
    }

    public void addChangeListener(ChangeListener l) {
        listeners.add(l);
    }

    public void removeChangeListener(ChangeListener l) {
        listeners.remove(l);
    }

    public void setMinimum(int i) {
        min = i;
    }

    public void setMaximum(int i) {
        max = i;
    }

    public int getMaximum() {
        return max;
    }

    public int getMinimum() {
        return min;
    }
}

