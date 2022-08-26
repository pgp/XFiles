package it.pgp.xfiles.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import java.util.List;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.utils.Pair;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class RecentPositionsAdapter extends ArrayAdapter<Pair<Integer,BasePathContent>> {

    List<Pair<Integer, BasePathContent>> objects;

    public RecentPositionsAdapter(Context context, List<Pair<Integer, BasePathContent>> objects) {
        super(context, android.R.layout.simple_list_item_1, objects);
        this.objects = objects;
    }

    @Override
    public Pair<Integer, BasePathContent> getItem(int position) {
        return super.getItem(position);
    }

    public static void showPopup(MainActivity activity, boolean beforeOrAfterCurrentIndex, View anchor) {
        List<Pair<Integer, BasePathContent>> objects = activity.getCurrentDirCommander().splitPositions(beforeOrAfterCurrentIndex);
        if(objects.isEmpty()) {
            Toast.makeText(activity, "No items in commander for the specified direction", Toast.LENGTH_SHORT).show();
            return;
        }
        RecentPositionsAdapter a = new RecentPositionsAdapter(activity, objects);
        LayoutInflater layoutInflater = (LayoutInflater) activity.getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = layoutInflater.inflate(R.layout.dircommander_popup_window, null);
        ListView dircommander_positions = popupView.findViewById(R.id.dircommander_positions);
        dircommander_positions.setAdapter(a);
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        PopupWindow window = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dircommander_positions.setOnItemClickListener((parent, view, position, id) -> {
            activity.goDir_async(a.getItem(position).j,null);
            window.dismiss();
        });
        window.setFocusable(true); // in order to dismiss when clicked outside
        window.showAsDropDown(anchor,0 ,0, Gravity.NO_GRAVITY);
    }
}
