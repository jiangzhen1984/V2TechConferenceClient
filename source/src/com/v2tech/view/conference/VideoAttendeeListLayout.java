package com.v2tech.view.conference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.text.Editable;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.util.V2Log;
import com.v2tech.vo.Attendee;
import com.v2tech.vo.Conference;
import com.v2tech.vo.ConferencePermission;
import com.v2tech.vo.PermissionState;
import com.v2tech.vo.UserDeviceConfig;

public class VideoAttendeeListLayout extends LinearLayout {
	
	private Conference conf;

	private View rootView;

	private VideoAttendeeActionListener listener;

	private ListView mAttendeeContainer;

	private List<ViewWrapper> mCachedAttendsView;
	private List<ViewWrapper> mAttendsView;

	private boolean mIsStartedSearch;
	private EditText mSearchET;

	private View mPinButton;
	private TextView attendPersons;

	private AttendeesAdapter adapter = new AttendeesAdapter();

	private int onLinePersons = 0;
	private boolean initAttendPersons;

	public interface VideoAttendeeActionListener {

		public void OnAttendeeDragged(Attendee at, UserDeviceConfig udc, int x,
				int y);

		public void OnAttendeeClicked(Attendee at, UserDeviceConfig udc);

		public void requestAttendeeViewFixedLayout(View v);

		public void requestAttendeeViewFloatLayout(View v);

	};

	public VideoAttendeeListLayout(Conference conf, Context context) {
		super(context);
		this.conf = conf;
		initLayout();
	}


	private void initLayout() {
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.video_attendee_list_layout, null, false);

		attendPersons = (TextView) view.findViewById(R.id.video_attendee_pin_persons);
		mAttendeeContainer = (ListView) view
				.findViewById(R.id.video_attendee_container);
		mSearchET = (EditText) view.findViewById(R.id.attendee_search);

		mPinButton = view.findViewById(R.id.video_attendee_pin_button);
		mPinButton.setOnClickListener(mRequestFixedListener);

