package com.v2tech.vo;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

public class V2ShapeEarser extends V2Shape {

	private Path p; 
	
	
	
	public V2ShapeEarser() {
		p = new Path();
	}
	
	
	public void addPoint(int x, int y) {
		p.moveTo(x, y);
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
		canvas.drawColor(Color.TRANSPARENT);
		canvas.drawPath(p, new Paint());
	}

}
