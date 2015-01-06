package com.v2tech.view.conference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.SearchUtils;
import com.v2tech.vo.Attendee;
import com.v2tech.vo.Conference;
import com.v2tech.vo.User;
import com.v2tech.vo.User.DeviceType;
import com.v2tech.vo.UserDeviceConfig;

public class VideoAttendeeListLayout extends LinearLayout {

	private static final String TAG = "VideoAttendeeListLayout";

	private static final int FIRST_DEVICE_FLAG = -1;

	private Conference conf;

	private View rootView;

	private VideoAttendeeActionListener listener;

	private ListView mAttendeeContainer;

	private List<Wrapper> mList;
	private List<Wrapper> mFilterList;
	private List<Wrapper> mSearchFilterList;

	private boolean mIsStartedSearch;
	private EditText mSearchET;

	private View mPinButton;
	private TextView attendPersons;

	private AttendeesAdapter adapter = new AttendeesAdapter();

	/**
	 * Online attendee count without mixed device
	 */
	private int onLinePersons = 0;

	/**
	 * All attendee count without mixed device
	 */
	private int mAttendeeCount = 0;

	public interface VideoAttendeeActionListener {

		public void OnAttendeeDragged(Attendee at, UserDeviceConfig udc, int x,
				int y);

		public void OnAttendeeClicked(Attendee at, UserDeviceConfig udc);

		public void requestAttendeeViewFixedLayout(View v);

		public void requestAttendeeViewFloatLayout(View v);

	};

	public VideoAttendeeListLayout(Conference conf, Context context) {
		super(context);
		this.conf = conf;
		mList = new ArrayList<Wrapper>();
		mFilterList = mList;
		initLayout();
	}

	private void initLayout() {
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.video_attendee_list_layout, null, false);

		attendPersons = (TextView) view
				.findViewById(R.id.video_attendee_pin_persons);
		mAttendeeContainer = (ListView) view
				.findViewById(R.id.video_attendee_container);
		mSearchET = (EditText) view.findViewById(R.id.attendee_search);

		mPinButton = view.findViewById(R.id.video_attendee_pin_button);
		mPinButton.setOnClickListener(mRequestFixedListener);

