package com.bonozo.velo;

import java.util.Random;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class MyView extends SurfaceView implements Runnable,
		SurfaceHolder.Callback {

	public static final int PITCHER = 0;
	public static final int CATCHER = 1;

	public int pitcherStartX = -1;
	public int pitcherStartY = -1;
	public int pitcherLastX = -1;
	public int pitcherLastY = -1;

	public int catcherStartX = -1;
	public int catcherStartY = -1;
	public int catcherLastX = -1;
	public int catcherLastY = -1;

	public static double calculatedSpeed = 0;

	public int mCurrentPlayer = -1;

	public int mDebug = 0;

	public static int calculatingSpeed = 0;

	boolean drawing = false;
	public boolean canDraw = false;

	Paint pitcherPaint;
	Paint catcherPaint;

	// Logcat tag
	private static final String TAG = "MyView";

	public Mat myViewMat = new Mat();
	// private Bitmap bmp;

	private boolean stop;

	long starttime = 0;

	private MyView myThreadSurfaceView;

	private VideoPlayer mVideoPlayer;

	public Thread thread = null;
	SurfaceHolder surfaceHolder;
	volatile boolean running = false;

	private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	Random random;

	volatile boolean touched = false;
	volatile float touched_x, touched_y;

	public native void nativeCurrentMat(long mat);

	public MyView(Context context, VideoPlayer videoPlayer, AttributeSet attrs) {
		super(context, attrs);

		mVideoPlayer = videoPlayer;

		this.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				if (!canDraw)
					return true;

				switch (mCurrentPlayer) {
				case PITCHER:
					drawPitcher(event);
					// mCurrentPlayer = CATCHER;
					break;
				case CATCHER:
					drawCatcher(event);
					// mCurrentPlayer = PITCHER;
					break;
				}

				return true;
			}

		});
		init();

		// TODO Auto-generated constructor stub
		surfaceHolder = this.getHolder();

		thread = new Thread(this);
		getHolder().addCallback(this);

		random = new Random();

		// nativeCurrentMat(myViewMat.getNativeObjAddr());

		running = true;
	}

	public MyView(Context context, VideoPlayer videoPlayer) {
		super(context);

		mVideoPlayer = videoPlayer;

		this.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				switch (mCurrentPlayer) {
				case PITCHER:
					drawPitcher(event);
					// mCurrentPlayer = CATCHER;
					break;
				case CATCHER:
					drawCatcher(event);
					// mCurrentPlayer = PITCHER;
					break;
				}

				return true;
			}

		});
		init();

		// TODO Auto-generated constructor stub
		surfaceHolder = this.getHolder();

		thread = new Thread(this);
		getHolder().addCallback(this);

		// nativeCurrentMat(myViewMat.getNativeObjAddr());

		random = new Random();
	}

	public void stopIt() {
		System.out.println("Stopped thread");
		running = false;
		calculatingSpeed = 0;
		try {
			if (myViewMat != null)
				myViewMat.release();
			myViewMat = null;
		} catch (Exception e) {
			Log.e(TAG, "Jumped myViewMat");
		}

	}

	private void drawPitcher(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN && drawing == false) {
			pitcherStartX = (int) event.getX();
			pitcherStartY = (int) event.getY();

			pitcherLastX = pitcherStartX;
			pitcherLastY = pitcherStartY;
			drawing = true;
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			drawing = false;
			canDraw = false;
			mCurrentPlayer = -1;

			mVideoPlayer.mHideContainer.setVisibility(View.VISIBLE);
		} else {
			pitcherLastX = (int) event.getX();
			pitcherLastY = (int) event.getY();
		}
	}

	private void drawCatcher(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN && drawing == false) {
			catcherStartX = (int) event.getX();
			catcherStartY = (int) event.getY();

			catcherLastX = catcherStartX;
			catcherLastY = catcherStartY;
			drawing = true;
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			drawing = false;
			canDraw = false;
			mCurrentPlayer = -1;

			mVideoPlayer.mHideContainer.setVisibility(View.VISIBLE);
		} else {
			catcherLastX = (int) event.getX();
			catcherLastY = (int) event.getY();
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		// super.onDraw(canvas);

		try {
			/*
			 * if (myViewMat.height() > 0) { Bitmap bmp =
			 * Bitmap.createBitmap(myViewMat.cols(), myViewMat.rows(),
			 * Bitmap.Config.ARGB_8888);
			 * 
			 * try { Utils.matToBitmap(myViewMat, bmp); } catch (Exception e) {
			 * Log.e(TAG, "Utils.matToBitmap() throws an exception: " +
			 * e.getMessage()); bmp.recycle(); bmp = null; }
			 */

			// canvas = surfaceHolder.lockCanvas();

			if (canvas != null) {

				// try {

				canvas.drawColor(0, Mode.CLEAR);

				// Log.i(TAG, "Drawing mat");
				/*
				 * canvas.drawBitmap(bmp, (canvas.getWidth() - bmp.getWidth()) /
				 * 2, (canvas.getHeight() - bmp.getHeight()) / 2, null);
				 */

				if (pitcherStartX != -1 && pitcherStartY != -1
						&& pitcherLastX != -1 && pitcherLastY != -1) {
					canvas.drawRect(pitcherStartX, pitcherStartY, pitcherLastX,
							pitcherLastY, pitcherPaint);
				}

				if (catcherStartX != -1 && catcherStartY != -1
						&& catcherLastX != -1 && catcherLastY != -1) {
					canvas.drawRect(catcherStartX, catcherStartY, catcherLastX,
							catcherLastY, catcherPaint);
				}

				// invalidate();
				// Log.i(TAG, "Got inside past");

				// } catch (Exception e) {
				// Log.e(TAG, "Jumped");
				// }

				// surfaceHolder.unlockCanvasAndPost(canvas);

				// Thread.sleep(500);

				/*
				 * try { bmp.recycle();
				 * 
				 * bmp = null; } catch (Exception e) { }
				 */

				// }

			} else {
				init();
				// Log.i(TAG, "Failed to get mat");
			}
		} catch (Exception e) {
			init();
			Log.i(TAG, "Failed miserably");
		}

	}

	private void init() {
		try {
			nativeCurrentMat(myViewMat.getNativeObjAddr());
		} catch (Exception e) {
		}
		pitcherPaint = new Paint();
		pitcherPaint.setColor(Color.RED);
		pitcherPaint.setStrokeWidth(2);
		pitcherPaint.setStyle(Paint.Style.STROKE);

		catcherPaint = new Paint();
		catcherPaint.setColor(Color.BLUE);
		catcherPaint.setStrokeWidth(2);
		catcherPaint.setStyle(Paint.Style.STROKE);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (running) {
			if (surfaceHolder.getSurface().isValid()) {

				Canvas c = null;
				try {
					c = surfaceHolder.lockCanvas(null);
					synchronized (surfaceHolder) {
						this.onDraw(c);
					}

				} finally {
					// do this in a finally so that if an exception is thrown
					// during the above, we don't leave the Surface in an
					// inconsistent state
					if (c != null) {
						surfaceHolder.unlockCanvasAndPost(c);
						this.postInvalidate();
					}
				}
			}
		}
	}

	/*
	 * @Override public boolean onTouchEvent(MotionEvent event) { // TODO
	 * Auto-generated method stub
	 * 
	 * touched_x = event.getX(); touched_y = event.getY();
	 * 
	 * int action = event.getAction(); switch (action) { case
	 * MotionEvent.ACTION_DOWN: touched = true; break; case
	 * MotionEvent.ACTION_MOVE: touched = true; break; case
	 * MotionEvent.ACTION_UP: touched = false; break; case
	 * MotionEvent.ACTION_CANCEL: touched = false; break; case
	 * MotionEvent.ACTION_OUTSIDE: touched = false; break; default: }
	 * 
	 * // nativeCurrentMat(myViewMat.getNativeObjAddr());
	 * 
	 * this.bringToFront();
	 * 
	 * running = true; return true; // processed }
	 */

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		running = true;

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		init();

		this.setWillNotDraw(false);

		running = true;

		thread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		stopIt();
	}

}