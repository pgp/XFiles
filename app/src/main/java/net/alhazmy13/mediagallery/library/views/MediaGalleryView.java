package net.alhazmy13.mediagallery.library.views;

import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import net.alhazmy13.mediagallery.library.views.adapter.GridImagesAdapter;

import java.util.ArrayList;

import it.pgp.xfiles.R;

/**
 * Created by alhazmy13 on 2/12/17.
 */
public class MediaGalleryView extends RecyclerView {

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    public static final int DEFAULT = 6131;

    private final Activity mActivity;
    private GridImagesAdapter mAdapter;
    private ArrayList<String> mDataset;
    private Drawable mPlaceHolder;
    private OnImageClicked mOnImageClickListener;
    private int mSpanCount;
    private int mOrientation;
    private int mWidth;
    private int mHeight;
    private int defaultSystemUIVisibility = -1;
    private final int fullScreenVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    /**
     * Instantiates a new Media gallery view.
     *
     * @param context the context
     */
    public MediaGalleryView(Activity activity) {
        super(activity);
        this.mActivity = activity;
        init();
    }

    /**
     * Instantiates a new Media gallery view.
     *
     * @param context the context
     * @param attrs   the attrs
     */
    public MediaGalleryView(Activity activity, @Nullable AttributeSet attrs) {
        super(activity, attrs);
        this.mActivity = activity;
        TypedArray a = activity.getTheme().obtainStyledAttributes(attrs, R.styleable.MediaGalleryView, 0, 0);
        mSpanCount = a.getInteger(R.styleable.MediaGalleryView_span_count, 2);
        mPlaceHolder = a.getDrawable(R.styleable.MediaGalleryView_place_holder);
        mOrientation = a.getInt(R.styleable.MediaGalleryView_gallery_orientation, VERTICAL);
        mWidth = a.getDimensionPixelSize(R.styleable.MediaGalleryView_image_width, DEFAULT);
        mHeight = a.getDimensionPixelSize(R.styleable.MediaGalleryView_image_height, DEFAULT);
        if (mPlaceHolder == null) {
            mPlaceHolder = ContextCompat.getDrawable(activity, R.drawable.media_gallery_placeholder);
        }
        init();

    }

    /**
     * Init.
     */
    public void init() {
        mDataset = new ArrayList<>();
        mAdapter = new GridImagesAdapter(mActivity, mDataset, mPlaceHolder);
        setOrientation(mOrientation);
        mAdapter.setImageSize(mWidth, mHeight);
        mAdapter.setOnImageClickListener(pos -> {
            View dcv = mActivity.getWindow().getDecorView();
            int currV = dcv.getSystemUiVisibility();
            defaultSystemUIVisibility = defaultSystemUIVisibility==-1 ? currV : defaultSystemUIVisibility;
            mActivity.getWindow().getDecorView().setSystemUiVisibility(currV==fullScreenVisibility?defaultSystemUIVisibility:fullScreenVisibility);
        });
        setAdapter(mAdapter);
    }

    /**
     * Sets images.
     *
     * @param itemList the item list
     */
    public void setImages(ArrayList<String> itemList) {
        this.mDataset.clear();
        this.mDataset.addAll(itemList);
    }

    /**
     * Notify data set changed.
     */
    public void notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        } else {
            init();
        }
    }

    /**
     * Sets place holder.
     *
     * @param placeHolder the place holder
     */
    public void setPlaceHolder(int placeHolder) {
        this.mPlaceHolder = ContextCompat.getDrawable(mActivity, placeHolder);
        mAdapter.setImgPlaceHolder(mPlaceHolder);
    }

    /**
     * Sets on image click listener.
     *
     * @param onImageClickListener the on image click listener
     */
    public void setOnImageClickListener(OnImageClicked onImageClickListener) {
        this.mOnImageClickListener = onImageClickListener;
        mAdapter.setOnImageClickListener(mOnImageClickListener);
    }

    /**
     * span count in each row.
     *
     * @param spanCount the span count
     */
    public void setSpanCount(int spanCount) {
        this.mSpanCount = spanCount;
        setLayoutManager(new GridLayoutManager(mActivity, mSpanCount));

    }

    /**
     * Sets orientation for image scrolling.
     *
     * @param orientation the orientation
     */
    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
        if (orientation == HORIZONTAL) {
            setLayoutManager(new GridLayoutManager(mActivity, mSpanCount, GridLayoutManager.HORIZONTAL, false));
        } else if (orientation == VERTICAL) {
            setLayoutManager(new GridLayoutManager(mActivity, mSpanCount, GridLayoutManager.VERTICAL, false));

        }
    }


    public void setImageSize(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
        mAdapter.setImageSize(width, height);
    }


    /**
     * The interface On image clicked.
     */
    public interface OnImageClicked {
        /**
         * On image clicked.
         *
         * @param pos the pos
         */
        void onImageClicked(int pos);
    }


}
