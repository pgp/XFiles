package net.alhazmy13.mediagallery.library.activity.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import net.alhazmy13.mediagallery.library.Utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

import it.pgp.xfiles.R;
import uk.co.senab.photoview.PhotoViewAttacher;


/**
 * The type View pager adapter.
 */
public class ViewPagerAdapter extends PagerAdapter {

    private Activity activity;
    private LayoutInflater mLayoutInflater;
    private ArrayList<String> mDataSet;
    private PhotoViewAttacher mPhotoViewAttacher;
    private boolean isShowing = true;
    private Toolbar toolbar;
    private RecyclerView imagesHorizontalList;
    private ImageView imageView;
    private TextView filename;

    /**
     * Instantiates a new View pager adapter.
     *
     * @param activity             the activity
     * @param dataSet              the images
     * @param toolbar              the toolbar
     * @param imagesHorizontalList the images horizontal list
     */
    public ViewPagerAdapter(Activity activity, ArrayList<String> dataSet, Toolbar toolbar, RecyclerView imagesHorizontalList) {
        this.activity = activity;
        mLayoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mDataSet = dataSet;
        this.toolbar = toolbar;
        this.imagesHorizontalList = imagesHorizontalList;
    }

    @Override
    public int getCount() {
        return mDataSet.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View itemView = mLayoutInflater.inflate(R.layout.pager_item, container, false);
        String o = mDataSet.get(position);
        boolean isImageValid;
        imageView = itemView.findViewById(R.id.iv);
        filename = itemView.findViewById(R.id.pager_item_filename);

        if (new File(o).exists() || Utility.isValidURL(o)) {
            Glide.with(activity)
                    .load(String.valueOf(mDataSet.get(position)))
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            filename.setText(String.valueOf(mDataSet.get(position)));
                            onTap();
                            return false;
                        }
                    }).into(imageView);
            isImageValid = true;
        } else {
            ByteArrayOutputStream stream = Utility.toByteArrayOutputStream(o);
            if (stream != null) {
                Glide.with(activity)
                        .load(stream.toByteArray())
                        .asBitmap()
                        .listener(new RequestListener<byte[], Bitmap>() {
                            @Override
                            public boolean onException(Exception e, byte[] model, Target<Bitmap> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, byte[] model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                filename.setText(String.valueOf(mDataSet.get(position)));
                                onTap();
                                return false;
                            }
                        }).into(imageView);
                isImageValid = true;
            } else {
                throw new RuntimeException("Image at position: " + position + " it's not valid image");
            }

        }
        if (!isImageValid) {
            throw new RuntimeException("Value at position: " + position + " Should be as url string or bitmap object");
        }

        container.addView(itemView);

        return itemView;
    }

    private void onTap() {
        mPhotoViewAttacher = new PhotoViewAttacher(imageView);

        mPhotoViewAttacher.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float x, float y) {
                if (isShowing) {
                    isShowing = false;
                    toolbar.animate().translationY(-toolbar.getBottom()).setInterpolator(new AccelerateInterpolator()).start();
                    imagesHorizontalList.animate().translationY(imagesHorizontalList.getBottom()).setInterpolator(new AccelerateInterpolator()).start();
                    filename.setVisibility(View.GONE);
                } else {
                    isShowing = true;
                    toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator()).start();
                    imagesHorizontalList.animate().translationY(0).setInterpolator(new DecelerateInterpolator()).start();
                    filename.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onOutsidePhotoTap() {

            }
        });
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((RelativeLayout) object);
    }


}
