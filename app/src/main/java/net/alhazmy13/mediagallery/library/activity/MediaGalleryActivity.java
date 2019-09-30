package net.alhazmy13.mediagallery.library.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import net.alhazmy13.mediagallery.library.activity.adapter.CustomViewPager;
import net.alhazmy13.mediagallery.library.activity.adapter.HorizontalListAdapters;
import net.alhazmy13.mediagallery.library.activity.adapter.ViewPagerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.service.BaseBackgroundService;
import it.pgp.xfiles.service.visualization.ViewType;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;


/**
 * The type Media gallery activity.
 */
public class MediaGalleryActivity extends BaseActivity implements ViewPager.OnPageChangeListener, HorizontalListAdapters.OnImgClick {
    private CustomViewPager mViewPager;
    private RecyclerView imagesHorizontalList;
    private HorizontalListAdapters hAdapter;
    private RelativeLayout mMainLayout;
    private ImageButton showImageOnLockScreen;

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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if(getIntent().getBooleanExtra("setShowOnLockScreenFlags",false))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getResourceLayoutId() {
        return R.layout.activity_gallery;
    }

    @Override
    protected void onCreateActivity() {
        // init layouts
        initViews();

        mViewPager.setAdapter(new ViewPagerAdapter(this, dataSet, mToolbar, imagesHorizontalList));
        hAdapter = new HorizontalListAdapters(this, dataSet, this,placeHolder);
        imagesHorizontalList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imagesHorizontalList.setAdapter(hAdapter);
        hAdapter.notifyDataSetChanged();
        mViewPager.addOnPageChangeListener(this);
        hAdapter.setSelectedItem(selectedImagePosition);
        mViewPager.setCurrentItem(selectedImagePosition);
    }

    private void initViews() {
        mViewPager = findViewById(R.id.pager);
        imagesHorizontalList = findViewById(R.id.imagesHorizontalList);
        mMainLayout = findViewById(R.id.mainLayout);
        if (backgroundColor != -1){
            mMainLayout.setBackgroundColor(ContextCompat.getColor(this,backgroundColor));
        }
        showImageOnLockScreen = findViewById(R.id.showImageOnLockScreen);
    }

    public void setShowImageOnLockScreen(View unused) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setTitle("Show this gallery on lock screen, if any?");
        bld.setNegativeButton("No", BaseBackgroundService.emptyListener);
        bld.setPositiveButton("Yes", (dialog, which) -> {
            Intent i = new Intent(this, MediaGalleryActivity.class);
            i.putExtras(getIntent().getExtras());
            i.putExtra("setShowOnLockScreenFlags",true);
            Toast.makeText(this, "Gallery is being shown on lock screen now", Toast.LENGTH_SHORT).show();
            finish();
            startActivity(i);
        });
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
