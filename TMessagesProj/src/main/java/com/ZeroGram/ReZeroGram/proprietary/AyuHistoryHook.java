/*
 * This is the source code of ReZeroGram for Android.
 * Stub implementation of proprietary AyuHistoryHook.
 */

package com.ZeroGram.ReZeroGram.proprietary;

import android.util.Pair;
import android.util.SparseArray;
import org.telegram.messenger.MessageObject;

import java.util.ArrayList;

public class AyuHistoryHook {

    public static Pair<Integer, Integer> getMinAndMaxIds(ArrayList<MessageObject> messArr) {
        if (messArr == null || messArr.isEmpty()) {
            return new Pair<>(Integer.MIN_VALUE, Integer.MIN_VALUE);
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (MessageObject msg : messArr) {
            int id = msg.getId();
            if (id < min) min = id;
            if (id > max) max = id;
        }
        return new Pair<>(min, max);
    }

    @SuppressWarnings("unchecked")
    public static void doHook(int currentAccount, ArrayList<MessageObject> messArr,
                               SparseArray<MessageObject>[] messagesDict,
                               int startId, int endId, long dialogId,
                               int limit, int topicId, boolean isSecretChat) {
        // stub - no-op implementation
    }
}
