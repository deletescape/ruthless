package com.google.android.apps.nexuslauncher;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.TwoStatePreference;
import android.text.TextUtils;
import android.util.Log;

import com.android.launcher3.LauncherModel;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.dynamicui.WallpaperColorInfo;
import com.android.launcher3.util.LooperExecutor;

public class SettingsActivity extends com.android.launcher3.SettingsActivity implements PreferenceFragment.OnPreferenceStartFragmentCallback {
    public final static String ICON_PACK_PREF = "pref_icon_pack";
    public final static String SHOW_PREDICTIONS_PREF = "pref_show_predictions";
    public final static String ENABLE_MINUS_ONE_PREF = "pref_enable_minus_one";
    public final static String SMARTSPACE_PREF = "pref_smartspace";
    public final static String APP_VERSION_PREF = "about_app_version";
    public final static String BASED_ON_PREF = "based_on";
    public final static String COMPILED_BY_PREF = "Compiled_By";
    private final static String GOOGLE_APP = "com.google.android.googlequicksearchbox";
    public final static String Icon_BY_PREF = "Icon_By";
    private static final long WAIT_BEFORE_RESTART = 250;

    @Override
    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        if (bundle == null) {
            getFragmentManager().beginTransaction().replace(android.R.id.content, new MySettingsFragment()).commit();
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment, Preference preference) {
        Fragment instantiate = Fragment.instantiate(this, preference.getFragment(), preference.getExtras());
        if (instantiate instanceof DialogFragment) {
            ((DialogFragment) instantiate).show(getFragmentManager(), preference.getKey());
        } else {
            getFragmentManager().beginTransaction().replace(android.R.id.content, instantiate).addToBackStack(preference.getKey()).commit();
        }
        return true;
    }

    public static class MySettingsFragment extends com.android.launcher3.SettingsActivity.LauncherSettingsFragment
            implements Preference.OnPreferenceChangeListener {
        private Context mContext;


        @Override
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);

            mContext = getActivity();



            findPreference(SHOW_PREDICTIONS_PREF).setOnPreferenceChangeListener(this);
            findPreference(ENABLE_MINUS_ONE_PREF).setTitle(getDisplayGoogleTitle());

            findPreference(Utilities.GRID_COLUMNS).setOnPreferenceChangeListener(this);
            findPreference(Utilities.DGRID_COLUMNS).setOnPreferenceChangeListener(this);
            findPreference(Utilities.GRID_ROWS).setOnPreferenceChangeListener(this);
            findPreference(Utilities.HOTSEAT_ICONS).setOnPreferenceChangeListener(this);
            findPreference(Utilities.BOTTOM_SEARCH_BAR_KEY).setOnPreferenceChangeListener(this);
            findPreference(Utilities.PHYSICAL_ANIMATION_KEY).setOnPreferenceChangeListener(this);
            findPreference(Utilities.TRANSPARENT_NAV_BAR).setOnPreferenceChangeListener(this);

            //findPreference(Utilities.TOP_SEARCH_BAR_KEY).setOnPreferenceChangeListener(this);

