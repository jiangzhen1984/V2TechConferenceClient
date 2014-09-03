package com.v2tech.view.contacts;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
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
import com.v2tech.view.widget.CommonAdapter;
import com.v2tech.view.widget.CommonAdapter.CommonAdapterItemWrapper;
import com.v2tech.view.widget.CommonAdapter.ViewConvertListener;
import com.v2tech.vo.ContactGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;

public class ContactsGroupActivity extends Activity {

	private TextView mDialogTitleTV;
	private EditText mGroupNameET;
	private Dialog mDialog;
	private Context mContext;
	private ListView mListView;

	private List<CommonAdapterItemWrapper> mDataset;
	private BaseAdapter adapter;

	private ContactsService contactService = new ContactsService();

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
				GroupType.CONTACT);
		mDataset = convert(listGroup);
		adapter = new CommonAdapter(mDataset, converter);
		mListView.setAdapter(adapter);
		mListView.setOnItemLongClickListener(mLongClickListener);
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
			final Button cancelB = (Button) mDialog
					.findViewById(R.id.contacts_group_cancel_button);
			cancelB.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mDialog.dismiss();
				}

			});
			final Button confirmButton = (Button) mDialog
					.findViewById(R.id.contacts_group_confirm_button);
			confirmButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mGroupNameET.getText().toString().isEmpty()) {
						mGroupNameET.setError(mContext
								.getText(R.string.activiy_contact_group_dialog_group_name_required));
						return;
					}
					ContactGroup newGroup = new ContactGroup(0, mGroupNameET
							.getText().toString());
					updateGroup(newGroup, group == null ? OPT.CREATE
							: OPT.UPDATE);
				}

			});

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
			mGroupNameET.setText(R.string.activiy_contact_group_name_content);
			mDialogTitleTV
					.setText(R.string.activiy_contact_group_dialog_title_create);
		}

		mDialog.show();
	}

	private void updateGroup(ContactGroup group, OPT opt) {
		if (opt == OPT.CREATE) {
			contactService.createGroup(group, null);
		} else if (opt == OPT.UPDATE) {
			contactService.updateGroup(group, null);
		} else {
			contactService.removeGroup(group, null);
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
			return view;
		}

	};

	enum OPT {
		CREATE, UPDATE, DELETE;
	}

}
