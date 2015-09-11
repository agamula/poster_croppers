package ua.video.opensvit.activities;

import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import io.vov.vitamio.LibsChecker;
import ua.video.opensvit.R;
import ua.video.opensvit.VideoStreamApp;
import ua.video.opensvit.fragments.ProgramsFragment;
import ua.video.opensvit.fragments.player.VitamioVideoFragment;

public class ShowVideoActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private View mDrawerFragmentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!LibsChecker.checkVitamioLibs(this)) {
            return;
        }
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mDrawerFragmentView = findViewById(R.id.drawer_fragment);

        if (savedInstanceState == null) {
            Bundle args = getIntent().getExtras();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    ProgramsFragment.newInstance(args)).commit();
        } else {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    getSupportFragmentManager().findFragmentById(R.id.fragment_container)).commit();
        }

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowTitleEnabled(false);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        setupDrawerToggle();
    }

    private void setupDrawerToggle() {
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);


        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        final ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this,
                mDrawerLayout, R.string.nav_drawer_open_desc, R.string
                .nav_drawer_close_desc) {

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                invalidateOptionsMenu();
            }
        };

        // If the user hasn't 'learned' about the drawer, open it to introduce
        // them to the drawer,
        // per the navigation drawer design guidelines.
        // if (!mUserLearnedDrawer && !mFromSavedInstanceState)
        // {
        mDrawerLayout.closeDrawer(mDrawerFragmentView);
        // }

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.setDrawerIndicatorEnabled(true);
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    public DrawerLayout getDrawerLayout() {
        return mDrawerLayout;
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) instanceof
                VitamioVideoFragment) {
            VideoStreamApp.getInstance().getPlayerInfo().setForceStart(false);
            getSupportActionBar().show();
        }
        super.onBackPressed();
    }
}