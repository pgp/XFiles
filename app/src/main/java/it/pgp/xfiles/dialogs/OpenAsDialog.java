package it.pgp.xfiles.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import it.pgp.xfiles.R;

/**
 * Created by pgp on 26/10/16 (converted inner class to standalone one)
 */
public class OpenAsDialog extends Dialog {

    private static final String[] columnTags = new String[]{"image", "label"};
    private static final int[] columnIds = new int[]{R.id.openAsTypeImage, R.id.openAsTypeName};

    private static final String[] openAsMIMETypes = {"text/plain", "image/*", "audio/*", "video/*", "application/x-compressed", "*/*"};

    private static final String[] openAsLabels = {"Text", "Image", "Audio", "Video", "Archive", "Any"};
    private static final int[] openAsDrawables = {R.drawable.xfiles_text, R.drawable.xfiles_image,
            R.drawable.xfiles_audio, R.drawable.xfiles_video,
            R.drawable.xfiles_archive, R.drawable.xfiles_anytype};

    public OpenAsDialog(final Activity activityContext, final File file) {
        super(activityContext);
        setTitle("Open as");
        setContentView(R.layout.open_as_dialog);
        Button openAsCancelButton = findViewById(R.id.openAsCancelButton);
        openAsCancelButton.setOnClickListener(v -> dismiss());

        ListView openAsListView = findViewById(R.id.openAsListView);

        ArrayList<HashMap<String, String>> openAsListData = new ArrayList<>();
        for (int i = 0; i < openAsLabels.length; i++) {
            HashMap<String, String> h = new HashMap<>();
            h.put(columnTags[0], "" + openAsDrawables[i]);
            h.put(columnTags[1], openAsLabels[i]);
            openAsListData.add(h);
        }

        SimpleAdapter openAsAdapter = new SimpleAdapter(activityContext, openAsListData, R.layout.open_as_dialog_item, columnTags, columnIds);
        openAsListView.setAdapter(openAsAdapter);

        openAsListView.setOnItemClickListener((parent, view, position, id) -> {
            Uri uri = Uri.fromFile(file);
//                Uri uri = FileProvider.getUriForFile(activityContext,
//                        BuildConfig.APPLICATION_ID + ".provider",
//                        file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(uri, openAsMIMETypes[position]);
            //  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activityContext.startActivity(intent);
            dismiss();
        });
    }
}
