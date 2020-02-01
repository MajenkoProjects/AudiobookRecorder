package uk.co.majenko.audiobookrecorder;

import javax.swing.JTextField;

public class JTextFieldOb extends JTextField {
    Object object;

    public JTextFieldOb(String s) {
        super(s);
    }

    public void setObject(Object o) {
        object = o;
    }

    public Object getObject() {
        return object;
    }
}

