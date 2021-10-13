package it.pgp.xfiles;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

import it.pgp.xfiles.adapters.FindResultsAdapter;
import it.pgp.xfiles.dialogs.PropertiesDialog;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.roothelperclient.FindInArchiveThread;
import it.pgp.xfiles.roothelperclient.FindManager;
import it.pgp.xfiles.roothelperclient.reqs.find_rq;
import it.pgp.xfiles.utils.ArrayTextView;
import it.pgp.xfiles.utils.FileSelectFragment;
import it.pgp.xfiles.utils.pathcontent.ArchivePathContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;
import it.pgp.xfiles.utils.popupwindow.PopupWindowUtils;

public class FindActivity extends EffectActivity implements FileSelectFragment.Callbacks {

    ImageButton dropdownButton;
    View dropdownLayout;
    STATUS currentStatus = STATUS.DOWN;

    public static FindActivity instance; // never null after first initialization

    // Dropdown layout views
    Button startSearch,stopSearch,clearResults;

    ArrayTextView basePathTextView;
    ImageView pathTypeImageView;

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
        BasePathContent bpc = FindResultsAdapter.instance.basePaths.get(0).getCopy();
        // by construction, if one is local, all ones are local (archive path must be a single one)
        // and then we ignore it at all, since for local search, result paths are returned as full paths
        if(bpc instanceof LocalPathContent)
            bpc = new LocalPathContent(b.getFilename());
        else if(bpc instanceof ArchivePathContent) {
            // ensure we are starting from archive's root before concat,
            // since find thread returns paths w.r.t. archive's root
            bpc.dir = "";
            bpc = bpc.concat(b.getFilename());
        }
        switch (item.getItemId()) {
            case R.id.findItemLocate:
                finish();
                MainActivity.mainActivity.goDir(
                        bpc.getParent(),
                        MainActivity.mainActivity.browserPager.getCurrentItem(),
                        b.getFilename());
                break;
            case R.id.findItemProperties:
                new PropertiesDialog(this,
                        b.isDirectory ? FileMode.DIRECTORY : FileMode.FILE,
                        Collections.singletonList(bpc)).show();
                break;
        }
        return true;
    }

    void startSearchTask(View unused) {
        FindResultsAdapter.reset(basePathTextView.getTexts());
        resultsView.setAdapter(FindResultsAdapter.instance);
        if(basePathTextView.getTexts().get(0) instanceof ArchivePathContent) startSearchTaskArchive();
        else startSearchTaskLocal();
    }

    void startSearchTaskLocal() {
        findRq = new find_rq(
                basePathTextView.getMultipleLocalPathsAsStrings(),
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

        Toast.makeText(this,
                (FindManager.find_action(FindManager.FIND_ACTION.START,findRq) == 1)?
                        "Search started":
                        "Unable to start search, another search task still active?",
                Toast.LENGTH_SHORT).show();
    }

    void startSearchTaskArchive() {
        new FindInArchiveThread(
                (ArchivePathContent) basePathTextView.getTexts().get(0),
                namePattern.getText().toString(),
                !searchOnlyCurrentFolder.isChecked(),
                caseInsensitiveSearch.isChecked()
        ).start();
    }

    void stopSearchTask(View unused) {
        Toast.makeText(this,
                (FindManager.find_action(FindManager.FIND_ACTION.STOP) == 1) ?
                        "Search cancelled":
                        "Error cancelling search"
                , Toast.LENGTH_SHORT).show();
    }

    // called by find threads
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

        basePathTextView = findViewById(R.id.find_path_textview);
        basePathTextView.setTexts((List<BasePathContent>) getIntent().getSerializableExtra("paths"));
        pathTypeImageView = findViewById(R.id.find_path_type);
        findPathChooseButton = findViewById(R.id.find_path_choose_button);
        findPathChooseButton.setOnClickListener(this::openDestinationFolderSelector);
        namePattern = findViewById(R.id.find_name_pattern_edittext);
        contentPattern = findViewById(R.id.find_content_pattern_edittext);

        searchOnlyCurrentFolder = findViewById(R.id.find_only_current_folder_checkbox);
        caseInsensitiveSearch = findViewById(R.id.case_insensitive_search_checkbox);

        startSearch.setOnClickListener(this::startSearchTask);
        stopSearch.setOnClickListener(this::stopSearchTask);
        clearResults.setOnClickListener(v -> FindResultsAdapter.reset(basePathTextView.getTexts()));

        // depending on search status (active or not) toggle buttons state
        toggleSearchButtons(FindManager.findManagerThreadRef.get()!=null);

        resultsView = findViewById(R.id.results_view);
        FindResultsAdapter.createIfNotExisting();
        resultsView.setAdapter(FindResultsAdapter.instance);
        registerForContextMenu(resultsView);

        dropdownLayout.bringToFront();
        dropdownButton.bringToFront();

        // set current base path textview
        if(basePathTextView.getTexts().size()>1) pathTypeImageView.setImageResource(R.drawable.xf_dir_blu);
        else {
            BasePathContent bpc = basePathTextView.getTexts().get(0);
            if (bpc.providerType == ProviderType.LOCAL_WITHIN_ARCHIVE)
                pathTypeImageView.setImageResource(R.drawable.xfiles_archive);
            else
                pathTypeImageView.setImageResource(R.drawable.xf_dir_blu);
        }
    }

    public void onSlideViewButtonClick(View unused) {
        dropdownLayout.animate().y(
                (currentStatus == STATUS.DOWN) ?
                        -dropdownLayout.getHeight() : 0).setDuration(1000).start();
        currentStatus = currentStatus.next();
        dropdownButton.bringToFront();
        dropdownButton.setImageResource(currentStatus.getDrawable());
        PopupWindowUtils.toggleSoftKeyBoard(dropdownButton, false);
    }

    @Override
    public void onConfirmSelect(String absolutePath, String fileName) {
        basePathTextView.setText(new LocalPathContent(absolutePath));
        pathTypeImageView.setImageResource(R.drawable.xf_dir_blu);
    }

    @Override
    public boolean isValid(String absolutePath, String fileName) { return true; }

    // duplicated code from ExtractActivity
    public void openDestinationFolderSelector(View unused) {
        String fragTag = getResources().getString(R.string.tag_fragment_FileSelect);

        BasePathContent bpc = basePathTextView.getTexts().get(0);

        // Set up a selector for directory selection.
        FileSelectFragment fsf = FileSelectFragment.newInstance(
                FileSelectFragment.Mode.DirectorySelector,
                android.R.string.ok,
                android.R.string.cancel,
                R.string.alert_file_select,
                R.drawable.xfiles_new_app_icon,
                R.drawable.xf_dir_blu,
                R.drawable.xfiles_file_icon,
                bpc instanceof ArchivePathContent ? bpc.getParent().dir : bpc.dir);

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
