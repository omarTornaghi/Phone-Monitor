package com.example.systemservice;

import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;


public class NotificationReceiver extends NotificationListenerService {

    /*
        Codici notifiche associati
     */
    public static final class InterceptedNotificationCode {
        public static final int FACEBOOK_CODE = 1;
        public static final int WHATSAPP_CODE = 2;
        public static final int INSTAGRAM_CODE = 3;
        public static final int TELEGRAM_CODE = 4;
        public static final int OTHER_NOTIFICATIONS_CODE = 20;
    }

    class ContattoAC {
        public String NomeContatto;
        public String UltimoMessaggio;

        public ContattoAC() {
        }

        public ContattoAC(String nomeContatto, String ultimoMessaggio) {
            NomeContatto = nomeContatto;
            UltimoMessaggio = ultimoMessaggio;
        }
    }

    LinkedList<ContattoAC> UltimiMessaggi = new LinkedList<ContattoAC>();

    //Metodi accessori
    public int TrovaContattoInUltimiMessaggi(String nomeContatto) {
        int pos = 0;
        for (ContattoAC c : UltimiMessaggi) {
            if (c.NomeContatto.equals(nomeContatto))
                return pos;
            pos++;
        }
        return -1;
    }

    public void SettaUltimoMessaggio(int posizione, String ultimoMessaggio) {
        ContattoAC vecchio = UltimiMessaggi.get(posizione);
        vecchio.UltimoMessaggio = new String(ultimoMessaggio);
        UltimiMessaggi.set(posizione, vecchio);
    }

