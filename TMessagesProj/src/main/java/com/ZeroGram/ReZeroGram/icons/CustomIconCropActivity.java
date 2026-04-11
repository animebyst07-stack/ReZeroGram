package com.ZeroGram.ReZeroGram.icons;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;

import java.io.IOException;
import java.io.InputStream;

public class CustomIconCropActivity extends Activity {

    public static final String EXTRA_SLOT = "slot";
    public static final int REQUEST_PICK_IMAGE = 1001;
    public static final int RESULT_ICON_SAVED = RESULT_FIRST_USER + 1;

    private int slot;
    private Bitmap sourceBitmap;
    private CropView cropView;
    private ImageView previewView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        slot = getIntent().getIntExtra(EXTRA_SLOT, 1);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF1C2733);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(this);
        title.setText("Кадрирование иконки");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        layout.addView(title);

        cropView = new CropView(this);
        LinearLayout.LayoutParams cropParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        cropParams.setMargins(AndroidUtilities.dp(16), AndroidUtilities.dp(8),
                AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        layout.addView(cropView, cropParams);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), 0);

        TextView previewLabel = new TextView(this);
        previewLabel.setText("Предпросмотр:");
        previewLabel.setTextColor(0xFFAAAAAA);
        previewLabel.setTextSize(14);
        row.addView(previewLabel);

        previewView = new ImageView(this);
        previewView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        previewView.setBackgroundColor(0xFF2D3D4E);
        LinearLayout.LayoutParams pvParams = new LinearLayout.LayoutParams(
                AndroidUtilities.dp(64), AndroidUtilities.dp(64));
        pvParams.setMargins(AndroidUtilities.dp(12), 0, 0, 0);
        row.addView(previewView, pvParams);

        layout.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(80)));

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8),
                AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        btnRow.setGravity(Gravity.CENTER);

        TextView btnSelect = makeButton("Выбрать фото", 0xFF3F8AE0);
        btnSelect.setOnClickListener(v -> pickImage());
        btnRow.addView(btnSelect, new LinearLayout.LayoutParams(
                0, AndroidUtilities.dp(44), 1f));

        View spacer = new View(this);
        btnRow.addView(spacer, new LinearLayout.LayoutParams(AndroidUtilities.dp(12),
                ViewGroup.LayoutParams.MATCH_PARENT));

        TextView btnSave = makeButton("Сохранить", 0xFF4CAF50);
        btnSave.setOnClickListener(v -> saveIcon());
        btnRow.addView(btnSave, new LinearLayout.LayoutParams(
                0, AndroidUtilities.dp(44), 1f));

        layout.addView(btnRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(layout);
        setContentView(root);

        pickImage();
    }

    private TextView makeButton(String text, int color) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(14);
        btn.setGravity(Gravity.CENTER);
        btn.setBackgroundColor(color);
        btn.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), 0);
        return btn;
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                loadBitmapFromUri(uri);
            }
        }
    }

    private void loadBitmapFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return;
            sourceBitmap = BitmapFactory.decodeStream(is);
            is.close();
            if (sourceBitmap != null) {
                cropView.setBitmap(sourceBitmap);
                updatePreview();
            }
        } catch (IOException e) {
            FileLog.e("CustomIconCrop", e);
            Toast.makeText(this, "Не удалось загрузить изображение", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePreview() {
        Bitmap cropped = cropView.getCroppedBitmap();
        if (cropped != null) {
            previewView.setImageBitmap(cropped);
        }
    }

    private void saveIcon() {
        if (sourceBitmap == null) {
            Toast.makeText(this, "Сначала выберите фото", Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap cropped = cropView.getCroppedBitmap();
        if (cropped == null) {
            Toast.makeText(this, "Ошибка кадрирования", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean saved = CustomIconManager.getInstance().saveCustomIcon(slot, cropped);
        if (saved) {
            Toast.makeText(this, "Иконка сохранена!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_ICON_SAVED);
            finish();
        } else {
            Toast.makeText(this, "Не удалось сохранить иконку", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("ViewConstructor")
    private class CropView extends View {

        private Bitmap bitmap;
        private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint overlayPaint = new Paint();
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private float cropLeft, cropTop, cropRight, cropBottom;
        private boolean initialized = false;
        private int activeHandle = -1;

        private static final int HANDLE_NONE = -1;
        private static final int HANDLE_TL = 0, HANDLE_TR = 1, HANDLE_BL = 2, HANDLE_BR = 3;
        private static final int HANDLE_MOVE = 4;
        private static final float HANDLE_SIZE = 40f;
        private static final float MIN_CROP_SIZE = 80f;

        private float lastTouchX, lastTouchY;

        private Rect imageRect = new Rect();

        public CropView(Activity context) {
            super(context);
            overlayPaint.setColor(0x88000000);
            overlayPaint.setStyle(Paint.Style.FILL);
            borderPaint.setColor(0xFFFFFFFF);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(3f);
            handlePaint.setColor(0xFF3F8AE0);
            handlePaint.setStyle(Paint.Style.FILL);
        }

        public void setBitmap(Bitmap bmp) {
            this.bitmap = bmp;
            initialized = false;
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (bitmap != null && !initialized) {
                initCropRect(w, h);
            }
        }

        private void initCropRect(int viewW, int viewH) {
            if (bitmap == null) return;
            float bw = bitmap.getWidth(), bh = bitmap.getHeight();
            float scale = Math.min((float) viewW / bw, (float) viewH / bh);
            float imgW = bw * scale, imgH = bh * scale;
            float imgX = (viewW - imgW) / 2f, imgY = (viewH - imgH) / 2f;
            imageRect.set((int) imgX, (int) imgY, (int) (imgX + imgW), (int) (imgY + imgH));

            float side = Math.min(imgW, imgH) * 0.8f;
            float cx = imgX + imgW / 2f, cy = imgY + imgH / 2f;
            cropLeft   = cx - side / 2f;
            cropTop    = cy - side / 2f;
            cropRight  = cx + side / 2f;
            cropBottom = cy + side / 2f;
            clampCropRect();
            initialized = true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (bitmap == null) {
                canvas.drawColor(0xFF1A2537);
                return;
            }
            if (!initialized) {
                initCropRect(getWidth(), getHeight());
            }
            canvas.drawBitmap(bitmap, null, imageRect, imagePaint);

            canvas.drawRect(imageRect.left, imageRect.top, imageRect.right, cropTop, overlayPaint);
            canvas.drawRect(imageRect.left, cropBottom, imageRect.right, imageRect.bottom, overlayPaint);
            canvas.drawRect(imageRect.left, cropTop, cropLeft, cropBottom, overlayPaint);
            canvas.drawRect(cropRight, cropTop, imageRect.right, cropBottom, overlayPaint);

            canvas.drawRect(cropLeft, cropTop, cropRight, cropBottom, borderPaint);

            float hs = HANDLE_SIZE;
            canvas.drawRect(cropLeft - hs / 2, cropTop - hs / 2, cropLeft + hs / 2, cropTop + hs / 2, handlePaint);
            canvas.drawRect(cropRight - hs / 2, cropTop - hs / 2, cropRight + hs / 2, cropTop + hs / 2, handlePaint);
            canvas.drawRect(cropLeft - hs / 2, cropBottom - hs / 2, cropLeft + hs / 2, cropBottom + hs / 2, handlePaint);
            canvas.drawRect(cropRight - hs / 2, cropBottom - hs / 2, cropRight + hs / 2, cropBottom + hs / 2, handlePaint);
        }

        @Override
        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouchEvent(MotionEvent event) {
            if (bitmap == null) return false;
            float x = event.getX(), y = event.getY();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    activeHandle = getHandleAt(x, y);
                    lastTouchX = x; lastTouchY = y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = x - lastTouchX, dy = y - lastTouchY;
                    applyDrag(activeHandle, dx, dy);
                    makeCropSquare();
                    clampCropRect();
                    lastTouchX = x; lastTouchY = y;
                    invalidate();
                    updatePreview();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    activeHandle = HANDLE_NONE;
                    return true;
            }
            return false;
        }

        private int getHandleAt(float x, float y) {
            float hs = HANDLE_SIZE * 1.5f;
            if (Math.abs(x - cropLeft) < hs && Math.abs(y - cropTop) < hs)    return HANDLE_TL;
            if (Math.abs(x - cropRight) < hs && Math.abs(y - cropTop) < hs)   return HANDLE_TR;
            if (Math.abs(x - cropLeft) < hs && Math.abs(y - cropBottom) < hs) return HANDLE_BL;
            if (Math.abs(x - cropRight) < hs && Math.abs(y - cropBottom) < hs) return HANDLE_BR;
            if (x > cropLeft && x < cropRight && y > cropTop && y < cropBottom) return HANDLE_MOVE;
            return HANDLE_NONE;
        }

        private void applyDrag(int handle, float dx, float dy) {
            float avg = (Math.abs(dx) + Math.abs(dy)) / 2f;
            switch (handle) {
                case HANDLE_TL:
                    cropLeft += dx; cropTop += dy; break;
                case HANDLE_TR:
                    cropRight += dx; cropTop += dy; break;
                case HANDLE_BL:
                    cropLeft += dx; cropBottom += dy; break;
                case HANDLE_BR:
                    cropRight += dx; cropBottom += dy; break;
                case HANDLE_MOVE:
                    cropLeft += dx; cropRight += dx;
                    cropTop += dy; cropBottom += dy; break;
            }
        }

        private void makeCropSquare() {
            float w = cropRight - cropLeft;
            float h = cropBottom - cropTop;
            float side = Math.min(w, h);
            float cx = (cropLeft + cropRight) / 2f;
            float cy = (cropTop + cropBottom) / 2f;
            cropLeft   = cx - side / 2f;
            cropRight  = cx + side / 2f;
            cropTop    = cy - side / 2f;
            cropBottom = cy + side / 2f;
        }

        private void clampCropRect() {
            if (cropRight - cropLeft < MIN_CROP_SIZE) {
                float cx = (cropLeft + cropRight) / 2f;
                cropLeft  = cx - MIN_CROP_SIZE / 2f;
                cropRight = cx + MIN_CROP_SIZE / 2f;
            }
            if (cropBottom - cropTop < MIN_CROP_SIZE) {
                float cy = (cropTop + cropBottom) / 2f;
                cropTop    = cy - MIN_CROP_SIZE / 2f;
                cropBottom = cy + MIN_CROP_SIZE / 2f;
            }
            cropLeft   = Math.max(cropLeft,   imageRect.left);
            cropTop    = Math.max(cropTop,    imageRect.top);
            cropRight  = Math.min(cropRight,  imageRect.right);
            cropBottom = Math.min(cropBottom, imageRect.bottom);
        }

        public Bitmap getCroppedBitmap() {
            if (bitmap == null) return null;
            float scaleX = (float) bitmap.getWidth() / imageRect.width();
            float scaleY = (float) bitmap.getHeight() / imageRect.height();
            int bx = (int) ((cropLeft - imageRect.left) * scaleX);
            int by = (int) ((cropTop - imageRect.top) * scaleY);
            int bw = (int) ((cropRight - cropLeft) * scaleX);
            int bh = (int) ((cropBottom - cropTop) * scaleY);
            bx = Math.max(0, Math.min(bx, bitmap.getWidth() - 1));
            by = Math.max(0, Math.min(by, bitmap.getHeight() - 1));
            bw = Math.max(1, Math.min(bw, bitmap.getWidth() - bx));
            bh = Math.max(1, Math.min(bh, bitmap.getHeight() - by));
            try {
                return Bitmap.createBitmap(bitmap, bx, by, bw, bh);
            } catch (Exception e) {
                FileLog.e("CustomIconCrop", e);
                return null;
            }
        }
    }
}
