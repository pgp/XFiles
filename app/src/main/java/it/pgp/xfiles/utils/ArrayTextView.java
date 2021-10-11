package it.pgp.xfiles.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ArrayTextView extends TextView {

    public final List<String> texts = new ArrayList<>();

    public ArrayTextView(Context context) {
        super(context);
    }

    public ArrayTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ArrayTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public final void addText(String input) {
        texts.add(input);
        StringBuilder sb = new StringBuilder();
        for(String s : texts) sb.append(s).append("\n");
        super.setText(sb.toString());
    }

    public void clear() {
        texts.clear();
        super.setText("");
    }
}