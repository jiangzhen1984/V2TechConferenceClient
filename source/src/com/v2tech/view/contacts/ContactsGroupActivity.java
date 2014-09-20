package com.v2tech.view.contacts;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.ContactsService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.jni.GroupServiceJNIResponse;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.widget.CommonAdapter;
import com.v2tech.view.widget.CommonAdapter.CommonAdapterItemWrapper;
import com.v2tech.view.widget.CommonAdapter.ViewConvertListener;
import com.v2tech.vo.ContactGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;

public class ContactsGroupActivity extends Activity {

	private static final int CREATE_GROUP_DONE = 1;
	private static final int UPDATE_GROUP_DONE = 2;
	private static final int REMOVE_GROUP_DONE = 3;

	private TextView mDialogTitleTV;
	private EditText mGroupNameET;
	private Dialog mDialog;
	private Context mContext;
	private ListView mListView;

	private List<CommonAdapterItemWrapper> mDataset;
	private BaseAdapter adapter;

	private ContactsService contactService = new ContactsService();
	
	private boolean changed;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.activity_contacts_group);
		mListView = (ListView) findViewById(R.id.contacts_group_listview);

		View finishButton = findViewById(R.id.contacts_group_title_button);
		finishButton.setOnClickListener(finishClickListener);

		View createGroupButton = findViewById(R.id.contacts_group_add_button);
		createGroupButton.setOnClickListener(createGroupButtonClickListener);

		List<Group> listGroup = GlobalHolder.getInstance().getGroup(
				GroupType.CONTACT.intValue());
		mDataset = convert(listGroup);
		adapter = new CommonAdapter(mDataset, converter);
		mListView.setAdapter(adapter);
		mListView.setOnItemLongClickListener(mLongClickListener);
	}

	@Override
	public void finish() {
		if (changed) {
			Intent i = new Intent(
					PublicIntent.BROADCAST_REQUEST_UPDATE_CONTACTS_GROUP);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			mContext.sendBroadcast(i);
		}
		super.finish();
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}
	
	
	

	@Override
	protected void onDestroy() {
		super.onDestroy();
		contactService.clearCalledBack();
	}

	private List<CommonAdapterItemWrapper> convert(List<Group> listGroup) {
		List<CommonAdapterItemWrapper> ds = new ArrayList<CommonAdapterItemWrapper>(
				listGroup.size());
		for (int i = 0; i < listGroup.size(); i++) {
			final Group g = listGroup.get(i);
			ds.add(new CommonAdapterItemWrapper() {

				@Override
				public Object getItemObject() {
					return g;
				}

				@Override
				public long getItemLongId() {
					return g.getmGId();
				}

				@Override
				public View getView() {
					return null;
				}

			});
		}
		return ds;
	}

	private void showDialog(final ContactGroup group) {
		if (mDialog == null) {

			mDialog = new Dialog(this, R.style.ContactUserActionDialog);

			mDialog.setContentView(R.layout.activity_contacts_group_dialog);
			mDialogTitleTV = (TextView) mDialog
					.findViewById(R.id.contacts_group_title);
			mGroupNameET = (EditText) mDialog
					.findViewById(R.id.contacts_group_name);
			mGroupNameET.setOnFocusChangeListener(textListener);
		}

		if (group != null) {
			mDialogTitleTV
					.setText(R.string.activiy_contact_group_dialog_title_update);
			mGroupNameET.setText(group.getName());
		} else {
			//mGroupNameET.setText(R.string.activiy_contact_group_name_content);
			mDialogTitleTV
					.setText(R.string.activiy_contact_group_dialog_title_create);
		}
		
		
		final Button cancelB = (Button) mDialog
				.findViewById(R.id.contacts_group_cancel_button);
		cancelB.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(mGroupNameET.getWindowToken(),
						0);
				mDialog.dismiss();
			}

		});
		final Button confirmButton = (Button) mDialog
				.findViewById(R.id.contacts_group_confirm_button);
		confirmButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(mGroupNameET.getWindowToken(),
						0);

				if (mGroupNameET.getText().toString().trim().isEmpty()) {
					mGroupNameET.setError(mContext
							.getText(R.string.activiy_contact_group_dialog_group_name_required));
					return;
				}
				if (group == null) {
					ContactGroup newGroup = new ContactGroup(0,
							mGroupNameET.getText().toString());
					updateGroup(newGroup, OPT.CREATE);
				} else {
					group.setName(mGroupNameET.getText().toString());
					updateGroup(group, OPT.UPDATE);
				}
			}

		});

		
		

		mDialog.show();
	}

	private void updateGroup(ContactGroup group, OPT opt) {
		if (opt == OPT.CREATE) {
			contactService.createGroup(group, new Registrant(mLocalHandler,
					CREATE_GROUP_DONE, null));
		} else if (opt == OPT.UPDATE) {
			contactService.updateGroup(group, new Registrant(mLocalHandler,
					UPDATE_GROUP_DONE, null));
		} else {
			contactService.removeGroup(group, new Registrant(mLocalHandler,
					REMOVE_GROUP_DONE, null));
		}
	}

	private OnClickListener finishClickListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			finish();
		}

	};

	private OnClickListener createGroupButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			showDialog(null);
		}

	};

	private OnClickListener deleteGroupButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			updateGroup((ContactGroup) view.getTag(), OPT.DELETE);
		}

	};

	private OnFocusChangeListener textListener = new OnFocusChangeListener() {

		@Override
		public void onFocusChange(View view, boolean flag) {
			if (flag
					&& mGroupNameET
							.getText()
							.equals(mContext
									.getText(R.string.activiy_contact_group_name_content))) {
				mGroupNameET.setText("");
			} else if (!flag) {
				mGroupNameET
						.setText(R.string.activiy_contact_group_name_content);
			}
		}

	};

	private OnItemLongClickListener mLongClickListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> adapter, View view,
				int pos, long id) {
			showDialog((ContactGroup) mDataset.get(pos).getItemObject());
			return true;
		}

	};

	private ViewConvertListener converter = new ViewConvertListener() {

		@Override
		public View converView(CommonAdapterItemWrapper wr, View view,
				ViewGroup vg) {
			if (view == null) {
				view = LayoutInflater.from(mContext).inflate(
						R.layout.activity_contacts_group_adapter_item, null,
						false);
			}
			TextView tv = (TextView) view
					.findViewById(R.id.contacts_group_item_name);
			tv.setText(((Group) wr.getItemObject()).getName());
			View v = view
					.findViewById(R.id.contacts_group_item_adapter_delelte_button);
			v.setTag(wr.getItemObject());
			if (((ContactGroup)wr.getItemObject()).isDefault()) {
				v.setVisibility(View.INVISIBLE);
				view.findViewById(R.id.contacts_group_delete_icon).setVisibility(View.INVISIBLE);
			} else {
				v.setVisibility(View.VISIBLE);;
				v.setOnClickListener(deleteGroupButtonClickListener);
				view.findViewById(R.id.contacts_group_delete_icon).setVisibility(View.VISIBLE);
			}
			
			return view;
		}

	};

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			JNIResponse res = (JNIResponse) msg.obj;
			switch (msg.what) {
			case CREATE_GROUP_DONE:
				if (res.getResult() == JNIResponse.Result.SUCCESS) {
					final Group g = ((GroupServiceJNIResponse) res).g;
					mDataset.add(new CommonAdapterItemWrapper() {

						@Override
						public Object getItemObject() {
							return g;
						}

						@Override
						public long getItemLongId() {
							return g.getmGId();
						}

						@Override
						public View getView() {
							return null;
						}

					});
				}
				break;
			case UPDATE_GROUP_DONE:
				break;
			case REMOVE_GROUP_DONE:
				if (res.getResult() == JNIResponse.Result.SUCCESS) {
					for (int i = 0; i < mDataset.size(); i++) {
						Group g = (Group) mDataset.get(i).getItemObject();
						if (((GroupServiceJNIResponse) res).g.getmGId() == g
								.getmGId()) {
							mDataset.remove(i);
							break;
						}
					}
				}
				break;

			}
			if (res.getResult() == JNIResponse.Result.SUCCESS) {
				adapter.notifyDataSetChanged();
			}
			if (mDialog != null) {
				mDialog.dismiss();
			}
			
			changed = true;
		}

	};

	enum OPT {
		CREATE, UPDATE, DELETE;
	}

}
