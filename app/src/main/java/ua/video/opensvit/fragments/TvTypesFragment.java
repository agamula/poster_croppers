package ua.video.opensvit.fragments;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.squareup.leakcanary.RefWatcher;

import java.io.IOException;

import ua.video.opensvit.R;
import ua.video.opensvit.VideoStreamApp;
import ua.video.opensvit.activities.MainActivity;
import ua.video.opensvit.api.OpensvitApi;
import ua.video.opensvit.data.authorization.AuthorizationInfoBase;
import ua.video.opensvit.data.menu.TvMenuInfo;

public class TvTypesFragment extends ListFragment implements OpensvitApi.ResultListener {

    public TvTypesFragment() {
    }

    public static TvTypesFragment newInstance(AuthorizationInfoBase authorizationInfo) {
        TvTypesFragment fragment = new TvTypesFragment();
        Bundle args = new Bundle();
        args.putParcelable(MainFragment.AUTHORIZATION_INFO_TAG, authorizationInfo);
        fragment.setArguments(args);
        return fragment;
    }

    private AuthorizationInfoBase mAuthorizationInfo;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuthorizationInfo = getArguments().getParcelable(MainFragment.AUTHORIZATION_INFO_TAG);
        String tvTypes[] = getResources().getStringArray(R.array.tv_types);
        getListView().setDivider(new ColorDrawable());
        setListAdapter(new ArrayAdapter<>(getActivity(), R.layout.fragment_tv_type_item, R
                .id.tv_type_text, tvTypes));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        OpensvitApi api1 = VideoStreamApp.getInstance().getServerApi();
        try {
            switch (position) {
                case 0:
                    api1.macIpTvMenu(this, this);
                    return;
                case 1:
                    api1.macVodMenu(this, this);
                    return;
                case 2:
                    MainActivity.startFragment(getActivity(), new AboutFragment());
                    return;
            }
            String errMsg = getString(R.string.unknown_service);
            Toast.makeText(getActivity(), errMsg, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResult(Object res) {
        TvMenuInfo info = (TvMenuInfo) res;
        String errMsg = info.getError();
        if (errMsg != null) {
            Toast.makeText(getActivity(), errMsg, Toast.LENGTH_SHORT).show();
        } else {
            MainActivity.startFragment(getActivity(), MenuFragment.newInstance(info));
        }
    }

    @Override
    public void onError(String result) {
        Toast.makeText(getActivity(), result, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = VideoStreamApp.getInstance().getRefWatcher();
        refWatcher.watch(this);
    }
}
