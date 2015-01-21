package com.bizcom.util;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.v2tech.R;

/**
 * For example :
 * <pre>
 * Resources res = getResources(); 
 * mQuitDialog = DialogManager.getInstance().showQuitModeDialog(
 * 	DialogManager.getInstance().new DialogInterface(
 * 		mContext,res.getString(R.string.xxx) ,
 * 			res.getString(R.string.xxx) ,
 * 			res.getString(R.string.xxx) ,
 * 			res.getString(R.string.xxx)) {
 * 		public void confirmCallBack() {}
 * 
 * 		public void cannelCallBack() {};
 * mQuitDialog.show();
 * 
 * 如果使用该类，需要再Activity销毁时，调用clearDialogObject函数，用来解除绑定关系。
 * </pre>
 * @author
 * 
 */
public class DialogManager {

	private static DialogManager dialogManager;
	private Dialog quitDialog;

	public static synchronized DialogManager getInstance() {
		if (dialogManager == null) {
			dialogManager = new DialogManager();
		}
		return dialogManager;
	}

	private TextView dialogTitle;
	private TextView dialogContent;
	private TextView quitButtonContent;
	private TextView cannelButtonContent;

	public Dialog showQuitModeDialog(final DialogInterface inter) {
		if (quitDialog == null) {
			quitDialog = new Dialog(inter.mContext, R.style.InMeetingQuitDialog);
			quitDialog.setContentView(R.layout.in_meeting_quit_window);
			dialogTitle = (TextView) quitDialog
					.findViewById(R.id.dialog_title_content);
			dialogContent = (TextView) quitDialog
					.findViewById(R.id.in_meeting_quit_window_content);
			quitButtonContent = (TextView) quitDialog
					.findViewById(R.id.IMWQuitButton);
			cannelButtonContent = (TextView) quitDialog
					.findViewById(R.id.IMWCancelButton);
		}

		dialogTitle.setText(inter.title);
		dialogContent.setText(inter.content);
		quitButtonContent.setText(inter.quitButtonContent);
		cannelButtonContent.setText(inter.cannelButtonContent);
		cannelButtonContent.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				inter.cannelCallBack();
			}

		});

		quitButtonContent.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				inter.confirmCallBack();
			}

		});
		return quitDialog;
	}

	public void clearDialogObject() {
		quitDialog = null;
	}

	public abstract class DialogInterface {

		private Context mContext;
		private String title;
		private String content;
		private String quitButtonContent;
		private String cannelButtonContent;

		public DialogInterface(Context mContext, String title, String content,
				String quitButtonContent, String cannelButtonContent) {
			this.mContext = mContext;
			this.title = title;
			this.content = content;
			this.quitButtonContent = quitButtonContent;
			this.cannelButtonContent = cannelButtonContent;
		}

		public abstract void confirmCallBack();

		public abstract void cannelCallBack();
	}
}
