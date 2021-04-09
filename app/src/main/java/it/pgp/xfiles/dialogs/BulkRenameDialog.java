package it.pgp.xfiles.dialogs;

import android.app.Dialog;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.items.BulkRenameItem;
import it.pgp.xfiles.utils.FileOperationHelper;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

public class BulkRenameDialog extends Dialog {
    private final FileOperationHelper helper;

    private final List<String> inputFilenames;
    private final ArrayAdapter<String> inputAdapter;

    private final ArrayList<BulkRenameItem> outputFilenames = new ArrayList<>();
    private final ArrayAdapter<BulkRenameItem> outputAdapter;

    private final ListView[] lvs = new ListView[]{null,null}; // inputLv, outputLv

    private Map<BulkRenameItem, Set<Integer>> itemsOccurrences;

    private final EditText inputPattern, outputPattern;
    private final Button preview, ok;
    private final ProgressBar pb;
    private final RadioGroup renameRadioGroup;

    private AdapterView.OnItemClickListener getListener(int lvPos) {
        return (parent, view, position, id) -> {
            // TODO use smoothScrollToPosition only if there are few items (e.g. less than 100)
            lvs[1-lvPos].setSelection(position);
            MainActivity.handler.postDelayed(()-> Misc.highlightListViewItem(position, lvs[1-lvPos]),250);
        };
    }

    public enum PatternType {
        STANDARD, REGEX, GLOB
    }

    public boolean highlightOutputPanel() {
        boolean duplicates = false;
        itemsOccurrences = Misc.createOccurrencesMap(outputFilenames);
        for(Map.Entry<BulkRenameItem, Set<Integer>> me : itemsOccurrences.entrySet()) {
            Set<Integer> v = me.getValue();
            if(v.size()>1) {
                duplicates = true;
                // TODO this could also be generalized with a different color for each duplicate set
                //  by storing the int resId instead of the duplicate boolean in BulkRenameItem
                for(Integer i : v)
                    outputFilenames.get(i).duplicate = true;
            }
        }
        outputAdapter.notifyDataSetChanged();
        ok.setEnabled(!duplicates);
        return duplicates;
    }

    public boolean refreshOutputAdapter() {
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
                    outputFilenames.add(new BulkRenameItem(s.replace(ii,oo)));
                break;
            case REGEX:
                for(String s : inputFilenames)
                    outputFilenames.add(new BulkRenameItem(s.replaceAll(ii,oo)));
                break;
            case GLOB:
                for(String s : inputFilenames)
                    outputFilenames.add(new BulkRenameItem(s.replaceAll(Misc.convertGlobToRegex(ii),oo)));
                break;
        }
        return highlightOutputPanel();
    }

    private BulkRenameDialog(MainActivity activity, BasePathContent baseDir, List<String> inputFilenames) {
        super(activity, R.style.fs_dialog);
        this.inputFilenames = inputFilenames;
        for(String s : inputFilenames)
            outputFilenames.add(new BulkRenameItem(s));
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.bulk_rename_dialog);
        helper = activity.getFileOpsHelper(baseDir.providerType);

        inputPattern = findViewById(R.id.inputRenamePattern);
        outputPattern = findViewById(R.id.outputRenamePattern);
        renameRadioGroup = findViewById(R.id.renameRadioGroup);
        preview = findViewById(R.id.renamePreviewButton);
        ok = findViewById(R.id.renameOkButton);
        pb = findViewById(R.id.renameProgressBar);

        inputAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, inputFilenames);
        lvs[0] = findViewById(R.id.inputRenameLv);
        lvs[0].setAdapter(inputAdapter);
        outputAdapter = new ArrayAdapter<BulkRenameItem>(activity, android.R.layout.simple_list_item_1, outputFilenames){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                BulkRenameItem item = getItem(position);
                View view = super.getView(position, convertView, parent);
                view.findViewById(android.R.id.text1).setBackgroundResource(item.duplicate?android.R.color.holo_red_dark:R.color.transparentCobaltBlue);
                return view;
            }
        };
        lvs[1] = findViewById(R.id.outputRenameLv);
        lvs[1].setAdapter(outputAdapter);

        lvs[0].setOnItemClickListener(getListener(0));
        lvs[1].setOnItemClickListener(getListener(1));
        highlightOutputPanel();

        preview.setOnClickListener(v -> refreshOutputAdapter());

        ok.setOnClickListener(v->{
            if(refreshOutputAdapter()) {
                Toast.makeText(activity, "Duplicates are present in output list, please set a different rename transformation", Toast.LENGTH_SHORT).show();
                return;
            }
            ok.setVisibility(View.GONE);
            preview.setVisibility(View.GONE);
            pb.setVisibility(View.VISIBLE);
            pb.setIndeterminate(false);
            pb.setMax(inputFilenames.size());
            setCancelable(false);
            // TODO at the end, don't dismiss the dialog, instead remove progress bar and restore previous buttons
            new Thread(()-> {
                int itemsToRename = 0;
                List<BasePathContent> failedPaths = new ArrayList<>();
                for(int i=0;i<inputFilenames.size();i++) {
                    int k=i;
                    String i1 = inputFilenames.get(i);
                    String i2 = outputFilenames.get(i).filename;
                    if(i1.equals(i2)) {
                        MainActivity.handler.post(()->pb.setProgress(k));
                        continue;
                    }
                    BasePathContent p1 = baseDir.concat(i1);
                    BasePathContent p2 = baseDir.concat(i2);
                    try {
                        helper.renameFile(p1,p2);
                        itemsToRename++;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        failedPaths.add(p1);
                    }
                    MainActivity.handler.post(()->pb.setProgress(k));
                }
                int renamedItems = itemsToRename;
                activity.runOnUiThread(()->{
                    dismiss();
                    if(failedPaths.isEmpty())
                        Toast.makeText(activity, "Rename completed, renamed "+renamedItems+" items", Toast.LENGTH_SHORT).show();
                    else {
                        StringBuilder errMsg = new StringBuilder();
                        for(BasePathContent p : failedPaths)
                            errMsg.append(p).append("\n");
                        Toast.makeText(activity, "Failed items:\n"+errMsg.toString(), Toast.LENGTH_SHORT).show();
                    }
                    activity.browserPagerAdapter.showDirContent(activity.getCurrentDirCommander().refresh(),activity.browserPager.getCurrentItem(),null);
                });
            }).start();
        });
    }

    public static void createAndShow(MainActivity activity, BasePathContent baseDir, List<String> inputFilenames) {
        if(baseDir.providerType != ProviderType.LOCAL && baseDir.providerType != ProviderType.SFTP) {
            Toast.makeText(activity, "Rename is available only for local and SFTP paths", Toast.LENGTH_SHORT).show();
            return;
        }
        new BulkRenameDialog(activity, baseDir, inputFilenames).show();
    }
}
