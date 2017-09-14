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
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.autofill.AutofillManager;
import android.widget.Toast;

import com.example.android.autofillframework.R;

/**
 * Activity used to demonstrated safe partitioning of data.
 *
 * <p>It has multiple partitions, but only accepts autofill on each partition at time.
 */
/*
 * TODO list
 *
 * - Fix top margin.
 * - Use a combo box to select which partition is visible (will require changes on CustomView to
 *   hide a partition).
 * - Use a dedicated TextView (instead of Toast) for error messages.
 * - Resize line height based on number of lines on screen.
 * - Use wrap_context to CustomView container.
 * - Use different background color (or borders) for each partition.
 * - Add more partitions (like address) - should match same partitions from service.
 * - Add more hints (like w3c ones) - should match same hints from service.
 */
public class MultiplePartitionsActivity extends AppCompatActivity {

    private ScrollableCustomVirtualView mCustomVirtualView;
    private AutofillManager mAutofillManager;

    private CustomVirtualView.Partition mCredentialsPartition;
    private CustomVirtualView.Partition mCcPartition;

    public static Intent getStartActivityIntent(Context context) {
        Intent intent = new Intent(context, MultiplePartitionsActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.multiple_partitions_activity);

        mCustomVirtualView = findViewById(R.id.custom_view);

        mCredentialsPartition =
                mCustomVirtualView.addPartition(getString(R.string.partition_credentials));
        mCredentialsPartition.addLine("username", getString(R.string.username_label),
                "         ", false, View.AUTOFILL_HINT_USERNAME);
        mCredentialsPartition.addLine("password", getString(R.string.password_label),
                "         ", true, View.AUTOFILL_HINT_PASSWORD);

        mCcPartition = mCustomVirtualView.addPartition(getString(R.string.partition_credit_card));
        mCcPartition.addLine("ccNumber", getString(R.string.credit_card_number_label),
                "         ", true, View.AUTOFILL_HINT_CREDIT_CARD_NUMBER);
        mCcPartition.addLine("ccDay", getString(R.string.credit_card_expiration_day_label),
                "         ", true, View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY);
        mCcPartition.addLine("ccMonth", getString(R.string.credit_card_expiration_month_label),
                "         ", true, View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH);
        mCcPartition.addLine("ccYear", getString(R.string.credit_card_expiration_year_label),
                "         ", true, View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR);
        // TODO: figure out why expiration date is not being autofilled
        mCcPartition.addLine("ccDate", getString(R.string.credit_card_expiration_date_label),
                "         ", true, View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE);
        mCcPartition.addLine("ccSecurityCode", getString(R.string.credit_card_security_code_label),
                "         ", true, View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE);

        // TODO: add ComboBox that changes visibility of partitions
        // mCcPartition.setVisibility(false);

        findViewById(R.id.clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetFields();
                mCustomVirtualView.resetPositions();
                mAutofillManager.cancel();
            }
        });
        mAutofillManager = getSystemService(AutofillManager.class);
    }

    private void resetFields() {
        mCredentialsPartition.reset();
        mCcPartition.reset();
        mCustomVirtualView.postInvalidate();
    }
}
