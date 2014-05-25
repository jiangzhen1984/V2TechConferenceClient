package com.v2tech.view.contacts;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.view.PublicIntent;
import com.v2tech.vo.User;

public class ContactUserView extends LinearLayout {

	private static final int ACTION_TYPE_INVITE_VIDEO = 1;
	private static final int ACTION_TYPE_START_CONVERSATION = 2;
	private static final int ACTION_TYPE_CALL = 3;
	private static final int ACTION_TYPE_VIEW_DETAIL = 4;

	private User mUser;

	private ImageView mPhotoIV;
	private TextView mUserNameTV;
	private TextView mUserSignatureTV;
	private ImageView mButtonIV;
	private ImageView mStatusIV;

	private Dialog mUserActionDialog;

	private RelativeLayout contentContainer;

	public ContactUserView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public ContactUserView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ContactUserView(Context context) {
		super(context);
	}

	public ContactUserView(Context context, User u) {
		super(context);
		initData(u);
	}

	public void initData(User u) {
		if (u == null || u.getmUserId() <= 0) {
			throw new RuntimeException("Invalid user data");
		}
		this.mUser = u;

		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.contacts_user_view, null, false);

		contentContainer = (RelativeLayout) view
				.findViewById(R.id.contact_user_view_root);

		mPhotoIV = (ImageView) view.findViewById(R.id.contact_user_img);
		if (mPhotoIV != null && u.getAvatarBitmap() != null) {
			mPhotoIV.setImageBitmap(u.getAvatarBitmap());
		}
		
		mUserNameTV = (TextView) view.findViewById(R.id.contact_user_name);
		mUserSignatureTV = (TextView) view
				.findViewById(R.id.contact_user_signature);
		mButtonIV = (ImageView) view.findViewById(R.id.contact_show_user_menu);

		mUserNameTV.setText(mUser.getName());
		mUserSignatureTV.setText(mUser.getSignature() == null ? "" : mUser
				.getSignature());

		mStatusIV = (ImageView) view.findViewById(R.id.contact_user_status_iv);
		updateStatus(u.getDeviceType(), u.getmStatus());

		if (this.mUser.getmUserId() != GlobalHolder.getInstance()
				.getCurrentUserId()) {
			mButtonIV.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					getActionDialog().show();
				}

			});
		} else {
			mButtonIV.setVisibility(View.GONE);
		}

		this.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				handle(4);
			}

		});
		
		this.addView(view, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

	}

	private Dialog getActionDialog() {
		if (mUserActionDialog != null) {
			return mUserActionDialog;
		}
		mUserActionDialog = new Dialog(getContext(),
				R.style.ContactUserActionDialog);
		mUserActionDialog
				.setContentView(R.layout.contacts_user_view_action_dialog);

		TextView tv = (TextView) mUserActionDialog
				.findViewById(R.id.contacts_user_action_invite_chat);
		tv.setOnClickListener(listener);
		tv = (TextView) mUserActionDialog
				.findViewById(R.id.contacts_user_action_invite_video);
		tv.setOnClickListener(listener);
		tv = (TextView) mUserActionDialog
				.findViewById(R.id.contacts_user_action_call);
		tv.setOnClickListener(listener);
		tv = (TextView) mUserActionDialog
				.findViewById(R.id.contacts_user_action_view_detail);
		tv.setOnClickListener(listener);

		return mUserActionDialog;
	}

	private OnClickListener listener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			int type = 0;
			if (v.getId() == R.id.contacts_user_action_invite_chat) {
				type = ACTION_TYPE_START_CONVERSATION;
			} else if (v.getId() == R.id.contacts_user_action_invite_video) {
				type = ACTION_TYPE_INVITE_VIDEO;
			} else if (v.getId() == R.id.contacts_user_action_call) {
				type = ACTION_TYPE_CALL;
			} else if (v.getId() == R.id.contacts_user_action_view_detail) {
				type = ACTION_TYPE_VIEW_DETAIL;
			}
			mUserActionDialog.dismiss();
			handle(type);
		}

	};

	private void handle(int type) {
		Intent i = new Intent();
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		switch (type) {
		case ACTION_TYPE_INVITE_VIDEO:
			i.setAction(PublicIntent.START_VIDEO_CONVERSACTION_ACTIVITY);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			i.putExtra("is_coming_call", false);
			i.putExtra("name", this.mUser.getName());
			i.putExtra("uid", this.mUser.getmUserId());
			break;
		case ACTION_TYPE_START_CONVERSATION:
			i.setAction(PublicIntent.START_CONVERSACTION_ACTIVITY);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			i.putExtra("user1id", GlobalHolder.getInstance().getCurrentUserId());
			i.putExtra("user2id", this.mUser.getmUserId());
			i.putExtra("user2Name", this.mUser.getName());
			break;
		case ACTION_TYPE_VIEW_DETAIL:
			i.setClass(this.getContext(), ContactDetail.class);
			i.putExtra("uid", this.mUser.getmUserId());
			break;
		}
		this.getContext().startActivity(i);
		((Activity) this.getContext()).overridePendingTransition(
				R.animator.nonam_scale_center_0_100,
				R.animator.nonam_scale_null);
	}

	public User getUser() {
		return this.mUser;
	}

	public void updateStatus(User.DeviceType dType, User.Status st) {
		// If user logged device is cell phone, only show cell phone icon
		if (dType == User.DeviceType.CELL_PHONE) {
			mStatusIV.setImageResource(R.drawable.cell_phone_user);
		} else {
			switch (st) {
			case ONLINE:
				mStatusIV.setImageResource(R.drawable.online);
				break;
			case LEAVE:
				mStatusIV.setImageResource(R.drawable.leave);
				break;
			case BUSY:
				mStatusIV.setImageResource(R.drawable.busy);
				break;
			case DO_NOT_DISTURB:
				mStatusIV.setImageResource(R.drawable.do_not_distrub);
				break;
			default:
				break;
			}
		}
		if (st == User.Status.OFFLINE || st == User.Status.HIDDEN) {
			mStatusIV.setVisibility(View.GONE);
			mPhotoIV.setColorFilter(Color.GRAY,PorterDuff.Mode.LIGHTEN);
		} else {
			mStatusIV.setVisibility(View.VISIBLE);
			mPhotoIV.clearColorFilter();
		}
	}

	public void updateAvatar(Bitmap bt) {
		if (bt == null) {
			mPhotoIV.setImageResource(R.drawable.avatar);
		} else {
			mPhotoIV.setImageBitmap(bt);
		}
	}

	public void updateSign() {
		mUserSignatureTV.setText(this.mUser.getSignature());
		mUserNameTV.setText(mUser.getName());
	}

	public void removePadding() {
		contentContainer.setPadding(0, contentContainer.getPaddingTop(),
				contentContainer.getPaddingRight(),
				contentContainer.getPaddingRight());
	}

	public void setPaddingT(int left, int top, int right, int bottom) {
		contentContainer.setPadding(left, top, right, bottom);
	}

}
