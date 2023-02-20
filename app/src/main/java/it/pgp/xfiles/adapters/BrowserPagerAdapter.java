package it.pgp.xfiles.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.SortingItem;
import it.pgp.xfiles.SwipeRefreshLayoutChildCanScroll;
import it.pgp.xfiles.adapters.continuousselection.CSCheckboxes;
import it.pgp.xfiles.adapters.continuousselection.ContSelHandlingLayout;
import it.pgp.xfiles.adapters.continuousselection.ContSelListener;
import it.pgp.xfiles.comparators.AdvancedComparator;
import it.pgp.xfiles.comparators.FilenameComparator;
import it.pgp.xfiles.dialogs.CreateFileOrDirectoryDialog;
import it.pgp.xfiles.enums.BrowserViewMode;
import it.pgp.xfiles.enums.ComparatorField;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.exceptions.DirCommanderException;
import it.pgp.xfiles.utils.CheckableSingleExecutor;
import it.pgp.xfiles.utils.DirCommander;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.Pair;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;
import it.pgp.xfiles.utils.popupwindow.PopupWindowUtils;

/**
 * Created by pgp on 03/11/16
 */

public class BrowserPagerAdapter extends PagerAdapter {

    private final Context mContext;
    private final MainActivity mainActivity;

    private static final int ADAPTER_SIZE = 2;

    public final CheckableSingleExecutor[] goDirExecutors;

    private final ViewGroup[] rootLayouts = new ViewGroup[ADAPTER_SIZE];

    // almost every array is initially populated with null objects
    public final DirCommander[] dirCommanders = new DirCommander[ADAPTER_SIZE];
    public final AbsListView[] mainBrowserViews = new AbsListView[ADAPTER_SIZE]; // to be assigned as ListView or GridView (same adapter as support)
    public final BrowserAdapter[] browserAdapters = new BrowserAdapter[ADAPTER_SIZE];
    public final BrowserViewMode[] browserViewModes = new BrowserViewMode[]{BrowserViewMode.LIST,BrowserViewMode.LIST};
    public final TextView[] currentDirectoryTextViews = new TextView[ADAPTER_SIZE];
    public final RelativeLayout[] mainBrowserViewLayouts = new RelativeLayout[ADAPTER_SIZE]; // target container for inflating different browser views (list, grid)
    public final RelativeLayout[] mainBrowserViewLayoutParents = new RelativeLayout[ADAPTER_SIZE];

    public final View[] fastRenameModeViews = new View[ADAPTER_SIZE];


    private final SwipeRefreshLayoutChildCanScroll[] swipeRefreshLayouts = new SwipeRefreshLayoutChildCanScroll[ADAPTER_SIZE];

    private final CSCheckboxes[] csCheckBoxes = new CSCheckboxes[ADAPTER_SIZE];
    private final ContSelListener[] csListeners = new ContSelListener[ADAPTER_SIZE];
    private final ContSelHandlingLayout[] csLayouts = new ContSelHandlingLayout[ADAPTER_SIZE];

    private final LinearLayout[] quickFindModeLayouts = new LinearLayout[ADAPTER_SIZE];

    public void createStandardCommanders() {
        BasePathContent path0, path1;
        path0 = dirCommanders[0]==null?
                new LocalPathContent(Misc.internalStorageDir.getAbsolutePath()):
                dirCommanders[0].getCurrentDirectoryPathname();
        path1 = dirCommanders[1]==null?
                new LocalPathContent("/"):
                dirCommanders[1].getCurrentDirectoryPathname();

        // first commander expected to load an always-accessible directory (fail on first exception)
        try {
            this.dirCommanders[0] = new DirCommander(path0);
            this.dirCommanders[1] = new DirCommander(path1,path0);
        }
        catch (DirCommanderException e) {
            Toast.makeText(mainActivity, "Unable to create or rebuild dir commanders, exiting...", Toast.LENGTH_SHORT).show();
            mainActivity.finishAffinity();
        }
    }

    public BrowserPagerAdapter(Context context, final MainActivity mainActivity) {
        mContext = context;
        this.mainActivity = mainActivity;
        this.goDirExecutors = new CheckableSingleExecutor[]{new CheckableSingleExecutor(context),new CheckableSingleExecutor(context)};
        createStandardCommanders();
    }

