package it.pgp.xfiles.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

public class ArrayTextView extends TextView {

    final List<BasePathContent> texts = new ArrayList<>();

    public ArrayTextView(Context context) {
        super(context);
    }

    public ArrayTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ArrayTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public List<BasePathContent> getTexts() {
        return texts;
    }

    public void setTexts(List<BasePathContent> bpcs) {
        texts.clear();
        texts.addAll(bpcs);
        StringBuilder sb = new StringBuilder();
        for(BasePathContent bpc : bpcs) sb.append(bpc.toString()).append("\n");
        super.setText(sb.toString());
    }

    public void setText(BasePathContent bpc) {
        texts.clear();
        texts.add(bpc);
        super.setText(bpc.toString());
    }

    public List<String> getMultipleLocalPathsAsStrings() {
        ArrayList<String> l = new ArrayList<>();
        for(BasePathContent bpc : texts) {
            if(bpc instanceof LocalPathContent) l.add(bpc.dir);
        }
        return l;
    }
}