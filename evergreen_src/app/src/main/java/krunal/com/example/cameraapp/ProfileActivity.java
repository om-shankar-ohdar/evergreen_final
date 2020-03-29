package krunal.com.example.cameraapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProfileActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private ImageView profile_image;
    private TextView name;
    private TextView email;
    private Button signout;
    private Button steps;

    private GoogleApiClient googleApiClient;
    private GoogleSignInOptions gso;
    private static int REQUEST_OAUTH_REQUEST_CODE = 0x1001;
    int totalSteps  = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        profile_image = findViewById(R.id.profile_image);
        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        signout = findViewById(R.id.signout);
        steps = findViewById(R.id.steps);

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        googleApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso).build();
        GoogleSignInOptionsExtension fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build();
        final GoogleSignInAccount googleSignInAccount1 =
                GoogleSignIn.getAccountForExtension(this, fitnessOptions);


        signout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if(status.isSuccess()){
                            gotoMainactivity();
                        }else{
                            Toast.makeText(ProfileActivity.this, "Log Out Failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        steps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar cal = (Calendar) Calendar.getInstance();
                Date now = new Date();
                long endTime = cal.getTimeInMillis();
                Date sdate = new Date(now.getTime()-10000);
                long startTime = sdate.getTime();
                DataSet finalData = null;
                DataReadRequest readRequest =
                        new DataReadRequest.Builder().aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA).bucketByTime(1, TimeUnit.DAYS)
                                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                                .build();

                GoogleSignInOptionsExtension fitnessOptions =
                        FitnessOptions.builder()
                                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                                .build();

                GoogleSignIn.requestPermissions(
                        ProfileActivity.this,
                        REQUEST_OAUTH_REQUEST_CODE,
                        GoogleSignIn.getLastSignedInAccount(ProfileActivity.this),
                        fitnessOptions);
                ActivityCompat.requestPermissions(ProfileActivity.this,
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        1);
//            } else {
////                subscribe();
//                System.out.println("not permitted");
//            }
                GoogleSignInAccount googleSignInAccount = GoogleSignIn.getAccountForExtension(ProfileActivity.this, fitnessOptions);
                final Task<DataReadResponse> response = Fitness.getHistoryClient(ProfileActivity.this, GoogleSignIn.getLastSignedInAccount(ProfileActivity.this)).readData(readRequest);
                response.addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        System.out.println("hello this is onClick");
                        List<DataSet> dataSets = dataReadResponse.getDataSets();
                        List<Integer> list = new ArrayList<Integer>();
                        list.add(7);
                        System.out.println(list);
                        System.out.println("Data set: " + dataSets);
                    }
                });
                response.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println(e);
                    }
                });

                Fitness.getHistoryClient(ProfileActivity.this, GoogleSignIn.getLastSignedInAccount(ProfileActivity.this))
                        .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                        .addOnSuccessListener(
                                new OnSuccessListener<DataSet>() {
                                    @Override
                                    public void onSuccess(DataSet dataSet) {
                                        totalSteps =
                                                dataSet.isEmpty()
                                                        ? 326
                                                        : dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                                        Log.i("Steps", "Total steps: " + totalSteps);
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w("TotalSteps", "There was a problem getting the step count.", e);
                                    }
                                });
                startActivity(new Intent(ProfileActivity.this, ReportActivity.class));

            }

        });

    }

    private void gotoMainactivity() {

        startActivity(new Intent(ProfileActivity.this, MainActivity.class));
        finish();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void handleSignInResult(GoogleSignInResult result){
        if(result.isSuccess()){
            GoogleSignInAccount account = result.getSignInAccount();
            name.setText(account.getDisplayName());
            email.setText(account.getEmail());

            Picasso.get().load(account.getPhotoUrl()).placeholder(R.mipmap.ic_launcher).into(profile_image);
        } else {
            gotoMainactivity();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onStart() {
        super.onStart();
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(googleApiClient);

        if(opr.isDone()){
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult result) {
                    handleSignInResult(result);
                }
            });
        }
    }
}
