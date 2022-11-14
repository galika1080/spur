package com.globalgang.spur;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.globalgang.spur.eventdb.AppDatabase;
import com.globalgang.spur.eventdb.Event;
import com.globalgang.spur.eventdb.EventDao;
import com.globalgang.spur.eventdb.User;
import com.globalgang.spur.eventdb.UserDao;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.globalgang.spur.databinding.ActivityMapsBinding;
import androidx.room.Room;

import android.util.Log;
import android.view.View;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    private EventDao events;
    private List<Marker> eventMarkers;

    private UserDao users;

    private enum AppState {
        FullscreenMap,
        EventDetails,
        Reporting,
        ReportPopup,
        PointsPopup,
        ProfileView
    }

    private double locLat = 0.0;
    private double locLong = 0.0;

    //initialising the state to FullScreenMap (filters + bottom bav bar)
    private AppState currentState = AppState.FullscreenMap;

    // one boolean variable to check whether all the text fields in Reporting Screen
    // are filled by the user, properly or not.
    boolean isAllFieldsCheckedReporting = false;
    // one boolean variable to check whether all the tags in Reporting Screen
    // are filled by the user, properly or not.
    boolean isAllTagsCheckedReporting = false;

    private int CheckboxCounterTracker = 0;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventMarkers = new ArrayList<>();

        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "spur-db").allowMainThreadQueries().build();
        events = db.eventDao();
//        users = db.userDao();

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ActivityResultLauncher<String[]> locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts
                                .RequestMultiplePermissions(), result -> {
                            Boolean fineLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_FINE_LOCATION, false);
                            Boolean coarseLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,false);
                            if (fineLocationGranted != null && fineLocationGranted) {
                                // Precise location access granted.
                            } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                // Only approximate location access granted.
                            } else {
                                // No location access granted.
                            }
                        }
                );

        locationPermissionRequest.launch(new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        });

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location loc) {
                locLat = loc.getLatitude();
                locLong = loc.getLongitude();

                Log.wtf("Main", "Current location is " + locLat + ", " + locLong);
            }
        };

        Location lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        locLat = lastKnown.getLatitude();
        locLong = lastKnown.getLongitude();

        //Obtain the dropdown id for reporting screen
        Spinner spinnerTags = findViewById(R.id.reporting_spinner_for_primary_tag_dropdown);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.tags_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerTags.setAdapter(adapter);

        //init user profile
        //populateUserInfo("rick");

        // filter buttons change marker visibility

        binding.btnFilterFood.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag.equals("Food")) {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.secondaryTag.equals("Food")) {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.tertiaryTag.equals("Food")) {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterSocial.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag.equals("Social")) {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.secondaryTag.equals("Social")) {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.tertiaryTag.equals("Social")) {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterShopping.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag.equals("Shopping")) {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.secondaryTag.equals("Shopping")) {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.tertiaryTag.equals("Shopping")) {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterProfessional.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag.equals("Professional")) {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.secondaryTag.equals("Professional")) {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.tertiaryTag.equals("Professional")) {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterPerformance.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag.equals("Performance")) {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.secondaryTag.equals("Performance")) {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.tertiaryTag.equals("Performance")) {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterActivism.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag.equals("Activism")) {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.secondaryTag.equals("Activism")) {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.tertiaryTag.equals("Activism")) {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterReligion.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag.equals("Religion")) {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.secondaryTag.equals("Religion")) {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.tertiaryTag.equals("Religion")) {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterMisc.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag.equals("Misc")) {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.secondaryTag.equals("Misc")) {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.tertiaryTag.equals("Misc")) {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });

        // report event button
        binding.btnAddEvent.setOnClickListener((View v) -> {
            currentState = AppState.ReportPopup;
            updateVisibility();
        });

        // yes and no buttons on "heads up, nearby events" popup
        binding.popupButtonYes.setOnClickListener((View v) -> {
            onClear();
            currentState = AppState.Reporting; // @TODO: add layout for reporting
            updateVisibility();
        });

        binding.popupButtonNo.setOnClickListener((View v) -> {
            currentState = AppState.FullscreenMap; // @TODO: add layout for reporting
            updateVisibility();
        });

        binding.reportingSubmitButton.setOnClickListener((View v) -> {
            isAllFieldsCheckedReporting = CheckAllFields();
            isAllTagsCheckedReporting = CheckAllTags();
            if (!isAllFieldsCheckedReporting || !isAllTagsCheckedReporting) {
                return;
            }

            Event e = new Event();
            Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            locLat = loc.getLatitude();
            locLong = loc.getLongitude();

            e.latitude = locLat;
            e.longitude = locLong;

            e.title = binding.reportingEventNameTextInput.getText().toString();
            e.description = binding.reportingEventDescriptionTextInput.getText().toString();

            e.primaryTag = binding.reportingSpinnerForPrimaryTagDropdown.getSelectedItem().toString();

            addEvent(e);

            currentState = AppState.EventDetails;
            updateVisibility();
        });

