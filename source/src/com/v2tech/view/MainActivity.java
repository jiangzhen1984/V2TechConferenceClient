package com.v2tech.view;

import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.Notificator;
import com.v2tech.util.V2Log;
import com.v2tech.view.widget.TitleBar;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.NetworkStateCode;

public class MainActivity extends FragmentActivity implements NotificationListener {

	private Context mContext;
	private boolean exitedFlag = false;

	private TitleBar titleBar;

	private TabHost mTabHost;
	private ViewPager mViewPager;
	private PagerAdapter mPagerAdapter;

	private static final int SUB_ACTIVITY_CODE_CREATE_CONF = 100;

	public static final String SERVICE_BOUNDED_EVENT = "com.v2tech.SERVICE_BOUNDED_EVENT";
	public static final String SERVICE_UNBOUNDED_EVENT = "com.v2tech.SERVICE_UNBOUNDED_EVENT";

	private int[] imgs = new int[] { R.drawable.conversation_video_button,
			R.drawable.conversation_group_button,
			R.drawable.conversation_call_button,
			R.drawable.conversation_sms_button,
			R.drawable.conversation_email_button,
			R.drawable.conversation_files_button };

	private int[] items = new int[] {
			R.string.conversation_popup_menu_video_call_button,
			R.string.conversation_popup_menu_group_create_button,
			R.string.conversation_popup_menu_call_button,
			R.string.conversation_popup_menu_sms_call_button,
			R.string.conversation_popup_menu_email_button,
			R.string.conversation_popup_menu_files_button };

	private TabClass[] mTabClasses = new TabClass[] {
			new TabClass(PublicIntent.TAG_CONTACT,
					R.drawable.selector_tab_contact_button,
					R.string.tab_contact_name, R.string.tab_contact_name,

					ContactsTabFragment.class.getName()),
			new TabClass(PublicIntent.TAG_ORG,
					R.drawable.selector_tab_org_button, R.string.tab_org_name,
					R.string.tab_org_name, ContactsTabFragment.class.getName()),
			new TabClass(PublicIntent.TAG_GROUP,
					R.drawable.selector_tab_group_button,
					R.string.tab_group_name, R.string.tab_group_name,
					ConversationsTabFragment.class.getName()),
			new TabClass(PublicIntent.TAG_CONF,
					R.drawable.selector_tab_conference_button,
					R.string.tab_conference_name, R.string.tab_conference_name,
					ConversationsTabFragment.class.getName()),
			new TabClass(PublicIntent.TAG_COV,
					R.drawable.selector_tab_conversation_button,
					R.string.tab_conversation_name,
					R.string.tab_conversation_name,
					ConversationsTabFragment.class.getName()) };

	private LocalReceiver receiver = new LocalReceiver();

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

