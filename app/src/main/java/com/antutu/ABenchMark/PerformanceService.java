package com.antutu.ABenchMark;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.service.quicksettings.TileService;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/**
 * Foreground service that keeps the benchmark-identity process alive.
 */
public final class PerformanceService extends Service {

    static final String ACTION_STOP = "com.antutu.ABenchMark.action.STOP";

    private WindowManager windowManager;
    private View overlay;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
                "spoofclean", "Performance mode", NotificationManager.IMPORTANCE_MIN);
        channel.setDescription("Keeps the performance profile active");
        nm.createNotificationChannel(channel);
        setRunning(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            removeOverlay();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification note = new Notification.Builder(this, "spoofclean")
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Spoof clean")
                .setContentText("Performance mode active")
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= 34) {
            // ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            startForeground(1, note, 0x40000000);
        } else {
            startForeground(1, note);
        }

        try {
            addOverlay();
        } catch (Exception e) {
            // Overlay permission was revoked while we were restarting - stop cleanly.
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private void addOverlay() {
        if (overlay != null) {
            return;
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = 0;
        lp.y = 0;

        overlay = new View(this);
        overlay.setBackgroundColor(0x01000000); // effectively fully transparent
        windowManager.addView(overlay, lp);
    }

    private void removeOverlay() {
        if (overlay != null && windowManager != null) {
            try {
                windowManager.removeView(overlay);
            } catch (Exception ignored) {
                // window already gone
            }
            overlay = null;
        }
    }

    @Override
    public void onDestroy() {
        removeOverlay();
        setRunning(false);
        super.onDestroy();
    }

    private void setRunning(boolean value) {
        SharedPreferences p = getSharedPreferences("spoofclean", MODE_PRIVATE);
        p.edit().putBoolean("running", value).apply();
        // Refresh the quick-control tile: if the control-center / QS panel
        // is currently open showing our tile, the system will rebind to
        // PerformanceTileService and call onStartListening() so the tile
        // re-reads this state (no-op when the panel is closed).
        try {
            TileService.requestListeningState(this,
                    new ComponentName(this, PerformanceTileService.class));
        } catch (Exception ignored) {
            // Only reachable on ROMs without a TileService implementation.
        }
    }
}
