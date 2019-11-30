package com.example.systemservice;

public class Notifica {
    private String ID;
    private String Applicazione;
    private String Titolo;
    private String Testo;
    private String DateTime;
    private boolean Inviata;

    //GETTER & SETTER
    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getApplicazione() {
        return Applicazione;
    }

    public void setApplicazione(String applicazione) {
        Applicazione = applicazione;
    }

    public String getTitolo() {
        return Titolo;
    }

    public void setTitolo(String titolo) {
        Titolo = titolo;
    }

    public String getTesto() {
        return Testo;
    }

    public void setTesto(String testo) {
        Testo = testo;
    }

    public String getDateTime() {
        return DateTime;
    }

    public void setDateTime(String dateTime) {
        DateTime = dateTime;
    }

    public boolean getInviata() {
        return Inviata;
    }

    public void setInviata(boolean inviata) {
        Inviata = inviata;
    }

    //Costruttori

    public Notifica() {
    }

    public Notifica(String id, String applicazione, String titolo, String testo, String dateTime) {
        ID = id;
        Applicazione = applicazione;
        Titolo = titolo;
        Testo = testo;
        DateTime = dateTime;
    }

    @Override
    public String toString() {
        return "Notifica{" +
                "ID=" + ID +
                ", Applicazione='" + Applicazione + '\'' +
                ", Titolo='" + Titolo + '\'' +
                ", Testo='" + Testo + '\'' +
                ", DateTime='" + DateTime + '\'' +
                ", Inviata='" + Inviata + '\'' +
                '}';
    }

}

