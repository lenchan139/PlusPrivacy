package eu.operando.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.special.ResideMenu.ResideMenu;

import java.util.List;

import eu.operando.R;
import eu.operando.adapter.DrawerRecyclerViewAdapter;
import eu.operando.feedback.view.FeedbackActivity;
import eu.operando.lightning.activity.MainBrowserActivity;
import eu.operando.models.InstalledApp;
import eu.operando.storage.Storage;
import eu.operando.swarmService.SwarmService;
import eu.operando.swarmService.models.GetNotificationsSwarm;
import eu.operando.swarmService.models.LoginSwarmEntity;
import eu.operando.swarmService.models.RegisterZoneSwarm;
import eu.operando.swarmService.models.UDESwarm;
import eu.operando.swarmclient.SwarmClient;
import eu.operando.swarmclient.models.SwarmCallback;
import eu.operando.utils.PermissionUtils;

public class MainActivity extends AppCompatActivity implements DrawerRecyclerViewAdapter.IDrawerClickCallback {

    private AlertDialog disconnectDialog;
    private ResideMenu resideMenu;
    private ProgressDialog loadingDialog;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private RecyclerView drawerList;
    private Toolbar toolbar;

    public static void start(Context context, boolean autologin) {
        Intent starter = new Intent(context, MainActivity.class);
        starter.putExtra("autologin", autologin);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setMenu();

        autoLogin();

        SwarmClient.getInstance().setConnectionListener(new SwarmClient.ConnectionListener() {
            @Override
            public void onConnect() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (disconnectDialog != null)
                            disconnectDialog.dismiss();
                    }
                });
            }

            @Override
            public void onDisconnect() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) {
                            disconnectDialog =
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setMessage("Connection lost, trying to reconnect...")
                                            .setNegativeButton("Exit PlusPrivacy", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    finish();
                                                }
                                            }).setCancelable(false)
                                            .create();
                            disconnectDialog.show();
                        }
                    }
                });
            }
        });
    }

    private void autoLogin() {
        if (getIntent().getBooleanExtra("autologin", false)) {
            Pair<String, String> credentials = Storage.readCredentials();
            if (credentials.first != null && credentials.second != null) {
                swarmLogin(credentials.first, credentials.second);
                return;
            }
        } else {
            initUI();
        }
    }

    private void swarmLogin(final String username, final String password) {

        loadingDialog = new ProgressDialog(this);
        loadingDialog.setCancelable(false);
        loadingDialog.setMessage("Please wait...");
        loadingDialog.show();

        SwarmService.getInstance().login(username, password, new SwarmCallback<LoginSwarmEntity>() {
            @Override
            public void call(final LoginSwarmEntity result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingDialog.dismiss();
                        if (!result.isAuthenticated()) {
                            Storage.clearData();
                            finish();
                        } else {
                            Storage.saveUserID(result.getUserId());
                            initUI();
                            registerZone();
                        }
                    }
                });
            }
        });
    }

    private void registerZone() {
        SwarmClient.getInstance().startSwarm(new RegisterZoneSwarm("Android"), new SwarmCallback<RegisterZoneSwarm>() {
            @Override
            public void call(final RegisterZoneSwarm result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("RegisterZoneSwarm", result.toString());
                    }
                });
            }
        });
        final String androidId = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.w("UUID", androidId);
        SwarmClient.getInstance().startSwarm(new UDESwarm(androidId), new SwarmCallback<UDESwarm>() {
            @Override
            public void call(final UDESwarm result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("UDESwarm", result.toString());
                    }
                });
            }
        });
    }

    private void initUI() {

        setInfo();
        View.OnClickListener scanListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScannerActivity.start(MainActivity.this);
            }
        };
        View.OnClickListener identitiesListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IdentitiesActivity.start(MainActivity.this);
            }
        };
        findViewById(R.id.apps_rl).setOnClickListener(scanListener);
