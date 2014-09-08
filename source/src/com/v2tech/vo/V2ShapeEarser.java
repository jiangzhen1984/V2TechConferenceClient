package com.v2tech.vo;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;

public class V2ShapeEarser extends V2Shape {

	private Path p;
	List<Float> l;

	public V2ShapeEarser() {
		p = new Path();
		l = new ArrayList<Float>();
	}

	public void addPoint(int x, int y) {
		l.add(Float.valueOf(x));
		l.add(Float.valueOf(y));
	}

	public void addLine(int x1, int y1, int x2, int y2) {
		p.moveTo(x1, y1);
		p.lineTo(x2, y2);
	}

	public void lineToLine(int x1, int y1, int x2, int y2) {
		p.lineTo(x1, y1);
		p.lineTo(x2, y2);
	}

	public void addRect(int left, int top, int right, int bottom) {
		p.addRect(left, top, right, bottom, Path.Direction.CCW);
	}

	@Override
	public void draw(Canvas canvas) {
		if (canvas == null) {
			throw new NullPointerException(" canvas is null ");
		}
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setDither(true);
		paint.setColor(0xFFFF0000);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		    paint.setStrokeCap(Paint.Cap.ROUND);
		    paint.setStrokeWidth(20);
		    paint.setXfermode(new PorterDuffXfermode(
		            PorterDuff.Mode.CLEAR));
//		// canvas.drawPath(p, paint);
		for (int i = 0; i < l.size(); i+=2) {
			Bitmap bm = Bitmap.createBitmap(12, 12, Bitmap.Config.ALPHA_8);
			bm.eraseColor(Color.TRANSPARENT);
			canvas.drawBitmap(bm, l.get(i), l.get(i+1), paint);
			bm.recycle();
		}
	//	canvas.drawPath(p, paint);
	}

}