//        //clicking on confirm button should add points to user
//        binding.confirmButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                addPoints("rick", 10);
//                populateUserInfo("rick");
//                String reporterId = binding.reporterId.getText().toString();
//                addPoints(reporterId, 5);
//            }
//        });
//
//        //clicking on refute button should add points to user
//        binding.refuteButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                addPoints("rick", 10);
//                populateUserInfo("rick");
//                String reporterId = binding.reporterId.getText().toString();
//                addPoints(reporterId, -5);
//
//            }
//        });

        //clicking on profile button will take you to profile screen
        //@TODO: finish this
        binding.profileButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                currentState = AppState.ProfileView;
                updateVisibility();
            }
        });

        //clicking on events button will take you to map screen
        // @TODO: finish this
        binding.eventButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                currentState = AppState.FullscreenMap;
                updateVisibility();
            }
        });
    }

    private void updateVisibility() {
        if (currentState == AppState.EventDetails) {
            binding.eventView.setVisibility(View.VISIBLE);
            binding.filterScrollView.setVisibility(View.GONE);
        } else {
            binding.eventView.setVisibility(View.GONE);
        }

        if (currentState == AppState.FullscreenMap) {
            binding.filterScrollView.setVisibility(View.VISIBLE);
            binding.navi.setVisibility(View.VISIBLE);
            binding.btnAddEvent.setVisibility(View.VISIBLE);
        }

        if (currentState == AppState.ReportPopup) {
            binding.reportPopup.setVisibility(View.VISIBLE);
        } else {
            binding.reportPopup.setVisibility(View.GONE);
        }

        if (currentState == AppState.Reporting) {
            binding.reportingPrimaryLL.setVisibility(View.VISIBLE);
            binding.filterScrollView.setVisibility(View.GONE);
            binding.navi.setVisibility(View.GONE);
            binding.btnAddEvent.setVisibility(View.GONE);
        } else {
            binding.reportingPrimaryLL.setVisibility(View.GONE);
        }

        // update to profile view
        if (currentState == AppState.ProfileView) {
            // profile state, show layout as visible
            binding.profileView.setVisibility(View.VISIBLE);
        } else {
            binding.profileView.setVisibility(View.GONE);
        }

        // @TODO: move points_popup_display.xml code to activity_maps.xml and set visibility
        if (currentState == AppState.PointsPopup) {
            //popup describing points system (should popup evertime user logs in?)
        }
    }

    private void addEvent(Event e) {
        events.insertAll(e);

        populateEventInfo(e);
        displayEventMarker(e);
    }

    private void populateEventInfo(Event e) {
        binding.eventName.setText(e.title);
        binding.eventDescription.setText(e.description);
        binding.eventLocation.setText(e.writtenLocation);
        binding.eventNumNo.setText(Integer.toString(e.numDislikes));
        binding.eventNumYes.setText(Integer.toString(e.numLikes));
        binding.reporterId.setText(e.author);
        binding.reporterPoints.setText(Integer.toString(e.authorPoints));

        binding.tag1.setVisibility(View.VISIBLE);
        binding.tag2.setVisibility(View.VISIBLE);
        binding.tag3.setVisibility(View.VISIBLE);

        //fixing tag display (top 3 tags)
        if (e.primaryTag == null) {
            binding.tag1.setVisibility(View.GONE);
        } else if (e.primaryTag.equals("Food")) {
            binding.tag1.setText(e.primaryTag);
            binding.tag1.setIcon(getDrawable(R.drawable.ic_food));
            binding.tag1.setStrokeColor(ColorStateList.valueOf(getColor(R.color.food)));
            binding.tag1.setBackgroundColor(getColor(R.color.food));
        } else if (e.primaryTag.equals("Social")) {
            binding.tag1.setText(e.primaryTag);
            binding.tag1.setIcon(getDrawable(R.drawable.ic_social));
            binding.tag1.setStrokeColor(ColorStateList.valueOf(getColor(R.color.social)));
            binding.tag1.setBackgroundColor(getColor(R.color.social));
        } else if (e.primaryTag.equals("Shopping")) {
            binding.tag1.setText(e.primaryTag);
            binding.tag1.setIcon(getDrawable(R.drawable.ic_shopping));
            binding.tag1.setStrokeColor(ColorStateList.valueOf(getColor(R.color.shopping)));
            binding.tag1.setBackgroundColor(getColor(R.color.shopping));
        } else if (e.primaryTag.equals("Professional")) {
            binding.tag1.setText(e.primaryTag);
            binding.tag1.setIcon(getDrawable(R.drawable.ic_professional));
            binding.tag1.setStrokeColor(ColorStateList.valueOf(getColor(R.color.professional)));
            binding.tag1.setBackgroundColor(getColor(R.color.professional));
        } else if (e.primaryTag.equals("Performance")) {
            binding.tag1.setText(e.primaryTag);
            binding.tag1.setIcon(getDrawable(R.drawable.ic_performance));
            binding.tag1.setStrokeColor(ColorStateList.valueOf(getColor(R.color.performance)));
            binding.tag1.setBackgroundColor(getColor(R.color.performance));
        } else if (e.primaryTag.equals("Activism")) {
            binding.tag1.setText(e.primaryTag);
            binding.tag1.setIcon(getDrawable(R.drawable.ic_activism));
            binding.tag1.setStrokeColor(ColorStateList.valueOf(getColor(R.color.activism)));
            binding.tag1.setBackgroundColor(getColor(R.color.activism));
        } else if (e.primaryTag.equals("Religion")) {
            binding.tag1.setText(e.primaryTag);
            binding.tag1.setIcon(getDrawable(R.drawable.ic_religion));
            binding.tag1.setStrokeColor(ColorStateList.valueOf(getColor(R.color.religion)));
            binding.tag1.setBackgroundColor(getColor(R.color.religion));
        } else if (e.primaryTag.equals("Misc")) {
            binding.tag1.setText(e.primaryTag);
            binding.tag1.setIcon(getDrawable(R.drawable.ic_misc));
            binding.tag1.setStrokeColor(ColorStateList.valueOf(getColor(R.color.misc)));
            binding.tag1.setBackgroundColor(getColor(R.color.misc));
        } else {
            binding.tag1.setVisibility(View.GONE);
        }

        if (e.secondaryTag == null) {
            binding.tag2.setVisibility(View.GONE);
        } else if (e.secondaryTag.equals("Food")) {
            binding.tag2.setText(e.secondaryTag);
            binding.tag2.setIcon(getDrawable(R.drawable.ic_food));
            binding.tag2.setStrokeColor(ColorStateList.valueOf(getColor(R.color.food)));
            binding.tag2.setBackgroundColor(getColor(R.color.food));
        } else if (e.secondaryTag.equals("Social")) {
            binding.tag2.setText(e.secondaryTag);
            binding.tag2.setIcon(getDrawable(R.drawable.ic_social));
            binding.tag2.setStrokeColor(ColorStateList.valueOf(getColor(R.color.social)));
            binding.tag2.setBackgroundColor(getColor(R.color.social));
        } else if (e.secondaryTag.equals("Shopping")) {
            binding.tag2.setText(e.secondaryTag);
            binding.tag2.setIcon(getDrawable(R.drawable.ic_shopping));
            binding.tag2.setStrokeColor(ColorStateList.valueOf(getColor(R.color.shopping)));
            binding.tag2.setBackgroundColor(getColor(R.color.shopping));
        } else if (e.secondaryTag.equals("Professional")) {
            binding.tag2.setText(e.secondaryTag);
            binding.tag2.setIcon(getDrawable(R.drawable.ic_professional));
            binding.tag2.setStrokeColor(ColorStateList.valueOf(getColor(R.color.professional)));
            binding.tag2.setBackgroundColor(getColor(R.color.professional));
        } else if (e.secondaryTag.equals("Performance")) {
            binding.tag2.setText(e.secondaryTag);
            binding.tag2.setIcon(getDrawable(R.drawable.ic_performance));
            binding.tag2.setStrokeColor(ColorStateList.valueOf(getColor(R.color.performance)));
            binding.tag2.setBackgroundColor(getColor(R.color.performance));
        } else if (e.secondaryTag.equals("Activism")) {
            binding.tag2.setText(e.secondaryTag);
            binding.tag2.setIcon(getDrawable(R.drawable.ic_activism));
            binding.tag2.setStrokeColor(ColorStateList.valueOf(getColor(R.color.activism)));
            binding.tag2.setBackgroundColor(getColor(R.color.activism));
        } else if (e.secondaryTag.equals("Religion")) {
            binding.tag2.setText(e.secondaryTag);
            binding.tag2.setIcon(getDrawable(R.drawable.ic_religion));
            binding.tag2.setStrokeColor(ColorStateList.valueOf(getColor(R.color.religion)));
            binding.tag2.setBackgroundColor(getColor(R.color.religion));
        } else if (e.secondaryTag.equals("Misc")) {
            binding.tag2.setText(e.secondaryTag);
            binding.tag2.setIcon(getDrawable(R.drawable.ic_misc));
            binding.tag2.setStrokeColor(ColorStateList.valueOf(getColor(R.color.misc)));
            binding.tag2.setBackgroundColor(getColor(R.color.misc));
        } else {
            binding.tag2.setVisibility(View.GONE);
        }

        if (e.tertiaryTag == null) {
            binding.tag3.setVisibility(View.GONE);
        } else if (e.tertiaryTag.equals("Food")) {
            binding.tag3.setText(e.tertiaryTag);
            binding.tag3.setIcon(getDrawable(R.drawable.ic_food));
            binding.tag3.setStrokeColor(ColorStateList.valueOf(getColor(R.color.food)));
            binding.tag3.setBackgroundColor(getColor(R.color.food));
        } else if (e.tertiaryTag.equals("Social")) {
            binding.tag3.setText(e.tertiaryTag);
            binding.tag3.setIcon(getDrawable(R.drawable.ic_social));
            binding.tag3.setStrokeColor(ColorStateList.valueOf(getColor(R.color.social)));
            binding.tag3.setBackgroundColor(getColor(R.color.social));
        } else if (e.tertiaryTag.equals("Shopping")) {
            binding.tag3.setText(e.tertiaryTag);
            binding.tag3.setIcon(getDrawable(R.drawable.ic_shopping));
            binding.tag3.setStrokeColor(ColorStateList.valueOf(getColor(R.color.shopping)));
            binding.tag3.setBackgroundColor(getColor(R.color.shopping));
        } else if (e.tertiaryTag.equals("Professional")) {
            binding.tag3.setText(e.tertiaryTag);
            binding.tag3.setIcon(getDrawable(R.drawable.ic_professional));
            binding.tag3.setStrokeColor(ColorStateList.valueOf(getColor(R.color.professional)));
            binding.tag3.setBackgroundColor(getColor(R.color.professional));
        } else if (e.tertiaryTag.equals("Performance")) {
            binding.tag3.setText(e.tertiaryTag);
            binding.tag3.setIcon(getDrawable(R.drawable.ic_performance));
            binding.tag3.setStrokeColor(ColorStateList.valueOf(getColor(R.color.performance)));
            binding.tag3.setBackgroundColor(getColor(R.color.performance));
        } else if (e.tertiaryTag.equals("Activism")) {
            binding.tag3.setText(e.tertiaryTag);
            binding.tag3.setIcon(getDrawable(R.drawable.ic_activism));
            binding.tag3.setStrokeColor(ColorStateList.valueOf(getColor(R.color.activism)));
            binding.tag3.setBackgroundColor(getColor(R.color.activism));
        } else if (e.tertiaryTag.equals("Religion")) {
            binding.tag3.setText(e.tertiaryTag);
            binding.tag3.setIcon(getDrawable(R.drawable.ic_religion));
            binding.tag3.setStrokeColor(ColorStateList.valueOf(getColor(R.color.religion)));
            binding.tag3.setBackgroundColor(getColor(R.color.religion));
        } else if (e.tertiaryTag.equals("Misc")) {
            binding.tag3.setText(e.tertiaryTag);
            binding.tag3.setIcon(getDrawable(R.drawable.ic_misc));
            binding.tag3.setStrokeColor(ColorStateList.valueOf(getColor(R.color.misc)));
            binding.tag3.setBackgroundColor(getColor(R.color.misc));
        } else {
            binding.tag3.setVisibility(View.GONE);
        }

        // @TODO: add remaining fields
        //need to set distance from current location
    }

    private void displayEventMarker(Event e) {
        Event resolvedEvent = events.getByNameLocation(e.title, e.latitude, e.longitude);
        int id = resolvedEvent.id;

        Map<String, String> tagToMarker = Map.of(
                "Activism", "ic_marker__activism",
                "Food", "ic_marker__food",
                "Misc", "ic_marker__misc",
                "Performance", "ic_marker__performance",
                "Professional", "ic_marker__professional",
                "Religion", "ic_marker__religion",
                "Shopping", "ic_marker__shopping",
                "Social", "ic_marker__social"
        );

        Log.wtf("Get tag", e.primaryTag);

        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(tagToMarker.get(e.primaryTag), "drawable", getPackageName()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, 85, 110, false);

        LatLng eventLoc = new LatLng(e.latitude, e.longitude);
        Marker eventMarker = mMap.addMarker(new MarkerOptions()
                .position(eventLoc)
                .title(e.title)
                .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)));

        eventMarker.setTag(id);
        eventMarkers.add(eventMarker);
    }

    private boolean CheckAllFields() {
        if (binding.reportingEventNameTextInput.length() == 0) {
            binding.reportingEventNameTextInput.setError("Event Name is required");
            return false;
        }

        if (binding.reportingEventDescriptionTextInput.length() == 0) {
            binding.reportingEventDescriptionTextInput.setError("Event Description is required");
            return false;
        }

        if (binding.reportingLocationTextInput.length() == 0) {
            binding.reportingLocationTextInput.setError("Location is required");
            return false;
        }

        // after all validation return true.
        return true;
    }

    private boolean CheckAllTags() {

        if(CheckboxCounterTracker > 0 && CheckboxCounterTracker <4){
            return true;
        } else {
            Toast.makeText(this,CheckboxCounterTracker + " No of Tags not within valid range" ,Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void isCheckedOrNot(boolean isChecked) {
        if (isChecked) {
            CheckboxCounterTracker++;
        }
        else {
            if (CheckboxCounterTracker > 0) {
                CheckboxCounterTracker--;
            }
        }
        System.out.println(CheckboxCounterTracker);
    }


    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.reporting_checkBox1:
                isCheckedOrNot(checked);
                break;

            case R.id.reporting_checkBox2:
                isCheckedOrNot(checked);
                break;


            case R.id.reporting_checkBox3:
                isCheckedOrNot(checked);
                break;


            case R.id.reporting_checkBox4:
                isCheckedOrNot(checked);
                break;

            case R.id.reporting_checkBox5:
                isCheckedOrNot(checked);
                break;

            case R.id.reporting_checkBox6:
                isCheckedOrNot(checked);
                break;

            case R.id.reporting_checkBox7:
                isCheckedOrNot(checked);
                break;

            case R.id.reporting_checkBox8:
                isCheckedOrNot(checked);
                break;

        }
    }

//    private void populateUserInfo(String userId){
//        User u = new User();
//        u = users.getUserById(userId);
//        binding.username.setText(u.userId);
//        binding.pointsField.setText(u.points);
//
//        //TODO: LEVEL, PROGRESS BAR
//    }

//    private void addUser(User u) {
//        users.insertUser(u);
//    }

//    public void addPoints(String userId, int pts){
//        User user = users.getUserById(userId);
//        int newPts = user.points + pts;
//        users.updatePoints(userId, newPts);
//    }

    private void onClear()
    {

        if (binding.reportingEventNameTextInput != null) binding.reportingEventNameTextInput.setText("");
        if (binding.reportingEventDescriptionTextInput != null) binding.reportingEventDescriptionTextInput.setText("");
        if (binding.reportingLocationTextInput != null) binding.reportingLocationTextInput.setText("");
        if (binding.reportingCheckBox1.isChecked() == true) binding.reportingCheckBox1.setChecked(false);
        if (binding.reportingCheckBox2.isChecked() == true) binding.reportingCheckBox2.setChecked(false);
        if (binding.reportingCheckBox3.isChecked() == true) binding.reportingCheckBox3.setChecked(false);
        if (binding.reportingCheckBox4.isChecked() == true) binding.reportingCheckBox4.setChecked(false);
        if (binding.reportingCheckBox5.isChecked() == true) binding.reportingCheckBox5.setChecked(false);
        if (binding.reportingCheckBox6.isChecked() == true) binding.reportingCheckBox6.setChecked(false);
        if (binding.reportingCheckBox7.isChecked() == true) binding.reportingCheckBox7.setChecked(false);
        if (binding.reportingCheckBox8.isChecked() == true) binding.reportingCheckBox8.setChecked(false);
        CheckboxCounterTracker = 0;

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapClickListener((LatLng loc) -> {
            currentState = AppState.FullscreenMap;
            updateVisibility();
        });

        mMap.setOnMarkerClickListener((Marker m) -> {
            Event event = events.getById((int) m.getTag());
            populateEventInfo(event);

            currentState = AppState.EventDetails;
            updateVisibility();

            return true;
        });

        // Add a marker in Sydney and move the camera
        LatLng quad = new LatLng(40.1074821,-88.2265963);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(quad, 14f));

        Event exampleEvent1 = new Event();
        exampleEvent1.author = "userGuy123";
        exampleEvent1.authorPoints = 344;
        exampleEvent1.description = "This is an event. You should pull up.";
        exampleEvent1.writtenLocation = "Fourth floor chemistry building!";
        exampleEvent1.title = "Super secret social stuff";
        exampleEvent1.latitude = 40.1074821; exampleEvent1.longitude = -88.2265963;
        exampleEvent1.primaryTag = "Social";
        exampleEvent1.secondaryTag = "Food";
        exampleEvent1.tertiaryTag = "Shopping";

        Event exampleEvent2 = new Event();
        exampleEvent2.author = "anotherUser";
        exampleEvent2.description = "Tons of food!";
        exampleEvent2.writtenLocation = "South of the Union, north quad";
        exampleEvent2.title = "Bake sale on the quad";
        exampleEvent2.primaryTag = "Food";
        exampleEvent2.latitude = 40.108308; exampleEvent2.longitude = -88.227017;

        googleMap.setMyLocationEnabled(true);

        addEvent(exampleEvent1);
        addEvent(exampleEvent2);
    }
}
