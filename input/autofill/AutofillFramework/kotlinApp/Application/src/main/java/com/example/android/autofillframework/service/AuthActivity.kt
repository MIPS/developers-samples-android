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
package com.example.android.autofillframework.service

import android.app.Activity
import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.util.Log
import android.view.autofill.AutofillManager.EXTRA_ASSIST_STRUCTURE
import android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT
import android.widget.Toast
import com.example.android.autofillframework.CommonUtil.EXTRA_DATASET_NAME
import com.example.android.autofillframework.CommonUtil.EXTRA_FOR_RESPONSE
import com.example.android.autofillframework.CommonUtil.TAG
import com.example.android.autofillframework.R
import com.example.android.autofillframework.service.datasource.SharedPrefsAutofillRepository
import com.example.android.autofillframework.service.settings.MyPreferences
import kotlinx.android.synthetic.main.auth_activity.cancel
import kotlinx.android.synthetic.main.auth_activity.login
import kotlinx.android.synthetic.main.auth_activity.master_password

/**
 * This Activity controls the UI for logging in to the Autofill service.
 * It is launched when an Autofill Response or specific Dataset within the Response requires
 * authentication to access. It bundles the result in an Intent.
 */
class AuthActivity : Activity() {

    private var mReplyIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.auth_activity)
        login.setOnClickListener { submitLogin() }
        cancel.setOnClickListener {
            onFailure()
            this@AuthActivity.finish()
        }
    }

    private fun submitLogin() {
        val password = master_password.text
        if (password.toString() == MyPreferences.getMasterPassword(this@AuthActivity)) {
            onSuccess()
        } else {
            Toast.makeText(this, "Password incorrect", Toast.LENGTH_SHORT).show()
            onFailure()
        }
        finish()
    }

    override fun finish() {
        if (mReplyIntent != null) {
            setResult(Activity.RESULT_OK, mReplyIntent)
        } else {
            setResult(Activity.RESULT_CANCELED)
        }
        super.finish()
    }

    private fun onFailure() {
        Log.w(TAG, "Failed auth.")
        mReplyIntent = null
    }

    private fun onSuccess() {
        val intent = intent
        val forResponse = intent.getBooleanExtra(EXTRA_FOR_RESPONSE, true)
        val structure = intent.getParcelableExtra<AssistStructure>(EXTRA_ASSIST_STRUCTURE)
        val parser = StructureParser(structure)
        parser.parseForFill()
        val autofillFields = parser.autofillFields
        mReplyIntent = Intent()
        val clientFormDataMap = SharedPrefsAutofillRepository
                .getClientFormData(this, autofillFields.focusedAutofillHints, autofillFields.allAutofillHints)
        if (forResponse) {
            AutofillHelper.newResponse(this, false, autofillFields, clientFormDataMap)?.let(this::setResponseIntent)
        } else {
            val datasetName = intent.getStringExtra(EXTRA_DATASET_NAME)
            clientFormDataMap?.let {
                it[datasetName]?.let {
                    AutofillHelper.newDataset(this, autofillFields, it)?.let(this::setDatasetIntent)
                }
            }
        }
    }

    private fun setResponseIntent(fillResponse: FillResponse) {
        mReplyIntent?.putExtra(EXTRA_AUTHENTICATION_RESULT, fillResponse)
    }

    private fun setDatasetIntent(dataset: Dataset) {
        mReplyIntent?.putExtra(EXTRA_AUTHENTICATION_RESULT, dataset)
    }

    companion object {

        // Unique autofillId for dataset intents.
        private var sDatasetPendingIntentId = 0

        internal fun getAuthIntentSenderForResponse(context: Context): IntentSender {
            val intent = Intent(context, AuthActivity::class.java)
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
                    .intentSender
        }

        internal fun getAuthIntentSenderForDataset(context: Context, datasetName: String): IntentSender {
            val intent = Intent(context, AuthActivity::class.java)
            intent.putExtra(EXTRA_DATASET_NAME, datasetName)
            intent.putExtra(EXTRA_FOR_RESPONSE, false)
            return PendingIntent.getActivity(context, ++sDatasetPendingIntentId, intent,
                    PendingIntent.FLAG_CANCEL_CURRENT).intentSender
        }
    }
}
