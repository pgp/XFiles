package it.pgp.xfiles.adapters;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tomclaw.imageloader.util.ImageViewHandlers;
import com.tomclaw.imageloader.util.ImageViews;

import java.io.File;
import java.util.ArrayList;

import it.pgp.xfiles.R;
import it.pgp.xfiles.utils.Misc;


public class HorizontalListAdapter extends RecyclerView.Adapter<HorizontalListAdapter.ViewHolder> {
    private final ArrayList<String> mDataset;
    private int mSelectedItem = -1;
    private final OnImgClick mClickListner;

    public HorizontalListAdapter(ArrayList<String> images, OnImgClick imgClick) {
        this.mDataset = images;
        this.mClickListner = imgClick;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_horizontal, null));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        int blue = holder.image.getContext().getResources().getColor(R.color.transparentCobaltBlue);
        String o = mDataset.get(holder.getAdapterPosition());
        if(new File(o).exists() || Misc.isValidURL(o)) {
            ImageViews.fetch(holder.image, "file://"+o, handlers -> {
                ImageViewHandlers.centerCrop(handlers);
                ImageViewHandlers.withPlaceholder(handlers, R.drawable.ic_image);
                ImageViewHandlers.whenError(handlers, R.drawable.ic_image_remove, blue);
            });
        }
        else throw new RuntimeException("Not implemented");
        ColorMatrix matrix = new ColorMatrix();
        if (mSelectedItem != holder.getAdapterPosition()) {
            matrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            holder.image.setColorFilter(filter);
            holder.image.setAlpha(0.5f);
        } else {
            matrix.setSaturation(1);

            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            holder.image.setColorFilter(filter);
            holder.image.setAlpha(1f);
        }
        holder.image.setOnClickListener(v->mClickListner.onClick(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public void setSelectedItem(int position) {
        if (position >= mDataset.size()) return;
        mSelectedItem = position;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView image;
        public TextView filename;

        ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.iv);
            filename = itemView.findViewById(R.id.pager_item_filename);
        }
    }

    public interface OnImgClick {
        void onClick(int pos);
    }
}
