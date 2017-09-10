/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.autofillframework.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStructure;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import static com.example.android.autofillframework.CommonUtil.bundleToString;

/**
 * Custom View with virtual child views for Username/Password text fields.
 */
public class CustomVirtualView extends View {

    private static final String TAG = "CustomView";
    private static final boolean DEBUG = true;
    private static final boolean VERBOSE = false;

    private static final int TOP_MARGIN = 100;
    private static final int LEFT_MARGIN = 100;
    private static final int TEXT_HEIGHT = 90;
    private static final int VERTICAL_GAP = 10;
    private static final int LINE_HEIGHT = TEXT_HEIGHT + VERTICAL_GAP;
    private static final int UNFOCUSED_COLOR = Color.BLACK;
    private static final int FOCUSED_COLOR = Color.RED;
    private static int sNextId;

    private final ArrayList<Line> mVirtualViewGroups = new ArrayList<>();
    private final SparseArray<Item> mVirtualViews = new SparseArray<>();
    private final AutofillManager mAutofillManager;

    private Line mFocusedLine;
    private Paint mTextPaint;

    public CustomVirtualView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAutofillManager = context.getSystemService(AutofillManager.class);
        mTextPaint = new Paint();
        mTextPaint.setStyle(Style.FILL);
        mTextPaint.setTextSize(TEXT_HEIGHT);
    }

    @Override
    public void autofill(SparseArray<AutofillValue> values) {
        // User has just selected a Dataset from the list of autofill suggestions.
        // The Dataset is comprised of a list of AutofillValues, with each AutofillValue meant
        // to fill a specific autofillable view. Now we have to update the UI based on the
        // AutofillValues in the list.
        if (DEBUG) Log.d(TAG, "autofill(): " + values);
        for (int i = 0; i < values.size(); i++) {
            int id = values.keyAt(i);
            AutofillValue value = values.valueAt(i);
            Item item = mVirtualViews.get(id);
            if (item != null && item.editable) {
                // Set the item's text to the text wrapped in the AutofillValue.
                item.text = value.getTextValue();
            } else if (item == null) {
                Log.w(TAG, "No item for id " + id);
            } else {
                Log.w(TAG, "Item for id " + id + " is not editable: " + item);
            }
        }
        postInvalidate();
    }

    @Override
    public void onProvideAutofillVirtualStructure(ViewStructure structure, int flags) {
        // Build a ViewStructure that will get passed to the AutofillService by the framework
        // when it is time to find autofill suggestions.
        structure.setClassName(getClass().getName());
        int childrenSize = mVirtualViews.size();
        if (DEBUG) {
            Log.d(TAG, "onProvideAutofillVirtualStructure(): flags = " + flags + ", items = "
                    + childrenSize + ", extras: " + bundleToString(structure.getExtras()));
        }
        int index = structure.addChildCount(childrenSize);
        // Traverse through the view hierarchy, including virtual child views. For each view, we
        // need to set the relevant autofill metadata and add it to the ViewStructure.
        for (int i = 0; i < childrenSize; i++) {
            Item item = mVirtualViews.valueAt(i);
            if (DEBUG) Log.d(TAG, "Adding new child at index " + index + ": " + item);
            ViewStructure child = structure.newChild(index);
            child.setAutofillId(structure.getAutofillId(), item.id);
            child.setAutofillHints(item.hints);
            child.setAutofillType(item.type);
            child.setDataIsSensitive(!item.sanitized);
            if (TextUtils.getTrimmedLength(item.text) > 0) {
                child.setAutofillValue(AutofillValue.forText(item.text));
            }
            child.setFocused(item.focused);
            child.setId(item.id, getContext().getPackageName(), null, item.line.mIdEntry);
            child.setClassName(item.getClassName());
            index++;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (VERBOSE) {
            Log.v(TAG, "onDraw(): " + mVirtualViewGroups.size() + " lines; canvas:" + canvas);
        }
        float x;
        float y = TOP_MARGIN + LINE_HEIGHT;
        for (int i = 0; i < mVirtualViewGroups.size(); i++) {
            x = LEFT_MARGIN;
            Line line = mVirtualViewGroups.get(i);
            if (VERBOSE) Log.v(TAG, "Drawing '" + line + "' at " + x + "x" + y);
            mTextPaint.setColor(line.mFieldTextItem.focused ? FOCUSED_COLOR : UNFOCUSED_COLOR);
            String readOnlyText = line.mLabelItem.text + ":  [";
            String writeText = line.mFieldTextItem.text + "]";
            // Paints the label first...
            canvas.drawText(readOnlyText, x, y, mTextPaint);
            // ...then paints the edit text and sets the proper boundary
            float deltaX = mTextPaint.measureText(readOnlyText);
            x += deltaX;
            line.mBounds.set((int) x, (int) (y - LINE_HEIGHT),
                    (int) (x + mTextPaint.measureText(writeText)), (int) y);
            if (VERBOSE) Log.v(TAG, "setBounds(" + x + ", " + y + "): " + line.mBounds);
            canvas.drawText(writeText, x, y, mTextPaint);
            y += LINE_HEIGHT;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int y = (int) event.getY();
        if (DEBUG) Log.d(TAG, "Touched: y=" + y + ", range=" + LINE_HEIGHT + ", top=" + TOP_MARGIN);
        int lowerY = TOP_MARGIN;
        int upperY = -1;
        for (int i = 0; i < mVirtualViewGroups.size(); i++) {
            upperY = lowerY + LINE_HEIGHT;
            Line line = mVirtualViewGroups.get(i);
            if (DEBUG) Log.d(TAG, "Line " + i + " ranges from " + lowerY + " to " + upperY);
            if (lowerY <= y && y <= upperY) {
                if (mFocusedLine != null) {
                    Log.d(TAG, "Removing focus from " + mFocusedLine);
                    mFocusedLine.changeFocus(false);
                }
                Log.d(TAG, "Changing focus to " + line);
                mFocusedLine = line;
                mFocusedLine.changeFocus(true);
                invalidate();
                break;
            }
            lowerY += LINE_HEIGHT;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Adds a new line (containining a label and an input field) to the view.
     *
     * @param idEntry id used to identify the line.
     * @param label text used in the label.
     * @param text initial text used in the input field.
     * @param sensitive whether the input is considered sensitive.
     * @param hints list of autofill hints.
     *
     * @return the new line.
     */
    public Line addLine(String idEntry, String label, String text, boolean sensitive,
            String... hints) {
        Line line = new Line(idEntry, label, hints, text, !sensitive);
        mVirtualViewGroups.add(line);
        mVirtualViews.put(line.mLabelItem.id, line.mLabelItem);
        mVirtualViews.put(line.mFieldTextItem.id, line.mFieldTextItem);
        return line;
    }

    private static final class Item {
        private final Line line;
        private final int id;
        private final boolean editable;
        private final boolean sanitized;
        private final String[] hints;
        private final int type;
        private CharSequence text;
        private boolean focused = false;

        Item(Line line, int id, String[] hints, int type, CharSequence text, boolean editable,
                boolean sanitized) {
            this.line = line;
            this.id = id;
            this.text = text;
            this.editable = editable;
            this.sanitized = sanitized;
            this.hints = hints;
            this.type = type;
        }

        @Override
        public String toString() {
            return id + ": " + text + (editable ? " (editable)" : " (read-only)"
                    + (sanitized ? " (sanitized)" : " (sensitive"));
        }

        public String getClassName() {
            return editable ? EditText.class.getName() : TextView.class.getName();
        }
    }

    /**
     * A line in the virtual view contains a label and an input field.
     */
    public final class Line {

        // Boundaries of the text field, relative to the CustomView
        private final Rect mBounds = new Rect();
        private final Item mLabelItem;
        private final Item mFieldTextItem;
        private final String mIdEntry;

        private Line(String idEntry, String label, String[] hints, String text, boolean sanitized) {
            this.mIdEntry = idEntry;
            this.mLabelItem = new Item(this, ++sNextId, null, AUTOFILL_TYPE_NONE, label,
                    false, true);
            this.mFieldTextItem = new Item(this, ++sNextId, hints, AUTOFILL_TYPE_TEXT, text,
                    true, sanitized);
        }

        private void changeFocus(boolean focused) {
            mFieldTextItem.focused = focused;
            if (focused) {
                Rect absBounds = getAbsCoordinates();
                if (DEBUG) {
                    Log.d(TAG, "focus gained on " + mFieldTextItem.id + "; absBounds=" + absBounds);
                }
                mAutofillManager.notifyViewEntered(CustomVirtualView.this, mFieldTextItem.id,
                        absBounds);
            } else {
                if (DEBUG) Log.d(TAG, "focus lost on " + mFieldTextItem.id);
                mAutofillManager.notifyViewExited(CustomVirtualView.this, mFieldTextItem.id);
            }
        }

        private Rect getAbsCoordinates() {
            // Must offset the boundaries so they're relative to the CustomView.
            int offset[] = new int[2];
            getLocationOnScreen(offset);
            Rect absBounds = new Rect(mBounds.left + offset[0],
                    mBounds.top + offset[1],
                    mBounds.right + offset[0], mBounds.bottom + offset[1]);
            if (VERBOSE) {
                Log.v(TAG, "getAbsCoordinates() for " + mFieldTextItem.id + ": bounds=" + mBounds
                        + " offset: " + Arrays.toString(offset) + " absBounds: " + absBounds);
            }
            return absBounds;
        }

        /**
         * Gets the value of the input field text.
         */
        public CharSequence getText() {
            return mFieldTextItem.text;
        }

        /**
         * Resets the value of the input field text.
         */
        public void reset() {
            mFieldTextItem.text = "        ";
        }

        @Override
        public String toString() {
            return "Label: " + mLabelItem + " Text: " + mFieldTextItem + " Focused: " +
                    mFieldTextItem.focused;
        }
    }
}