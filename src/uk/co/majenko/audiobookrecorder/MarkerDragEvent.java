package uk.co.majenko.audiobookrecorder;

public class MarkerDragEvent {

    Object src;
    int position;

    public MarkerDragEvent(Object s, int pos) {
        src = s;
        position = pos;
    }

    public Object getSource() {
        return src;
    }

    public int getPosition() {
        return position;
    }
}
