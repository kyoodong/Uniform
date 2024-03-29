package com.songjin.usum.controllers.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.nkzawa.emitter.Emitter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.kakao.usermgmt.callback.UnLinkResponseCallback;
import com.kakao.util.helper.SharedPreferencesCache;
import com.securepreferences.SecurePreferences;
import com.songjin.usum.Global;
import com.songjin.usum.R;
import com.songjin.usum.controllers.activities.BaseActivity;
import com.songjin.usum.controllers.activities.LoginActivity;
import com.songjin.usum.controllers.activities.MainActivity;
import com.songjin.usum.entities.AlarmEntity;
import com.songjin.usum.entities.ReservedCategoryEntity;
import com.songjin.usum.gcm.gcm.PushHandler;
import com.songjin.usum.managers.AuthManager;
import com.songjin.usum.managers.RequestManager;
import com.songjin.usum.slidingtab.SlidingBaseFragment;
import com.songjin.usum.socketIo.SocketIO;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public class SettingFragment extends SlidingBaseFragment {
    private static final String TAG = "SettingFragment";
    public static Context context;
    private static PushHandler handler;

    private class ViewHolder {
        public Button disconnectButton;
        public Button logoutButton;
        public Switch useTransactionPush;
        public Switch useTimelinePush;

        public ViewHolder(View view) {
            disconnectButton = (Button) view.findViewById(R.id.disconnect_button);
            logoutButton = (Button) view.findViewById(R.id.logout_button);
            useTransactionPush = (Switch) view.findViewById(R.id.use_transaction_push);
            useTimelinePush = (Switch) view.findViewById(R.id.use_timeline_push);
        }
    }

    private ViewHolder viewHolder;

    public static final String PREFERENCE_USE_TRANSACTION_PUSH = "useTransactionPush";
    public static final String PREFERENCE_USE_TIMELINE_PUSH = "useTimelinePush";
    public static final String PREFERENCE_RESERVED_CATEGORIES = "useReservationPush";
    public static final String PREFERENCE_RECEIVED_PUSH_MESSAGES = "pushMessages";
    public static final String PREFERENCE_ALARM_SYNCED_TIMESTAMP = "alarmSyncedTimestamp";
    public static final String PREFERENCE_SCHOOLS_LOADED = "schoolsLoaded";
    public static final String PREFERENCE_LAST_RANK = "lastRank";

    public static SettingFragment newInstance(PushHandler handler) {
        SettingFragment.handler = handler;
        return new SettingFragment();
    }

    @Override
    public void onPageSelected() {
        switch (AuthManager.getSignedInUserType()) {
            case Global.GUEST:
                BaseActivity.showGuestBlockedDialog();
                break;
            case Global.STUDENT:
                break;
            case Global.PARENT:
                viewHolder.useTimelinePush.setEnabled(false);
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        viewHolder = new ViewHolder(view);
        viewHolder.disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestUnlink();
            }
        });
        viewHolder.logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestLogout();
            }
        });
        viewHolder.useTransactionPush.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SecurePreferences securePrefs = new SecurePreferences(getActivity());
                SecurePreferences.Editor editor = securePrefs.edit();
                editor.putBoolean(PREFERENCE_USE_TRANSACTION_PUSH, isChecked);
                editor.commit();
            }
        });
        viewHolder.useTimelinePush.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SecurePreferences securePrefs = new SecurePreferences(getActivity());
                SecurePreferences.Editor editor = securePrefs.edit();
                editor.putBoolean(PREFERENCE_USE_TIMELINE_PUSH, isChecked);
                editor.commit();
            }
        });
