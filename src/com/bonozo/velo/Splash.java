package com.bonozo.velo;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Splash extends Activity {

	private Button prLoadBtn;
	private Button prRecordBtn;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);

		TextView cpu_abi = (TextView) findViewById(R.id.cpu_abi);

		cpu_abi.setText(Build.CPU_ABI);

		prLoadBtn = (Button) findViewById(R.id.main_load);
		prRecordBtn = (Button) findViewById(R.id.main_record);

		prLoadBtn.setOnClickListener(new View.OnClickListener() {
			// @Override
			public void onClick(View v) {
				Intent myIntent = new Intent(Splash.this, AVPlayerMain.class);

				Splash.this.startActivity(myIntent);
				Splash.this.finish();
			}
		});

		prRecordBtn.setOnClickListener(new View.OnClickListener() {
			// @Override
			public void onClick(View v) {
				Intent myIntent = new Intent(Splash.this, CameraView.class);

				Splash.this.startActivity(myIntent);
				Splash.this.finish();
			}
		});

	}

	public void onClick(View v) {
		// TODO Auto-generated method stub
		Intent myIntent = new Intent(Splash.this, AVPlayerMain.class);

		Splash.this.startActivity(myIntent);
		Splash.this.finish();
	}

}
