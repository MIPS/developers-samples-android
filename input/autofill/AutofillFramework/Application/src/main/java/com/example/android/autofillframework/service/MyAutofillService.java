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
package com.example.android.autofillframework.service;

import android.app.assist.AssistStructure;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveRequest;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.widget.RemoteViews;

import com.example.android.autofillframework.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.android.autofillframework.CommonUtil.TAG;
import static com.example.android.autofillframework.CommonUtil.bundleToString;

public class MyAutofillService extends AutofillService {

    @Override
    public void onFillRequest(AssistStructure assistStructure, Bundle bundle, int i,
            CancellationSignal cancellationSignal, FillCallback fillCallback) {
        /* Deprecated, ignore */
    }

    @Override
    public void onSaveRequest(AssistStructure assistStructure, Bundle bundle,
            SaveCallback saveCallback) {
        /* Deprecated, ignore */
    }

    @Override
    public void onFillRequest(FillRequest request, CancellationSignal cancellationSignal,
            FillCallback callback) {
        AssistStructure structure = request.getStructure();
        final Bundle data = request.getClientState();
        Log.d(TAG, "onFillRequest(): data=" + bundleToString(data));

        // Temporary hack for disabling autofill for components in this autofill service.
        if (structure.getActivityComponent().toShortString()
                .contains("com.example.android.autofillframework.service")) {
            callback.onSuccess(null);
            return;
        }
        cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
            @Override
            public void onCancel() {
                Log.w(TAG, "Cancel autofill not implemented in this sample.");
            }
        });
        // Parse AutoFill data in Activity
        StructureParser parser = new StructureParser(structure);
        parser.parse();
        AutofillId usernameId = parser.getUsernameField().getId();
        AutofillId passwordId = parser.getPasswordField().getId();

        FillResponse.Builder responseBuilder = new FillResponse.Builder();
        // Check user's settings for authenticating Responses and Datasets
        boolean responseAuth = MyPreferences.getInstance(this).isResponseAuth();
        if (responseAuth) {
            // If the entire Autofill Response is authenticated, AuthActivity is used
            // to generate Response
            IntentSender sender = AuthActivity.getAuthIntentSenderForResponse(this);
            RemoteViews presentation = new RemoteViews(getPackageName(), R.layout.list_item);
            presentation.setTextViewText(R.id.text1, getString(R.string.autofill_sign_in_prompt));
            responseBuilder.setAuthentication(new AutofillId[]{usernameId, passwordId},
                    sender, presentation);
            callback.onSuccess(responseBuilder.build());
        } else {
            boolean datasetAuth = MyPreferences.getInstance(this).isDatasetAuth();
            Map<String, LoginCredential> credentialsMap =
                    AutofillData.getInstance().getCredentialsMap(this);
            if (usernameId == null || passwordId == null ||
                    credentialsMap == null || credentialsMap.isEmpty()) {
                // Activity does not have usernameField and passwordField fields, or service does not
                // have any usernameField and passwordField autofill data.
                Log.d(TAG, "No Autofill data found for this Activity");
                callback.onSuccess(null);
                return;
            }

            FillResponse response = AutofillHelper.newCredentialsResponse(
                    this, datasetAuth, usernameId, passwordId, credentialsMap);
            callback.onSuccess(response);
        }
    }

    @Override
    public void onSaveRequest(SaveRequest request, SaveCallback callback) {
        List<FillContext> context = request.getFillContexts();
        final AssistStructure structure = context.get(context.size() - 1).getStructure();
        final Bundle data = request.getClientState();
        Log.d(TAG, "onSaveFillRequest(): data=" + bundleToString(data));
        StructureParser parser = new StructureParser(structure);
        parser.parse();
        String packageName = structure.getActivityComponent().getPackageName();
        String username = parser.getUsernameField().getValue();
        String password = parser.getPasswordField().getValue();
        LoginCredential loginCredential = new LoginCredential(username, password);
        AutofillData.getInstance().updateCredentials(packageName, loginCredential);
    }

    @Override
    public void onConnected() {
        Log.d(TAG, "onConnected");
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected");
    }
}
