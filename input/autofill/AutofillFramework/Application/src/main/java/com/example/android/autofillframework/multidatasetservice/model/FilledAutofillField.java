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
package com.example.android.autofillframework.multidatasetservice.model;

import android.util.Log;
import android.view.View;
import android.view.autofill.AutofillValue;

import com.example.android.autofillframework.multidatasetservice.AutofillHelper;
import com.example.android.autofillframework.multidatasetservice.AutofillHints;
import com.google.gson.annotations.Expose;

import java.util.Arrays;

import static com.example.android.autofillframework.CommonUtil.TAG;

/**
 * JSON serializable data class containing the same data as an {@link AutofillValue}.
 */
public class FilledAutofillField {
    @Expose
    private String mTextValue = null;
    @Expose
    private Long mDateValue = null;
    @Expose
    private Boolean mToggleValue = null;

    /**
     * Does not need to be serialized into persistent storage, so it's not exposed.
     */
    private String[] mAutofillHints = null;

    public FilledAutofillField(String... hints) {
        mAutofillHints = AutofillHelper.filterForSupportedHints(hints);
    }

    public void setListValue(CharSequence[] autofillOptions, int listValue) {
        /* Only set list value when a hint is allowed to store list values. */
        if (AutofillHints.isValidTypeForHints(mAutofillHints, View.AUTOFILL_TYPE_LIST)) {
            if (autofillOptions != null && autofillOptions.length > 0) {
                mTextValue = autofillOptions[listValue].toString();
            }
        } else {
            Log.w(TAG, "List is invalid autofill type for hint(s) - " +
                    Arrays.toString(mAutofillHints));
        }
    }

    public String[] getAutofillHints() {
        return mAutofillHints;
    }

    public String getTextValue() {
        return mTextValue;
    }

    public void setTextValue(CharSequence textValue) {
        /* Only set text value when a hint is allowed to store text values. */
        if (AutofillHints.isValidTypeForHints(mAutofillHints, View.AUTOFILL_TYPE_TEXT)) {
            mTextValue = textValue.toString();
        } else {
            Log.w(TAG, "Text is invalid autofill type for hint(s) - " +
                    Arrays.toString(mAutofillHints));
        }
    }

    public Long getDateValue() {
        return mDateValue;
    }

    public void setDateValue(Long dateValue) {
        /* Only set date value when a hint is allowed to store date values. */
        if (AutofillHints.isValidTypeForHints(mAutofillHints, View.AUTOFILL_TYPE_DATE)) {
            mDateValue = dateValue;
        } else {
            Log.w(TAG, "Date is invalid autofill type for hint(s) - " +
                    Arrays.toString(mAutofillHints));
        }
    }

    public Boolean getToggleValue() {
        return mToggleValue;
    }

    public void setToggleValue(Boolean toggleValue) {
            /* Only set toggle value when a hint is allowed to store toggle values. */
        if (AutofillHints.isValidTypeForHints(mAutofillHints, View.AUTOFILL_TYPE_TOGGLE)) {
            mToggleValue = toggleValue;
        } else {
            Log.w(TAG, "Toggle is invalid autofill type for hint(s) - " +
                    Arrays.toString(mAutofillHints));
        }
    }

    public boolean isNull() {
        return mTextValue == null && mDateValue == null && mToggleValue == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilledAutofillField that = (FilledAutofillField) o;

        if (mTextValue != null ? !mTextValue.equals(that.mTextValue) : that.mTextValue != null)
            return false;
        if (mDateValue != null ? !mDateValue.equals(that.mDateValue) : that.mDateValue != null)
            return false;
        return mToggleValue != null ? mToggleValue.equals(that.mToggleValue) :
                that.mToggleValue == null;
    }

    @Override
    public int hashCode() {
        int result = mTextValue != null ? mTextValue.hashCode() : 0;
        result = 31 * result + (mDateValue != null ? mDateValue.hashCode() : 0);
        result = 31 * result + (mToggleValue != null ? mToggleValue.hashCode() : 0);
        return result;
    }
}
