package com.antutu.ABenchMark;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Rational;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * "Spoof clean" - minimal benchmark-identity performance unlock.
 */
public final class MainActivity extends Activity implements View.OnClickListener {

    private static final long AUTO_MINIMIZE_DELAY_MS = 5000L;
    /**
     * Tile-launched starts: shorter flash before auto-minimize. Only used
     * when the PiP mini window could not be entered.
     */
    private static final long TILE_MINIMIZE_DELAY_MS = 2500L;

    private static final long TILE_WINDOW_MINIMIZE_DELAY_MS = 5000L;
    /** Grace period to confirm PiP actually engaged before minimizing. */
    private static final long PIP_VERIFY_DELAY_MS = 700L;


    static final String EXTRA_START_FROM_TILE =
            "com.antutu.ABenchMark.extra.START_FROM_TILE";

    private SharedPreferences prefs;
    private TextView statusText;
    private Button startStopButton;
    private Button permButton;
    private Runnable minimizeTask;
    private boolean pendingTileStart;

    private boolean fromTile;
    private boolean pipHandled;

    private ScrollView scroll;
    private View pipView;

    /**
     * While parked in the PiP mini window, watch for the mode being stopped
     * (from the tile or anywhere else) so the mini window closes itself.
     */
    private final SharedPreferences.OnSharedPreferenceChangeListener runningListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences p, String key) {
                    // Stopped from the tile (or anywhere else): close the
                    // floating window we are parked in, if any - a PiP mini
                    // window or a freeform / multi window.
                    if ("running".equals(key) && !p.getBoolean("running", false)
                            && (isInPictureInPictureMode() || isInMultiWindowMode())) {
                        finish();
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("spoofclean", MODE_PRIVATE);
        pendingTileStart = getIntent() != null
                && getIntent().getBooleanExtra(EXTRA_START_FROM_TILE, false);
        fromTile = pendingTileStart;

        scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF0B1220);
        scroll.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(28), dp(64), dp(28), dp(40));
        scroll.addView(content);

        TextView title = new TextView(this);
        title.setText("Spoof clean");
        title.setTextSize(27);
        title.setTextColor(0xFFF1F5F9);
        title.setGravity(Gravity.CENTER);
        content.addView(title, linear(-2, -2, 0, 0, 0, dp(6)));

        TextView subtitle = new TextView(this);
        subtitle.setText("Ulla la la la ae o, ulla la la la...");
        subtitle.setTextSize(13);
        subtitle.setTextColor(0xFF38BDF8);
        subtitle.setGravity(Gravity.CENTER);
        content.addView(subtitle, linear(-2, -2, 0, 0, 0, dp(24)));

        statusText = new TextView(this);
        statusText.setTextSize(16);
        statusText.setGravity(Gravity.CENTER);
        content.addView(statusText, linear(-2, -2, 0, 0, 0, dp(20)));

        startStopButton = new Button(this);
        startStopButton.setTextSize(15);
        startStopButton.setAllCaps(false);
        startStopButton.setTextColor(0xFFE2E8F0);
        startStopButton.setOnClickListener(this);
        content.addView(startStopButton, linear(-2, -2, 0, 0, 0, dp(10)));

        permButton = new Button(this);
        permButton.setText("Grant overlay permission");
        permButton.setTextSize(13);
        permButton.setAllCaps(false);
        permButton.setTextColor(0xFFFBBF24);
        permButton.setOnClickListener(this);
        content.addView(permButton, linear(-2, -2, 0, 0, 0, dp(24)));

        TextView info = new TextView(this);
        info.setTextSize(11.5f);
        info.setTextColor(0xFF8EA0B5);
        info.setLineSpacing(dp(2), 1.0f);
        info.setGravity(Gravity.CENTER);
        info.setText(
                "Runs under the AnTuTu benchmark package identity so Vivo / iQOO firmware "
                        + "unlocks full performance by its benchmark performance whitelist. \n\n"
                        + "Tip: add 'Spoof Mode' in your control center, toggle from the 'Spoof Mode' quick-control tile.\n\n"
                        + "Note: cannot be installed alongside the real AnTuTu app (identical "
                        + "package name).");
        content.addView(info, linear(-2, -2, 0, dp(8), 0, 0));

        // Compact layout shown while parked in the PiP mini window.
        FrameLayout pip = new FrameLayout(this);
        pip.setBackgroundColor(0xFF0B1220);
        TextView pipText = new TextView(this);
        pipText.setText("\u25cf TURNING ON..");
        pipText.setTextSize(14);
        pipText.setTextColor(0xFF4ADE80);
        pipText.setGravity(Gravity.CENTER);
        pip.addView(pipText, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        pipView = pip;
        pipView.setVisibility(View.GONE);

        FrameLayout root = new FrameLayout(this);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        root.addView(pipView, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        // Android 13+: ask once so the foreground-service notification is visible.
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra(EXTRA_START_FROM_TILE, false)) {
            fromTile = true;
            pendingTileStart = true;
            if (!isInPictureInPictureMode()) {
                pipHandled = false;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        prefs.registerOnSharedPreferenceChangeListener(runningListener);
    }

    @Override
    protected void onStop() {
        prefs.unregisterOnSharedPreferenceChangeListener(runningListener);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (pendingTileStart && Settings.canDrawOverlays(this)) {
            pendingTileStart = false;
            if (!prefs.getBoolean("running", false)) {
                startForegroundService(new Intent(this, PerformanceService.class));

                scheduleMinimize(fromTile
                        ? TILE_WINDOW_MINIMIZE_DELAY_MS
                        : AUTO_MINIMIZE_DELAY_MS);
                Toast.makeText(this, "Performance mode started",
                        Toast.LENGTH_SHORT).show();
                startStopButton.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateUi();
                    }
                }, 300);
            }
        }
        updateUi();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && fromTile && !pipHandled && !isInMultiWindowMode()
                && Settings.canDrawOverlays(this)) {
            attemptPip();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (isInPictureInPictureMode) {
            scheduleMinimize(TILE_WINDOW_MINIMIZE_DELAY_MS);
            scroll.setVisibility(View.GONE);
            pipView.setVisibility(View.VISIBLE);
        } else {
            scroll.setVisibility(View.VISIBLE);
            pipView.setVisibility(View.GONE);
        }
    }


    private void attemptPip() {
        pipHandled = true;
        if (!isPictureInPictureSupported()) {
            scheduleMinimize(TILE_MINIMIZE_DELAY_MS);
            return;
        }
        try {
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(1, 2))
                    .build();
            // Called as a statement: compatible regardless of the exact
            // return-type across API levels; success is verified below.
            enterPictureInPictureMode(params);
        } catch (Exception e) {
            // ROM refused (e.g. top-resumed check) - fall through to the
            // verify step, which will minimize instead.
        }
        startStopButton.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isInPictureInPictureMode()) {
                    scheduleMinimize(TILE_MINIMIZE_DELAY_MS);
                }
            }
        }, PIP_VERIFY_DELAY_MS);
    }


    private boolean isPictureInPictureSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false;
        }
        try {
            return getPackageManager()
                    .hasSystemFeature("android.software.picture_in_picture");
        } catch (Throwable t) {
            return false;
        }
    }

    private void updateUi() {
        boolean overlayOk = Settings.canDrawOverlays(this);
        boolean running = prefs.getBoolean("running", false);

        if (!overlayOk) {
            permButton.setVisibility(View.VISIBLE);
            statusText.setText("Overlay permission required");
            statusText.setTextColor(0xFFFBBF24);
            startStopButton.setText("Grant permission");
        } else {
            permButton.setVisibility(View.GONE);
            if (running) {
                if (minimizeTask != null) {
                    statusText.setText("\u25cf Starting \u2014 minimizing in 5 s\u2026");
                } else {
                    statusText.setText("\u25cf Performance mode active");
                }
                statusText.setTextColor(0xFF4ADE80);
                startStopButton.setText("Stop");
            } else {
                statusText.setText("\u25cb Idle");
                statusText.setTextColor(0xFF94A3B8);
                startStopButton.setText("Start performance mode");
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == permButton || !Settings.canDrawOverlays(this)) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                Toast.makeText(this,
                        "Open 'Display over other apps' in Settings to allow this app",
                        Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (v == startStopButton) {
            Intent intent = new Intent(this, PerformanceService.class);
            if (prefs.getBoolean("running", false)) {
                intent.setAction(PerformanceService.ACTION_STOP);
                startService(intent);
                cancelMinimize();
                pendingTileStart = false; // explicit Stop cancels the tile request
            } else {
                startForegroundService(intent);
                scheduleMinimize(AUTO_MINIMIZE_DELAY_MS);
            }
            startStopButton.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateUi();
                }
            }, 300);
        }
    }

    private void scheduleMinimize(long delayMs) {
        cancelMinimize();
        final long giveUpAt = System.currentTimeMillis() + delayMs + 10000L;
        minimizeTask = new Runnable() {
            @Override
            public void run() {
                if (!prefs.getBoolean("running", false)
                        && System.currentTimeMillis() < giveUpAt) {
                    // Service not up yet - re-check shortly.
                    startStopButton.postDelayed(this, 500L);
                    return;
                }
                minimizeTask = null;
                moveTaskToBack(true);
                Toast.makeText(MainActivity.this,
                        "Enabled \u2014 Spoof performance mode running",
                        Toast.LENGTH_SHORT).show();
                updateUi();
            }
        };
        startStopButton.postDelayed(minimizeTask, delayMs);
    }

    private void cancelMinimize() {
        if (minimizeTask != null) {
            startStopButton.removeCallbacks(minimizeTask);
            minimizeTask = null;
        }
    }

    @Override
    protected void onDestroy() {
        cancelMinimize();
        super.onDestroy();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private LinearLayout.LayoutParams linear(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }
}
