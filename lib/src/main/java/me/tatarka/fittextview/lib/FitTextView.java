package me.tatarka.fittextview.lib;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class FitTextView extends TextView {
    private static final float LIMIT = 1;
    private static final float MIN_TEXT_SIZE_SP = 10;
    private static final float MAX_TEXT_SIZE_SP = 40;

    private static final int INCLUDE_FONT_PADDING = 0;
    private static final int LINE_SPACING_MULTIPLIER = 1;
    private static final int LINE_SPACING_EXTRA = 2;
    private static final int MAX_LINES = 3;
    private static final int SINGLE_LINE = 4;
    private static final int[] ANDROID_ATTRS = new int[] {
            android.R.attr.includeFontPadding,
            android.R.attr.lineSpacingMultiplier,
            android.R.attr.lineSpacingExtra,
            android.R.attr.maxLines,
            android.R.attr.singleLine
    };

    private float mOriginalTextSize = 0;
    private float mMinTextSize;
    private float mMaxTextSize;
    private boolean mMeasured;

    private boolean mIncludeFontPadding = true;
    private float mLineSpacingMult = 1;
    private float mLineSpacingAdd = 0;
    private int mMaxLines = Integer.MAX_VALUE;
    private boolean mSingleLine = false;

    private Cache mCache;

    public FitTextView(Context context) {
        this(context, null);
    }

    public FitTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FitTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mOriginalTextSize = getTextSize();

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FitTextView);
            mMinTextSize = a.getDimension(R.styleable.FitTextView_ftMinTextSize, getDefaultMinTextSize());
            mMaxTextSize = a.getDimension(R.styleable.FitTextView_ftMaxTextSize, getDefaultMaxTextSize());
            a.recycle();

            a = context.obtainStyledAttributes(attrs, ANDROID_ATTRS);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                mIncludeFontPadding = a.getBoolean(INCLUDE_FONT_PADDING, mIncludeFontPadding);
                mLineSpacingMult = a.getFloat(LINE_SPACING_MULTIPLIER, mLineSpacingMult);
                mLineSpacingAdd = a.getDimensionPixelSize(LINE_SPACING_EXTRA, (int) mLineSpacingAdd);
                mMaxLines = a.getInteger(MAX_LINES, mMaxLines);
            }
            mSingleLine = a.getBoolean(SINGLE_LINE, mSingleLine);
            a.recycle();
        } else {
            mMinTextSize = getDefaultMinTextSize();
            mMaxTextSize = getDefaultMaxTextSize();
        }
    }

    private float getDefaultMinTextSize() {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, MIN_TEXT_SIZE_SP, getResources().getDisplayMetrics());
    }

    private float getDefaultMaxTextSize() {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, MAX_TEXT_SIZE_SP, getResources().getDisplayMetrics());
    }

    public void setMinTextSize(float size) {
        mMinTextSize = size;
        if (mMaxTextSize < mMinTextSize) mMaxTextSize = mMinTextSize;
        requestLayout();
    }

    public void setMaxTextSize(float size) {
        mMaxTextSize = size;
        if (mMinTextSize > mMaxTextSize) mMinTextSize = mMaxTextSize;
        requestLayout();
    }

    public float getMinTextSize() {
        return mMinTextSize;
    }

    public float getMaxTextSize() {
        return mMaxTextSize;
    }

    public void setTextSizeCache(Cache cache) {
        mCache = cache;
    }

    public Cache getTextSizeCache() {
        return mCache;
    }

    public void clearTextSizeCache() {
        mCache.clear();
    }

    @Override
    public void setTextSize(int unit, float size) {
        super.setTextSize(unit, size);
        mOriginalTextSize = getTextSize();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, mOriginalTextSize);
            mMeasured = false;
        } else {
            mMeasured = true;
            fitText();
        }
    }

    private boolean mFittingText;
    private void fitText() {
        if (!mMeasured || mFittingText) return;

        float low = mMinTextSize; float hi = mMaxTextSize;
        TextPaint paint = new TextPaint(getPaint());

        String key = null;
        if (mCache != null) {
            key = getText().toString();
            Float guessSize = mCache.get(key);
            if (guessSize != null) {
                // Protect calling back into fitText() when textSize is changed
                mFittingText = true;
                setTextSize(TypedValue.COMPLEX_UNIT_PX, guessSize);
                mFittingText = false;
                return;
            }
        }

        while (Math.abs(hi - low) > LIMIT) {
            paint.setTextSize((low + hi) / 2);
            if (textFits(paint)) {
                low = paint.getTextSize();
            } else {
                hi = paint.getTextSize();
            }
        }

        if (mCache!= null) {
            mCache.put(key, low);
        }

        // Protect calling back into fitText() when textSize is changed
        mFittingText = true;
        setTextSize(TypedValue.COMPLEX_UNIT_PX, low);
        mFittingText = false;
    }

    private boolean textFits(TextPaint paint) {
        int width = getMeasuredWidth() - getTotalPaddingLeft() - getTotalPaddingRight();
        int height = getMeasuredHeight() - getCompoundPaddingTop() - getCompoundPaddingBottom();
        int lines = mSingleLine ? 1 : getMaxLinesCompat();

        StaticLayout layout = new StaticLayout(getText(), paint, width,
                getLayoutAlignment(), getLineSPacingMultiplierCompat(), getLineSpacingExtraCompat(),
                getIncludeFontPaddingCompat());

        return layout.getLineCount() <= lines && layout.getHeight() <= height;
    }

    public boolean getIncludeFontPaddingCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return getIncludeFontPadding();
        } else {
            return mIncludeFontPadding;
        }
    }

    public float getLineSPacingMultiplierCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return getLineSpacingMultiplier();
        } else {
            return mLineSpacingMult;
        }
    }

    public float getLineSpacingExtraCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return getLineSpacingExtra();
        } else {
            return mLineSpacingAdd;
        }
    }

    public int getMaxLinesCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return getMaxLines();
        } else {
            return mMaxLines;
        }
    }

    @Override
    public void setLineSpacing(float add, float mult) {
        super.setLineSpacing(add, mult);
        mLineSpacingAdd = add; mLineSpacingMult = mult;
    }

    @Override
    public void setIncludeFontPadding(boolean includepad) {
        super.setIncludeFontPadding(includepad);
        mIncludeFontPadding = includepad;
    }

    @Override
    public void setMaxLines(int maxlines) {
        super.setMaxLines(maxlines);
        mMaxLines = maxlines;
    }

    @Override
    public void setSingleLine(boolean singleLine) {
        super.setSingleLine(singleLine);
        mSingleLine = singleLine;
    }

    @Override
    public void invalidate() {
        fitText();
        super.invalidate();
    }

    /**
     * Taken and slightly modified from Android source
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private Layout.Alignment getLayoutAlignment() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Layout.Alignment.ALIGN_NORMAL;
        }

        Layout.Alignment alignment;
        switch (getTextAlignment()) {
            case TEXT_ALIGNMENT_GRAVITY:
                switch (getGravity() & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.START:
                        alignment = Layout.Alignment.ALIGN_NORMAL;
                        break;
                    case Gravity.END:
                        alignment = Layout.Alignment.ALIGN_OPPOSITE;
                        break;
                    case Gravity.LEFT:
                            alignment = (getLayoutDirection() == LAYOUT_DIRECTION_RTL) ?
                                    Layout.Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_NORMAL;
                        break;
                    case Gravity.RIGHT:
                            alignment = (getLayoutDirection() == LAYOUT_DIRECTION_RTL) ?
                                    Layout.Alignment.ALIGN_NORMAL : Layout.Alignment.ALIGN_OPPOSITE;
                        break;
                    case Gravity.CENTER_HORIZONTAL:
                        alignment = Layout.Alignment.ALIGN_CENTER;
                        break;
                    default:
                        alignment = Layout.Alignment.ALIGN_NORMAL;
                        break;
                }
                break;
            case TEXT_ALIGNMENT_TEXT_START:
                alignment = Layout.Alignment.ALIGN_NORMAL;
                break;
            case TEXT_ALIGNMENT_TEXT_END:
                alignment = Layout.Alignment.ALIGN_OPPOSITE;
                break;
            case TEXT_ALIGNMENT_CENTER:
                alignment = Layout.Alignment.ALIGN_CENTER;
                break;
            case TEXT_ALIGNMENT_VIEW_START:
                alignment = Layout.Alignment.ALIGN_NORMAL;
                break;
            case TEXT_ALIGNMENT_VIEW_END:
                alignment = Layout.Alignment.ALIGN_OPPOSITE;
                break;
            case TEXT_ALIGNMENT_INHERIT:
                // This should never happen as we have already resolved the text alignment
                // but better safe than sorry so we just fall through
            default:
                alignment = Layout.Alignment.ALIGN_NORMAL;
                break;
        }
        return alignment;
    }

    public static final class Cache {
        private Map<String, Float> cache = new HashMap<String, Float>();

        protected Float get(String text) {
            return cache.get(text);
        }

        protected void put(String text, Float value) {
            cache.put(text, value);
        }

        protected void clear() {
            cache.clear();
        }
    }
}
