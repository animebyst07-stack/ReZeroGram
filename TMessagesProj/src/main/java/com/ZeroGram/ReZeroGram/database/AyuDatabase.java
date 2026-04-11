/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.ZeroGram.ReZeroGram.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.ZeroGram.ReZeroGram.database.dao.DeletedMessageDao;
import com.ZeroGram.ReZeroGram.database.dao.EditedMessageDao;
import com.ZeroGram.ReZeroGram.database.entities.DeletedMessage;
import com.ZeroGram.ReZeroGram.database.entities.DeletedMessageReaction;
import com.ZeroGram.ReZeroGram.database.entities.EditedMessage;

@Database(entities = {
        EditedMessage.class,
        DeletedMessage.class,
        DeletedMessageReaction.class
}, version = 21)
public abstract class AyuDatabase extends RoomDatabase {
    public abstract EditedMessageDao editedMessageDao();

    public abstract DeletedMessageDao deletedMessageDao();
}