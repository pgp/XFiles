package it.pgp.xfiles.service;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

/**
 * Created by pgp on 21/06/17
 * Generic AsyncTask abstract wrapper, in order to execute arbitrary methods while showing
 * an indeterminate progress dialog
 */

public abstract class IndeterminateAsyncTask extends AsyncTask<Void,Void,Integer> {
    private final Context context;
    private ProgressDialog pd;

    private String successMessage;
    private String errorMessage;
    private String ongoingMessage;

    protected IndeterminateAsyncTask(Context context,
                                     String ongoingMessage,
                                     String successMessage,
                                     String errorMessage) {
        this.context = context;
        this.errorMessage = errorMessage;
        this.successMessage = successMessage;
        this.ongoingMessage = ongoingMessage;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        pd = new ProgressDialog(context);
        pd.setIndeterminate(true);
        pd.setMessage(ongoingMessage);
        pd.show();
    }

    @Override
    protected void onPostExecute(Integer integer) {
        super.onPostExecute(integer);
        pd.dismiss();
        Toast.makeText(context,integer==0?successMessage:errorMessage,Toast.LENGTH_LONG).show();
    }
}
