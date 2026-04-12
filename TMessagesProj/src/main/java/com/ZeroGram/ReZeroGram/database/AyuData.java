/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.ZeroGram.ReZeroGram.database;

import androidx.room.Room;
import com.ZeroGram.ReZeroGram.AyuConstants;
import com.ZeroGram.ReZeroGram.database.dao.DeletedMessageDao;
import com.ZeroGram.ReZeroGram.database.dao.EditedMessageDao;
import org.telegram.messenger.ApplicationLoader;

public class AyuData {
    private static AyuDatabase database;
    private static EditedMessageDao editedMessageDao;
    private static DeletedMessageDao deletedMessageDao;

    static {
        try {
            if (ApplicationLoader.applicationContext != null) {
                create();
            }
        } catch (Exception e) {
            // will be initialized later
        }
    }

    public static void create() {
        if (ApplicationLoader.applicationContext == null) {
            return;
        }
        if (database != null) {
            return;
        }
        database = Room.databaseBuilder(ApplicationLoader.applicationContext, AyuDatabase.class, AyuConstants.AYU_DATABASE)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();

        editedMessageDao = database.editedMessageDao();
        deletedMessageDao = database.deletedMessageDao();
    }

    public static AyuDatabase getDatabase() {
        if (database == null) {
            create();
        }
        return database;
    }

    public static EditedMessageDao getEditedMessageDao() {
        if (editedMessageDao == null) {
            create();
        }
        return editedMessageDao;
    }

    public static DeletedMessageDao getDeletedMessageDao() {
        if (deletedMessageDao == null) {
            create();
        }
        return deletedMessageDao;
    }

    public static void clean() {
        database.close();

        ApplicationLoader.applicationContext.deleteDatabase(AyuConstants.AYU_DATABASE);
    }
}
