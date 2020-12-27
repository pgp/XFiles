package it.pgp.xfiles.dialogs.compress;

import android.app.Dialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.utils.Pair;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

public class ExtractResultsDialog extends Dialog {
    public ExtractResultsDialog(Context context, List<BasePathContent> srcArchives, List<FileOpsErrorCodes> results, boolean isTest) {
        super(context);
        setTitle((isTest?"Test":"Extract")+" results");
        setContentView(R.layout.extract_results_dialog);
        List<Pair<String,String>> support = new ArrayList<>();
        for(int i=0;i<srcArchives.size();i++) {
            FileOpsErrorCodes res = results.get(i);
            if(res == null) res = FileOpsErrorCodes.OK;
            support.add(new Pair<>(srcArchives.get(i).toString(), res.toString()));
        }
        ArrayAdapter<Pair<String,String>> a = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, support);
        ListView lv = findViewById(R.id.extract_results_view);
        lv.setAdapter(a);
    }
}
