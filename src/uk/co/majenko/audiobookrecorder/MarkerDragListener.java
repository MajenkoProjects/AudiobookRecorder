package uk.co.majenko.audiobookrecorder;

public abstract interface MarkerDragListener {
    abstract public void leftMarkerMoved(MarkerDragEvent event);
    abstract public void rightMarkerMoved(MarkerDragEvent event);
}
    
