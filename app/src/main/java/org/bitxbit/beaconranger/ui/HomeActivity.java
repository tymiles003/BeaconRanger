package org.bitxbit.beaconranger.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;
import org.bitxbit.beaconranger.service.MyService;
import org.bitxbit.beaconranger.R;

import java.lang.ref.WeakReference;


public class HomeActivity extends Activity implements BeaconConsumer {

    private ImageView statusIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        statusIndicator = (ImageView) findViewById(R.id.img_status_indicator);
        BeaconManager.getInstanceForApplication(getApplicationContext()).bind(this);
//        BeaconManager.getInstanceForApplication(getApplicationContext()).setMonitorNotifier(new MonitorNotifierImpl(this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        BeaconManager.getInstanceForApplication(getApplicationContext()).setBackgroundMode(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        BeaconManager mgr = BeaconManager.getInstanceForApplication(getApplicationContext());
        mgr.setMonitorNotifier(null);
        mgr.setBackgroundMode(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BeaconManager.getInstanceForApplication(getApplicationContext()).unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        Intent intent = new Intent(this, MyService.class);
        intent.setAction("START");
        startService(intent);
    }

    private static class MonitorNotifierImpl implements MonitorNotifier {

        private WeakReference<HomeActivity> ref;

        private MonitorNotifierImpl(HomeActivity act) {
            this.ref = new WeakReference<HomeActivity>(act);
        }

        @Override
        public void didEnterRegion(Region region) {
            HomeActivity act = ref.get();
            if (act == null) return;
            act.statusIndicator.setImageResource(R.drawable.green_circle);
        }

        @Override
        public void didExitRegion(Region region) {
            HomeActivity act = ref.get();
            if (act == null) return;
            act.statusIndicator.setImageResource(R.drawable.green_circle);
        }

        @Override
        public void didDetermineStateForRegion(int i, Region region) {

        }
    }
}
