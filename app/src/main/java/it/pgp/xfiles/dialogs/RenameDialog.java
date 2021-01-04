package it.pgp.xfiles.dialogs;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.adapters.BrowserAdapter;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 26/10/16 (converted inner class to standalone one)
 */
public class RenameDialog extends ImmersiveModeDialog {

    public RenameDialog(final MainActivity mainActivity, final BasePathContent f) {
        super(mainActivity);
        setContentView(R.layout.single_filename_dialog);
        setTitle("Rename");
        EditText filename = findViewById(R.id.singleFilenameEditText);
        Button ok = findViewById(R.id.singleFilenameOkButton);
        filename.setText(f.getName());

        ok.setOnClickListener(v -> {
            String filename_ = filename.getText().toString();
            BasePathContent ff = f.getParent().concat(filename_);
            if(doRename(mainActivity,f,ff))
                mainActivity.browserPagerAdapter.showDirContent(
                        mainActivity.getCurrentDirCommander().refresh(),
                        mainActivity.browserPager.getCurrentItem(),filename_);

            dismiss();
        });
    }

    public static boolean doRename(MainActivity mainActivity, BasePathContent f, BasePathContent ff) {
        boolean ok = false;

        try {
            ok = MainActivity.currentHelper.renameFile(f,ff);
            Toast.makeText(mainActivity, ok?"Renamed":"Error renaming item", Toast.LENGTH_SHORT).show();
        }
        catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(mainActivity, "Roothelper communication error", Toast.LENGTH_SHORT).show();
        }
        return ok;
    }

    public static void resetRenameMode(int pagerPos, View[] fastModes) {
        View a = fastModes[pagerPos];
        if(a!=null) {
            TextView tv = a.findViewById(R.id.browserItemFilename);
            EditText et = a.findViewById(R.id.browserItemFilename_edit);
            et.setVisibility(View.GONE);
            tv.setVisibility(View.VISIBLE);
            fastModes[pagerPos] = null;
        }
    }

    public static void toggleFastRename(MainActivity mainActivity, int pos, BasePathContent f, boolean status) {
        AbsListView listView = mainActivity.browserPagerAdapter.mainBrowserViews[mainActivity.browserPager.getCurrentItem()];
        BrowserAdapter ba = (BrowserAdapter) listView.getAdapter();
        BrowserItem b = ba.getItem(pos);

        if(status) { // ON
            if(!(listView instanceof ListView))
                throw new RuntimeException("Won't modify layout of GridView");

            View a = Misc.getViewByPosition(pos, listView);
            TextView tv = a.findViewById(R.id.browserItemFilename);
            EditText et = a.findViewById(R.id.browserItemFilename_edit);

            tv.setVisibility(View.GONE);
            et.setText(tv.getText().toString());
            et.setVisibility(View.VISIBLE);
            et.setOnEditorActionListener((v, actionId, event) -> {
                // If the event is a key-down event on the "enter" button
                if ((event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) ||
                        (actionId == EditorInfo.IME_ACTION_DONE)) {
                    // Perform action on key press
                    toggleFastRename(mainActivity,pos,f,false);
                    v.clearFocus();
                    return true;
                }
                return false;
            });
            mainActivity.browserPagerAdapter.fastRenameModeViews[mainActivity.browserPager.getCurrentItem()] = a;
        }
        else { // OFF
            View a = mainActivity.browserPagerAdapter.fastRenameModeViews[mainActivity.browserPager.getCurrentItem()];
            TextView tv = a.findViewById(R.id.browserItemFilename);
            EditText et = a.findViewById(R.id.browserItemFilename_edit);

            if(et!=null) et.setVisibility(View.GONE);
            if(tv!=null) tv.setVisibility(View.VISIBLE);

            if(f!=null && et!=null)
                if(doRename(mainActivity,f,f.getParent().concat(et.getText().toString()))) {
                    b.filename = et.getText().toString();
//                tv.setText(b.filename);
                    ba.notifyDataSetChanged();
                }

            View view = mainActivity.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            mainActivity.browserPagerAdapter.fastRenameModeViews[mainActivity.browserPager.getCurrentItem()] = null;
        }
    }
}
