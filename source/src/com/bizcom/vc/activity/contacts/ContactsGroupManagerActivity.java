package com.bizcom.vc.activity.contacts;

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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.util.EscapedcharactersProcessing;
import com.bizcom.request.ContactsService;
import com.bizcom.request.MessageListener;
import com.bizcom.request.jni.GroupServiceJNIResponse;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.vc.adapter.CommonAdapter;
import com.bizcom.vc.adapter.CommonAdapter.CommonAdapterItemDateAndViewWrapper;
import com.bizcom.vc.adapter.CommonAdapter.CommonAdapterGetViewListener;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vo.ContactGroup;
import com.bizcom.vo.Group;
import com.bizcom.vo.Group.GroupType;
import com.v2tech.R;

public class ContactsGroupManagerActivity extends Activity {

	private static final int CREATE_GROUP_DONE = 1;
	private static final int UPDATE_GROUP_DONE = 2;
	private static final int REMOVE_GROUP_DONE = 3;

	private TextView mDialogTitleTV;
	private EditText mGroupNameET;
	private Dialog mDialog;
	private Context mContext;
	private ListView mListView;

	private List<CommonAdapterItemDateAndViewWrapper> mDataset;
	private BaseAdapter adapter;

	private ContactsService contactService = new ContactsService();

	private boolean changed;
	private boolean inDeleteMode = true;

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
		mListView.setOnItemClickListener(mClickListener);
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

	private List<CommonAdapterItemDateAndViewWrapper> convert(List<Group> listGroup) {
		List<CommonAdapterItemDateAndViewWrapper> ds = new ArrayList<CommonAdapterItemDateAndViewWrapper>(
				listGroup.size() - 1);
		for (int i = 0; i < listGroup.size(); i++) {
			final Group g = listGroup.get(i);
			if (((ContactGroup) g).isDefault()) {
				continue;
			}
			ds.add(new LocalItemWrapper(g, false));
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
			mGroupNameET.setText("");
			// mGroupNameET.setText(R.string.activiy_contact_group_name_content);
			mDialogTitleTV
					.setText(R.string.activiy_contact_group_dialog_title_create);
		}

		final Button cancelB = (Button) mDialog
				.findViewById(R.id.contacts_group_cancel_button);
		cancelB.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(mGroupNameET.getWindowToken(), 0);
				mDialog.dismiss();
			}

		});
		final Button confirmButton = (Button) mDialog
				.findViewById(R.id.contacts_group_confirm_button);
		confirmButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!GlobalHolder.getInstance().isServerConnected()) {
					Toast.makeText(mContext, R.string.error_local_connect_to_server, Toast.LENGTH_SHORT).show();
					return ;
				}
				
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(mGroupNameET.getWindowToken(), 0);

				if (mGroupNameET.getText().toString().trim().isEmpty()) {
					mGroupNameET.setError(mContext
							.getText(R.string.activiy_contact_group_dialog_group_name_required));
					return;
				}
				
				String groupName = EscapedcharactersProcessing.convert(mGroupNameET.getText().toString());
				if (group == null) {
					ContactGroup newGroup = new ContactGroup(0, groupName);
					updateGroup(newGroup, OPT.CREATE);
				} else {
					group.setName(groupName);
					updateGroup(group, OPT.UPDATE);
				}
				
				if(mDialog != null)
					mDialog.dismiss();
			}

		});

		mDialog.show();
	}

	private void updateGroup(ContactGroup group, OPT opt) {
		if (opt == OPT.CREATE) {
			contactService.createGroup(group, new MessageListener(mLocalHandler,
					CREATE_GROUP_DONE, null));
		} else if (opt == OPT.UPDATE) {
			contactService.updateGroup(group, new MessageListener(mLocalHandler,
					UPDATE_GROUP_DONE, null));
		} else {
			contactService.removeGroup(group, new MessageListener(mLocalHandler,
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
			if (!GlobalHolder.getInstance().isServerConnected()) {
				Toast.makeText(mContext, R.string.error_local_connect_to_server, Toast.LENGTH_SHORT).show();
				return ;
			}
			
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

	private OnItemClickListener mClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int pos,
				long id) {
			if (!GlobalHolder.getInstance().isServerConnected()) {
				Toast.makeText(mContext, R.string.error_local_connect_to_server, Toast.LENGTH_SHORT).show();
				return ;
			}
			showDialog((ContactGroup) mDataset.get(pos).getItemObject());
		}

	};

	private OnItemLongClickListener mLongClickListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			if (!inDeleteMode) {
				inDeleteMode = true;
				adapter.notifyDataSetChanged();
				return true;
			}
			return true;
		}

	};

	private CommonAdapterGetViewListener converter = new CommonAdapterGetViewListener() {

		@Override
		public View getView(CommonAdapterItemDateAndViewWrapper wr, View view,
				ViewGroup vg) {
			LocalItemWrapper liw= (LocalItemWrapper) wr;
			if (view == null) {
				view = LayoutInflater.from(mContext).inflate(
						R.layout.activity_contacts_group_adapter_item, null,
						false);
			}
			TextView tv = (TextView) view
					.findViewById(R.id.contacts_group_item_name);
			tv.setText(((Group) liw.getItemObject()).getName());
			View v = view
					.findViewById(R.id.contacts_group_item_adapter_delelte_button);
			v.setTag(wr.getItemObject());
			View deleteModeView = view
					.findViewById(R.id.contacts_group_delete_icon);
			deleteModeView.setTag(liw);
			deleteModeView.setOnClickListener(new OnClickListener () {

				@Override
				public void onClick(View v) {
					LocalItemWrapper liw = (LocalItemWrapper)v.getTag();
					if(liw.isShowDeleteButton())
						liw.setShowDeleteButton(false);
					else
						liw.setShowDeleteButton(true);
					adapter.notifyDataSetChanged();
				}
				
			});

			if (((ContactGroup) wr.getItemObject()).isDefault()) {
				v.setVisibility(View.INVISIBLE);
				deleteModeView.setVisibility(View.GONE);
			} else {
				v.setVisibility(View.VISIBLE);
				v.setOnClickListener(deleteGroupButtonClickListener);
				deleteModeView.setVisibility(View.VISIBLE);
			}

			if (inDeleteMode) {
				deleteModeView.setVisibility(View.VISIBLE);
				if (liw.showDeleteButton) {
					v.setVisibility(View.VISIBLE);
				} else {
					v.setVisibility(View.INVISIBLE);
				}
			} else {
				deleteModeView.setVisibility(View.GONE);
				v.setVisibility(View.INVISIBLE);
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
					mDataset.add(new LocalItemWrapper(g, false));
				}
				break;
			case UPDATE_GROUP_DONE:
				adapter.notifyDataSetChanged();
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

	class LocalItemWrapper implements CommonAdapterItemDateAndViewWrapper {

		private Group g;
		private boolean showDeleteButton;

		public LocalItemWrapper(Group g, boolean showDeleteButton) {
			super();
			this.g = g;
			this.showDeleteButton = showDeleteButton;
		}

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

		public Group getG() {
			return g;
		}

		public void setG(Group g) {
			this.g = g;
		}

		public boolean isShowDeleteButton() {
			return showDeleteButton;
		}

		public void setShowDeleteButton(boolean showDeleteButton) {
			this.showDeleteButton = showDeleteButton;
		}

	}

	enum OPT {
		CREATE, UPDATE, DELETE;
	}

}
