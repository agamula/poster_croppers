package ua.video.opensvit.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.vov.vitamio.MediaFormat;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.VideoView;
import ua.video.opensvit.R;
import ua.video.opensvit.VideoStreamApp;
import ua.video.opensvit.activities.MainActivity;
import ua.video.opensvit.activities.ShowVideoActivity;
import ua.video.opensvit.activities.VideoViewPlayer;
import ua.video.opensvit.adapters.programs.ProgramsPagerAdapter;
import ua.video.opensvit.api.OpensvitApi;
import ua.video.opensvit.data.GetUrlItem;
import ua.video.opensvit.data.ParcelableArray;
import ua.video.opensvit.data.PlayerInfo;
import ua.video.opensvit.data.constants.LoaderConstants;
import ua.video.opensvit.data.epg.EpgItem;
import ua.video.opensvit.data.epg.ProgramItem;
import ua.video.opensvit.fragments.player.VitamioVideoBaseFragment;
import ua.video.opensvit.fragments.player.VitamioVideoFragment;
import ua.video.opensvit.http.OkHttpClientRunnable;
import ua.video.opensvit.loaders.RunnableLoader;
import ua.video.opensvit.services.NextProgramNotifyService;
import ua.video.opensvit.utils.DateUtils;
import ua.video.opensvit.utils.InteractHandler;
import ua.video.opensvit.utils.WindowUtils;
import ua.video.opensvit.widgets.HorizontalScrollAutoSizeDetectable;
import ua.video.opensvit.widgets.RespondedLayout;

