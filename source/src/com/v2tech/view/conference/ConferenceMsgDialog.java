package com.v2tech.view.conference;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.v2tech.R;
import com.v2tech.service.ConferenceService;
import com.v2tech.vo.ConferencePermission;
import com.v2tech.vo.PermissionState;
import com.v2tech.vo.User;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ConferenceMsgDialog extends Dialog {

	private Set<User> users;
	private List<User> mList;
	private ListView lv;
	private LayoutInflater inflater;
	private ConferenceService service;
	private BaseAdapter mAdapter = new ListAdapter();

	public ConferenceMsgDialog(Context context, Set<User> list,
			ConferenceService service) {
		super(context, R.style.DialogStyle1);
		setCanceledOnTouchOutside(true);
		mList = new ArrayList<User>();
		mList.addAll(list);
		users = list;
		this.service = service;
		initLayout(context);
	}

	private void initLayout(Context context) {
		inflater = LayoutInflater.from(context);
		View root = inflater.inflate(R.layout.conference_request_host_list,
				null);
		lv = (ListView) root
				.findViewById(R.id.conference_request_host_list_view);
		lv.setAdapter(mAdapter);

		int height = ((WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
				.getHeight();

		this.setContentView(root, new LayoutParams(LayoutParams.WRAP_CONTENT,
				(int) (height * 2 / 3 + 0.5f)));
	}

	public void updateList(Set<User> list) {
		mList.clear();
		mList.addAll(list);
		users = list;
		mAdapter.notifyDataSetChanged();
	}

	class ListAdapter extends BaseAdapter {

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
				convertView = inflater.inflate(
						R.layout.conference_request_host_list_item, null);
			}
			User user = mList.get(position);
			((TextView) convertView
					.findViewById(R.id.conference_request_host_user_name))
					.setText(user.getName());

			View acceptButton = convertView
					.findViewById(R.id.conference_request_host_button_accept);
			acceptButton.setTag(user);
			acceptButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onAcceptButtonClick(v);
				}
			});

			View rejectButton = convertView
					.findViewById(R.id.conference_request_host_button_reject);
			rejectButton.setTag(user);
			rejectButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onRejectButtonClick(v);
				}
			});

			return convertView;
		}

		private void onAcceptButtonClick(View v) {
			User user = (User) v.getTag();
			service.grantPermission(user, ConferencePermission.CONTROL,
					PermissionState.GRANTED, null);
			mList.remove(user);
			users.remove(user);
			mAdapter.notifyDataSetChanged();
			ConferenceMsgDialog.this.dismiss();
		}

		private void onRejectButtonClick(View v) {
			User user = (User) v.getTag();
			service.grantPermission(user, ConferencePermission.CONTROL,
					PermissionState.NORMAL, null);
			mList.remove(user);
			users.remove(user);
			mAdapter.notifyDataSetChanged();
			ConferenceMsgDialog.this.dismiss();
		}
	}

}
