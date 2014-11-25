package com.v2tech.view.widget;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.DensityUtils;
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
	private int padding;

	private List<Wrapper> additionList;
	private List<Wrapper> normalList;

	private int[] imgs = new int[] { R.drawable.title_bar_item_detail_button,
			R.drawable.title_bar_item_setting_button,
			R.drawable.title_bar_item_help_button,
			R.drawable.title_bar_item_about_button };

	private int[] items = new int[] { R.string.title_bar_item_detail,
			R.string.title_bar_item_setting, R.string.title_bar_item_help,
			R.string.title_bar_item_about };
	
	private int[] plusImgs = new int[] { R.drawable.conversation_video_button,
			R.drawable.conversation_group_button,
			R.drawable.conversation_discussion_button,
			R.drawable.conversation_seach_crowd_button,
			R.drawable.conversation_seach_member_button,
			R.drawable.conversation_call_button,
			R.drawable.conversation_sms_button,
			R.drawable.conversation_email_button,
			R.drawable.conversation_files_button };

	private int[] plusItems = new int[] {
			R.string.conversation_popup_menu_video_call_button,
			R.string.conversation_popup_menu_group_create_button,
			R.string.conversation_popup_menu_discussion_board_create_button,
			R.string.conversation_popup_menu_crowd_search_button,
			R.string.conversation_popup_menu_member_search_button,
			R.string.conversation_popup_menu_call_button,
			R.string.conversation_popup_menu_sms_call_button,
			R.string.conversation_popup_menu_email_button,
			R.string.conversation_popup_menu_files_button };

	public TitleBar(Context context, View rootContainer) {
		this.context = context;
		this.rootContainer = rootContainer;
		normalList = new ArrayList<Wrapper>();
		additionList = new ArrayList<Wrapper>();

		networkNotificationView = this.rootContainer
				.findViewById(R.id.title_bar_notification);
		networkNotificationView.setVisibility(View.GONE);
		plusButton = this.rootContainer
				.findViewById(R.id.ws_common_mainActivity_title_plus);
		plusButton.setVisibility(View.VISIBLE);
		plusButton.setOnClickListener(mPlusButtonListener);

		this.rootContainer.findViewById(
				R.id.ws_common_activity_title_left_button).setVisibility(
				View.INVISIBLE);
		this.rootContainer.findViewById(
				R.id.ws_common_activity_title_right_button).setVisibility(
				View.GONE);

		moreButton = this.rootContainer
				.findViewById(R.id.ws_common_mainActivity_title_feature_more_button);
		moreButton.setVisibility(View.VISIBLE);
		moreButton.setOnClickListener(mMoreButtonListener);
		searchEdit = (EditText) this.rootContainer
				.findViewById(R.id.search_edit);
		
		// Initialise popupWindow padding
		Configuration conf = context.getResources().getConfiguration();
		int landscape = ((Activity)context).getRequestedOrientation();
		if (landscape == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
			padding = DensityUtils.dip2px(context, 2);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && conf.densityDpi < 240) {
			padding = DensityUtils.dip2px(context, 2);
		} else {
			padding = DensityUtils.dip2px(context, 5);
		}
		
	}

	public void updateTitle(String title) {
		TextView tv = (TextView) rootContainer
				.findViewById(R.id.ws_common_activity_title_content);
		tv.setText(title);
	}

	public void updateTitle(int resId) {
		TextView tv = (TextView) rootContainer
				.findViewById(R.id.ws_common_activity_title_content);
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

	public void initMoreitem(ViewGroup vg) {
		for (int i = 0; i < imgs.length; i++) {
			LinearLayout ll = new LinearLayout(context);
			ll.setOrientation(LinearLayout.HORIZONTAL);

			ImageView iv = new ImageView(context);
			iv.setImageResource(imgs[i]);
//			iv.setPadding(10, 5, 5, 10);
			iv.setPadding(10, padding, 5, padding);
			LinearLayout.LayoutParams ivLL = new LinearLayout.LayoutParams(0,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			ivLL.gravity = Gravity.RIGHT;
			ivLL.weight = 0.3F;

			ll.addView(iv, ivLL);

			TextView tv = new TextView(context);
			tv.setText(items[i]);
//			tv.setPadding(10, 15, 5, 15);
			tv.setPadding(10, padding, 5, padding);
			tv.setTextColor(Color.rgb(123, 123, 123));
			LinearLayout.LayoutParams tvLL = new LinearLayout.LayoutParams(0,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			tvLL.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
			tvLL.weight = 0.7F;

			ll.addView(tv, tvLL);
			ll.setOnClickListener(plusItemClickListener);

			ll.setId(imgs[i]);
			ll.setPadding(0, padding, 15, padding);
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
	
	public void initPlusItem() {
		for (int i = 0; i < plusImgs.length; i++) {
			LinearLayout ll = new LinearLayout(context);
			ll.setOrientation(LinearLayout.HORIZONTAL);

			ImageView iv = new ImageView(context);
			iv.setImageResource(plusImgs[i]);
			iv.setPadding(5, padding, 5, padding);
			LinearLayout.LayoutParams ivLL = new LinearLayout.LayoutParams(0,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			ivLL.gravity = Gravity.RIGHT;
			ivLL.weight = 0.3F;

			ll.addView(iv, ivLL);

			TextView tv = new TextView(context);
			tv.setText(plusItems[i]);
			tv.setPadding(5, padding, 5, padding);
			//TODO gray disable button
			if (i > 3) {
				tv.setTextColor(Color.rgb(198, 198, 198));
			} else {
				tv.setTextColor(Color.rgb(123, 123, 123));
			}
			LinearLayout.LayoutParams tvLL = new LinearLayout.LayoutParams(0,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			tvLL.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
			tvLL.weight = 0.7F;

			ll.addView(tv, tvLL);
			ll.setOnClickListener(titleBarMenuItemClickListener);

			ll.setId(plusImgs[i]);
			addAdditionalPopupMenuItem(ll, null);
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

	public void requestHidenSoftKeyboard() {
		InputMethodManager imm = (InputMethodManager) context
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(searchEdit.getWindowToken(),
				InputMethodManager.RESULT_UNCHANGED_SHOWN);
	}
	
	private OnClickListener titleBarMenuItemClickListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			dismissPlusWindow();
			if (!GlobalHolder.getInstance().isServerConnected()) {
				Toast.makeText(context, R.string.error_offline_of_no_network, Toast.LENGTH_SHORT).show();
				return;
			}
			int id = view.getId();
			switch (id) {
			case R.drawable.conversation_group_button: {

				Intent i = new Intent(PublicIntent.START_GROUP_CREATE_ACTIVITY);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				context.startActivity(i);
				break;
			}

			case R.drawable.conversation_video_button: {
				Intent i = new Intent(
						PublicIntent.START_CONFERENCE_CREATE_ACTIVITY);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				context.startActivity(i);
			}
				break;
			case R.drawable.conversation_seach_crowd_button: {
				Intent i = new Intent(
						PublicIntent.START_SEARCH_ACTIVITY);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				//For crowd search
				i.putExtra("type", 0);
				context.startActivity(i);
			}
				break;
			case R.drawable.conversation_discussion_button : {
				Intent i = new Intent(
						PublicIntent.START_DISCUSSION_BOARD_CREATE_ACTIVITY);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				context.startActivity(i);
			}
			break;
				
			case R.drawable.conversation_seach_member_button:
			{
				Intent i = new Intent(
						PublicIntent.START_SEARCH_ACTIVITY);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				//For member search
				i.putExtra("type", 1);
				context.startActivity(i);
			}
				break;
			case R.drawable.conversation_call_button:
				break;
			case R.drawable.conversation_sms_button:
				break;
			case R.drawable.conversation_email_button:
				break;
			case R.drawable.conversation_files_button:
				break;
			}
		}

	};

	private OnClickListener plusItemClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent();
			intent.addCategory(PublicIntent.DEFAULT_CATEGORY);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			int id = v.getId();
			switch (id) {
			case R.drawable.title_bar_item_setting_button:
				intent.setAction(PublicIntent.START_SETTING_ACTIVITY);
				break;
			case R.drawable.title_bar_item_detail_button:
				intent.setClass(context, ContactDetail.class);
				intent.putExtra("uid", GlobalHolder.getInstance()
						.getCurrentUserId());
				break;
			case R.drawable.title_bar_item_about_button:
				intent.setAction(PublicIntent.START_ABOUT_ACTIVITY);
				break;
			default:
				return;
			}
			context.startActivity(intent);
			moreWindow.dismiss();
		}

	};

	private PopupWindow plusWindow;
	private OnClickListener mPlusButtonListener = new OnClickListener() {

		@Override
		public void onClick(View anchor) {
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
				
				initPlusItem();
				if (additionList.size() <= 0) {
					return;
				}
				
				for (int i = 0; i < additionList.size(); i++) {
					additionList.get(i).v.setPadding(0, padding, 15, padding);
					itemContainer.addView(additionList.get(i).v, lp);

					if (i != additionList.size() - 1) {
						LinearLayout line = new LinearLayout(context);
						line.setBackgroundResource(R.color.line_color);
						LinearLayout.LayoutParams lineLL = new LinearLayout.LayoutParams(
								LinearLayout.LayoutParams.MATCH_PARENT, 1);
						itemContainer.addView(line, lineLL);
					}
				}

				int height = 300;

				plusWindow = buildPopupWindow(layout,
						ViewGroup.LayoutParams.WRAP_CONTENT, height);

				itemContainer.measure(View.MeasureSpec.UNSPECIFIED,
						View.MeasureSpec.UNSPECIFIED);
				View arrow = layout.findViewById(R.id.common_pop_up_arrow_up);
				arrow.measure(View.MeasureSpec.UNSPECIFIED,
						View.MeasureSpec.UNSPECIFIED);

				if (height < itemContainer.getMeasuredHeight()
						+ arrow.getMeasuredHeight()) {
					height = itemContainer.getMeasuredHeight()
							+ arrow.getMeasuredHeight();
				}
				plusWindow.setHeight(height);
			}

			int[] pos = new int[2];
			anchor.getLocationInWindow(pos);
			pos[0] += anchor.getMeasuredWidth() / 2;
			pos[1] += anchor.getMeasuredHeight() - anchor.getPaddingBottom();
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

				initMoreitem(itemContainer);

				itemContainer.measure(View.MeasureSpec.UNSPECIFIED,
						View.MeasureSpec.UNSPECIFIED);
				View arrow = layout.findViewById(R.id.common_pop_up_arrow_up);
				arrow.measure(View.MeasureSpec.UNSPECIFIED,
						View.MeasureSpec.UNSPECIFIED);

				int height = itemContainer.getMeasuredHeight()
						+ arrow.getMeasuredHeight();

				moreWindow = buildPopupWindow(layout,
						ViewGroup.LayoutParams.WRAP_CONTENT, height);

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
