package net.alhazmy13.mediagallery.library.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;

import net.alhazmy13.mediagallery.library.Constants;
import java.util.ArrayList;

import it.pgp.xfiles.R;


abstract class BaseActivity extends Activity {
    protected Toolbar mToolbar;
    protected ArrayList<String> dataSet;
    protected String title;
    @ColorRes
    protected int backgroundColor;
    @DrawableRes
    protected int placeHolder;
    protected int selectedImagePosition;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResourceLayoutId());
        initBase();
        onCreateActivity();
    }

    private void initBase() {
        initBaseValues();
        initBaseViews();

    }

    protected void initBaseValues() {
        Intent intent = getIntent();
        if(intent == null || intent.getExtras() == null) return;
        Bundle bundle = intent.getExtras();
        dataSet = bundle.getStringArrayList(Constants.IMAGES.name());
        title = bundle.getString(Constants.TITLE.name());
        backgroundColor = bundle.getInt(Constants.BACKGROUND_COLOR.name(),-1);
        placeHolder = bundle.getInt(Constants.PLACE_HOLDER.name(),-1);
        selectedImagePosition = bundle.getInt(Constants.SELECTED_IMAGE_POSITION.name(),0);
    }

    private void initBaseViews() {
        mToolbar = findViewById(R.id.toolbar_media_gallery);
    }


    protected abstract int getResourceLayoutId();

    protected abstract void onCreateActivity();
}
