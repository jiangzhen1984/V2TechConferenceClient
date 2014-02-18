package com.v2tech.view.conversation;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
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
	private final int QUERY_NEW_MESSAGE = 6;

	private final int BATCH_COUNT = 10;

	private int offset = 0;

	private LinearLayout mMessagesContainer;

	private ItemScrollView mScrollView;

	private long user1Id;

	private long user2Id;
	
	private String user2Name;

	private LocalHandler lh;

	private BackendHandler backEndHandler;

	private JNIService mService;
	private boolean isBound;

	private boolean isLoading;

	private boolean mLoadedAllMessages;

	private Context mContext;

	private TextView mSendButtonTV;

	private TextView mReturnButtonTV;

	private EditText mMessageET;

	private ImageView mLoadingImg;
	
	private TextView mUserTitleTV;
	

	private MessageReceiver receiver =  new MessageReceiver();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.activity_contact_message);
		mMessagesContainer = (LinearLayout) findViewById(R.id.conversation_message_list);

		mScrollView = (ItemScrollView) findViewById(R.id.conversation_message_list_scroll_view);
		mScrollView.setScrollListener(scrollListener);

		mSendButtonTV = (TextView) findViewById(R.id.message_send);
		//mSendButtonTV.setOnClickListener(sendMessageListener);
		mSendButtonTV.setOnTouchListener(sendMessageButtonListener);
		

		mMessageET = (EditText) findViewById(R.id.message_text);
		mReturnButtonTV = (TextView) findViewById(R.id.contact_detail_return_button);
		mReturnButtonTV.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				finish();
			}

		});
		
		mUserTitleTV = (TextView)findViewById(R.id.message_user_title);

		user1Id = this.getIntent().getLongExtra("user1id", 0);
		user2Id = this.getIntent().getLongExtra("user2id", 0);
		user2Name = this.getIntent().getStringExtra("user2Name");


		lh = new LocalHandler();

		HandlerThread thread = new HandlerThread("back-end");
		thread.start();
		synchronized (thread) {
			while (!thread.isAlive()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			backEndHandler = new BackendHandler(thread.getLooper());
		}
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(JNIService.JNI_BROADCAST_NEW_MESSAGE);
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		registerReceiver(receiver, filter);
		
		
	}

	@Override
	protected void onStart() {
		super.onStart();
		isBound = bindService(new Intent(this.getApplicationContext(),
				JNIService.class), mConnection, Context.BIND_AUTO_CREATE);
		if (!mLoadedAllMessages) { 
			android.os.Message m = android.os.Message
					.obtain(lh, START_LOAD_MESSAGE);
			lh.sendMessageDelayed(m, 500);
		}

		if (user1Id == 0 || user2Id == 0) {
			Toast.makeText(this, R.string.error_contact_messag_invalid_user_id,
					Toast.LENGTH_SHORT).show();
		}
		mUserTitleTV.setText(user2Name);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (isBound) {
			this.unbindService(mConnection);
		}
	}

	
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(receiver);
	}




	private OnTouchListener sendMessageButtonListener = new OnTouchListener () {

		@Override
		public boolean onTouch(View anchor, MotionEvent mv) {
			int action = mv.getAction();
			if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_HOVER_ENTER) {
				anchor.setBackgroundResource(R.drawable.message_send_button_pressed_bg);
			} else if (action == MotionEvent.ACTION_UP) {
				anchor.setBackgroundResource(R.drawable.message_send_button_bg);
				doSendMessage();
			} 
			return true;
		}
		
	};
	
	private void doSendMessage() {
		String content = mMessageET.getEditableText().toString();
		ContentValues cv = new ContentValues();
		User local = new User(user1Id);
		User remote = new User(user2Id);

		VMessage m = new VMessage(local, remote, content);
		cv.put(ContentDescriptor.Messages.Cols.FROM_USER_ID, user1Id);
		cv.put(ContentDescriptor.Messages.Cols.TO_USER_ID, user2Id);
		cv.put(ContentDescriptor.Messages.Cols.MSG_CONTENT, content);
		cv.put(ContentDescriptor.Messages.Cols.MSG_TYPE,
				VMessage.MessageType.TEXT.getIntValue());
		cv.put(ContentDescriptor.Messages.Cols.SEND_TIME, m.getDateTimeStr());
		getContentResolver().insert(ContentDescriptor.Messages.CONTENT_URI,
				cv);

		mMessageET.setText("");

		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mMessageET.getWindowToken(), 0);

		
		Message.obtain(lh, SEND_MESSAGE, m).sendToTarget();
		// Add message to container
		MessageBodyView mv = new MessageBodyView(mContext, m, true);
		mMessagesContainer.addView(mv);
		mScrollView.post(new Runnable() {
			@Override
			public void run() {
				mScrollView.fullScroll(View.FOCUS_DOWN);
			}
		});

	}

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

	private List<VMessage> loadMessages() {
		User localUser = new User(user1Id);
		User remoteUser = new User(user2Id);
		DateFormat dp = new SimpleDateFormat("yyyy-MM-dd HH:mm");

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
			return null;
		}
		List<VMessage> array = new ArrayList<VMessage>();
		int count = 0;
		VMessage lastM = null;
		while (mCur.moveToNext()) {
			int id = mCur.getInt(0);
			// msg_content column
			String content = mCur.getString(5);
			// to_user_id column
			long localUserId = mCur.getLong(1);
			// date time
			String dateString = mCur.getString(7);
			VMessage m = new VMessage(localUser, remoteUser, content,
					localUserId == user2Id);
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
//			final MessageBodyView mv = new MessageBodyView(this, m, showTime);
//			mv.setId(id);

			array.add(m);
			lastM = m;
			count++;
			offset++;
			if (count > BATCH_COUNT) {
				break;
			}
		}
		mCur.close();

		return array;
	}
	
	private void queryAndAddMessage(int mid) {
		User localUser = new User(user1Id);
		User remoteUser = new User(user2Id);
		
		Uri uri = ContentUris.withAppendedId(ContentDescriptor.Messages.CONTENT_URI, mid);
		
		Cursor mCur = this.getContentResolver().query(
				uri,
				ContentDescriptor.Messages.Cols.ALL_CLOS,
				null,
				null,
				null);
		DateFormat dp = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		while (mCur.moveToNext()) {
			int id = mCur.getInt(0);
			// msg_content column
			String content = mCur.getString(5);
			// date time
			String dateString = mCur.getString(7);
			VMessage m = new VMessage(remoteUser, localUser, content,
					true);
			
			try {
				m.setDate(dp.parse(dateString));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			 MessageBodyView mv = new MessageBodyView(this, m, true);
			 mMessagesContainer.addView(mv);
		}
		
		mScrollView.post(new Runnable() {
			@Override
			public void run() {
				mScrollView.fullScroll(View.FOCUS_DOWN);
			}
		});
		
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
	
	
	class MessageReceiver  extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (JNIService.JNI_BROADCAST_NEW_MESSAGE.equals(intent.getAction())) {
				Message.obtain(lh, QUERY_NEW_MESSAGE, intent.getExtras().get("mid"))
				.sendToTarget();
			}
		}
		
	}

	class BackendHandler extends Handler {

		public BackendHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LOAD_MESSAGE:
				List<VMessage> array = loadMessages();
				android.os.Message.obtain(lh, END_LOAD_MESSAGE, array)
						.sendToTarget();
				break;
			}
		}

	}

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

				android.os.Message.obtain(backEndHandler, LOAD_MESSAGE)
						.sendToTarget();
				break;
			case LOAD_MESSAGE:
				loadMessages();
				android.os.Message.obtain(this, END_LOAD_MESSAGE)
						.sendToTarget();
				break;
			case END_LOAD_MESSAGE:
				mMessagesContainer.removeView(mLoadingImg);
				List<VMessage> array = (List<VMessage>) msg.obj;
				MessageBodyView fir =  null;
				for (int i = 0; array != null && i < array.size(); i++) {
					MessageBodyView mv = new MessageBodyView(mContext, array.get(i), true);
					if (fir == null) {
						fir = mv;
					}
					mMessagesContainer.addView(mv, 0);
				}
				if (fir != null) {
					fir.requestFocus();
				}
				isLoading = false;
				break;
			case SEND_MESSAGE:
				mService.sendMessage((VMessage) msg.obj,
						Message.obtain(this, SEND_MESSAGE_DONE));
				break;
			case QUERY_NEW_MESSAGE:
				if (msg.obj ==null || "".equals(msg.obj.toString())) {
					break;
				}
				queryAndAddMessage(Integer.parseInt(msg.obj.toString()));
				break;
			}
		}

	}

}
