package org.bitxbit.beaconranger.service;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.List;


public class BeaconDataUploadService extends IntentService {
    private static final String TAG = BeaconDataUploadService.class.getSimpleName();
    private boolean bound;
    private ServiceConnection serviceConnection;
    private MyService.LocalBinder binder;

    public BeaconDataUploadService() {
        super("BeaconDataUploadService");
        serviceConnection = new ServiceConnectionImpl(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Intent serviceIntent = new Intent(this.getApplicationContext(), MyService.class);
        bindService(serviceIntent, serviceConnection, BIND_WAIVE_PRIORITY);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    private static class ServiceConnectionImpl implements ServiceConnection {
        private WeakReference<BeaconDataUploadService> ref;

        private ServiceConnectionImpl(BeaconDataUploadService serv) {
            ref = new WeakReference<BeaconDataUploadService>(serv);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyService.LocalBinder binder = (MyService.LocalBinder) service;
            List<MyService.BeaconLogEntry> l = binder.getLogEntries();
            if (!l.isEmpty()) {
                Log.d(TAG, "entries : " + l);
            } else {
                Log.d(TAG, "no entries");
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }
}
