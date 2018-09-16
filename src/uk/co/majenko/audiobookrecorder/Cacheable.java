package uk.co.majenko.audiobookrecorder;

public interface Cacheable {
    public abstract void clearCache();
    public abstract boolean lockedInCache();
}
