package krunal.com.example.cameraapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/*import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;*/

@IgnoreExtraProperties

public class ReportActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 1;

    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.fileprovider";

    private AppExecutor mAppExcutor;

    private Button mStartCamera;
    private Button submitstep;

    private String mTempPhotoPath;

    private FirebaseFirestore db;

    private String db_id;
    private double co2= 0;
    private double eco = 0;

    private FloatingActionButton mClear,mSave,mShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        //Button Bill_btn= (Button) findViewById(R.id.startCamera);

        // Update database
        db = FirebaseFirestore.getInstance();

        // Get username from Sign In Page
        Intent intent = getIntent();
        String username = intent.getStringExtra("name");
        Log.i("Evergreen_Name", username);

        // Check if user exists
        CollectionReference collref = db.collection("users");
        Query query = collref.whereEqualTo("name", username);

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    Log.d("Evergreen_ReadDB", "Task Successful");

                    for (QueryDocumentSnapshot document : task.getResult()) {

                        db_id = document.getId();
                        Log.d("Evergreen_ReadDB", document.getId() + " => " + document.getData());
                    }
                } else {
                    // Create a new user with a first and last name
                    Map<String, Object> user = new HashMap<>();
                    user.put("name", username);
                    user.put("gas", 0);
                    user.put("steps", 0);

                    // Add a new document with a generated ID
                    db.collection("users")
                            .add(user)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Log.d("Evergreen_CreateDB", "DocumentSnapshot added with ID: " + documentReference.getId());
                                    db_id =  documentReference.getId();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w("Evergreen_CreateDB", "Error adding document", e);
                                }
                            });
                }
            }
        });


        mAppExcutor = new AppExecutor();
        mStartCamera = findViewById(R.id.startCamera);

        mStartCamera.setOnClickListener(v -> {
            // Check for the external storage permission
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // If you do not have permission, request it
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            } else {
                // Launch the camera if the permission exists
                Log.d("Evergreen_Camera", "Launching camera");
                launchCamera();
            }
        });

    }

    /** Called when the user touches the button */
    public void sendMessage(View view) {
        // Do something in response to button click
        launchCamera();
    }
    public void sendSteps(View view){
        db = FirebaseFirestore.getInstance();
        // Update the document value
        DocumentReference docIdRef = db.collection("users").document(db_id);
        EditText step_val= (EditText) findViewById(R.id.step_value);
        int steps = Integer.parseInt(step_val.getText().toString());
        docIdRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @SuppressLint({"SetTextI18n", "DefaultLocale"})
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    assert document != null;
                    if (document.exists()) {
                        Map<String, Object> user = document.getData();
                        assert user != null;
                        user.put("steps", (Long) user.get("steps") + steps);

                        setContentView(R.layout.activity_report);

                        co2=(Long)user.get("gas");
                        co2 = Math.floor((co2*20)/2000);//1 gallon produces 20 pound of CO2 and each tree absorbs 2000 pound of CO2 in 40 years span.
                        TextView co2value= (TextView) findViewById(R.id.co2value);
                        co2value.setText(String.format("%d", (int)co2));

                        eco = (Long)user.get("steps");
                        eco=Math.floor(((eco*20)/(2000*25))/2000);//2000 steps is 1 mile and average mpg of a car is 25
                        TextView ecovalue = (TextView) findViewById(R.id.ecovalue);
                        ecovalue.setText(String.format("%d", (int)eco));
                        Log.i("Evergreen_Steps", String.format("Steps added %d",  steps));

                        // Add a new document with a generated ID
                        db.collection("users").document(db_id)
                                .set(user)
                                .addOnSuccessListener(new OnSuccessListener<Void>()  {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d("Evergreen_CreateDB", "DocumentSnapshot added with ID: " + db_id);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w("Evergreen_CreateDB", "Error adding document", e);
                                    }
                                });

                    } else {
                        Log.d("Evergreen_UpdateDB", "Document does not exist!");
                    }
                } else {
                    Log.d("Evergreen_UpdateDB", "Failed with: ", task.getException());
                }
            }
        });
        Toast.makeText(this,"Steps added",Toast.LENGTH_LONG).show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Called when you request permission to read and write to external storage
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // If you get permission, launch the camera
                    launchCamera();
                } else {
                    // If you do not get permission, show a Toast
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the image capture activity was called and was successful
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Process the image and set it to the TextView
            processAndSetImage();
        } else {

            // Otherwise, delete the temporary image file
            BitmapUtils.deleteImageFile(this, mTempPhotoPath);
        }
    }

    /**
     * Creates a temporary image file and captures a picture to store in it.
     */
    public void launchCamera() {

        // Create the capture image intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the temporary File where the photo should go
            File photoFile = null;
            try {
                photoFile = BitmapUtils.createTempImageFile(this);
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                // Get the path of the temporary file
                mTempPhotoPath = photoFile.getAbsolutePath();

                // Get the content URI for the image file
                Uri photoURI = FileProvider.getUriForFile(this,
                        FILE_PROVIDER_AUTHORITY,
                        photoFile);

                // Add the URI so the camera can store the image
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                // Launch the camera activity
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }


    /**
     * Method for processing the captured image and setting it to the TextView.
     */
    private void processAndSetImage() {

        // Toggle Visibility of the views
        mStartCamera.setVisibility(View.VISIBLE);

        // Resample the saved image to fit the ImageView
        Bitmap mResultsBitmap = BitmapUtils.resamplePic(this, mTempPhotoPath);
        // Save the image
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);
        FirebaseVisionImage image =
                FirebaseVisionImage.fromBitmap(mResultsBitmap);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();

        Task<FirebaseVisionText> result =
                detector.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                // Task completed successfully
                                processTextRecognitionResult(firebaseVisionText);
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
        BitmapUtils.saveImage(this, mResultsBitmap);
        Toast.makeText(this,"Image Save",Toast.LENGTH_LONG).show();
    }

    private void processTextRecognitionResult(FirebaseVisionText result) {
        String resultText = result.getText();
        Integer gas_val = 0;
        for (FirebaseVisionText.TextBlock block: result.getTextBlocks()) {
            String blockText = block.getText();

            if (blockText.contains("GALLONS")) {
                String disp[] = blockText.split("\n");
                Log.i("Evergreen_Block", blockText);
                gas_val = Integer.valueOf(disp[1]);
            }
            Float blockConfidence = block.getConfidence();
            List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
            Point[] blockCornerPoints = block.getCornerPoints();
            Rect blockFrame = block.getBoundingBox();
            for (FirebaseVisionText.Line line: block.getLines()) {
                String lineText = line.getText();
                Float lineConfidence = line.getConfidence();
                List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
                Point[] lineCornerPoints = line.getCornerPoints();
                Rect lineFrame = line.getBoundingBox();
                for (FirebaseVisionText.Element element: line.getElements()) {
                    String elementText = element.getText();
                    Float elementConfidence = element.getConfidence();
                    List<RecognizedLanguage> elementLanguages = element.getRecognizedLanguages();
                    Point[] elementCornerPoints = element.getCornerPoints();
                    Rect elementFrame = element.getBoundingBox();
                }
            }
        }

        db = FirebaseFirestore.getInstance();
        // Update the document value
        DocumentReference docIdRef = db.collection("users").document(db_id);
        Integer finalGas_val = gas_val;
        docIdRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @SuppressLint({"SetTextI18n", "DefaultLocale"})
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    assert document != null;
                    if (document.exists()) {
                        Map<String, Object> user = document.getData();
                        assert user != null;
                        user.put("gas", (Long) user.get("gas") + finalGas_val);

                        setContentView(R.layout.activity_report);

                        co2=(Long)user.get("gas");
                        co2 = Math.floor((co2*20)/2000);//1 gallon produces 20 pound of CO2 and each tree absorbs 2000 pound of CO2 in 40 years span.
                        TextView co2value= (TextView) findViewById(R.id.co2value);
                        co2value.setText(String.format("%d", (int)co2));

                        eco = (Long)user.get("steps");
                        eco=Math.floor(((eco*20)/(2000*25))/2000);//2000 steps is 1 mile and average mpg of a car is 25
                        TextView ecovalue = (TextView) findViewById(R.id.ecovalue);
                        ecovalue.setText(String.format("%d", (int)eco));


                        // Add a new document with a generated ID
                        db.collection("users").document(db_id)
                                .set(user)
                                .addOnSuccessListener(new OnSuccessListener<Void>()  {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d("Evergreen_CreateDB", "DocumentSnapshot added with ID: " + db_id);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w("Evergreen_CreateDB", "Error adding document", e);
                                    }
                                });

                    } else {
                        Log.d("Evergreen_UpdateDB", "Document does not exist!");
                    }
                } else {
                    Log.d("Evergreen_UpdateDB", "Failed with: ", task.getException());
                }
            }
        });

    }
}