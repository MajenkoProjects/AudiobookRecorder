package uk.co.majenko.audiobookrecorder;

import javax.swing.JSlider;

public class JSliderOb extends JSlider {
    Object object;

    public JSliderOb(int a, int b, int c) {
        super(a, b, c);
    }

    public void setObject(Object o) {
        object = o;
    }

    public Object getObject() {
        return object;
    }
}

