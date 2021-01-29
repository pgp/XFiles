package it.pgp.xfiles.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.support.annotation.NonNull;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import it.pgp.xfiles.R;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

public class BulkRenameDialog extends Dialog {
    private final BasePathContent baseDir;

    private final List<String> inputFilenames;
    private final ArrayAdapter<String> inputAdapter;
    private final ListView inputLv;

    private final ArrayList<String> outputFilenames = new ArrayList<>();
    private final ArrayAdapter<String> outputAdapter;
    private final ListView outputLv;

    private final EditText inputPattern, outputPattern;
    private final Button preview, ok;
    private final RadioGroup renameRadioGroup;

    public enum PatternType {
        STANDARD, REGEX, GLOB
    }

    public void highlightOutputPanel(boolean duplicates) {
        outputLv.setBackgroundResource(duplicates?android.R.color.holo_red_dark:R.color.transparentCobaltBlue);
        ok.setEnabled(!duplicates);
    }

    public BulkRenameDialog(@NonNull Activity activity, BasePathContent baseDir, List<String> inputFilenames) {
        super(activity, R.style.fs_dialog);
        this.baseDir = baseDir;
        this.inputFilenames = inputFilenames;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.bulk_rename_dialog);

        inputPattern = findViewById(R.id.inputRenamePattern);
        outputPattern = findViewById(R.id.outputRenamePattern);
        renameRadioGroup = findViewById(R.id.renameRadioGroup);
        preview = findViewById(R.id.renamePreviewButton);
        ok = findViewById(R.id.renameOkButton);

        inputAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, inputFilenames);
        inputLv = findViewById(R.id.inputRenameLv);
        inputLv.setAdapter(inputAdapter);
        outputAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, outputFilenames);
        outputLv = findViewById(R.id.outputRenameLv);
        outputLv.setAdapter(outputAdapter);
        highlightOutputPanel(false);

        preview.setOnClickListener(v -> {
            outputFilenames.clear();
            // convert names
            String ii = inputPattern.getText().toString();
            String oo = outputPattern.getText().toString();

            int idx = renameRadioGroup.indexOfChild(
                    renameRadioGroup.findViewById(
                            renameRadioGroup.getCheckedRadioButtonId()));
            switch(PatternType.values()[idx]) {
                case STANDARD:
                    for(String s : inputFilenames)
                        outputFilenames.add(s.replace(ii,oo));
                    break;
                case REGEX:
                    for(String s : inputFilenames)
                        outputFilenames.add(s.replaceAll(ii,oo));
                    break;
                case GLOB:
                    for(String s : inputFilenames)
                        outputFilenames.add(s.replaceAll(Misc.convertGlobToRegex(ii),oo));
                    break;
            }
            outputAdapter.notifyDataSetChanged();
            HashSet<String> s = new HashSet<>(outputFilenames);
            highlightOutputPanel(s.size() != outputFilenames.size());
        });
    }
}