            PackageManager packageManager = mContext.getPackageManager();
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(mContext.getPackageName(), 0);
                findPreference(APP_VERSION_PREF).setSummary(packageInfo.versionName);
            } catch (PackageManager.NameNotFoundException ex) {
                Log.e("SettingsActivity", "Unable to load my own package info", ex);
            }

            try {
                ApplicationInfo applicationInfo = mContext.getPackageManager().getApplicationInfo(GOOGLE_APP, 0);
                if (!applicationInfo.enabled) {
                    throw new PackageManager.NameNotFoundException();
                }
            } catch (PackageManager.NameNotFoundException ignored) {
                getPreferenceScreen().removePreference(findPreference(SettingsActivity.ENABLE_MINUS_ONE_PREF));
            }



            reloadIconPackSummary();
        }
        public static void restart(final Context context) {
            ProgressDialog.show(context, null, context.getString(R.string.state_loading), true, false);
            new LooperExecutor(LauncherModel.getWorkerLooper()).execute(new Runnable() {
                @SuppressLint("ApplySharedPref")
                @Override
                public void run() {
                    try {
                        Thread.sleep(WAIT_BEFORE_RESTART);
                    } catch (Exception e) {
                        Log.e("SettingsActivity", "Error waiting", e);
                    }

                    Intent intent = new Intent(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_HOME)
                            .setPackage(context.getPackageName())
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    alarmManager.setExact(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 50, pendingIntent);

                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            });


        }

        @Override
        public void onResume() {
            super.onResume();
            reloadIconPackSummary();
        }

        private void reloadIconPackSummary() {
            Preference preference = findPreference(ICON_PACK_PREF);
            if (preference == null) {
                return;
            }
            if( Utilities.ATLEAST_NOUGAT ) {
                getPreferenceScreen().findPreference("pref_DateFormats").setEnabled(true);
              //  getPreferenceScreen().findPreference("pref_allappqsb_color_picker").setEnabled(true);
            }
            String defaultPack = mContext.getString(R.string.default_iconpack);
            String iconPack = PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getString(Utilities.KEY_ICON_PACK, defaultPack);

            try {
                ApplicationInfo info = mContext.getPackageManager().getApplicationInfo(iconPack, 0);
                preference.setSummary(mContext.getPackageManager().getApplicationLabel(info));
            } catch (PackageManager.NameNotFoundException e) {
                preference.setSummary(defaultPack);
            }
        }


        private String getDisplayGoogleTitle() {
            CharSequence charSequence = null;
            try {
                Resources resourcesForApplication = mContext.getPackageManager().getResourcesForApplication(GOOGLE_APP);
                int identifier = resourcesForApplication.getIdentifier("title_google_home_screen", "string", GOOGLE_APP);
                if (identifier != 0) {
                    charSequence = resourcesForApplication.getString(identifier);
                }
            }
            catch (PackageManager.NameNotFoundException ex) {
            }
            if (TextUtils.isEmpty(charSequence)) {
                charSequence = mContext.getString(R.string.title_google_app);
            }
            return mContext.getString(R.string.title_show_google_app, charSequence);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, final Object newValue) {
            switch (preference.getKey()) {
                case SHOW_PREDICTIONS_PREF:
                    if ((boolean) newValue) {
                        return true;
                    }
                    SettingsActivity.SuggestionConfirmationFragment confirmationFragment = new SettingsActivity.SuggestionConfirmationFragment();
                    confirmationFragment.setTargetFragment(this, 0);
                    confirmationFragment.show(getFragmentManager(), preference.getKey());
                    break;


                case Utilities.BOTTOM_SEARCH_BAR_KEY:
                    if (preference instanceof TwoStatePreference) {
                        ((TwoStatePreference) preference).setChecked((boolean) newValue);
                    }
                    restart(mContext);
                    break;

                case Utilities.DGRID_COLUMNS:
                case Utilities.GRID_COLUMNS:
                case Utilities.GRID_ROWS:
                case Utilities.HOTSEAT_ICONS:
                    if (preference instanceof ListPreference) {
                        ((ListPreference) preference).setValue((String) newValue);
                    }
                    restart(mContext);
                    break;

                case Utilities.PHYSICAL_ANIMATION_KEY:
                    if (preference instanceof TwoStatePreference) {
                        ((TwoStatePreference) preference).setChecked((boolean) newValue);
                    }
                    reloadTheme(mContext);
                    break;

                case Utilities.TRANSPARENT_NAV_BAR:
                    if (preference instanceof TwoStatePreference) {
                        ((TwoStatePreference) preference).setChecked((boolean) newValue);
                    }
                    reloadTheme(mContext);
                    break;
                /* case Utilities.TOP_SEARCH_BAR_KEY:
                    if (preference instanceof TwoStatePreference) {
                        ((TwoStatePreference) preference).setChecked((boolean) newValue);
                    }
                    reloadTheme(mContext);
                    break;*/
            }
            return false; }

    }


    public static class SuggestionConfirmationFragment extends DialogFragment implements DialogInterface.OnClickListener {
        public void onClick(final DialogInterface dialogInterface, final int n) {
            if (getTargetFragment() instanceof PreferenceFragment) {
                Preference preference = ((PreferenceFragment) getTargetFragment()).findPreference(SHOW_PREDICTIONS_PREF);
                if (preference instanceof TwoStatePreference) {
                    ((TwoStatePreference) preference).setChecked(false);
                }
            }
        }

        public Dialog onCreateDialog(final Bundle bundle) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.title_disable_suggestions_prompt)
                    .setMessage(R.string.msg_disable_suggestions_prompt)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.label_turn_off_suggestions, this).create();
        }
    }

    public static void reloadTheme(Context context) {
        WallpaperColorInfo.getInstance(context).notifyChange(true);
    }

    public static void restart(final Context context) {
        ProgressDialog.show(context, null, context.getString(R.string.state_loading), true, false);
        new LooperExecutor(LauncherModel.getWorkerLooper()).execute(new Runnable() {
            @SuppressLint("ApplySharedPref")
            @Override
           public void run() {
                try {
                    Thread.sleep(WAIT_BEFORE_RESTART);
                } catch (Exception e) {
                    Log.e("SettingsActivity", "Error waiting", e);
                }
                Intent intent = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME)
                        .setPackage(context.getPackageName())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 50, pendingIntent);


                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
    }

}
