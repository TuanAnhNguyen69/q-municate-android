package com.quickblox.qmunicate.ui.chats;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.quickblox.qmunicate.R;
import com.quickblox.qmunicate.caching.DatabaseManager;
import com.quickblox.qmunicate.model.Friend;

import java.util.ArrayList;

public class NewDialogActivity extends BaseSelectableFriendListActivity implements NewDialogCounterFriendsListener {

    public static void start(Context context) {
        Intent intent = new Intent(context, NewDialogActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected Cursor getFriends() {
        return DatabaseManager.getAllFriends(this);
    }

    @Override
    protected int getActionModeLayoutId() {
        return R.layout.action_mode_new_dialog;
    }

    @Override
    protected void onFriendsSelected(ArrayList<Friend> selectedFriends) {
        GroupDialogActivity.start(NewDialogActivity.this, selectedFriends);
        finish();
    }
}