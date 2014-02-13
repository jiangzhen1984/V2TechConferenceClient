package com.v2tech.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TabHost;

import com.v2tech.R;
import com.v2tech.view.JNIService.LocalBinder;

public class MainActivity extends FragmentActivity implements OnTouchListener {

	private Context mContext;

	TabHost mTabHost;

	TabManager mTabManager;

	private GestureDetector mGestureDetector;

	private JNIService mService;
	private boolean isBound;

	private static final String TAG_CONF = "conference";
	private static final String TAG_CONTACT = "contacts";
	private static final String TAG_SETTING = "setting";
	
	public static final String SERVICE_BOUNDED_EVENT ="com.v2tech.SERVICE_BOUNDED_EVENT";
	public static final String SERVICE_UNBOUNDED_EVENT ="com.v2tech.SERVICE_UNBOUNDED_EVENT";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		Resources res = getResources();

		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();

		mTabManager = new TabManager(this, mTabHost, R.id.realtabcontent);

		mTabManager.addTab(
				mTabHost.newTabSpec(TAG_CONF).setIndicator(null,
						res.getDrawable(R.drawable.selector_conf)),
				ConferenceTabFragment.class, null);
		
		mTabManager.addTab(
				mTabHost.newTabSpec(TAG_CONTACT).setIndicator(null,
						res.getDrawable(R.drawable.selector_group)),
						ContactsTabFragment.class, null);
		
		mTabManager.addTab(
				mTabHost.newTabSpec(TAG_SETTING).setIndicator(null,
						res.getDrawable(R.drawable.selector_setting)),
				SettingTabFragment.class, null);

		if (savedInstanceState != null) {
			mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
		}
		mTabHost.getTabWidget().setDividerDrawable(
				R.drawable.group_list_separation);
		int count = mTabHost.getTabWidget().getChildCount();
		for (int i = 0; i < count; i++) {
			mTabHost.getTabWidget().getChildAt(i)
					.setBackgroundResource(R.color.confs_panel_bg);
		}

		this.mContext = this;

