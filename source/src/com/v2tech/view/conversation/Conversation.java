package com.v2tech.view.conversation;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.logic.User;
import com.v2tech.logic.VMessage;
import com.v2tech.view.JNIService;
import com.v2tech.view.JNIService.LocalBinder;
import com.v2tech.view.cus.ItemScrollView;
import com.v2tech.view.cus.ScrollViewListener;

public class Conversation extends Activity {

	private final int START_LOAD_MESSAGE = 1;
	private final int LOAD_MESSAGE = 2;
	private final int END_LOAD_MESSAGE = 3;
	private final int SEND_MESSAGE = 4;
	private final int SEND_MESSAGE_DONE = 5;

	private final int BATCH_COUNT = 10;

	private int offset = 0;

	private LinearLayout mMessagesContainer;

	private ItemScrollView mScrollView;

	private long user1Id;

	private long user2Id;

	private LocalHandler lh;

	private JNIService mService;
	private boolean isBound;

	private boolean isLoading;

	private boolean mLoadedAllMessages;

	private Context mContext;

	private TextView mSendButtonTV;

	private TextView mReturnButtonTV;

	private EditText mMessageET;

	private ImageView mLoadingImg;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.activity_contact_message);
		mMessagesContainer = (LinearLayout) findViewById(R.id.conversation_message_list);

		mScrollView = (ItemScrollView) findViewById(R.id.conversation_message_list_scroll_view);
		mScrollView.setScrollListener(scrollListener);

		mSendButtonTV = (TextView) findViewById(R.id.message_send);
		mSendButtonTV.setOnClickListener(sendMessageListener);

		mMessageET = (EditText) findViewById(R.id.message_text);
		mReturnButtonTV = (TextView) findViewById(R.id.contact_detail_return_button);
		mReturnButtonTV.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				finish();
			}

		});

		user1Id = this.getIntent().getLongExtra("user1id", 0);
		user1Id = this.getIntent().getLongExtra("user2id", 0);
		user1Id = 1;
		user2Id = 2;
		if (user1Id == 0 || user2Id == 0) {
			Toast.makeText(this, R.string.error_contact_messag_invalid_user_id,
					Toast.LENGTH_SHORT).show();
			return;
		}

		lh = new LocalHandler();
	}

	@Override
	protected void onStart() {
		super.onStart();
		isBound = bindService(new Intent(this.getApplicationContext(),
				JNIService.class), mConnection, Context.BIND_AUTO_CREATE);
		android.os.Message m = android.os.Message
				.obtain(lh, START_LOAD_MESSAGE);
		lh.sendMessageDelayed(m, 500);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (isBound) {
			this.unbindService(mConnection);
		}
	}

	private OnClickListener sendMessageListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			String content = mMessageET.getEditableText().toString();
			ContentValues cv = new ContentValues();
			User local = new User(user1Id);
			User remote = new User(user2Id);

			cv.put(ContentDescriptor.Messages.Cols.MSG_CONTENT, content);
			cv.put(ContentDescriptor.Messages.Cols.MSG_TYPE,
					VMessage.MessageType.TEXT.getIntValue());
			getContentResolver().insert(ContentDescriptor.Messages.CONTENT_URI,
					cv);

			mMessageET.setText("");

			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mMessageET.getWindowToken(), 0);

			VMessage m = new VMessage(local, remote, content);
			Message.obtain(lh, SEND_MESSAGE, m).sendToTarget();
			// Add message to container
			MessageBodyView mv = new MessageBodyView(mContext, m, true);
			mMessagesContainer.addView(mv);
			mScrollView
					.scrollTo(mScrollView.getLeft(), mScrollView.getBottom());
			mScrollView.post(new Runnable() {
				@Override
				public void run() {
					mScrollView.fullScroll(View.FOCUS_DOWN);
				}
			});

		}

	};

	private ScrollViewListener scrollListener = new ScrollViewListener() {

		@Override
		public void onScrollBottom(ItemScrollView scrollView, int x, int y,
				int oldx, int oldy) {

		}

		@Override
		public void onScrollTop(ItemScrollView scrollView, int x, int y,
				int oldx, int oldy) {
			if (isLoading || mLoadedAllMessages) {
				return;
			}
			isLoading = true;
			android.os.Message m = android.os.Message.obtain(lh,
					START_LOAD_MESSAGE);
			lh.sendMessageDelayed(m, 500);
		}

	};

	private void loadMessages() {
		User localUser = new User(user1Id);
		User remoteUser = new User(user2Id);
		DateFormat dp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		String selection = "(" + ContentDescriptor.Messages.Cols.FROM_USER_ID
				+ "=? and " + ContentDescriptor.Messages.Cols.TO_USER_ID
				+ "=? ) or " + "("
				+ ContentDescriptor.Messages.Cols.FROM_USER_ID + "=? and "
				+ ContentDescriptor.Messages.Cols.TO_USER_ID + "=? ) ";
		String[] args = new String[] { user1Id + "", user2Id + "",
				user2Id + "", user1Id + "" };

		Cursor mCur = this.getContentResolver().query(
				ContentDescriptor.Messages.CONTENT_URI,
				ContentDescriptor.Messages.Cols.ALL_CLOS,
				selection,
				args,
				ContentDescriptor.Messages.Cols.SEND_TIME + " desc "
						+ " limit " + BATCH_COUNT + " offset " + offset);

		if (mCur.getCount() == 0) {
			mLoadedAllMessages = true;
			return;
		}
		int count = 0;
		VMessage lastM = null;
		while (mCur.moveToNext()) {
			int id = mCur.getInt(0);
			// msg_content column
			String content = mCur.getString(5);
			// to_user_id column
			long remoteUserId = mCur.getLong(3);
			// date time
			String dateString = mCur.getString(7);
			VMessage m = new VMessage(localUser, remoteUser, content,
					remoteUserId == user2Id);
			try {
				m.setDate(dp.parse(dateString));
			} catch (ParseException e) {
				e.printStackTrace();
			}

			boolean showTime = false;
			if (lastM == null) {
				showTime = true;
			}
			if (lastM != null && lastM.getDate() != null && m.getDate() != null) {
				if (lastM.getDate().getTime() / 60000 == m.getDate().getTime() / 60000) {
					showTime = false;
				} else {
					showTime = true;
				}
			}
			final MessageBodyView mv = new MessageBodyView(this, m, showTime);
			mv.setId(id);
			mScrollView.post(new Runnable() {
				@Override
				public void run() {
					mMessagesContainer.addView(mv, 0);
				}
			});

			lastM = m;
			count++;
			offset++;
			if (count > BATCH_COUNT) {
				break;
			}
		}
		mCur.close();
	}

	/** Defines callback for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			isBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			isBound = false;
		}
	};

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case START_LOAD_MESSAGE:
				if (mLoadingImg == null) {
					mLoadingImg = new ImageView(mContext);
					mLoadingImg.setImageResource(R.drawable.loading);
				}
				mMessagesContainer.addView(mLoadingImg, 0);

				android.os.Message m = android.os.Message.obtain(lh,
						LOAD_MESSAGE);
				lh.sendMessageDelayed(m, 500);
				break;
			case LOAD_MESSAGE:
				loadMessages();
				android.os.Message.obtain(this, END_LOAD_MESSAGE)
						.sendToTarget();
				break;
			case END_LOAD_MESSAGE:
				mMessagesContainer.removeView(mLoadingImg);
				isLoading = false;
				break;
			case SEND_MESSAGE:
				mService.sendMessage((VMessage)msg.obj, Message.obtain(this, SEND_MESSAGE_DONE));
				break;
			}
		}

	}

}