		mAttendeeContainer.setAdapter(adapter);
		mAttendeeContainer.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> ad, View view, int pos,
					long id) {
				if (listener != null) {
					Wrapper wr = (Wrapper) view.getTag();
					listener.OnAttendeeClicked(wr.a, wr.udc);
				}
			}

		});

		mAttendeeContainer.setFocusable(true);
		mAttendeeContainer.setFocusableInTouchMode(true);

		this.addView(view, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT));

		mSearchET.addTextChangedListener(mSearchListener);
		rootView = this;
	}

	public void setListener(VideoAttendeeActionListener listener) {
		this.listener = listener;
	}

	public void addNewAttendee(Attendee at) {
		mAttendsView.addAll(buildAttendeeView(at));
		Collections.sort(mAttendsView);
		attendPersons.setText(onLinePersons + "/" + mAttendsView.size());
		adapter.notifyDataSetChanged();
	}

	public void setAttendsList(Set<Attendee> l) {
		mAttendsView = new ArrayList<ViewWrapper>();
		initAttendPersons = true;
		// Copy list for void concurrency exception
		List<Attendee> list = new ArrayList<Attendee>(l);
		synchronized (mAttendsView) {
			for (Attendee at : list) {
				mAttendsView.addAll(buildAttendeeView(at));
			}
		}

		Collections.sort(mAttendsView);
		attendPersons.setText(onLinePersons + "/" + mAttendsView.size());
		initAttendPersons = false;
		adapter.notifyDataSetChanged();
	}

	private List<ViewWrapper> buildAttendeeView(final Attendee a) {
		Context ctx = this.getContext();
		List<ViewWrapper> list = new ArrayList<ViewWrapper>();

		View view = LayoutInflater.from(ctx).inflate(
				R.layout.video_attendee_device_layout, null, false);

		TextView nameTV = (TextView) view
				.findViewById(R.id.video_attendee_device_name);
		ImageView cameraIV = (ImageView) view
				.findViewById(R.id.video_attendee_device_camera_icon);
		ImageView spIV = (ImageView) view
				.findViewById(R.id.video_attendee_device_speaker_icon);

		cameraIV.setOnTouchListener(mListViewOnTouchListener);

		nameTV.setText(a.getAttName());

		// Set text color and camera icon
		setStyle(a, a.getDefaultDevice(), nameTV, cameraIV, spIV);

		view.setTag(new Wrapper(a, a.getDefaultDevice(), -1));
		list.add(new ViewWrapper(view));

		for (int i = 1; a.getmDevices() != null && i < a.getmDevices().size(); i++) {
			View view2 = LayoutInflater.from(ctx).inflate(
					R.layout.video_attendee_device_layout, null, false);
			TextView nameTV2 = (TextView) view2
					.findViewById(R.id.video_attendee_device_name);
			ImageView cameraIV2 = (ImageView) view2
					.findViewById(R.id.video_attendee_device_camera_icon);
			ImageView spIV2 = (ImageView) view2
					.findViewById(R.id.video_attendee_device_speaker_icon);

			UserDeviceConfig udc = a.getmDevices().get(i);
			nameTV2.setText("     视频" + i);
			nameTV2.setTextSize(20);

			// Set text color and camera icon
			setStyle(a, udc, nameTV2, cameraIV2, spIV2);

			cameraIV2.setOnTouchListener(mListViewOnTouchListener);
			view2.setTag(new Wrapper(a, udc, 1));
			list.add(new ViewWrapper(view2));
		}

		return list;

	}

	private void setStyle(Attendee at, UserDeviceConfig udc, TextView name,
			ImageView iv, ImageView speaker) {
		if (at.isChairMan() || conf.getChairman() == at.getAttId()) {
			name.setTextColor(getContext().getResources().getColor(
					R.color.video_attendee_chair_man_name_color));
			
			if(initAttendPersons && at.isJoined()){
				
				onLinePersons += 1;
			}
		} else if (at.isSelf()) {
			name.setTypeface(null, Typeface.BOLD);
			name.setTextColor(getContext().getResources().getColor(
					R.color.video_attendee_name_color));
			// set camera icon
			iv.setImageResource(R.drawable.camera);
			if(initAttendPersons){
				
				onLinePersons += 1;
			}
		} else if (at.isJoined()) {
			name.setTextColor(getContext().getResources().getColor(
					R.color.video_attendee_name_color));
			if(initAttendPersons){
				
				onLinePersons += 1;
			}
		} else {
			name.setTextColor(getContext().getResources().getColor(
					R.color.video_attendee_name_color_offline));

		}

		// set image view
		if (at.isSelf()) {
			iv.setImageResource(R.drawable.camera);
		} else if (at.isJoined()) {
			if (at.getType() != Attendee.TYPE_MIXED_VIDEO) {
				if (udc != null) {
					iv.setImageResource(R.drawable.camera);
				}
			} else {
				iv.setImageResource(R.drawable.mixed_video_camera);
			}
		} else {
			iv.setImageResource(R.drawable.camera_pressed);
		}

		if (at.getType() == Attendee.TYPE_MIXED_VIDEO) {
			speaker.setVisibility(View.INVISIBLE);
		}
	}
	
	private Attendee lastAttendee;
	public void updateEnteredAttendee(Attendee at) {
		at.setJoined(true);
		if(lastAttendee == null || !lastAttendee.equals(at)){
			
			lastAttendee = at;
			onLinePersons += 1;
		}
		
		if (mAttendsView == null) {
			mAttendsView = new ArrayList<ViewWrapper>();
		}
		int index = 0;
		synchronized (mAttendsView) {
			for (int i = 0; i < mAttendsView.size();) {
				ViewWrapper v = mAttendsView.get(i);
				Wrapper wr = (Wrapper) v.v.getTag();
				if (wr.a.getAttId() == at.getAttId()) {
					mAttendsView.remove(i);
					index = i;
				} else {
					i++; 
				}
			}
		}

		mAttendsView.addAll(index, buildAttendeeView(at));
		Collections.sort(mAttendsView);
		attendPersons.setText(onLinePersons + "/" + mAttendsView.size());
		adapter.notifyDataSetChanged();
	}

	public void updateExitedAttendee(Attendee at) {
		if (at == null) {
			return;
		}
		at.setJoined(false);
		at.setmDevices(null);
		if (mAttendsView == null) {
			return;
		}

		for (int i = 0; i < mAttendsView.size(); i++) {
			ViewWrapper v = mAttendsView.get(i);
			Wrapper wr = (Wrapper) v.v.getTag();
			if (wr.a.getAttId() == at.getAttId()) {
				// If attendee type is mixed video, remove destroyed mixed video
				if (wr.a.getType() == Attendee.TYPE_MIXED_VIDEO) {
					mAttendsView.remove(v);
					break;
				}
				wr.a.setJoined(false);
				ImageView cameraIV2 = (ImageView) v.v
						.findViewById(R.id.video_attendee_device_camera_icon);
				cameraIV2.setImageResource(R.drawable.camera_pressed);

				ImageView spIV = (ImageView) v.v
						.findViewById(R.id.video_attendee_device_speaker_icon);
				spIV.setImageResource(R.drawable.conf_speaker);

				// set offline color
				TextView nameTV = (TextView) v.v
						.findViewById(R.id.video_attendee_device_name);
				onLinePersons -= 1;
				lastAttendee = null;
				nameTV.setTextColor(getContext().getResources().getColor(
						R.color.video_attendee_name_color_offline));

				v.v.setBackgroundColor(Color.TRANSPARENT);

			}
		}

		Collections.sort(mAttendsView);
		attendPersons.setText(onLinePersons + "/" + mAttendsView.size());
		adapter.notifyDataSetChanged();
	}

	/**
	 * Update attendee speaker image according to user speaking state
	 * 
	 * @param at
	 * @param cp
	 * @param state
	 */
	public void updateAttendeeSpeakingState(Attendee at,
			ConferencePermission cp, PermissionState state) {
		View atView = null;
		synchronized (mAttendsView) {
			for (ViewWrapper v : mAttendsView) {
				Wrapper wr = (Wrapper) v.v.getTag();
				if (wr.a.getAttId() == at.getAttId()) {
					atView = v.v;
					break;
				}
			}
		}
		if (atView != null) {
			ImageView spIV = (ImageView) atView
					.findViewById(R.id.video_attendee_device_speaker_icon);
			if (state == PermissionState.NORMAL) {
				spIV.setImageResource(R.drawable.conf_speaker);
				at.setSpeakingState(false);
			} else if (state == PermissionState.GRANTED
					&& cp == ConferencePermission.SPEAKING) {
				spIV.setImageResource(R.drawable.conf_speaking);
				at.setSpeakingState(true);
				((AnimationDrawable) spIV.getDrawable()).start();
			}
		}
	}

	/**
	 * Remove attend and device from layout.
	 * 
	 * @param at
	 */
	public void removeAttendee(Attendee at) {
		synchronized (mAttendsView) {
			for (int i = 0; mAttendsView != null && i < mAttendsView.size(); i++) {
				ViewWrapper v = mAttendsView.get(i);
				Wrapper wr = (Wrapper) v.v.getTag();
				if (wr.a.getAttId() == at.getAttId()) {
					mAttendsView.remove(v);
					i--;
				}
			}
		}
		attendPersons.setText(onLinePersons + "/" + mAttendsView.size());
		adapter.notifyDataSetChanged();
	}

	/**
	 * Add attend and device from layout.
	 * 
	 * @param at
	 */
	public void addAttendee(Attendee at) {
		synchronized (mAttendsView) {
			mAttendsView.addAll(buildAttendeeView(at));
		}
		attendPersons.setText(onLinePersons + "/" + mAttendsView.size());
		adapter.notifyDataSetChanged();

	}

	/**
	 * Used to manually request FloatLayout, Because when this layout will hide,
	 * call this function to inform interface
	 */
	public void requestFloatLayout() {

		if ("float".equals(mPinButton.getTag())) {
			return;
		}

		if (this.listener != null) {
			this.listener.requestAttendeeViewFloatLayout(rootView);
		}

		mPinButton.setTag("float");
		((ImageView) mPinButton)
				.setImageResource(R.drawable.pin_button_selector);
	}

	/**
	 * request update background of current selected Item
	 * 
	 * @param flag
	 */
	public void updateCurrentSelectedBg(boolean flag, Attendee at,
			UserDeviceConfig udc) {

		for (int i = 0; i < mAttendsView.size(); i++) {
			ViewWrapper v = mAttendsView.get(i);
			Wrapper w = (Wrapper) v.v.getTag();
			if (w.a == at && udc == w.udc) {
				if (flag) {
					v.v.setBackgroundColor(getContext().getResources()
							.getColor(R.color.attendee_select_bg));
				} else {
					v.v.setBackgroundColor(Color.TRANSPARENT);
				}
				break;
			}
		}

	}

	private OnTouchListener mListViewOnTouchListener = new OnTouchListener() {

		private View dragView;
		private Bitmap cbm;

		@Override
		public synchronized boolean onTouch(View view, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {

				if (cbm != null && !cbm.isRecycled()) {
					cbm.recycle();
					cbm = null;
				}

				Wrapper w = (Wrapper) ((View) view.getParent().getParent())
						.getTag();
				dragView = getView(w);

				WindowManager wd = (WindowManager) getContext()
						.getSystemService(Context.WINDOW_SERVICE);
				WindowManager.LayoutParams vl = getLayoutParams(event);
				wd.addView(dragView, vl);
				mAttendeeContainer
						.setOverScrollMode(ListView.OVER_SCROLL_NEVER);
			} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				if (dragView != null) {
					WindowManager wd = (WindowManager) getContext()
							.getSystemService(Context.WINDOW_SERVICE);
					WindowManager.LayoutParams vl = getLayoutParams(event);
					wd.updateViewLayout(dragView, vl);
				}

			} else if (event.getAction() == MotionEvent.ACTION_UP
					|| event.getAction() == MotionEvent.ACTION_CANCEL) {
				if (dragView != null) {
					if (listener != null
							&& event.getAction() != MotionEvent.ACTION_CANCEL) {
						View itemView = (View) (View) view.getParent()
								.getParent();
						Wrapper wr = (Wrapper) itemView.getTag();
						// Rect r = new Rect();
						// mAttendeeContainer.getLocalVisibleRect(r);
						// if (r.contains((int) event.getX(), (int)
						// event.getY())) {
						// listener.OnAttendeeClicked(wr.a, wr.udc);
						// } else
						{
							listener.OnAttendeeDragged(wr.a, wr.udc,
									(int) event.getRawX(),
									(int) event.getRawY());
						}
					}
					WindowManager wd = (WindowManager) getContext()
							.getSystemService(Context.WINDOW_SERVICE);
					wd.removeView(dragView);
				}
				mAttendeeContainer
						.setOverScrollMode(ListView.OVER_SCROLL_ALWAYS);
				dragView = null;
			}
			return true;
		}

		private WindowManager.LayoutParams getLayoutParams(MotionEvent event) {
			WindowManager.LayoutParams vl = new WindowManager.LayoutParams();
			vl.format = PixelFormat.TRANSLUCENT;
			vl.gravity = Gravity.TOP | Gravity.LEFT;
			vl.width = 80;
			vl.height = 80;
			vl.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
			vl.x = (int) event.getRawX();
			vl.y = (int) event.getRawY();
			return vl;
		}

		private View getView(Wrapper wr) {
			RelativeLayout rl = new RelativeLayout(getContext());
			ImageView iv = new ImageView(getContext());
			Bitmap bm = wr.a.getAvatar();
			if (bm == null) {
				bm = BitmapFactory.decodeResource(getContext().getResources(),
						R.drawable.avatar);
				cbm = bm;
			}
			iv.setImageBitmap(bm);
			RelativeLayout.LayoutParams rll = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			rll.addRule(RelativeLayout.CENTER_IN_PARENT);
			rl.addView(iv, rll);

			TextView tv = new TextView(getContext());
			tv.setText(wr.a.getAttName());
			tv.setGravity(Gravity.CENTER);
			tv.setEllipsize(TruncateAt.END);
			tv.setMaxWidth(60);
			tv.setSingleLine();
			rl.addView(tv, rll);

			return rl;
		}
	};

	private OnClickListener mRequestFixedListener = new OnClickListener() {

		@Override
		public void onClick(View view) {

			if (view.getTag().equals("float")) {
				if (listener != null) {
					listener.requestAttendeeViewFixedLayout(rootView);
				}
			} else {
				if (listener != null) {
					listener.requestAttendeeViewFloatLayout(rootView);
				}
			}

			if (view.getTag().equals("float")) {
				view.setTag("fix");
				((ImageView) view)
						.setImageResource(R.drawable.pin_fixed_button_selector);
			} else {
				view.setTag("float");
				((ImageView) view)
						.setImageResource(R.drawable.pin_button_selector);
			}
		}

	};

	public boolean getWindowSizeState() {
		String str = (String) mPinButton.getTag();
		if (str == null || str.equals("float")) {
			return false;
		} else {
			return true;
		}
	}

	private TextWatcher mSearchListener = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable et) {
			String str = et.toString().trim();
			if (str.length() > 0) {
				if (!mIsStartedSearch) {
					mCachedAttendsView = mAttendsView;
					mIsStartedSearch = true;
				}
			} else {
				mAttendsView = mCachedAttendsView;
				adapter.notifyDataSetChanged();
				mIsStartedSearch = false;
				return;
			}
			List<ViewWrapper> searchedViewList = new ArrayList<ViewWrapper>();
			for (int i = 0; mCachedAttendsView != null
					&& i < mCachedAttendsView.size(); i++) {
				ViewWrapper v = mCachedAttendsView.get(i);
				Wrapper w = (Wrapper) v.v.getTag();
				if (w.a.getAttName() == null || w.a.getAbbraName() == null) {
					V2Log.w("Attendee name: " + w.a.getAttName() + "  arrba:"
							+ w.a.getAbbraName());
					continue;
				}
				if (w.a.getAttName().contains(str)
						|| w.a.getAbbraName().contains(str)) {
					searchedViewList.add(v);
				}
			}
			mAttendsView = searchedViewList;
			adapter.notifyDataSetChanged();
		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int cout) {

		}

	};

	class AttendeesAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mAttendsView == null ? 0 : mAttendsView.size();
		}

		@Override
		public Object getItem(int position) {
			return mAttendsView.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return mAttendsView.get(position).v;
		}

	}

	class ViewWrapper implements Comparable<ViewWrapper> {
		View v;

		public ViewWrapper(View v) {
			super();
			this.v = v;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + v.getTag().hashCode();
			result = prime * result
					+ ((v.getTag() == null) ? 0 : v.getTag().hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ViewWrapper other = (ViewWrapper) obj;
			if (!v.getTag().equals(other.v.getTag()))
				return false;
			if (v == null) {
				if (other.v != null)
					return false;
			} else if (!v.getTag().equals(other.v.getTag()))
				return false;
			return true;
		}

		@Override
		public int compareTo(ViewWrapper v) {
			return ((Wrapper) this.v.getTag())
					.compareTo((Wrapper) v.v.getTag());
		}

	}

	class Wrapper implements Comparable<Wrapper> {
		Attendee a;
		UserDeviceConfig udc;
		int sortFlag;

		public Wrapper(Attendee a, UserDeviceConfig udc, int sortFlag) {
			super();
			this.a = a;
			this.udc = udc;
			this.sortFlag = sortFlag;
		}

		@Override
		public int compareTo(Wrapper wr) {
			if (this.a == null) {
				return 1;
			}
			if (this.a.getType() == Attendee.TYPE_MIXED_VIDEO) {
				return -1;
			}

			if (wr.a == null) {
				return -1;
			}
			if (wr.a.getType() == Attendee.TYPE_MIXED_VIDEO) {
				return 1;
			}

			if (this.a.isSelf()) {
				return -1;
			} else if (wr.a.isSelf()) {
				return 1;
			}
			if (this.a == null) {
				V2Log.e(" attendee  is null ");
				return -1;
			} else if (wr.a == null) {
				V2Log.e(" wr attendee  is null ");
				return 1;
			} else {

				int ret = this.a.compareTo(wr.a);
				if (ret == 0) {
					return this.sortFlag;
				} else if (ret < 0 || ret > 0) {
					if (this.a.isJoined()) {
						return -1;
					} else if (wr.a.isJoined()) {
						return 1;
					} else {
						return 0;
					}
				} else {
					return ret;
				}
			}
		}

	}

}