//        viewHolder.useTransactionPush.setChecked(useTransactionPush(getActivity()));
//        viewHolder.useTimelinePush.setChecked(useTimelinePush(getActivity()));
        viewHolder.useTransactionPush.setChecked(useTransactionPush());
        viewHolder.useTimelinePush.setChecked(useTimelinePush());

        return view;
    }

    public static Boolean useTransactionPush() {
        SecurePreferences securePreferences = new SecurePreferences(context);
        return securePreferences.getBoolean(SettingFragment.PREFERENCE_USE_TRANSACTION_PUSH, true);
    }

    public static Boolean useTimelinePush() {
        SecurePreferences securePreferences = new SecurePreferences(context);
        return securePreferences.getBoolean(SettingFragment.PREFERENCE_USE_TIMELINE_PUSH, true);
    }

    private void requestUnlink() {
        final Handler handler = new Handler();

        final String appendMessage = getString(R.string.com_kakao_confirm_unlink);
        new AlertDialog.Builder(getActivity())
                .setMessage(appendMessage)
                .setPositiveButton(
                        getString(R.string.com_kakao_ok_button),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                BaseActivity.showLoadingView();

                                final SharedPreferencesCache cache = Session.getAppCache();

                                UserManagement.requestUnlink(new UnLinkResponseCallback() {
                                    @Override
                                    public void onSessionClosed(ErrorResult errorResult) {
                                        BaseActivity.hideLoadingView();
                                        new MaterialDialog.Builder(BaseActivity.context)
                                                .title(R.string.app_name)
                                                .content("연결해제에 실패하였습니다.")
                                                .show();
                                    }

                                    @Override
                                    public void onNotSignedUp() {
                                    }

                                    @Override
                                    public void onSuccess(Long result) {
                                        SocketIO.setToken(Global.userEntity.id, null, new RequestManager.OnSetToken() {
                                            @Override
                                            public void onSuccess() {

                                            }

                                            @Override
                                            public void onException() {

                                            }
                                        });

                                        RequestManager.deleteUser(Global.userEntity.id, new Emitter.Listener() {
                                            @Override
                                            public void call(final Object... args) {

                                                handler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        cache.clearAll();

                                                        JSONObject object = (JSONObject) args[0];

                                                        SharedPreferences preferences = getActivity().getSharedPreferences(Global.APP_NAME, Context.MODE_PRIVATE);
                                                        SharedPreferences.Editor editor = preferences.edit();
                                                        editor.clear();
                                                        editor.commit();

                                                        BaseActivity.hideLoadingView();
                                                        BaseActivity.startActivityOnTopStack(LoginActivity.class);
                                                        ((Activity) MainActivity.context).finish();
                                                    }
                                                });
                                            }
                                        });
                                    }
                                });

                                dialog.dismiss();

                                clearAllSharedPreferences();
                            }
                        })
                .setNegativeButton(
                        getString(R.string.com_kakao_cancel_button),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                .show();
    }

    private void requestLogout() {
        BaseActivity.showLoadingView();
        SocketIO.setToken(Global.userEntity.id, null, new RequestManager.OnSetToken() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onException() {

            }
        });

        UserManagement.requestLogout(new LogoutResponseCallback() {
            @Override
            public void onCompleteLogout() {
                SocketIO.setDeviceId(Global.userEntity.id, null, new RequestManager.OnSetDeviceId() {
                    @Override
                    public void onSuccess() {
                        SharedPreferencesCache cache = Session.getAppCache();
                        cache.clearAll();

                        ((Activity) MainActivity.context).finish();
                    }

                    @Override
                    public void onException() {

                    }
                });
            }
        });
    }

    public static ArrayList<ReservedCategoryEntity> getReservedCategories() {
        ArrayList<String> reservedCategoryJsonStrings;
//        SecurePreferences securePreferences = new SecurePreferences(BaasioApplication.context);
        if (context == null)
            return null;
        SecurePreferences securePreferences = new SecurePreferences(context);
        String rawJsonString = securePreferences.getString(PREFERENCE_RESERVED_CATEGORIES, "");
        Gson gson = new Gson();
        try {
            reservedCategoryJsonStrings = gson.fromJson(
                    rawJsonString,
                    new TypeToken<ArrayList<String>>() {
                    }.getType()
            );
        } catch (Exception e) {
            reservedCategoryJsonStrings = new ArrayList<>();
        }
        if (reservedCategoryJsonStrings == null) {
            return new ArrayList<>();
        }

        ArrayList<ReservedCategoryEntity> reservedCategoryEntities = new ArrayList<>();
        for (String jsonString : reservedCategoryJsonStrings) {
            reservedCategoryEntities.add(ReservedCategoryEntity.createObject(jsonString));
        }

        return reservedCategoryEntities;
    }

    public static void addReservedCategory(int schoolId, int category) {
        ArrayList<ReservedCategoryEntity> reservedCategories = getReservedCategories();
        reservedCategories.add(new ReservedCategoryEntity(schoolId, category, System.currentTimeMillis()));
        writeReservedCategory(reservedCategories);
    }

    public static void removeReservedCategory(int schoolId, int category) {
        ArrayList<ReservedCategoryEntity> reservedCategories = getReservedCategories();
        int index = 1;
        while (index != -1) {
            index = findIndexOfReservedCategories(reservedCategories ,schoolId, category);
            if (index != -1 && index < reservedCategories.size()) {
                reservedCategories.remove(index);
            }
        }
        writeReservedCategory(reservedCategories);
    }

    public static void updateReservedCategoryTimestamp(int schoolId, int category) {
        removeReservedCategory(schoolId, category);
        addReservedCategory(schoolId, category);
    }

    public static int findIndexOfReservedCategories(ArrayList<ReservedCategoryEntity> reservedCategories, int schoolId, int category) {
        int index = -1;
        if (reservedCategories == null)
            return index;

        for (int i=0 ; i<reservedCategories.size() ; i++) {
            ReservedCategoryEntity reservedCategory = reservedCategories.get(i);
            if (reservedCategory.schoolId == schoolId && reservedCategory.category == category) {
                index = i;
                break;
            }
        }

        return index;
    }

    private static void writeReservedCategory(ArrayList<ReservedCategoryEntity> reservedCategories) {
        ArrayList<String> reservedCategoryJsonStrings = new ArrayList<>();
        for (ReservedCategoryEntity reservedCategory : reservedCategories) {
            reservedCategoryJsonStrings.add(reservedCategory.toString());
        }

        SecurePreferences securePrefs = new SecurePreferences(context);
        SecurePreferences.Editor editor = securePrefs.edit();
        Gson gson = new Gson();
        editor.putString(PREFERENCE_RESERVED_CATEGORIES, gson.toJson(reservedCategoryJsonStrings));
        editor.commit();
    }

    public static ArrayList<AlarmEntity> getReceivedPushMessages() {
        SecurePreferences securePreferences = new SecurePreferences(context);
        String pushMessageJsonString = securePreferences.getString(PREFERENCE_RECEIVED_PUSH_MESSAGES, "");

        Gson gson = new Gson();
        ArrayList<String> pushMessageJsonStrings;
        try {
            pushMessageJsonStrings = gson.fromJson(
                    pushMessageJsonString,
                    new TypeToken<ArrayList<String>>() {
                    }.getType());
        } catch (Exception e) {
            pushMessageJsonStrings = new ArrayList<>();
        }
        if (pushMessageJsonStrings == null) {
            return new ArrayList<>();
        }

        ArrayList<AlarmEntity> receivedPushMessages = new ArrayList<>();
        for (String jsonString : pushMessageJsonStrings) {
            receivedPushMessages.add(gson.fromJson(jsonString, AlarmEntity.class));
        }

        return receivedPushMessages;
    }

    public static void addReceivedPushMessage(AlarmEntity alarmEntity) {
        ArrayList<AlarmEntity> pushMessages = getReceivedPushMessages();
        pushMessages.add(alarmEntity);

        Gson gson = new Gson();
        ArrayList<String> pushMessageJsonStrings = new ArrayList<>();
        for (AlarmEntity pushMessage : pushMessages) {
            pushMessageJsonStrings.add(0, gson.toJson(pushMessage));
        }

        SecurePreferences securePreferences = new SecurePreferences(context);
        SecurePreferences.Editor editor = securePreferences.edit();
        editor.putString(PREFERENCE_RECEIVED_PUSH_MESSAGES, gson.toJson(pushMessageJsonStrings));
        editor.commit();

        SettingFragment.handler.onReceivePushMessage();
    }

    public static long getLastAlarmSyncedTimestamp() {
        SecurePreferences securePreferences = new SecurePreferences(context);
        return securePreferences.getLong(PREFERENCE_ALARM_SYNCED_TIMESTAMP, 0);
    }

    public static void updateLastAlarmSyncedTimestamp() {
        SecurePreferences securePreferences = new SecurePreferences(context);
        SecurePreferences.Editor editor = securePreferences.edit();
        editor.putLong(PREFERENCE_ALARM_SYNCED_TIMESTAMP, System.currentTimeMillis());
        editor.commit();
    }

    private static void clearAllSharedPreferences() {
        SecurePreferences securePrefs = new SecurePreferences(context);
        SecurePreferences.Editor editor = securePrefs.edit();
        for (Map.Entry<String, String> entry : securePrefs.getAll().entrySet()) {
            editor.remove(entry.getKey());
        }
        editor.commit();
    }

    public static int getLastSchoolRank() {
        SecurePreferences securePreferences = new SecurePreferences(context);
        return securePreferences.getInt(PREFERENCE_LAST_RANK, -1);
    }

    public static void setLastSchoolRank(int lastRank) {
        SecurePreferences securePreferences = new SecurePreferences(context);
        SecurePreferences.Editor editor = securePreferences.edit();
        editor.putInt(PREFERENCE_LAST_RANK, lastRank);
        editor.commit();
    }

    public static boolean getSchoolsLoaded() {
        SecurePreferences securePreferences = new SecurePreferences(context);
        return securePreferences.getBoolean(PREFERENCE_SCHOOLS_LOADED, false);
    }

    public static void setSchoolsLoaded(boolean loaded) {
        SecurePreferences securePreferences = new SecurePreferences(context);
        SecurePreferences.Editor editor = securePreferences.edit();
        editor.putBoolean(PREFERENCE_SCHOOLS_LOADED, loaded);
        editor.commit();
    }
}
