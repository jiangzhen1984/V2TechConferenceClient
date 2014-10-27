package com.v2tech.view.search;

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

import com.v2tech.R;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.Registrant;
import com.v2tech.service.SearchService;
import com.v2tech.service.UserService;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.vo.SearchedResult;

public class SearchActivity extends Activity {

	private static final int SEARCH_DONE = 1;

	private State mState = State.DONE;
	private EditText mSearchedText;
	private TextView mSearchButton;
	private TextView mReturnButton;
	private TextView mTitleText;
	private TextView mContentText;

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
		switch (type) {
		case CROWD:
			titleRid = R.string.search_title_crowd;
			contentRid = R.string.search_content_text_crowd;
			break;
		case MEMBER:
			titleRid = R.string.search_title_member;
			contentRid = R.string.search_content_text_member;
			break;
		default:
			break;
		}

		mTitleText.setText(titleRid);
		mContentText.setText(contentRid);
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
				mState = State.SEARCHING;

			}

			SearchService.SearchParameter par = searchService
					.generateSearchPatameter(
							mType == Type.CROWD ? SearchService.Type.CROWD
									: SearchService.Type.MEMBER, mSearchedText
									.getText().toString(), 1);

			searchService.search(par, new Registrant(mLocalHandler,
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
