/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.ZeroGram.ReZeroGram.messages;

import android.os.Environment;
import android.text.TextUtils;
import com.google.android.exoplayer2.util.Log;
import com.ZeroGram.ReZeroGram.AyuConfig;
import com.ZeroGram.ReZeroGram.AyuConstants;
import com.ZeroGram.ReZeroGram.database.AyuData;
import com.ZeroGram.ReZeroGram.database.dao.DeletedMessageDao;
import com.ZeroGram.ReZeroGram.database.dao.EditedMessageDao;
import com.ZeroGram.ReZeroGram.database.entities.DeletedMessage;
import com.ZeroGram.ReZeroGram.database.entities.DeletedMessageFull;
import com.ZeroGram.ReZeroGram.database.entities.DeletedMessageReaction;
import com.ZeroGram.ReZeroGram.database.entities.EditedMessage;
import com.ZeroGram.ReZeroGram.proprietary.AyuMessageUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AyuMessagesController {
    public static final String attachmentsSubfolder = "Saved Attachments";
    public static final File attachmentsPath = new File(
            new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), AyuConstants.APP_NAME),
            attachmentsSubfolder
    );
    private static volatile AyuMessagesController instance;
    private static final Object lock = new Object();
    private final EditedMessageDao editedMessageDao;
    private final DeletedMessageDao deletedMessageDao;

    private AyuMessagesController() {
        initializeAttachmentsFolder();

        EditedMessageDao tmpEdited = null;
        DeletedMessageDao tmpDeleted = null;
        try {
            tmpEdited = AyuData.getEditedMessageDao();
            tmpDeleted = AyuData.getDeletedMessageDao();
        } catch (Exception e) {
            Log.e("AyuGram", "Failed to initialize DAOs", e);
        }
        editedMessageDao = tmpEdited;
        deletedMessageDao = tmpDeleted;
    }

    private static void initializeAttachmentsFolder() {
        try {
            if (!attachmentsPath.exists()) {
                attachmentsPath.mkdirs();
                try {
                    new File(attachmentsPath, ".nomedia").createNewFile();
                } catch (IOException e) {
                    // ignored
                }
            }
        } catch (Exception e) {
            // no permissions or storage unavailable
        }
    }

    public static AyuMessagesController getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new AyuMessagesController();
                }
            }
        }
        return instance;
    }

    public void onMessageEdited(AyuSavePreferences prefs, TLRPC.Message newMessage) {
        if (editedMessageDao == null) return;
        try {
            onMessageEditedInner(prefs, newMessage, false);
        } catch (Exception e) {
            Log.e("AyuGram", "error onMessageEdited", e);
            FileLog.e("onMessageEdited", e);
        }
    }

    public void onMessageEditedForce(AyuSavePreferences prefs) {
        if (editedMessageDao == null) return;
        try {
            onMessageEditedInner(prefs, prefs.getMessage(), true);
        } catch (Exception e) {
            Log.e("AyuGram", "error onMessageEditedForce", e);
            FileLog.e("onMessageEditedForce", e);
        }
    }

    private void onMessageEditedInner(AyuSavePreferences prefs, TLRPC.Message newMessage, boolean force) {
        if (!AyuConfig.saveEditedMessageFor(prefs.getAccountId(), prefs.getDialogId())) {
            return;
        }

        var oldMessage = prefs.getMessage();

        boolean sameMedia = oldMessage.media == newMessage.media ||
                (oldMessage.media != null && newMessage.media != null && oldMessage.media.getClass() == newMessage.media.getClass());
        if (oldMessage.media instanceof TLRPC.TL_messageMediaPhoto && newMessage.media instanceof TLRPC.TL_messageMediaPhoto && oldMessage.media.photo != null && newMessage.media.photo != null) {
            sameMedia = oldMessage.media.photo.id == newMessage.media.photo.id;
        } else if (oldMessage.media instanceof TLRPC.TL_messageMediaDocument && newMessage.media instanceof TLRPC.TL_messageMediaDocument && oldMessage.media.document != null && newMessage.media.document != null) {
            sameMedia = oldMessage.media.document.id == newMessage.media.document.id;
        }

        if (force) {
            sameMedia = false;
        }

        if (sameMedia && TextUtils.equals(oldMessage.message, newMessage.message)) {
            return;
        }

        var revision = new EditedMessage();
        AyuMessageUtils.map(prefs, revision);
        AyuMessageUtils.mapMedia(prefs, revision, !sameMedia);

        if (!sameMedia && !TextUtils.isEmpty(revision.mediaPath)) {
            var lastRevision = editedMessageDao.getLastRevision(prefs.getUserId(), prefs.getDialogId(), prefs.getMessageId());

            if (lastRevision != null && !TextUtils.equals(revision.mediaPath, lastRevision.mediaPath) && lastRevision.mediaPath != null && !lastRevision.mediaPath.contains(attachmentsSubfolder)) {
                // update previous revisions to reflect media change
                // like, there's no previous file, so replace it with one we copied before...
                editedMessageDao.updateAttachmentForRevisionsBetweenDates(prefs.getUserId(), prefs.getDialogId(), prefs.getMessageId(), lastRevision.mediaPath, revision.mediaPath);
            }
        }

        editedMessageDao.insert(revision);

        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getInstance(prefs.getAccountId()).postNotificationName(AyuConstants.MESSAGE_EDITED_NOTIFICATION, prefs.getDialogId(), prefs.getMessageId());
        });
    }

    public void onMessageDeleted(AyuSavePreferences prefs) {
        if (deletedMessageDao == null) return;
        if (prefs.getMessage() == null) {
            Log.w("AyuGram", "null msg ?");
            return;
        }

        try {
            onMessageDeletedInner(prefs);
        } catch (Exception e) {
            Log.e("AyuGram", "error onMessageDeleted", e);
            FileLog.e("onMessageDeleted", e);
        }
    }

    private void onMessageDeletedInner(AyuSavePreferences prefs) {
        if (!AyuConfig.saveDeletedMessageFor(prefs.getAccountId(), prefs.getDialogId())) {
            return;
        }

        if (deletedMessageDao.exists(prefs.getUserId(), prefs.getDialogId(), prefs.getTopicId(), prefs.getMessageId())) {
            return;
        }

        var deletedMessage = new DeletedMessage();
        deletedMessage.userId = prefs.getUserId();
        deletedMessage.dialogId = prefs.getDialogId();
        deletedMessage.messageId = prefs.getMessageId();
        deletedMessage.entityCreateDate = prefs.getRequestCatchTime();

        var msg = prefs.getMessage();

        Log.d("AyuGram", "saving message " + prefs.getMessageId() + " for " + prefs.getDialogId() + " with topic " + prefs.getTopicId());

        AyuMessageUtils.map(prefs, deletedMessage);
        AyuMessageUtils.mapMedia(prefs, deletedMessage, true);

        var fakeMsgId = deletedMessageDao.insert(deletedMessage);

        if (msg != null && msg.reactions != null && AyuConfig.saveReactions) {
            processDeletedReactions(fakeMsgId, msg.reactions);
        }
    }

    private void processDeletedReactions(long fakeMessageId, TLRPC.TL_messageReactions reactions) {
        for (var reaction : reactions.results) {
            if (reaction.reaction instanceof TLRPC.TL_reactionEmpty) {
                continue;
            }

            var deletedReaction = new DeletedMessageReaction();
            deletedReaction.deletedMessageId = fakeMessageId;
            deletedReaction.count = reaction.count;
            deletedReaction.selfSelected = reaction.chosen;

            if (reaction.reaction instanceof TLRPC.TL_reactionEmoji) {
                deletedReaction.emoticon = ((TLRPC.TL_reactionEmoji) reaction.reaction).emoticon;
            } else if (reaction.reaction instanceof TLRPC.TL_reactionCustomEmoji) {
                deletedReaction.documentId = ((TLRPC.TL_reactionCustomEmoji) reaction.reaction).document_id;
                deletedReaction.isCustom = true;
            } else {
                Log.e("AyuGram", "fake news emoji");
                continue;
            }

            deletedMessageDao.insertReaction(deletedReaction);
        }
    }

    public boolean hasAnyRevisions(long userId, long dialogId, int messageId) {
        if (editedMessageDao == null) return false;
        try {
            return editedMessageDao.hasAnyRevisions(userId, dialogId, messageId);
        } catch (Exception e) {
            Log.e("AyuGram", "hasAnyRevisions error", e);
            return false;
        }
    }

    public List<EditedMessage> getRevisions(long userId, long dialogId, int messageId) {
        if (editedMessageDao == null) return new ArrayList<>();
        try {
            List<EditedMessage> result = editedMessageDao.getAllRevisions(userId, dialogId, messageId);
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            Log.e("AyuGram", "getRevisions error", e);
            return new ArrayList<>();
        }
    }

    public DeletedMessageFull getMessage(long userId, long dialogId, int messageId) {
        if (deletedMessageDao == null) return null;
        try {
            return deletedMessageDao.getMessage(userId, dialogId, messageId);
        } catch (Exception e) {
            Log.e("AyuGram", "getMessage error", e);
            return null;
        }
    }

    public List<DeletedMessageFull> getMessages(long userId, long dialogId, long topicId, int startId, int endId, int limit) {
        if (deletedMessageDao == null) return new ArrayList<>();
        try {
            List<DeletedMessageFull> result = deletedMessageDao.getMessages(userId, dialogId, topicId, startId, endId, limit);
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            Log.e("AyuGram", "getMessages error", e);
            return new ArrayList<>();
        }
    }

    public List<DeletedMessageFull> getMessagesGrouped(long userId, long dialogId, long groupedId) {
        if (deletedMessageDao == null) return new ArrayList<>();
        try {
            List<DeletedMessageFull> result = deletedMessageDao.getMessagesGrouped(userId, dialogId, groupedId);
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            Log.e("AyuGram", "getMessagesGrouped error", e);
            return new ArrayList<>();
        }
    }

    public void delete(long userId, long dialogId, int messageId) {
        var msg = getMessage(userId, dialogId, messageId);
        if (msg == null) {
            return;
        }

        deletedMessageDao.delete(userId, dialogId, messageId);

        if (!TextUtils.isEmpty(msg.message.mediaPath)) {
            var p = new File(msg.message.mediaPath);
            if (p.exists()) {
                try {
                    p.delete();
                } catch (Exception e) {
                    Log.e("AyuGram", "failed to delete file " + msg.message.mediaPath, e);
                }
            }
        }
    }

    public void clean() {
        synchronized (lock) {
            AyuData.clean();
            AyuData.create();

            instance = null;
        }
    }
}
