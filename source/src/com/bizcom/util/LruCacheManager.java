package com.bizcom.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.jakewharton.disklrucache.DiskLruCache;

import android.content.Context;

/**
 * Created by zhaoyu on 2014/9/26.
 */

public class LruCacheManager {
	// LruCache 内存缓存
	// DiskLruCache 磁盘缓存
	private static final String CACHE_FOLDER = "long_image_cache";
	// Default disk cache size
	private static final long DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 10;
	private int APP_VERSION = 1;
	private int VALUE_COUNT = 1;

	private DiskLruCache cache;
	private Context context;

	private LruCacheManager(Context c) {
		this.context = c.getApplicationContext();
		try {
			cache = DiskLruCache.open(SubsamplingScaleImageUtils
					.getDiskCacheDir(context, CACHE_FOLDER), APP_VERSION,
					VALUE_COUNT, DEFAULT_DISK_CACHE_SIZE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static LruCacheManager cacheManager;

	public static synchronized LruCacheManager getInstance(Context context) {
		if (cacheManager == null)
			cacheManager = new LruCacheManager(context);
		return cacheManager;
	}

	public InputStream saveCache(String key, InputStream inputStream) {
		if (inputStream == null)
			return null;
		DiskLruCache.Editor editor = null;
		OutputStream ops = null;
		try {
			editor = cache.edit(key);
			if (editor == null)
				return inputStream;
			ops = editor.newOutputStream(0);
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			byte[] buffer = new byte[4 * 1024];
			int numread;
			while ((numread = inputStream.read(buffer)) != -1) {
				os.write(buffer, 0, numread);
				ops.write(buffer, 0, numread);
			}
			InputStream is2 = new ByteArrayInputStream(os.toByteArray());
			editor.commit();
			return is2;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public InputStream get(String key) {
		DiskLruCache.Snapshot snapshot;
		try {
			snapshot = cache.get(key);
			if (snapshot == null)
				return null;
			return snapshot.getInputStream(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void clearCache() {
		try {
			cache.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
