package com.v2tech.view.conference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.graphics.Color;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.logic.Attendee;
import com.v2tech.logic.UserDeviceConfig;

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
		
		mAttendeeContainer = (ListView)view.findViewById(R.id.video_attendee_container);
		mAttendeeContainer.setAdapter(adapter);
		mAttendeeContainer.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?>  ad, View view, int pos,
					long id) {
				if (listener != null) {
					listener.OnAttendeeClicked(mAttends.get(pos), (UserDeviceConfig)view.getTag());
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
		for (Attendee at: mAttends.values()) {
			mAttendsView.addAll(buildAttendeeView(at));
		}
		adapter.notifyDataSetChanged();
	}
	
	
	private List<View> buildAttendeeView(final Attendee a) {
		Context ctx = this.getContext();
		List<View> list = new ArrayList<View>();
		
		RelativeLayout rl = new RelativeLayout(ctx);
		rl.setBackgroundColor(Color.WHITE);

		TextView tv = new TextView(ctx);
		final ImageView iv = new ImageView(ctx);
		tv.setTextColor(Color.BLACK);
		tv.setText(a.getUser().getName());

		if (a.isSelf() == false) {
			tv.setTextSize(20);
			iv.setImageResource(R.drawable.camera);
			iv.setTag("camera");
		} else {
			tv.setTextSize(22);
			tv.setTypeface(null, Typeface.BOLD);
			iv.setImageResource(R.drawable.camera_showing);
			iv.setTag("camera_showing");
		}

		RelativeLayout.LayoutParams rp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		rp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		rp.addRule(RelativeLayout.CENTER_VERTICAL);
		rp.leftMargin = 20;
		rl.addView(tv, rp);

		rl.setTag(a.getDefaultDevice());

		// add secondary video
		RelativeLayout.LayoutParams rpIv = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		rpIv.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		rpIv.addRule(RelativeLayout.CENTER_VERTICAL);
		rpIv.rightMargin = 20;
		rl.addView(iv, rpIv);
		list.add(rl);
		

		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);

		for (int i = 1; a.getmDevices() != null && i < a.getmDevices().size(); i++) {
			RelativeLayout rlm = new RelativeLayout(ctx);

			final UserDeviceConfig udc = a.getmDevices().get(i);
			TextView tt = new TextView(ctx);
			tt.setText(a.getUser().getName() + (i > 0 ? ("_视频" + i) : ""));
			tt.setTextSize(20);
			tt.setTextColor(Color.BLACK);

			RelativeLayout.LayoutParams rp1 = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);

			rp1.leftMargin = 20;
			rlm.addView(tt, rp1);

			final ImageView ivm = new ImageView(ctx);
			ivm.setImageResource(R.drawable.camera);
			ivm.setTag("camera");
			rlm.setTag(udc);

			rlm.addView(ivm, rpIv);
			list.add(rlm);
		}

		return list;
	
	}
	
	
	
	public void updateEnteredAttendee(Attendee at) {
		mAttends.put(Long.valueOf(at.getUser().getmUserId()), at);
		mAttendsView.clear();
		populate();
	}
	
	public void updateExitedAttendee(Attendee at) {
		mAttends.put(Long.valueOf(at.getUser().getmUserId()), at);
		mAttendsView.clear();
		populate();
	}
	
	
	class AttendeesAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mAttendsView==null? 0 : mAttendsView.size();
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
	
}
