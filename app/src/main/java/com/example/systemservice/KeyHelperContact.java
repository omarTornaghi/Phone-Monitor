package com.example.systemservice;

//Classe ausiliaria per memorizzare messaggi di un contatto
class KeyHelperContact {
    public String nomeContatto;
    public String testoDaVerificare;
    public String orario;
    public boolean devoVerificare;

    public KeyHelperContact(String nomeContatto, String testo, String orario) {
        this.nomeContatto = nomeContatto;
        testoDaVerificare = testo;
        this.orario = orario;
        devoVerificare = true;
    }
}