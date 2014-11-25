package com.v2tech.view;

import java.util.ArrayList;
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

import com.V2.jni.V2GlobalEnum;
import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.db.V2techSearchContentProvider;
import com.v2tech.service.ChatService;
import com.v2tech.service.FileOperationEnum;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.Notificator;
import com.v2tech.view.conversation.CommonCallBack;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.view.conversation.CommonCallBack.CommonUpdateConversationStateInterface;
import com.v2tech.view.conversation.CommonCallBack.CommonUpdateFileStateInterface;
import com.v2tech.view.conversation.MessageBuilder;
import com.v2tech.view.receiver.HeadSetPlugReceiver;
import com.v2tech.view.widget.CommonAdapter.CommonAdapterItemWrapper;
import com.v2tech.view.widget.TitleBar;
import com.v2tech.vo.Conference;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.NetworkStateCode;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageFileItem;

public class MainActivity extends FragmentActivity implements
		NotificationListener , CommonUpdateFileStateInterface{

	private Context mContext;
	private boolean exitedFlag = false;

	private TitleBar titleBar;
	private EditText searchEdit;
	private TabHost mTabHost;
	private ViewPager mViewPager;
	private PagerAdapter mPagerAdapter;
	
	private ConferenceListener mConfListener;
	
	private List<CommonAdapterItemWrapper> messageArray;
	private List<Fragment> fragments;
	
	private HeadSetPlugReceiver localReceiver = new HeadSetPlugReceiver();

	private static final int SUB_ACTIVITY_CODE_CREATE_CONF = 100;

	public static final String SERVICE_BOUNDED_EVENT = "com.v2tech.SERVICE_BOUNDED_EVENT";
	public static final String SERVICE_UNBOUNDED_EVENT = "com.v2tech.SERVICE_UNBOUNDED_EVENT";
	private static final String TAG = "MainActivity";

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
		// Inflate the layout
		setContentView(R.layout.activity_main);
		// Initialise the TabHost
		mContext = this;
		searchEdit = (EditText) findViewById(R.id.search_edit);
		// Init title bar
		View titleBarLayout = findViewById(R.id.title_bar_ly);
		titleBar = new TitleBar(mContext, titleBarLayout);
		titleBar.regsiterSearchedTextListener(searchTextWatcher);

		// Initialize first title name
		titleBar.updateTitle(mTabClasses[0].mTabTitleId);

		this.initialiseTabHost(savedInstanceState);
		if (savedInstanceState != null) {
			mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
		}

		int index = getIntent().getIntExtra("initFragment", 0);
		// Intialise ViewPager
		this.intialiseViewPager(index);
		initReceiver();
		// Start animation
		this.overridePendingTransition(R.animator.left_in, R.animator.left_out);
//		CommonCallBack.getInstance().setFileStateInterface(this);
		messageArray = new ArrayList<CommonAdapterItemWrapper>();
		updateFileState();
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

	/**
	 * Initialise ViewPager
	 */
	private void intialiseViewPager(int index) {

		fragments = new Vector<Fragment>();

		for (TabClass tc : mTabClasses) {
			Bundle bundle = new Bundle();
			bundle.putString("tag", tc.mTabName);
			Fragment frg = Fragment.instantiate(this, tc.clsName, bundle);
			if (frg instanceof ConferenceListener && tc.mTabName.equals(PublicIntent.TAG_CONF)) {
				mConfListener =(ConferenceListener) frg;
			}
			fragments.add(frg);

		}

		this.mPagerAdapter = new PagerAdapter(
				super.getSupportFragmentManager(), fragments);
		this.mViewPager = (ViewPager) super.findViewById(R.id.viewpager);
		this.mViewPager.setAdapter(this.mPagerAdapter);
		this.mViewPager.setOnPageChangeListener(pageChangeListener);
		this.mViewPager.setOffscreenPageLimit(5);
		this.mViewPager.setCurrentItem(index);
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
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		filter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		filter.addAction(PublicIntent.FINISH_APPLICATION);
		filter.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		mContext.registerReceiver(receiver, filter);
		
		//Register listener for headset
		IntentFilter filterHeadSet = new IntentFilter();
		filterHeadSet.addAction(Intent.ACTION_HEADSET_PLUG);
		Intent result = mContext.registerReceiver(localReceiver, filterHeadSet);
		if (result != null) {
			localReceiver.onReceive(this, result);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
	}
	
	

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		int index = intent.getIntExtra("initFragment", 0);
		mViewPager.setCurrentItem(index);
		if (intent.getExtras() != null) {
			Conference conf= (Conference)intent.getExtras().get("conf");
			mConfListener.requestJoinConf(conf);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
	}

	public void requestQuit() {
		if (exitedFlag) {
			this.getApplicationContext().stopService(
					new Intent(this.getApplicationContext(), JNIService.class));
			finish();
			((MainApplication)this.getApplicationContext()).requestQuit();

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
		//unregister for headset
		mContext.unregisterReceiver(localReceiver);
		mContext.stopService(new Intent(this.getApplicationContext(),
				JNIService.class));
		//Just for user remove application from recent task list
		Notificator
		.cancelAllSystemNotification(getApplicationContext());

		V2Log.d("system destroyed v2tech");
	}

	public void updateNotificator(int type, boolean flag) {

		View noticator = null;
		if (type == Conversation.TYPE_GROUP || type == V2GlobalEnum.GROUP_TYPE_DEPARTMENT) {
			noticator = mTabClasses[2].notificator;
		} else if (type ==Conversation.TYPE_CONFERNECE) {
			noticator = mTabClasses[3].notificator;
		} else if (type ==Conversation.TYPE_CONTACT) {
			noticator = mTabClasses[4].notificator;
		}
		
		if (flag) {
			noticator.setVisibility(View.VISIBLE);
		} else {
			noticator.setVisibility(View.GONE);
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
			((TextWatcher) ((FragmentPagerAdapter) mViewPager.getAdapter())
					.getItem(mTabHost.getCurrentTab())).beforeTextChanged(arg0, arg1, arg2, arg3);
		}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
			((TextWatcher) ((FragmentPagerAdapter) mViewPager.getAdapter())
					.getItem(mTabHost.getCurrentTab())).onTextChanged(arg0, arg1, arg2, arg3);
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
			//恢复搜索状态
			TabClass tab = mTabClasses[pos];
			Fragment fragment = fragments.get(pos);
			if(tab.mTabName.equals(PublicIntent.TAG_COV) || tab.mTabName.equals(PublicIntent.TAG_GROUP) || 
					tab.mTabName.equals(PublicIntent.TAG_CONF)){
				((ConversationsTabFragment)fragment).updateSearchState();
			} else if(tab.mTabName.equals(PublicIntent.TAG_CONTACT) || tab.mTabName.equals(PublicIntent.TAG_ORG)){
				
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
			searchEdit.setText("");
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
			if (PublicIntent.FINISH_APPLICATION.equals(action)) {
				exitedFlag = true;
				//把会话中所有发送或下载的文件，状态更新到数据库
//				executeUpdateFileState();
				requestQuit();
			} else if (JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION
					.equals(action)) {
				NetworkStateCode code = (NetworkStateCode) intent.getExtras()
						.get("state");
				GlobalHolder.getInstance().setServerConnection(code == NetworkStateCode.CONNECTED);
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
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		  View v = getCurrentFocus(); 
		    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);  
		    if (imm != null && v != null) {  
		        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);  
		    }  
		  V2techSearchContentProvider.closedDataBase();  
		return super.dispatchTouchEvent(ev);
	}
	
	/**
	 * Detecting all VMessageFileItem Object that state is sending or downloading in the database , 
	 * and change their status to failed..  
	 */
	public void updateFileState() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				List<VMessageFileItem> loadFileMessages = MessageLoader.loadFileMessages(-1, -1);
				if(loadFileMessages != null){
					for (VMessageFileItem fileItem : loadFileMessages) {
						if (fileItem.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADING)
							fileItem.setState(VMessageAbstractItem.STATE_FILE_DOWNLOADED_FALIED);
						else if (fileItem.getState() == VMessageAbstractItem.STATE_FILE_SENDING)
							fileItem.setState(VMessageAbstractItem.STATE_FILE_SENT_FALIED);
						int update = MessageLoader.updateFileItemState(mContext , fileItem);
						if(update == -1){
							V2Log.e(TAG, "update file state failed... file id is : " + fileItem.getUuid());
						}
					}
				}
				else
					V2Log.e(TAG, "load all files failed... get null");
			}
		}).start();
	}

	@Override
	public void updateFileState(List<CommonAdapterItemWrapper> messageArray) {
		this.messageArray.addAll(messageArray);
	}
	
	/**
	 * now it's not used..
	 * @deprecated
	 */
	public void executeUpdateFileState(){
		
		if(messageArray == null){
			V2Log.e(TAG, "executeUpdateFileState is failed ... because messageArray is null");
			return ;
		}
			
		ChatService mChat = new ChatService();
		for (int i = 0; i < messageArray.size(); i++) {
			VMessage vm = (VMessage) messageArray.get(i)
					.getItemObject();
			if (vm.getFileItems().size() > 0) {
				List<VMessageFileItem> fileItems = vm.getFileItems();
				for (int j = 0; j < fileItems.size(); j++) {
					VMessageFileItem item = fileItems.get(j);
					switch (item.getState()) {
					case VMessageAbstractItem.STATE_FILE_DOWNLOADING:
					case VMessageAbstractItem.STATE_FILE_PAUSED_DOWNLOADING:
						item.setState(VMessageFileItem.STATE_FILE_DOWNLOADED_FALIED);
						MessageBuilder.updateVMessageItemToSentFalied(
								mContext, vm);
						mChat.updateFileOperation(item,
								FileOperationEnum.OPERATION_CANCEL_DOWNLOADING,
								null);
						break;
					case VMessageAbstractItem.STATE_FILE_SENDING:
					case VMessageAbstractItem.STATE_FILE_PAUSED_SENDING:
						item.setState(VMessageFileItem.STATE_FILE_SENT_FALIED);
						MessageBuilder.updateVMessageItemToSentFalied(
								mContext, vm);
						mChat.updateFileOperation(item,
								FileOperationEnum.OPERATION_CANCEL_SENDING,
								null);
						break;
					default:
						break;
					}
				}
			}
		}
	}
}
