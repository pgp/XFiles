package it.pgp.xfiles.service.visualization;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import it.pgp.xfiles.R;
import it.pgp.xfiles.utils.Pair;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

/**
 * Created by pgp on 10/07/17
 * Overlay with two progress bars (for showing file number and size progress)
 */

public class MovingRibbonTwoBars extends ProgressIndicator {

    public ProgressBar pbOuter;
    public ProgressBar pbInner; // outer progress: current number of files, inner: current size
    public TextView pbSpeed;

    public long lastProgressTime = 0;
    public Pair<Long,Long> lastOuterProgress;

    public MovingRibbonTwoBars(Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        oView = inflater.inflate(R.layout.ribbon_two, null);

        pbOuter = oView.findViewById(R.id.pbOuter);
        pbInner = oView.findViewById(R.id.pbInner);
        pbSpeed = oView.findViewById(R.id.pbSpeed);
        lastProgressTime = System.currentTimeMillis();

        pbOuter.setMax(100);
        pbOuter.setIndeterminate(false);
        pbOuter.setBackgroundColor(0x880000ff);

        pbInner.setMax(100);
        pbInner.setIndeterminate(false);
        pbInner.setBackgroundColor(0x8800ff00);

        oView.setOnTouchListener(this);

        addViewToOverlay(oView, ViewType.CONTAINER.getParams());

        topLeftView = new View(context);
        addViewToOverlay(topLeftView, ViewType.ANCHOR.getParams());
    }

    @Override
    public void setProgress(Pair<Long,Long>... values) {
        pbOuter.setProgress((int) Math.round(values[0].i * 100.0 / values[0].j));
        pbInner.setProgress((int) Math.round(values[1].i * 100.0 / values[1].j));
        if(lastOuterProgress == null) {
            lastProgressTime = System.currentTimeMillis();
            lastOuterProgress = values[0];
            pbSpeed.setText("0 Mbps");
        }
        else {
            long dt = lastProgressTime;
            lastProgressTime = System.currentTimeMillis();
            dt = lastProgressTime - dt;

            long ds = lastOuterProgress.i;
            lastOuterProgress = values[0];
            ds = lastOuterProgress.i - ds;

            double speedMbps = ds/(dt*1000.0);
            pbSpeed.setText(String.format("%.2f Mbps",speedMbps));
        }
    }
}
