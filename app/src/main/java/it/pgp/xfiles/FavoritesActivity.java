package it.pgp.xfiles;

import android.os.Bundle;
import android.support.v4.view.ViewPager;

import it.pgp.xfiles.adapters.FavoritesPagerAdapter;

/**
 * Created by pgp on 06/07/17
 */

public class FavoritesActivity extends EffectActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Favorites Manager");
        setActivityIcon(R.drawable.xfiles_favorites);
        setContentView(R.layout.favorites);

        ViewPager viewPager = findViewById(R.id.favorites_viewpager);
        viewPager.setAdapter(new FavoritesPagerAdapter(this));
    }
}