		mGestureDetector = new GestureDetector(this, mGestrueListener);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("tab", mTabHost.getCurrentTabTag());
	}

	@Override
	protected void onStart() {
		super.onStart();
		isBound = bindService(new Intent(this, JNIService.class), mConnection,
				Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (isBound) {
			unbindService(mConnection);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Process.killProcess(Process.myPid());
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private OnGestureListener mGestrueListener = new OnGestureListener() {
		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {

			if (e1.getX() - e2.getX() > 100 && Math.abs(velocityX) > 200) {
				mTabManager.turnToRightTab();
			} else if (e2.getX() - e1.getX() > 200 && Math.abs(velocityX) > 200) {
				mTabManager.turnToLeftTab();
			}

			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {

		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {

		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}

	};

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return false;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		mGestureDetector.onTouchEvent(ev);
		return super.dispatchTouchEvent(ev);
	}
	
	
	
	public JNIService getService() {
		return this.mService;
	}
	
//	public void doBind() {
//		isBound = bindService(new Intent(this, JNIService.class), mConnection,
//				Context.BIND_AUTO_CREATE);
//	}

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			isBound = true;
			mContext.sendBroadcast(new Intent(SERVICE_BOUNDED_EVENT));
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			isBound = false;
			mContext.sendBroadcast(new Intent(SERVICE_UNBOUNDED_EVENT));
		}
	};

	/**
	 * This is a helper class that implements a generic mechanism for
	 * associating fragments with the tabs in a tab host. It relies on a trick.
	 * Normally a tab host has a simple API for supplying a View or Intent that
	 * each tab will show. This is not sufficient for switching between
	 * fragments. So instead we make the content part of the tab host 0dp high
	 * (it is not shown) and the TabManager supplies its own dummy view to show
	 * as the tab content. It listens to changes in tabs, and takes care of
	 * switch to the correct fragment shown in a separate content area whenever
	 * the selected tab changes.
	 */
	public static class TabManager implements TabHost.OnTabChangeListener {
		private final FragmentActivity mActivity;
		private final TabHost mTabHost;
		private final int mContainerId;
		private final HashMap<String, TabInfo> mTabs = new HashMap<String, TabInfo>();
		TabInfo mLastTab;

		private List<String> tabs = new ArrayList<String>();

		static final class TabInfo {
			private final String tag;
			private final Class<?> clss;
			private final Bundle args;
			private Fragment fragment;
			private int index;
			static int CONSTANT_INDEX = 0;

			TabInfo(String _tag, Class<?> _class, Bundle _args) {
				tag = _tag;
				clss = _class;
				args = _args;
				index = CONSTANT_INDEX++;
			}
		}

		static class DummyTabFactory implements TabHost.TabContentFactory {
			private final Context mContext;

			public DummyTabFactory(Context context) {
				mContext = context;
			}

			@Override
			public View createTabContent(String tag) {
				View v = new View(mContext);
				v.setMinimumWidth(0);
				v.setMinimumHeight(0);
				return v;
			}
		}

		public TabManager(FragmentActivity activity, TabHost tabHost,
				int containerId) {
			mActivity = activity;
			mTabHost = tabHost;
			mContainerId = containerId;
			mTabHost.setOnTabChangedListener(this);
		}

		public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
			tabs.add(tabSpec.getTag());
			tabSpec.setContent(new DummyTabFactory(mActivity));
			String tag = tabSpec.getTag();

			TabInfo info = new TabInfo(tag, clss, args);

			// Check to see if we already have a fragment for this tab, probably
			// from a previously saved state. If so, deactivate it, because our
			// initial state is that a tab isn't shown.
			info.fragment = mActivity.getSupportFragmentManager()
					.findFragmentByTag(tag);
			if (info.fragment != null && !info.fragment.isDetached()) {
				FragmentTransaction ft = mActivity.getSupportFragmentManager()
						.beginTransaction();
				ft.detach(info.fragment);
				ft.commit();
			}
			mTabs.put(tag, info);
			mTabHost.addTab(tabSpec);
		}

		public void turnToRightTab() {
			if (mLastTab.index < (tabs.size() - 1)) {
				String key = tabs.get(mLastTab.index + 1);
				mTabHost.setCurrentTabByTag(key);
			}
		}

		public void turnToLeftTab() {
			if (mLastTab.index > 0) {
				String key = tabs.get(mLastTab.index - 1);
				mTabHost.setCurrentTabByTag(key);
			}
		}

		@Override
		public void onTabChanged(String tabId) {
			TabInfo newTab = mTabs.get(tabId);
			if (mLastTab != newTab) {
				FragmentTransaction ft = mActivity.getSupportFragmentManager()
						.beginTransaction();

				// first time
				if (mLastTab == null) {
					ft.setCustomAnimations(R.animator.left_in,
							R.animator.left_out);
				} else {
					if (newTab == null) {
						ft.setCustomAnimations(R.animator.right_in,
								R.animator.right_out);
					}
				}

				if (newTab != null && mLastTab != null) {
					if (newTab.index > mLastTab.index) {
						ft.setCustomAnimations(R.animator.left_in,
								R.animator.left_out);
					} else {
						ft.setCustomAnimations(R.animator.right_in,
								R.animator.right_out);
					}
				}

				if (newTab != null) {
					if (newTab.fragment == null) {
						newTab.fragment = Fragment.instantiate(mActivity,
								newTab.clss.getName(), newTab.args);
						// ft.add(mContainerId, newTab.fragment, newTab.tag);
					}
					ft.replace(mContainerId, newTab.fragment, newTab.tag);

				}

				mLastTab = newTab;

				ft.commit();
				mActivity.getSupportFragmentManager()
						.executePendingTransactions();
			}
		}
	}
}