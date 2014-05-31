package com.v2tech.view.conference;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.util.V2Log;
import com.v2tech.vo.Attendee;
import com.v2tech.vo.ConferencePermission;
import com.v2tech.vo.PermissionState;
import com.v2tech.vo.UserDeviceConfig;

public class VideoAttendeeListLayout extends LinearLayout {

	private View rootView;

	private VideoAttendeeActionListener listener;

	private ListView mAttendeeContainer;

	private List<View> mCachedAttendsView;
	private List<View> mAttendsView;

	private boolean mIsStartedSearch;
	private EditText mSearchET;

	private View mPinButton;

	private AttendeesAdapter adapter = new AttendeesAdapter();

	public interface VideoAttendeeActionListener {

		public void OnAttendeeClicked(Attendee at, UserDeviceConfig udc);

		public void requestAttendeeViewFixedLayout(View v);

		public void requestAttendeeViewFloatLayout(View v);

	};

	public VideoAttendeeListLayout(Context context) {
		super(context);
		initLayout();
	}

	public VideoAttendeeListLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		initLayout();
	}

	public VideoAttendeeListLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		initLayout();
	}

	private void initLayout() {
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.video_attendee_list_layout, null, false);

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

		this.addView(view, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT));

		mSearchET.addTextChangedListener(mSearchListener);

		rootView = this;
	}

	public void setListener(VideoAttendeeActionListener listener) {
		this.listener = listener;
	}

	public void setAttendsList(Set<Attendee> l) {
		mAttendsView = new ArrayList<View>();
		// Copy list for void concurrency exception
		List<Attendee> list = new ArrayList<Attendee>(l);
		synchronized (mAttendsView) {
			for (Attendee at : list) {
				mAttendsView.addAll(buildAttendeeView(at));
			}
		}
		adapter.notifyDataSetChanged();
	}

	private List<View> buildAttendeeView(final Attendee a) {
		Context ctx = this.getContext();
		List<View> list = new ArrayList<View>();

		View view = LayoutInflater.from(ctx).inflate(
				R.layout.video_attendee_device_layout, null, false);

		TextView nameTV = (TextView) view
				.findViewById(R.id.video_attendee_device_name);
		ImageView cameraIV = (ImageView) view
				.findViewById(R.id.video_attendee_device_camera_icon);

		nameTV.setText(a.getAttName());

		if (a.isSelf() == false) {
			if ((a.getDefaultDevice() != null && a.isJoined()) || a.isSelf()) {
				cameraIV.setImageResource(R.drawable.camera);
			}
		} else {
			nameTV.setTypeface(null, Typeface.BOLD);
			if (a.isJoined() || a.isSelf()) {
				cameraIV.setImageResource(R.drawable.camera);
			}
		}

		// set online attendee color
		if (a.isJoined()) {
			nameTV.setTextColor(getContext().getResources().getColor(
					R.color.video_attendee_online_name_color));
		}

		if (a.isChairMan()) {
			nameTV.setTextColor(getContext().getResources().getColor(
					R.color.video_attendee_chair_man_name_color));
		}

		view.setTag(new Wrapper(a, a.getDefaultDevice()));
		list.add(view);

		for (int i = 1; a.getmDevices() != null && i < a.getmDevices().size(); i++) {
			View view2 = LayoutInflater.from(ctx).inflate(
					R.layout.video_attendee_device_layout, null, false);
			TextView nameTV2 = (TextView) view2
					.findViewById(R.id.video_attendee_device_name);
			ImageView cameraIV2 = (ImageView) view2
					.findViewById(R.id.video_attendee_device_camera_icon);
			UserDeviceConfig udc = a.getmDevices().get(i);
			nameTV2.setText(a.getAttName() + (i > 0 ? ("_视频" + i) : ""));
			nameTV2.setTextSize(20);
			// Hide additional speaker if user has more than one camera
			view2.findViewById(R.id.video_attendee_device_speaker_icon)
					.setVisibility(View.INVISIBLE);

			if (a.isJoined() || a.isSelf()) {
				cameraIV2.setImageResource(R.drawable.camera);
			}

			view2.setTag(new Wrapper(a, udc));
			list.add(view2);
		}

		return list;

	}

	public void updateEnteredAttendee(Attendee at) {
		at.setJoined(true);
		if (mAttendsView == null) {
			mAttendsView = new ArrayList<View>();
		}
		int index = 0;
		synchronized (mAttendsView) {
			for (int i = 0; i < mAttendsView.size();) {
				View v = mAttendsView.get(i);
				Wrapper wr = (Wrapper) v.getTag();
				if (wr.a.getAttId() == at.getAttId()) {
					mAttendsView.remove(i);
					index = i;
				} else {
					i++;
				}
			}
		}

		mAttendsView.addAll(index, buildAttendeeView(at));
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
			View v = mAttendsView.get(i);
			Wrapper wr = (Wrapper) v.getTag();
			if (wr.a.getAttId() == at.getAttId()) {
				wr.a.setJoined(false);
				ImageView cameraIV2 = (ImageView) v
						.findViewById(R.id.video_attendee_device_camera_icon);
				cameraIV2.setImageResource(R.drawable.camera_pressed);

				// set offline color
				TextView nameTV = (TextView) v
						.findViewById(R.id.video_attendee_device_name);

				nameTV.setTextColor(getContext().getResources().getColor(
						R.color.video_attendee_name_color));
			}
		}

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
			for (View v : mAttendsView) {
				Wrapper wr = (Wrapper) v.getTag();
				if (wr.a.getAttId() == at.getAttId()) {
					atView = v;
					break;
				}
			}
		}
		if (atView != null) {
			ImageView spIV = (ImageView) atView
					.findViewById(R.id.video_attendee_device_speaker_icon);
			if (state == PermissionState.NORMAL) {
				spIV.setImageResource(R.drawable.conf_speaker);
			} else if (state == PermissionState.GRANTED) {
				spIV.setImageResource(R.drawable.conf_speaking);
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
				View v = mAttendsView.get(i);
				Wrapper wr = (Wrapper) v.getTag();
				if (wr.a.getAttId() == at.getAttId()) {
					mAttendsView.remove(v);
					i--;
				}
			}
		}
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
		adapter.notifyDataSetChanged();

	}

	/**
	 * Used to manually request FloatLayout, Because when this layout will hide,
	 * call this function to inform interface
	 */
	public void requestFloatLayout() {
		if (this.listener != null) {
			this.listener.requestAttendeeViewFloatLayout(rootView);
		}

		mPinButton.setTag("float");
		((ImageView) mPinButton)
				.setImageResource(R.drawable.pin_button_selector);
	}

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
			List<View> searchedViewList = new ArrayList<View>();
			for (int i = 0; mCachedAttendsView != null
					&& i < mCachedAttendsView.size(); i++) {
				View v = mCachedAttendsView.get(i);
				Wrapper w = (Wrapper) v.getTag();
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
			return mAttendsView.get(position);
		}

	}

	class Wrapper {
		Attendee a;
		UserDeviceConfig udc;

		public Wrapper(Attendee a, UserDeviceConfig udc) {
			super();
			this.a = a;
			this.udc = udc;
		}

	}

}
