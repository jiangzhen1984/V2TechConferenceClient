package com.bizcom.vc.activity.main;

import java.util.List;
import java.util.Vector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.util.V2Log;
import com.bizcom.vc.activity.conversation.MessageLoader;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.MainApplication;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.application.V2GlobalConstants;
import com.bizcom.vc.listener.ConferenceListener;
import com.bizcom.vc.listener.NotificationListener;
import com.bizcom.vc.receiver.HeadSetPlugReceiver;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vc.widget.HeadLayoutManager;
import com.bizcom.vo.Conference;
import com.bizcom.vo.Conversation;
import com.bizcom.vo.NetworkStateCode;
import com.bizcom.vo.VFile.State;
import com.bizcom.vo.VMessageAbstractItem;
import com.bizcom.vo.VMessageFileItem;
import com.v2tech.R;

public class MainActivity extends FragmentActivity implements
		NotificationListener {
	public static final String SERVICE_BOUNDED_EVENT = "com.v2tech.SERVICE_BOUNDED_EVENT";
	public static final String SERVICE_UNBOUNDED_EVENT = "com.v2tech.SERVICE_UNBOUNDED_EVENT";
	private static final String TAG = "MainActivity";

	private HeadLayoutManager mHeadLayoutManager;
	private EditText searchEdit;
	private TabHost mTabHost;
	private ViewPager mViewPager;
	private MyFragmentPagerAdapter mPagerAdapter;
	private LocalReceiver receiver = new LocalReceiver();
	private ConferenceListener mConfListener;
	private List<Fragment> fragments;
	private HeadSetPlugReceiver localReceiver = new HeadSetPlugReceiver();
	private TabHostOnTabChangeListener mOnTabChangeListener = new TabHostOnTabChangeListener();

	private Context mContext;
	private Conference conf;
	private boolean exitedFlag = false;

	private TabWrap[] mTabClasses = new TabWrap[] {

			new TabWrap(PublicIntent.TAG_CONTACT, R.string.tab_contact_name,
					R.string.tab_contact_name,
					R.drawable.selector_tab_contact_button,
					TabFragmentContacts.class.getName()),
			new TabWrap(PublicIntent.TAG_ORG, R.string.tab_org_name,
					R.string.tab_org_name, R.drawable.selector_tab_org_button,
					TabFragmentOrganization.class.getName()),
			new TabWrap(PublicIntent.TAG_GROUP, R.string.tab_group_name,
					R.string.tab_group_name,
					R.drawable.selector_tab_group_button,
					TabFragmentCrowd.class.getName()),
			new TabWrap(PublicIntent.TAG_CONF, R.string.tab_conference_name,
					R.string.tab_conference_name,
					R.drawable.selector_tab_conference_button,
					TabFragmentConference.class.getName()),
			new TabWrap(PublicIntent.TAG_COV, R.string.tab_conversation_name,
					R.string.tab_conversation_name,
					R.drawable.selector_tab_conversation_button,
					TabFragmentMessage.class.getName()) };

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mContext = this;
		searchEdit = (EditText) findViewById(R.id.search_edit);
		mHeadLayoutManager = new HeadLayoutManager(mContext,
				findViewById(R.id.head_layout));
		mHeadLayoutManager
				.regsiterSearchedTextListener(listenerOfSearchTextWatcher);
		mHeadLayoutManager.updateTitle(mTabClasses[0].mTabTitle);

		initialiseTabHost();
		if (savedInstanceState != null) {
			mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
		}

		int index = getIntent().getIntExtra("initFragment", 0);
		intialiseViewPager(index);

		initReceiver();
		updateFileState();
	}

	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("tab", mTabHost.getCurrentTabTag());
		super.onSaveInstanceState(outState);
	}

	private void initialiseTabHost() {
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();

		for (TabWrap tabWrap : mTabClasses) {
			TabHost.TabSpec tabSpec = this.mTabHost
					.newTabSpec(tabWrap.mTabName).setIndicator(
							getTabView(tabWrap));
			tabSpec.setContent(new TabFactory(this));
			mTabHost.addTab(tabSpec);
		}

		mTabHost.setOnTabChangedListener(mOnTabChangeListener);
	}

	private View getTabView(TabWrap tabWrap) {

		LayoutInflater inflater = LayoutInflater.from(this);
		View v = inflater.inflate(R.layout.tab_widget_view, null, false);
		ImageView iv = (ImageView) v.findViewById(R.id.tab_image);
		if (iv != null) {
			iv.setImageDrawable(this.getResources().getDrawable(
					tabWrap.mDrawableId));
			iv.bringToFront();
		}

		TextView tv = (TextView) v.findViewById(R.id.tab_name);
		if (tv != null) {
			tv.setText(this.getResources().getText(tabWrap.mShowText));
			tv.bringToFront();
		}

		View notifi = v.findViewById(R.id.tab_notificator);
		tabWrap.mViewNotificator = notifi;
		tabWrap.mViewNotificator.setVisibility(View.INVISIBLE);
		return v;
	}

	private void intialiseViewPager(int index) {

		fragments = new Vector<Fragment>();

		for (TabWrap tabWrap : mTabClasses) {
			Bundle bundle = new Bundle();
			bundle.putString("tag", tabWrap.mTabName);
			Fragment fragment = Fragment.instantiate(this, tabWrap.mFragmentClassName,
					bundle);
			
			if (fragment instanceof ConferenceListener
					&& tabWrap.mTabName.equals(PublicIntent.TAG_CONF)) {
				mConfListener = (ConferenceListener) fragment;
			}
			
			fragments.add(fragment);
		}

		mViewPager = (ViewPager) super.findViewById(R.id.viewpager);
		
		mPagerAdapter = new MyFragmentPagerAdapter(
				super.getSupportFragmentManager(), fragments);
		mViewPager.setAdapter(this.mPagerAdapter);
		
		mViewPager.setOnPageChangeListener(listenerOfPageChange);
		mViewPager.setOffscreenPageLimit(5);
		mViewPager.setCurrentItem(index);
	}

	private void initReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		filter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		filter.addAction(PublicIntent.FINISH_APPLICATION);
		filter.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		mContext.registerReceiver(receiver, filter);

		// Register listener for headset
		IntentFilter filterHeadSet = new IntentFilter();
		filterHeadSet.addAction(Intent.ACTION_HEADSET_PLUG);
		Intent result = mContext.registerReceiver(localReceiver, filterHeadSet);
		if (result != null) {
			localReceiver.onReceive(this, result);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (conf != null) {
			mConfListener.requestJoinConf(conf);
			conf = null;
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		int index = intent.getIntExtra("initFragment", 0);
		mViewPager.setCurrentItem(index);
		if (intent.getExtras() != null) {
			conf = (Conference) intent.getExtras().get("conf");
		}
	}

	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
	}

	public void requestQuit() {
		if (exitedFlag) {
			this.getApplicationContext().stopService(
					new Intent(this.getApplicationContext(), JNIService.class));
			//finish();
			((MainApplication) this.getApplicationContext()).requestQuit();

		} else {
			exitedFlag = true;
			Toast.makeText(this, R.string.quit_promption, Toast.LENGTH_SHORT)
					.show();
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {

				@Override
				public void run() {
					exitedFlag = false;
				}

			}, 2500);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mContext.unregisterReceiver(receiver);
		// unregister for headset
		mContext.unregisterReceiver(localReceiver);
		// mContext.stopService(new Intent(this.getApplicationContext(),
		// JNIService.class));
		// Just for user remove application from recent task list
		// Notificator.cancelAllSystemNotification(getApplicationContext());

		((MainApplication) getApplication()).uninitForExitProcess();
		V2Log.d("system destroyed v2tech");

	}

	public void updateNotificator(int type, boolean flag) {

		View noticator = null;
		if (type == Conversation.TYPE_GROUP
				|| type == V2GlobalConstants.GROUP_TYPE_DEPARTMENT) {
			noticator = mTabClasses[2].mViewNotificator;
		} else if (type == Conversation.TYPE_CONFERNECE) {
			noticator = mTabClasses[3].mViewNotificator;
		} else if (type == Conversation.TYPE_CONTACT) {
			noticator = mTabClasses[4].mViewNotificator;
		} else {
			V2Log.e(TAG, "Error TabFragment Type Value : " + type);
			return;
		}

		if (flag) {
			noticator.setVisibility(View.VISIBLE);
		} else {
			noticator.setVisibility(View.GONE);
		}
	}

	private TextWatcher listenerOfSearchTextWatcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable edit) {

			((TextWatcher) ((FragmentPagerAdapter) mViewPager.getAdapter())
					.getItem(mTabHost.getCurrentTab())).afterTextChanged(edit);
		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
			((TextWatcher) ((FragmentPagerAdapter) mViewPager.getAdapter())
					.getItem(mTabHost.getCurrentTab())).beforeTextChanged(arg0,
					arg1, arg2, arg3);
		}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
			((TextWatcher) ((FragmentPagerAdapter) mViewPager.getAdapter())
					.getItem(mTabHost.getCurrentTab())).onTextChanged(arg0,
					arg1, arg2, arg3);
		}

	};

	/**
	 * Detecting all VMessageFileItem Object that state is sending or
	 * downloading in the database , and change their status to failed..
	 */
	public void updateFileState() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				List<VMessageFileItem> loadFileMessages = MessageLoader
						.loadFileMessages(-1, -1);
				if (loadFileMessages != null) {
					for (VMessageFileItem fileItem : loadFileMessages) {
						V2Log.d(TAG,
								"Iterator VMessageFileItem -- name is : "
										+ fileItem.getFileName() + " state : "
										+ State.fromInt(fileItem.getState()));
						if (fileItem.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADING
								|| fileItem.getState() == VMessageAbstractItem.STATE_FILE_PAUSED_DOWNLOADING)
							fileItem.setState(VMessageAbstractItem.STATE_FILE_DOWNLOADED_FALIED);
						else if (fileItem.getState() == VMessageAbstractItem.STATE_FILE_SENDING
								|| fileItem.getState() == VMessageAbstractItem.STATE_FILE_PAUSED_SENDING)
							fileItem.setState(VMessageAbstractItem.STATE_FILE_SENT_FALIED);
						int update = MessageLoader.updateFileItemState(
								mContext, fileItem);
						if (update == -1) {
							V2Log.e(TAG,
									"update file state failed... file id is : "
											+ fileItem.getUuid());
						}

						if (fileItem.getState() == VMessageAbstractItem.STATE_FILE_UNDOWNLOAD) {
							GlobalHolder.getInstance().mFailedFiles
									.add(fileItem.getUuid());
						}
					}
				} else
					V2Log.e(TAG, "load all files failed... get null");
			}
		}).start();
	}

	private class TabHostOnTabChangeListener implements
			TabHost.OnTabChangeListener {
		/**
		 * (non-Javadoc)
		 * 
		 * @see android.widget.TabHost.OnTabChangeListener#onTabChanged(java.lang.String)
		 */
		public void onTabChanged(String tag) {
			int pos = mTabHost.getCurrentTab();
			V2Log.d(" onTabChanged " + "  " + pos);
			if (mViewPager == null) {
				V2Log.e(" MainActivity state is illegal");
				return;
			}
			// 恢复搜索状态
			TabWrap tab = mTabClasses[pos];
			Fragment fragment = fragments.get(pos);
			if (tab.mTabName.equals(PublicIntent.TAG_CONTACT)) {
			} else if (tab.mTabName.equals(PublicIntent.TAG_ORG)) {
			} else if (tab.mTabName.equals(PublicIntent.TAG_GROUP)) {
				((TabFragmentCrowd) fragment).updateSearchState();
			} else if (tab.mTabName.equals(PublicIntent.TAG_CONF)) {
				((TabFragmentConference) fragment).updateSearchState();
			} else if (tab.mTabName.equals(PublicIntent.TAG_COV)) {
				((TabFragmentMessage) fragment).updateSearchState();
			}

			mViewPager.setCurrentItem(pos);
			mHeadLayoutManager.updateTitle(mTabClasses[pos].mTabTitle);
		}

	};

	private ViewPager.OnPageChangeListener listenerOfPageChange = new ViewPager.OnPageChangeListener() {

		@Override
		public void onPageScrollStateChanged(int arg0) {

		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {

		}

		@Override
		public void onPageSelected(int pos) {
			searchEdit.setText("");
			mTabHost.setCurrentTab(pos);
			mHeadLayoutManager.updateTitle(mTabClasses[pos].mTabTitle);
		}

	};

	public class MyFragmentPagerAdapter extends FragmentPagerAdapter {

		private List<Fragment> fragments;

		/**
		 * @param fm
		 * @param fragments
		 */
		public MyFragmentPagerAdapter(FragmentManager fm,
				List<Fragment> fragments) {
			super(fm);
			this.fragments = fragments;
		}

		/**
		 * (non-Javadoc)
		 * 
		 * @see android.support.v4.app.FragmentPagerAdapter#getItem(int)
		 */
		@Override
		public Fragment getItem(int position) {
			Fragment frag = this.fragments.get(position);
			return frag;
		}

		/**
		 * (non-Javadoc)
		 * 
		 * @see android.support.v4.view.PagerAdapter#getCount()
		 */
		@Override
		public int getCount() {
			return this.fragments.size();
		}
	}

	class LocalReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (PublicIntent.FINISH_APPLICATION.equals(action)) {
				exitedFlag = true;
				requestQuit();
			} else if (JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION
					.equals(action)) {
				V2Log.d("CONNECT",
						"MainActivity Receiver Broadcast ! Globle Connection State is : "
								+ GlobalHolder.getInstance()
										.isServerConnected());
				NetworkStateCode code = (NetworkStateCode) intent.getExtras()
						.get("state");
				V2Log.d("CONNECT",
						"MainActivity Receiver Broadcast ! receiver Connection State is : "
								+ code.name());
				V2Log.d("CONNECT",
						"--------------------------------------------------------------------");
				if (mHeadLayoutManager != null) {
					mHeadLayoutManager.updateConnectState(code);
				} else {
					V2Log.d("CONNECT", "TitleBar is null !");
				}
				V2Log.d("CONNECT",
						"--------------------------------------------------------------------");
			}
		}
	}

	class TabWrap {
		String mTabName;
		int mTabTitle;
		int mShowText;
		int mDrawableId;
		String mFragmentClassName;
		View mViewNotificator;

		public TabWrap(String tabName, int tabTitle, int showText,
				int drawableId, String clsName, View viewNotificator) {
			super();
			this.mTabName = tabName;
			this.mTabTitle = tabTitle;
			this.mShowText = showText;
			this.mDrawableId = drawableId;
			this.mFragmentClassName = clsName;
			this.mViewNotificator = viewNotificator;
		}

		public TabWrap(String tabName, int tabTitle, int showText,
				int drawableId, String clsName) {
			this(tabName, tabTitle, showText, drawableId, clsName, null);
		}

	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		View v = getCurrentFocus();
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null && v != null) {
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
		}
		v.clearFocus();
		return super.dispatchTouchEvent(ev);
	}

	/**
	 * A simple factory that returns dummy views to the Tabhost
	 * 
	 */
	class TabFactory implements TabContentFactory {

		private final Context mContext;

		/**
		 * @param context
		 */
		public TabFactory(Context context) {
			mContext = context;
		}

		/**
		 * (non-Javadoc)
		 * 
		 * @see android.widget.TabHost.TabContentFactory#createTabContent(java.lang.String)
		 */
		public View createTabContent(String tag) {
			View v = new View(mContext);
			v.setMinimumWidth(0);
			v.setMinimumHeight(0);
			return v;
		}

	}
}
