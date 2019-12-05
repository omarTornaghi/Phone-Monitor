package com.example.systemservice;

public class Messaggio {
    public String mittente, messaggio, tipo, data;

    public Messaggio(String mittente, String messaggio, String tipo, String data) {
        this.mittente = mittente;
        this.messaggio = messaggio;
        this.tipo = tipo;
        this.data = data;
    }

    public String toString() {
        return mittente + " " + messaggio + " " + tipo + " " + data;
    }
}
