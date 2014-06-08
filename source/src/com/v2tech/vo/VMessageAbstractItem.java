package com.v2tech.vo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;

import com.v2tech.util.BitmapUtil;
import com.v2tech.util.V2Log;

public abstract class VMessageAbstractItem {

	public static final int ITEM_TYPE_TEXT = 1;

	public static final int ITEM_TYPE_IMAGE = 2;

	public static final int ITEM_TYPE_FACE = 3;

	public static final int ITEM_TYPE_AUDIO = 4;

	public static final int ITEM_TYPE_VIDEO = 5;

	protected VMessage vm;

	protected boolean isNewLine;

	protected int type;

	public VMessageAbstractItem(VMessage vm) {
		super();
		this.vm = vm;
		this.vm.addItem(this);
	}

	public VMessage getVm() {
		return vm;
	}

	public void setVm(VMessage vm) {
		this.vm = vm;
	}

	public boolean isNewLine() {
		return isNewLine;
	}

	public void setNewLine(boolean isNewLine) {
		this.isNewLine = isNewLine;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public abstract String toXmlItem();

	

}
