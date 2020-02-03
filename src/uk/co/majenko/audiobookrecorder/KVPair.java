package uk.co.majenko.audiobookrecorder;

public class KVPair<K,V> implements Comparable {
    public K key;
    public V value;

    public KVPair(K k, V v) {
        key = k;
        value = v;
    }

    public String toString() {
        return (String)value;
    }

    public int compareTo(Object o) {
        return 0;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}
