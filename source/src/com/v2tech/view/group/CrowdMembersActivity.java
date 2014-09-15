package com.v2tech.view.group;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
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
import com.v2tech.service.GlobalHolder;
import com.v2tech.view.PublicIntent;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.User;
import com.v2tech.vo.Group.GroupType;

public class CrowdMembersActivity extends Activity {

	private Context mContext;

	private ListView mMembersContainer;
	private MembersAdapter adapter;
	private View mInvitationButton;
	private View mReturnButton;

	private List<User> mMembers;
	private CrowdGroup crowd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.crowd_members_activity);
		mContext = this;
		mMembersContainer = (ListView) findViewById(R.id.crowd_members_list);
		mMembersContainer.setOnItemClickListener(itemListener);
		

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
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
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
		super.onBackPressed();
	}
	
	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.animator.right_in, R.animator.right_out);
	}

	private OnItemClickListener itemListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int pos,
				long id) {

		}

	};

	private OnClickListener mInvitationButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			Intent i = new Intent(PublicIntent.START_GROUP_CREATE_ACTIVITY);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			i.putExtra("cid", crowd.getmGId());
			startActivity(i);
		}

	};

	private OnClickListener mReturnListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			onBackPressed();
		}

	};

	class MemberView extends LinearLayout {

		private ImageView mPhotoIV;
		private TextView  mNameTV;
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

			this.addView(line, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));

			LinearLayout lineBottom = new LinearLayout(context);
			lineBottom.setBackgroundColor(Color.rgb(194, 194, 194));
			this.addView(lineBottom, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, 2));
		}
		
		public void update(User user) {
			if ( this.mUser == user) {
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
			return 10;
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
