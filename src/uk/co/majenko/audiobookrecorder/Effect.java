package uk.co.majenko.audiobookrecorder;

import java.util.ArrayList;

public interface Effect  {
    public void process(double[][] samples);
    public String getName();
    public ArrayList<Effect> getChildEffects();
    public void init(double sr);
    public String toString();
}
