package it.pgp.xfiles.dialogs;

import android.app.Activity;
import android.widget.Button;
import android.widget.EditText;

import it.pgp.xfiles.adapters.BrowserAdapter;
import it.pgp.xfiles.R;

/**
 * Created by pgp on 26/10/16 (converted inner class to standalone one)
 */
public class FilterSelectionDialog extends BaseDialog {
    public FilterSelectionDialog(final Activity activity, final BrowserAdapter browserAdapter, final boolean selectOrDeselect) {
        super(activity);
        setTitle(selectOrDeselect ? "Filter selection" : "Filter deselection");
        setContentView(R.layout.single_filename_dialog);
        setDialogIcon(R.drawable.xfiles_find);
        EditText content = findViewById(R.id.singleFilenameEditText);
        Button okButton = findViewById(R.id.singleFilenameOkButton);
        okButton.setOnClickListener(v -> {
            // TODO parse wildcards (e.g. * for any chars)
            browserAdapter.filterSelection(content.getText().toString(), selectOrDeselect);
            dismiss();
        });
    }
}
