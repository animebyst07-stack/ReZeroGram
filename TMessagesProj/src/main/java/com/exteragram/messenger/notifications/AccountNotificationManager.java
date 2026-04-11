package com.exteragram.messenger.notifications;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.UserConfig;

public class AccountNotificationManager {

    private static final String TAG = "AccountNotifManager";
    private static final String PREFS_NAME = "zerogram_account_notif";
    private static final String KEY_PREFIX = "notif_enabled_";

    private static volatile AccountNotificationManager instance;
    private SharedPreferences prefs;

    private AccountNotificationManager() {
    }

    public static AccountNotificationManager getInstance() {
        if (instance == null) {
            synchronized (AccountNotificationManager.class) {
                if (instance == null) {
                    instance = new AccountNotificationManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        FileLog.d(TAG + " initialized");
    }

    public boolean isNotificationsEnabled(long userId) {
        if (prefs == null) return true;
        return prefs.getBoolean(KEY_PREFIX + userId, true);
    }

    public void setNotificationsEnabled(int accountIndex, long userId, boolean enabled) {
        if (prefs == null) return;
        prefs.edit().putBoolean(KEY_PREFIX + userId, enabled).apply();
        applyNotificationState(accountIndex, enabled);
        FileLog.d(TAG + " userId=" + userId + " enabled=" + enabled);
    }

    private void applyNotificationState(int accountIndex, boolean enabled) {
        try {
            NotificationsController controller = NotificationsController.getInstance(accountIndex);
            int muteTime = enabled ? 0 : Integer.MAX_VALUE;
            controller.setGlobalNotificationsEnabled(NotificationsController.TYPE_PRIVATE, muteTime);
            controller.setGlobalNotificationsEnabled(NotificationsController.TYPE_GROUP,   muteTime);
            controller.setGlobalNotificationsEnabled(NotificationsController.TYPE_CHANNEL, muteTime);
        } catch (Exception e) {
            FileLog.e(TAG, e);
        }
    }

    public boolean isCurrentAccountNotificationsEnabled() {
        int account = UserConfig.selectedAccount;
        long userId = UserConfig.getInstance(account).getClientUserId();
        return isNotificationsEnabled(userId);
    }

    public void setCurrentAccountNotificationsEnabled(boolean enabled) {
        int account = UserConfig.selectedAccount;
        long userId = UserConfig.getInstance(account).getClientUserId();
        setNotificationsEnabled(account, userId, enabled);
    }
}
