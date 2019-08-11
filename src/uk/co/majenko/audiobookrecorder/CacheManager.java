package uk.co.majenko.audiobookrecorder;

import java.util.*;

public class CacheManager {
    static ArrayList<Cacheable> cache = new ArrayList<Cacheable>();

    static int cacheSize = 10;

    public static void addToCache(Cacheable c) {
        while (cache.size() >= cacheSize) {
            Cacheable ob = cache.remove(0);
            if (ob.lockedInCache()) {
                cache.add(ob);
            } else {
                ob.clearCache();
            }
        }

        cache.add(c);

        System.gc();
    }

    public static void setCacheSize(int c) {
        cacheSize = c;
    }

    public static void removeFromCache(Cacheable c) {
        cache.remove(c);
    }
}
