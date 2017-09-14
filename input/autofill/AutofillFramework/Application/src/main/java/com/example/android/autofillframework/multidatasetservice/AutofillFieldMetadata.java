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
package com.example.android.autofillframework.multidatasetservice;

import android.app.assist.AssistStructure.ViewNode;
import android.service.autofill.SaveInfo;
import android.view.View;
import android.view.autofill.AutofillId;

/**
 * A stripped down version of a {@link ViewNode} that contains only autofill-relevant metadata. It
 * also contains a {@code mSaveType} flag that is calculated based on the {@link ViewNode}]'s
 * autofill hints.
 */
public class AutofillFieldMetadata {
    private int mSaveType = 0;
    private String[] mAutofillHints;
    private AutofillId mAutofillId;
    private int mAutofillType;
    private CharSequence[] mAutofillOptions;
    private boolean mFocused;

    public AutofillFieldMetadata(ViewNode view) {
        mAutofillId = view.getAutofillId();
        mAutofillType = view.getAutofillType();
        mAutofillOptions = view.getAutofillOptions();
        mFocused = view.isFocused();
        setHints(AutofillHelper.filterForSupportedHints(view.getAutofillHints()));
    }

    public String[] getHints() {
        return mAutofillHints;
    }

    public void setHints(String[] hints) {
        mAutofillHints = hints;
        updateSaveTypeFromHints();
    }

    public int getSaveType() {
        return mSaveType;
    }

    public AutofillId getId() {
        return mAutofillId;
    }

    public int getAutofillType() {
        return mAutofillType;
    }

    /**
     * When the {@link ViewNode} is a list that the user needs to choose a string from (i.e. a
     * spinner), this is called to return the index of a specific item in the list.
     */
    public int getAutofillOptionIndex(String value) {
        for (int i = 0; i < mAutofillOptions.length; i++) {
            if (mAutofillOptions[i].toString().equals(value)) {
                return i;
            }
        }
        return -1;
    }

    public boolean isFocused() {
        return mFocused;
    }

    private void updateSaveTypeFromHints() {
        mSaveType = 0;
        if (mAutofillHints == null) {
            return;
        }
        for (String hint : mAutofillHints) {
            switch (hint) {
                case View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE:
                case View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY:
                case View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH:
                case View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR:
                case View.AUTOFILL_HINT_CREDIT_CARD_NUMBER:
                case View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE:
                case W3cHints.CC_NAME:
                case W3cHints.CC_GIVEN_NAME:
                case W3cHints.CC_ADDITIONAL_NAME:
                case W3cHints.CC_FAMILY_NAME:
                case W3cHints.CC_NUMBER:
                case W3cHints.CC_EXPIRATION:
                case W3cHints.CC_EXPIRATION_MONTH:
                case W3cHints.CC_EXPIRATION_YEAR:
                case W3cHints.CC_CSC:
                case W3cHints.CC_TYPE:
                case W3cHints.TRANSACTION_CURRENCY:
                case W3cHints.TRANSACTION_AMOUNT:
                    mSaveType |= SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD;
                    break;
                case View.AUTOFILL_HINT_EMAIL_ADDRESS:
                case W3cHints.EMAIL:
                    mSaveType |= SaveInfo.SAVE_DATA_TYPE_EMAIL_ADDRESS;
                    break;
                case View.AUTOFILL_HINT_PASSWORD:
                case W3cHints.NEW_PASSWORD:
                case W3cHints.CURRENT_PASSWORD:
                    mSaveType |= SaveInfo.SAVE_DATA_TYPE_PASSWORD;
                    mSaveType &= ~SaveInfo.SAVE_DATA_TYPE_EMAIL_ADDRESS;
                    mSaveType &= ~SaveInfo.SAVE_DATA_TYPE_USERNAME;
                    break;
                case View.AUTOFILL_HINT_POSTAL_ADDRESS:
                case View.AUTOFILL_HINT_POSTAL_CODE:
                case W3cHints.STREET_ADDRESS:
                case W3cHints.ADDRESS_LINE1:
                case W3cHints.ADDRESS_LINE2:
                case W3cHints.ADDRESS_LINE3:
                case W3cHints.ADDRESS_LEVEL4:
                case W3cHints.ADDRESS_LEVEL3:
                case W3cHints.ADDRESS_LEVEL2:
                case W3cHints.ADDRESS_LEVEL1:
                case W3cHints.COUNTRY:
                case W3cHints.COUNTRY_NAME:
                case W3cHints.POSTAL_CODE:
                    mSaveType |= SaveInfo.SAVE_DATA_TYPE_ADDRESS;
                    break;
                case View.AUTOFILL_HINT_NAME:
                case View.AUTOFILL_HINT_PHONE:
                case View.AUTOFILL_HINT_USERNAME:
                case W3cHints.HONORIFIC_PREFIX:
                case W3cHints.GIVEN_NAME:
                case W3cHints.ADDITIONAL_NAME:
                case W3cHints.FAMILY_NAME:
                case W3cHints.HONORIFIC_SUFFIX:
                    mSaveType |= SaveInfo.SAVE_DATA_TYPE_USERNAME;
                    break;
                default:
                    mSaveType |= SaveInfo.SAVE_DATA_TYPE_GENERIC;
            }
        }
    }
}
