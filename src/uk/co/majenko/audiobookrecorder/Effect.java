package uk.co.majenko.audiobookrecorder;

import java.util.ArrayList;

public interface Effect  {
    public void process(Sample[] samples);
    public String getName();
    public ArrayList<Effect> getChildEffects();
    public void dump();
    public void init(double sr);
    public String toString();
}