    @Override
    public IBinder onBind(Intent intent) {

        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        //Toast.makeText(getApplicationContext(), "NOTIFICA ARRIVATA", Toast.LENGTH_LONG).show();
        int notificationCode = matchNotificationCode(sbn);
        String pack = "";
        Bundle extras = null;
        String title = "";
        String text = "";
        String subtext = "";
        String key = "";
        try {
            pack = sbn.getPackageName();
            extras = sbn.getNotification().extras;
            //Prendo i dati della notifica
            title = extras.getString("android.title");
            //Parso il titolo
            if (title.contains("(") && title.contains(")") && (title.contains(("messaggi")) || title.contains("messaggio"))) {
                int posA = title.lastIndexOf('(');
                title = title.substring(0, posA - 1);
            }
            text = extras.getCharSequence("android.text").toString();
            subtext = "";
        } catch (Exception ex) {
            Log.d("WHATSAPP RIGA 100", "Conv fallita");
        }
        if (matchNotificationCode(sbn) != InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE) {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)) {
                //EDIT
                /* Used for SendBroadcast */
                try {
                    Parcelable b[] = (Parcelable[]) extras.get(Notification.EXTRA_MESSAGES);
                    String content;
                    if (b != null) {
                        content = "";
                        for (Parcelable tmp : b) {
                            key = String.valueOf(tmp.hashCode());
                            Bundle msgBundle = (Bundle) tmp;
                            subtext = msgBundle.getString("text");
                            //content = content + msgBundle.getString("text") + "\n";
                        }
                    /*Toast.makeText(getApplicationContext(), content,
                            Toast.LENGTH_LONG).show();*/
                    }
                    if (subtext.isEmpty()) {
                        subtext = text;
                    }
                } catch (Exception e) {
                    Log.d("ERRORE RIGA 90 WHATSAPP", "Probabilmente la notifica è sballata");
                }
                Notifica nuovaNotifica = new Notifica();

                if (subtext != null) {
                    if (!text.contains("nuovi messaggi") && !text.contains("WhatsApp Web is currently active") && !text.contains("WhatsApp Web login") && !title.toLowerCase().equals("whatsapp")) {
                        try {
                            DateFormat df = new SimpleDateFormat("ddMMyyyyHHmmss");
                            String date = df.format(Calendar.getInstance().getTime());
                            nuovaNotifica = new Notifica(key, pack, title, subtext, date);
                        } catch (Exception ex) {
                            Log.d("WHATSAPP RIGA 100", "Conversione fallita");
                        }
                        try {
                            //Parso il messaggio se viene da instagram
                            if (matchNotificationCode(sbn) == InterceptedNotificationCode.INSTAGRAM_CODE) {
                                //Elimino l'username nel messaggio
                                if (subtext.contains(":"))
                                    subtext = subtext.substring(subtext.indexOf(':') + 2, subtext.length());
                                else {
                                    if (subtext.contains(title)) {
                                        int ind = subtext.indexOf(title);
                                        subtext = subtext.replace(title, "");
                                        subtext = subtext.substring(1, subtext.length());
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            Log.d("WHATSAPP RIGA 160", "Parser fallito");
                        }
                        //Controllo che non abbia già ricevuto il messaggio
                        String contatto = title + matchNotificationCode(sbn);
                        int posizioneContatto = TrovaContattoInUltimiMessaggi(contatto);
                        boolean daInviare = true;
                        //Se ho trovato il contatto, controllo il valore se è diverso aggiungo e invio
                        //Altrimenti modifico e non invio
                        //Se non lo trovo lo registro e invio
                        if (posizioneContatto != -1) {
                            String strV = UltimiMessaggi.get(posizioneContatto).UltimoMessaggio;
                            if (strV.equals(subtext)) {
                                daInviare = false;
                            } else {
                                SettaUltimoMessaggio(posizioneContatto, subtext);
                            }
                        } else {
                            //Aggiungo il contatto e il messaggio nella lista
                            UltimiMessaggi.add(new ContattoAC(contatto, subtext));
                        }
                        //Invio la notifica alla mainActivity
                        if (daInviare) {
                            Intent intent = new Intent("ricevitoreNotifiche");
                            try {
                                /*//LOG
                                Log.d("Notifica KEY", key);
                                Log.d("Contatto", title);
                                Log.d("Messaggio", subtext);*/
                                intent.putExtra("ID", nuovaNotifica.getID());
                                intent.putExtra("Applicazione", nuovaNotifica.getApplicazione());
                                intent.putExtra("Titolo", nuovaNotifica.getTitolo());
                                intent.putExtra("Testo", nuovaNotifica.getTesto());
                                intent.putExtra("DateTime", nuovaNotifica.getDateTime());
                                sendBroadcast(intent);
                            } catch (Exception ex) {
                                Log.d("ERROR RIGA 103 WHATSAPP", "Non riesco a inviare in broadcast");
                            }
                        }
                    }
                    /* End Used for Toast */
                }
            }
        }

    }

    /*EDIT 24/10/2019 NON TESTATO
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        int notificationCode = matchNotificationCode(sbn);

        if (notificationCode != InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE) {

            StatusBarNotification[] activeNotifications = this.getActiveNotifications();

            if (activeNotifications != null && activeNotifications.length > 0) {
                for (int i = 0; i < activeNotifications.length; i++) {
                    if (notificationCode == matchNotificationCode(activeNotifications[i])) {
                        Intent intent = new Intent("com.example.ssa_ezra.whatsappmonitoring");
                        intent.putExtra("Notification Code", notificationCode);
                        sendBroadcast(intent);
                        break;
                    }
                }
            }
        }
    }*/

    //Associo al nome del package il codice
    private int matchNotificationCode(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        if (packageName.equals(ApplicationPackageNames.FACEBOOK_PACK_NAME)
                || packageName.equals(ApplicationPackageNames.FACEBOOK_MESSENGER_PACK_NAME)) {
            return (InterceptedNotificationCode.FACEBOOK_CODE);
        } else if (packageName.equals(ApplicationPackageNames.INSTAGRAM_PACK_NAME)) {
            return (InterceptedNotificationCode.INSTAGRAM_CODE);
        } else if (packageName.equals(ApplicationPackageNames.WHATSAPP_PACK_NAME)) {
            return (InterceptedNotificationCode.WHATSAPP_CODE);
        } else if (packageName.equals(ApplicationPackageNames.TELEGRAM_PACK_NAME))
            return (InterceptedNotificationCode.TELEGRAM_CODE);
        else {
            return (InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE);
        }
    }
}
