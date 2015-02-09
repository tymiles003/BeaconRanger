package org.bitxbit.beaconranger.ui;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;

import org.bitxbit.beaconranger.R;

import java.lang.ref.WeakReference;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class LoginActivity extends Activity {

    private UiLifecycleHelper uiHelper;

    private Session.StatusCallback callback;
    private boolean isResumed;
    @InjectView(R.id.progress_login) ProgressBar progressLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        callback = new Callback(this);
        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        ButterKnife.inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiHelper.onResume();
        isResumed = true;
        progressLogin.setVisibility(View.GONE);
        Session session = Session.getActiveSession();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (session != null && session.isOpened()) {
            ft.replace(R.id.container, new LoggedInFragment(), "LOGGED_IN");
        } else  {
            ft.replace(R.id.container, new FacebookLoginFragment(), "LOG_IN");
        }
        ft.commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHelper.onPause();
        isResumed = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    private void onSessionStateChanged(Session session, SessionState sessionState, Exception e) {
        if (!isResumed) return;
        displayFragment(sessionState);
    }

    private void displayFragment(SessionState sessionState) {
        progressLogin.setVisibility(View.GONE);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (sessionState.equals(SessionState.OPENED)) {
            ft.replace(R.id.container, new LoggedInFragment(), "LOGGED_IN");
        } else if (sessionState.equals(SessionState.CLOSED)) {
            ft.replace(R.id.container, new FacebookLoginFragment(), "LOG_IN");
        }
        ft.commit();
    }

    private static class Callback implements Session.StatusCallback {
        private WeakReference<LoginActivity> ref;

        private Callback(LoginActivity act) {
            ref = new WeakReference<LoginActivity>(act);
        }

        @Override
        public void call(Session session, SessionState sessionState, Exception e) {
            LoginActivity act = ref.get();
            if (act == null) return;
            act.onSessionStateChanged(session, sessionState, e);
        }
    }
}
