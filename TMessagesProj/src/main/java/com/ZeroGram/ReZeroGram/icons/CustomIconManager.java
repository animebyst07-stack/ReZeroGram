package com.ZeroGram.ReZeroGram.icons;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.File;
import java.io.FileOutputStream;

public class CustomIconManager {

    private static final String TAG = "CustomIconManager";
    private static final String DIR_NAME = "custom_icons";
    public static final int MAX_SLOTS = 3;
    public static final int ICON_SIZE_PX = 192;

    private static volatile CustomIconManager instance;
    private File iconDir;

    private CustomIconManager() {
    }

    public static CustomIconManager getInstance() {
        if (instance == null) {
            synchronized (CustomIconManager.class) {
                if (instance == null) {
                    instance = new CustomIconManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        iconDir = new File(context.getApplicationContext().getFilesDir(), DIR_NAME);
        if (!iconDir.exists()) {
            iconDir.mkdirs();
        }
        FileLog.d(TAG + " initialized. dir=" + iconDir.getAbsolutePath());
    }

    private File getIconFile(int slot) {
        return new File(iconDir, "custom_icon_" + slot + ".png");
    }

    public boolean saveCustomIcon(int slot, Bitmap bitmap) {
        if (slot < 1 || slot > MAX_SLOTS || bitmap == null || iconDir == null) return false;
        try {
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, ICON_SIZE_PX, ICON_SIZE_PX, true);
            File file = getIconFile(slot);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                scaled.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
            }
            FileLog.d(TAG + " saved slot=" + slot);
            return true;
        } catch (Exception e) {
            FileLog.e(TAG, e);
            return false;
        }
    }

    public Bitmap loadCustomIcon(int slot) {
        if (slot < 1 || slot > MAX_SLOTS || iconDir == null) return null;
        File file = getIconFile(slot);
        if (!file.exists()) return null;
        try {
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Exception e) {
            FileLog.e(TAG, e);
            return null;
        }
    }

    public void deleteCustomIcon(int slot) {
        if (slot < 1 || slot > MAX_SLOTS || iconDir == null) return;
        File file = getIconFile(slot);
        if (file.exists()) {
            boolean deleted = file.delete();
            FileLog.d(TAG + " deleted slot=" + slot + " result=" + deleted);
        }
    }

    public boolean hasCustomIcon(int slot) {
        if (slot < 1 || slot > MAX_SLOTS || iconDir == null) return false;
        return getIconFile(slot).exists();
    }

    public int getCustomIconCount() {
        int count = 0;
        for (int i = 1; i <= MAX_SLOTS; i++) {
            if (hasCustomIcon(i)) count++;
        }
        return count;
    }
}
