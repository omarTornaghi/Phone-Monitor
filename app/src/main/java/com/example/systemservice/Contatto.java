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

    public static String normalizzaNumero(String numero) {
        if (numero.length() > 0) {
            numero = numero.replaceAll("\\s", "");
            if (numero.charAt(0) == '+') {
                return numero;
            } else
                return "+39" + numero;
        } else
            return numero;
    }

}
