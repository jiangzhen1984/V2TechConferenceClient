package com.v2tech.view.group;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

public class CrowdMembersActivity extends Activity {

	private Context mContext;

	private ListView mMembersContainer;
	private MembersAdapter adapter;
	private TextView mInvitationButton;
	private View mReturnButton;

	private List<User> mMembers;
	private CrowdGroup crowd;
	
	private boolean isInDeleteMode;
	private CrowdGroupService service;
	private LocalReceiver localReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.crowd_members_activity);
		mContext = this;
		mMembersContainer = (ListView) findViewById(R.id.crowd_members_list);
		mMembersContainer.setOnItemClickListener(itemListener);
		mMembersContainer.setOnItemLongClickListener(itemLongListener);
		

		mInvitationButton = (TextView) findViewById(R.id.crowd_members_invitation_button);
		mInvitationButton.setOnClickListener(mInvitationButtonListener);

		mReturnButton = findViewById(R.id.crowd_members_return_button);
		mReturnButton.setOnClickListener(mReturnListener);

		crowd = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.CHATING.intValue(), getIntent().getLongExtra("cid", 0));
		mMembers = crowd.getUsers();
		adapter = new MembersAdapter();
		mMembersContainer.setAdapter(adapter);
		overridePendingTransition(R.animator.left_in, R.animator.left_out);
		service = new CrowdGroupService();
		if (crowd.getOwnerUser().getmUserId() != GlobalHolder.getInstance().getCurrentUserId()) {
			mInvitationButton.setVisibility(View.INVISIBLE);
		}
		initReceiver();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(localReceiver);
		service.clearCalledBack();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		if (isInDeleteMode) {
			isInDeleteMode = false;
			adapter.notifyDataSetChanged();
			return;
		}
		super.onBackPressed();
	}
	
	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.animator.right_in, R.animator.right_out);
	}

	
	private void initReceiver() {
		localReceiver = new LocalReceiver(); 
		IntentFilter filter = new IntentFilter();
		filter.addAction(JNIService.JNI_BROADCAST_KICED_CROWD);
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		this.registerReceiver(localReceiver, filter);
	}

	
	
	
	private OnItemClickListener itemListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int pos,
				long id) {
			
		}

	};
	
	private OnItemLongClickListener itemLongListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int pos,
				long id) {
			if (crowd.getOwnerUser().getmUserId() != GlobalHolder.getInstance().getCurrentUserId()) {
				return false;
			}
			if (!isInDeleteMode) {
				isInDeleteMode = true;
				mInvitationButton.setText(R.string.crowd_members_deletion_mode_quit_button);
				adapter.notifyDataSetChanged();
				return true;
			}
			return false;
		}

	};
	

	private OnClickListener mInvitationButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (isInDeleteMode) {
				isInDeleteMode = false;
				mInvitationButton.setText(R.string.crowd_members_invitation);
				adapter.notifyDataSetChanged();
				
			} else {
				Intent i = new Intent(PublicIntent.START_GROUP_CREATE_ACTIVITY);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.putExtra("cid", crowd.getmGId());
				i.putExtra("mode", true);
				startActivity(i);
			}
		}

	};

	private OnClickListener mReturnListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			onBackPressed();
		}

	};
	
	
	class LocalReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(JNIService.JNI_BROADCAST_KICED_CROWD)) {
				long crowdId = intent.getLongExtra("crowd", 0);
				if (crowdId == crowd.getmGId()) {
					finish();
				}
			}
			
		}
		
	}
	

	class MemberView extends LinearLayout {

		private ImageView mDeleteIV;
		private ImageView mPhotoIV;
		private TextView  mNameTV;
		private TextView mDeleteButtonTV;
		private User mUser;

		public MemberView(Context context, User user) {
			super(context);
			this.mUser = user;
			this.setOrientation(LinearLayout.VERTICAL);
			LinearLayout line = new LinearLayout(context);
			line.setOrientation(LinearLayout.HORIZONTAL);
			
			
			LinearLayout.LayoutParams lineRL = new LinearLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			int margin = (int)context.getResources().getDimension(R.dimen.conversation_view_margin);
			lineRL.leftMargin = margin;
			lineRL.rightMargin = margin;
			lineRL.topMargin = margin;
			lineRL.bottomMargin = margin;
			lineRL.gravity = Gravity.CENTER_VERTICAL;
			
			//Add delete icon
			mDeleteIV = new ImageView(mContext);
			mDeleteIV.setImageResource(R.drawable.contacts_group_item_icon);
			mDeleteIV.setVisibility(View.GONE);
			mDeleteIV.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					mDeleteButtonTV.setVisibility(View.VISIBLE);
				}
				
			});
			line.addView(mDeleteIV, lineRL);
			
			
			
			mPhotoIV = new ImageView(context);
			if (user.getAvatarBitmap() != null) {
				mPhotoIV.setImageBitmap(user.getAvatarBitmap());
			} else {
				mPhotoIV.setImageResource(R.drawable.avatar);
			}
			line.addView(mPhotoIV, lineRL);
			
			
			mNameTV = new TextView(context);
			mNameTV.setText(user.getName());
			mNameTV.setTextColor(context.getResources().getColor(R.color.contacts_user_view_item_color_offline));
			
			line.addView(mNameTV, lineRL);
			
			
			
			//Add delete button
			mDeleteButtonTV = new TextView(mContext);
			mDeleteButtonTV.setText(R.string.crowd_members_delete);
			mDeleteButtonTV.setVisibility(View.GONE);
			mDeleteButtonTV.setTextColor(Color.WHITE);
			mDeleteButtonTV.setBackgroundResource(R.drawable.rounded_crowd_members_delete_button);
			mDeleteButtonTV.setPadding(10, 5, 5, 10);
			mDeleteButtonTV.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					v.setVisibility(View.GONE);
					service.removeMember(crowd, mUser, null);
					mMembers.remove(mUser);
					adapter.notifyDataSetChanged();
				}
				
			});
			line.addView(mDeleteButtonTV, new LinearLayout.LayoutParams(
					RelativeLayout.LayoutParams.MATCH_PARENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT));

			this.addView(line, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));

			LinearLayout lineBottom = new LinearLayout(context);
			lineBottom.setBackgroundColor(Color.rgb(194, 194, 194));
			this.addView(lineBottom, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, 2));
		}
		
		public void update(User user) {
			if (isInDeleteMode) {
				if (this.mUser.getmUserId() != crowd.getOwnerUser().getmUserId()) {
					mDeleteIV.setVisibility(View.VISIBLE);
				} else {
					mDeleteIV.setVisibility(View.GONE);
				}
			} else {
				mDeleteIV.setVisibility(View.GONE);
				mDeleteButtonTV.setVisibility(View.GONE);
			}
			if (this.mUser == user) {
				return;
			}
			this.mUser = user;
			
			mNameTV.setText(user.getName());
			if (user.getAvatarBitmap() != null) {
				mPhotoIV.setImageBitmap(user.getAvatarBitmap());
			} else {
				mPhotoIV.setImageResource(R.drawable.avatar);
			}
		}

	}

	class MembersAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mMembers.size();
		}

		@Override
		public Object getItem(int position) {
			return mMembers.get(position);
		}

		@Override
		public long getItemId(int position) {
			return mMembers.get(position).getmUserId();
		}
		
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = new MemberView(mContext, mMembers.get(position));
			} else {
				((MemberView)convertView).update(mMembers.get(position));
			}
			return convertView;
		}

	}

}
