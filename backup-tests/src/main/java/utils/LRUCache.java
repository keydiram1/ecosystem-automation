package utils;

import java.util.LinkedHashMap;
import java.util.Map;

class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private final int lruSize;

    public LRUCache(int lruSize) {
        super(lruSize);
        this.lruSize = lruSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > lruSize;
    }

    public void add(K key) {
        put(key, null);
    }
}
