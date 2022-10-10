package it.pgp.xfiles.viewmodels;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import it.pgp.xfiles.R;

public class PasteableEditText extends LinearLayout {
    EditText editText;
    ImageButton pasteButton;
    ClipboardManager clipboard;

    private void init(Context context) {
        clipboard = (ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.pasteable_edittext, this);
    }

    public PasteableEditText(Context context) {
        super(context);
        init(context);
    }

    public PasteableEditText(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        editText = findViewById(R.id.pasteable_edittext_et);
        pasteButton = findViewById(R.id.pasteable_edittext_ib);
        pasteButton.setOnClickListener(v -> {
            ClipData clipData = clipboard.getPrimaryClip();
            ClipData.Item item = clipData.getItemAt(0);
            editText.setText(item.getText());
        });
    }

    public Editable getText() {
        return editText.getText();
    }

    public void setText(CharSequence text) {
        editText.setText(text);
    }
}
