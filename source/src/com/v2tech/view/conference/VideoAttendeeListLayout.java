package com.v2tech.view.conference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.vo.Attendee;
import com.v2tech.vo.ConferencePermission;
import com.v2tech.vo.PermissionState;
import com.v2tech.vo.UserDeviceConfig;

public class VideoAttendeeListLayout extends LinearLayout {

	private VideoAttendeeActionListener listener;

	private ListView mAttendeeContainer;

	private Map<Long, Attendee> mAttends = new HashMap<Long, Attendee>();

	private List<View> mAttendsView;

	private AttendeesAdapter adapter = new AttendeesAdapter();

	public interface VideoAttendeeActionListener {
		public void OnAttendeeClicked(Attendee at, UserDeviceConfig udc);
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
		mAttendeeContainer.setAdapter(adapter);
		mAttendeeContainer.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> ad, View view, int pos,
					long id) {
				if (listener != null) {
					Wrapper wr = (Wrapper)view.getTag() ;
					listener.OnAttendeeClicked(wr.a,
							wr.udc);
				}
			}

		});

		this.addView(view, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT));
	}

	public void setListener(VideoAttendeeActionListener listener) {
		this.listener = listener;
	}

	public void setAttendsList(Set<Attendee> l) {
		mAttendsView = new ArrayList<View>();
		for (Attendee at : l) {
			mAttends.put(Long.valueOf(at.getUser().getmUserId()), at);
		}
		populate();
	}

	private void populate() {
		for (Attendee at : mAttends.values()) {
			mAttendsView.addAll(buildAttendeeView(at));
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
		ImageView cameraIV =(ImageView)view.findViewById(R.id.video_attendee_device_camera_icon);

		nameTV.setText(a.getUser().getName());

		if (a.isSelf() == false) {
			nameTV.setTextSize(20);
			if (a.getDefaultDevice() != null) {
				cameraIV.setImageResource(R.drawable.camera);
			}
		} else {
			nameTV.setTextSize(22);
			nameTV.setTypeface(null, Typeface.BOLD);
			cameraIV.setImageResource(R.drawable.camera);
		}
		view.setTag(new Wrapper(a, a.getDefaultDevice()));
		list.add(view);

		for (int i = 1; a.getmDevices() != null && i < a.getmDevices().size(); i++) {
			View view2 = LayoutInflater.from(ctx).inflate(
					R.layout.video_attendee_device_layout, null, false);
			TextView nameTV2 = (TextView) view2
					.findViewById(R.id.video_attendee_device_name);
			ImageView cameraIV2 =(ImageView)view2.findViewById(R.id.video_attendee_device_camera_icon);

			UserDeviceConfig udc = a.getmDevices().get(i);
			nameTV2.setText(a.getUser().getName() + (i > 0 ? ("_视频" + i) : ""));
			nameTV2.setTextSize(20);
			cameraIV2.setImageResource(R.drawable.camera);

			view2.setTag(new Wrapper(a, udc));
			list.add(view2);
		}

		return list;

	}

	public void updateEnteredAttendee(Attendee at) {
		mAttends.put(Long.valueOf(at.getUser().getmUserId()), at);
		mAttendsView.clear();
		populate();
	}

	public void updateExitedAttendee(Attendee at) {
		at.setmDevices(null);
		mAttends.put(Long.valueOf(at.getUser().getmUserId()), at);
		mAttendsView.clear();
		populate();
	}
	
	/**
	 * Update attendee speaker image according to user speaking state
	 * @param at
	 * @param cp
	 * @param state
	 */
	public void updateAttendeeSpeakingState(Attendee at,ConferencePermission cp, PermissionState state) {
		View atView = null;
		synchronized (mAttendsView) {
			for (View v : mAttendsView) {
				Wrapper wr = (Wrapper)v.getTag();
				if (wr.a.getUser().getmUserId() == at.getUser().getmUserId()) {
					atView = v;
					break;
				}
			}
		}
		if (atView != null) {
			ImageView spIV = (ImageView)atView.findViewById(R.id.video_attendee_device_speaker_icon);
			if (state == PermissionState.NORMAL) {
				spIV.setImageResource(R.drawable.conf_speaker);
			} else if (state == PermissionState.GRANTED) {
				spIV.setImageResource(R.drawable.conf_speaking);
			}
		}
	}
	
	/**
	 * Remove attend and device from layout.
	 * @param at
	 */
	public void removeAttendee(Attendee at) {
		mAttends.remove(Long.valueOf(at.getUser().getmUserId()));
		synchronized (mAttendsView) {
			for (View v : mAttendsView) {
				Wrapper wr = (Wrapper)v.getTag();
				if (wr.a.getUser().getmUserId() == at.getUser().getmUserId()) {
					mAttendsView.remove(v);
				}
			}
		}
		adapter.notifyDataSetChanged();
	}
	
	/**
	 * Add attend and device from layout.
	 * @param at
	 */
	public void addAttendee(Attendee at) {
		mAttendsView.addAll(buildAttendeeView(at));
		adapter.notifyDataSetChanged();
		
	}

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
