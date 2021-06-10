package uk.co.majenko.audiobookrecorder;

import java.util.ArrayList;

public class CacheManager {
    static ArrayList<Cacheable> cache = new ArrayList<Cacheable>();

    static int cacheSize = 10;

    public static void addToCache(Cacheable c) {
        Debug.trace();
        int iterations = 0;
        while (cache.size() >= cacheSize) {
            iterations++;
            if (iterations > cacheSize * 2) {
                System.err.println("Cache locked. Flushing.");
                cache.clear();
                cache.add(c);
                return;
            }
            Cacheable ob = cache.remove(0);
            if (ob != null) {
                if (ob.lockedInCache()) {
                    cache.add(ob);
                } else {
                    ob.clearCache();
                }
            }
        }

        cache.add(c);
    }

    public static void setCacheSize(int c) {
        cacheSize = c;
    }

    public static void removeFromCache(Cacheable c) {
        Debug.trace();
        cache.remove(c);
        c.clearCache();
    }

    public static void purgeCache() {
        for (Cacheable c : cache) {
            c.clearCache();
        }
        cache.clear();
    }
}
