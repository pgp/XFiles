package net.alhazmy13.mediagallery.library.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;

import net.alhazmy13.mediagallery.library.Constants;

import java.util.ArrayList;


public class MediaGallery {
    private Activity mActivity;
    private ArrayList mDataset;
    private String mTitle;
    private int mSelectedImagePosition;
    @ColorRes
    private int mBackgroundColor;
    @DrawableRes
    private int mPlaceHolder;

    public static MediaGallery Builder(Activity activity, ArrayList<String> imagesURLs) {
        return new MediaGallery(activity, imagesURLs);
    }




    private MediaGallery(Activity activity, ArrayList<String> imagesList) {
        this.mDataset = imagesList;
        this.mActivity = activity;
    }



    public MediaGallery title(String title) {
        this.mTitle = title;
        return this;
    }

    public MediaGallery backgroundColor(@ColorRes int color){
        this.mBackgroundColor = color;
        return this;
    }
    public MediaGallery placeHolder(@DrawableRes int placeholder) {
        this.mPlaceHolder = placeholder;
        return this;
    }

    public MediaGallery selectedImagePosition(int position) {
        this.mSelectedImagePosition = position;
        return this;
    }

    public void show() {
        Intent intent = new Intent(mActivity, MediaGalleryActivity.class);
        Bundle bundle = new Bundle();

        bundle.putStringArrayList(Constants.IMAGES.name(), mDataset);
        bundle.putString(Constants.TITLE.name(), mTitle);
        bundle.putInt(Constants.BACKGROUND_COLOR.name(),mBackgroundColor);
        bundle.putInt(Constants.PLACE_HOLDER.name(),mPlaceHolder);
        bundle.putInt(Constants.SELECTED_IMAGE_POSITION.name(),mSelectedImagePosition);
        intent.putExtras(bundle);
        mActivity.startActivity(intent);
    }
}
