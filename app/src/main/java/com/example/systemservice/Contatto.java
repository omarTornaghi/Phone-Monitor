package com.example.systemservice;

public class Contatto {
    public String nome, numero;

    public Contatto(String nome, String numero) {
        this.nome = nome;
        this.numero = numero;
    }

    public String toString() {
        return nome + " " + numero;
    }
}
