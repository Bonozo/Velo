package com.bonozo.velo;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TableLayout;
import android.widget.TextView;

public class VideoPlayer extends Activity {

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onBackPressed() {

		drawView.stopIt();

		nativePointReset();

		mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
		seekBarUpdater.stopIt();
		demoRenderer.exitApp();
	}

	@Override
	protected void onStop() {
		if (wakeLock != null) {
			wakeLock.release();
			wakeLock = null;
		}

		super.onStop();
	}

	PhoneStateListener phoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			if (state == TelephonyManager.CALL_STATE_RINGING) {
				// Incoming call: Pause music
				System.out.println("Video call state ringing");
				// Pause the video, only if video is playing
				if ((demoRenderer != null) && (!paused)) {
					System.out.println("Triggered");
					demoRenderer.nativePlayerPlay();
				}

				// seekBarUpdater = new Updater();
				// mHandler.postDelayed(seekBarUpdater, 500);
			} else if (state == TelephonyManager.CALL_STATE_IDLE) {
				// Not in call: Play music
				System.out.println("Video call state idle");
				// do not resume, if already paused by User
				if ((demoRenderer != null) && (!paused)) {
					System.out.println("Triggered");
					demoRenderer.nativePlayerPause();
				}
				// seekBarUpdater.stopIt();
			} else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
				// A call is dialing, active or on hold
				System.out.println("Video call state offhook");

