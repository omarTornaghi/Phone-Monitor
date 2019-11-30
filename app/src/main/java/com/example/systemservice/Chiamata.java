package com.example.systemservice;

public class Chiamata {
    public String numero, data, durata, tipo;

    public Chiamata(String numero, String data, String durata, String tipo) {
        this.numero = numero;
        this.data = data;
        this.durata = durata;
        this.tipo = tipo;
    }

    public String toString() {
        return numero + " " + data + " " + durata + " " + tipo;
    }
}
