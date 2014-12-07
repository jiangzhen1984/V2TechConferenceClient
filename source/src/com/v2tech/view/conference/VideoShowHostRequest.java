package com.v2tech.view.conference;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.ConferenceService;
import com.v2tech.vo.ConferencePermission;
import com.v2tech.vo.PermissionState;
import com.v2tech.vo.User;

public class VideoShowHostRequest extends PopupWindow {

	private Set<User> users;
	
	private List<User> mList;

	private ListView lv;

	private LayoutInflater inflater;
	
	private ConferenceService service;

	public VideoShowHostRequest(Context context,  Set<User> list,
			ConferenceService service) {
		super(context);
		mList = new ArrayList<User>();
		mList.addAll(list);
		users = list;
		this.service = service;
		inflater = LayoutInflater.from(context);
		View root = inflater.inflate(R.layout.conference_request_host_list,
				null);
		lv = (ListView) root
				.findViewById(R.id.conference_request_host_list_view);
		lv.setAdapter(mAdapter);
		this.setContentView(root);
		this.setWidth(context.getResources().getDimensionPixelSize(R.dimen.conference_activity_request_host_list_width));
		this.setHeight(context.getResources().getDimensionPixelSize(R.dimen.conference_activity_request_host_list_height));
		this.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		this.setFocusable(true);
		this.setTouchable(true);
		this.setOutsideTouchable(true);
	}

	public void updateList(Set<User> list) {
		mList.clear();
		mList.addAll(list);
		users = list;
		mAdapter.notifyDataSetChanged();
	}

	private BaseAdapter mAdapter = new BaseAdapter() {

		@Override
		public int getCount() {
			return mList == null ? 0 : mList.size();
		}

		@Override
		public Object getItem(int position) {
			return mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return mList.get(position).getmUserId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = buildView(mList.get(position));
			}
			User user = mList.get(position);
			((TextView) convertView
					.findViewById(R.id.conference_request_host_user_name))
					.setText(user.getName());
			
			View acceptButton = convertView
			.findViewById(R.id.conference_request_host_button_accept);
			acceptButton.setTag(user);
			acceptButton.setOnClickListener(listener);
			
			View rejectButton = convertView
					.findViewById(R.id.conference_request_host_button_reject);
			rejectButton.setTag(user);
			rejectButton.setOnClickListener(listener);
			
			return convertView;
		}

		View buildView(User user) {
			View root = inflater.inflate(
					R.layout.conference_request_host_list_item, null);
			return root;
		}
		
		OnClickListener listener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				User user = (User) v.getTag();
				if (v.getId() == R.id.conference_request_host_button_reject) {
					
				} else if (v.getId() == R.id.conference_request_host_button_accept) {
					service.grantPermission(user, ConferencePermission.CONTROL, PermissionState.GRANTED, null);
				}
				mList.remove(user);
				users.remove(user);
				mAdapter.notifyDataSetChanged();
				VideoShowHostRequest.this.dismiss();
			}
			
		};

	};
	
	
}
