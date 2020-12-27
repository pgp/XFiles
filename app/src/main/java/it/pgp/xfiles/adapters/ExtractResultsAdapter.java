package it.pgp.xfiles.adapters;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

public class ExtractResultsAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater inflater;
    private final List<BasePathContent> srcArchives;
    private final List<FileOpsErrorCodes> results;

    public ExtractResultsAdapter(Context context, List<BasePathContent> srcArchives, List<FileOpsErrorCodes> results) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.srcArchives = srcArchives;
        this.results = results;
    }

    public static class ExtractResultsViewHolder {
        public TextView srcArchive, result;

        ExtractResultsViewHolder(TextView srcArchive, TextView result) {
            this.srcArchive = srcArchive;
            this.result = result;
        }
    }

    @Override
    public int getCount() {
        return srcArchives.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        TextView srcArchive, result;

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.extract_results_item, null);

            srcArchive = convertView.findViewById(R.id.extract_results_srcArchive);
            result = convertView.findViewById(R.id.extract_results_result);

            convertView.setTag(new ExtractResultsAdapter.ExtractResultsViewHolder(srcArchive,result));
        }
        else {
            ExtractResultsAdapter.ExtractResultsViewHolder viewHolder = (ExtractResultsAdapter.ExtractResultsViewHolder) convertView.getTag();
            srcArchive = viewHolder.srcArchive;
            result = viewHolder.result;
        }

        srcArchive.setText(srcArchives.get(position).toString());
        FileOpsErrorCodes res = results.get(position);
        if(res==null) res = FileOpsErrorCodes.OK;
        result.setText(res.toString());
        result.setTextColor(context.getResources().getColor(res == FileOpsErrorCodes.OK ? R.color.green : R.color.red));

        return convertView;
    }
}
