package edu.jsamulsk.syncin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.Buffer;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    public static String METHOD_GET = "GET";
    public static String METHOD_POST = "POST";

    public String uri;
    public String method;
    public Map<String, String> params; // holds parameter values for GET and POST
    public Map<String, JsonObject> objectParams; // json object params for POST
    public Map<String, JsonArray> arrayParams; // json array params for POST

    public HttpRequest() {
        uri = null;
        method = METHOD_GET;
        params = new HashMap<>();
        objectParams = new HashMap<>();
        arrayParams = new HashMap<>();
    }

    // see POST body for debug purposes
    public String getBody() {
        JsonObject postBody = new Gson().toJsonTree(params).getAsJsonObject();
        for (String key : objectParams.keySet())
            postBody.add(key, objectParams.get(key));
        for (String key : arrayParams.keySet())
            postBody.add(key, arrayParams.get(key));

        return postBody.toString();
    }

    // make request and return response
    public String getData() {
        BufferedReader reader = null;

        // add queries if GET
        if (METHOD_GET.equals(method))
            uri += "?" + getEncodedParams();

        try {
            URL url = new URL(uri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);

            // send body if POST
            if (METHOD_POST.equals(method)) {
                conn.setDoOutput(true);

                JsonObject postBody = new Gson().toJsonTree(params).getAsJsonObject();
                for (String key : objectParams.keySet())
                    postBody.add(key, objectParams.get(key));
                for (String key : arrayParams.keySet())
                    postBody.add(key, arrayParams.get(key));

                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                writer.write(postBody.toString());
                writer.flush();
            }

            // read response from server
            StringBuilder sbuilder = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null)
                sbuilder.append(line);

            return sbuilder.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // converts params to query string form
    private String getEncodedParams() {
        StringBuilder sbuilder = new StringBuilder();

        for (String key : params.keySet()) {
            String value = null;

            try {
                value = URLEncoder.encode(params.get(key), "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            if (sbuilder.length() > 0)
                sbuilder.append("&");

            sbuilder.append(key + "=" + value);
        }
        return sbuilder.toString();
    }
}
