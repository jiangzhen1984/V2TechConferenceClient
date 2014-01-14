package v2av;

import v2av.VideoPlayer;
//import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class VideoTouchListener implements OnTouchListener
{

	private float x1 = 0,y1 = 0,x2 = 0,y2 = 0;
	private double lastLength = 0;
	
	private boolean mbClicked = true;   //
	
	private VideoPlayer mVideoPlayer = null;
	
	public VideoTouchListener(VideoPlayer vp)
	{
		mVideoPlayer = vp;
	}
	
	public void setVideoPlayer(VideoPlayer vp)
	{
		mVideoPlayer = vp;
	}
	
	//计算两点间的距离
	private double Get2PointLength(float x1, float y1, float x2, float y2)
	{
		double x = x1 * x1 + x2 * x2 - 2 * x1 * x2;
		double y = y1 * y1 + y2 * y2 - 2 * y1 * y2;
		return Math.sqrt(x + y);
	}
	
	public boolean onTouch(View v, MotionEvent event) 
	{
		try
		{
			switch (event.getAction()) 
			{
				case MotionEvent.ACTION_DOWN:
					//保存
					x2 = event.getX(0);
					y2 = event.getY(0);
					lastLength  = 0;
					mbClicked = true;
					break;
				case MotionEvent.ACTION_MOVE:
				{
				//	Log.v("touch point","action move..........");
					x1 = event.getX(0);
					y1 = event.getY(0);
					//只有一个触点时移动图片
					if(event.getPointerCount() == 1)
					{
						if(x2 == -1)
						{
							x2 = x1;
							y2 = y1;
						}
						if(Math.abs((x1 - x2)) > 5 || Math.abs((y1 - y2)) > 5)
						{
							mbClicked = false;
							//从x2移动x1
							mVideoPlayer.translate(x1 - x2, y1 - y2);        //移动x1-x2的距离
							x2 = x1;
							y2 = y1;
						}
						lastLength = 0;
						return true;
					}
					mbClicked = false;
					//两个点缩放
					x2 = event.getX(1);
					y2 = event.getY(1);
					double tempLength = Get2PointLength(x1,y1,x2,y2);
					if(lastLength == 0)
					{
						lastLength = tempLength;
						return true;
					}
					if(tempLength > lastLength)
					{
						mVideoPlayer.zoomIn();
					}
					else
					{
						mVideoPlayer.zoomOut();
					}
					lastLength = tempLength;
					x2 = -1;
					y2 = -1;
				}
				break;
				case MotionEvent.ACTION_UP:
				//	Log.v("touch point","action up..........");
					lastLength  = 0;
					//调用这个控件的单击事件
					if(mbClicked)
					{
						v.performClick();       //自动点击
					}
					else
					{
						//这里只是为了使图像居中
						float scale = mVideoPlayer.getScale();
						if (scale != 0.0f)
						{
							mVideoPlayer.zoomTo(mVideoPlayer.getScale(), 0, 0, 1);
						}	
					}
					break;
				default:
					break;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return true;		//true已经处理了,不交给click,long click 处理
	}

}
