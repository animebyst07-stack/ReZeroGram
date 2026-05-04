package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class LogViewerFragment extends BaseFragment {

    private ScrollView scrollView;
    private TextView logsTextView;
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private boolean autoScroll = true;
    private static final int MENU_SHARE = 1;
    private static final int MENU_CLEAR = 2;
    private static final int MENU_SCROLL = 3;
    private static final int REFRESH_INTERVAL_MS = 2000;
    private static final int MAX_LINES = 1500;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Логи приложения");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_SHARE) {
                    shareLog();
                } else if (id == MENU_CLEAR) {
                    FileLog.cleanupLogs();
                    if (logsTextView != null) logsTextView.setText("Логи очищены.");
                    Toast.makeText(getParentActivity(), "Логи очищены", Toast.LENGTH_SHORT).show();
                } else if (id == MENU_SCROLL) {
                    autoScroll = !autoScroll;
                    Toast.makeText(getParentActivity(), autoScroll ? "Автопрокрутка вкл" : "Автопрокрутка выкл", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(MENU_SCROLL, R.drawable.msg_go_down);
        menu.addItem(MENU_SHARE, R.drawable.msg_share);
        menu.addItem(MENU_CLEAR, R.drawable.msg_delete);

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);

        logsTextView = new TextView(context);
        logsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        logsTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        logsTextView.setTypeface(android.graphics.Typeface.MONOSPACE);
        logsTextView.setPadding(
            AndroidUtilities.dp(8), AndroidUtilities.dp(8),
            AndroidUtilities.dp(8), AndroidUtilities.dp(8)
        );
        logsTextView.setTextIsSelectable(true);

        scrollView.addView(logsTextView, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frameLayout.addView(scrollView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        fragmentView = frameLayout;
        loadLogs();
        startAutoRefresh();
        return fragmentView;
    }

    private File getLatestLogFile() {
        try {
            File dir = AndroidUtilities.getLogsDir();
            if (dir == null || !dir.exists()) return null;
            File[] files = dir.listFiles((d, name) -> name.endsWith(".txt") && !name.endsWith("_net.txt") && !name.endsWith("_tonlib.txt"));
            if (files == null || files.length == 0) return null;
            Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            return files[0];
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    private void loadLogs() {
        new Thread(() -> {
            String content = readLogContent();
            AndroidUtilities.runOnUIThread(() -> {
                if (logsTextView != null) {
                    logsTextView.setText(content);
                    if (autoScroll) {
                        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                    }
                }
            });
        }).start();
    }

    private String readLogContent() {
        try {
            File logFile = getLatestLogFile();
            if (logFile == null) return "Лог-файл не найден.\nПуть: " + AndroidUtilities.getLogsDir();
            StringBuilder sb = new StringBuilder();
            sb.append("Файл: ").append(logFile.getName())
              .append("  Размер: ").append(logFile.length() / 1024).append(" KB\n")
              .append("─────────────────────────────────────────\n");

            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), "UTF-8"));
            ArrayList<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
                if (lines.size() > MAX_LINES * 2) {
                    lines.subList(0, MAX_LINES).clear();
                }
            }
            br.close();

            int start = Math.max(0, lines.size() - MAX_LINES);
            if (start > 0) sb.append("[... показаны последние ").append(MAX_LINES).append(" строк из ").append(lines.size()).append(" ...]\n\n");
            for (int i = start; i < lines.size(); i++) {
                sb.append(lines.get(i)).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            return "Ошибка чтения логов: " + e.getMessage();
        }
    }

    private void shareLog() {
        try {
            File logFile = getLatestLogFile();
            if (logFile == null) {
                Toast.makeText(getParentActivity(), "Лог-файл не найден", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            android.net.Uri uri;
            if (Build.VERSION.SDK_INT >= 24) {
                uri = FileProvider.getUriForFile(getParentActivity(),
                    ApplicationLoader.applicationContext.getPackageName() + ".provider", logFile);
            } else {
                uri = android.net.Uri.fromFile(logFile);
            }
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getParentActivity().startActivity(Intent.createChooser(intent, "Поделиться логом"));
        } catch (Exception e) {
            FileLog.e(e);
            Toast.makeText(getParentActivity(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startAutoRefresh() {
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadLogs();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }
}
