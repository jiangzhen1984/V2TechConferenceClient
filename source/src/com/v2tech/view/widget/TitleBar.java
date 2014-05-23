package com.v2tech.view.widget;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.GlobalConfig;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.contacts.ContactDetail;
import com.v2tech.vo.NetworkStateCode;

public class TitleBar {

	public static int TITLE_BAR_TYPE_PLUS = 1;
	public static int TITLE_BAR_TYPE_MORE = 2;

	private Context context;

	private View networkNotificationView;
	private View rootContainer;
	private View plusButton;
	private View moreButton;
	private TitleBarMenuItemClickListener listener;
	private EditText searchEdit;

	private List<Wrapper> additionList;
	private List<Wrapper> normalList;

	private int[] imgs = new int[] { R.drawable.title_bar_item_detail_button,
			R.drawable.title_bar_item_setting_button,
			R.drawable.title_bar_item_help_button,
			R.drawable.title_bar_item_about_button };

	private int[] items = new int[] { R.string.title_bar_item_detail,
			R.string.title_bar_item_setting, R.string.title_bar_item_help,
			R.string.title_bar_item_about };

	public TitleBar(Context context, View rootContainer) {
		this.context = context;
		this.rootContainer = rootContainer;
		normalList = new ArrayList<Wrapper>();
		additionList = new ArrayList<Wrapper>();

		networkNotificationView = this.rootContainer
				.findViewById(R.id.title_bar_notification);
		networkNotificationView.setVisibility(View.GONE);
		plusButton = this.rootContainer
				.findViewById(R.id.title_bar_plus_button);
		plusButton.setOnClickListener(mPlusButtonListener);
		moreButton = this.rootContainer
				.findViewById(R.id.title_bar_feature_more_button);
		moreButton.setOnClickListener(mMoreButtonListener);
		searchEdit = (EditText) this.rootContainer
				.findViewById(R.id.search_edit);

	}

	public void updateTitle(String title) {
		TextView tv = (TextView) rootContainer
				.findViewById(R.id.fragment_title);
		tv.setText(title);
	}

	public void updateTitle(int resId) {
		TextView tv = (TextView) rootContainer
				.findViewById(R.id.fragment_title);
		tv.setText(resId);
	}

	public void addAdditionalPopupMenuItem(View v, Object obj) {
		additionList.add(new Wrapper(v, obj));
	}

	public void addNormalPopupMenuItem(View v, Object obj) {
		normalList.add(new Wrapper(v, obj));
	}

	public void setListener(TitleBarMenuItemClickListener listener) {
		this.listener = listener;
	}

