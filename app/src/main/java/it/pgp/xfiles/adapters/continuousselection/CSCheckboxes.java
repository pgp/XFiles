package it.pgp.xfiles.adapters.continuousselection;

import android.widget.CheckedTextView;

import it.pgp.xfiles.utils.Misc;

public class CSCheckboxes {
    public CheckedTextView continuousSelection; // governs the visibility of the other two checkboxes
    public CheckedTextView invertSelection;
    public CheckedTextView stickySelection;

    public CheckedTextView getContinuousSelection() {
        return continuousSelection;
    }

    public void setContinuousSelection(CheckedTextView continuousSelection) {
        this.continuousSelection = continuousSelection;
//        this.continuousSelection.setOnClickListener(Misc.ctvListener); // already done in method toggleSelStatus of ContSelHandlingLayout
    }

    public CheckedTextView getInvertSelection() {
        return invertSelection;
    }

    public void setInvertSelection(CheckedTextView invertSelection) {
        this.invertSelection = invertSelection;
        this.invertSelection.setOnClickListener(Misc.ctvListener);
    }

    public CheckedTextView getStickySelection() {
        return stickySelection;
    }

    public void setStickySelection(CheckedTextView stickySelection) {
        this.stickySelection = stickySelection;
        this.stickySelection.setOnClickListener(Misc.ctvListener);
    }

    public boolean[] getAsBooleans() {
        return new boolean[]{
                continuousSelection != null && continuousSelection.isChecked(),
                invertSelection != null && invertSelection.isChecked(),
                stickySelection != null && stickySelection.isChecked()
        };
    }
}