	/**
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (GlobalConfig.GLOBAL_LAYOUT_SIZE == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		// Inflate the layout
		setContentView(R.layout.activity_main);
		// Initialise the TabHost
		mContext = this;

		// Init title bar
		View titleBarLayout = findViewById(R.id.title_bar_ly);
		titleBar = new TitleBar(mContext, titleBarLayout);
		initPlusItem();
		titleBar.regsiterSearchedTextListener(searchTextWatcher);

		// Initialize first title name
		titleBar.updateTitle(mTabClasses[0].mTabTitleId);

		this.initialiseTabHost(savedInstanceState);
		if (savedInstanceState != null) {
			mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
		}

		// Intialise ViewPager
		this.intialiseViewPager();
		initReceiver();
		// Start animation
		this.overridePendingTransition(R.animator.left_in, R.animator.left_out);
		V2Log.d(" main onCreate ");
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onSaveInstanceState(android.os.Bundle)
	 */
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("tab", mTabHost.getCurrentTabTag());
		super.onSaveInstanceState(outState);
	}

	private void initPlusItem() {
		for (int i = 0; i < imgs.length; i++) {
			LinearLayout ll = new LinearLayout(mContext);
			ll.setOrientation(LinearLayout.HORIZONTAL);

			ImageView iv = new ImageView(mContext);
			iv.setImageResource(imgs[i]);
			iv.setPadding(10, 5, 5, 10);
			LinearLayout.LayoutParams ivLL = new LinearLayout.LayoutParams(0,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			ivLL.gravity = Gravity.RIGHT;
			ivLL.weight = 0.3F;

			ll.addView(iv, ivLL);

			TextView tv = new TextView(mContext);
			tv.setText(items[i]);
			tv.setPadding(10, 5, 5, 10);
			LinearLayout.LayoutParams tvLL = new LinearLayout.LayoutParams(0,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			tvLL.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
			tvLL.weight = 0.7F;

			ll.addView(tv, tvLL);
			ll.setOnClickListener(titleBarMenuItemClickListener);

			ll.setId(imgs[i]);
			titleBar.addAdditionalPopupMenuItem(ll, null);
		}
	}

	/**
	 * Initialise ViewPager
	 */
	private void intialiseViewPager() {

		List<Fragment> fragments = new Vector<Fragment>();

		for (TabClass tc : mTabClasses) {
			Bundle bundle = new Bundle();
			bundle.putString("tag", tc.mTabName);
			Fragment frg = Fragment.instantiate(this, tc.clsName, bundle);
			fragments.add(frg);

		}

		this.mPagerAdapter = new PagerAdapter(
				super.getSupportFragmentManager(), fragments);
		this.mViewPager = (ViewPager) super.findViewById(R.id.viewpager);
		this.mViewPager.setAdapter(this.mPagerAdapter);
		this.mViewPager.setOnPageChangeListener(pageChangeListener);
		this.mViewPager.setOffscreenPageLimit(5);
		this.mViewPager.setCurrentItem(0);
	}

	/**
	 * Initialize the Tab Host
	 */
	private void initialiseTabHost(Bundle args) {
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();

		for (TabClass tc : mTabClasses) {
			TabHost.TabSpec confTabSpec = this.mTabHost.newTabSpec(tc.mTabName)
					.setIndicator(getTabView(tc));
			confTabSpec.setContent(new TabFactory(this));
			mTabHost.addTab(confTabSpec);
		}

		mTabHost.setOnTabChangedListener(tabChnageListener);
	}

	private View getTabView(TabClass tcl) {

		LayoutInflater inflater = LayoutInflater.from(this);
		View v = inflater.inflate(R.layout.tab_widget_view, null, false);
		ImageView iv = (ImageView) v.findViewById(R.id.tab_image);
		if (iv != null) {
			iv.setImageDrawable(this.getResources().getDrawable(tcl.mDraId));
			iv.bringToFront();
		}

		TextView tv = (TextView) v.findViewById(R.id.tab_name);
		if (tv != null) {
			tv.setText(this.getResources().getText(tcl.mTabNameId));
			tv.bringToFront();
		}

		View notifi = v.findViewById(R.id.tab_notificator);
		tcl.notificator = notifi;
		tcl.notificator.setVisibility(View.INVISIBLE);
		return v;
	}

	private void initReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(PublicIntent.UPDATE_CONVERSATION);
		filter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		filter.addAction(PublicIntent.FINISH_APPLICATION);
		filter.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		mContext.registerReceiver(receiver, filter);
	}

	@Override
	protected void onStart() {
		super.onStart();
		V2Log.d(" main onStart ");
	}

	@Override
	protected void onStop() {
		super.onStop();
		V2Log.d(" main onStop ");
	}

	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
	}

	public void requestQuit() {
		if (exitedFlag) {
			this.getApplicationContext().stopService(
					new Intent(this.getApplicationContext(), JNIService.class));
			Handler h = new Handler();
			h.postDelayed(new Runnable() {

				@Override
				public void run() {
					GlobalConfig.saveLogoutFlag(mContext);
					Notificator.cancelAllSystemNotification(mContext);
					System.exit(0);
				}
				
			}, 1000);
			finish();
			
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == Activity.RESULT_OK) {
			long gid = data.getLongExtra("newGid", 0);
			Group g = GlobalHolder.getInstance().getGroupById(
					GroupType.CONFERENCE, gid);
			int count = ((FragmentPagerAdapter) mViewPager.getAdapter())
					.getCount();
			for (int i = 0; i < count; i++) {
				Fragment frg = ((FragmentPagerAdapter) mViewPager.getAdapter())
						.getItem(i);
				if (frg instanceof ActionListener) {
					((ActionListener) frg).listenGroupCreated(g);
				}
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mContext.unregisterReceiver(receiver);
		mContext.stopService(new Intent(this.getApplicationContext(),
				JNIService.class));
		V2Log.d("system destroyed v2tech");
	}
	
	
	
	public void updateNotificator(String type) {
		
		View noticator = null;
		if (type.equals(Conversation.TYPE_GROUP)) {
			noticator = mTabClasses[2].notificator;
		} else if (type.equals(Conversation.TYPE_CONFERNECE)) {
			noticator = mTabClasses[3].notificator;
		} else if (type.equals(Conversation.TYPE_CONTACT)) {
			noticator = mTabClasses[4].notificator;
		}

		V2Log.i("type:" + type + "  count:"
				+ GlobalHolder.getInstance().getNoticatorCount(type));
		if (noticator != null) {
			if (GlobalHolder.getInstance().getNoticatorCount(type) > 0) {
				noticator.setVisibility(View.VISIBLE);
			} else {
				noticator.setVisibility(View.GONE);
			}
		}
		
	}

	private TextWatcher searchTextWatcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable edit) {

			((TextWatcher) ((FragmentPagerAdapter) mViewPager.getAdapter())
					.getItem(mTabHost.getCurrentTab())).afterTextChanged(edit);
		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {

		}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {

		}

	};

	private OnClickListener titleBarMenuItemClickListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			int id = view.getId();
			switch (id) {
			case R.drawable.conversation_video_button: {
				titleBar.dismissPlusWindow();
				Intent i = new Intent(
						PublicIntent.START_CONFERENCE_CREATE_ACTIVITY);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				startActivityForResult(i, SUB_ACTIVITY_CODE_CREATE_CONF);
			}
				break;
			case R.drawable.conversation_call_button:
				break;
			case R.drawable.conversation_sms_button:
				break;
			case R.drawable.conversation_email_button:
				break;
			case R.drawable.conversation_files_button:
				break;
			}
		}

	};

	private TabHost.OnTabChangeListener tabChnageListener = new TabHost.OnTabChangeListener() {

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
			mViewPager.setCurrentItem(pos);
			titleBar.updateTitle(mTabClasses[pos].mTabTitleId);

		}

	};

	private ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {

		@Override
		public void onPageScrollStateChanged(int arg0) {

		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {

		}

		@Override
		public void onPageSelected(int pos) {
			mTabHost.setCurrentTab(pos);
			titleBar.updateTitle(mTabClasses[pos].mTabTitleId);
		}

	};

	public class PagerAdapter extends FragmentPagerAdapter {

		private List<Fragment> fragments;

		/**
		 * @param fm
		 * @param fragments
		 */
		public PagerAdapter(FragmentManager fm, List<Fragment> fragments) {
			super(fm);
			this.fragments = fragments;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.support.v4.app.FragmentPagerAdapter#getItem(int)
		 */
		@Override
		public Fragment getItem(int position) {
			Fragment frag = this.fragments.get(position);
			return frag;
		}

		/*
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
			if (PublicIntent.UPDATE_CONVERSATION.equals(action)) {
				String type = intent.getExtras().getString("type");
				updateNotificator(type);
			} else if (PublicIntent.FINISH_APPLICATION.equals(action)) {
				exitedFlag = true;
				requestQuit();
			} else if (JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION.equals(action)) {
				NetworkStateCode code = (NetworkStateCode) intent.getExtras()
						.get("state");
				if (titleBar != null) {
					titleBar.updateConnectState(code);
				}
			}
		}

	}

	class TabClass {
		String mTabName;
		int mDraId;
		int mTabNameId;
		int mTabTitleId;
		String clsName;
		View notificator;

		public TabClass(String mTabName, int mDraId, int mTabNameId,
				int tabTitleId, String clsName, View notificator) {
			super();
			this.mTabName = mTabName;
			this.mDraId = mDraId;
			this.mTabNameId = mTabNameId;
			this.mTabTitleId = tabTitleId;
			this.clsName = clsName;
			this.notificator = notificator;
		}

		public TabClass(String tabName, int draId, int tabNameId,
				int tabTitleId, String clsName) {
			this(tabName, draId, tabNameId, tabTitleId, clsName, null);
		}

	}

}