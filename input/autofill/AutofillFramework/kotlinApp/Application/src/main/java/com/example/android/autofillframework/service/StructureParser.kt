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

import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.util.Log
import com.example.android.autofillframework.CommonUtil.TAG
import com.example.android.autofillframework.service.model.AutofillField
import com.example.android.autofillframework.service.model.AutofillFieldsCollection
import com.example.android.autofillframework.service.model.ClientFormData
import com.example.android.autofillframework.service.model.SavableAutofillData

/**
 * Parser for an AssistStructure object. This is invoked when the Autofill Service receives an
 * AssistStructure from the client Activity, representing its View hierarchy. In this sample, it
 * parses the hierarchy and collects autofill metadata from {@link ViewNode}s along the way.
 */
internal class StructureParser(private val mStructure: AssistStructure) {
    val autofillFields = AutofillFieldsCollection()
    var clientFormData: ClientFormData = ClientFormData()
        private set


    fun parseForFill() {
        parse(true)
    }

    fun parseForSave() {
        parse(false)
    }

    /**
     * Traverse AssistStructure and add ViewNode metadata to a flat list.
     */
    private fun parse(forFill: Boolean) {
        Log.d(TAG, "Parsing structure for " + mStructure.activityComponent)
        val nodes = mStructure.windowNodeCount
        clientFormData = ClientFormData()
        for (i in 0..nodes - 1) {
            val node = mStructure.getWindowNodeAt(i)
            val view = node.rootViewNode
            parseLocked(forFill, view)
        }
    }

    private fun parseLocked(forFill: Boolean, viewNode: ViewNode) {
        viewNode.autofillHints?.let { autofillHints ->
            if (autofillHints.isNotEmpty()) {
                if (forFill) {
                    autofillFields.add(AutofillField(viewNode))
                } else {
                    clientFormData.setAutofillValuesForHints(viewNode.autofillHints,
                            SavableAutofillData(viewNode))
                }
            }
        }
        val childrenSize = viewNode.childCount
        if (childrenSize > 0) {
            for (i in 0..childrenSize - 1) {
                parseLocked(forFill, viewNode.getChildAt(i))
            }
        }
    }
}