    public boolean checkUpdateIntent = false;

    // position: 0 or 1
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup collection, int position) {
        viewPager = (ViewPager)collection;
        return instantiateItem_(BrowserViewMode.LIST, position);
    }

    private Object instantiateItem_(BrowserViewMode browserViewMode, int position) {
        final LayoutInflater inflater = LayoutInflater.from(mContext);

        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.browser_page, viewPager, false);

        initMainViews(inflater, layout, browserViewMode, position);
        viewPager.addView(layout);

        rootLayouts[position] = layout;

        if (checkUpdateIntent) {
            mainActivity.updateFromSelfIntent(mainActivity.getIntent());
            checkUpdateIntent = false;
        }

        return layout;
    }

    // browserPageLayout: the one to perform findviewbyid over
    private void initMainViews(LayoutInflater inflater,
                               @NonNull View browserPageLayout,
                               BrowserViewMode browserViewMode,
                               int position) {
        browserViewModes[position] = browserViewMode;

        final Runnable refreshRunnable = () -> {
            GenericDirWithContent dwc = mainActivity.getCurrentDirCommander().refresh_background();

            if(dwc.errorCode == FileOpsErrorCodes.CURRENT_DIR_NO_LONGER_AVAILABLE) {
                mainActivity.runOnUiThread(()-> {
                    if(dwc.listViewPosition == null) {
                        Toast.makeText(mainActivity, "Current dir was no longer available, unable to go back even to start folder, exiting...", Toast.LENGTH_SHORT).show();
                        mainActivity.finishAffinity();
                    }
                    else Toast.makeText(mainActivity,"Current dir is no longer available, went back of " + dwc.listViewPosition + " positions", Toast.LENGTH_SHORT).show();
                });
            }

            if(dwc.listViewPosition != null)
                mainActivity.runOnUiThread(()->showDirContent(dwc,position,null));
        };

        swipeRefreshLayouts[position] = browserPageLayout.findViewById(R.id.activity_main_swipe_refresh_layout);
        swipeRefreshLayouts[position].setParentActivity(mainActivity);
        swipeRefreshLayouts[position].setOnRefreshListener(() -> {
            if(browserAdapters[position].getSelectedCount() == 0) {
                goDirExecutors[position].submit(refreshRunnable);
            }
            else {
                AlertDialog.Builder bld = new AlertDialog.Builder(mainActivity);
                bld.setTitle("Refreshing will clear active selection");
                bld.setNegativeButton(android.R.string.cancel, (dialog, which) -> swipeRefreshLayouts[position].setRefreshing(false));
                bld.setPositiveButton(android.R.string.ok, (dialog, which) -> goDirExecutors[position].submit(refreshRunnable));
                bld.create().show();
            }
        });

        mainBrowserViewLayouts[position] = browserPageLayout.findViewById(R.id.mainBrowserViewLayout);
        mainBrowserViewLayoutParents[position] = browserPageLayout.findViewById(R.id.mainBrowserViewLayoutParent);

        View targetLayout = inflater.inflate(browserViewModes[position].getLayout(),null);
        mainBrowserViewLayouts[position].addView(targetLayout);

        mainBrowserViews[position] = browserPageLayout.findViewById(browserViewModes[position].getId());

        mainBrowserViews[position].setFastScrollEnabled(true);
        currentDirectoryTextViews[position] = browserPageLayout.findViewById(R.id.currentDirectoryTextView);
        mainActivity.registerForContextMenu(currentDirectoryTextViews[position]);

        showDirContent(dirCommanders[position].refresh(),position,null);

        mainBrowserViews[position].setOnItemClickListener(mainActivity.listViewLevelOICL);
        setLongClickListener(position);
    }

    private void changeMainViews(BrowserViewMode browserViewMode, int position) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        browserViewModes[position] = browserViewMode;

        mainBrowserViewLayouts[position].removeAllViews();

        View targetLayout = inflater.inflate(browserViewModes[position].getLayout(),null);
        mainBrowserViewLayouts[position].addView(targetLayout);

        mainBrowserViews[position] = mainBrowserViewLayouts[position].findViewById(browserViewModes[position].getId());

        mainBrowserViews[position].setFastScrollEnabled(true);

        // avoid refreshing directory, retrieve current dir with content from current browseradapter and dircommander
        showDirContent(
                new GenericDirWithContent(
                        dirCommanders[position].getCurrentDirectoryPathname().dir,
                        browserAdapters[position].objects
                ),
                position,
                null);

        // mainBrowserViews[position].setAdapter(browserAdapters[position]); // already called in showDirContent
        mainBrowserViews[position].setOnItemClickListener(mainActivity.listViewLevelOICL);
        setLongClickListener(position);
    }

    @Override
    public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
        collection.removeView((View) view);
    }

    // number of browser views
    @Override
    public int getCount() {
        return ADAPTER_SIZE;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view==object;
    }

    private ViewPager viewPager; // actually it is the ViewPager instance
    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        rootLayouts[position] = (ViewGroup)object;
    }

    public void changeBrowserViewMode(int position) {
        browserViewModes[position] = browserViewModes[position].next();
        changeMainViews(browserViewModes[position], position);
    }

    public void setLongClickListener(int position) {
        mainBrowserViews[position].setOnItemLongClickListener((parent, view, position1, id) -> {
            if(fastRenameModeViews[position]==null) {
                mainActivity.showPopup(parent, view, position1, id);
                return true;
            }
            else return false; // fast rename mode for the current browser page, allow clipboard context menu to popup
        });
    }

    public void recreateAdapterAndSelectMode(BrowserViewMode m, int position, GenericDirWithContent dirWithContent) {
        boolean[] lastcontselmode = (csCheckBoxes[position] != null)?csCheckBoxes[position].getAsBooleans():new boolean[]{false,false,false};
        CreateFileOrDirectoryDialog.resetCreateMode(browserAdapters[position], mainBrowserViews[position]);
        browserAdapters[position] = m.newAdapter(mainActivity,dirWithContent.content);
        fastRenameModeViews[position] = null;
        setMultiSelectModeLayout(multiSelectModes[position],position);
        int[] resIds = {R.id.toggleSelectMode,R.id.invertSelection,R.id.stickySelection};
        for(int i=0;i<resIds.length;i++) {
            if(lastcontselmode[i]) {
                View v = mainActivity.findViewById(resIds[i]);
                if(v!=null) v.performClick();
            }
        }
        PopupWindowUtils.toggleSoftKeyBoard(mainBrowserViews[position],false);
    }

    public void showDirContent(GenericDirWithContent dirWithContent,
                               int position,
                               @Nullable Object targetFilenameToHighlight) { // with filename comparator

        Collections.sort(dirWithContent.content,new FilenameComparator());

        currentDirectoryTextViews[position].setText(
                dirCommanders[position].getCurrentDirectoryPathname().toString());

        recreateAdapterAndSelectMode(browserViewModes[position],position,dirWithContent);
        mainBrowserViews[position].setAdapter(browserAdapters[position]);
        if (targetFilenameToHighlight != null) {
            if (targetFilenameToHighlight instanceof String) { // reposition listview with FindActivity locate
                int locatedPos = browserAdapters[position].findPositionByFilename((String)targetFilenameToHighlight);
                if (locatedPos < 0)
                    MainActivity.showToast("Unable to find file position in browser adapter");
                else {
                    setSelectionOnAbsListView(mainBrowserViews[position],locatedPos,position);
                }
            }
            else { // reposition listview after delete operation
                Integer locatedPos = (Integer)targetFilenameToHighlight;
                mainBrowserViews[position].setSelection(locatedPos);
            }
        }
        else { // reposition listview when going back in dir navigation
            mainBrowserViews[position].setSelection(dirWithContent.listViewPosition);
        }
        swipeRefreshLayouts[position].setRefreshing(false);
        mainActivity.quickPathsLayout.setVisibility(View.GONE); // close quick paths menu if open, once a showDirContent is complete
    }

    public void showSortedDirContent(GenericDirWithContent dirWithContent, Pair<ComparatorField,Boolean> whichAttribute_reverse, int position) {
        Collections.sort(dirWithContent.content,
                new AdvancedComparator(new SortingItem(whichAttribute_reverse.i, true, whichAttribute_reverse.j)));

        currentDirectoryTextViews[position].setText(
                dirCommanders[position].getCurrentDirectoryPathname().toString());

        recreateAdapterAndSelectMode(browserViewModes[position],position,dirWithContent);
        mainBrowserViews[position].setAdapter(browserAdapters[position]);
    }

    private final LinearLayout.LayoutParams onParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);

    private void togglePaddingToMainBrowserView(int position, boolean enabled) {
        int measuredHeight = 0;
        // measure last item height
        if (enabled) {
            int nItems = browserAdapters[position].getCount();
            if(nItems > 0) {
                View item = browserAdapters[position].getView(nItems-1,null,mainBrowserViews[position]);
                item.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                measuredHeight = item.getMeasuredHeight()*3;
            }
        }

        mainBrowserViews[position].setPadding(0,0,0,measuredHeight);
        mainBrowserViews[position].setClipToPadding(!enabled);
        if (enabled) mainBrowserViews[position].setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
    }

    private void setMultiSelectModeLayout(boolean active, int position) {
        if (mainBrowserViewLayoutParents[position] != null && csLayouts[position] != null)
            mainBrowserViewLayoutParents[position].removeView(csLayouts[position]);

        if (active) {
            // TODO rootLayouts member must become RelativeLayout

            csCheckBoxes[position] = new CSCheckboxes();
            csListeners[position] = browserViewModes[position].buildCSListener(
                    mainBrowserViews[position],
                    browserAdapters[position],
                    browserAdapters[position].objects,
                    csCheckBoxes[position]
            );

            csLayouts[position] = new ContSelHandlingLayout(
                    mainActivity,
                    csListeners[position],
                    csCheckBoxes[position],
                    browserViewModes[position].isFullPadLayout()
            );

            mainBrowserViewLayoutParents[position].addView(csLayouts[position]);
        }

        togglePaddingToMainBrowserView(position,active);

        // reset swipe settings on BrowserViewPager
        if(!active)
            mainActivity.browserPager.swipeDisabled = false;
    }

    private final EditText[] quickFindEditTexts = new EditText[ADAPTER_SIZE];
    private final CheckedTextView[] quickFindIgnoreCases = new CheckedTextView[ADAPTER_SIZE];

    private void setQuickFindModeLayout(boolean active, int position) {
        final LayoutInflater inflater = LayoutInflater.from(mContext);

        // FIXME adjust layouts (inflated target does not fill screen horizontally)
        // TODO test also with visibility GONE instead of dynamic layout params
        quickFindModeLayouts[position] = rootLayouts[position].findViewById(R.id.quickFindModeLayout);
        quickFindModeLayouts[position].removeAllViews();

        if (active) {
            View targetLayout = inflater.inflate(R.layout.quickfind_edittext_layout,null);
            quickFindModeLayouts[position].setLayoutParams(onParams);
            quickFindModeLayouts[position].addView(targetLayout);

            quickFindEditTexts[position] = rootLayouts[position].findViewById(R.id.quickFindEditText);

            quickFindEditTexts[position].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // reload filter results on every typed character
                    browserAdapters[position].filterObjects(s,quickFindIgnoreCases[position].isChecked());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            quickFindIgnoreCases[position] = rootLayouts[position].findViewById(R.id.quickFindIgnoreCase);
            quickFindIgnoreCases[position].setOnClickListener(v -> {
                quickFindIgnoreCases[position].toggle();
                browserAdapters[position].filterObjects(
                        quickFindEditTexts[position].getText(),
                        quickFindIgnoreCases[position].isChecked());
            });
        }
    }

    public final boolean[] multiSelectModes = new boolean[ADAPTER_SIZE];
    public void switchMultiSelectMode(int position) {
        multiSelectModes[position] = !multiSelectModes[position];
        setMultiSelectModeLayout(multiSelectModes[position],position);
    }

    public final boolean[] quickFindModes = new boolean[ADAPTER_SIZE];
    public void switchQuickFindMode(int position) {
        quickFindModes[position] = !quickFindModes[position];
        setQuickFindModeLayout(quickFindModes[position],position);
    }

    // use a method here, instead of inserting intermediate class between AbsListView and (ListView and GridView)
    public void setSelectionOnAbsListView(AbsListView absListView, int targetPos, int viewPagerPos) {
        absListView.setSelection(targetPos);
        MainActivity.handler.postDelayed(()-> Misc.highlightListViewItem(targetPos, mainBrowserViews[viewPagerPos]),250);
    }
}
