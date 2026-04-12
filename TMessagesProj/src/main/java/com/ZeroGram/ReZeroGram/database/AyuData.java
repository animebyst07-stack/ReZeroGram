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
    private static final Object lock = new Object();
    private static volatile AyuDatabase database;
    private static volatile EditedMessageDao editedMessageDao;
    private static volatile DeletedMessageDao deletedMessageDao;

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
        synchronized (lock) {
            if (database != null) {
                return;
            }
            try {
                database = Room.databaseBuilder(ApplicationLoader.applicationContext, AyuDatabase.class, AyuConstants.AYU_DATABASE)
                        .allowMainThreadQueries()
                        .fallbackToDestructiveMigration()
                        .build();

                editedMessageDao = database.editedMessageDao();
                deletedMessageDao = database.deletedMessageDao();
            } catch (Exception e) {
                database = null;
                editedMessageDao = null;
                deletedMessageDao = null;
            }
        }
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
        synchronized (lock) {
            if (database != null) {
                try {
                    database.close();
                } catch (Exception e) {
                    // ignore
                }
            }

            database = null;
            editedMessageDao = null;
            deletedMessageDao = null;

            if (ApplicationLoader.applicationContext != null) {
                ApplicationLoader.applicationContext.deleteDatabase(AyuConstants.AYU_DATABASE);
            }
        }
    }
}
