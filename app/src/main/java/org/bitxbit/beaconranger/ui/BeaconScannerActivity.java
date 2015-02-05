package org.bitxbit.beaconranger.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.bitxbit.beaconranger.R;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class BeaconScannerActivity extends Activity implements BeaconConsumer {

    private static final String TAG = BeaconScannerActivity.class.getSimpleName();
    private static final Set<Beacon> knownBeacons = new HashSet<Beacon>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon_scanner);
        BeaconManager.getInstanceForApplication(getApplicationContext()).bind(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BeaconManager.getInstanceForApplication(getApplicationContext()).unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {

        BeaconManager mgr = BeaconManager.getInstanceForApplication(getApplicationContext());

        monitor(mgr);

//        range(mgr);
    }

    private void range(BeaconManager mgr) {
        mgr.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                for (Beacon b : beacons) {
                    if (!knownBeacons.contains(b)) {
                        knownBeacons.add(b);
                        Log.d(TAG, "Saw new beacon: " + b.getIdentifiers());
                    }
                }
            }
        });
        try {
            mgr.startRangingBeaconsInRegion(new Region("home", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void monitor(BeaconManager mgr) {
        mgr.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                Log.d(TAG, "entered " + region.getId1() + " :: " + region.getId2() + " :: " + region.getId3());
            }

            @Override
            public void didExitRegion(Region region) {
                Log.d(TAG, "exited " + region.getId1() + " :: " + region.getId2() + " :: " + region.getId3());
            }

            @Override
            public void didDetermineStateForRegion(int i, Region region) {

            }
        });

        try {
            BeaconManager.getInstanceForApplication(getApplicationContext()).startMonitoringBeaconsInRegion(
                    new Region("home", Identifier.parse("e2154275-f952-44f4-add2-2b1cf9febec7"), null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