//        findViewById(R.id.btn_identities).setOnClickListener(identitiesListener);
        findViewById(R.id.real_identity_rl).setOnClickListener(identitiesListener);
        findViewById(R.id.notifications_rl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationsActivity.start(MainActivity.this);
            }
        });
        findViewById(R.id.btn_pfb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PFBActivity.start(MainActivity.this);
            }
        });
        findViewById(R.id.btn_browser).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                KotlinBrowserActivityKt.start(MainBrowserActivity.this);
                startActivity(new Intent(MainActivity.this, MainBrowserActivity.class));
            }
        });

        findViewById(R.id.btn_osp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                KotlinBrowserActivityKt.start(MainBrowserActivity.this);
                startActivity(new Intent(MainActivity.this, OSPSettingsActivity.class));
            }
        });
    }

    private void setMenu() {

        setToolbar();
        setRecyclerViewDrawerList();

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);
        drawerToggle.setDrawerIndicatorEnabled(true);
        drawerLayout.addDrawerListener(drawerToggle);

        drawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void setToolbar() {
        toolbar = (Toolbar) findViewById(R.id.activity_main_screen_toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().show();

            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                    if (drawer != null) {
                        drawer.openDrawer(GravityCompat.START);
                    }
                }
            });
        }
    }

    private void setRecyclerViewDrawerList() {

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (RecyclerView) findViewById(R.id.drawer_rv);

//        int width = getResources().getDisplayMetrics().widthPixels;
//        DrawerLayout.LayoutParams params = (android.support.v4.widget.DrawerLayout.LayoutParams) drawerLayout.getLayoutParams();
//        params.width = width;
//        drawerList.setLayoutParams(params);

        DrawerRecyclerViewAdapter recyclerViewAdapter = new DrawerRecyclerViewAdapter(this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        drawerList.setLayoutManager(linearLayoutManager);
        drawerList.setAdapter(recyclerViewAdapter);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    private void setInfo() {
        ((TextView) findViewById(R.id.real_identity)).setText(Storage.readUserID());
        showUnsafeApps();
        initNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initNotifications();
    }

    private void initNotifications() {
        SwarmClient.getInstance().startSwarm(new GetNotificationsSwarm(),
                new SwarmCallback<GetNotificationsSwarm>() {
                    @Override
                    public void call(final GetNotificationsSwarm result) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String text = "You have " + result.getNotifications().size() + " notifications";
                                ((TextView) findViewById(R.id.tv_notifications)).setText(text);
                            }
                        });
                    }
                });
    }

    private void showUnsafeApps() {
        List<InstalledApp> installedApps = PermissionUtils.getApps(this, false);
        if (installedApps != null) {
            ((TextView) findViewById(R.id.tv_installed_apps)).setText(getString(R.string.installed_apps) + " " + installedApps.size());
            int unsafe = 0;
            for (InstalledApp app : installedApps)
                if (app.getPollutionScore() > PermissionUtils.SAFE_THRESHOLD) unsafe++;
            ((TextView) findViewById(R.id.unsafe_apps)).setText(getString(R.string.potentially_unsafe_apps) + " " + unsafe);
            if (unsafe == 0)
                ((TextView) findViewById(R.id.unsafe_apps)).setTextColor(getResources().getColor(android.R.color.holo_green_light));

        }
        Storage.saveAppList(installedApps);
    }

//    private void initMenu() {
//        resideMenu = new ResideMenu(this);
//        resideMenu.setBackground(R.drawable.bg);
//        resideMenu.attachToActivity(this);
//        resideMenu.setScaleValue(0.9f);
//        resideMenu.setUse3D(true);
//
//        View.OnClickListener menuClickListener = new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                int index = (int) v.getTag();
//                onDrawerItemClicked(index);
//            }
//        };
//
//
//        String[] titles = new String[]{
////                "Trusted apps",
//                "About",
//                "Privacy Policy",
//                "Settings",
//                "Feedback"
//        };
//        int[] icons = new int[]{
////                R.drawable.ic_trusted,
//                R.drawable.ic_action_about,
//                R.drawable.ic_privacy_policy,
//                R.drawable.ic_settings,
//                R.drawable.ic_feedback
//        };
//
//        for (int i = 0; i < titles.length; i++) {
//            ResideMenuItem item = new ResideMenuItem(this, icons[i], titles[i]);
//            item.setOnClickListener(menuClickListener);
//            item.setTag(i);
//            resideMenu.addMenuItem(item, ResideMenu.DIRECTION_LEFT);
//        }
//
//        findViewById(R.id.ic_menu).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                resideMenu.openMenu(ResideMenu.DIRECTION_LEFT);
//            }
//        });
//
//        findViewById(R.id.ic_profile).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                resideMenu.openMenu(ResideMenu.DIRECTION_RIGHT);
//            }
//        });
//
//        ResideMenuItem logoutItem = new ResideMenuItem(this, R.drawable.ic_logout, "Log Out");
//        logoutItem.setTag(titles.length);
//        logoutItem.setOnClickListener(menuClickListener);
//        resideMenu.addMenuItem(logoutItem, ResideMenu.DIRECTION_RIGHT);
//
//    }

    private void onDrawerItemClicked(int index) {
//        Toast.makeText(this, index + "", Toast.LENGTH_SHORT).show();
        switch (index + 1) {
//            case 0: //Trusted Apps
//                Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show();
////                TrustedAppsActivity.start(this);
//                break;
            case 1: //About
                HtmlActivity.start(this, "file:///android_asset/about.html", "About PlusPrivacy");
                break;
            case 2: //Privacy Policy
                HtmlActivity.start(this, "file:///android_asset/privacy_policy.html", "Privacy Policy");
                break;
            case 3: //Settings
                SettingsActivity.start(this);
                break;
            case 4: //Feedback
                startFeedbackActivity();
                break;
            case 5: //LogOut
                logOut();
                break;
        }
    }

    private void startFeedbackActivity() {
        startActivity(new Intent(MainActivity.this, FeedbackActivity.class));
    }

    private void logOut() {
        Toast.makeText(this, "Log Out", Toast.LENGTH_SHORT).show();
        SwarmService.getInstance().logout(null);
        Storage.clearData();
        LoginActivity.start(MainActivity.this);
        finish();
    }

//    @Override
//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        return resideMenu.dispatchTouchEvent(ev);
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SwarmService.getInstance().logout(null);
    }

    @Override
    public void selectItem(int position) {

        if (position > 0) {
            switch (position) {

                case 1: //About
                    HtmlActivity.start(this, "file:///android_asset/about.html", "About PlusPrivacy");
                    break;
                case 2: //Privacy Policy
                    HtmlActivity.start(this, "file:///android_asset/privacy_policy.html", "Privacy Policy");
                    break;
                case 3: //Settings
                    SettingsActivity.start(this);
                    break;
                case 4: //Feedback
                    startFeedbackActivity();
                    break;
                case 5: //LogOut
                    logOut();
                    break;
            }
            drawerLayout.closeDrawer(Gravity.LEFT);

        }
    }
}
