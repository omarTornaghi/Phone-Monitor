package com.example.systemservice;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpPostRequest extends AsyncTask<String, Void, String> {
    @Override
    protected void onPostExecute(String response) {
        super.onPostExecute(response);
    }

    @Override
    protected String doInBackground(String... params) {
        //Prendo i campi
        String stringUrl = params[0];
        int numeroChiavi = Integer.parseInt(params[1]);
        URL url = null;
        StringBuffer response = new StringBuffer();
        try {
            url = new URL(stringUrl);
        } catch (Exception ex) {
            Log.d("++++", "URL non corretto");
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) (url.openConnection());
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            try {
                Uri.Builder builder = new Uri.Builder();
                int i = 2;
                int cont = 0;
                while (cont < numeroChiavi) {
                    //Parametro
                    builder.appendQueryParameter(params[i], params[i + 1]);
                    i = i + 2;
                    cont++;
                }
                String query = builder.build().getEncodedQuery();

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, StandardCharsets.UTF_8));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();
            } catch (Exception exce) {
                Log.e("++++", exce.toString());
            }
            try {
                conn.connect();
            } catch (Exception exce) {
                Log.d("++++", exce.getMessage());
            }
            int responseCode = conn.getResponseCode();
            Log.d("++++", "POST Response Code :: " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) { //success
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        conn.getInputStream()));
                String inputLine;


                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

            } else {
                Log.d("++++", "POST request not worked");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response.toString();
    }
}