public class ProgramsFragment extends VitamioVideoBaseFragment implements LoaderManager
        .LoaderCallbacks<String>, View.OnClickListener, Handler.Callback {

    private static final String VIDEO_WIDTH = "video_width";
    private static final String VIDEO_HEIGHT = "video_height";

    private static final String START_UT_TAG = "startUT";
    private static final String END_UT_TAG = "endUT";
    private static final String PER_PAGE_TAG = "perPage";
    private static final String PAGE_TAG = "page";

    public static String NEXT_URL;

    public ProgramsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public static ProgramsFragment newInstance(Bundle args) {
        ProgramsFragment programsFragment = new ProgramsFragment();
        programsFragment.setArguments(args);
        return programsFragment;
    }

    public static ProgramsFragment newInstance(String url, int channelId, int serviceId, int
            videoWidth, int videoHeight) {
        ProgramsFragment programsFragment = new ProgramsFragment();
        Bundle bundle = programsFragment.getArgsBundle(url, channelId, serviceId, TimeUnit
                .MILLISECONDS.toSeconds(System.currentTimeMillis()));
        bundle.putInt(VIDEO_WIDTH, videoWidth);
        bundle.putInt(VIDEO_HEIGHT, videoHeight);
        programsFragment.setArguments(bundle);
        return programsFragment;
    }

    @Override
    public int getLayoutId() {
        return R.layout.layout_programs;
    }

    private ProgressBar mLoadVideoProgress, mLoadAdapterProgress;
    private ViewPager mPager;
    private SparseArray<ParcelableArray<ProgramItem>> mPrograms;
    private List<String> mDayNames;
    private int mVideoWidth, mVideoHeight;
    private EpgItem epgItem;
    private int mCountDays;
    private PagerTabStrip mTabStrip;
    private HorizontalScrollAutoSizeDetectable autoSizeDetectable;
    private ProgramItem mFirstProgramItem;
    private GetUrlItem mFirstProgramUrl;

    private static final InteractHandler HANDLER = new InteractHandler(Looper.getMainLooper(),
            new ProgramsFragment());

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ShowVideoActivity activity = (ShowVideoActivity) getActivity();
        activity.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        Bundle args = getArguments();
        mVideoWidth = args.getInt(VIDEO_WIDTH);
        mVideoHeight = args.getInt(VIDEO_HEIGHT);

        autoSizeDetectable = (HorizontalScrollAutoSizeDetectable) view.findViewById(R.id
                .autosizeable);

        mPager = (ViewPager) view.findViewById(R.id.days_pager);
        mTabStrip = (PagerTabStrip) view.findViewById(R.id.day_names);

        mPrograms = new SparseArray<>();

        RespondedLayout respondedLayout = getRespondedLayout();

        if (respondedLayout != null) {
            mLoadVideoProgress = (ProgressBar) view.findViewById(R.id.load_video_program_progress);
            mLoadVideoProgress.setVisibility(View.VISIBLE);

            respondedLayout.setOnLayoutHappenedListener(new RespondedLayout.OnLayoutHappenedListener() {
                @Override
                public void onLayoutHappened(RespondedLayout layout) {
                    final VideoView mVideoView = getVideoView();
                    mVideoView.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);
                    Pair<Integer, Integer> screenSizes = WindowUtils.getScreenSizes();
                    int width = screenSizes.first;
                    int scHeight = screenSizes.second;
                    int height = (int) ((float) (width * mVideoHeight) / mVideoWidth);
                    if (scHeight < height) {
                        height = scHeight;
                        width = (int) ((float) (height * mVideoWidth) / mVideoHeight);
                    }
                    View parentView = (View) mVideoView.getParent();
                    ViewGroup.LayoutParams pars = parentView.getLayoutParams();
                    pars.width = width;
                    pars.height = height;
                    pars = mVideoView.getLayoutParams();
                    pars.width = width;
                    pars.height = height;
                    layout.setReactOnLayout(false);
                    if (mFirstProgramUrl != null) {
                        VideoStreamApp.getInstance().getPlayerInfo().setVideoPath
                                (mFirstProgramUrl.getUrl());
                        onPostViewCreated();
                    }
                }
            });
            respondedLayout.setReactOnLayout(true);
            respondedLayout.requestLayout();
            VideoStreamApp.getInstance().getPlayerInfo().setForceStart(true);
            VideoStreamApp.getInstance().getPlayerInfo().setVideoPath(getPath());
        } else {
            PlayerInfo playerInfo = VideoStreamApp.getInstance().getPlayerInfo();
            playerInfo.setVideoPath(getPath());
            if (playerInfo.isPlaying() && playerInfo.isForceStart()) {
                Activity activity1 = getActivity();
                Bundle extras = new Bundle();
                extras.putString(VideoViewPlayer.URL_TAG, playerInfo.getVideoPath());
                extras.putInt(NextProgramNotifyService.CHANNEL_ID, getChannelId());
                extras.putInt(NextProgramNotifyService.SERVICE_ID, getServiceId());
                extras.putLong(NextProgramNotifyService.TIMESTAMP, getTimestamp());

                Intent intent = new Intent(activity1, VideoViewPlayer.class);
                intent.putExtras(extras);
                startActivity(intent);
                /*
                MainActivity.startFragment(getActivity(), VitamioVideoFragment.newInstance
                        (playerInfo.getVideoPath(), getChannelId(), getServiceId(),
                                getTimestamp(), mVideoWidth, mVideoHeight));*/
                return;
            }
        }

        mLoadAdapterProgress = (ProgressBar) view.findViewById(R.id.load_programs_progress);
        mLoadAdapterProgress.setVisibility(View.VISIBLE);

        Calendar calendar = Calendar.getInstance(DateUtils.getTimeZone());
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);

        mCountDays = getResources().getInteger(R.integer.count_days_back);

        int channelId = getChannelId();
        int serviceId = getServiceId();

        mCountLoaded = 0;

        for (int i = 0; i < mCountDays; i++) {
            long now = calendar.getTimeInMillis();
            long endUt = TimeUnit.MILLISECONDS.toSeconds(now);

            long dayMillis = TimeUnit.DAYS.toMillis(1);

            long startUt = TimeUnit.MILLISECONDS.toSeconds(now - dayMillis + 1);
            int perPage = 0;
            int page = -1;

            args = new Bundle();
            args.putInt(NextProgramNotifyService.CHANNEL_ID, channelId);
            args.putInt(NextProgramNotifyService.SERVICE_ID, serviceId);
            args.putLong(START_UT_TAG, startUt);
            args.putLong(END_UT_TAG, endUt);
            args.putInt(PER_PAGE_TAG, perPage);
            args.putInt(PAGE_TAG, page);

            getLoaderManager().initLoader(LoaderConstants.LOAD_PROGRAMS_LOADER_ID + i, args, this);
            calendar.setTimeInMillis(calendar.getTimeInMillis() - dayMillis);
        }
        mDayNames = new ArrayList<>(mCountDays);
        for (int i = 0; i < mCountDays; i++) {
            mDayNames.add(null);
        }
    }

    @Override
    public void onDestroyView() {
        LoaderManager manager = getLoaderManager();

        for (int loaderId = LoaderConstants.LOAD_PROGRAMS_LOADER_ID; loaderId < LoaderConstants
                .LOAD_PROGRAMS_LOADER_ID + mCountDays; loaderId++) {
            manager.destroyLoader(loaderId);
        }

        super.onDestroyView();
    }

    public void setVideoPath(String videoPath, long timeSeek) {
        //timeSeek = 0;
        PlayerInfo playerInfo = VideoStreamApp.getInstance().getPlayerInfo();
        if (videoPath.equals(mFirstProgramUrl.getUrl())) {
            videoPath = getPath();
        }
        playerInfo.setVideoPath(videoPath);
        VideoView videoView = getVideoView();
        if (videoView != null) {
            Map<String, String> headers = new HashMap<>();
            OkHttpClientRunnable.populateHeaders(headers);
            videoView.setVideoURI(Uri.parse(videoPath), headers);
            seekTo = timeSeek;
            videoView.requestFocus();
            videoView.start();
        } else {
            MainActivity.startFragment(getActivity(), VitamioVideoFragment.newInstance
                    (playerInfo.getVideoPath(), getChannelId(), getServiceId(), getTimestamp(),
                            mVideoWidth, mVideoHeight));
        }
    }

    private long seekTo;

    @Override
    public VideoView getVideoView() {
        return super.getVideoView();
    }

    public ProgressBar getLoadAdapterProgress() {
        return mLoadAdapterProgress;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        super.onPrepared(mediaPlayer);
        if (seekTo != 0) {
            getVideoView().seekTo(seekTo);
            seekTo = 0;
        }
        mLoadVideoProgress.setVisibility(View.GONE);
        SparseArray<MediaFormat> audioFormats = getAudioFormats();
        if (audioFormats.size() > 1) {
            autoSizeDetectable.setVisibility(View.VISIBLE);
            autoSizeDetectable.removeAllViews();
            for (int i = 0; i < audioFormats.size(); i++) {
                TextView tv = new TextView(getActivity());
                tv.setGravity(Gravity.CENTER);
                tv.setText(audioFormats.valueAt(i).getString(MediaFormat.KEY_LANGUAGE));
                tv.setTag(audioFormats.keyAt(i));
                if (i == 0) {
                    tv.setBackgroundDrawable(getResources().getDrawable(R.drawable.left_language_drawable));
                } else if (i == audioFormats.size() - 1) {
                    tv.setBackgroundDrawable(getResources().getDrawable(R.drawable
                            .right_language_drawable));
                } else {
                    tv.setBackgroundDrawable(getResources().getDrawable(R.drawable
                            .center_language_drawable));
                }
                tv.setOnClickListener(this);
                autoSizeDetectable.addView(tv);
            }

            mFirstLanguageSelect = true;
            autoSizeDetectable.getTextViewChildAt(0).performClick();
        } else {
            autoSizeDetectable.setVisibility(View.GONE);
        }
    }

    @Override
    public Loader<String> onCreateLoader(int id, Bundle args) {
        final Loader<String> res;
        if (id >= LoaderConstants.LOAD_PROGRAMS_LOADER_ID && id < LoaderConstants
                .LOAD_PROGRAMS_LOADER_ID + mCountDays) {
            int channelId = args.getInt(NextProgramNotifyService.CHANNEL_ID);
            int serviceId = args.getInt(NextProgramNotifyService.SERVICE_ID);
            long startUt = args.getLong(START_UT_TAG);
            long endUt = args.getLong(END_UT_TAG);
            int perPage = args.getInt(PER_PAGE_TAG);
            int page = args.getInt(PAGE_TAG);

            RunnableLoader loader = new RunnableLoader();
            loader.setRunnable(VideoStreamApp.getInstance().getServerApi().macGetEpgRunnable
                    (channelId, serviceId, startUt, endUt, perPage, page, new LoadResultListener
                            (id - LoaderConstants.LOAD_PROGRAMS_LOADER_ID)));
            res = loader;
        } else {
            switch (id) {
                default:
                    res = null;
            }
        }
        return res;
    }

    private int mCountLoaded;

    @Override
    public void onLoadFinished(Loader<String> loader, String data) {
        int loaderId = loader.getId();
        if (loaderId >= LoaderConstants.LOAD_PROGRAMS_LOADER_ID && loaderId < LoaderConstants
                .LOAD_PROGRAMS_LOADER_ID + mCountDays) {
            epgItem = (EpgItem) VideoStreamApp.getInstance().getTempLoaderObject(loaderId);
            mCountLoaded++;

            int loadPosition = LoaderConstants.LOAD_PROGRAMS_LOADER_ID + mCountDays - loaderId - 1;

            final List<ProgramItem> programItems = epgItem.getUnmodifiablePrograms();
            ProgramItem programItemsArr[] = new ProgramItem[programItems.size()];
            programItems.toArray(programItemsArr);
            mPrograms.put(loadPosition, new ParcelableArray<>(programItemsArr));

            if (loadPosition == mCountDays - 1) {
                mDayNames.set(loadPosition, getResources().getString(R.string.today));
            } else {
                Calendar calendar = Calendar.getInstance(DateUtils.getTimeZone());
                //calendar.set(Calendar.DAY_OF_WEEK, epgItem.getDayOfWeek());
                calendar.setTimeInMillis(calendar.getTimeInMillis() - TimeUnit.DAYS.toMillis
                        (mCountDays - 1 - loadPosition));
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
                Date d = new Date();
                d.setTime(calendar.getTimeInMillis());
                mDayNames.set(loadPosition, sdf.format(d));
            }

            if (mCountLoaded == mCountDays) {
                new AsyncTask<Void, Void, Void>() {

                    private OpensvitApi mApi;
                    private GetUrlItem getUrlItem;
                    private GetUrlItem getUrlItem2;
                    private ProgramItem mSecondItem;

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        ParcelableArray<ProgramItem> firstDayPrograms = mPrograms.valueAt(0);
                        mFirstProgramItem = firstDayPrograms.get(1);
                        mSecondItem = firstDayPrograms.get(2);
                        mApi = VideoStreamApp.getInstance().getServerApi();
                    }

                    @Override
                    protected Void doInBackground(Void... params) {
                        mApi.macGetArchiveUrlRunnable(getChannelId(), mFirstProgramItem.getTimestamp()
                                , new OpensvitApi.ResultListener() {
                            @Override
                            public void onResult(Object res) {
                                if (res != null) {
                                    getUrlItem = (GetUrlItem) res;
                                }
                            }

                            @Override
                            public void onError(String result) {

                            }
                        }).run();
                        mApi.macGetArchiveUrlRunnable(getChannelId(), mSecondItem.getTimestamp()
                                , new OpensvitApi.ResultListener() {
                            @Override
                            public void onResult(Object res) {
                                if (res != null) {
                                    getUrlItem2 = (GetUrlItem) res;
                                }
                            }

                            @Override
                            public void onError(String result) {

                            }
                        }).run();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        mFirstProgramUrl = getUrlItem;
                        if (mFirstProgramUrl != null) {
                            onPostViewCreated();
                        }
                        /*MediaMetadataRetriever mRetriever = new MediaMetadataRetriever
                                (getActivity());
                        try {
                            mRetriever.setDataSource(getUrlItem2.getUrl());
                            String videoWidth = mRetriever.extractMetadata(MediaMetadataRetriever
                                    .METADATA_KEY_VIDEO_WIDTH);
                            String a = videoWidth + "asd";
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            mRetriever.release();
                        }

                        mRetriever = new MediaMetadataRetriever(getActivity());
                        try {
                            mRetriever.setDataSource(mFirstProgramUrl.getUrl());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        setRetriever(mRetriever);*/
                        setAdapter(getUrlItem2);
                    }
                }.execute();
            }
        } else {
            switch (loader.getId()) {
                default:
                    break;
            }
        }
    }

    private void setAdapter(GetUrlItem mSecondUrl) {
        mPager.setAdapter(new ProgramsPagerAdapter(mPrograms, mDayNames, this,
                getChannelId(), mFirstProgramItem, mFirstProgramUrl, mSecondUrl));
        mPager.setCurrentItem(mDayNames.size() - 1);
        mTabStrip.setTabIndicatorColorResource(R.color.color_login);
    }

    @Override
    public void onClick(View v) {
        if (!(v instanceof TextView) || v.getTag() == null || !(v.getTag() instanceof Integer)) {
            return;
        }
        for (int i = 0; i < autoSizeDetectable.getTextViewChildCount(); i++) {
            autoSizeDetectable.getTextViewChildAt(i).setSelected(false);
        }
        v.setSelected(true);
        Integer audioKey = (Integer) v.getTag();
        VideoView videoView = getVideoView();
        videoView.pause();
        long pos = videoView.getCurrentPosition();
        if (mFirstLanguageSelect) {
            videoView.setAudioTrack(audioKey);
            videoView.seekTo(pos);
            videoView.start();
        } else {
            sendSwitchLangMessage(audioKey, pos);
        }
        mFirstLanguageSelect = false;
    }

    private boolean mFirstLanguageSelect;

    private static final int MSG_DELAY_START_TRACK_NEW_LANG = 0;

    private void sendSwitchLangMessage(int audioTrackId, long pos) {
        WeakReference<ProgramsFragment> fragmentWeakReference = new
                WeakReference<>(this);
        Pair<WeakReference<VideoView>, Pair<Integer, Long>> data = new Pair<>(new WeakReference<>
                (getVideoView()), new Pair<>(audioTrackId, pos));
        Pair<WeakReference<?>, Object> msgData = new Pair<WeakReference<?>, Object>
                (fragmentWeakReference, data);
        Message message = HANDLER.obtainMessage(MSG_DELAY_START_TRACK_NEW_LANG, msgData);
        HANDLER.sendMessageDelayed(message, VideoStreamApp.getInstance().getResources()
                .getInteger(R.integer.load_new_lang_delay));
    }

    @Override
    public boolean handleMessage(Message msg) {
        boolean handled;
        switch (msg.what) {
            case MSG_DELAY_START_TRACK_NEW_LANG:
                Pair<WeakReference<VideoView>, Pair<Integer, Long>> data =
                        (Pair<WeakReference<VideoView>,
                                Pair<Integer, Long>>) msg.obj;
                VideoView videoView = data.first.get();
                videoView.setAudioTrack(data.second.first);
                //videoView.seekTo(data.second.second);
                videoView.start();
                handled = true;
                break;
            default:
                handled = false;
        }
        return handled;
    }

    private class LoadResultListener implements OpensvitApi.ResultListener {

        private final int position;

        LoadResultListener(int position) {
            this.position = position;
        }

        @Override
        public void onResult(Object res) {
            if (res != null) {
                VideoStreamApp.getInstance().setTempLoaderObject(LoaderConstants
                        .LOAD_PROGRAMS_LOADER_ID + position, res);
            }
        }

        @Override
        public void onError(String result) {

        }
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {

    }
}
