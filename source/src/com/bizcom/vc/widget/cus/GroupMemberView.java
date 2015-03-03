package com.bizcom.vc.widget.cus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Color;
import android.text.TextUtils.TruncateAt;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vo.User;
import com.v2tech.R;

public class GroupMemberView extends LinearLayout {

	private Context mContext;
	private ImageView mDeleteIV;
	private ImageView mPhotoIV;
	private TextView mNameTV;
	private TextView mDeleteButtonTV;
	private RelativeLayout mContentLayout;
	private User mUser;

	public GroupMemberView(Context context, User user,
			final ClickListener callBack) {
		super(context);
		mContext = context;
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

		Options opts = new Options();
		opts.outWidth = (int) getResources().getDimension(
				R.dimen.common_delete_icon_width);
		opts.outHeight = (int) getResources().getDimension(
				R.dimen.common_delete_icon_height);
		Bitmap bit = BitmapFactory.decodeResource(getResources(),
				R.drawable.ic_delete, opts);
		mDeleteIV.setImageBitmap(bit);
		mDeleteIV.setVisibility(View.GONE);

		int padding = (int) getResources().getDimension(
				R.dimen.common_delete_icon_padding);
		mDeleteIV.setPadding(padding, padding, padding, padding);
		mDeleteIV.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mUser.isShowDelete) {
					mUser.isShowDelete = false;
					mDeleteButtonTV.setVisibility(View.GONE);
				} else {
					mUser.isShowDelete = true;
					mDeleteButtonTV.setVisibility(View.VISIBLE);
				}
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
				R.color.common_black));
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
		mDeleteButtonTV.setVisibility(View.GONE);
		mDeleteButtonTV.setTextColor(Color.WHITE);
		mDeleteButtonTV.setId(2);
		mDeleteButtonTV
				.setBackgroundResource(R.drawable.rounded_crowd_members_delete_button);
		mDeleteButtonTV.setGravity(Gravity.CENTER_VERTICAL);
		mDeleteButtonTV.setPadding(20, 10, 20, 10);
		mDeleteButtonTV.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!GlobalHolder.getInstance().isServerConnected()) {
					Toast.makeText(mContext,
							R.string.error_discussion_no_network,
							Toast.LENGTH_SHORT).show();
					return;
				}
				v.setVisibility(View.GONE);
				callBack.removeMember(mUser);
			}

		});

		RelativeLayout.LayoutParams mDeleteButtonTVLP = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		mDeleteButtonTVLP.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		mDeleteButtonTVLP.addRule(RelativeLayout.CENTER_VERTICAL);
		mDeleteButtonTVLP.rightMargin = margin;
		mDeleteButtonTVLP.leftMargin = margin;

		mContentLayout.addView(mDeleteButtonTV, mDeleteButtonTVLP);

		this.addView(root, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		LinearLayout lineBottom = new LinearLayout(context);
		lineBottom.setBackgroundColor(mContext.getResources().getColor(R.color.common_line_color));
		this.addView(lineBottom, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 1));
	}

	public void update(boolean isInDeleteMode, User user, User groupOwnerUser) {
		if (isInDeleteMode) {
			if (user.getmUserId() != groupOwnerUser.getmUserId()) {
				mDeleteIV.setVisibility(View.VISIBLE);
			} else {
				mDeleteIV.setVisibility(View.GONE);
			}

			if (user.isShowDelete) {
				mDeleteButtonTV.setVisibility(View.VISIBLE);
			} else {
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

	public interface ClickListener {

		public void removeMember(User user);
	}
}
