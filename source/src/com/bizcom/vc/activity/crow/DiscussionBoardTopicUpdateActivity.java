package com.bizcom.vc.activity.crow;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.util.EscapedcharactersProcessing;
import com.bizcom.request.V2CrowdGroupRequest;
import com.bizcom.request.util.HandlerWrap;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vo.DiscussionGroup;
import com.bizcom.vo.Group.GroupType;
import com.v2tech.R;

/**
 * <ul>
 * Intent key:<br>
 *   cid  : discussion board id<br>
 * </ul>
 * @see PublicIntent#SHOW_DISCUSSION_BOARD_TOPIC_ACTIVITY
 * @author jiangzhen
 *
 */
public class DiscussionBoardTopicUpdateActivity extends Activity {


	private final static int REQUEST_UPDATE_CROWD_DONE = 1;

	private EditText mContentET;
	private TextView mReturnButton;

	private TextView mUpdateButton;

	private DiscussionGroup crowd;
	private V2CrowdGroupRequest service = new V2CrowdGroupRequest();
	private State mState = State.NONE;
	private Toast mToast;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.discussion_board_topic_update_activity);

		mContentET = (EditText) findViewById(R.id.dicussion_board_topic_et);

		mUpdateButton = (TextView) findViewById(R.id.discussion_board_topic_update_button);
		mUpdateButton.setOnClickListener(mUpdateButtonListener);

		mReturnButton = (TextView) findViewById(R.id.discussion_board_topic_return_button);
		mReturnButton.setOnClickListener(mReturnButtonListener);

		crowd = (DiscussionGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.DISCUSSION.intValue(),
				getIntent().getExtras().getLong("cid"));


		mContentET.setText(crowd.getName());
		overridePendingTransition(R.anim.left_in, R.anim.left_out);
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.right_in, R.anim.right_out);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		service.clearCalledBack();
	}

	


	private OnClickListener mReturnButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			onBackPressed();

		}

	};

	private OnClickListener mUpdateButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (!GlobalHolder.getInstance().isServerConnected()) {
				Toast.makeText(DiscussionBoardTopicUpdateActivity.this, R.string.error_local_connect_to_server, Toast.LENGTH_SHORT).show();
				return;
			}
			
			synchronized (mState) {
				if (mState == State.PENDING) {
					return;
				}
				mState = State.PENDING;
			}
			crowd.setName(EscapedcharactersProcessing.convert(mContentET.getText().toString()));
			service.updateDiscussion(crowd, new HandlerWrap(mLocalHandler,
					REQUEST_UPDATE_CROWD_DONE, null));
		}

	};

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case REQUEST_UPDATE_CROWD_DONE:
				synchronized (mState) {
					mState = State.NONE;
				}
				if (mToast != null) {
					mToast.cancel();
				}

				mToast = Toast.makeText(DiscussionBoardTopicUpdateActivity.this,
						R.string.crowd_content_udpate_succeed,
						Toast.LENGTH_SHORT);
				mToast.show();
				finish();
				break;
			}
		}

	};

	enum State {
		NONE, PENDING;
	}

}
