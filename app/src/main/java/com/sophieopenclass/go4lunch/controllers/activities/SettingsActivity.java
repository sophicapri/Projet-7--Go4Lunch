package com.sophieopenclass.go4lunch.controllers.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.sophieopenclass.go4lunch.MyViewModel;
import com.sophieopenclass.go4lunch.R;
import com.sophieopenclass.go4lunch.base.BaseActivity;
import com.sophieopenclass.go4lunch.databinding.ActivitySettingsBinding;
import com.sophieopenclass.go4lunch.models.User;
import com.sophieopenclass.go4lunch.utils.NotificationWorker;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static android.content.Intent.EXTRA_UID;

public class SettingsActivity extends BaseActivity<MyViewModel> {
    private static final String TAG = "SettingsActivity";
    private static final String WORK_REQUEST_NAME = "Lunch reminder";
    private ActivitySettingsBinding binding;
    public static boolean localeHasChanged = false;

    @Override
    public Class getViewModelClass() {
        return MyViewModel.class;
    }

    @Override
    public View getFragmentLayout() {
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!sharedPrefs.getBoolean(PREF_REMINDER, false))
            binding.notificationToggle.setChecked(false);

        binding.notificationToggle.setOnClickListener(v -> {
            if (binding.notificationToggle.isChecked())
                activateReminder();
            else {
                cancelReminder();
            }
        });

        binding.appLocale.setOnClickListener( v -> changeAppLanguage("fr"));

        binding.appLocaleEn.setOnClickListener( v -> changeAppLanguage("en"));
    }

    private void cancelReminder() {
        // deletes finished works, to not crash the app
        // cancels unfinished works
        workManager.cancelAllWork();
        sharedPrefs.edit().putBoolean(PREF_REMINDER, false).apply();
        Log.i(TAG, "cancelReminder: ");
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (networkUnavailable()) {
            Snackbar.make(binding.getRoot(), getString(R.string.internet_unavailable), BaseTransientBottomBar.LENGTH_INDEFINITE)
                    .setDuration(5000).setTextColor(getResources().getColor(R.color.quantum_white_100)).show();
        } /*else if (getCurrentUser() != null)
            viewModel.getUser(getCurrentUser().getUid()).observe(this,this::initUI);
        */
    }

   // private void initUI(User user) { }

    private void changeAppLanguage(String locale) {
        if (!locale.equals(sharedPrefs.getString(PREF_LANGUAGE, Locale.getDefault().getLanguage()))) {
            sharedPrefs.edit().putString(PREF_LANGUAGE, locale).apply();
            localeHasChanged = true;
            updateLocale();
            finish();
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            Toast.makeText(this, R.string.locale_saved, Toast.LENGTH_SHORT).show();
        }
    }

/*
    public void activateReminder() {
        Calendar currentDate = Calendar.getInstance();
        Calendar dueDate = Calendar.getInstance();
        // Set Execution time of the reminder
        dueDate.set(Calendar.HOUR_OF_DAY, 2);
        dueDate.set(Calendar.MINUTE, 15);
        dueDate.set(Calendar.SECOND, 0);
        dueDate.set(Calendar.MILLISECOND, 0);

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.DAY_OF_MONTH, 1);
        }

        long timeDiff = dueDate.getTimeInMillis() - currentDate.getTimeInMillis();

        Data userId = new Data.Builder().build();
        if (getCurrentUser() != null)
            userId = new Data.Builder()
                    .putString(EXTRA_UID, getCurrentUser().getUid())
                    .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, 1,
                TimeUnit.DAYS)
                .setInputData(userId)
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .build();

        workManager.enqueueUniquePeriodicWork(WORK_REQUEST_NAME, ExistingPeriodicWorkPolicy.REPLACE, workRequest);
        sharedPrefs.edit().putBoolean(PREF_REMINDER, true).apply();
    }

 */

}