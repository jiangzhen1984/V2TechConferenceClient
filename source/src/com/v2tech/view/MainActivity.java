package com.v2tech.view;

import java.util.List;
import java.util.Vector;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.Notificator;
import com.v2tech.util.V2Log;
import com.v2tech.view.JNIService.LocalBinder;

public class MainActivity extends FragmentActivity implements
		TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {

	private Context mContext;
	private JNIService mService;
	private boolean isBound = false;
	private boolean exitedFlag = false;

	private TabHost mTabHost;
	private ViewPager mViewPager;
	private PagerAdapter mPagerAdapter;

	public static final String SERVICE_BOUNDED_EVENT = "com.v2tech.SERVICE_BOUNDED_EVENT";
	public static final String SERVICE_UNBOUNDED_EVENT = "com.v2tech.SERVICE_UNBOUNDED_EVENT";

	private static final String TAG_CONF = "conference";
	private static final String TAG_CONTACT = "contacts";
	private static final String TAG_SETTING = "setting";

	private LocalReceiver receiver = new LocalReceiver();

	private ImageView mTabNoticator;

	public JNIService getService() {
		return mService;
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

	/**
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Inflate the layout
		setContentView(R.layout.activity_main);
		// Initialise the TabHost
		mContext = this;
		this.initialiseTabHost(savedInstanceState);
		if (savedInstanceState != null) {
			mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
		}
		// Intialise ViewPager
		this.intialiseViewPager();
		initDPI();
		initReceiver();
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

	private void initDPI() {
		DisplayMetrics metrics = new DisplayMetrics();

		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		GlobalConfig.GLOBAL_DPI = metrics.densityDpi;

		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
		double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
		GlobalConfig.SCREEN_INCHES = Math.sqrt(x + y);
	}

	/**
	 * Initialise ViewPager
	 */
	private void intialiseViewPager() {

		List<Fragment> fragments = new Vector<Fragment>();
		fragments.add(Fragment.instantiate(this,
				ConversationsTabFragment.class.getName()));
		fragments.add(Fragment.instantiate(this,
				ContactsTabFragment.class.getName()));
		fragments.add(Fragment.instantiate(this,
				SettingTabFragment.class.getName()));
		this.mPagerAdapter = new PagerAdapter(
				super.getSupportFragmentManager(), fragments);
		//
		this.mViewPager = (ViewPager) super.findViewById(R.id.viewpager);
		this.mViewPager.setAdapter(this.mPagerAdapter);
		this.mViewPager.setOnPageChangeListener(this);
	}

	/**
	 * Initialize the Tab Host
	 */
	private void initialiseTabHost(Bundle args) {
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();


		TabHost.TabSpec confTabSpec = this.mTabHost.newTabSpec(TAG_CONF)
				.setIndicator(
						getTabView( mContext.getResources()
								.getDrawable(R.drawable.selector_conference), mContext.getResources().getString(R.string.tab_conference_name)));
		confTabSpec.setContent(new TabFactory(this));
		mTabHost.addTab(confTabSpec);

		TabHost.TabSpec contactTabSpec = this.mTabHost.newTabSpec(TAG_CONTACT)
				.setIndicator(getTabView( mContext.getResources()
						.getDrawable(R.drawable.selector_conf), mContext.getResources().getString(R.string.tab_conversation_org)));
		contactTabSpec.setContent(new TabFactory(this));
		mTabHost.addTab(contactTabSpec);

		TabHost.TabSpec settingTabSpec = this.mTabHost.newTabSpec(TAG_SETTING)
				.setIndicator(getTabView( mContext.getResources()
						.getDrawable(R.drawable.selector_conf), mContext.getResources().getString(R.string.tab_conversation_org)));
		settingTabSpec.setContent(new TabFactory(this));
		mTabHost.addTab(settingTabSpec);

		mTabHost.setOnTabChangedListener(this);
//
		for (int i = 0; i < mTabHost.getTabWidget().getChildCount(); i++) {
			mTabHost.getTabWidget().getChildAt(i)
					.setBackgroundColor(Color.rgb(247, 247, 247));
		}
	}

	
	
	private View getTabView(Drawable drw, String title) {
		LayoutInflater inflater = LayoutInflater.from(this);
		View v = inflater.inflate(R.layout.tab_widget_view, null, false);
		ImageView iv = (ImageView) v.findViewById(R.id.tab_image);
		if (iv != null) {
			iv.setImageDrawable(drw);
			iv.bringToFront();
		}
		
		TextView tv = (TextView) v.findViewById(R.id.tab_name);
		if (tv != null) {
			tv.setText(title);
			tv.bringToFront();
		//	tv.setTextColor(this.getResources().getColorStateList(R.color.selector_tab_text_color));
		}
		return v;
	}

	private void initReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(PublicIntent.UPDATE_CONVERSATION);
		filter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		filter.addAction(PublicIntent.FINISH_APPLICATION);
		mContext.registerReceiver(receiver, filter);
	}

	@Override
	protected void onStart() {
		super.onStart();
		isBound = bindService(new Intent(this, JNIService.class), mConnection,
				Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (isBound) {
			unbindService(mConnection);
		}
	}

	@Override
	public void onBackPressed() {
		requestQuit();
	}

	public void requestQuit() {
		if (exitedFlag) {
			GlobalConfig.saveLogoutFlag(this);
			Notificator.cancelSystemNotification(this);
			finish();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.exit(0);
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

	/**
	 * (non-Javadoc)
	 * 
	 * @see android.widget.TabHost.OnTabChangeListener#onTabChanged(java.lang.String)
	 */
	public void onTabChanged(String tag) {
		// TabInfo newTab = this.mapTabInfo.get(tag);
		int pos = this.mTabHost.getCurrentTab();
		// if (this.mViewPager != null) {
		this.mViewPager.setCurrentItem(pos);
		// }
	}

	@Override
	public void onPageScrollStateChanged(int pos) {

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mContext.unregisterReceiver(receiver);
		mContext.stopService(new Intent(this.getApplicationContext(),
				JNIService.class));
		V2Log.d("system destroyed v2tech");
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {

	}

	@Override
	public void onPageSelected(int pos) {
		this.mTabHost.setCurrentTab(pos);
	}

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
			return this.fragments.get(position);
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

	class LocalReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (PublicIntent.UPDATE_CONVERSATION.equals(action)) {
				if (GlobalHolder.getInstance().getNoticatorCount() > 0) {
					mTabNoticator.setVisibility(View.VISIBLE);
				} else {
					mTabNoticator.setVisibility(View.GONE);
				}
			} else if (PublicIntent.FINISH_APPLICATION.equals(action)) {
				finish();
			}
		}

	}

}