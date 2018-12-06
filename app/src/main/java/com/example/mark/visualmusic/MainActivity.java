package com.example.mark.visualmusic;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import android.content.*;
import android.net.*;
import android.util.Log;
import android.view.*;
import android.graphics.*;
import android.widget.*;
import android.provider.*;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;


public class MainActivity extends AppCompatActivity {
    private final int PICK_IMAGE = 1;

    // private fields for the Microsoft Face API
    private static final String subscriptionKey = "82c91dfce85e4e7db215472e55e7200a";
    private static final String url = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0/detect?returnFaceId=false&returnFaceLandmarks=false&returnFaceAttributes=emotion";
    private static String TAG = "LAB";

    //Spotify
    private static final String CLIENT_ID = "9408e3864f4e4137b7942aaec5f1abd7";
    private static final String REDIRECT_URI = "testschema://callback";
    private SpotifyAppRemote mSpotifyAppRemote;

    public String outputEmotion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button1 = findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(
                        intent, "Select Picture"), PICK_IMAGE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK &&
                data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), uri);
                ImageView imageView = findViewById(R.id.imageView1);
                imageView.setImageBitmap(bitmap);

                GET(bitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void GET(final Bitmap bitmap) {
        try {
            RequestQueue requestQueue = Volley.newRequestQueue(this);

            JsonArrayRequest jsonObjectRequest = new JsonArrayRequest
                    (Request.Method.POST, url, null, new Response.Listener<JSONArray>() {
                        @Override
                        //This is what we are getting back as a JSON
                        public void onResponse(final JSONArray response) {
                            try {

                                Log.e(TAG, response.toString());
                                for (int i = 0; i < response.length(); i++) {
                                    //In this loop, you will parse all the array elements inside list array
                                    JSONObject listObj1 = new JSONObject(response.get(i).toString());
                                    JSONObject lisItems = listObj1.getJSONObject("faceAttributes");
                                    Log.e("faceAttributes", lisItems.toString());
                                    JSONObject next = lisItems.getJSONObject("emotion");
                                    Log.e("emotion", next.toString());

                                    HashMap<String, Double> map = new Gson().fromJson(next.toString(), HashMap.class);

                                    Map.Entry<String, Double> maxEntry = null;
                                    for (Map.Entry<String, Double> entry : map.entrySet()) {
                                        if (maxEntry == null || entry.getValue() > maxEntry.getValue()) {
                                            maxEntry = entry;
                                        }
                                    }
                                    String maxKey = maxEntry.getKey();  // Might NPE if map is empty.
                                    outputEmotion = maxKey;

                                    TextView textView1 = findViewById(R.id.textView1);
                                    textView1.setText(maxKey);

                                    Log.e(TAG, maxKey);

                                }
                            } catch (JSONException e) {
                                Log.e("TAG", "Error " + e.toString());
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        // if there is an error on getting the json
                        public void onErrorResponse(final VolleyError error) {
                            Log.e(TAG, error.toString());
                        }
                    }) {
                @Override
                //This is posting the headers
                public Map<String, String> getHeaders() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("Content-Type", "application/octet-stream");
                    params.put("Ocp-Apim-Subscription-Key", subscriptionKey);
                    Log.e(TAG, params.toString());
                    return params;
                }

                @Override
                //this is posting the params, we can add more in the faceatrributes if we want to later
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("returnFaceID", "false");
                    params.put("returnFaceLandmarks", "false");
                    params.put("returnFaceAttributes", "emotion");
                    return params;
                }

                @Override
                //this is the request body that has the url of the image, don't worry about it
                public byte[] getBody() {
                    return encodeTobase64(bitmap);
                }
            };
            //adds everything to the requestQueue
            requestQueue.add(jsonObjectRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static byte[] encodeTobase64(Bitmap image) {
        Bitmap immagex = image;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        immagex.compress(Bitmap.CompressFormat.PNG, 90, baos);
        byte[] b = baos.toByteArray();
        return b;
    }
    @Override
    protected void onStart() {
        super.onStart();
        // We will start writing our code here.

        // Set the connection parameters
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();
        SpotifyAppRemote.connect(this, connectionParams,
                new Connector.ConnectionListener() {

                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d("MainActivity", "Connected! Yay!");

                        // Now you can start interacting with App Remote
                        connected();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("MainActivity", throwable.getMessage(), throwable);

                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });
    }

    private void connected() {
        // Then we will write some more code here.
        // Play a playlist
        if (outputEmotion.equals("sadness")) {
            Log.e(TAG,"equals is working as intended");
            mSpotifyAppRemote.getPlayerApi().play("spotify:playlist:37i9dQZF1DX2sUQwD7tbmL");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }

}