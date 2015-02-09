package org.bitxbit.beaconranger.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.bitxbit.beaconranger.service.MyService;
import org.bitxbit.beaconranger.R;

import java.lang.ref.WeakReference;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends Activity implements BeaconConsumer {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String BEACON_RANGER = "BeaconRanger";
    private static final String OPTED_IN = "OPTED_INTO_BEACON";
    private BeaconManager beaconManager;

    @InjectView(R.id.img_status_indicator) ImageView imgStatusIndicator;
    @InjectView(R.id.chk_opt_in_monitoring) CheckBox chkOptIn;
    @InjectView(R.id.layout_indicator) ViewGroup layoutIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        beaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        ButterKnife.inject(this);
    }


    @Override
    protected void onStart() {
        super.onStart();
        boolean optedIn = getSharedPreferences(BEACON_RANGER, MODE_PRIVATE).getBoolean(OPTED_IN, false);
        if (optedIn) {
            beaconManager.bind(this);
            layoutIndicator.setVisibility(View.VISIBLE);
            beaconManager.setBackgroundMode(true);
        }
        chkOptIn.setChecked(optedIn);

        chkOptIn.setOnCheckedChangeListener(new OptInCheckedChangeListener(this));
    }

    @Override
    protected void onStop() {
        super.onStop();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        Intent intent = new Intent(this, MyService.class);
        intent.setAction("START");
        startService(intent);

        imgStatusIndicator.setImageResource(R.drawable.green_circle);
    }

    private static class OptInCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
        private WeakReference<MainActivity> ref;

        private OptInCheckedChangeListener(MainActivity act) {
            ref = new WeakReference<MainActivity>(act);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            MainActivity act = ref.get();
            if (act == null) return;
            if (isChecked) {
                if (!act.beaconManager.isBound(act)) act.beaconManager.bind(act);
                act.getSharedPreferences(BEACON_RANGER, MODE_PRIVATE).edit().putBoolean(OPTED_IN, true).commit();
                act.layoutIndicator.setVisibility(View.VISIBLE);
            } else {
                if (act.beaconManager.isBound(act)) act.beaconManager.unbind(act);
                act.getSharedPreferences(BEACON_RANGER, MODE_PRIVATE).edit().putBoolean(OPTED_IN, false).commit();
                act.layoutIndicator.setVisibility(View.INVISIBLE);
            }
        }
    }
}
