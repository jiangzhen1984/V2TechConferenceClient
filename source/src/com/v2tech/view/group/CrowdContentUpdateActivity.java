package com.v2tech.view.group;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.Group.GroupType;

public class CrowdContentUpdateActivity extends Activity {

	public final static int UPDATE_TYPE_BRIEF = 1;
	public final static int UPDATE_TYPE_ANNOUNCEMENT = 2;

	private final static int REQUEST_UPDATE_CROWD_DONE = 1;

	private EditText mContentTV;
	private TextView mTitleTV;

	private View mUpdateButton;

	private CrowdGroup crowd;
	private CrowdGroupService service = new CrowdGroupService();
	private State mState = State.NONE;
	private int mType;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.crowd_content_activity);

		mContentTV = (EditText) findViewById(R.id.crowd_content_et);
		mUpdateButton = findViewById(R.id.crowd_content_update_button);
		mUpdateButton.setOnClickListener(mUpdateButtonListener);

		mTitleTV = (TextView) findViewById(R.id.crowd_content_title);
		crowd = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.CHATING.intValue(), getIntent().getExtras().getLong("cid"));

		mType = getIntent().getExtras().getInt("type");
		updateView(mType);
		overridePendingTransition(R.animator.left_in, R.animator.left_out);
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.animator.right_in, R.animator.right_out);
	}

	private void updateView(int type) {
		if (type == UPDATE_TYPE_BRIEF) {
			mContentTV.setText(crowd.getBrief());
			mTitleTV.setText(R.string.crowd_content_brief);
		} else if (type == UPDATE_TYPE_ANNOUNCEMENT) {
			mContentTV.setText(crowd.getAnnouncement());
			mTitleTV.setText(R.string.crowd_content_announce);
		}
	}

	private OnClickListener mUpdateButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			synchronized (mState) {
				if (mState == State.PENDING) {
					return;
				}
				mState = State.PENDING;
			}
			if (mType == UPDATE_TYPE_BRIEF) {
				crowd.setBrief(mContentTV.getText().toString());
			} else if (mType == UPDATE_TYPE_ANNOUNCEMENT) {
				crowd.setAnnouncement(mContentTV.getText().toString());
			}
			service.updateCrowd(crowd, new Registrant(mLocalHandler,
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
				Toast.makeText(CrowdContentUpdateActivity.this,
						R.string.crowd_content_udpate_succeed,
						Toast.LENGTH_SHORT).show();
				setResult(mType, null);
				finish();
				break;
			}
		}

	};

	enum State {
		NONE, PENDING;
	}

}
