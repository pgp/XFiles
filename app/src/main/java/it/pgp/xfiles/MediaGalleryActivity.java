package it.pgp.xfiles;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.pgp.xfiles.adapters.GalleryPagerAdapter;
import it.pgp.xfiles.adapters.HorizontalListAdapter;
import it.pgp.xfiles.service.visualization.ViewType;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;


public class MediaGalleryActivity extends Activity implements ViewPager.OnPageChangeListener, HorizontalListAdapter.OnImgClick {
    protected HorizontalScrollView mToolbar;
    protected ArrayList<String> dataSet;
    protected String title;
    @ColorRes
    protected int backgroundColor;
    protected int selectedImagePosition;

    private GalleryViewPager mViewPager;
    private RecyclerView imagesHorizontalList;
    private HorizontalListAdapter hAdapter;
    private RelativeLayout rlParentMain;

    public static final Set<String> allowedImageExtensions = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(".bmp",".gif",".jpg",".png")));

    public static int targetIdx = -1;

    public static ArrayList<String> filterByImageExtensionsAndSaveTargetIdx(BasePathContent currentDir, String targetFilename) {
        ArrayList<String> x = new ArrayList<>();
        if(targetFilename==null) targetIdx = 0;
        int idx = 0;
        for (BrowserItem b : MainActivity.mainActivity.getCurrentBrowserAdapter().objects) {
            String y = b.getFilename();
            int l = y.length();
            if(l < 4) continue;
            if(!b.isDirectory && allowedImageExtensions.contains(y.toLowerCase().substring(l-4))) {
                x.add(currentDir.concat(y).dir);
                if(targetIdx==-1 && y.equals(targetFilename)) targetIdx = idx;
                idx++;
            }
        }
        return x;
    }

    public static ArrayList<String> filterByImageExtensionsOnSelection(BasePathContent currentDir, List<BrowserItem> activeSelection) {
        ArrayList<String> x = new ArrayList<>();
        targetIdx = 0;
        for (BrowserItem b : activeSelection) {
            String y = b.getFilename();
            int l = y.length();
            if(l < 4) continue;
            if(!b.isDirectory && allowedImageExtensions.contains(y.toLowerCase().substring(l-4)))
                x.add(currentDir.concat(y).dir);
        }
        return x;
    }

    @Override
    protected void onDestroy() {
        targetIdx = -1;
        super.onDestroy();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // setShowOnLockScreenFlags:
        // -1 -> show only current image in lockscreen mode (i.e. swipe and horizontal viewpager are disabled)
        //  0 (not present) -> no lockscreen mode
        //  1 -> allow browsing current folder in lockscreen mode (i.e. swipe and horizontal viewpager are enabled)

        int lockScreenMode = getIntent().getIntExtra("setShowOnLockScreenFlags",0);
        if(lockScreenMode != 0)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_gallery);
        if(lockScreenMode != 0) findViewById(R.id.showImageOnLockScreen).setVisibility(View.GONE); // no need to show the related button again when we already are in lockscreen mode

        Intent intent = getIntent();
        if(intent == null || intent.getExtras() == null) return;
        Bundle bundle = intent.getExtras();
        dataSet = bundle.getStringArrayList(MediaGallery.Constants.IMAGES.name());
        title = bundle.getString(MediaGallery.Constants.TITLE.name());
        backgroundColor = bundle.getInt(MediaGallery.Constants.BACKGROUND_COLOR.name(),-1);
        selectedImagePosition = bundle.getInt(MediaGallery.Constants.SELECTED_IMAGE_POSITION.name(),0);

        mToolbar = findViewById(R.id.toolbar_media_gallery);

        // init layouts
        mViewPager = findViewById(R.id.pager);
        imagesHorizontalList = findViewById(R.id.imagesHorizontalList);
        rlParentMain = findViewById(R.id.rl_parent_main);
        if(backgroundColor != -1)
            rlParentMain.setBackgroundColor(getResources().getColor(backgroundColor));

        GalleryPagerAdapter adapter = new GalleryPagerAdapter(this, dataSet, mToolbar, imagesHorizontalList, lockScreenMode == -1);
        if(lockScreenMode == -1) mViewPager.enabled = false;
        mViewPager.addOnPageChangeListener(this);
        mViewPager.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        hAdapter = new HorizontalListAdapter(dataSet, this);
        imagesHorizontalList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imagesHorizontalList.setAdapter(hAdapter);
        if(lockScreenMode == -1) imagesHorizontalList.setVisibility(View.GONE);
        hAdapter.notifyDataSetChanged();
        hAdapter.setSelectedItem(selectedImagePosition);
        mViewPager.setCurrentItem(selectedImagePosition);
    }

    public DialogInterface.OnClickListener getListener(int lockscreenMode) {
        return (dialog, which) -> {
            Intent i = new Intent(this, MediaGalleryActivity.class);
            i.putExtras(getIntent().getExtras());
            i.putExtra("setShowOnLockScreenFlags", lockscreenMode);
            i.putExtra(MediaGallery.Constants.SELECTED_IMAGE_POSITION.name(), mViewPager.getCurrentItem());
            Toast.makeText(this, "Gallery is being shown on lock screen now", Toast.LENGTH_SHORT).show();
            finish();
            startActivity(i);
        };
    }

    public void setShowImageOnLockScreen(View unused) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please grant overlay permission in order to show gallery over lock screen", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setTitle("Show this gallery on lock screen, if any?");
        bld.setNegativeButton(android.R.string.cancel, null);
        bld.setNeutralButton("This folder", getListener(1));
        bld.setPositiveButton("This image only", getListener(-1));
        AlertDialog alertDialog = bld.create();
        alertDialog.getWindow().setType(ViewType.OVERLAY_WINDOW_TYPE);
        alertDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        imagesHorizontalList.smoothScrollToPosition(position);
        hAdapter.setSelectedItem(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onClick(int pos) {
        mViewPager.setCurrentItem(pos, true);
    }


}
