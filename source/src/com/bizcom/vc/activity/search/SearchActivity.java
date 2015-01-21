package com.bizcom.vc.activity.search;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bizcom.request.CrowdGroupService;
import com.bizcom.request.MessageListener;
import com.bizcom.request.SearchService;
import com.bizcom.request.UserService;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.util.ProgressUtils;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vo.SearchedResult;
import com.v2tech.R;

public class SearchActivity extends Activity {

	private static final int SEARCH_DONE = 1;

	private State mState = State.DONE;
	private EditText mSearchedText;
	private TextView mSearchButton;
	private TextView mReturnButton;
	private TextView mTitleText;
	private TextView mContentText;
	private TextView mEditTextBelowText;

	private UserService usService = new UserService();
	private CrowdGroupService crowdService = new CrowdGroupService();

	private Type mType = Type.CROWD;

	private SearchService searchService = new SearchService();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_activity);
		mSearchedText = (EditText) findViewById(R.id.search_text);
		mSearchButton = (TextView) findViewById(R.id.search_search_button);
		mSearchButton.setOnClickListener(mSearchButtonListener);
		mReturnButton = (TextView) findViewById(R.id.search_title_return_button);
		mReturnButton.setOnClickListener(mReturnButtonListener);

		mTitleText = (TextView) findViewById(R.id.search_title_text);
		mContentText = (TextView) findViewById(R.id.search_content_text);
		mEditTextBelowText = (TextView) findViewById(R.id.search_below_hit);

		int type = getIntent().getIntExtra("type", 0);
		if (type == 0) {
			mType = Type.CROWD;
		} else if (type == 1) {
			mType = Type.MEMBER;
		}
		initView(mType);
		overridePendingTransition(R.animator.left_in, R.animator.left_out);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.animator.right_in, R.animator.right_out);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		usService.clearCalledBack();
		crowdService.clearCalledBack();
	}

	private void initView(Type type) {
		int titleRid = R.string.search_title_crowd;
		int contentRid = R.string.search_content_text_crowd;
		int belowHit = R.string.search_crowd_rules_tips;
		switch (type) {
		case CROWD:
			titleRid = R.string.search_title_crowd;
			contentRid = R.string.search_content_text_crowd;
			belowHit = R.string.search_crowd_rules_tips;
			break;
		case MEMBER:
			titleRid = R.string.search_title_member;
			contentRid = R.string.search_content_text_member;
			belowHit = R.string.search_rules_tips;
			break;
		default:
			break;
		}

		mTitleText.setText(titleRid);
		mContentText.setText(contentRid);
		mEditTextBelowText.setText(belowHit);
	}
	
	private void showToast(int res) {
		Toast.makeText(this, res, Toast.LENGTH_SHORT).show();
	}

	private OnClickListener mSearchButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			synchronized (mState) {
				if (mState == State.SEARCHING) {
					return;
				}

				if (TextUtils.isEmpty(mSearchedText.getText())) {
					mSearchedText.setError(SearchActivity.this
							.getText(R.string.search_text_required));
					return;
				}
				
				if (!GlobalHolder.getInstance().isServerConnected()) {
					Toast.makeText(SearchActivity.this, R.string.error_local_connect_to_server, Toast.LENGTH_SHORT).show();
					return ;
				}
				mState = State.SEARCHING;

			}

			SearchService.SearchParameter par = searchService
					.generateSearchPatameter(
							mType == Type.CROWD ? SearchService.Type.CROWD
									: SearchService.Type.MEMBER, mSearchedText
									.getText().toString(), 1);
			ProgressUtils.showNormalWithHintProgress(SearchActivity.this,
					getResources().getString(R.string.status_searching), true);
			searchService.search(par, new MessageListener(mLocalHandler,
					SEARCH_DONE, null));

		}

	};

	private OnClickListener mReturnButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			onBackPressed();

		}

	};

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SEARCH_DONE:
				ProgressUtils.showNormalWithHintProgress(SearchActivity.this,
						getResources().getString(R.string.error_local_connect_to_server), false);
				synchronized (mState) {
					mState = State.DONE;
				}
				JNIResponse jni = (JNIResponse) msg.obj;
				if (jni.getResult() == JNIResponse.Result.SUCCESS) {
					SearchedResult result = (SearchedResult)jni.resObj;
					if (result.getList().size() <= 0) {
						switch (mType) {
						case CROWD:
							showToast(R.string.search_result_toast_no_crowd_entry);
							break;
						case MEMBER:
							showToast(R.string.search_result_toast_no_member_entry);
							break;
						}
					} else {
						Intent i = new Intent();
						i.setClass(getApplicationContext(), SearchedResultActivity.class);
						i.putExtra("result", result);
						startActivity(i);
					}
				} else {
					showToast(R.string.search_result_toast_error);
				}
				break;
			}
		}

	};

	enum State {
		DONE, SEARCHING,
	}

	enum Type {
		CROWD, MEMBER;
	}
}
