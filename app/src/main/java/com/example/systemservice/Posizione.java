package com.example.systemservice;

public class Posizione {
    String indirizzo, data;
    double latitudine, longitudine;


    public Posizione(String indirizzo, double latitudine, double longitudine, String data) {
        this.indirizzo = indirizzo;
        this.latitudine = latitudine;
        this.longitudine = longitudine;
        this.data = data;
    }

    public String toString() {
        return indirizzo + " " + latitudine + " " + longitudine + " " + data;
    }

    public Posizione() {
    }
}