	public void initPlusItemList(ViewGroup vg) {
		for (int i = 0; i < imgs.length; i++) {
			LinearLayout ll = new LinearLayout(context);
			ll.setOrientation(LinearLayout.HORIZONTAL);

			ImageView iv = new ImageView(context);
			iv.setImageResource(imgs[i]);
			iv.setPadding(10, 5, 5, 10);
			LinearLayout.LayoutParams ivLL = new LinearLayout.LayoutParams(0,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			ivLL.gravity = Gravity.RIGHT;
			ivLL.weight = 0.3F;

			ll.addView(iv, ivLL);

			TextView tv = new TextView(context);
			tv.setText(items[i]);
			tv.setPadding(10, 15, 5, 15);
			LinearLayout.LayoutParams tvLL = new LinearLayout.LayoutParams(0,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			tvLL.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
			tvLL.weight = 0.7F;

			ll.addView(tv, tvLL);
			ll.setOnClickListener(plusItemClickListener);

			ll.setId(imgs[i]);
			ll.setPadding(0, 10, 15, 10);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			lp.rightMargin = 20;
			vg.addView(ll, lp);

			if (i != imgs.length - 1) {
				LinearLayout line = new LinearLayout(context);
				line.setBackgroundResource(R.color.line_color);
				LinearLayout.LayoutParams lineLL = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT, 1);
				vg.addView(line, lineLL);
			}
		}
	}

	public void regsiterSearchedTextListener(TextWatcher tw) {
		this.searchEdit.addTextChangedListener(tw);
	}

	public void unRegsiterSearchedTextListener(TextWatcher tw) {
		this.searchEdit.removeTextChangedListener(tw);
	}
	
	
	public void updateConnectState(NetworkStateCode code) {
		if (code != NetworkStateCode.CONNECTED) {
			networkNotificationView.setVisibility(View.VISIBLE);
		} else {
			networkNotificationView.setVisibility(View.GONE);
		}
	}

	public void dismiss() {
		additionList.clear();
		normalList.clear();
	}

	public void dismissPlusWindow() {
		if (plusWindow != null && plusWindow.isShowing()) {
			plusWindow.dismiss();
		}
	}

	private OnClickListener plusItemClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent();
			intent.addCategory(PublicIntent.DEFAULT_CATEGORY);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			int id = v.getId();
			switch (id) {
			case R.drawable.title_bar_item_detail_button:
				intent.setClass(context, ContactDetail.class);
				intent.putExtra("uid", GlobalHolder.getInstance().getCurrentUserId());
				break;
			case R.drawable.title_bar_item_about_button:
				intent.setAction(PublicIntent.START_ABOUT_ACTIVITY);
				break;
			}
			context.startActivity(intent);
			moreWindow.dismiss();
		}

	};

	private PopupWindow plusWindow;
	private OnClickListener mPlusButtonListener = new OnClickListener() {

		@Override
		public void onClick(View anchor) {
			if (additionList.size() <= 0) {
				return;
			}
			DisplayMetrics dm = new DisplayMetrics();
			((Activity) context).getWindowManager().getDefaultDisplay()
					.getMetrics(dm);
			if (plusWindow == null) {
				LayoutInflater inflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View layout = inflater.inflate(
						R.layout.title_bar_pop_up_window, null);
				LinearLayout itemContainer = (LinearLayout) layout
						.findViewById(R.id.common_pop_window_container);

				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.WRAP_CONTENT,
						LinearLayout.LayoutParams.WRAP_CONTENT);
				lp.rightMargin = 20;
				for (int i = 0; i < additionList.size(); i++) {
					additionList.get(i).v.setPadding(0, 15, 15, 15);
					itemContainer.addView(additionList.get(i).v, lp);

					if (i != additionList.size() - 1) {
						LinearLayout line = new LinearLayout(context);
						line.setBackgroundResource(R.color.line_color);
						LinearLayout.LayoutParams lineLL = new LinearLayout.LayoutParams(
								LinearLayout.LayoutParams.MATCH_PARENT, 1);
						itemContainer.addView(line, lineLL);
					}
				}
				
				itemContainer.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

				int height = 300;
				if (height < itemContainer.getMeasuredHeight()) {
					height = itemContainer.getMeasuredHeight();
				}

				layout.findViewById(R.id.common_pop_up_arrow_up).measure(
						View.MeasureSpec.UNSPECIFIED,
						View.MeasureSpec.UNSPECIFIED);

				plusWindow = buildPopupWindow(layout,
						ViewGroup.LayoutParams.WRAP_CONTENT, height);
			}

			int[] pos = new int[2];
			anchor.getLocationInWindow(pos);
			pos[0] += anchor.getMeasuredWidth() / 2;
			pos[1] += anchor.getMeasuredHeight()- anchor.getPaddingBottom();
			// calculate arrow offset
			View arrow = plusWindow.getContentView().findViewById(
					R.id.common_pop_up_arrow_up);
			arrow.bringToFront();

			int x = pos[0];

			RelativeLayout.LayoutParams arrowRL = (RelativeLayout.LayoutParams) arrow
					.getLayoutParams();
			arrowRL.rightMargin = (dm.widthPixels - pos[0])
					- arrow.getMeasuredWidth() / 2;
			arrow.setLayoutParams(arrowRL);

			plusWindow.setAnimationStyle(R.style.TitleBarPopupWindowAnim);
			plusWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, x, pos[1]);

		}

	};

	private PopupWindow moreWindow;
	private OnClickListener mMoreButtonListener = new OnClickListener() {

		@Override
		public void onClick(View anchor) {
			DisplayMetrics dm = new DisplayMetrics();
			((Activity) context).getWindowManager().getDefaultDisplay()
					.getMetrics(dm);
			if (moreWindow == null) {
				LayoutInflater inflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View layout = inflater.inflate(
						R.layout.title_bar_pop_up_window, null);
				LinearLayout itemContainer = (LinearLayout) layout
						.findViewById(R.id.common_pop_window_container);

				int height = 300;
				if (GlobalConfig.GLOBAL_LAYOUT_SIZE == Configuration.SCREENLAYOUT_SIZE_NORMAL) {
					height = (int) (dm.heightPixels * 0.6);
				} else {
					height = (int) (dm.heightPixels * 0.4);
				}

				layout.findViewById(R.id.common_pop_up_arrow_up).measure(
						View.MeasureSpec.UNSPECIFIED,
						View.MeasureSpec.UNSPECIFIED);

				moreWindow = buildPopupWindow(layout,
						ViewGroup.LayoutParams.WRAP_CONTENT, height);
				initPlusItemList(itemContainer);
			}

			int[] pos = new int[2];
			anchor.getLocationInWindow(pos);
			pos[0] += anchor.getMeasuredWidth() / 2;
			pos[1] += anchor.getMeasuredHeight() - anchor.getPaddingBottom();
			// calculate arrow offset
			View arrow = moreWindow.getContentView().findViewById(
					R.id.common_pop_up_arrow_up);
			arrow.bringToFront();

			int x = pos[0];

			RelativeLayout.LayoutParams arrowRL = (RelativeLayout.LayoutParams) arrow
					.getLayoutParams();
			arrowRL.rightMargin = (dm.widthPixels - pos[0])
					- arrow.getMeasuredWidth() / 2;
			arrow.setLayoutParams(arrowRL);

			moreWindow.setAnimationStyle(R.style.TitleBarPopupWindowAnim);
			moreWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, x, pos[1]);

		}

	};

	private PopupWindow buildPopupWindow(View view, int width, int height) {

		final PopupWindow pw = new PopupWindow(view, width, height, true);
		pw.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss() {
				pw.dismiss();
			}

		});
		pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		pw.setFocusable(true);
		pw.setTouchable(true);
		pw.setOutsideTouchable(true);
		return pw;
	}

	class Wrapper {
		View v;
		Object o;

		public Wrapper(View v, Object o) {
			super();
			this.v = v;
			this.o = o;
		}

	}

	interface TitleBarMenuItemClickListener {
		public void onClick(View view, Object obj);
	}

}
