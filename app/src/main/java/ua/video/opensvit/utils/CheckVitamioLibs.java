package ua.video.opensvit.utils;

import android.content.Context;

import io.vov.vitamio.Vitamio;
import ua.video.opensvit.VideoStreamApp;

public class CheckVitamioLibs {
    private CheckVitamioLibs() {
    }

    public static boolean isInited() {
        Context ctx = VideoStreamApp.getInstance();
        return Vitamio.isInitialized(ctx);
    }

    public static void init() {
        Context ctx = VideoStreamApp.getInstance();
        Vitamio.initialize(ctx, ctx.getResources().getIdentifier("libarm",
                "raw", ctx.getPackageName()));
    }

    public static void init(Context ctx) {
        Vitamio.initialize(ctx, ctx.getResources().getIdentifier("libarm",
                "raw", ctx.getPackageName()));
    }
}
