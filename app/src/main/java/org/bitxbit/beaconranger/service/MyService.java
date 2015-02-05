package org.bitxbit.beaconranger.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
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
import org.bitxbit.beaconranger.ui.MainActivity;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyService extends Service implements BeaconConsumer {
    private static final String TAG = MyService.class.getSimpleName();
    private Set<Beacon> beaconsSeen = new HashSet<Beacon>();
    private BeaconManager mgr;
    private static final String[] BEACON_IDS =
            new String[] {"e2154275-f952-44f4-add2-2b1cf9febec7",
                    "2f234454-cf6d-4a0f-adf2-f4911ba9ffa6"};
    private static final Set<Region> knownRegions = new HashSet<Region>();
    private static final long ALARM_INTERVAL = /*15 * 60 * 1000;*/ 10000;
    private List<BeaconLogEntry> log;
    private PendingIntent pendingIntent;

    public MyService() {
        mgr = BeaconManager.getInstanceForApplication(this);
        log = new ArrayList<BeaconLogEntry>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand " + intent.getAction());
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent stopIntent = new Intent(this, MyService.class);
        stopIntent.setAction("STOP");
        if ("START".equals(intent.getAction())) {
            if (!mgr.isBound(this)) mgr.bind(this);

            Notification notification = new Notification.Builder(this)
                    .setContentTitle("Beacon Ranger")
                    .setContentIntent(pendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "STOP", PendingIntent.getService(MyService.this.getApplicationContext(), 0, stopIntent, 0))
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setOngoing(true).build();

            Intent uploadIntent = new Intent(this, BeaconDataUploadService.class);

            AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            this.pendingIntent = PendingIntent.getService(this.getApplicationContext(), 1000, uploadIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, 0, ALARM_INTERVAL,
                    this.pendingIntent);

            startForeground(1000, notification);
        } else if ("STOP".equals(intent.getAction())) {
            cleanup();


            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    private void cleanup() {
        for (String s : BEACON_IDS) {
            try {
                Region r = new Region("gannett-" + s, Identifier.parse(s), null, null);
                if (mgr.isBound(this)) mgr.stopMonitoringBeaconsInRegion(r);
                if (mgr.isBound(this)) mgr.stopRangingBeaconsInRegion(r);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mgr.unbind(this);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    @Override
    public void onDestroy() {
        cleanup();
    }

    @Override
    public void onBeaconServiceConnect() {
        BeaconManager mgr = BeaconManager.getInstanceForApplication(getApplicationContext());
        mgr.setMonitorNotifier(new MonitorNotifierImpl(this));
        for (String s : BEACON_IDS) {
            try {
                mgr.startMonitoringBeaconsInRegion(new Region("gannett-" + s, Identifier.parse(s), null, null));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private static class MonitorNotifierImpl implements MonitorNotifier {

        private WeakReference<MyService> ref;

        private MonitorNotifierImpl(MyService serv) {
            this.ref = new WeakReference<MyService>(serv);
        }

        @Override
        public void didEnterRegion(Region region) {
            MyService act = ref.get();
            if (act == null) return;
            if (knownRegions.contains(region)) return;

            act.mgr.setRangeNotifier(new RangeNotifierImpl(act));
            try {
                act.mgr.startRangingBeaconsInRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void didExitRegion(Region region) {
            MyService act = ref.get();
            if (act == null) return;
            act.knownRegions.remove(region);

            try {
                act.mgr.stopRangingBeaconsInRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void didDetermineStateForRegion(int i, Region region) {

        }
    }

    private static class RangeNotifierImpl implements RangeNotifier {
        private static final double MAX_DISTANCE = 2.0d;
        private WeakReference<MyService> ref;

        private RangeNotifierImpl(MyService serv) {
            ref = new WeakReference<MyService>(serv);
        }

        @Override
        public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
            MyService serv = ref.get();
            if (serv == null) return;
            for (Beacon b : beacons) {
                if (!serv.beaconsSeen.contains(b) && b.getDistance() < MAX_DISTANCE) {
                    serv.beaconsSeen.add(b);
                    BeaconLogEntry logEntry = new BeaconLogEntry(
                    b.getId1().toHexString(), b.getId2().toHexString(), b.getId3().toHexString(), System.currentTimeMillis());
                    serv.log.add(logEntry);
//                    Log.d(TAG, "log entry : " + logEntry);
                }
            }
        }
    }

    public static class BeaconLogEntry {
        private String id1;
        private String id2;
        private String id3;
        private long ts;

        private BeaconLogEntry(String id1, String id2, String id3, long ts) {
            this.id1 = id1;
            this.id2 = id2 == null ? "" : id2;
            this.id3 = id3 == null ? "" : id3;
            this.ts = ts;
        }

        @Override
        public String toString() {
            Date d = new Date(ts);
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            return String.format("%1$s:%2$s:%3$s\t%4$s", id1, id2, id3, fmt.format(d));
        }
    }

    public class LocalBinder extends Binder {
        public List<BeaconLogEntry> getLogEntries() {
            return log;
        }

        public void emptyLog() {
            log.clear();
        }
    }

}
