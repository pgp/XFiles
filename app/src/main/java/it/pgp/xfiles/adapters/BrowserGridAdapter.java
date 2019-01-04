package it.pgp.xfiles.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;

/**
 * Created by pgp on 01/11/16
 */

public class BrowserGridAdapter extends BrowserAdapter {
    public static class BrowserItemViewHolder {
        TextView name;
        ImageView imageView;
        BrowserItemViewHolder(TextView name, ImageView imageView) {
            this.name = name;
            this.imageView = imageView;
        }
    }

    public BrowserGridAdapter(Context context, List<BrowserItem> objects) {
        super(context, objects);
        containerLayout = R.layout.browser_item_grid;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        BrowserItem item = this.getItem(position);
        TextView name;
        ImageView imageView;

        if(convertView == null){
            convertView = inflater.inflate(containerLayout, null);

            name = convertView.findViewById(R.id.browserItemFilename);
            imageView = convertView.findViewById(R.id.fileTypeImage);

            convertView.setTag(new BrowserItemViewHolder(name,imageView));
        }
        else {
            BrowserItemViewHolder viewHolder = (BrowserItemViewHolder) convertView.getTag();
            name = viewHolder.name;
            imageView = viewHolder.imageView;
        }

        convertView.setBackgroundColor(item.isChecked()? 0x9934B5E4: Color.TRANSPARENT);

        name.setText(item.getFilename());

//        imageView.setImageBitmap(item.isDirectory?dirIV:fileIV);
        imageView.setImageBitmap(getBitmapByExtension(item));
        return convertView;
    }
}
