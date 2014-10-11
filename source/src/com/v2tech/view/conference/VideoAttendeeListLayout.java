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
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
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

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.vo.Attendee;
import com.v2tech.vo.Conference;
import com.v2tech.vo.ConferencePermission;
import com.v2tech.vo.PermissionState;
import com.v2tech.vo.UserDeviceConfig;

public class VideoAttendeeListLayout extends LinearLayout {

	private static final String TAG = "VideoAttendeeListLayout";

	private static final int DEFAULT_DEVICE_FLAG = -1;

	private Conference conf;

	private View rootView;

	private VideoAttendeeActionListener listener;

	private ListView mAttendeeContainer;

	private List<Wrapper> mList;

	private boolean mIsStartedSearch;
	private EditText mSearchET;

	private View mPinButton;
	private TextView attendPersons;

	private AttendeesAdapter adapter = new AttendeesAdapter();

	private int onLinePersons = 0;
	private int mAttendeeCount = 0;
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
		mList = new ArrayList<Wrapper>();
		initLayout();
	}

	private void initLayout() {
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.video_attendee_list_layout, null, false);

		attendPersons = (TextView) view
				.findViewById(R.id.video_attendee_pin_persons);
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
					if (wr.udc == null || !wr.udc.isEnable()) {
						V2Log.i("User device config is disabled ");
						return;
					}

					listener.OnAttendeeClicked(wr.a, wr.udc);

					if (!wr.udc.isShowing()) {
						view.setBackgroundColor(Color.WHITE);
					} else {
						view.setBackgroundColor(getContext().getResources()
								.getColor(R.color.attendee_select_bg));
					}

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
		if (at == null) {
			return;
		}
		V2Log.d(TAG, "addNewAttendee 执行了");
		if (at.getType() != Attendee.TYPE_MIXED_VIDEO
				&& !TextUtils.isEmpty(at.getAttName())) {
			configAttendee(at);
		}
		List<UserDeviceConfig> dList = at.getmDevices();
		int i = 0;
		int deviceIndex = 1;
		do {
			if (dList == null) {
				mList.add(new Wrapper(at, null, DEFAULT_DEVICE_FLAG));
			} else {
				mList.add(new Wrapper(at, dList.get(i),
						i == 0 ? DEFAULT_DEVICE_FLAG : deviceIndex++));
			}
			i++;
		} while (dList != null && i < dList.size());

		Collections.sort(mList);
		adapter.notifyDataSetChanged();
	}

	public void addNewAttendee(List<Attendee> atList) {
		if (atList == null) {
			return;
		}
		for (int index = 0; index < atList.size(); index++) {
			Attendee at = atList.get(index);
			if (at.getType() != Attendee.TYPE_MIXED_VIDEO
					&& !TextUtils.isEmpty(at.getAttName())) {
				configAttendee(at);
			}
			List<UserDeviceConfig> dList = at.getmDevices();
			int i = 0;
			int deviceIndex = 1;
			do {
				if (dList == null) {
					mList.add(new Wrapper(at, null, DEFAULT_DEVICE_FLAG));
				} else {
					mList.add(new Wrapper(at, dList.get(i),
							i == 0 ? DEFAULT_DEVICE_FLAG : deviceIndex++));
				}
				i++;
			} while (dList != null && i < dList.size());
		}
		Collections.sort(mList);
		adapter.notifyDataSetChanged();
	}

	private View buildAttendeeView(Wrapper wr) {

		Context ctx = this.getContext();
		View view = LayoutInflater.from(ctx).inflate(
				R.layout.video_attendee_device_layout, null, false);

		ImageView cameraIV = (ImageView) view
				.findViewById(R.id.video_attendee_device_camera_icon);

		cameraIV.setOnTouchListener(mListViewOnTouchListener);

		updateView(wr, view);
		return view;

	}

	private void updateView(Wrapper wr, View view) {
		if (wr == null || view == null) {
			return;
		}
		view.setTag(wr);

		TextView nameTV = (TextView) view
				.findViewById(R.id.video_attendee_device_name);
		ImageView cameraIV = (ImageView) view
				.findViewById(R.id.video_attendee_device_camera_icon);
		ImageView spIV = (ImageView) view
				.findViewById(R.id.video_attendee_device_speaker_icon);

		if (wr.sortFlag != DEFAULT_DEVICE_FLAG) {
			nameTV.setText("     "
					+ getContext().getText(
							R.string.confs_user_video_device_item)
					+ wr.sortFlag);
		} else {
			nameTV.setText(wr.a.getAttName());
		}

		if (wr.udc != null) {
			if (!wr.udc.isShowing()) {
				view.setBackgroundColor(Color.WHITE);
			} else {
				view.setBackgroundColor(getContext().getResources().getColor(
						R.color.attendee_select_bg));
			}
		} else {
			view.setBackgroundColor(Color.WHITE);
		}

		// Set text color and camera icon
		setStyle(wr.a, wr.udc, nameTV, cameraIV, spIV);
	}

	/**
	 * 
	 * @param at
	 * @param udc
	 * @param name
	 * @param iv
	 *            camera imave view
	 * @param speaker
	 *            speaker image view
	 */
	private void setStyle(Attendee at, UserDeviceConfig udc, TextView name,
			ImageView iv, ImageView speaker) {
		if (at.isChairMan() || conf.getChairman() == at.getAttId()) {

			if (at.isSelf() || at.isJoined()) {

				name.setTextColor(getContext().getResources().getColor(
						R.color.video_attendee_chair_man_name_color));
			}
		} else if (at.isSelf()) {
			name.setTypeface(null, Typeface.BOLD);
			name.setTextColor(getContext().getResources().getColor(
					R.color.video_attendee_name_color));
			// set camera icon
			iv.setImageResource(R.drawable.camera);
		} else if (at.isJoined()) {
			name.setTextColor(getContext().getResources().getColor(
					R.color.video_attendee_name_color));
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

		if (udc != null && !udc.isEnable()
				&& at.getType() == Attendee.TYPE_ATTENDEE && at.isSelf()) {
			iv.setImageResource(R.drawable.camera_pressed);
		}

		// Update speaking animation
		if (speaker.getVisibility() == View.VISIBLE) {
			if (!at.isSpeaking()) {
				speaker.setImageResource(R.drawable.conf_speaker);
			} else {
				speaker.setImageResource(R.drawable.conf_speaking);
				((AnimationDrawable) speaker.getDrawable()).start();
			}
		}
	}

	public void setAttendsList(Set<Attendee> l) {
		// Copy list for void concurrency exception
		List<Attendee> list = new ArrayList<Attendee>(l);
		for (Attendee at : list) {
			List<UserDeviceConfig> dList = at.getmDevices();

			int i = 0;
			int deviceIndex = 1;
			do {
				if (dList == null) {
					mList.add(new Wrapper(at, null, DEFAULT_DEVICE_FLAG));
				} else {
					mList.add(new Wrapper(at, dList.get(i),
							i == 0 ? DEFAULT_DEVICE_FLAG : deviceIndex++));
				}
				i++;
			} while (dList != null && i < dList.size());

			configAttendee(at);

		}
		Collections.sort(mList);
		adapter.notifyDataSetChanged();
	}

	public void updateEnteredAttendee(Attendee at) {
		if (at == null) {
			return;
		}

		V2Log.d(TAG,
				"updateEnteredAttendee 执行了" + at.getAttName() + "---"
						+ at.isJoined());
		at.setJoined(true);
		configAttendee(at);
		// TODO update device count and mixed video

		Collections.sort(mList);
		adapter.notifyDataSetChanged();
	}

	public void updateExitedAttendee(Attendee at) {
		V2Log.d(TAG, "updateExitedAttendee 执行了");
		if (at == null) {
			return;
		}

		at.setmDevices(null);
		at.setJoined(false);
		configAttendee(at);
		boolean found = false;
		for (int i = 0; i < mList.size(); i++) {
			Wrapper wr = mList.get(i);
			// Remove attendee devices, leave one device item
			if (wr.a.getAttId() == at.getAttId()) {
				if (found) {
					mList.remove(i--);
					continue;
				} else {
					found = true;
				}
			}
		}

		// Update on line count
		if (at.getType() == Attendee.TYPE_MIXED_VIDEO) {
			onLinePersons--;
			updateStatist();
		}
		// if (mAttendsView == null) {
		// return;
		// }
		//
		// if (at.getType() != Attendee.TYPE_MIXED_VIDEO) {
		//
		// configAttendee();
		// }
		// for (int i = 0; i < mAttendsView.size(); i++) {
		// ViewWrapper v = mAttendsView.get(i);
		// Wrapper wr = (Wrapper) v.v.getTag();
		// if (wr.a.getAttId() == at.getAttId()) {
		// // If attendee type is mixed video, remove destroyed mixed video
		// if (wr.a.getType() == Attendee.TYPE_MIXED_VIDEO) {
		// V2Log.d(TAG, "successful remove 混合视频");
		// mAttendsView.remove(v);
		// onLineDevices -= 1;
		// break;
		// }
		// wr.a.setJoined(false);
		// ImageView cameraIV2 = (ImageView) v.v
		// .findViewById(R.id.video_attendee_device_camera_icon);
		// cameraIV2.setImageResource(R.drawable.camera_pressed);
		//
		// ImageView spIV = (ImageView) v.v
		// .findViewById(R.id.video_attendee_device_speaker_icon);
		// spIV.setImageResource(R.drawable.conf_speaker);
		//
		// // set offline color
		// TextView nameTV = (TextView) v.v
		// .findViewById(R.id.video_attendee_device_name);
		// nameTV.setTextColor(getContext().getResources().getColor(
		// R.color.video_attendee_name_color_offline));
		//
		// v.v.setBackgroundColor(Color.WHITE);
		//
		// }
		// }
		Collections.sort(mList);
		adapter.notifyDataSetChanged();
	}

	/**
	 * Update attendee device status
	 * 
	 * @param att
	 * @param udc
	 */
	public void updateAttendeeDevice(Attendee att, UserDeviceConfig udc) {
		// View atView = null;
		// synchronized (mAttendsView) {
		// for (ViewWrapper v : mAttendsView) {
		// Wrapper wr = (Wrapper) v.v.getTag();
		// if (wr.a.getAttId() == att.getAttId()) {
		// atView = v.v;
		// ImageView spIV = (ImageView) atView
		// .findViewById(R.id.video_attendee_device_camera_icon);
		// if (udc.isEnable()) {
		// spIV.setImageResource(R.drawable.camera);
		// } else {
		// spIV.setImageResource(R.drawable.camera_pressed);
		// atView.setBackgroundColor(Color.WHITE);
		// }
		// // If user doesn't exist video device before, update video
		// // device
		// if (wr.udc == null) {
		// wr.udc = udc;
		// }
		// wr.udc.setShowing(false);
		// wr.udc.setEnable(udc.isEnable());
		// break;
		// }
		// }
		// }

		for (int i = 0; i < mList.size(); i++) {
			Wrapper wr = mList.get(i);
			// Remove attendee devices, leave one device item
			if (wr.a.getAttId() == att.getAttId()) {
				// If user doesn't exist device before, then set
				if (wr.udc == null) {
					wr.udc = udc;
				} else {
					wr.udc.setShowing(false);
					wr.udc.setEnable(udc.isEnable());
				}
			}
		}
		adapter.notifyDataSetChanged();

	}

	/**
	 * Update attendee speaker image according to user speaking state
	 * 
	 * @param at
	 * @param cp
	 * @param state
	 */
	public void updateAttendeeSpeakingState(Attendee at) {
		adapter.notifyDataSetChanged();
	}

	/**
	 * Remove attend and device from layout.
	 * 
	 * @param at
	 */
	public void removeAttendee(Attendee at) {
		configAttendee(at);
		for (int i = 0; i < mList.size(); i++) {
			Wrapper wr = mList.get(i);
			// Remove attendee devices, leave one device item
			if (wr.a.getAttId() == at.getAttId()) {
				mList.remove(i--);
				continue;
			}
		}
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

		// for (int i = 0; i < mAttendsView.size(); i++) {
		// ViewWrapper v = mAttendsView.get(i);
		// Wrapper w = (Wrapper) v.v.getTag();
		// if (w.a == at && udc == w.udc) {
		// if (flag) {
		// v.v.setBackgroundColor(getContext().getResources()
		// .getColor(R.color.attendee_select_bg));
		// } else {
		// v.v.setBackgroundColor(Color.WHITE);
		// }
		// v.v.invalidate();
		// break;
		// }
		// }

		adapter.notifyDataSetChanged();
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
			// String str = et.toString().trim();
			// if (str.length() > 0) {
			// if (!mIsStartedSearch) {
			// mCachedAttendsView = mAttendsView;
			// mIsStartedSearch = true;
			// }
			// } else {
			// mAttendsView = mCachedAttendsView;
			// adapter.notifyDataSetChanged();
			// mIsStartedSearch = false;
			// return;
			// }
			// List<ViewWrapper> searchedViewList = new
			// ArrayList<ViewWrapper>();
			// for (int i = 0; mCachedAttendsView != null
			// && i < mCachedAttendsView.size(); i++) {
			// ViewWrapper v = mCachedAttendsView.get(i);
			// Wrapper w = (Wrapper) v.v.getTag();
			// if (w.a.getAttName() == null || w.a.getAbbraName() == null) {
			// V2Log.w("Attendee name: " + w.a.getAttName() + "  arrba:"
			// + w.a.getAbbraName());
			// continue;
			// }
			// if (w.a.getAttName().contains(str)
			// || w.a.getAbbraName().contains(str)) {
			// searchedViewList.add(v);
			// }
			// }
			// mAttendsView = searchedViewList;
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
			return mList.size();
		}

		@Override
		public Object getItem(int position) {
			return mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = buildAttendeeView(mList.get(position));
			} else {
				updateView(mList.get(position), convertView);
			}
			return convertView;
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

	private void configAttendee(Attendee at) {
		if (at == null) {
			return;
		}
		if (at.getType() != Attendee.TYPE_MIXED_VIDEO) {
			mAttendeeCount++;
			if ((at.isJoined() || at.isSelf())) {
				onLinePersons++;
			}
		}
		updateStatist();

	}

	private void updateStatist() {
		attendPersons.setText(onLinePersons + "/" + (mAttendeeCount));
	}
}
