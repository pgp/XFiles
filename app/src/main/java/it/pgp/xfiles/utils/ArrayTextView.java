package it.pgp.xfiles.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import it.pgp.xfiles.utils.pathcontent.ArchivePathContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

public class ArrayTextView extends TextView {

    List<BasePathContent> texts = new ArrayList<>();

    public ArrayTextView(Context context) {
        super(context);
    }

    public ArrayTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ArrayTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void clear() {
        texts.clear();
        super.setText("");
    }

    public List<BasePathContent> getTexts() {
        return texts;
    }

    public void setTexts(List<BasePathContent> texts) {
        this.texts = texts;
        StringBuilder sb = new StringBuilder();
        for(BasePathContent bpc : texts) sb.append(bpc instanceof ArchivePathContent ?
                ((ArchivePathContent) bpc).archivePath :
                bpc.dir
                ).append("\n");
        super.setText(sb.toString());
    }

    public List<String> getMultipleLocalPathsAsStrings() {
        ArrayList<String> l = new ArrayList<>();
        for(BasePathContent bpc : texts) {
            if(bpc instanceof LocalPathContent) l.add(bpc.dir);
        }
        return l;
    }
}