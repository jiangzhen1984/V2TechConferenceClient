package com.v2tech.view.group;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils.TruncateAt;
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
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.view.PublicIntent;
import com.v2tech.vo.DiscussionGroup;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

/**
 * 
 * <ul>
 * Intent key:
 *      cid  : discussion board id
 * </ul>
 * @see PublicIntent#SHOW_DISCUSSION_BOARD_MEMBERS_ACTIVITY
 * @author 28851274
 *
 */
public class DiscussionBoardMembersActivity extends Activity {

	private Context mContext;

	private ListView mMembersContainer;
	private MembersAdapter adapter;
	private TextView mInvitationButton;
	private View mReturnButton;

	private List<User> mMembers;
	private DiscussionGroup crowd;

	private boolean isInDeleteMode;
	private CrowdGroupService service;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.discussion_board_members_activity);
		mContext = this;
		mMembersContainer = (ListView) findViewById(R.id.discussion_board_members_list);
		mMembersContainer.setOnItemClickListener(itemListener);
		mMembersContainer.setOnItemLongClickListener(itemLongListener);

		mInvitationButton = (TextView) findViewById(R.id.discussion_board_members_invitation_button);
		mInvitationButton.setOnClickListener(mInvitationButtonListener);

		mReturnButton = findViewById(R.id.discussion_board_members_return_button);
		mReturnButton.setOnClickListener(mReturnListener);

		crowd = (DiscussionGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.DISCUSSION.intValue(),
				getIntent().getLongExtra("cid", 0));
		mMembers = crowd.getUsers();
		adapter = new MembersAdapter();
		mMembersContainer.setAdapter(adapter);
		overridePendingTransition(R.animator.left_in, R.animator.left_out);
		service = new CrowdGroupService();
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != Activity.RESULT_CANCELED) {
			mMembers.clear();
			mMembers = crowd.getUsers();
			adapter.notifyDataSetChanged();
		}
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

	private OnItemLongClickListener itemLongListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int pos, long id) {
			if (crowd.getOwnerUser().getmUserId() != GlobalHolder.getInstance()
					.getCurrentUserId()) {
				return false;
			}
			if (!isInDeleteMode) {
				isInDeleteMode = true;
				mInvitationButton
						.setText(R.string.discussion_board_members_deletion_mode_quit_button);
				adapter.notifyDataSetChanged();
				return true;
			}
			return false;
		}

	};

	private OnClickListener mInvitationButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (!GlobalHolder.getInstance().isServerConnected()) {
				Toast.makeText(DiscussionBoardMembersActivity.this,
						R.string.error_discussion_no_network,
						Toast.LENGTH_SHORT).show();
				return;
			}
			
			if (isInDeleteMode) {
				isInDeleteMode = false;
				mInvitationButton.setText(R.string.discussion_board_members_invitation_button_text);
				adapter.notifyDataSetChanged();

			} else {
				// start CrowdCreateActivity 
				Intent i = new Intent(PublicIntent.START_DISCUSSION_BOARD_CREATE_ACTIVITY);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.putExtra("cid", crowd.getmGId());
				i.putExtra("mode", true);
				startActivityForResult(i, 100);
			}
		}

	};

	private OnClickListener mReturnListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			onBackPressed();
		}

	};


	class MemberView extends LinearLayout {

		private ImageView mDeleteIV;
		private ImageView mPhotoIV;
		private TextView mNameTV;
		private TextView mDeleteButtonTV;
		private RelativeLayout mContentLayout;
		private User mUser;

		public MemberView(Context context, User user) {
			super(context);
			this.mUser = user;
			this.setOrientation(LinearLayout.VERTICAL);
			LinearLayout root = new LinearLayout(context);
			root.setOrientation(LinearLayout.HORIZONTAL);

			LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			int margin = (int) context.getResources().getDimension(
					R.dimen.conversation_view_margin);
			rootParams.leftMargin = margin;
			rootParams.rightMargin = margin;
			rootParams.topMargin = margin;
			rootParams.bottomMargin = margin;
			rootParams.gravity = Gravity.CENTER_VERTICAL;

			// Add delete icon
			mDeleteIV = new ImageView(mContext);
			mDeleteIV.setImageResource(R.drawable.contacts_group_item_icon);
			if (isInDeleteMode) {
				if (this.mUser.getmUserId() != crowd.getOwnerUser()
						.getmUserId()) {
					mDeleteIV.setVisibility(View.VISIBLE);
				} else {
					mDeleteIV.setVisibility(View.GONE);
				}
			}
			else
				mDeleteIV.setVisibility(View.GONE);
			mDeleteIV.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					mUser.isDelete = true;
					mDeleteButtonTV.setVisibility(View.VISIBLE);
				}

			});
			root.addView(mDeleteIV, rootParams);

			mPhotoIV = new ImageView(context);
			if (user.getAvatarBitmap() != null) {
				mPhotoIV.setImageBitmap(user.getAvatarBitmap());
			} else {
				mPhotoIV.setImageResource(R.drawable.avatar);
			}
			root.addView(mPhotoIV, rootParams);

			mContentLayout = new RelativeLayout(context);
			root.addView(mContentLayout, rootParams);
			
			mNameTV = new TextView(context);
			mNameTV.setText(user.getName());
			mNameTV.setGravity(Gravity.CENTER_VERTICAL);
			mNameTV.setTextColor(context.getResources().getColor(
					R.color.contacts_user_view_item_color_offline));
			mNameTV.setEllipsize(TruncateAt.END);
			RelativeLayout.LayoutParams mNameParams = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			mNameParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			mNameParams.addRule(RelativeLayout.LEFT_OF, 2);
			mNameParams.addRule(RelativeLayout.CENTER_VERTICAL);
			mContentLayout.addView(mNameTV, mNameParams);

			// Add delete button
			mDeleteButtonTV = new TextView(mContext);
			mDeleteButtonTV.setText(R.string.crowd_members_delete);
			mDeleteButtonTV.setVisibility(View.INVISIBLE);
			mDeleteButtonTV.setTextColor(Color.WHITE);
			mDeleteButtonTV.setId(2);
			mDeleteButtonTV
					.setBackgroundResource(R.drawable.rounded_crowd_members_delete_button);
			mDeleteButtonTV.setPadding(20, 10, 20, 10);
			mDeleteButtonTV.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (!GlobalHolder.getInstance().isServerConnected()) {
						Toast.makeText(DiscussionBoardMembersActivity.this,
								R.string.error_discussion_no_network,
								Toast.LENGTH_SHORT).show();
						return;
					}
					v.setVisibility(View.GONE);
					service.removeMember(crowd, mUser, null);
					mMembers.remove(mUser);
					adapter.notifyDataSetChanged();
				}

			});

			RelativeLayout.LayoutParams mDeleteButtonTVLP = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			mDeleteButtonTVLP.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			mDeleteButtonTVLP.rightMargin = margin;
			mDeleteButtonTVLP.leftMargin = margin;
			
			mContentLayout.addView(mDeleteButtonTV, mDeleteButtonTVLP);

			this.addView(root, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));

			LinearLayout lineBottom = new LinearLayout(context);
			lineBottom.setBackgroundColor(Color.rgb(194, 194, 194));
			this.addView(lineBottom, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, 2));
		}

		public void update(User user) {
			if (isInDeleteMode) {
//				if (this.mUser.getmUserId() != crowd.getOwnerUser()
//						.getmUserId()) {
				if (user.getmUserId() != crowd.getOwnerUser()
						.getmUserId()) {
					mDeleteIV.setVisibility(View.VISIBLE);
				} else {
					mDeleteIV.setVisibility(View.GONE);
				}
				
				if(user.isDelete){
					mDeleteButtonTV.setVisibility(View.VISIBLE);
				}
				else{
					mDeleteButtonTV.setVisibility(View.GONE);
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
				((MemberView) convertView).update(mMembers.get(position));
			}
			return convertView;
		}

	}

}
