package com.ZeroGram.ReZeroGram.components;

import android.annotation.SuppressLint;
import android.content.Context;

import com.ZeroGram.ReZeroGram.groq.GroqManager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.Theme;

@SuppressLint("ViewConstructor")
public class GroqBeforeSendWrapper extends ActionBarMenuSubItem {

    public GroqBeforeSendWrapper(Context context, boolean top, boolean bottom, Theme.ResourcesProvider resourcesProvider) {
        super(context, top, bottom, resourcesProvider);
        setTextAndIcon("✨ Groq AI", R.drawable.msg_translate);
        setSubtext("Улучшить текст");
        setMinimumWidth(AndroidUtilities.dp(196));
        setItemHeight(56);
        setOnClickListener(v -> onClick());
    }

    protected void onClick() {
    }

    public static boolean isAvailable() {
        return GroqManager.getInstance().isEnabled();
    }
}
