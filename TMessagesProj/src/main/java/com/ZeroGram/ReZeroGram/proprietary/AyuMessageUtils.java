/*
 * This is the source code of ReZeroGram for Android.
 * Stub implementation of proprietary AyuMessageUtils.
 */

package com.ZeroGram.ReZeroGram.proprietary;

import com.ZeroGram.ReZeroGram.database.entities.DeletedMessage;
import com.ZeroGram.ReZeroGram.database.entities.EditedMessage;
import com.ZeroGram.ReZeroGram.messages.AyuSavePreferences;
import org.telegram.tgnet.TLRPC;

public class AyuMessageUtils {

    public static void map(AyuSavePreferences prefs, EditedMessage revision) {
        // stub
    }

    public static void mapMedia(AyuSavePreferences prefs, EditedMessage revision, boolean includeMedia) {
        // stub
    }

    public static void map(AyuSavePreferences prefs, DeletedMessage deletedMessage) {
        // stub
    }

    public static void mapMedia(AyuSavePreferences prefs, DeletedMessage deletedMessage, boolean includeMedia) {
        // stub
    }

    public static void map(EditedMessage editedMessage, TLRPC.Message msg, int currentAccount) {
        // stub
    }

    public static void mapMedia(EditedMessage editedMessage, TLRPC.Message msg) {
        // stub
    }
}
