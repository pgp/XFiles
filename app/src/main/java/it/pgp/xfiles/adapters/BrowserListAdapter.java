package it.pgp.xfiles.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.R;

/**
 * Created by pgp on 01/11/16
 */

public class BrowserListAdapter extends BrowserAdapter {
    public BrowserListAdapter(Context context, List<BrowserItem> objects) {
        super(context, objects);
        containerLayout = R.layout.browser_item;
    }

    public static class BrowserItemViewHolder {
        TextView name,size,date;
        ImageView imageView;

        BrowserItemViewHolder(TextView name,TextView size, TextView date, ImageView imageView) {
            this.name = name;
            this.size = size;
            this.date = date;
            this.imageView = imageView;
        }
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        BrowserItem item = this.getItem(position);
        TextView name,size,date;
        ImageView imageView;

        if(convertView == null){
            convertView = inflater.inflate(containerLayout, null);

            name = convertView.findViewById(R.id.browserItemFilename);
            size = convertView.findViewById(R.id.browserItemFileSize);
            date = convertView.findViewById(R.id.browserItemFileDate);
            imageView = convertView.findViewById(R.id.fileTypeImage);

            convertView.setTag(new BrowserItemViewHolder(name,size,date,imageView));
        }
        else {
            BrowserItemViewHolder viewHolder = (BrowserItemViewHolder) convertView.getTag();
            name = viewHolder.name;
            size = viewHolder.size;
            date = viewHolder.date;
            imageView = viewHolder.imageView;
        }

        convertView.setBackgroundColor(item.isChecked()? 0x9934B5E4: Color.TRANSPARENT);

        name.setText(item.getFilename());
        size.setText(""+item.size);
        date.setText(""+item.date); // TODO DateFormat

//        imageView.setImageBitmap(item.isDirectory?dirIV:fileIV);
        imageView.setImageBitmap(getBitmapByExtension(item));

        return convertView;
    }


}
