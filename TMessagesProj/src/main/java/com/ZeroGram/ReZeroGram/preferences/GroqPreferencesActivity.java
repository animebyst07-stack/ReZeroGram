package com.ZeroGram.ReZeroGram.preferences;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ZeroGram.ReZeroGram.groq.GroqCallback;
import com.ZeroGram.ReZeroGram.groq.GroqConfig;
import com.ZeroGram.ReZeroGram.groq.GroqManager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

public class GroqPreferencesActivity extends BasePreferencesActivity {

    private int groqHeaderRow;
    private int groqEnabledRow;
    private int groqApiKeyRow;
    private int groqModelRow;
    private int groqTestRow;
    private int groqInfoRow;

    private int promptHeaderRow;
    private int groqPromptRow;
    private int groqResetPromptRow;
    private int groqPromptInfoRow;

    @Override
    protected void updateRowsId() {
        super.updateRowsId();

        groqHeaderRow       = newRow();
        groqEnabledRow      = newRow();
        groqApiKeyRow       = newRow();
        groqModelRow        = newRow();
        groqTestRow         = newRow();
        groqInfoRow         = newRow();

        promptHeaderRow     = newRow();
        groqPromptRow       = newRow();
        groqResetPromptRow  = newRow();
        groqPromptInfoRow   = newRow();
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == groqEnabledRow) {
            boolean newValue = !GroqManager.getInstance().isEnabled();
            GroqManager.getInstance().setEnabled(newValue);
            ((TextCheckCell) view).setChecked(GroqManager.getInstance().isEnabled());
        } else if (position == groqApiKeyRow) {
            showApiKeyDialog();
        } else if (position == groqModelRow) {
            showModelPicker();
        } else if (position == groqTestRow) {
            testGroqConnection();
        } else if (position == groqPromptRow) {
            showPromptDialog();
        } else if (position == groqResetPromptRow) {
            GroqManager.getInstance().resetPromptToDefault();
            BulletinFactory.of(this).createErrorBulletin(
                    "Промпт сброшен к стандартному", resourcesProvider).show();
            listAdapter.notifyItemChanged(groqPromptRow, payload);
        }
    }

    private void showApiKeyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Groq API ключ");

        EditText input = new EditText(getParentActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setHint("gsk_xxxxxxxxxxxx");
        input.setText(GroqManager.getInstance().getApiKey());
        input.setSelection(input.getText().length());
        input.setPadding(
                AndroidUtilities.dp(16), AndroidUtilities.dp(8),
                AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        input.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        input.setBackgroundDrawable(Theme.createEditTextDrawable(getParentActivity(), true));
        builder.setView(input);

        builder.setPositiveButton(LocaleController.getString("Save", R.string.Save), (dialog, which) -> {
            String key = input.getText().toString().trim();
            GroqManager.getInstance().setApiKey(key);
            listAdapter.notifyItemChanged(groqApiKeyRow, payload);
            listAdapter.notifyItemChanged(groqEnabledRow, payload);
            if (!key.isEmpty()) {
                BulletinFactory.of(this).createErrorBulletin(
                        "API ключ сохранён", resourcesProvider).show();
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    private void showModelPicker() {
        String[] modelNames = GroqConfig.MODEL_NAMES;
        String[] modelIds   = GroqConfig.MODEL_IDS;
        String current      = GroqManager.getInstance().getModel();

        int selected = 0;
        for (int i = 0; i < modelIds.length; i++) {
            if (modelIds[i].equals(current)) { selected = i; break; }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Выберите модель Groq");
        builder.setSingleChoiceItems(modelNames, selected, (dialog, which) -> {
            GroqManager.getInstance().setModel(modelIds[which]);
            dialog.dismiss();
            listAdapter.notifyItemChanged(groqModelRow, payload);
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    private void testGroqConnection() {
        if (GroqManager.getInstance().isProcessing()) {
            BulletinFactory.of(this).createErrorBulletin(
                    "Уже идёт обработка, подождите", resourcesProvider).show();
            return;
        }
        GroqManager.getInstance().testConnection(new GroqCallback() {
            @Override public void onSuccess(String text) {
                BulletinFactory.of(GroqPreferencesActivity.this)
                        .createErrorBulletin(text, resourcesProvider).show();
            }
            @Override public void onError(String errorMessage) {
                BulletinFactory.of(GroqPreferencesActivity.this)
                        .createErrorBulletin("Ошибка: " + errorMessage, resourcesProvider).show();
            }
            @Override public void onProcessing() {
                BulletinFactory.of(GroqPreferencesActivity.this)
                        .createErrorBulletin("Проверка подключения...", resourcesProvider).show();
            }
        });
    }

    private void showPromptDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Системный промпт Groq");

        LinearLayout container = new LinearLayout(getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(
                AndroidUtilities.dp(16), AndroidUtilities.dp(8),
                AndroidUtilities.dp(16), AndroidUtilities.dp(8));

        EditText input = new EditText(getParentActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        input.setMinLines(4);
        input.setMaxLines(8);
        input.setGravity(android.view.Gravity.TOP);
        input.setVerticalScrollBarEnabled(true);
        input.setHint("Введите свой промпт...");
        input.setText(GroqManager.getInstance().getCustomPrompt());
        input.setSelection(input.getText().length());
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        input.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        input.setBackgroundDrawable(Theme.createEditTextDrawable(getParentActivity(), true));
        container.addView(input, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView hint = new TextView(getParentActivity());
        hint.setText("В конец промпта автоматически добавляется исходный текст.");
        hint.setTextSize(11);
        hint.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        hint.setPadding(0, AndroidUtilities.dp(6), 0, 0);
        container.addView(hint, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString("Save", R.string.Save), (dialog, which) -> {
            String prompt = input.getText().toString().trim();
            GroqManager.getInstance().setCustomPrompt(prompt);
            listAdapter.notifyItemChanged(groqPromptRow, payload);
            BulletinFactory.of(this).createErrorBulletin(
                    "Промпт сохранён", resourcesProvider).show();
        });
        builder.setNeutralButton("Сбросить", (dialog, which) -> {
            GroqManager.getInstance().resetPromptToDefault();
            listAdapter.notifyItemChanged(groqPromptRow, payload);
            BulletinFactory.of(this).createErrorBulletin(
                    "Промпт сброшен к стандартному", resourcesProvider).show();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    @Override
    protected String getTitle() {
        return "Groq AI";
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 1:
                    holder.itemView.setBackground(Theme.getThemedDrawable(
                            mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case 2:
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position == groqApiKeyRow) {
                        String key = GroqManager.getInstance().getApiKey();
                        String display = (key != null && !key.isEmpty())
                                ? "gsk_•••••••" + (key.length() > 8 ? key.substring(key.length() - 4) : "")
                                : "Не указан";
                        textCell.setTextAndValue("API ключ", display, true);
                    } else if (position == groqModelRow) {
                        String model = GroqManager.getInstance().getModel();
                        String name  = model;
                        for (int i = 0; i < GroqConfig.MODEL_IDS.length; i++) {
                            if (GroqConfig.MODEL_IDS[i].equals(model)) {
                                name = GroqConfig.MODEL_NAMES[i];
                                break;
                            }
                        }
                        textCell.setTextAndValue("Модель", name, true);
                    } else if (position == groqTestRow) {
                        textCell.setText("Проверить подключение", false);
                    } else if (position == groqPromptRow) {
                        String prompt = GroqManager.getInstance().getCustomPrompt();
                        String preview = prompt.length() > 40 ? prompt.substring(0, 40) + "…" : prompt;
                        textCell.setTextAndValue("Системный промпт", preview, true);
                    } else if (position == groqResetPromptRow) {
                        textCell.setText("Сбросить промпт к стандартному", false);
                    }
                    break;
                case 3:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == groqHeaderRow) {
                        headerCell.setText("Groq AI");
                    } else if (position == promptHeaderRow) {
                        headerCell.setText("Промпт");
                    }
                    break;
                case 4:
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (position == groqEnabledRow) {
                        checkCell.setTextAndCheck(
                                "Включить Groq AI",
                                GroqManager.getInstance().isEnabled(),
                                true);
                    }
                    break;
                case 5:
                    TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == groqInfoRow) {
                        infoCell.setText(
                                "Groq AI обрабатывает ваш текст перед отправкой. " +
                                "Кнопка ✨ появится в поле ввода сообщений. " +
                                "Получите API ключ на groq.com — бесплатно.");
                    } else if (position == groqPromptInfoRow) {
                        infoCell.setText(
                                "Промпт определяет, как Groq обрабатывает текст. " +
                                "По умолчанию: улучшение структуры и читаемости без изменения смысла.");
                    }
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == groqHeaderRow || position == promptHeaderRow) return 3;
            if (position == groqEnabledRow)                               return 4;
            if (position == groqInfoRow || position == groqPromptInfoRow) return 5;
            if (position == groqApiKeyRow || position == groqModelRow ||
                position == groqTestRow  || position == groqPromptRow ||
                position == groqResetPromptRow)                           return 2;
            return 1;
        }
    }
}
