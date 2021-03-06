package com.example.audionote;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;

import android.util.Log;
import android.view.View;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.toolbox.Volley;
import com.android.volley.request.SimpleMultiPartRequest;

import org.json.JSONException;
import org.json.JSONObject;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class RecordActivity extends AppCompatActivity {

    Button btn_start_record, btn_stop_record, btn_play, btn_stop, btn_upload;
    String pathSaveInDevice = "";
    String RandomAudioFileName = "ABCDEFGHIJKLMNOP";
    String url = "https://audionoteucsb.herokuapp.com/transcription";
    MediaRecorder mediaRecorder;
    MediaPlayer mediaPlayer;
    Random random;
    static final int RequestPermissionCode = 1;
    static String audioNameOri = "";
    static String audioNameMod = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        btn_start_record = (Button) findViewById(R.id.btn_start_record);
        btn_stop_record = (Button) findViewById(R.id.btn_stop_record);
        btn_play = (Button) findViewById(R.id.btn_play);
        btn_stop = (Button) findViewById(R.id.btn_stop);
        btn_upload = (Button) findViewById(R.id.btn_upload);


        btn_stop_record.setEnabled(false);
        btn_play.setEnabled(false);
        btn_stop.setEnabled(false);

        random = new Random();

        btn_start_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission()) {
                    audioNameOri = CreateRandomAudioFileName(5);
                    pathSaveInDevice = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + audioNameOri + ".mp4";
                    MediaRecorderReady();
                    try {
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    btn_start_record.setEnabled(false);
                    btn_stop_record.setEnabled(true);
                    Toast.makeText(RecordActivity.this, "Recording started", Toast.LENGTH_LONG).show();
                }
                else {
                    requestPermission();
                }
            }
        });

        btn_stop_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaRecorder.stop();
                btn_stop.setEnabled(false);
                btn_stop_record.setEnabled(false);
                btn_play.setEnabled(true);
                btn_start_record.setEnabled(true);
                Toast.makeText(RecordActivity.this, "Recording Completed", Toast.LENGTH_LONG).show();

            }
        });

        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) throws IllegalStateException, IllegalArgumentException, SecurityException{
                btn_stop.setEnabled(true);
                btn_stop_record.setEnabled(false);
                btn_start_record.setEnabled(false);
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(pathSaveInDevice);
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mediaPlayer.start();
                Toast.makeText(RecordActivity.this, "Recording Playing", Toast.LENGTH_LONG).show();
            }
        });

        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_stop.setEnabled(false);
                btn_start_record.setEnabled(true);
                btn_play.setEnabled(true);
                btn_stop_record.setEnabled(false);
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    MediaRecorderReady();
                }
            }
        });

        //button to upload file
        btn_upload.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                String filePath = pathSaveInDevice;
                try {
                    System.out.println("uploading audio: " + filePath);
                    upload(filePath);
                } catch (AuthFailureError authFailureError) {
                    authFailureError.printStackTrace();
                }
            }
        });
    }

    public void upload(String filePath) throws AuthFailureError {
        btn_upload.setText("Uploading...");
        SimpleMultiPartRequest smr = new SimpleMultiPartRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Response", response);
                        try {
                            JSONObject this_json = new JSONObject(response);
                            String jobID = this_json.getString("transcription_job");
                            Transcript this_transcript = new Transcript(jobID);
                            TranscriptManager.saveNewTranscript(this_transcript);
                            System.out.println("Saved recording");
                            finish();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                    }
        }){
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("Token", "1234");
                return headers;
            }
        };
        smr.addFile("audio", filePath);
        RequestQueue mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        mRequestQueue.add(smr);
        System.out.println("bottom upload");
    }

    public void MediaRecorderReady() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(pathSaveInDevice);
    }

    public String CreateRandomAudioFileName(int string){
        StringBuilder stringBuilder = new StringBuilder( string );
        int i = 0 ;
        while(i < string ) {
            stringBuilder.append(RandomAudioFileName.
                    charAt(random.nextInt(RandomAudioFileName.length())));
            i++ ;
        }
        return stringBuilder.toString();
    }


    private void requestPermission() {
        ActivityCompat.requestPermissions(RecordActivity.this, new String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, RequestPermissionCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
            switch (requestCode) {
                case RequestPermissionCode:
                    if (grantResults.length > 0) {
                        boolean StoragePermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                        boolean RecordPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                        if (StoragePermission && RecordPermission) {
                            Toast.makeText(RecordActivity.this, "Permission Granted", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RecordActivity.this, "Permission Denied", Toast.LENGTH_LONG).show();
                        }
                    }
                    break;
            }
    }


    public boolean checkPermission() {
            int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                    WRITE_EXTERNAL_STORAGE);
            int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                    RECORD_AUDIO);
            return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void setFileName() {

        String input = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        Random random = new Random();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            int index = random.nextInt(input.length());
            builder.append(input.charAt(index));
        }

        File sdcard = Environment.getExternalStorageDirectory();
        File from = new File(sdcard,audioNameOri+".mp4");
        File to = new File(sdcard,builder.toString()+".mp4");
        from.renameTo(to);
        pathSaveInDevice = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + audioNameMod + ".mp4";
    }


}
