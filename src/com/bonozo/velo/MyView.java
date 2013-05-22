package com.bonozo.velo;

import java.util.Random;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.Typeface;
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

	public int regionStartX = 0;
	public int regionStartY = 0;
	public int regionLastX = 10;
	public int regionLastY = 10;

	public static int point1X = 0;
	public static int point1Y = 0;
	public static int point2X = 0;
	public static int point2Y = 0;

	public int mWidth = 0;
	public int mHeight = 0;

	public static double calculatedSpeed = 0;
	
	public int fieldSize = 60;

	public static String points = "";

	public int mCurrentPlayer = -1;

	public int mDebug = 0;

	public static int calculatingSpeed = 0;

	boolean drawing = false;
	public boolean canDraw = false;

	Paint pitcherPaint;
	Paint catcherPaint;
	Paint roiPaint;
	Paint circlePaint;
	Paint pathPaint;
	Paint fontPaint;
	Paint trajectoryPaint;

	// Logcat tag
	private static final String TAG = "MyView";

	public Mat myViewMat = new Mat();
	// private Bitmap bmp;

	private boolean stop;

	long starttime = 0;

	private MyView myThreadSurfaceView;

	public VideoPlayer mVideoPlayer;

	public Thread thread = null;
	SurfaceHolder surfaceHolder;
	volatile boolean running = false;

	private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	Random random;

	volatile boolean touched = false;
	volatile float touched_x, touched_y;

	public native void nativeCurrentMat(long mat);

	private Context mContext;

	private Point coords[], xcoords[];

	private double vA = 0.0, vB = 0.0, vC = 0.0;

	private int reDraw = 0;

	Path mPat;

	public boolean isDirty = false;

	public Point center1, center2;

	public void setPoints(int x[], int y[]) {
		Log.i("setPoints CALLED", "Found " + x.length + " x");
		if (x.length > 0 && y.length > 0) {
			Point points[] = new Point[x.length];

			for (int i = 0; i < x.length && i < y.length; i++) {
				points[i] = new Point();

				points[i].x = (int) x[i]; // + regionStartX;
				points[i].y = (int) y[i]; // + regionStartY;

				Log.i("setPoints CALLED", "Got x as: " + x[i] + "; y as: "
						+ y[i]);
			}

			coords = points;
		}
	}

	public void setValues(double x[], double y[], double yX[], double a,
			double b, double c) {
		Log.i(TAG, "@@@@@@@@@@@@@@@@@@@@@@@@ x: " + x.length + "; y: "
				+ y.length + "; yx: " + yX.length + "; a: " + a + "; b: " + b
				+ "; c: " + c);

		if (x.length > 0 && y.length > 0) {
			Point points[] = new Point[x.length];
			Point xpoints[] = new Point[yX.length];

			vA = a;
			vB = b;
			vC = c;

			for (int i = 0; i < x.length && i < y.length; i++) {
				points[i] = new Point();

				points[i].x = (int) x[i] + regionStartX;
				points[i].y = (int) y[i] + regionStartY;

			}

			for (int i = 0; i < yX.length && i < x.length; i++) {
				try {
					xpoints[i].x = (int) x[i] + regionStartX;
					xpoints[i].y = (int) yX[i] + regionStartY;
				} catch (Exception e) {
				}
			}

			coords = points;
			xcoords = xpoints;
		}
	}

	public double findY(double a, double b, double c, double x) {
		return a * x * x + b * x + c;
	}

	public MyView(Context context, VideoPlayer videoPlayer, AttributeSet attrs) {
		super(context, attrs);

		mContext = context;

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
		points = "";
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
			isDirty = true;

			mVideoPlayer.mHideContainer.setVisibility(View.VISIBLE);
		} else {
			pitcherLastX = (int) event.getX();
			pitcherLastY = (int) event.getY();

			if (pitcherLastX < catcherStartX) {
				regionStartX = pitcherLastX + 10;
				regionLastX = catcherStartX - 10;
			} else {
				regionStartX = catcherLastX + 10;
				regionLastX = pitcherStartX - 10;
			}

			int height = pitcherLastY - pitcherStartY;

			regionStartY = 20;

			regionLastY = pitcherLastY;

		}

		center1.x = pitcherStartX + ((pitcherLastX - pitcherStartX) / 2);
		center1.y = pitcherStartY + ((pitcherLastY - pitcherStartY) / 9);

	}

	private void drawFixed(Canvas canvas) {
		// Canvas canvas = mFixedStuff
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

		center2.x = catcherStartX + ((catcherLastX - catcherStartX) / 2);
		center2.y = catcherStartY + ((catcherLastY - catcherStartY) / 2);

	}

	@Override
	protected void onDraw(Canvas canvas) {

		try {

			if (canvas != null) {

				mWidth = canvas.getWidth();
				mHeight = canvas.getHeight();

				canvas.drawColor(0, Mode.CLEAR);

				if (points.trim().length() > 0) {
					String[] pointlist = points.split("\\|");

					for (int i = 0; i < pointlist.length; i++) {
						String[] item = pointlist[i].trim().split(";");

						canvas.drawText(item[2], Float.parseFloat(item[0])
								+ regionStartX, Float.parseFloat(item[1])
								+ regionStartY, fontPaint);
					}
				}

				if (calculatingSpeed <= 0) {
					if (pitcherStartX != -1 && pitcherStartY != -1
							&& pitcherLastX != -1 && pitcherLastY != -1) {
						canvas.drawRect(pitcherStartX, pitcherStartY,
								pitcherLastX, pitcherLastY, pitcherPaint);

					}

					if (catcherStartX != -1 && catcherStartY != -1
							&& catcherLastX != -1 && catcherLastY != -1) {
						canvas.drawRect(catcherStartX, catcherStartY,
								catcherLastX, catcherLastY, catcherPaint);
					}
				} else {
					if (pitcherStartX != -1 && pitcherStartY != -1
							&& pitcherLastX != -1 && pitcherLastY != -1) {
						canvas.drawRect(pitcherStartX, pitcherStartY,
								pitcherLastX, pitcherLastY, pitcherPaint);

					}

					if (catcherStartX != -1 && catcherStartY != -1
							&& catcherLastX != -1 && catcherLastY != -1) {
						canvas.drawRect(catcherStartX, catcherStartY,
								catcherLastX, catcherLastY, catcherPaint);
					}

					if (regionStartX != -1 && regionStartY != -1
							&& regionLastX != -1 && regionLastY != -1) {
						canvas.drawRect(regionStartX, regionStartY,
								regionLastX, regionLastY, roiPaint);
					}

					if (point1X > 0 && point1Y > 0) {

						canvas.drawRect(point1X + regionStartX, point1Y
								+ regionStartY, point1X + regionStartX + 15,
								point1Y + regionStartY + 15, roiPaint);

					}

					if (point2X > 0 && point2Y > 0) {

						canvas.drawRect(point2X + regionStartX, point2Y
								+ regionStartY, point2X + regionStartX + 15,
								point2Y + regionStartY + 15, roiPaint);

						canvas.drawCircle((float) (point2X + regionStartX),
								(float) (point2Y + regionLastY), 10, roiPaint);
					}

					float aP = 0.0002f, bP = 1.0f, cP = 0.0f;

					if (center1.x > 10 && center2.x > 10) {

						/*canvas.drawLine((float) center1.x, (float) center1.y,
								(float) center2.x, (float) center2.y,
								trajectoryPaint);*/

						float d = (float) (center2.x - center1.x)
								/ (float) mWidth;

						float xA = (float) center1.x, yA = (float) center1.y;	// pitcher
						float xB = (float) center2.x, yB = (float) center2.y;	// catcher

						if (xA > xB) {

							d = (float) (center1.x - center2.x)
									/ (float) mWidth;

							aP = 0.0002f / d;

							bP = (yA - (aP * xA * xA) + (aP * xB * xB) - yB)
									/ (xA - xB);

							cP = yB - (aP * xB * xB) - (bP * xB);

							float s = (xA - xB) / 10.0f;

							for (float x = xB; x < xA; x += s) {

								float y = (aP * x * x) + (bP * x) + cP;
								float y2 = (aP * (x + s) * (x + s))
										+ (bP * (x + s)) + cP;

								/*canvas.drawText("(" + x + "," + y + ")", x
										+ (s / 2f), y + ((y2 - y) / 2f),
										fontPaint);*/

								canvas.drawLine(x, y, x + s, y2,
										trajectoryPaint);
							}

						} else {

							aP = 0.0002f / d;

							bP = (yA - (aP * xA * xA) + (aP * xB * xB) - yB)
									/ (xA - xB);

							cP = yB - (aP * xB * xB) - (bP * xB);

							float s = (xB - xA) / 10.0f;

							for (float x = xA; x < xB; x += s) {

								float y = (aP * x * x) + (bP * x) + cP;
								float y2 = (aP * (x + s) * (x + s))
										+ (bP * (x + s)) + cP;

								canvas.drawLine(x, y, x + s, y2,
										trajectoryPaint);
							}
						}

					}

				}

				if (coords.length > 0) {

					float radius = 5;

					for (int c = 0; c < coords.length; c++) {

						canvas.drawCircle((float) coords[c].x,
								(float) coords[c].y, radius, circlePaint);

					}
				}

			}

		} catch (Exception e) {
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

		roiPaint = new Paint();
		roiPaint.setColor(Color.GREEN);
		roiPaint.setStrokeWidth(2);
		roiPaint.setStyle(Paint.Style.STROKE);
		// roiPaint.setPathEffect(new DashPathEffect(new float[] {10,10}, 5));

		pathPaint = new Paint();
		pathPaint.setColor(Color.MAGENTA);
		pathPaint.setStrokeWidth(2);
		pathPaint.setStyle(Paint.Style.STROKE);
		pathPaint.setPathEffect(new DashPathEffect(new float[] { 10, 10 }, 2));

		trajectoryPaint = new Paint();
		trajectoryPaint.setColor(Color.WHITE);
		trajectoryPaint.setStrokeWidth(2);
		trajectoryPaint.setStyle(Paint.Style.STROKE);
		trajectoryPaint.setPathEffect(new DashPathEffect(
				new float[] { 10, 10 }, 2));

		circlePaint = new Paint();
		circlePaint.setColor(Color.GREEN);
		circlePaint.setStrokeWidth(2);
		circlePaint.setStyle(Paint.Style.STROKE);
		// circlePaint.setTextSize(15);

		fontPaint = new Paint();
		fontPaint.setColor(Color.MAGENTA);
		// fontPaint.setStrokeWidth(1);
		fontPaint.setAntiAlias(true);
		// fontPaint.setStyle(Paint.Style.FILL);
		fontPaint.setTextSize(18);
		fontPaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.ITALIC));

		mPat = new Path();

		center1 = new Point(0, 0);
		center2 = new Point(0, 0);
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