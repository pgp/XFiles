package it.pgp.xfiles.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.conflicthandling.ConflictDecision;
import it.pgp.xfiles.enums.conflicthandling.ConflictType;
import it.pgp.xfiles.utils.ProgressConflictHandler;

public class ConflictDialog extends Dialog implements View.OnClickListener {
    private final TextView srcPathTv,destPathTv;
    private EditText newFilename;
    private Button
            srcRename,srcRenameAll,
            destRename,destRenameAll,
            overwrite,overwriteAll,
            skip,skipAll,cancel;

    private Button merge,mergeAll;

    private ImageView srcImage,destImage;

    private final ProgressConflictHandler handler;

    // srcType == cflType
//    public FileConflictDialog(Context context, ConflictType cflType) {
    public ConflictDialog(Context context,
                          ConflictType srcType,
                          String srcPath,
                          ConflictType destType,
                          String destPath,
                          ProgressConflictHandler handler) {
        super(context);
        this.handler = handler;
        setCancelable(false);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        setTitle(srcType.name()+" over "+destType.name()+" conflict");
        setContentView(R.layout.conflict_dialog);

        srcImage = findViewById(R.id.conflictSrcImage);
        destImage = findViewById(R.id.conflictDestImage);
        srcImage.setImageResource(srcType.getImageRes());
        destImage.setImageResource(destType.getImageRes());

        newFilename = findViewById(R.id.conflictNewPathnameEditText);

        srcPathTv = findViewById(R.id.conflictSrcPath);
        destPathTv = findViewById(R.id.conflictDestPath);
        srcPathTv.setText(srcPath);
        destPathTv.setText(destPath);

        srcRename = findViewById(R.id.conflictRenameSrc);
        srcRename.setOnClickListener(this);
        srcRenameAll = findViewById(R.id.conflictAutoRenameSrc);
        srcRenameAll.setOnClickListener(this);
        destRename = findViewById(R.id.conflictRenameDest);
        destRename.setOnClickListener(this);
        destRenameAll = findViewById(R.id.conflictAutoRenameDest);
        destRenameAll.setOnClickListener(this);
        overwrite = findViewById(R.id.conflictOverwrite);
        overwrite.setOnClickListener(this);
        overwriteAll = findViewById(R.id.conflictOverwriteAll);
        overwriteAll.setOnClickListener(this);
        skip = findViewById(R.id.conflictSkip);
        skip.setOnClickListener(this);
        skipAll = findViewById(R.id.conflictSkipAll);
        skipAll.setOnClickListener(this);

        merge = findViewById(R.id.conflictMerge);
        mergeAll = findViewById(R.id.conflictMergeAll);
        if (srcType == ConflictType.DIR && destType == ConflictType.DIR) {
            merge.setOnClickListener(this);
            mergeAll.setOnClickListener(this);
        }
        else {
            merge.setEnabled(false);
            mergeAll.setEnabled(false);
        }

        // just enable only skip, cancel and copy-with-rename (a.k.a. rename-source) option in case of copying a file onto itself
        if (srcPath.equals(destPath)) {
            destRename.setEnabled(false);
            destRenameAll.setEnabled(false);
            overwrite.setEnabled(false);
            overwriteAll.setEnabled(false);
            merge.setEnabled(false);
            mergeAll.setEnabled(false);
        }

        cancel = findViewById(R.id.conflictCancel);
        cancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int res = v.getId();
        handler.lastDecision = ConflictDecision.fromResource(res);
        if (handler.lastDecision == ConflictDecision.CD_REN_SRC ||
                handler.lastDecision == ConflictDecision.CD_REN_DEST) {
            handler.lastNewName = newFilename.getText().toString();
            if (handler.lastNewName.isEmpty()) {
                Toast.makeText(getContext(), "Please insert a valid filename", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        else handler.lastNewName = null; // FIXME should be useless

        dismiss();
        synchronized (ConflictDecision.m) {
            ConflictDecision.m.notifyAll();
        }
    }
}
