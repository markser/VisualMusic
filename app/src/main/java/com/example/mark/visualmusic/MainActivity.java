package com.example.mark.visualmusic;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import android.app.*;
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
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.microsoft.projectoxford.face.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
    private final int PICK_IMAGE = 1;
    private ProgressDialog detectionProgressDialog;

    // private fields for the Microsoft Face API
    private static final String subscriptionKey = "b9a0a727d32a4f6d8e19c2271ecb9856";
    private static final String url = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0/detect?returnFaceId=false&returnFaceLandmarks=false&returnFaceAttributes=emotion";
    private static String TAG = "LAB";
    private static final String imageWithFaces =
            "https://thenypost.files.wordpress.com/2016/05/north_korea_the_real_kim.jpg?quality=90&strip=all&w=443";


    private final FaceServiceClient faceServiceClient =
            new FaceServiceRestClient(url, subscriptionKey);

    private static String outputEmotion = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GET();
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
        detectionProgressDialog = new ProgressDialog(this);
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

                // Comment out for tutorial
//                GET();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void GET() {
        try {
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            //Creating a json field for the Request body
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("url", imageWithFaces);
            final String mRequestBody = jsonBody.toString();

            JsonArrayRequest jsonObjectRequest = new JsonArrayRequest
                    (Request.Method.POST, url, null, new Response.Listener<JSONArray>() {
                        @Override
                        //This is what we are getting back as a JSON
                        //TODO need to parse the object to get the emotions and figure out the highest emotion and store it
                        public void onResponse(final JSONArray response) {
                            try {

                                Log.e(TAG,response.toString());
                                for (int i = 0; i < response.length(); i++) {
                                    //In this loop, you will parse all the array elements inside list array
                                    JSONObject listObj1 = new JSONObject(response.get(i).toString());
//                                    String FA = listObj1.getString("faceAttributes");
//                                    Log.e("FA", FA);
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
                                    Log.e(TAG,maxKey);

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
                    params.put("Content-Type", "application/json");
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
                //don't worry about it
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                //this is the request body that has the url of the image, don't worry about it
                public byte[] getBody() {
                    try {
                        return mRequestBody == null ? null : mRequestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", mRequestBody, "utf-8");
                        return null;
                    }
                }
            };
            //adds everything to the requestQueue
            requestQueue.add(jsonObjectRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
