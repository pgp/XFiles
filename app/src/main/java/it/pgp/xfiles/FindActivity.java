package it.pgp.xfiles;

import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;

import it.pgp.xfiles.adapters.FindResultsAdapter;
import it.pgp.xfiles.dialogs.PropertiesDialog;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.roothelperclient.FindManager;
import it.pgp.xfiles.roothelperclient.reqs.find_rq;
import it.pgp.xfiles.utils.FileSelectFragment;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

public class FindActivity extends EffectActivity implements FileSelectFragment.Callbacks {

    ImageButton dropdownButton;
    View dropdownLayout;
    STATUS currentStatus = STATUS.DOWN;

    public static FindActivity instance; // never null after first initialization

    // Dropdown layout views
    Button startSearch,stopSearch,clearResults;
    TextView basePath;
    Button findPathChooseButton;
    EditText namePattern,contentPattern;
    CheckBox searchOnlyCurrentFolder, caseInsensitiveSearch;

    // TODO add case sensitive,escape,regex options widgets for both name and content pattern fields


    ListView resultsView;

    find_rq findRq;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_find,menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        BrowserItem b = FindResultsAdapter.instance.getItem(info.position);
        switch (item.getItemId()) {
            case R.id.findItemLocate:
                finish();
                MainActivity.mainActivity.goDir(new LocalPathContent(b.getFilename()).getParent(),b.getFilename());
                break;
            case R.id.findItemProperties:
                new PropertiesDialog(this,
                        b.isDirectory? FileMode.DIRECTORY :FileMode.FILE,
                        Collections.singletonList(new LocalPathContent(b.getFilename()))).show();
                break;
        }
        return true;
    }

    void startSearchTask(View unused) {
        findRq = new find_rq(
                basePath.getText().toString().getBytes(),
                namePattern.getText().toString().getBytes(),
                contentPattern.getText().toString().getBytes(),
                new find_rq.FlagBits(searchOnlyCurrentFolder.isChecked()), // only search in subfolders supported currently
                new find_rq.SearchBits(
                        false,
                        false,
                        caseInsensitiveSearch.isChecked(),
                        false,
                        false,
                        false,
                        false,
                        false,
                        false
                        )
        );

        FindResultsAdapter.reset();
        resultsView.setAdapter(FindResultsAdapter.instance);

        Toast.makeText(this,
                (FindManager.find_action(FindManager.FIND_ACTION.START,findRq) == 1)?
                        "Search started":
                        "Unable to start search, another search task still active?",
                Toast.LENGTH_SHORT).show();
    }

    void stopSearchTask(View unused) {
        Toast.makeText(this,
                (FindManager.find_action(FindManager.FIND_ACTION.STOP) == 1) ?
                        "Search cancelled":
                        "Error cancelling search"
                , Toast.LENGTH_SHORT).show();
    }

    // called by UpdaterThread
    public synchronized void toggleSearchButtons(boolean searchIsActive) {
        startSearch.setEnabled(!searchIsActive);
        stopSearch.setEnabled(searchIsActive);
        clearResults.setEnabled(!searchIsActive);
//        if (searchIsActive ||
//                (FindResultsAdapter.instance != null &&
//                        !FindResultsAdapter.instance.isEmpty()))
        if (searchIsActive)
            onSlideViewButtonClick(null); // slide up search options panel
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setActivityIcon(R.drawable.xfiles_find);
        setContentView(R.layout.activity_find);

        dropdownLayout = findViewById(R.id.dropdown_layout);
        dropdownButton = findViewById(R.id.dropdown_button);
        dropdownButton.setImageResource(currentStatus.getDrawable());

        // dropdown layout views
        startSearch = findViewById(R.id.startSearch);
        stopSearch = findViewById(R.id.stopSearch);
        clearResults = findViewById(R.id.clearResults);

        basePath = findViewById(R.id.find_path_textview);
        findPathChooseButton = findViewById(R.id.find_path_choose_button);
        findPathChooseButton.setOnClickListener(this::openDestinationFolderSelector);
        namePattern = findViewById(R.id.find_name_pattern_edittext);
        contentPattern = findViewById(R.id.find_content_pattern_edittext);

        searchOnlyCurrentFolder = findViewById(R.id.find_only_current_folder_checkbox);
        caseInsensitiveSearch = findViewById(R.id.case_insensitive_search_checkbox);

        startSearch.setOnClickListener(this::startSearchTask);
        stopSearch.setOnClickListener(this::stopSearchTask);
        clearResults.setOnClickListener(v -> FindResultsAdapter.reset());

        // depending on search status (active or not) toggle buttons state
        toggleSearchButtons(FindManager.findManagerThreadRef.get()!=null);

        resultsView = findViewById(R.id.results_view);
        FindResultsAdapter.createIfNotExisting();
        resultsView.setAdapter(FindResultsAdapter.instance);
        registerForContextMenu(resultsView);

        dropdownLayout.bringToFront();
        dropdownButton.bringToFront();

        // set current base path textview
        BasePathContent bpc = MainActivity.mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname();
        if (bpc.providerType== ProviderType.LOCAL) basePath.setText(bpc.dir);
        else basePath.setText(Environment.getExternalStorageDirectory().getPath());
    }

    public void onSlideViewButtonClick(View unused) {
        dropdownLayout.animate().y(
                (currentStatus == STATUS.DOWN) ?
                        -dropdownLayout.getHeight() : 0).setDuration(1000).start();
        currentStatus = currentStatus.next();
        dropdownButton.bringToFront();
        dropdownButton.setImageResource(currentStatus.getDrawable());
    }

    @Override
    public void onConfirmSelect(String absolutePath, String fileName) {
        basePath.setText(absolutePath);
    }

    @Override
    public boolean isValid(String absolutePath, String fileName) { return true; }

    // duplicated code from ExtractActivity
    public void openDestinationFolderSelector(View unused) {
        String fragTag = getResources().getString(R.string.tag_fragment_FileSelect);

        // Set up a selector for directory selection.
        FileSelectFragment fsf = FileSelectFragment.newInstance(
                FileSelectFragment.Mode.DirectorySelector,
                R.string.alert_OK,
                R.string.alert_cancel,
                R.string.alert_file_select,
                R.drawable.xfiles_new_app_icon,
                R.drawable.xf_dir_blu,
                R.drawable.xfiles_file_icon,
                basePath.getText().toString());

        fsf.show(getFragmentManager(), fragTag);
    }

    private enum STATUS {
        UP(android.R.drawable.arrow_down_float),
        DOWN(android.R.drawable.arrow_up_float);

        int drawable;

        STATUS(int drawable) {
            this.drawable = drawable;
        }

        public int getDrawable() {
            return drawable;
        }

        STATUS next() {
            return this == UP ? DOWN : UP;
        }
    }
}
