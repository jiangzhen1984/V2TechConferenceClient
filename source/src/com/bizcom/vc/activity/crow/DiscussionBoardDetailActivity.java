package com.bizcom.vc.activity.crow;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bizcom.request.CrowdGroupService;
import com.bizcom.request.MessageListener;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vo.DiscussionGroup;
import com.bizcom.vo.Group.GroupType;
import com.v2tech.R;

/**
 * <ul>
 * Display discussion board detail.
 * </ul>
 * <ul>
 * Intent key: cid : discussion board id
 * </ul>
 * 
 * @see PublicIntent#SHOW_DISCUSSION_BOARD_DETAIL_ACTIVITY
 * @author 28851274
 * 
 */
public class DiscussionBoardDetailActivity extends Activity {

	private final static int TYPE_UPDATE_MEMBERS = 3;
	private final static int REQUEST_QUIT_CROWD_DONE = 2;

	private TextView mNameTV;
	private TextView mMembersCountsTV;

	private View mQuitButton;
	private View mShowTopicButton;
	private View mShowMembersButton;
	private View mReturnButton;

	private TextView mDialogTitleTV;

	private DiscussionGroup crowd;
	private CrowdGroupService service = new CrowdGroupService();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.discussion_board_detail_activity);

		mNameTV = (TextView) findViewById(R.id.discussion_board_detail_name);
		mMembersCountsTV = (TextView) findViewById(R.id.discussion_board_detail_members);

		mQuitButton = findViewById(R.id.discussion_board_detail_button);
		mQuitButton.setOnClickListener(mQuitButtonListener);
		mReturnButton = findViewById(R.id.discussion_board_detail_return_button);
		mReturnButton.setOnClickListener(mReturnButtonListener);

		mShowTopicButton = findViewById(R.id.discussion_board_detail_update_name_button);
		mShowTopicButton.setOnClickListener(mTopicButtonListener);

		mShowMembersButton = findViewById(R.id.discussion_board_detail_invitation_members_button);
		mShowMembersButton.setOnClickListener(mShowMembersButtonListener);

		crowd = (DiscussionGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.DISCUSSION.intValue(),
				getIntent().getLongExtra("cid", 0));

		mNameTV.setText(crowd.getName());

		mMembersCountsTV.setText(crowd.getUsers().size() + "");

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		mNameTV.setText(crowd.getName());
		mMembersCountsTV.setText(crowd.getUsers().size() + "");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		service.clearCalledBack();
	}

	private Dialog mDialog;

	private void showDialog() {
		if (mDialog == null) {

			mDialog = new Dialog(this, R.style.ContactUserActionDialog);

			mDialog.setContentView(R.layout.crowd_quit_confirmation_dialog);
			final Button cancelB = (Button) mDialog
					.findViewById(R.id.contacts_group_cancel_button);
			cancelB.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mDialog.dismiss();
				}

			});
			final Button confirmButton = (Button) mDialog
					.findViewById(R.id.contacts_group_confirm_button);
			confirmButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!GlobalHolder.getInstance().isServerConnected()) {
						mDialog.dismiss();
						Toast.makeText(DiscussionBoardDetailActivity.this,
								R.string.error_discussion_no_network,
								Toast.LENGTH_SHORT).show();
						return;
					}
					service.quitDiscussionBoard(crowd, new MessageListener(
							mLocalHandler, REQUEST_QUIT_CROWD_DONE, null));
					Intent kick = new Intent();
					kick.setAction(JNIService.JNI_BROADCAST_KICED_CROWD);
					kick.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
					kick.putExtra("crowd", crowd.getmGId());
					DiscussionBoardDetailActivity.this.sendBroadcast(kick);
				}

			});

			mDialogTitleTV = (TextView) mDialog
					.findViewById(R.id.crowd_quit_dialog_title);
		}

		mDialogTitleTV
				.setText(R.string.discussion_board_detail_quit_confirm_title);
		mDialog.show();
	}

	private void handleQuitDone() {
		// Remove cache crowd
		GlobalHolder.getInstance().removeGroup(GroupType.DISCUSSION,
				crowd.getmGId());
		// send broadcast to notify conversationtabfragment refresh list
		Intent i = new Intent(
				PublicIntent.BROADCAST_DISCUSSION_QUIT_NOTIFICATION);
		i.addCategory(PublicIntent.DEFAULT_CATEGORY);
		i.putExtra("userId", GlobalHolder.getInstance().getCurrentUserId());
		i.putExtra("groupId", crowd.getmGId());
		sendBroadcast(i);
		onBackPressed();
	}

	private OnClickListener mQuitButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			showDialog();
		}

	};

	private OnClickListener mReturnButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			onBackPressed();
		}

	};

	private OnClickListener mTopicButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			Intent i = new Intent(
					PublicIntent.SHOW_DISCUSSION_BOARD_TOPIC_ACTIVITY);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			i.putExtra("cid", crowd.getmGId());
			startActivityForResult(i, 100);
		}

	};

	private OnClickListener mShowMembersButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			Intent i = new Intent(
					PublicIntent.SHOW_DISCUSSION_BOARD_MEMBERS_ACTIVITY);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			i.putExtra("cid", crowd.getmGId());
			startActivityForResult(i, TYPE_UPDATE_MEMBERS);
		}

	};

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case REQUEST_QUIT_CROWD_DONE:
				JNIResponse res = (JNIResponse) msg.obj;
				if (res.getResult() == JNIResponse.Result.SUCCESS) {
					handleQuitDone();
				} else {
					Toast.makeText(DiscussionBoardDetailActivity.this,
							R.string.error_discussion_board_quit_failed,
							Toast.LENGTH_SHORT).show();
				}
				if (mDialog != null) {
					mDialog.dismiss();
				}
				break;

			}
		}

	};

	enum State {
		NONE, PENDING;
	}

}
