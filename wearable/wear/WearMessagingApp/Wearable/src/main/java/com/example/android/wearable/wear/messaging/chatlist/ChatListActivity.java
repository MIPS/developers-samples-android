/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.wearable.wear.messaging.chatlist;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WearableRecyclerView;
import android.util.Log;
import com.example.android.wearable.wear.messaging.GoogleSignedInActivity;
import com.example.android.wearable.wear.messaging.R;
import com.example.android.wearable.wear.messaging.chat.ChatActivity;
import com.example.android.wearable.wear.messaging.contacts.ContactsListActivity;
import com.example.android.wearable.wear.messaging.mock.MockDatabase;
import com.example.android.wearable.wear.messaging.model.Chat;
import com.example.android.wearable.wear.messaging.model.Profile;
import com.example.android.wearable.wear.messaging.util.Constants;
import com.example.android.wearable.wear.messaging.util.DividerItemDecoration;
import com.example.android.wearable.wear.messaging.util.SharedPreferencesHelper;
import java.util.ArrayList;

/**
 * Displays list of active chats of user.
 *
 * <p>Uses a simple mocked backend solution with shared preferences.
 *
 * <p>TODO: Processes database activities on the UI thread, move to async.
 */
public class ChatListActivity extends GoogleSignedInActivity {

    private static final String TAG = "ChatListActivity";

    // Triggered by contact selection in ContactsListActivity.
    private static final int CONTACTS_SELECTED_REQUEST_CODE = 9004;

    private WearableRecyclerView mRecyclerView;
    private ChatListAdapter mRecyclerAdapter;
    private Profile mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.BlueTheme);
        setContentView(R.layout.activity_chat_list);

        mRecyclerView = (WearableRecyclerView) findViewById(R.id.recycler_view);

        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, R.drawable.divider));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

        mRecyclerAdapter = new ChatListAdapter(this, new MyChatListAdapterListener(this));
        mRecyclerView.setAdapter(mRecyclerAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        // Try to get the user if they don't exist, return to login.
        mUser = SharedPreferencesHelper.readUserFromJsonPref(this);
        if (mUser == null) {
            Log.e(TAG, "User is not stored locally");
            onGoogleSignInFailure();
        } else {
            mRecyclerAdapter.setChats(MockDatabase.getAllChats(this));
        }
    }

    /**
     * A handler for the adapter to launch the correct activities based on the type of item selected
     */
    private class MyChatListAdapterListener implements ChatListAdapter.ChatAdapterListener {

        private final WearableActivity activity;

        MyChatListAdapterListener(WearableActivity activity) {
            this.activity = activity;
        }

        @Override
        public void newChatSelected() {
            Intent intent = new Intent(activity, ContactsListActivity.class);
            activity.startActivityForResult(intent, CONTACTS_SELECTED_REQUEST_CODE);
        }

        @Override
        public void openChat(Chat chat) {
            Intent startChat = new Intent(activity, ChatActivity.class);
            startChat.putExtra(Constants.EXTRA_CHAT, chat);
            activity.startActivity(startChat);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONTACTS_SELECTED_REQUEST_CODE) {
            // Selected contacts confirmed.
            if (resultCode == RESULT_OK) {
                ArrayList<Profile> contacts =
                        data.getParcelableArrayListExtra(Constants.RESULT_CONTACTS_KEY);
                // TODO: this should be moved to the background,
                Chat newChatWithSelectedContacts =
                        MockDatabase.createChat(this, contacts, getUser());

                Log.d(TAG, String.format("Starting chat with %d contact(s)", contacts.size()));

                // Launch ChatActivity with new chat.
                Intent startChat = new Intent(this, ChatActivity.class);
                startChat.putExtra(Constants.EXTRA_CHAT, newChatWithSelectedContacts);
                startActivity(startChat);
            }
        }
    }
}
