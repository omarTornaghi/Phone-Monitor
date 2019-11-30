package com.example.systemservice;

public class Documento {
    public String percorso, nome, estensione, data, base64;

    public Documento(String percorso, String nome, String estensione, String data, String base64) {
        this.percorso = percorso;
        this.nome = nome;
        this.estensione = estensione;
        this.data = data;
        this.base64 = base64;
    }

    public String toString() {
        return percorso + " " + nome + " " + estensione + " " + data + " " + base64;
    }
}
