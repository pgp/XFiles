package it.pgp.xfiles.utils;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/*
Web source: http://stackoverflow.com/questions/1670038/does-java-have-a-hashmap-with-reverse-lookup
(Qw3ry's answer - 07/02/2017 - removed loadFactor params)
 */

class BidirectionalMap<K, V> implements Map<K, V> {

    private final Map<K, V> map;
    private final Map<V, K> revMap;

    BidirectionalMap() {
        this.map = new HashMap<>();
        this.revMap = new HashMap<>();
    }

    private BidirectionalMap(Map<K, V> map, Map<V, K> reverseMap) {
        this.map = map;
        this.revMap = reverseMap;
    }

    @Override
    public void clear() {
        map.clear();
        revMap.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return revMap.containsKey(value);
    }

    @NonNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(map.entrySet());
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @NonNull
    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(map.keySet());
    }

    @Override
    public void putAll(@NonNull Map<? extends K, ? extends V> m) {
//        m.entrySet().forEach(e -> put(e.getKey(), e.getValue()));

        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public int size() {
        return map.size();
    }

    @NonNull
    @Override
    public Collection<V> values() {
        return Collections.unmodifiableCollection(map.values());
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        V v = remove(key);
        getReverseView().remove(value);
        map.put(key, value);
        revMap.put(value, key);
        return v;
    }

    Map<V, K> getReverseView() {
        return new BidirectionalMap<>(revMap, map);
    }

    @Override
    public V remove(Object key) {
        if (containsKey(key)) {
            V v = map.remove(key);
            revMap.remove(v);
            return v;
        } else {
            return null;
        }
    }

}