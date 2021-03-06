package com.bizcom.util;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bizcom.vc.widget.cus.WaitDialog;
import com.v2tech.R;

public class WaitDialogBuilder {
	public static final int TIME_OUT = 10;
	private static RotateAnimation animation;
	public static WaitDialog dialog;

	public static WaitDialog showNormalProgress(Context mContext, boolean show) {
		LinearLayout linearLayout = null;
		if (show) {
			linearLayout = new LinearLayout(mContext);
			linearLayout.setBackgroundColor(Color.TRANSPARENT);
			linearLayout.setOrientation(LinearLayout.VERTICAL);

			ImageView iv = new ImageView(mContext);
			iv.setImageResource(R.drawable.spin_black_70);
			iv.setPadding(60, 60, 60, 60);
			linearLayout.addView(iv, new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
			linearLayout.setTag(iv);
		} else {
			linearLayout = null;
		}
		return showProgress(mContext, show, linearLayout);
	}

	public static WaitDialog showNormalWithHintProgress(Context mContext,
			boolean show) {
		return showNormalWithHintProgress(mContext, mContext.getResources()
				.getString(R.string.util_progress_waiting), show);
	}

	public static WaitDialog showNormalWithHintProgress(Context mContext,
			String hint, boolean show) {
		LinearLayout ll = null;
		if (show) {
			ll = new LinearLayout(mContext);
			ll.setBackgroundColor(Color.TRANSPARENT);
			ll.setOrientation(LinearLayout.VERTICAL);

			ImageView iv = new ImageView(mContext);
			iv.setImageResource(R.drawable.spin_black_70);
			iv.setPadding(20, 20, 20, 20);
			ll.addView(iv, new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
			ll.setTag(iv);

			TextView tv = new TextView(mContext);
			tv.setText(hint);
			ll.addView(tv, new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));

			ll.setGravity(Gravity.CENTER);
		} else
			ll = null;
		return showProgress(mContext, show, ll);
	}

	private static WaitDialog showProgress(Context mContext, boolean show,
			ViewGroup layout) {

		if (show == false) {
			dismissDialog();
			return null;
		}

		if (animation == null) {
			animation = new RotateAnimation(0f, 359f,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			animation.setDuration(500);
			animation.setRepeatCount(RotateAnimation.INFINITE);
			animation.setRepeatMode(RotateAnimation.RESTART);
			LinearInterpolator lin = new LinearInterpolator();
			animation.setInterpolator(lin);
		}

		dialog = new WaitDialog(mContext, R.style.IpSettingDialog, animation);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		dialog.setContentView(layout);

		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);

		if (!dialog.isShowing()) {
			((View) layout.getTag()).startAnimation(animation);
			dialog.show();
		}
		return dialog;
	}

	private static void dismissDialog() {
		animation.cancel();
		if (dialog != null) {
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
			dialog.cannelTimeOut();
			dialog = null;
			return;
		}
	}
}