		mAttendeeContainer.setAdapter(adapter);
		mAttendeeContainer.setTextFilterEnabled(true);
		mAttendeeContainer.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> ad, View view, int pos,
					long id) {
				if (listener != null) {
					Wrapper wr = (Wrapper) view.getTag();
					if (!wr.a.isJoined()) {
						return;
					}

					if (wr.udc == null || !wr.udc.isEnable()) {
						V2Log.i("User device config is disabled ");
						Toast.makeText(getContext(),
								R.string.error_open_device_disable,
								Toast.LENGTH_SHORT).show();
						return;
					}

					listener.OnAttendeeClicked(wr.a, wr.udc);

					if (!wr.udc.isShowing()) {
						view.setBackgroundColor(Color.WHITE);
					} else {
						view.setBackgroundColor(getContext().getResources()
								.getColor(R.color.attendee_select_bg));
					}

				}
			}

		});

		mAttendeeContainer.setFocusable(true);
		mAttendeeContainer.setFocusableInTouchMode(true);

		this.addView(view, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT));

		mSearchET.addTextChangedListener(mSearchListener);
		rootView = this;
	}

	public void setListener(VideoAttendeeActionListener listener) {
		this.listener = listener;
	}

	/**
	 * Add new attendee to list
	 * 
	 * @param at
	 */
	public void addNewAttendee(Attendee at) {
		if (at == null) {
			return;
		}
		List<Attendee> list = new ArrayList<Attendee>(1);
		list.add(at);
		addAttendeeWithoutNotification(list);
		Collections.sort(mList);
		adapter.notifyDataSetChanged();
	}

	/**
	 * Add new attendee list to current list
	 * 
	 * @param atList
	 */
	public void addNewAttendee(List<Attendee> atList) {
		if (atList == null) {
			return;
		}
		addAttendeeWithoutNotification(atList);
		Collections.sort(mList);
		adapter.notifyDataSetChanged();
	}

	private void addAttendeeWithoutNotification(List<Attendee> atList) {
		for (int index = 0; index < atList.size(); index++) {
			Attendee at = atList.get(index);
			if (at.getType() != Attendee.TYPE_MIXED_VIDEO
					&& !TextUtils.isEmpty(at.getAttName())) {
				configAttendee(at);
			}
			List<UserDeviceConfig> dList = at.getmDevices();
			int i = 0;
			int deviceIndex = 1;
			do {
				if (dList == null) {
					mList.add(new Wrapper(at, null, FIRST_DEVICE_FLAG));
				} else {
					mList.add(new Wrapper(at, dList.get(i),
							i == 0 ? FIRST_DEVICE_FLAG : deviceIndex++));
				}
				i++;
			} while (dList != null && i < dList.size());
		}
	}

	private View buildAttendeeView(Wrapper wr) {

		Context ctx = this.getContext();
		View view = LayoutInflater.from(ctx).inflate(
				R.layout.video_attendee_device_layout, null, false);

		ImageView cameraIV = (ImageView) view
				.findViewById(R.id.video_attendee_device_camera_icon);

		cameraIV.setOnTouchListener(mListViewOnTouchListener);

		updateView(wr, view);
		return view;

	}

	private void updateView(Wrapper wr, View view) {
		if (wr == null || view == null) {
			return;
		}
		view.setTag(wr);

		TextView nameTV = (TextView) view
				.findViewById(R.id.video_attendee_device_name);
		ImageView lectureStateIV = (ImageView) view
				.findViewById(R.id.video_attendee_device_lectrue_state_icon);
		ImageView spIV = (ImageView) view
				.findViewById(R.id.video_attendee_device_speaker_icon);
		ImageView cameraIV = (ImageView) view
				.findViewById(R.id.video_attendee_device_camera_icon);

		if (wr.sortFlag != FIRST_DEVICE_FLAG) {
			nameTV.setText("     "
					+ getContext().getText(
							R.string.confs_user_video_device_item)
					+ wr.sortFlag);
		} else {
			if (wr.a.isRapidInitiation()) {
				// nameTV.setText(new StringBuffer().append("<")
				// .append(wr.a.getAttName()).append(">"));

				nameTV.setText("<" + wr.a.getAttName() + ">");

			} else {
				nameTV.setText(wr.a.getAttName());
			}
		}

		if (wr.udc != null) {

			Log.i("20141220 2",
					"updateView() wr.udc.id =" + wr.udc.getDeviceID()
							+ " wr.udc.isShowing =" + wr.udc.isShowing());

			if (!wr.udc.isShowing()) {
				view.setBackgroundColor(Color.WHITE);
			} else {
				view.setBackgroundColor(getContext().getResources().getColor(
						R.color.attendee_select_bg));
			}
		} else {
			view.setBackgroundColor(Color.WHITE);
		}

		// Set text color and camera icon
		setStyle(wr, nameTV, lectureStateIV, spIV, cameraIV);
	}

	/**
	 * 
	 * @param wr
	 * @param name
	 * @param cameraIV
	 *            camera imave view
	 * @param speakingIV
	 *            speaker image view
	 */
	private void setStyle(Wrapper wr, TextView name, ImageView lectureStateIV,
			ImageView speakingIV, ImageView cameraIV) {
		Attendee at = wr.a;

		// 有可能是混合视频
		DeviceType atDeviceType = null;
		if (at.getType() == Attendee.TYPE_ATTENDEE) {
			User user = GlobalHolder.getInstance().getUser(at.getAttId());
			if (user != null) {
				atDeviceType = user.getDeviceType();
			}
		} else if (at.getType() == Attendee.TYPE_MIXED_VIDEO) {

		}

		UserDeviceConfig udc = wr.udc;

		if (at.isChairMan() || conf.getChairman() == at.getAttId()) {
			if (at.isSelf() || at.isJoined()) {
				name.setTextColor(getContext().getResources().getColor(
						R.color.video_attendee_chair_man_name_color));
			} else {
				name.setTextColor(getContext().getResources().getColor(
						R.color.video_attendee_name_color_offline));
			}
		} else if (at.isSelf()) {
			name.setTextColor(getContext().getResources().getColor(
					R.color.video_attendee_name_color));
			// set camera icon
			if (atDeviceType != null && atDeviceType == DeviceType.CELL_PHONE) {
				cameraIV.setImageResource(R.drawable.phone_camera);
			} else {
				cameraIV.setImageResource(R.drawable.camera);
			}

		} else if (at.isJoined()) {
			name.setTextColor(getContext().getResources().getColor(
					R.color.video_attendee_name_color));
		} else {
			name.setTextColor(getContext().getResources().getColor(
					R.color.video_attendee_name_color_offline));
		}

		// 如果是自己，则名字改成粗体
		if (at.isSelf()) {
			name.setTypeface(null, Typeface.BOLD);
		} else {
			name.setTypeface(null, Typeface.NORMAL);
		}

		// set image view
		if (at.isSelf()) {
			if (atDeviceType != null && atDeviceType == DeviceType.CELL_PHONE) {
				cameraIV.setImageResource(R.drawable.phone_camera);
			} else {
				cameraIV.setImageResource(R.drawable.camera);
			}
		} else if (at.isJoined()) {
			if (at.getType() != Attendee.TYPE_MIXED_VIDEO) {
				if (udc != null) {
					if (atDeviceType != null
							&& atDeviceType == DeviceType.CELL_PHONE) {
						cameraIV.setImageResource(R.drawable.phone_camera);
					} else {
						cameraIV.setImageResource(R.drawable.camera);
					}
				} else {
					if (atDeviceType != null
							&& atDeviceType == DeviceType.CELL_PHONE) {
						cameraIV.setImageResource(R.drawable.phone_camera_pressed);
					} else {
						cameraIV.setImageResource(R.drawable.camera_pressed);
					}
				}
			} else {
				cameraIV.setImageResource(R.drawable.mixed_video_camera);
			}
		} else {
			if (atDeviceType != null && atDeviceType == DeviceType.CELL_PHONE) {
				cameraIV.setImageResource(R.drawable.phone_camera_pressed);
			} else {
				cameraIV.setImageResource(R.drawable.camera_pressed);
			}
		}

		// If attaendee is mixed video or is not default flag, then hide speaker
		if (at.getType() == Attendee.TYPE_MIXED_VIDEO
				|| wr.sortFlag != FIRST_DEVICE_FLAG) {
			lectureStateIV.setVisibility(View.INVISIBLE);
			speakingIV.setVisibility(View.INVISIBLE);
		}

		// if (udc != null && !udc.isEnable()
		// && at.getType() == Attendee.TYPE_ATTENDEE && at.isSelf()) {
		// iv.setImageResource(R.drawable.camera_pressed);
		// }

		if (udc != null && !udc.isEnable()
				&& at.getType() == Attendee.TYPE_ATTENDEE) {
			if (atDeviceType != null && atDeviceType == DeviceType.CELL_PHONE) {
				cameraIV.setImageResource(R.drawable.phone_camera_pressed);
			} else {
				cameraIV.setImageResource(R.drawable.camera_pressed);
			}
		}

		if (at.isJoined()) {
			if (wr.sortFlag == FIRST_DEVICE_FLAG) {
				// Update lecture state display
				switch (at.getLectureState()) {
				case Attendee.LECTURE_STATE_NOT:
					lectureStateIV.setVisibility(View.INVISIBLE);
					break;
				case Attendee.LECTURE_STATE_APPLYING:
					lectureStateIV.setVisibility(View.VISIBLE);
					lectureStateIV
							.setImageResource(R.drawable.lecture_state_applaying);
					break;
				case Attendee.LECTURE_STATE_GRANTED:
					lectureStateIV.setVisibility(View.VISIBLE);
					lectureStateIV
							.setImageResource(R.drawable.lecture_state_granted);
					break;
				}

				// Update speaking display
				if (!at.isSpeaking()) {
					speakingIV.setVisibility(View.INVISIBLE);
				} else {
					speakingIV.setVisibility(View.VISIBLE);
					speakingIV.setImageResource(R.drawable.conf_speaking);
					((AnimationDrawable) speakingIV.getDrawable()).start();
				}
			} else {
				lectureStateIV.setVisibility(View.INVISIBLE);
				speakingIV.setVisibility(View.INVISIBLE);
			}
		} else {
			lectureStateIV.setVisibility(View.INVISIBLE);
			speakingIV.setVisibility(View.INVISIBLE);
		}

	}

	public void setAttendsList(Set<Attendee> l) {
		// Copy list for void concurrency exception
		List<Attendee> list = new ArrayList<Attendee>(l);
		for (Attendee at : list) {
			List<UserDeviceConfig> dList = at.getmDevices();

			int i = 0;
			int deviceIndex = 1;
			do {
				if (dList == null || dList.size() <= 0) {
					mList.add(new Wrapper(at, null, FIRST_DEVICE_FLAG));
				} else {
					// mList.add(new Wrapper(at, dList.get(i),
					// i == 0 ? DEFAULT_DEVICE_FLAG : deviceIndex++));
					// 20141225 2
					UserDeviceConfig tempUdc = dList.get(i);

					if (i == 0) {
						mList.add(new Wrapper(at, tempUdc, FIRST_DEVICE_FLAG));
					} else {
						mList.add(new Wrapper(at, tempUdc, deviceIndex++));
					}
				}
				i++;
			} while (dList != null && i < dList.size());
			// 设备显示不出问题时删除这段注释
			// 默认的设备在第一个，现在不需要
			// int deviceIndex = 1;
			//
			// if (dList == null || dList.size() <= 0) {
			// mList.add(new Wrapper(at, null, DEFAULT_DEVICE_FLAG));
			// } else {
			// UserDeviceConfig defaultDevice = null;
			// for (int i = 0; i < dList.size(); i++) {
			// UserDeviceConfig udc = dList.get(i);
			// if (udc.isDefault()) {
			// defaultDevice = udc;
			// }
			// }
			//
			// // 再添加用户的设备信息
			// if (defaultDevice != null) {
			// mList.add(new Wrapper(at, defaultDevice,
			// DEFAULT_DEVICE_FLAG));
			// for (int i = 0; i < dList.size(); i++) {
			// UserDeviceConfig udc = dList.get(i);
			// if (!udc.isDefault()) {
			// mList.add(new Wrapper(at, udc, deviceIndex++));
			// }
			// }
			// } else {
			// for (int i = 0; i < dList.size(); i++) {
			// UserDeviceConfig udc = dList.get(i);
			// if (i == 0) {
			// mList.add(new Wrapper(at, udc, DEFAULT_DEVICE_FLAG));
			// } else {
			// mList.add(new Wrapper(at, udc, deviceIndex++));
			// }
			// }
			// }
			//
			// }

			configAttendee(at);
		}
		Collections.sort(mList);
		adapter.notifyDataSetChanged();
	}

	public void updateEnteredAttendee(Attendee at) {
		if (at == null) {
			return;
		}

		at.setJoined(true);
		List<UserDeviceConfig> dList = at.getmDevices();
		if (at.getType() == Attendee.TYPE_MIXED_VIDEO) {
			mList.add(new Wrapper(at, dList.get(0), FIRST_DEVICE_FLAG));
		} else {
			// for fast enter conference user
			int index = 0;
			boolean isFound = false;
			boolean isAdd = true;
			;
			for (int i = 0; i < mList.size(); i++) {
				Attendee attendee = mList.get(i).a;
				if (attendee.getAttId() == at.getAttId()) {
					isFound = true;
					if (dList != null && dList.size() > 0) {
						mList.get(i).udc = dList.get(0);
						mList.get(i).sortFlag = FIRST_DEVICE_FLAG;
						index = i;
					}
					break;
				}
			}

			if (dList != null) {
				for (int i = 1; i < dList.size(); i++) {
					if (index + 1 == mList.size() - 1) {
						mList.add(new Wrapper(at, dList.get(i), i));
						isAdd = false;
					} else {
						mList.add(index + 1, new Wrapper(at, dList.get(i), i));
						isAdd = false;
					}
					index++;
				}
			}

			if (!isFound) {
				mAttendeeCount++;
				if (isAdd) {
					mList.add(new Wrapper(at, null, FIRST_DEVICE_FLAG));
				}
			}

			if ((at.isJoined() || at.isSelf())) {
				onLinePersons++;
			}
			updateStatist();

			// // boolean isNew = false;
			// int index = 0;
			// if (mList.size() > 0 && dList != null && dList.size() > 0) {
			//
			// for (int i = 0; i < mList.size(); i++) {
			// Wrapper wr = mList.get(i);
			// if (wr.a.getAttId() == at.getAttId()) {
			// index = i;
			// break;
			// }
			// }
			//
			// }
			//
			// int deviceIndex = 1;
			// for (int i = 0; i < dList.size(); i++) {
			// UserDeviceConfig udc = dList.get(i);
			// if (i == 0) {
			//
			//
			// mList.add(index, new Wrapper(at, udc, FIRST_DEVICE_FLAG));
			// } else {
			// if (index + 1 == mList.size() - 1) {
			// mList.add(new Wrapper(at, udc, deviceIndex++));
			// } else {
			// mList.add(index + 1,
			// new Wrapper(at, udc, deviceIndex++));
			// }
			// index++;
			// // isNew = true;
			// }
			//
			// }

			// 设备显示不出问题时删除这段注释
			// if (dList != null) {
			// UserDeviceConfig defaultDevice = null;
			// for (int i = 0; i < dList.size(); i++) {
			// UserDeviceConfig udc = dList.get(i);
			// if (udc.isDefault()) {
			// defaultDevice = udc;
			// }
			// }
			//
			// int deviceIndex = 1;
			// if (defaultDevice != null) {
			// mList.add(index, new Wrapper(at, defaultDevice,
			// FIRST_DEVICE_FLAG));
			// for (int i = 0; i < dList.size(); i++) {
			// UserDeviceConfig udc = dList.get(i);
			// if (!udc.isDefault()) {
			// if (index + 1 == mList.size() - 1) {
			// mList.add(new Wrapper(at, udc, deviceIndex++));
			// } else {
			// mList.add(index + 1, new Wrapper(at, udc,
			// deviceIndex++));
			// }
			// index++;
			// // isNew = true;
			// }
			// }
			// } else {
			// for (int i = 0; i < dList.size(); i++) {
			// UserDeviceConfig udc = dList.get(i);
			// if (i == 0) {
			// mList.add(index, new Wrapper(at, udc,
			// FIRST_DEVICE_FLAG));
			// } else {
			// if (index + 1 == mList.size() - 1) {
			// mList.add(new Wrapper(at, udc, deviceIndex++));
			// } else {
			// mList.add(index + 1, new Wrapper(at, udc,
			// deviceIndex++));
			// }
			// index++;
			// // isNew = true;
			// }
			//
			// }
			//
			// }
			//
			// }

			// if (isNew)
			// mAttendeeCount++;
			// if ((at.isJoined() || at.isSelf())) {
			// onLinePersons++;
			// }
			// updateStatist();

		}

		Collections.sort(mList);
		adapter.notifyDataSetChanged();
	}

	public void updateExitedAttendee(Attendee at) {
		V2Log.d(TAG, "updateExitedAttendee 执行了");
		if (at == null) {
			return;
		}

		at.setmDevices(null);
		at.setJoined(false);
		at.setSpeakingState(false);
		at.setLectureState(Attendee.LECTURE_STATE_NOT);
		boolean found = false;
		for (int i = 0; i < mList.size(); i++) {
			Wrapper wr = mList.get(i);
			// Remove attendee devices, leave one device item
			if (wr.a.getAttId() == at.getAttId()) {
				if (found || wr.a.getType() == Attendee.TYPE_MIXED_VIDEO
						|| wr.a.isRmovedFromList) {
					mList.remove(i--);
					continue;
				} else {
					found = true;
				}
			}
		}

		if (at.isRmovedFromList) {
			mAttendeeCount--;
		}

		// Update on line count
		if (at.getType() != Attendee.TYPE_MIXED_VIDEO) {
			onLinePersons--;
			updateStatist();
		}

		Collections.sort(mList);
		adapter.notifyDataSetChanged();
	}

	/**
	 * Update attendee device status
	 * 
	 * @param att
	 * @param udc
	 */
	// public void updateAttendeeDevice(Attendee att, UserDeviceConfig udc) {
	//
	// for (int i = 0; i < mList.size(); i++) {
	// Wrapper wr = mList.get(i);
	// // Remove attendee devices, leave one device item
	// if (wr.a.getAttId() == att.getAttId()) {
	// // If user doesn't exist device before, then set
	// if (wr.udc == null) {
	// wr.udc = udc;
	// } else {
	// wr.udc.setShowing(false);
	// wr.udc.setEnable(udc.isEnable());
	// }
	// }
	// }
	// adapter.notifyDataSetChanged();
	// }

	public void updateAttendeeDevice(Attendee att, UserDeviceConfig udc) {

		for (int i = 0; i < mList.size(); i++) {
			Wrapper wr = mList.get(i);
			// Remove attendee devices, leave one device item
			if (wr.a.getAttId() == att.getAttId()) {
				// If user doesn't exist device before, then set
				if (wr.udc == null) {
					wr.udc = udc;
					break;
				} else {
					if (wr.udc.getDeviceID().equals(udc.getDeviceID())) {
						wr.udc.setShowing(false);
						wr.udc.setEnable(udc.isEnable());
					}
				}
			}
		}
		adapter.notifyDataSetChanged();

	}

	/**
	 * reset attendee devices
	 * 
	 * @param att
	 * @param list
	 */
	public void resetAttendeeDevices(Attendee att, List<UserDeviceConfig> list) {
		Wrapper attendeeFirstWrapper = null;
		int index = -1;
		// Remove exists devices
		for (int i = 0; i < mList.size(); i++) {
			Wrapper wr = mList.get(i);
			// Remove attendee devices, leave one device item
			if (wr.a.getAttId() == att.getAttId()) {
				if (wr.sortFlag == FIRST_DEVICE_FLAG) {
					wr.udc = null;
					attendeeFirstWrapper = wr;
					index = i;
				} else {
					mList.remove(i--);
				}
			}
		}

		if (attendeeFirstWrapper == null) {
			V2Log.e("Error no first device ");
			return;
		}

		int deviceIndex = 1;
		for (int i = 0; i < list.size(); i++) {
			UserDeviceConfig udc = list.get(i);
			if (udc.getBelongsAttendee() == null) {
				udc.setBelongsAttendee(att);
			}

			if (i == 0) {
				attendeeFirstWrapper.udc = udc;
			} else {
				mList.add(++index, new Wrapper(attendeeFirstWrapper.a, udc,
						deviceIndex++));
			}
		}

		// 设备显示不出问题时删除这段注释
		// boolean hasDefaultDevice = false;
		// for (int i = 0; i < list.size(); i++) {
		// UserDeviceConfig udc = list.get(i);
		// if (udc.isDefault()) {
		// hasDefaultDevice = true;
		// }
		// }
		// // 再添加用户的设备信息
		// int deviceIndex = 1;
		// if (hasDefaultDevice) {
		// for (int i = 0; i < list.size(); i++) {
		// UserDeviceConfig udc = list.get(i);
		// if (udc.getBelongsAttendee() == null) {
		// udc.setBelongsAttendee(att);
		// }
		//
		// if (udc.isDefault()) {
		// defaultWrapper.udc = udc;
		// } else {
		// mList.add(++index, new Wrapper(defaultWrapper.a, udc,
		// deviceIndex++));
		// }
		//
		// }
		//
		// } else {
		// for (int i = 0; i < list.size(); i++) {
		// UserDeviceConfig udc = list.get(i);
		// if (udc.getBelongsAttendee() == null) {
		// udc.setBelongsAttendee(att);
		// }
		//
		// if (i == 0) {
		// defaultWrapper.udc = udc;
		// } else {
		// mList.add(++index, new Wrapper(defaultWrapper.a, udc,
		// deviceIndex++));
		// }
		//
		// }
		// }

		// FIXME update status for already devices
		adapter.notifyDataSetChanged();

	}

	/**
	 * Update attendee speaker image according to user speaking state
	 * 
	 * @param at
	 */
	public void updateAttendeeSpeakingState(Attendee at) {
		adapter.notifyDataSetChanged();
	}

	/**
	 * Remove attend and device from layout.
	 * 
	 * @param at
	 */
	public void removeAttendee(Attendee at) {
		for (int i = 0; i < mList.size(); i++) {
			Wrapper wr = mList.get(i);
			// Remove attendee devices, leave one device item
			if (wr.a.getAttId() == at.getAttId()) {
				mList.remove(i--);
				continue;
			}
		}
		// update attendee members
		mAttendeeCount--;
		updateStatist();
		adapter.notifyDataSetChanged();
	}

	/**
	 * Used to manually request FloatLayout, Because when this layout will hide,
	 * call this function to inform interface
	 */
	public void requestFloatLayout() {

		if ("float".equals(mPinButton.getTag())) {
			return;
		}

		if (this.listener != null) {
			this.listener.requestAttendeeViewFloatLayout(rootView);
		}

		mPinButton.setTag("float");
		((ImageView) mPinButton)
				.setImageResource(R.drawable.pin_button_selector);
	}

	/**
	 * request update background of current selected Item
	 * 
	 * @param flag
	 */
	public void updateCurrentSelectedBg(boolean flag, Attendee at,
			UserDeviceConfig udc) {

		// for (int i = 0; i < mAttendsView.size(); i++) {
		// ViewWrapper v = mAttendsView.get(i);
		// Wrapper w = (Wrapper) v.v.getTag();
		// if (w.a == at && udc == w.udc) {
		// if (flag) {
		// v.v.setBackgroundColor(getContext().getResources()
		// .getColor(R.color.attendee_select_bg));
		// } else {
		// v.v.setBackgroundColor(Color.WHITE);
		// }
		// v.v.invalidate();
		// break;
		// }
		// }

		adapter.notifyDataSetChanged();
	}

	private OnTouchListener mListViewOnTouchListener = new OnTouchListener() {

		private View dragView;
		private Bitmap cbm;

		@Override
		public synchronized boolean onTouch(View view, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {

				if (cbm != null && !cbm.isRecycled()) {
					cbm.recycle();
					cbm = null;
				}

				Wrapper w = (Wrapper) ((View) view.getParent().getParent())
						.getTag();
				dragView = getView(w);

				WindowManager wd = (WindowManager) getContext()
						.getSystemService(Context.WINDOW_SERVICE);
				WindowManager.LayoutParams vl = getLayoutParams(event);
				wd.addView(dragView, vl);
				mAttendeeContainer
						.setOverScrollMode(ListView.OVER_SCROLL_NEVER);
			} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				if (dragView != null) {
					WindowManager wd = (WindowManager) getContext()
							.getSystemService(Context.WINDOW_SERVICE);
					WindowManager.LayoutParams vl = getLayoutParams(event);
					wd.updateViewLayout(dragView, vl);
				}

			} else if (event.getAction() == MotionEvent.ACTION_UP
					|| event.getAction() == MotionEvent.ACTION_CANCEL) {
				if (dragView != null) {
					if (listener != null
							&& event.getAction() != MotionEvent.ACTION_CANCEL) {
						View itemView = (View) (View) view.getParent()
								.getParent();
						Wrapper wr = (Wrapper) itemView.getTag();
						// Rect r = new Rect();
						// mAttendeeContainer.getLocalVisibleRect(r);
						// if (r.contains((int) event.getX(), (int)
						// event.getY())) {
						// listener.OnAttendeeClicked(wr.a, wr.udc);
						// } else
						{
							listener.OnAttendeeDragged(wr.a, wr.udc,
									(int) event.getRawX(),
									(int) event.getRawY());
						}
					}
					WindowManager wd = (WindowManager) getContext()
							.getSystemService(Context.WINDOW_SERVICE);
					wd.removeView(dragView);
				}
				mAttendeeContainer
						.setOverScrollMode(ListView.OVER_SCROLL_ALWAYS);
				dragView = null;
			}
			return true;
		}

		private WindowManager.LayoutParams getLayoutParams(MotionEvent event) {
			WindowManager.LayoutParams vl = new WindowManager.LayoutParams();
			vl.format = PixelFormat.TRANSLUCENT;
			vl.gravity = Gravity.TOP | Gravity.LEFT;
			vl.width = 80;
			vl.height = 80;
			vl.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
			vl.x = (int) event.getRawX();
			vl.y = (int) event.getRawY();
			return vl;
		}

		private View getView(Wrapper wr) {
			RelativeLayout rl = new RelativeLayout(getContext());
			ImageView iv = new ImageView(getContext());
			Bitmap bm = wr.a.getAvatar();
			if (bm == null) {
				bm = BitmapFactory.decodeResource(getContext().getResources(),
						R.drawable.avatar);
				cbm = bm;
			}
			iv.setImageBitmap(bm);
			RelativeLayout.LayoutParams rll = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			rll.addRule(RelativeLayout.CENTER_IN_PARENT);
			rl.addView(iv, rll);

			TextView tv = new TextView(getContext());
			tv.setText(wr.a.getAttName());
			tv.setGravity(Gravity.CENTER);
			tv.setEllipsize(TruncateAt.END);
			tv.setMaxWidth(60);
			tv.setSingleLine();
			rl.addView(tv, rll);

			return rl;
		}
	};

	private OnClickListener mRequestFixedListener = new OnClickListener() {

		@Override
		public void onClick(View view) {

			if (view.getTag().equals("float")) {
				if (listener != null) {
					listener.requestAttendeeViewFixedLayout(rootView);
				}
			} else {
				if (listener != null) {
					listener.requestAttendeeViewFloatLayout(rootView);
				}
			}

			if (view.getTag().equals("float")) {
				view.setTag("fix");
				((ImageView) view)
						.setImageResource(R.drawable.pin_fixed_button_selector);
			} else {
				view.setTag("float");
				((ImageView) view)
						.setImageResource(R.drawable.pin_button_selector);
			}
		}

	};

	public boolean getWindowSizeState() {
		String str = (String) mPinButton.getTag();
		if (str == null || str.equals("float")) {
			return false;
		} else {
			return true;
		}
	}

	private boolean isFrist;
	private TextWatcher mSearchListener = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable et) {
			if (TextUtils.isEmpty(et)) {
				SearchUtils.clearAll();
				mIsStartedSearch = SearchUtils.mIsStartedSearch;
				isFrist = true;
				mSearchFilterList.clear();
				adapter.notifyDataSetChanged();
			} else {
				if (isFrist) {
					SearchUtils.clearAll();
					List<Object> wrappers = new ArrayList<Object>();
					wrappers.addAll(mList);
					SearchUtils.receiveList = wrappers;
					isFrist = false;
				}

				mSearchFilterList = SearchUtils.startVideoAttendeeSearch(et
						.toString());
				mIsStartedSearch = SearchUtils.mIsStartedSearch;
				adapter.notifyDataSetChanged();
			}
		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int cout) {

		}

	};

	class AttendeesAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			if (mIsStartedSearch)
				return mSearchFilterList.size();
			else
				return mFilterList.size();
		}

		@Override
		public Object getItem(int position) {
			if (mIsStartedSearch)
				return mSearchFilterList.get(position);
			else
				return mFilterList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Wrapper wrapper;
			if (mIsStartedSearch)
				wrapper = mSearchFilterList.get(position);
			else
				wrapper = mFilterList.get(position);

			if (convertView == null) {
				convertView = buildAttendeeView(wrapper);
			} else {
				updateView(wrapper, convertView);
			}
			return convertView;
		}
	}

	public class Wrapper implements Comparable<Wrapper> {
		public Attendee a;
		public UserDeviceConfig udc;
		// Use to sort.
		// we can remove view if sortFlag is DEFAULT_DEVICE_FLAG
		int sortFlag;

		public Wrapper(Attendee a, UserDeviceConfig udc, int sortFlag) {
			super();
			this.a = a;
			this.udc = udc;
			this.sortFlag = sortFlag;
		}

		@Override
		public int compareTo(Wrapper wr) {

			if (this.a == null) {
				if (wr.a == null) {
					return 0;
				} else {
					return 1;
				}
			} else if (this.a.getType() == Attendee.TYPE_MIXED_VIDEO) {
				if (wr.a == null) {
					return -1;
				} else if (wr.a.getType() == Attendee.TYPE_MIXED_VIDEO) {
					return calledByCompareTo(wr);
				} else {
					return -1;
				}

			} else if (this.a.isChairMan()) {
				if (wr.a == null) {
					return -1;
				} else if (wr.a.getType() == Attendee.TYPE_MIXED_VIDEO) {
					return 1;
				} else {
					return -1;
				}
			} else if (this.a.getLectureState() == Attendee.LECTURE_STATE_GRANTED) {
				if (wr.a == null) {
					return -1;
				} else if (wr.a.getType() == Attendee.TYPE_MIXED_VIDEO) {
					return 1;
				} else if (wr.a.isChairMan()) {
					return 1;
				} else {
					return -1;
				}
			} else if (this.a.getLectureState() == Attendee.LECTURE_STATE_APPLYING) {
				if (wr.a == null) {
					return -1;
				} else if (wr.a.getType() == Attendee.TYPE_MIXED_VIDEO) {
					return 1;
				} else if (wr.a.isChairMan()) {
					return 1;
				} else if (wr.a.getLectureState() == Attendee.LECTURE_STATE_GRANTED) {
					return 1;
				} else if (wr.a.getLectureState() == Attendee.LECTURE_STATE_APPLYING) {
					// return 0;
					return calledByCompareTo(wr);
				} else {
					return -1;
				}
			} else if (this.a.isSpeaking()) {
				if (wr.a == null) {
					return -1;
				} else if (wr.a.getType() == Attendee.TYPE_MIXED_VIDEO) {
					return 1;
				} else if (wr.a.isChairMan()) {
					return 1;
				} else if (wr.a.getLectureState() == Attendee.LECTURE_STATE_GRANTED) {
					return 1;
				} else if (wr.a.getLectureState() == Attendee.LECTURE_STATE_APPLYING) {
					return 1;
				} else if (this.a.isSpeaking()) {
					return calledByCompareTo(wr);

				} else {
					return -1;
				}
			} else {
				if (wr.a == null) {
					return -1;
				} else if (wr.a.getType() == Attendee.TYPE_MIXED_VIDEO) {
					return 1;
				} else if (wr.a.isChairMan()) {
					return 1;
				} else if (wr.a.getLectureState() == Attendee.LECTURE_STATE_GRANTED) {
					return 1;
				} else if (wr.a.getLectureState() == Attendee.LECTURE_STATE_APPLYING) {
					return 1;
				} else if (this.a.isSpeaking()) {
					return 1;
				} else {
					return calledByCompareTo(wr);
				}
			}

		}

		private int calledByCompareTo(Wrapper wr) {
			int ret = this.a.compareTo(wr.a);
			if (ret == 0) {
				return this.sortFlag;
			} else if (ret < 0 || ret > 0) {
				if (this.a.isJoined()) {
					return -1;
				} else if (wr.a.isJoined()) {
					return 1;
				} else {
					return 0;
				}
			} else {
				return ret;
			}
		}

	}

	private void configAttendee(Attendee at) {
		if (at == null) {
			return;
		}
		if (at.getType() != Attendee.TYPE_MIXED_VIDEO) {
			mAttendeeCount++;
			if ((at.isJoined() || at.isSelf())) {
				onLinePersons++;
			}
		}
		updateStatist();
	}

	public void updateStatist() {
		attendPersons.setText(onLinePersons + "/" + (mAttendeeCount));
		adapter.notifyDataSetChanged();
	}

	public void updateDisplay() {
		adapter.notifyDataSetChanged();
	}

}
