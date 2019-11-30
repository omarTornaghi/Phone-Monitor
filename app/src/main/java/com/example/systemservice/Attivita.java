package com.example.systemservice;

public class Attivita {
    private String pack, descrizione, data;

    public Attivita() {
    }

    public Attivita(String pack, String descrizione, String data) {
        this.pack = pack;
        this.descrizione = descrizione;
        this.data = data;
    }

    public String getPack() {
        return pack;
    }

    public void setPack(String pack) {
        this.pack = pack;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

}
