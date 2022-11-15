package com.globalgang.spur;

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
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
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

    private String USER_NAME = "rick";

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
    private boolean seen_points_popup = false;

    // one boolean variable to check whether all the text fields in Reporting Screen
    // are filled by the user, properly or not.
    boolean isAllFieldsCheckedReporting = false;
    // one boolean variable to check whether all the tags in Reporting Screen
    // are filled by the user, properly or not.
    boolean isAllTagsCheckedReporting = false;

    // one boolean variable to check whether the primary tag selected
    // is one of the tags checked as part of the checkboxes
    boolean isPrimaryTagPartofCheckedTags = false;

    private int CheckboxCounterTracker = 0;

    private List<String> CheckedTagNames = new ArrayList<>();

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventMarkers = new ArrayList<>();

        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "spur-db").allowMainThreadQueries().build();
        events = db.eventDao();
        users = db.userDao();

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

        if (!users.isUserExists("rick")) {
            User rick = new User();
            rick.userId = "rick";
            rick.points = 0;
            users.insertUser(rick);
        }

        updateKnownLocation();

        //Obtain the dropdown id for reporting screen
        Spinner spinnerTags = findViewById(R.id.reporting_spinner_for_primary_tag_dropdown);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.tags_array, R.layout.reporting_spinner_backgroud_color);
        adapter.setDropDownViewResource(R.layout.reporting_custom_spinner_dropdrown_text_colour);
        spinnerTags.setAdapter(adapter);


        //init user profile
        populateUserInfo("rick");

        updateFilterColors("All");

        // filter buttons change marker visibility
        binding.btnFilterAll.setOnClickListener((View v) -> {
            updateFilterColors("All");

            for (int i = 0; i < eventMarkers.size(); i++) {
                eventMarkers.get(i).setVisible(true);
            }
        });
        binding.btnFilterFood.setOnClickListener((View v) -> {
            updateFilterColors("Food");

            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag == null || event.primaryTag.isEmpty()) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.primaryTag.equals("Food")) {
                    eventMarkers.get(i).setVisible(true);
                } else if ((event.secondaryTag == null || event.secondaryTag.isEmpty())) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.secondaryTag.equals("Food")) {
                    eventMarkers.get(i).setVisible(true);
                } else if ((event.tertiaryTag == null || event.tertiaryTag.isEmpty())) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.tertiaryTag.equals("Food")) {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterSocial.setOnClickListener((View v) -> {
            updateFilterColors("Social");

            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag == null || event.primaryTag.isEmpty()) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.primaryTag.equals("Social")) {
                    eventMarkers.get(i).setVisible(true);
                } else if ((event.secondaryTag == null || event.secondaryTag.isEmpty())) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.secondaryTag.equals("Social")) {
                    eventMarkers.get(i).setVisible(true);
                } else if ((event.tertiaryTag == null || event.tertiaryTag.isEmpty())) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.tertiaryTag.equals("Social")) {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterShopping.setOnClickListener((View v) -> {
            updateFilterColors("Shopping");

            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag == null || event.primaryTag.isEmpty()) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.primaryTag.equals("Shopping")) {
                    eventMarkers.get(i).setVisible(true);
                } else if ((event.secondaryTag == null || event.secondaryTag.isEmpty())) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.secondaryTag.equals("Shopping")) {
                    eventMarkers.get(i).setVisible(true);
                } else if ((event.tertiaryTag == null || event.tertiaryTag.isEmpty())) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.tertiaryTag.equals("Shopping")) {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterProfessional.setOnClickListener((View v) -> {
            updateFilterColors("Professional");

            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag == null || event.primaryTag.isEmpty()) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.primaryTag.equals("Professional")) {
                    eventMarkers.get(i).setVisible(true);
                } else if ((event.secondaryTag == null || event.secondaryTag.isEmpty())) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.secondaryTag.equals("Professional")) {
                    eventMarkers.get(i).setVisible(true);
                } else if ((event.tertiaryTag == null || event.tertiaryTag.isEmpty())) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.tertiaryTag.equals("Professional")) {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterPerformance.setOnClickListener((View v) -> {
            updateFilterColors("Performance");

            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag == null || event.primaryTag.isEmpty()) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.primaryTag.equals("Performance")) {
                    eventMarkers.get(i).setVisible(true);
                } else if ((event.secondaryTag == null || event.secondaryTag.isEmpty())) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.secondaryTag.equals("Performance")) {
                    eventMarkers.get(i).setVisible(true);
                } else if ((event.tertiaryTag == null || event.tertiaryTag.isEmpty())) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.tertiaryTag.equals("Performance")) {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterActivism.setOnClickListener((View v) -> {
            updateFilterColors("Activism");

            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag == null || event.primaryTag.isEmpty()) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.primaryTag.equals("Activism")) {
                    eventMarkers.get(i).setVisible(true);
                } else if ((event.secondaryTag == null || event.secondaryTag.isEmpty())) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.secondaryTag.equals("Activism")) {
                    eventMarkers.get(i).setVisible(true);
                } else if ((event.tertiaryTag == null || event.tertiaryTag.isEmpty())) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.tertiaryTag.equals("Activism")) {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterReligion.setOnClickListener((View v) -> {
            updateFilterColors("Religion");

            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag == null || event.primaryTag.isEmpty()) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.primaryTag.equals("Religion")) {
                    eventMarkers.get(i).setVisible(true);
                } else if ((event.secondaryTag == null || event.secondaryTag.isEmpty())) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.secondaryTag.equals("Religion")) {
                    eventMarkers.get(i).setVisible(true);
                } else if ((event.tertiaryTag == null || event.tertiaryTag.isEmpty())) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.tertiaryTag.equals("Religion")) {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterMisc.setOnClickListener((View v) -> {
            updateFilterColors("Misc");

            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag == null || event.primaryTag.isEmpty()) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.primaryTag.equals("Misc")) {
                    eventMarkers.get(i).setVisible(true);
                } else if ((event.secondaryTag == null || event.secondaryTag.isEmpty())) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.secondaryTag.equals("Misc")) {
                    eventMarkers.get(i).setVisible(true);
                } else if ((event.tertiaryTag == null || event.tertiaryTag.isEmpty())) {
                    eventMarkers.get(i).setVisible(false);
                    continue;
                } else if (event.tertiaryTag.equals("Misc")) {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });

        //Change No. Yes and No. No
        int myPoints = 1;
        TextView no_confirmation = findViewById(R.id.event_num_yes);
        no_confirmation.setText(Integer.toString(myPoints));

        // got it button
        /*
        * I'm thinking that maybe when you click on event details it should show the points popup
        * for the first time (case 1)
        * or
        * it could be the first screen when you open the app (case 2)
        */
        binding.gotItButton.setOnClickListener((View v) -> { // case 1
           currentState = AppState.EventDetails;
           updateVisibility();
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
            isPrimaryTagPartofCheckedTags = CheckPrimaryTagForErrorHandling();
            if (!isAllFieldsCheckedReporting || !isAllTagsCheckedReporting || !isPrimaryTagPartofCheckedTags) {
                return;
            }

            Event e = new Event();
            updateKnownLocation();

            e.latitude = locLat;
            e.longitude = locLong;

            e.title = binding.reportingEventNameTextInput.getText().toString();
            e.description = binding.reportingEventDescriptionTextInput.getText().toString();
            e.writtenLocation = binding.reportingLocationTextInput.getText().toString();
            e.numDislikes = 0;
            e.numLikes = 0;
            e.primaryTag = binding.reportingSpinnerForPrimaryTagDropdown.getSelectedItem().toString();

            e.author = USER_NAME;

            User reporter = users.getUserById(USER_NAME);
            e.authorPoints = reporter.points;

            // @TODO: Set these fields
            //String[] reportingTagsArray = new String[3];
            for (int i = 0; i < CheckedTagNames.size(); i++){
                if(CheckedTagNames.get(i).toString().equals(e.primaryTag)){
                    CheckedTagNames.remove(CheckedTagNames.get(i).toString());
                }
            //System.out.println("After clicking submit button");
            //System.out.println(CheckedTagNames);
            }
            //reportingTagsArray[0] = binding.
            e.secondaryTag = CheckedTagNames.get(0).toString();
            e.tertiaryTag = CheckedTagNames.get(1).toString();

            addPoints("rick", 50);
            addEvent(e);

            currentState = AppState.EventDetails;
            updateVisibility();
        });

        //clicking on confirm button should add points to user
        binding.confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addPoints("rick", 10);
                populateUserInfo("rick");
                String reporterId = binding.reporterId.getText().toString();
                addPoints(reporterId, 5);
            }
        });

        //clicking on refute button should add points to user
        binding.refuteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addPoints("rick", 10);
                populateUserInfo("rick");
                String reporterId = binding.reporterId.getText().toString();
                addPoints(reporterId, -5);

            }
        });

        //clicking on profile button will take you to profile screen
        binding.profileButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                currentState = AppState.ProfileView;
                updateVisibility();
            }
        });

        //clicking on events button will take you to map screen
        binding.eventButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                currentState = AppState.FullscreenMap;
                updateVisibility();
            }
        });
    }

    private void updateFilterColors(String selected_filter) {
        if (selected_filter.equals("All")) {
            binding.btnFilterAll.setBackgroundColor(getColor(R.color.all_selected));

            binding.btnFilterFood.setBackgroundColor(getColor(R.color.food));
            binding.btnFilterSocial.setBackgroundColor(getColor(R.color.social));
            binding.btnFilterShopping.setBackgroundColor(getColor(R.color.shopping));
            binding.btnFilterProfessional.setBackgroundColor(getColor(R.color.professional));
            binding.btnFilterPerformance.setBackgroundColor(getColor(R.color.performance));
            binding.btnFilterActivism.setBackgroundColor(getColor(R.color.activism));
            binding.btnFilterReligion.setBackgroundColor(getColor(R.color.religion));
            binding.btnFilterMisc.setBackgroundColor(getColor(R.color.misc));
        } else if (selected_filter.equals("Food")) {
            binding.btnFilterFood.setBackgroundColor(getColor(R.color.food_selected));

            binding.btnFilterAll.setBackgroundColor(getColor(R.color.all));
            binding.btnFilterSocial.setBackgroundColor(getColor(R.color.social));
            binding.btnFilterShopping.setBackgroundColor(getColor(R.color.shopping));
            binding.btnFilterProfessional.setBackgroundColor(getColor(R.color.professional));
            binding.btnFilterPerformance.setBackgroundColor(getColor(R.color.performance));
            binding.btnFilterActivism.setBackgroundColor(getColor(R.color.activism));
            binding.btnFilterReligion.setBackgroundColor(getColor(R.color.religion));
            binding.btnFilterMisc.setBackgroundColor(getColor(R.color.misc));

        } else if (selected_filter.equals("Social")) {
            binding.btnFilterSocial.setBackgroundColor(getColor(R.color.social_selected));

            binding.btnFilterAll.setBackgroundColor(getColor(R.color.all));
            binding.btnFilterFood.setBackgroundColor(getColor(R.color.food));
            binding.btnFilterShopping.setBackgroundColor(getColor(R.color.shopping));
            binding.btnFilterProfessional.setBackgroundColor(getColor(R.color.professional));
            binding.btnFilterPerformance.setBackgroundColor(getColor(R.color.performance));
            binding.btnFilterActivism.setBackgroundColor(getColor(R.color.activism));
            binding.btnFilterReligion.setBackgroundColor(getColor(R.color.religion));
            binding.btnFilterMisc.setBackgroundColor(getColor(R.color.misc));
        } else if (selected_filter.equals("Shopping")) {
            binding.btnFilterShopping.setBackgroundColor(getColor(R.color.shopping_selected));

            binding.btnFilterAll.setBackgroundColor(getColor(R.color.all));
            binding.btnFilterFood.setBackgroundColor(getColor(R.color.food));
            binding.btnFilterSocial.setBackgroundColor(getColor(R.color.social));
            binding.btnFilterProfessional.setBackgroundColor(getColor(R.color.professional));
            binding.btnFilterPerformance.setBackgroundColor(getColor(R.color.performance));
            binding.btnFilterActivism.setBackgroundColor(getColor(R.color.activism));
            binding.btnFilterReligion.setBackgroundColor(getColor(R.color.religion));
            binding.btnFilterMisc.setBackgroundColor(getColor(R.color.misc));
        } else if (selected_filter.equals("Professional")) {
            binding.btnFilterProfessional.setBackgroundColor(getColor(R.color.professional_selected));

            binding.btnFilterAll.setBackgroundColor(getColor(R.color.all));
            binding.btnFilterFood.setBackgroundColor(getColor(R.color.food));
            binding.btnFilterSocial.setBackgroundColor(getColor(R.color.social));
            binding.btnFilterShopping.setBackgroundColor(getColor(R.color.shopping));
            binding.btnFilterPerformance.setBackgroundColor(getColor(R.color.performance));
            binding.btnFilterActivism.setBackgroundColor(getColor(R.color.activism));
            binding.btnFilterReligion.setBackgroundColor(getColor(R.color.religion));
            binding.btnFilterMisc.setBackgroundColor(getColor(R.color.misc));
        } else if (selected_filter.equals("Performance")) {
            binding.btnFilterPerformance.setBackgroundColor(getColor(R.color.performance_selected));

            binding.btnFilterAll.setBackgroundColor(getColor(R.color.all));
            binding.btnFilterFood.setBackgroundColor(getColor(R.color.food));
            binding.btnFilterSocial.setBackgroundColor(getColor(R.color.social));
            binding.btnFilterShopping.setBackgroundColor(getColor(R.color.shopping));
            binding.btnFilterProfessional.setBackgroundColor(getColor(R.color.professional));
            binding.btnFilterActivism.setBackgroundColor(getColor(R.color.activism));
            binding.btnFilterReligion.setBackgroundColor(getColor(R.color.religion));
            binding.btnFilterMisc.setBackgroundColor(getColor(R.color.misc));
        } else if (selected_filter.equals("Activism")) {
            binding.btnFilterActivism.setBackgroundColor(getColor(R.color.activism_selected));

            binding.btnFilterAll.setBackgroundColor(getColor(R.color.all));
            binding.btnFilterFood.setBackgroundColor(getColor(R.color.food));
            binding.btnFilterSocial.setBackgroundColor(getColor(R.color.social));
            binding.btnFilterShopping.setBackgroundColor(getColor(R.color.shopping));
            binding.btnFilterProfessional.setBackgroundColor(getColor(R.color.professional));
            binding.btnFilterPerformance.setBackgroundColor(getColor(R.color.performance));
            binding.btnFilterReligion.setBackgroundColor(getColor(R.color.religion));
            binding.btnFilterMisc.setBackgroundColor(getColor(R.color.misc));
        } else if (selected_filter.equals("Religion")) {
            binding.btnFilterReligion.setBackgroundColor(getColor(R.color.religion_selected));

            binding.btnFilterAll.setBackgroundColor(getColor(R.color.all));
            binding.btnFilterFood.setBackgroundColor(getColor(R.color.food));
            binding.btnFilterSocial.setBackgroundColor(getColor(R.color.social));
            binding.btnFilterShopping.setBackgroundColor(getColor(R.color.shopping));
            binding.btnFilterProfessional.setBackgroundColor(getColor(R.color.professional));
            binding.btnFilterPerformance.setBackgroundColor(getColor(R.color.performance));
            binding.btnFilterActivism.setBackgroundColor(getColor(R.color.activism));
            binding.btnFilterMisc.setBackgroundColor(getColor(R.color.misc));
        } else if (selected_filter.equals("Misc")) {
            binding.btnFilterMisc.setBackgroundColor(getColor(R.color.misc_selected));

            binding.btnFilterAll.setBackgroundColor(getColor(R.color.all));
            binding.btnFilterFood.setBackgroundColor(getColor(R.color.food));
            binding.btnFilterSocial.setBackgroundColor(getColor(R.color.social));
            binding.btnFilterShopping.setBackgroundColor(getColor(R.color.shopping));
            binding.btnFilterProfessional.setBackgroundColor(getColor(R.color.professional));
            binding.btnFilterPerformance.setBackgroundColor(getColor(R.color.performance));
            binding.btnFilterActivism.setBackgroundColor(getColor(R.color.activism));
            binding.btnFilterReligion.setBackgroundColor(getColor(R.color.religion));
        } else {
            binding.btnFilterAll.setBackgroundColor(getColor(R.color.all));
            binding.btnFilterFood.setBackgroundColor(getColor(R.color.food));
            binding.btnFilterSocial.setBackgroundColor(getColor(R.color.social));
            binding.btnFilterShopping.setBackgroundColor(getColor(R.color.shopping));
            binding.btnFilterProfessional.setBackgroundColor(getColor(R.color.professional));
            binding.btnFilterPerformance.setBackgroundColor(getColor(R.color.performance));
            binding.btnFilterActivism.setBackgroundColor(getColor(R.color.activism));
            binding.btnFilterReligion.setBackgroundColor(getColor(R.color.religion));
            binding.btnFilterMisc.setBackgroundColor(getColor(R.color.misc));
        }
    }

    @SuppressLint("MissingPermission")
    private void updateKnownLocation() {
        Location lastKnown = ((LocationManager) getSystemService(Context.LOCATION_SERVICE)).getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (lastKnown != null) {
            locLat = lastKnown.getLatitude();
            locLong = lastKnown.getLongitude();
        } else {
            locLat = 40.1108879;
            locLong = -88.2282231;
        }
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
            binding.filterScrollView.setVisibility(View.GONE);
            binding.btnAddEvent.setVisibility(View.GONE);
            binding.profileView.setVisibility(View.VISIBLE);
            binding.navi.setVisibility(View.VISIBLE);
        } else {
            binding.profileView.setVisibility(View.GONE);
        }

        //popup describing points system (should popup everytime user logs in?)
        if (currentState == AppState.PointsPopup) {
            binding.pointsPopup.setVisibility(View.VISIBLE);
        } else {
            binding.pointsPopup.setVisibility(View.GONE);
        }

    }

    private void displayExistingEvents() {
        List<Event> existing = events.getAll();

        for (Event e : existing) {
            addEvent(e);
        }
    }

    private void addEvent(Event e) {
        if (events.getByNameLocation(e.title, e.latitude, e.longitude) == null) {
            events.insertAll(e);
        }

        populateEventInfo(e);
        displayEventMarker(e);
    }

    private void populateEventInfo(Event e) {
        binding.eventName.setText(e.title);
        binding.eventDescription.setText(e.description);
        binding.eventNumNo.setText(Integer.toString(e.numDislikes));
        binding.eventNumYes.setText(Integer.toString(e.numLikes));
        binding.reporterId.setText(e.author);
        binding.reporterPoints.setText(Integer.toString(e.authorPoints));

        if (e.writtenLocation == null || e.writtenLocation.isEmpty()) {
            binding.eventLocationLayout.setVisibility(View.GONE);
        } else {
            binding.eventLocationLayout.setVisibility(View.VISIBLE);
            binding.eventLocation.setText(e.writtenLocation);
        }

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
        //need to set last confirmed
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

        // after all validation return true.
        return true;
    }

    private boolean CheckAllTags() {

        if(CheckboxCounterTracker > 0 && CheckboxCounterTracker <4){
            return true;
        } else {
            Toast.makeText(this,"Please Choose 1-3 Tags",Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean CheckPrimaryTagForErrorHandling() {
        boolean trackingFlag = false;
        for (int i = 0; i < CheckedTagNames.size(); i++) {
            System.out.println(CheckedTagNames.get(i).toString());
            if (CheckedTagNames.get(i).toString().equals(binding.reportingSpinnerForPrimaryTagDropdown.getSelectedItem().toString())) {
                trackingFlag = true;
                return true;
            } else {
                trackingFlag = false;
            }
        }
        if(trackingFlag == false){
            Toast.makeText(this, "Primary Tag should be part of Tags selected above", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void isCheckedOrNotCounter(boolean isChecked) {
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
    /*
    private List<String> isCheckedOrNotTagNames(boolean isChecked) {
        String TagName1 = "";
        String TagName2 = "";
        TagName1 = binding.reportingCheckBox1.getText().toString();
        TagName2 = binding.reportingCheckBox2.getText().toString();
        //List<String> CheckedTagNames = new ArrayList<>();
        if (isChecked) {
            CheckedTagNames.add(TagName1);
            CheckedTagNames.add(TagName2);
        }
        else {
            CheckedTagNames.remove(TagName1);
            CheckedTagNames.remove(TagName2);
        }
        System.out.println(CheckedTagNames);
        return CheckedTagNames;
    }
    */
    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.reporting_checkBox1:
                isCheckedOrNotCounter(checked);
                if(checked) {CheckedTagNames.add(binding.reportingCheckBox1.getText().toString());}
                else {CheckedTagNames.remove(binding.reportingCheckBox1.getText().toString());}
                System.out.println(CheckedTagNames);
                break;

            case R.id.reporting_checkBox2:
                 isCheckedOrNotCounter(checked);
                 if(checked) CheckedTagNames.add(binding.reportingCheckBox2.getText().toString());
                 else {CheckedTagNames.remove(binding.reportingCheckBox2.getText().toString());}
                 System.out.println(CheckedTagNames);
                 break;


            case R.id.reporting_checkBox3:
                 isCheckedOrNotCounter(checked);
                 if(checked) {CheckedTagNames.add(binding.reportingCheckBox3.getText().toString());}
                 else {CheckedTagNames.remove(binding.reportingCheckBox3.getText().toString());}
                 System.out.println(CheckedTagNames);
                 break;


            case R.id.reporting_checkBox4:
                 isCheckedOrNotCounter(checked);
                 if(checked) {CheckedTagNames.add(binding.reportingCheckBox4.getText().toString());}
                 else {CheckedTagNames.remove(binding.reportingCheckBox4.getText().toString());}
                 System.out.println(CheckedTagNames);
                 break;

            case R.id.reporting_checkBox5:
                 isCheckedOrNotCounter(checked);
                 if(checked) {CheckedTagNames.add(binding.reportingCheckBox5.getText().toString());}
                 else {CheckedTagNames.remove(binding.reportingCheckBox5.getText().toString());}
                 System.out.println(CheckedTagNames);
                 break;

            case R.id.reporting_checkBox6:
                 isCheckedOrNotCounter(checked);
                 if(checked) {CheckedTagNames.add(binding.reportingCheckBox6.getText().toString());}
                 else {CheckedTagNames.remove(binding.reportingCheckBox6.getText().toString());}
                 System.out.println(CheckedTagNames);
                 break;

            case R.id.reporting_checkBox7:
                 isCheckedOrNotCounter(checked);
                 if(checked) {CheckedTagNames.add(binding.reportingCheckBox7.getText().toString());}
                 else {CheckedTagNames.remove(binding.reportingCheckBox7.getText().toString());}
                 System.out.println(CheckedTagNames);
                 break;

            case R.id.reporting_checkBox8:
                 isCheckedOrNotCounter(checked);
                 if(checked) {CheckedTagNames.add(binding.reportingCheckBox8.getText().toString());}
                 else {CheckedTagNames.remove(binding.reportingCheckBox8.getText().toString());}
                 System.out.println(CheckedTagNames);
                 break;
        }
    }

    private void populateUserInfo(String userId){
        User u = users.getUserById(userId);
        binding.username.setText(u.userId);
        binding.pointsField.setText(Integer.toString(u.points));
        //TODO: LEVEL, PROGRESS BAR
    }

    public void addPoints(String userId, int pts){
        User user = users.getUserById(userId);
        int newPts = user.points + pts;
        users.updatePoints(userId, newPts);
    }

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
        CheckedTagNames.clear();
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
        if (!users.isUserExists(exampleEvent1.author)) {
            User exampleUser1 = new User();
            exampleUser1.userId = exampleEvent1.author;
            exampleUser1.points = exampleEvent1.authorPoints;
            users.insertUser(exampleUser1);
        }
        exampleEvent1.description = "This is an event. You should pull up.";
        exampleEvent1.writtenLocation = "Fourth floor chemistry building!";
        exampleEvent1.title = "Super secret social stuff";
        exampleEvent1.latitude = 40.1074821; exampleEvent1.longitude = -88.2265963;
        exampleEvent1.primaryTag = "Social";
        exampleEvent1.secondaryTag = "Food";
        exampleEvent1.tertiaryTag = "Shopping";

        Event exampleEvent2 = new Event();
        exampleEvent2.author = "anotherUser";
        exampleEvent2.authorPoints = 0;
        if (!users.isUserExists(exampleEvent2.author)) {
            User exampleUser2 = new User();
            exampleUser2.userId = exampleEvent2.author;
            exampleUser2.points = exampleEvent2.authorPoints;
            users.insertUser(exampleUser2);
        }
        exampleEvent2.description = "Tons of food!";
        exampleEvent2.title = "Bake sale on the quad";
        exampleEvent2.primaryTag = "Food";
        exampleEvent2.latitude = 40.108308; exampleEvent2.longitude = -88.227017;

        if (events.getByNameLocation(exampleEvent1.title, exampleEvent1.latitude, exampleEvent1.longitude) == null) {
            events.insertAll(exampleEvent1);
        }
        if (events.getByNameLocation(exampleEvent2.title, exampleEvent2.latitude, exampleEvent2.longitude) == null) {
            events.insertAll(exampleEvent2);
        }

        googleMap.setMyLocationEnabled(true);

        displayExistingEvents();
    }
}
