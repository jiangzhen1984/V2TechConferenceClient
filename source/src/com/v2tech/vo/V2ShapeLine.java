package com.v2tech.vo;

import android.graphics.Canvas;

public class V2ShapeLine extends V2Shape {

	private V2ShapePoint[] points;

	public V2ShapeLine(V2ShapePoint[] points) {
		this.points = points;
	}

	public void addPoints(V2ShapePoint point) {
		if (point == null) {
			return;
		}
		V2ShapePoint[] newPointsArr = new V2ShapePoint[points.length + 1];
		System.arraycopy(points, 0, newPointsArr, 0, points.length);
		newPointsArr[newPointsArr.length - 1] = point;
		this.points = newPointsArr;
	}

	public void addPoints(V2ShapePoint[] newPoints) {
		if (points == null) {
			return;
		}
		V2ShapePoint[] newPointsArr = new V2ShapePoint[this.points.length + points.length];
		System.arraycopy(this.points, 0, newPointsArr, 0, this.points.length);
		System.arraycopy(newPoints, 0, newPointsArr, this.points.length, newPoints.length);
		this.points = newPointsArr;
	}

	@Override
	public void draw(Canvas canvas) {
		for(V2ShapePoint p : points) {
			p.draw(canvas);
		}
	}
	
	

}
