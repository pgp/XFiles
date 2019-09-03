package it.pgp.xfiles.utils;

import android.support.annotation.NonNull;

import java.util.Objects;

public class Pair<I,J> implements Comparable<Pair<I,J>> {
    @NonNull public I i;
    @NonNull public J j;

    public Pair(@NonNull I i, @NonNull J j) {
        this.i = i;
        this.j = j;
    }

    public void set(Pair<I,J> p) {
        i = p.i;
        j = p.j;
    }

    public void set(I i, J j) {
        this.i = i;
        this.j = j;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(i, pair.i) &&
                Objects.equals(j, pair.j);
    }

    @Override
    public String toString() {
        return "("+i+","+j+")";
    }

    @Override
    public int compareTo(Pair<I, J> otherPair) {
        return this.toString().compareTo(otherPair.toString());
    }
}
