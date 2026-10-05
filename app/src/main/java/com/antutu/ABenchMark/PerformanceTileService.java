package com.antutu.ABenchMark;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Quick-control (Quick Settings) tile that toggles performance mode.
 */
public final class PerformanceTileService extends TileService {

    /**
     * Re-sync window: the tile-launched activity flow takes a few seconds
     * (launch -> PiP/flash -> service start), so re-check the truth a
     * little later than the immediate tap feedback.
     */
    private static final long RESYNC_DELAY_MS = 3000L;

    private final Handler resync = new Handler(Looper.getMainLooper());

    @Override
    public void onTileAdded() {
        // User just dragged the tile into their panel - reflect reality.
        updateTile(isRunning());
    }

    @Override
    public void onStartListening() {
        // The control center is showing this tile - make it tell the truth.
        updateTile(isRunning());
    }

    @Override
    public void onDestroy() {
        resync.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onClick() {
        boolean running = isRunning();

        if (running) {

            stopService(new Intent(this, PerformanceService.class));
            updateTile(false);
            resyncLater();
            return;
        }


        boolean overlayOk = Settings.canDrawOverlays(this);
        try {
            openAppForStart();
            updateTile(overlayOk); // optimistic; verified by resync
        } catch (Exception e) {
            // Could not launch the activity from the panel. Best effort:
            // start the service directly - it maintains the mode, though it
            // may not trigger the whitelist on all ROMs.
            if (overlayOk) {
                try {
                    startForegroundService(
                            new Intent(this, PerformanceService.class));
                    updateTile(true);
                } catch (Exception ignored) {
                    updateTile(false);
                }
            } else {
                updateTile(false);
            }
        }
        resyncLater();
    }

    // ----------------------------------------------------------------- tile

    private void updateTile(boolean active) {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(getText(R.string.tile_label));
        tile.setContentDescription(getText(R.string.tile_label));
        if (Build.VERSION.SDK_INT >= 33) {
            // Pre-13 the icon comes from the manifest <service android:icon>.
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_stat));
        }
        if (Build.VERSION.SDK_INT >= 29) {
            if (active) {
                tile.setSubtitle("On");
            } else if (!Settings.canDrawOverlays(this)) {
                tile.setSubtitle("Setup needed");
            } else {
                tile.setSubtitle("Off");
            }
        }
        tile.updateTile();
    }

    private void resyncLater() {
        resync.removeCallbacksAndMessages(null);
        resync.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateTile(isRunning());
            }
        }, RESYNC_DELAY_MS);
    }

    private boolean isRunning() {
        SharedPreferences p = getSharedPreferences("spoofclean", MODE_PRIVATE);
        return p.getBoolean("running", false);
    }

    // -------------------------------------------------------------- helpers

    /**
     * Launch MainActivity (collapsing the panel first) asking it to start
     */
    private void openAppForStart() {
        Intent intent = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_START_FROM_TILE, true);
        if (Build.VERSION.SDK_INT >= 34) {
            PendingIntent pi = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_IMMUTABLE,
                    miniWindowLaunchOptions());
            startActivityAndCollapse(pi);
        } else {
            // Deprecated form, required below API 34 (no options-capable
            // variant exists there; MainActivity falls back to PiP/flash).
            startActivityAndCollapse(intent);
        }
    }

    // ---------------------------------------------------------- mini window


    private Bundle miniWindowLaunchOptions() {
        try {
            ActivityOptions opts = ActivityOptions.makeBasic();
            try {
                ActivityOptions.class
                        .getMethod("setLaunchWindowingMode", int.class)
                        .invoke(opts, 5); // 5 = WINDOWING_MODE_FREEFORM
            } catch (Throwable ignored) {
                // Windowing API unavailable or restricted - fullscreen
                // launch; MainActivity falls back to PiP / flash.
            }
            opts.setLaunchBounds(miniWindowBounds());
            return opts.toBundle();
        } catch (Throwable ignored) {
            return null; // plain launch without options
        }
    }

    /**
     * Center-top mini window bounds, sized from the short screen edge so
     * they stay reasonable in both orientations; the system clamps
     * off-screen or sub-minimum rects on its own.
     */
    private Rect miniWindowBounds() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int shortEdge = Math.min(dm.widthPixels, dm.heightPixels);
        int w = Math.max((int) (shortEdge * 0.55f), dp(280));
        int h = Math.max((int) (shortEdge * 0.42f), dp(230));
        int left = Math.max(0, (dm.widthPixels - w) / 2);
        int top = Math.max(dp(40), (int) (dm.heightPixels * 0.08f));
        return new Rect(left, top, left + w, top + h);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
