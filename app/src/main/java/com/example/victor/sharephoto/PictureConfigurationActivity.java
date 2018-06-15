package com.example.victor.sharephoto;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.provider.SyncStateContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PictureConfigurationActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {


    public final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 1;

    // for security permissions
    @LoginSuccessActivity.DialogType
    private int mDialogType;
    private String mRequestPermissions = "We are requesting the camera and Gallery permission as it is absolutely necessary for the app to perform it\'s functionality.\nPlease select \"Grant Permission\" to try again and \"Cancel \" to exit the application.";
    private String mRequestSettings = "You have rejected the camera and Gallery permission for the application. As it is absolutely necessary for the app to perform you need to enable it in the settings of your device.\nPlease select \"Go to settings\" to go to application settings in your device and \"Cancel \" to exit the application.";
    private String mGrantPermissions = "Grant Permissions";
    private String mCancel = "Cancel";
    private String mGoToSettings = "Go To Settings";

    private TextView locationTextView;
    private ImageView placeImageView;

    private String lastLocation;
    private Editable writeSomething;
    private List<Address> addresses;
    private FusedLocationProviderClient client;
    private Geocoder geocoder;
    private StorageReference mStorageRef;
    private Uri photoURI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_configuration);

        Bitmap imageBitmap = getIntent().getParcelableExtra("thumbnail");

        photoURI = getIntent().getParcelableExtra("photoURI");

        //Thumbnail
        ImageView thumbnailView = findViewById(R.id.thumbnail);
        thumbnailView.setImageBitmap(imageBitmap);

        //Add Location text label
        locationTextView = findViewById(R.id.location);
        locationTextView.setOnClickListener(this);

        // Add Location image
        placeImageView = findViewById(R.id.place);
        placeImageView.setOnClickListener(this);

        //Image sharing switches
        Switch facebookSwitch = findViewById(R.id.facebook_switch);
        facebookSwitch.setOnCheckedChangeListener(this);

        Switch twitterSwitch = findViewById(R.id.twitter_switch);
        twitterSwitch.setOnCheckedChangeListener(this);

        Switch instagramSwitch = findViewById(R.id.instagram_switch);
        instagramSwitch.setOnCheckedChangeListener(this);

        //Edit text
        EditText writeSomethingEditText = findViewById(R.id.write_something);
        writeSomething = writeSomethingEditText.getText();

        //Location services
        client = LocationServices.getFusedLocationProviderClient(this);

        //Geocoder
        geocoder = new Geocoder(this, Locale.getDefault());

        //Storage reference to Firebase
        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_finalize, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.finalize:
                uploadPictureToFirebase();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {

        int i = v.getId();

        if (i == R.id.location || i == R.id.place) {
                getLocation();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        int b = buttonView.getId();
        //TODO Implementar la forma de compartir en estas redes sociales
        if (b == R.id.facebook_switch) {
            if (isChecked) {
                // The toggle is enabled
                Toast.makeText(PictureConfigurationActivity.this, "Activado FB", Toast.LENGTH_SHORT).show();
            } else {
                // The toggle is disabled
                Toast.makeText(PictureConfigurationActivity.this, "Desactivado FB", Toast.LENGTH_SHORT).show();
            }
        }

        if (b == R.id.twitter_switch) {
            if (isChecked) {
                // The toggle is enabled
                Toast.makeText(PictureConfigurationActivity.this, "Activado Twitter", Toast.LENGTH_SHORT).show();
            } else {
                // The toggle is disabled
                Toast.makeText(PictureConfigurationActivity.this, "Desactivado Twitter", Toast.LENGTH_SHORT).show();
            }


        }
        if (b == R.id.instagram_switch) {
            if (isChecked) {
                // The toggle is enabled
                Toast.makeText(PictureConfigurationActivity.this, "Activado Instagram", Toast.LENGTH_SHORT).show();
            } else {
                // The toggle is disabled
                Toast.makeText(PictureConfigurationActivity.this, "Desactivado Instagram", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //CHECK PERMISSIONS
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    getLocation();
                } else {
                    boolean showRationale1 = shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION);
                    boolean showRationale2 = shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION);
                    if (showRationale1 && showRationale2) {
                        //explain to user why we need the permissions
                        mDialogType = LoginSuccessActivity.DialogType.DIALOG_DENY;
                        // Show dialog with
                        LoginSuccessActivity.openAlertDialog(mRequestPermissions, mGrantPermissions, mCancel, (OnDialogButtonClickListener) this, PictureConfigurationActivity.this);
                    } else {
                        //explain to user why we need the permissions and ask him to go to settings to enable it
                        mDialogType = LoginSuccessActivity.DialogType.DIALOG_NEVER_ASK;
                        LoginSuccessActivity.openAlertDialog(mRequestSettings, mGoToSettings, mCancel, (OnDialogButtonClickListener) this, PictureConfigurationActivity.this);
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkMultiplePermissions(int permissionCode, Context context) {

        String[] PERMISSIONS = {android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION};
        if (!hasPermissions(context, PERMISSIONS)) {
            ActivityCompat.requestPermissions((Activity) context, PERMISSIONS, permissionCode);
        } else {
            getLocation();
        }
    }

    private boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            checkMultiplePermissions(REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS, PictureConfigurationActivity.this);
        }
        else{
        client.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();

                        try {
                            addresses = geocoder.getFromLocation(
                                        location.getLatitude(),
                                        location.getLongitude(),1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (addresses == null || addresses.size()  == 0) {
                            Toast.makeText(PictureConfigurationActivity.this, "No addresses availables", Toast.LENGTH_SHORT).show();
                        } else {
                            Address address = addresses.get(0);

                            ArrayList<String> addressFragments = new ArrayList<String>();

                            // Fetch the address lines using getAddressLine,
                            // join them, and send them to the thread.
                            for(int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                                addressFragments.add(address.getAddressLine(i));
                            }
                            if(addressFragments.size() > 1){
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    lastLocation = String.join(", ", addressFragments);
                                }else{
                                     lastLocation = addressFragments.get(0);
                                }
                            }else{
                                 lastLocation = addressFragments.get(0);
                            }
                        }
                        //Hide the place image icon
                        placeImageView.setVisibility(View.INVISIBLE);

                        //Change the default value of the value by the last location
                        locationTextView.setText(lastLocation);
                    }
                });
            }
        }

    private void uploadPictureToFirebase(){
        StorageReference riversRef = mStorageRef.child("images/" + photoURI.getLastPathSegment());

        riversRef.putFile(photoURI)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(PictureConfigurationActivity.this, "Fail uploading picture", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
