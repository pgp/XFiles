package it.pgp.xfiles.adapters;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import it.pgp.xfiles.R;
import it.pgp.xfiles.dialogs.InsertEditLocalFavoritesDialog;
import it.pgp.xfiles.dialogs.InsertEditXreFavoritesDialog;

/**
 * Created by pgp on 04/07/17
 */

public class FavoritesPagerAdapter extends PagerAdapter {

    private final Context context;

//    private final ProviderType[] allowedValues = new ProviderType[]{ProviderType.LOCAL,ProviderType.SFTP};
    private final int[] allowedLayouts = {R.layout.favorites_local,R.layout.favorites_sftp,R.layout.favorites_xfiles_remote,R.layout.favorites_smb};
    private final String[] titles = new String[]{"Local favorites","Sftp favorites","XFiles remote favorites", "SMB favorites"};

    // page one widgets
    private ListView localFavoritesListView; // mapped to "@+id/favorites_local_list"
    private LocalFavoritesAdapter localFavoritesAdapter;
    private InsertEditLocalFavoritesDialog insertLocalFavoritesDialog; // only for insert, edit mode called from within its adapter
    private Button localFavoritesAddButton;

    // page two widgets
    private ListView sftpFavoritesListView; // mapped to "@+id/favorites_sftp_list"
    private SftpFavoritesAdapter sftpFavoritesAdapter;

    // page three widgets
    private ListView xreFavoritesListView; // mapped to "@+id/favorites_xre_list"
    private XreFavoritesAdapter xreFavoritesAdapter;
    private InsertEditXreFavoritesDialog insertXreFavoritesDialog;
    private Button xreFavoritesAddButton;

    // page four widgets
    private ListView smbFavoritesListView; // mapped to "@+id/favorites_smb_list"
    private SmbFavoritesAdapter smbFavoritesAdapter;


    public FavoritesPagerAdapter(final Context context) {
        this.context = context;
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup layout = (ViewGroup) inflater.inflate(allowedLayouts[position], collection, false);

        switch (position) {
            case 0: // local
                localFavoritesListView = layout.findViewById(R.id.favorites_local_list);
                localFavoritesAddButton = layout.findViewById(R.id.favorites_local_add_button);
                localFavoritesAddButton.setOnClickListener(v -> {
                    insertLocalFavoritesDialog = new InsertEditLocalFavoritesDialog(
                            context,localFavoritesAdapter);
                    insertLocalFavoritesDialog.show();
                });
                localFavoritesAdapter = new LocalFavoritesAdapter(context);
                localFavoritesListView.setAdapter(localFavoritesAdapter);
                break;

            case 1: // sftp
                sftpFavoritesListView = layout.findViewById(R.id.favorites_sftp_list);
                sftpFavoritesAdapter = new SftpFavoritesAdapter(context);
                sftpFavoritesListView.setAdapter(sftpFavoritesAdapter);
                sftpFavoritesListView.setEmptyView(layout.findViewById(R.id.favorites_sftp_list_empty_view));
                break;

            case 2: // xfiles_remote
                xreFavoritesListView = layout.findViewById(R.id.favorites_xre_list);
                xreFavoritesAddButton = layout.findViewById(R.id.favorites_xre_add_button);
                xreFavoritesAddButton.setOnClickListener(view -> {
                    insertXreFavoritesDialog = new InsertEditXreFavoritesDialog(
                            context,xreFavoritesAdapter);
                    insertXreFavoritesDialog.show();
                });
                xreFavoritesAdapter = new XreFavoritesAdapter(context);
                xreFavoritesListView.setAdapter(xreFavoritesAdapter);
                break;
            case 3: // smb
                smbFavoritesListView = layout.findViewById(R.id.favorites_smb_list);
                smbFavoritesAdapter = new SmbFavoritesAdapter(context);
                smbFavoritesListView.setAdapter(smbFavoritesAdapter);
                smbFavoritesListView.setEmptyView(layout.findViewById(R.id.favorites_smb_list_empty_view));
                break;
            default:
                Toast.makeText(context,"Position not available in the adapter",Toast.LENGTH_SHORT).show();
                throw new RuntimeException("Position not available in the adapter");
        }

        collection.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return allowedLayouts.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titles[position];
    }
}