				if ((demoRenderer != null) && (!paused)) {
					System.out.println("Triggered");
					demoRenderer.nativePlayerPlay();
				}
				// seekBarUpdater = new Updater();
				// mHandler.postDelayed(seekBarUpdater, 500);
			}
			super.onCallStateChanged(state, incomingNumber);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Globals.setNativeVideoPlayer(false);
		System.out.println("VideoPlayer onCreate");
		paused = false;

		// fullscreen mode
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

		// Window win = getWindow();
		// WindowManager.LayoutParams winParams = win.getAttributes();
		// winParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		// win.setAttributes(winParams);

		// setContentView(R.layout.video_player);

		setContentView(R.layout.video_player);

		// Utils.hideSystemUi(getWindow().getDecorView());
		// Utils.hideSystemUi(this.findViewById(R.id.glsurfaceview).getRootView());

		i = getIntent();

		if (i != null) {
			Uri uri = i.getData();
			if (uri != null) {
				openfileFromBrowser = uri.getEncodedPath();

				// Change from 1.6
				String decodedOpenFileFromBrowser = null;
				try {
					decodedOpenFileFromBrowser = URLDecoder.decode(
							openfileFromBrowser, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				}
				if (decodedOpenFileFromBrowser != null) {
					openfileFromBrowser = decodedOpenFileFromBrowser;
				}
			}
		}
		System.out.println("openfileFromBrowser:" + openfileFromBrowser);

		if (FileManager.isVideoFile(openfileFromBrowser)) {
			Globals.setFileName(openfileFromBrowser);
			System.out.println("================openfileFromBrowser:"
					+ openfileFromBrowser + "=============");

		} else {
			Bundle extras = i.getExtras();
			if (extras != null) {
				String tmpFileName = extras.getString("videofilename");

				if (FileManager.isVideoFile(tmpFileName)) {
					Globals.setFileName(tmpFileName);
					System.out
							.println("================extras.getString videofilename:"
									+ tmpFileName + "============");
				}
			}
		}

		System.out.println("=======================Playing filename:"
				+ Globals.fileName);

		mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		System.out.println("TelephoneManager : " + mgr);
		if (mgr != null) {
			System.out.println("telephonemanager start");
			mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

		}

		// Find the views whose visibility will change
		/*
		 * mSeekBar = (SeekBar) findViewById(R.id.progressbar);
		 * 
		 * currentTime = (TextView) findViewById(R.id.currenttime); totalTime =
		 * (TextView) findViewById(R.id.totaltime); controlPanel = (TableLayout)
		 * findViewById(R.id.controlPanel);
		 * controlPanel.getBackground().setAlpha(85);
		 * 
		 * imgPlay = findViewById(R.id.img_vp_play); imgForward =
		 * findViewById(R.id.img_vp_forward); imgBackward =
		 * findViewById(R.id.img_vp_backward); imgAspectRatio =
		 * findViewById(R.id.fs_shadow);
		 * 
		 * //trScrolledTime = findViewById(R.id.trscrolledtime); //scrolledtime
		 * = (TextView) findViewById(R.id.scrolledtime);
		 * 
		 * //trScrolledTime.setVisibility(View.INVISIBLE);
		 * //trScrolledTime.setVisibility(View.GONE); mHideContainer =
		 * findViewById(R.id.hidecontainer);
		 * mHideContainer.setOnClickListener(mVisibleListener);
		 * 
		 * mControlPanelContainer = findViewById(R.id.controlPanel);
		 * mControlPanelContainer.setOnClickListener(mControlPanelListener);
		 * 
		 * imgAspectRatio.setOnTouchListener(imgAspectRatioTouchListener);
		 * imgPlay.setOnTouchListener(imgPlayTouchListener);
		 * imgForward.setOnTouchListener(imgForwardTouchListener);
		 * imgBackward.setOnTouchListener(imgBackwardTouchListener);
		 * mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
		 */

		// mFixedStuff = (SurfaceView) findViewById(R.id.fixedStuff);

		// Find the views whose visibility will change
		mSeekBar = (SeekBar) findViewById(R.id.progressbar);

		currentTime = (TextView) findViewById(R.id.currenttime);
		totalTime = (TextView) findViewById(R.id.totaltime);
		controlPanel = (TableLayout) findViewById(R.id.controlPanel);
		controlPanel.getBackground().setAlpha(85);

		imgPlay = findViewById(R.id.img_vp_play);
		imgForward = findViewById(R.id.img_vp_forward);
		imgBackward = findViewById(R.id.img_vp_backward);
		// imgAspectRatio = findViewById(R.id.fs_shadow);

		// imgPrev = findViewById(R.id.img_backward);
		// imgNext = findViewById(R.id.img_forward);
		// imgStop = findViewById(R.id.img_vp_play);
		imgHelp = findViewById(R.id.img_vp_help);

		// imgForward = findViewById(R.id.img_vp_forward);
		// imgBackward = findViewById(R.id.img_vp_backward);

		imgLogo = findViewById(R.id.calculateSpeed);

		imgText = (TextView) findViewById(R.id.currentspeed);

		imgPoints = (TextView) findViewById(R.id.currentpoints);

		mProgressBar = (ProgressBar) findViewById(R.id.progress);

		// trScrolledTime = findViewById(R.id.trscrolledtime);
		// scrolledtime = (TextView) findViewById(R.id.scrolledtime);

		// trScrolledTime.setVisibility(View.INVISIBLE);
		// trScrolledTime.setVisibility(View.GONE);
		mHideContainer = findViewById(R.id.hidecontainer);
		mHideContainer.setOnClickListener(mVisibleListener);
		// mHideContainer.setVisibility(View.GONE);

		mControlPanelContainer = findViewById(R.id.controlPanel);
		mControlPanelContainer.setOnClickListener(mControlPanelListener);

		imgHelp.setOnTouchListener(imgHelpTouchListener);
		imgPlay.setOnTouchListener(imgPlayTouchListener);
		imgBackward.setOnTouchListener(imgPrevTouchListener);
		imgForward.setOnTouchListener(imgNextTouchListener);

		// imgForward.setOnTouchListener(imgForwardTouchListener);
		// imgBackward.setOnTouchListener(imgBackwardTouchListener);

		imgLogo.setOnTouchListener(imgCalculateSpeedTouchListener);

		mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);

		/*
		 * imgAspectRatio.setOnTouchListener(imgAspectRatioTouchListener);
		 * imgPlay.setOnTouchListener(imgPlayTouchListener);
		 * imgForward.setOnTouchListener(imgForwardTouchListener);
		 * imgBackward.setOnTouchListener(imgBackwardTouchListener);
		 * mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
		 */

		// System.out.println("Start - InitSDL()");

		// OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
		// mLoaderCallback);

		initSDL();

	}

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");

				initSDL();

				FrameLayout parent = (FrameLayout) findViewById(R.id.ovelaycontainer);

				drawView = new MyView(VideoPlayer.this, VideoPlayer.this);

				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.FILL_PARENT,
						FrameLayout.LayoutParams.FILL_PARENT);

				drawView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

				parent.addView(drawView, 0, params);

				drawView.setFocusable(true);
				drawView.requestFocus();

				// mProgressBar.setVisibility(View.VISIBLE);

				drawView.calculatedSpeed = 0;

				Handler myHandler = new stopPlay();
				Message m = new Message();
				// m.obj =
				// "$$$$$$$$$$$$$$$$$$$$$$$4 - Test - $$$$$$$$$$$$$$$$$$$$$$$$$$$4";//
				// passing
				// a
				// parameter
				// here
				myHandler.sendMessageDelayed(m, 10000);

				current_position = 1;

				stageMsgDisplayed = false;

				imgText.setText("-- MPH");

				// mOpenCvCameraView.enableView();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public void initSDL() {
		// Wake lock code
		try {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			// wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
			// Globals.ApplicationName);
			wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,
					Globals.ApplicationName);

			wakeLock.acquire();
		} catch (Exception e) {
			System.out.println("Inside wake lock exception" + e.toString());
		}
		System.out.println("Acquired wakeup lock");

		// OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
		// mLoaderCallback);

		// Native libraries loading code
		Globals.LoadNativeLibraries();
		System.out.println("native libraries loaded");

		// Audio thread initializer
		mAudioThread = new AudioThread(this);
		System.out.println("Audio thread initialized");

		GLSurfaceView_SDL surfaceView = (GLSurfaceView_SDL) findViewById(R.id.glsurfaceview);
		System.out.println("got the surface view:");

		surfaceView.setOnClickListener(mGoneListener);

		DemoRenderer demoRenderer = new DemoRenderer(this);
		this.demoRenderer = demoRenderer;

		// surfaceView.setRenderer(null);

		surfaceView.setRenderer(demoRenderer);

		System.out.println("Set the surface view renderer");

		SurfaceHolder holder = surfaceView.getHolder();
		holder.addCallback(surfaceView);
		System.out.println("Added the holder callback");
		holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
		System.out.println("Hold type set");

		surfaceView.setFocusable(true);
		surfaceView.requestFocus();

		totalDuration = demoRenderer.nativePlayerTotalDuration();
		// totalTime.setText(Utils.formatTime(totalDuration));

		FrameLayout parent = (FrameLayout) findViewById(R.id.ovelaycontainer);

		drawView = new MyView(VideoPlayer.this, VideoPlayer.this);

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.FILL_PARENT,
				FrameLayout.LayoutParams.FILL_PARENT);

		drawView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

		parent.addView(drawView, 0, params);

		drawView.setFocusable(true);
		drawView.requestFocus();

		// mProgressBar.setVisibility(View.VISIBLE);

		drawView.calculatedSpeed = 0;

		Handler myHandler = new stopPlay();
		Message m = new Message();
		// m.obj =
		// "$$$$$$$$$$$$$$$$$$$$$$$4 - Test - $$$$$$$$$$$$$$$$$$$$$$$$$$$4";//
		// passing
		// a
		// parameter
		// here
		myHandler.sendMessageDelayed(m, 20000);

		current_position = 1;

		stageMsgDisplayed = false;

		imgText.setText("-- MPH");

		mHandler.postDelayed(seekBarUpdater, 100);

		// Hide ICS System bar/Navigation bar
		// Window win = getWindow();
		// WindowManager.LayoutParams winParams = win.getAttributes();
		// winParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		// win.setAttributes(winParams);
		//
		// Utils.hideSystemUi(surfaceView);
		// Utils.hideSystemUi(this.findViewById(R.id.glsurfaceview).getRootView());

		// Utils.hideSystemUi(getWindow().getDecorView());

	}

	public void restartUpdater() {
		seekBarUpdater.stopIt();
		seekBarUpdater = new Updater();
		mHandler.postDelayed(seekBarUpdater, 100);
	}

	private class Updater implements Runnable {
		private boolean stop;

		public void stopIt() {
			System.out.println("Stopped updater");
			stop = true;
		}

		@Override
		public void run() {

			if (tracking && trackingCount < 1) {
				trackingCount++;

				// show("Tracking: " + trackingCount + "!");
			} else if (!calculatingSpeed) {
				tracking = false;
				trackingCount = 0;

				if (!paused)
					clickButton(imgPlay);

				imgBackward.setVisibility(View.VISIBLE);

				imgForward.setVisibility(View.VISIBLE);

				mProgressBar.setVisibility(View.GONE);

				if (current_position == 1 && !stageMsgDisplayed) {
					show(msg1);
					stageMsgDisplayed = true;
				}
			}

			if (drawView.calculatedSpeed > 0 && !stageMsgDisplayed) {
				speedCalculated(drawView.calculatedSpeed);
			}

			if (drawView.points.trim().length() > 0) {
				// displayPoints(drawView.points.trim());
			}

			// Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			if (currentTime != null && demoRenderer != null) {
				long playedDuration = demoRenderer.nativePlayerDuration();
				currentTime.setText(Utils.formatTime(playedDuration));
				totalDuration = demoRenderer.nativePlayerTotalDuration();
				if (totalDuration != 0) {
					int progress = (int) ((1000 * playedDuration) / totalDuration);
					mSeekBar.setProgress(progress);
					totalTime.setText(Utils.formatTime(totalDuration));
				}
				if (demoRenderer.fileInfoUpdated) {
					// if (Globals.fileName != null) {
					// videoInfo.setText(FileManager.getFileName(Globals.fileName));
					// }
					demoRenderer.fileInfoUpdated = false;
				}
			}

			if (!stop) {
				if (Globals.fileName != null) {
					// Restart the updater if file is still playing
					mHandler.postDelayed(seekBarUpdater, 500);
				}
			}
		}
	}

	OnClickListener mGoneListener = new OnClickListener() {
		public void onClick(View v) {
			System.out.println("Inside mGone Click");
			if ((mHideContainer.getVisibility() == View.INVISIBLE)
					|| (mHideContainer.getVisibility() == View.GONE)) {
				mHideContainer.setVisibility(View.VISIBLE);
				restartUpdater();
			} /*
			 * else { mHideContainer.setVisibility(View.INVISIBLE);
			 * seekBarUpdater.stopIt(); }
			 */
		}
	};

	OnClickListener mVisibleListener = new OnClickListener() {
		public void onClick(View v) {
			if ((mHideContainer.getVisibility() == View.GONE)
					|| (mHideContainer.getVisibility() == View.INVISIBLE)) {
				mHideContainer.setVisibility(View.VISIBLE);
				restartUpdater();
			} /*
			 * else { mHideContainer.setVisibility(View.INVISIBLE);
			 * seekBarUpdater.stopIt(); }
			 */
		}
	};

	OnClickListener mControlPanelListener = new OnClickListener() {
		public void onClick(View v) {
			// Do not hide the control panel par, when clicked
			// System.out.println("CONTROL PANEL  LISTENER ONCLICK ");
		}
	};

	OnTouchListener imgAspectRatioTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			ImageView img = (ImageView) v;
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				// Do nothing for now
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				if (current_aspect_ratio_type == 3) {
					img.setImageResource(R.drawable.fs_shadow_4_3);
					demoRenderer.nativePlayerSetAspectRatio(0);
					current_aspect_ratio_type = 1;
				} else if (current_aspect_ratio_type == 1) {
					img.setImageResource(R.drawable.fs_shadow);
					demoRenderer.nativePlayerSetAspectRatio(3);
					current_aspect_ratio_type = 2;
				} else if (current_aspect_ratio_type == 2) {
					img.setImageResource(R.drawable.fs_shadow_16_9);
					demoRenderer.nativePlayerSetAspectRatio(2);
					current_aspect_ratio_type = 3;

				}
			}
			// resetAutoHider();
			return true;
		}
	};

	OnTouchListener imgPlayTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			ImageView img = (ImageView) v;

			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				System.out.println("Down paused:" + paused);
				if (paused) {
					img.setImageResource(R.drawable.vp_play);
				} else {
					img.setImageResource(R.drawable.vp_pause);
				}
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				System.out.println("Up paused:" + paused);
				System.out.println("Total:"
						+ demoRenderer.nativePlayerTotalDuration()
						+ "---Current:" + demoRenderer.nativePlayerDuration());
				if (paused) {
					demoRenderer.nativePlayerPause();
					seekBarUpdater = new Updater();
					mHandler.postDelayed(seekBarUpdater, 500);
					img.setImageResource(R.drawable.vp_pause_shadow);
				} else {
					demoRenderer.nativePlayerPlay();
					seekBarUpdater.stopIt();
					img.setImageResource(R.drawable.vp_play_shadow);
				}
				paused = !paused;
			}
			// resetAutoHider();
			return true;
		}
	};

	OnTouchListener imgForwardTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			ImageView img = (ImageView) v;
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				img.setImageResource(R.drawable.vp_forward_glow);
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				img.setImageResource(R.drawable.vp_forward);
				demoRenderer.nativePlayerForward();
			}
			// resetAutoHider();
			return true;
		}
	};

	OnTouchListener imgBackwardTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			ImageView img = (ImageView) v;
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				img.setImageResource(R.drawable.vp_backward_glow_80x60);
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				img.setImageResource(R.drawable.vp_backward);
				demoRenderer.nativePlayerRewind();
			}
			// resetAutoHider();
			return true;
		}
	};

	OnSeekBarChangeListener mSeekBarChangeListener = new OnSeekBarChangeListener() {
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// System.out.println("Should be visible:" +
			// trScrolledTime.getVisibility());
			// trScrolledTime.setVisibility(View.GONE);
			// trScrolledTime.setVisibility(View.INVISIBLE);
			// System.out.println("Should be gone:" +
			// trScrolledTime.getVisibility());

			int progress = seekBar.getProgress();
			// System.out.println("Seeked to new progress" + (float) (progress /
			// 10F ));
			// System.out.println("Progress new:"+progress);
			demoRenderer.nativePlayerSeek(progress);
			restartUpdater();

			Handler myHandler = new stopPlay();
			Message m = new Message();

			myHandler.sendMessageDelayed(m, 500);
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// // TODO Auto-generated method stub
			// trScrolledTime.setVisibility(View.VISIBLE);
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
			// System.out.println("Progress Changed = " + progress);
			// System.out.println("Progres change percent=" + (float) (progress
			// / 10F ));
			if (fromUser) {
				long currentSecsMoved = (long) ((totalDuration * ((float) (progress / 10F))) / 100);
				String timeMoved = Utils.formatTime(currentSecsMoved);
				// scrolledtime.setText(timeMoved);
				currentTime.setText(timeMoved);
				// resetAutoHider();
			}
		}
	};

	public class stopPlay extends Handler {
		@Override
		public void handleMessage(Message msg) {

			// Obtain MotionEvent object
			long downTime = SystemClock.uptimeMillis();
			long eventTime = SystemClock.uptimeMillis() + 100;
			float x = 0.0f;
			float y = 0.0f;
			// List of meta states found here:
			// developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
			int metaState = 0;
			MotionEvent motionEvent = MotionEvent.obtain(downTime, eventTime,
					MotionEvent.ACTION_UP, x, y, metaState);

			// Dispatch touch event to view
			imgPlay.dispatchTouchEvent(motionEvent);

			imgBackward.setVisibility(View.VISIBLE);

			imgForward.setVisibility(View.VISIBLE);

			mProgressBar.setVisibility(View.GONE);

			if (current_position == 1 && !stageMsgDisplayed) {
				show(msg1);
				stageMsgDisplayed = true;
			}
		}
	}

	OnTouchListener imgCalculateSpeedTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {

			if (event.getAction() == MotionEvent.ACTION_UP) {

				calculatingSpeed = false;

				if (current_position == 1) {
					current_position = 2;
					current_message = msg2;
					show(current_message);
					stageMsgDisplayed = true;
				} else if (current_position == 2) {
					current_position = 3;
					current_message = msg3;
					show(current_message);
					stageMsgDisplayed = true;
				} else if (current_position == 3) {
					current_position = 4;
					fieldSize();
				} else {
					if (current_position == 4)
						current_position = 5;

					current_message = msg4;
					stageMsgDisplayed = false;
					confirm("Calculate speed?");
				}
			}

			return true;
		}
	};

	OnTouchListener imgNextTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {

			if (event.getAction() == MotionEvent.ACTION_UP) {
				if (paused)
					clickButton(imgPlay);

				imgBackward.setVisibility(View.INVISIBLE);

				imgForward.setVisibility(View.INVISIBLE);

				mProgressBar.setVisibility(View.VISIBLE);

				tracking = true;

				trackingCount = 0;

			}

			return true;
		}
	};

	OnTouchListener imgPrevTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {

			if (event.getAction() == MotionEvent.ACTION_UP) {
				if (paused)
					clickButton(imgPlay);

				imgBackward.setVisibility(View.INVISIBLE);
				// cv::Mat src;

				// frame.copyTo(src);

				imgForward.setVisibility(View.INVISIBLE);

				mProgressBar.setVisibility(View.VISIBLE);

				long playedDuration = demoRenderer.nativePlayerDuration();

				long totalDuration = demoRenderer.nativePlayerTotalDuration();

				int percentage = (int) (((float) playedDuration / (float) totalDuration) * 100f);

				int step = (int) (((1f / (float) totalDuration)) * 100f);

				if (percentage - step > 0) {
					percentage = percentage - step;
				}

				String msg = "Position: " + playedDuration + "\nDuration: "
						+ totalDuration + "\nPercentage: " + percentage
						+ "\nStep: " + step;

				demoRenderer.nativePlayerSeek(percentage);
				if (!paused) {
					restartUpdater();
				}

				Handler myHandler = new stopPlay();
				Message m = new Message();

				myHandler.sendMessageDelayed(m, 5000);

			}

			return true;
		}
	};

	OnTouchListener imgHelpTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {

			if (event.getAction() == MotionEvent.ACTION_UP) {
				show(current_message);
			}

			return true;
		}
	};

	public void show(String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(true);
		builder.setTitle(msg);
		builder.setInverseBackgroundForced(true);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (current_position) {
				case 3:
					drawView.mCurrentPlayer = VIEW_MODE_PITCHER;
					mHideContainer.setVisibility(View.INVISIBLE);
					drawView.canDraw = true;
					break;
				case 2:
					mHideContainer.setVisibility(View.INVISIBLE);
					drawView.mCurrentPlayer = VIEW_MODE_CATCHER;
					drawView.canDraw = true;
					break;
				}
				dialog.dismiss();
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
	}

	public void confirm(String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(true);
		builder.setTitle(msg);
		builder.setInverseBackgroundForced(true);
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				calculatingSpeed = true;
				stageMsgDisplayed = false;
				drawView.calculatedSpeed = 0;
				MyView.calculatingSpeed = 1;
				clickButton(imgPlay);
				dialog.dismiss();
			}
		});

		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		AlertDialog alert = builder.create();
		alert.show();

	}

	public void fieldSize() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(true);
		builder.setTitle("Select Field Size");
		builder.setInverseBackgroundForced(true);
		builder.setPositiveButton("60ft",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						drawView.fieldSize = 60;
						dialog.dismiss();
					}
				});

		builder.setNegativeButton("45ft",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						drawView.fieldSize = 45;
						dialog.dismiss();
					}
				});

		AlertDialog alert = builder.create();
		alert.show();

	}

	public void speedCalculated(double calculatedSpeed) {

		clickButton(imgPlay);

		msg4 = "THE CALCULATED SPEED IS: " + calculatedSpeed
				+ " MPH; Recalculate?";

		imgText.setText(calculatedSpeed + " MPH");

		// show(msg4);
		confirm(msg4);
		stageMsgDisplayed = true;
	}

	public void displayPoints(String points) {
		imgPoints.setText(points);
	}

	public void clickButton(View button) {

		// Obtain MotionEvent object
		long downTime = SystemClock.uptimeMillis();
		long eventTime = SystemClock.uptimeMillis() + 100;
		float x = 0.0f;
		float y = 0.0f;
		// List of meta states found here:
		// developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
		int metaState = 0;
		MotionEvent motionEvent = MotionEvent.obtain(downTime, eventTime,
				MotionEvent.ACTION_UP, x, y, metaState);

		// Dispatch touch event to view
		button.dispatchTouchEvent(motionEvent);

	}

	View mHideContainer;
	View mControlPanelContainer;

	View imgPlay;
	View imgBackward;
	View imgForward;
	View imgAspectRatio;
	SeekBar mSeekBar;
	TextView currentTime;
	TextView totalTime;

	long totalDuration;

	DemoRenderer demoRenderer;
	TableLayout controlPanel;

	private AudioThread mAudioThread = null;
	private PowerManager.WakeLock wakeLock = null;
	private Handler mHandler = new Handler();

	private Updater seekBarUpdater = new Updater();
	private static int current_aspect_ratio_type = 1; // Default Aspect Ratio of
														// the file
	private static boolean paused;
	String openfileFromBrowser = "";
	Intent i = getIntent();

	TelephonyManager mgr;

	private String TAG = "VideoPlayer";

	private MyView drawView;
	// private SurfaceView mFixedStuff;

	// private ColorBlobDetectionView mView;
	private FrameLayout mParent;

	public static final int VIEW_MODE_PITCHER = 0;
	public static final int VIEW_MODE_CATCHER = 1;

	private MenuItem mItemPreviewPitcher;
	private MenuItem mItemPreviewCatcher;
	private MenuItem mItemDebug;
	private MenuItem mItemTrackbar;
	private MenuItem mItemReset;
	private MenuItem mItemTest;

	public static int threshVal = 20;
	public static int blobthreshVal = 2000;

	public native void nativePointReset();

	public native void nativePause();

	public boolean alertValue = false;

	public boolean alertValueSet = false;

	public boolean tracking = false;

	public int trackingCount = 0;

	public boolean calculatingSpeed = false;

	View imgPrev;
	View imgNext;
	View imgStop;
	View imgHelp;

	// View imgBackward;
	// View imgForward;
	View imgLogo;

	TextView imgText;
	TextView imgPoints;

	ProgressBar mProgressBar;

	public static String msg1 = "STEP TO BEGINNING OF PITCH RELEASE";
	public static String msg3 = "SELECT PITCHER";
	public static String msg2 = "SELECT CATCHER";
	public String msg4 = "THE CALCULATED SPEED IS ";

	String current_message = msg1;
	int current_position = -1;

	public boolean stageMsgDisplayed = false;

}
