package it.pgp.xfiles;

import it.pgp.xfiles.enums.ComparatorField;

/**
 * Created by pgp on 28/10/16
 */

public class SortingItem {
    public ComparatorField comparatorField;
    private boolean selected;
    private boolean reversed;

    public SortingItem(ComparatorField comparatorField, boolean selected, boolean reversed) {
        this.comparatorField = comparatorField;
        this.selected = selected;
        this.reversed = reversed;
    }

    public boolean isSelected() {
        return selected;
    }
    public boolean isReversed() {
        return reversed;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    public void setReversed(boolean reversed) {
        this.reversed = reversed;
    }

    public void toggleSelected() {
        selected = !selected;
    }
    public void toggleReversed() {
        reversed = !reversed;
    }

    // DEBUG toString
    // return comparatorField.name1()+": "+(selected?"V":"O")+", "+(reversed?"V":"O");
}
