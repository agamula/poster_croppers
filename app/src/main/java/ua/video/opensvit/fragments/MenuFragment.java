package ua.video.opensvit.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.squareup.leakcanary.RefWatcher;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.vov.vitamio.MediaMetadataRetriever;
import ua.video.opensvit.R;
import ua.video.opensvit.VideoStreamApp;
import ua.video.opensvit.activities.MainActivity;
import ua.video.opensvit.activities.ShowVideoActivity;
import ua.video.opensvit.adapters.ChannelListAdapter;
import ua.video.opensvit.adapters.ChannelListData;
import ua.video.opensvit.api.OpensvitApi;
import ua.video.opensvit.data.GetUrlItem;
import ua.video.opensvit.data.PlayerInfo;
import ua.video.opensvit.data.channels.Channel;
import ua.video.opensvit.data.constants.LoaderConstants;
import ua.video.opensvit.data.menu.TvMenuInfo;
import ua.video.opensvit.data.menu.TvMenuItem;
import ua.video.opensvit.http.OkHttpClientRunnable;
import ua.video.opensvit.loaders.RunnableLoader;
import ua.video.opensvit.utils.CheckVitamioLibs;
import ua.video.opensvit.utils.ParseUtils;

public class MenuFragment extends Fragment implements LoaderManager.LoaderCallbacks<String> {

    private static final String MENU_INFO_TAG = "menu_info";
    private static final int START_NEW_ACTIVITY = 10;

    public static MenuFragment newInstance(TvMenuInfo menuInfo) {
        VideoStreamApp.getInstance().setMenuInfo(menuInfo);

        MenuFragment fragment = new MenuFragment();
        Bundle args = new Bundle();
        args.putParcelable(MENU_INFO_TAG, menuInfo);
        fragment.setArguments(args);
        return fragment;
    }

    private TvMenuInfo mMenuInfo;
    private ExpandableListView mExpandableListView;
    private View mProgress;
    private WeakReference<MenuFragment> weakFragment;

    private static class InternalHandler extends Handler {

        InternalHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == START_NEW_ACTIVITY) {
                InternalHandlerData data = (InternalHandlerData) msg.obj;
                ProgramsFragment programsFragment = ProgramsFragment.newInstance
                        (data.ip, data.channelId, data.service, data.videoWidth, data.videoHeight);
                MainActivity.startActivity(data.activity, new ShowVideoActivity(),
                        programsFragment.getArguments());
            }
        }
    }

    private static final InternalHandler sInternalHandler = new InternalHandler(Looper.getMainLooper());

    private static class InternalHandlerData {
        public final String ip;
        public final int channelId;
        public final int service;
        public final int videoWidth;
        public final int videoHeight;
        public final Activity activity;

        InternalHandlerData(String ip, int channelId, int service, int videoWidth, int
                videoHeight, Activity activity) {
            this.ip = ip;
            this.channelId = channelId;
            this.service = service;
            this.videoWidth = videoWidth;
            this.videoHeight = videoHeight;
            this.activity = activity;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mProgress = getActivity().findViewById(R.id.progress);
        mProgress.setVisibility(View.VISIBLE);
        mMenuInfo = getArguments().getParcelable(MENU_INFO_TAG);
        mExpandableListView = (ExpandableListView) view.findViewById(R.id.menu_list);

        weakFragment = new WeakReference<>(this);

        Bundle args = new Bundle();
        args.putParcelable(MENU_INFO_TAG, mMenuInfo);
        mListener = new GetIpAndShowEpgListener(weakFragment);

        getLoaderManager().initLoader(LoaderConstants.LOAD_MENU_LOADER_ID, args, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mExpandableListView.setOnChildClickListener(mListener);
    }

    @Override
    public void onPause() {
        mExpandableListView.setOnChildClickListener(null);
        super.onPause();
    }

    @Override
    public Loader<String> onCreateLoader(int id, Bundle args) {
        if (id == LoaderConstants.LOAD_MENU_LOADER_ID) {
            final TvMenuInfo menuInfo = args.getParcelable(MENU_INFO_TAG);
            RunnableLoader loader = new RunnableLoader();
            loader.setRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        VideoStreamApp mApp = VideoStreamApp.getInstance();
                        OpensvitApi api1 = mApp.getServerApi();
                        List<TvMenuItem> tvMenuItems = menuInfo.getUnmodifiableTVItems();
                        List<String> groupsList = new ArrayList<>();
                        for (int i = 0; i < tvMenuItems.size(); i++) {
                            groupsList.add(tvMenuItems.get(i).getName());
                        }

                        List<List<Channel>> channels = api1.macGetChannels(tvMenuItems);

                        mApp.setTempLoaderObject(LoaderConstants.LOAD_MENU_LOADER_ID, new ChannelListData(groupsList,
                                channels));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            return loader;
        } else {
            return null;
        }
    }



    @Override
    public void onLoadFinished(Loader<String> loader, String data) {
        switch (loader.getId()) {
            case LoaderConstants.LOAD_MENU_LOADER_ID:
                MenuFragment fragment = weakFragment.get();
                if (fragment != null) {
                    fragment.mProgress.setVisibility(View.GONE);
                    VideoStreamApp mApp = VideoStreamApp.getInstance();
                    ChannelListData mExpListData = (ChannelListData) mApp.getTempLoaderObject
                            (LoaderConstants.LOAD_MENU_LOADER_ID);
                    ExpandableListAdapter mExpListAdapter = new ChannelListAdapter(mExpListData
                            .groups, mExpListData.channels, mApp.getServerApi(), fragment.getActivity());
                    fragment.mExpandableListView.setAdapter(mExpListAdapter);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {

    }

    private GetIpAndShowEpgListener mListener;

    private static class GetIpAndShowEpgListener implements ExpandableListView.OnChildClickListener,
            OpensvitApi.ResultListener {

        private final WeakReference<MenuFragment> weakFragment;
        private AsyncTask<Void, Void, Void> mPressTask;

        public GetIpAndShowEpgListener(WeakReference<MenuFragment> weakFragment) {
            this.weakFragment = weakFragment;
        }

        private int videoWidth;
        private int videoHeight;

        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            final Channel mChannel = (Channel) parent.getExpandableListAdapter().getChild
                    (groupPosition, childPosition);
            final Channel nextChannel = (Channel) parent.getExpandableListAdapter().getChild
                    (groupPosition, childPosition + 1);
            final MenuFragment fragment = weakFragment.get();
            if (fragment != null) {
                mPressTask = new AsyncTask<Void, Void, Void>() {

                    private WeakReference<ProgressBar> weakProgress;

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        weakProgress = new WeakReference<>((ProgressBar) fragment.getActivity()
                                .findViewById(R.id.progress));
                        weakProgress.get().setVisibility(View.VISIBLE);
                        videoWidth = videoHeight = 0;
                    }

                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            VideoStreamApp app = VideoStreamApp.getInstance();
                            OpensvitApi api1 = app.getServerApi();
                            app.setChannelId(mChannel.getId());
                            OkHttpClientRunnable runnable = api1.macGetChannelIpRunnable(mChannel
                                    .getId());
                            runnable.setOnLoadResultListener(new OkHttpClientRunnable.OnLoadResultListener() {
                                @Override
                                public void onLoadResult(boolean isSuccess, String result) {
                                    if (isSuccess) {
                                        GetUrlItem getUrlItem = ParseUtils.parseGetUrl(result);
                                        Activity activity = fragment.getActivity();
                                        if (!CheckVitamioLibs.isInited()) {
                                            CheckVitamioLibs.init();
                                        }

                                        try {
                                            MediaMetadataRetriever retriever = new MediaMetadataRetriever
                                                    (activity);
                                            if (getUrlItem != null) {
                                                retriever.setDataSource(activity, Uri.parse(getUrlItem
                                                        .getUrl()));
                                            }
                                            videoWidth = Integer.parseInt(retriever.extractMetadata
                                                    (MediaMetadataRetriever
                                                            .METADATA_KEY_VIDEO_WIDTH));
                                            videoHeight = Integer.parseInt(retriever.extractMetadata
                                                    (MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                                            retriever.release();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        onResult(getUrlItem);
                                    } else {
                                        onError(result);
                                    }
                                }
                            });
                            runnable.run();

                            OkHttpClientRunnable runnable1 = api1.macGetChannelIpRunnable(nextChannel
                                    .getId());
                            runnable1.setOnLoadResultListener(new OkHttpClientRunnable.OnLoadResultListener() {
                                @Override
                                public void onLoadResult(boolean isSuccess, String result) {
                                    GetUrlItem urlItem = ParseUtils.parseGetUrl(result);
                                    ProgramsFragment.NEXT_URL = urlItem.getUrl();
                                }
                            });
                            runnable1.run();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        if (weakProgress.get() != null) {
                            weakProgress.get().setVisibility(View.GONE);
                        }
                    }
                };
                mPressTask.execute();

                return true;
            }
            return false;
        }

        public void onDestroyView() {
            if (mPressTask != null && mPressTask.getStatus() != AsyncTask.Status.FINISHED) {
                mPressTask.cancel(true);
                mPressTask = null;
            }
        }

        @Override
        public void onResult(Object res) {
            MenuFragment fragment = weakFragment.get();
            if (fragment != null) {
                if (res == null) {
                    Toast.makeText(fragment.getActivity(), fragment.getString(R.string
                            .load_failed_message), Toast.LENGTH_SHORT).show();
                    return;
                }
                GetUrlItem urlItem = (GetUrlItem) res;
                String ip = urlItem.getUrl();
                PlayerInfo playerInfo = VideoStreamApp.getInstance().getPlayerInfo();
                playerInfo.setPlaying(true);
                playerInfo.setForceStart(true);

                InternalHandlerData data = new InternalHandlerData(ip, VideoStreamApp.getInstance().getChannelId(), fragment.mMenuInfo
                        .getService(), videoWidth, videoHeight, fragment.getActivity());

                Message message = sInternalHandler.obtainMessage(START_NEW_ACTIVITY, data);
                message.sendToTarget();
            }
        }

        @Override
        public void onError(String result) {
            if (weakFragment.get() != null) {
                Toast.makeText(weakFragment.get().getActivity(), result, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        mListener.onDestroyView();
        getLoaderManager().destroyLoader(LoaderConstants.LOAD_MENU_LOADER_ID);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = VideoStreamApp.getInstance().getRefWatcher();
        refWatcher.watch(this);
    }
}
