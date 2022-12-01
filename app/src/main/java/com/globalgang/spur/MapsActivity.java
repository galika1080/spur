package com.globalgang.spur;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
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
import com.google.android.gms.common.util.ArrayUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.globalgang.spur.databinding.ActivityMapsBinding;
import com.google.android.material.button.MaterialButton;

import androidx.room.Room;

import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private Stack<AppState> backStack;

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    private EventDao events;
    private List<Marker> eventMarkers;
    private Map<MaterialButton, Pair<Integer, Integer>> filterButtonColors;

    private UserDao users;

    private String USER_NAME = "rick";

    private enum AppState {
        FullscreenMap,
        EventDetails,
        Reporting,
        ReportPopup,
        PointsPopup,
        ProfileView,
        TopReportersLeaderBoard
    }

    private Integer currentlyViewedEventId = -1;
    private Marker selectedMarker = null;

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

    @Override
    public void onBackPressed() {
        if (!backStack.empty()) backStack.pop();

        if (backStack.empty()) {
            currentState = AppState.FullscreenMap;
            updateVisibility();
            return;
        }

        AppState lastState = backStack.pop();
        currentState = lastState;
        updateVisibility();
    }

    @SuppressLint({"MissingPermission"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backStack = new Stack<>();
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

        if (!users.isUserExists(USER_NAME)) {
            User rick = new User();
            rick.userId = USER_NAME;
            rick.points = 0;
            users.insertUser(rick);
        }

        updateKnownLocation();

        //Obtain the dropdown id for reporting screen
        Spinner spinnerTags = findViewById(R.id.reporting_spinner_for_primary_tag_dropdown);
        String[] defaultTagList = getResources().getStringArray(R.array.tags_array);
        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, R.layout.reporting_spinner_backgroud_color, defaultTagList);
        dataAdapter.setDropDownViewResource(R.layout.reporting_custom_spinner_dropdrown_text_colour);
        spinnerTags.setAdapter(dataAdapter);
        spinnerTags.setSelection(0);

        // points popup as a shared pref
        SharedPreferences sharedPreferences = getSharedPreferences("sharedPref",MODE_PRIVATE);
        SharedPreferences.Editor myEdit = sharedPreferences.edit();
        myEdit.putBoolean("pointsPopupSeen", false);
        myEdit.commit();

        //init user profile
        populateUserInfo(USER_NAME);

        //populate top reporters leaderboard
        populateReporterLeaderboardInfo();

        // init the map from filter button to colors, enabling updateFilterColors() to work
        filterButtonColors = Map.of(
                binding.btnFilterAll, Pair.create(R.color.all, R.color.all_selected),
                binding.btnFilterFood, Pair.create(R.color.food, R.color.food_selected),
                binding.btnFilterSocial, Pair.create(R.color.social, R.color.social_selected),
                binding.btnFilterShopping, Pair.create(R.color.shopping, R.color.shopping_selected),
                binding.btnFilterProfessional, Pair.create(R.color.professional, R.color.professional_selected),
                binding.btnFilterPerformance, Pair.create(R.color.performance, R.color.performance_selected),
                binding.btnFilterActivism, Pair.create(R.color.activism, R.color.activism_selected),
                binding.btnFilterReligion, Pair.create(R.color.religion, R.color.religion_selected),
                binding.btnFilterMisc, Pair.create(R.color.misc, R.color.misc_selected));

        updateFilterColors(binding.btnFilterAll);

        binding.eventButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.navi_selected)));
        binding.profileButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.navi)));

        // filter buttons change marker visibility
        binding.btnFilterAll.setOnClickListener((View v) -> {
            updateFilterColors(v);
            eventMarkers.forEach((m) -> m.setVisible(true));
        });
        binding.btnFilterFood.setOnClickListener((View v) -> {
            updateFilterColors(v);
            updateMarkerVisibility("Food");
        });
        binding.btnFilterSocial.setOnClickListener((View v) -> {
            updateFilterColors(v);
            updateMarkerVisibility("Social");
        });
        binding.btnFilterShopping.setOnClickListener((View v) -> {
            updateFilterColors(v);
            updateMarkerVisibility("Shopping");
        });
        binding.btnFilterProfessional.setOnClickListener((View v) -> {
            updateFilterColors(v);
            updateMarkerVisibility("Professional");
        });
        binding.btnFilterPerformance.setOnClickListener((View v) -> {
            updateFilterColors(v);
            updateMarkerVisibility("Performance");
        });
        binding.btnFilterActivism.setOnClickListener((View v) -> {
            updateFilterColors(v);
            updateMarkerVisibility("Activism");
        });
        binding.btnFilterReligion.setOnClickListener((View v) -> {
            updateFilterColors(v);
            updateMarkerVisibility("Religion");
        });
        binding.btnFilterMisc.setOnClickListener((View v) -> {
            updateFilterColors(v);
            updateMarkerVisibility("Miscellaneous");
        });

        // got it button
        binding.gotItButton.setOnClickListener((View v) -> { // case 1
           currentState = AppState.FullscreenMap;
           updateVisibility();
        });

        // report event button
        binding.btnAddEvent.setOnClickListener((View v) -> {
            currentState = AppState.ReportPopup;
            getNearbyEvents();
            updateVisibility();
        });


       // yes and no buttons on "heads up, nearby events" popup
        binding.popupButtonYes.setOnClickListener((View v) -> {
            onClear();
            currentState = AppState.Reporting;
            updateVisibility();
        });

        binding.popupButtonNo.setOnClickListener((View v) -> {
            currentState = AppState.FullscreenMap;
            updateVisibility();
        });

        binding.reportingSubmitButton.setOnClickListener((View v) -> {
            isAllFieldsCheckedReporting = CheckAllFields();
            if(!isAllFieldsCheckedReporting) return;
            isAllTagsCheckedReporting = CheckAllTags();
            if(!isAllTagsCheckedReporting) return;
            isPrimaryTagPartofCheckedTags = CheckPrimaryTagForErrorHandling();
            if(!isPrimaryTagPartofCheckedTags) return;
            /*
            if (!isAllFieldsCheckedReporting || !isAllTagsCheckedReporting || !isPrimaryTagPartofCheckedTags) {
                return;
            }
            */
            Event e = new Event();
            updateKnownLocation();

            e.latitude = locLat;
            e.longitude = locLong;

            e.lastConfirmed = System.currentTimeMillis();

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
            if (!CheckedTagNames.isEmpty()) {
                e.secondaryTag = CheckedTagNames.get(0).toString();
                if (CheckedTagNames.size() >= 2) {
                    e.tertiaryTag = CheckedTagNames.get(1).toString();
                }
            }

            addPoints(USER_NAME, 50);
            populateUserInfo(USER_NAME);
            populateReporterLeaderboardInfo();
            Toast.makeText(MapsActivity.this, "Thanks for adding an event! +50 points", Toast.LENGTH_SHORT).show();

            addEvent(e);
            Event resolvedEvent = events.getByNameLocation(e.title, e.latitude, e.longitude);
            selectMarkerById(resolvedEvent.id);
            populateEventInfo(resolvedEvent);
        });

        //clicking on confirm button should add points to user
        binding.confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               //get current event from event id
                Event current_event = events.getById(currentlyViewedEventId);
                String reporterId = events.getById(currentlyViewedEventId).author;

                float[] distanceResults = new float[]{-1.0f};
                Location.distanceBetween(current_event.latitude, current_event.longitude, locLat, locLong, distanceResults);
                float distanceMiles = distanceResults[0] * 0.000621371f;
                if (distanceMiles > 0.1f) {
                    Toast.makeText(MapsActivity.this, "You must attend in-person to vote!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (current_event.isRefuted) {
                    //if previously refuted, but now changing to confirm
                    Toast.makeText(MapsActivity.this, "You changed your vote! +0 points", Toast.LENGTH_SHORT).show();
                    events.setIsRefuted(currentlyViewedEventId, false);
                    //remove 1 from refute count
                    events.updateDislikes(currentlyViewedEventId, -1);
                    if (!current_event.firstConfirmed) {
                        //if never confirmed before, give points
                        addPoints(reporterId, 5);
                        events.setFirstConfirmed(currentlyViewedEventId, true);
                    }

                } else if (!current_event.isConfirmed) {
                    //if you had not previously already selected check then give person points (voting for first time on event)
                    addPoints(USER_NAME, 10);
                    populateUserInfo(USER_NAME);
                    populateReporterLeaderboardInfo();
                    Toast.makeText(MapsActivity.this, "Thanks for your feedback! +10 points", Toast.LENGTH_SHORT).show();
                    //if never confirmed before, give points
                    addPoints(reporterId, 5);
                    events.setFirstConfirmed(currentlyViewedEventId, true);
                } else {
                    //do nothing if already checked and pressing check again
                    Toast.makeText(MapsActivity.this, "You have already confirmed this event", Toast.LENGTH_SHORT).show();
                    return;
                }

                events.setIsConfirmed(currentlyViewedEventId, true);


                //increase confirm count
                events.updateLikes(currentlyViewedEventId, 1);
                events.updateLastConfirmed(currentlyViewedEventId, System.currentTimeMillis());

                //call populate event to update confirm/refute count, and reporter points info
                populateEventInfo(events.getById(currentlyViewedEventId));
            }
        });

        //clicking on refute button should add points to user
        binding.refuteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get current event from event id
                Event current_event = events.getById(currentlyViewedEventId);

                float[] distanceResults = new float[]{-1.0f};
                Location.distanceBetween(current_event.latitude, current_event.longitude, locLat, locLong, distanceResults);
                float distanceMiles = distanceResults[0] * 0.000621371f;
                if (distanceMiles > 0.1f) {
                    Toast.makeText(MapsActivity.this, "You must attend in-person to vote!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (current_event.isConfirmed) {
                    //if previously confirmed, but now changing to refute
                    Toast.makeText(MapsActivity.this, "You changed your vote! +0 points", Toast.LENGTH_SHORT).show();
                    events.setIsConfirmed(currentlyViewedEventId, false);
                    //remove 1 from confirm count
                    events.updateLikes(currentlyViewedEventId, -1);
                } else if (!current_event.isRefuted) {
                    //if you had not previously already selected x then give person points (voting for first time on event)
                    addPoints(USER_NAME, 10);
                    populateUserInfo(USER_NAME);
                    populateReporterLeaderboardInfo();
                    Toast.makeText(MapsActivity.this, "Thanks for your feedback! +10 points", Toast.LENGTH_SHORT).show();
                } else {
                    //do nothing if already refuted and pressing x again
                    Toast.makeText(MapsActivity.this, "You have already refuted this event", Toast.LENGTH_SHORT).show();
                    return;
                }

                events.setIsRefuted(currentlyViewedEventId, true);

                //increase refute count
                events.updateDislikes(currentlyViewedEventId, 1);

                //call populate event to update confirm/refute count, and reporter points info
                populateEventInfo(events.getById(currentlyViewedEventId));
            }
        });

        //clicking on profile button will take you to profile screen
        binding.profileButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                currentState = AppState.ProfileView;
                populateUserInfo(USER_NAME);
                updateVisibility();
            }
        });

        //clicking on Top Reporters Leaderboard button will take you to Top Reporters Leaderboard screen
        binding.topReportersLeaderboardButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                currentState = AppState.TopReportersLeaderBoard;
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

        //clicking on the profile screen takes you to the profile screen
        binding.profileView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                currentState = AppState.ProfileView;
                binding.eventButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.navi)));
                binding.profileButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.navi_selected)));
                populateUserInfo(USER_NAME);
                updateVisibility();
            }
        });
    }

    private void updateMarkerVisibility(String filterTag) {
        for (Marker mark : eventMarkers) {
            Event event = events.getById((int) mark.getTag());

            String[] tags = {event.primaryTag, event.secondaryTag, event.tertiaryTag};
            mark.setVisible(ArrayUtils.contains(tags, filterTag));
        }
    }

    private void updateFilterColors(View clickedView) {
        filterButtonColors.forEach((btn, colorPair) -> {
            if (btn == clickedView) {
                btn.setBackgroundColor(getColor(colorPair.second));
            } else {
                btn.setBackgroundColor(getColor(colorPair.first));
            }
        });
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

    private void showHideView(View v, boolean visible, int x, int y, boolean fade) {
        if (visible) {
            v.animate()
                    .translationX(0)
                    .translationY(0)
                    .alpha(1)
                    .scaleX(1)
                    .scaleY(1)
                    .withStartAction(() -> {
                       v.setVisibility(View.VISIBLE);
                    });
        } else {
            v.animate()
                    .translationX(x)
                    .translationY(y)
                    .alpha(fade? 0f : 1f)
                    .scaleX(fade? 0.8f : 1f)
                    .scaleY(fade? 0.8f: 1f)
                    .withEndAction(() -> {
                        v.setVisibility(View.GONE);
                    });
        }
    }

    private void updateVisibility() {
        if (backStack.empty() || currentState != backStack.peek()) {
            backStack.push(currentState);
        }

        if (currentState == AppState.FullscreenMap) {
            backStack.clear();

            if (selectedMarker != null && currentlyViewedEventId != -1) {
                String tag = events.getById(currentlyViewedEventId).primaryTag;
                selectedMarker.setIcon(getEventMarkerBitmap(tag, false));
            }
            selectedMarker = null;
        }

        if (currentState == AppState.EventDetails) {
            showHideView(binding.filterScrollView, false, 0, -200, false);
            showHideView(binding.eventView, true, 0, 1500, false);

            mMap.setPadding(0, 0, 0, 850);
        } else {
            showHideView(binding.filterScrollView, true, 0, 200, false);
            showHideView(binding.eventView, false, 0, 1500, false);

            mMap.setPadding(0, 135, 0, 0);
        }

        if (currentState == AppState.FullscreenMap) {
            SharedPreferences sharedPreferences = getSharedPreferences("sharedPref", MODE_PRIVATE);
            if (sharedPreferences.getBoolean("pointsPopupSeen", false)) {
                SharedPreferences.Editor myEdit = sharedPreferences.edit();
                myEdit.putBoolean("pointsPopupSeen", true);
                myEdit.commit();
                currentState = AppState.PointsPopup;
            }
        }

        if (currentState == AppState.ReportPopup) {
            showHideView(binding.reportPopup, true, 0, 0, true);
        } else {
            showHideView(binding.reportPopup, false, 0, 0, true);
        }

        if (currentState == AppState.Reporting) {
            showHideView(binding.reportingPrimaryLL, true, 0, 0, true);

            showHideView(binding.navi, false, 0, 200, false);
            showHideView(binding.btnAddEvent, false, 0, 500, false);
            showHideView(binding.filterScrollView, false, 0, -200, false);
        } else {
            showHideView(binding.reportingPrimaryLL, false, 0, 0, true);

            showHideView(binding.btnAddEvent, true, 0, 500, false);
            showHideView(binding.navi, true, 0, 200, false);
        }

        // update to profile view
        if (currentState == AppState.ProfileView || currentState == AppState.TopReportersLeaderBoard) {
            binding.eventButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.navi)));
            binding.profileButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.navi_selected)));

            showHideView(binding.profileView, true, 1500, 0, false);
        } else {
            binding.eventButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.navi_selected)));
            binding.profileButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.navi)));

            showHideView(binding.profileView, false, 1500, 0, false);
        }


        // update to top reporters leaderboard view
        if (currentState == AppState.TopReportersLeaderBoard) {
            showHideView(binding.leaderboardPopup, true, 0, 0, true);
        } else {
            showHideView(binding.leaderboardPopup, false, 0, 0, true);
        }

        //popup describing points system (should popup everytime user logs in?)
        if (currentState == AppState.PointsPopup) {
            showHideView(binding.pointsPopup, true, 0, 1500, false);
        } else {
            showHideView(binding.pointsPopup, false, 0, 1500, false);
        }
    }

    // helper function to calculate distance in miles from current location to an event
    private float getEventDistance(Event e) {
        float[] distanceResults = new float[]{-1.0f};
        Location.distanceBetween(e.latitude, e.longitude, locLat, locLong, distanceResults);
        float distanceMiles = distanceResults[0] * 0.000621371f;
        System.out.println("title: " + e.title + " lat: " + e.latitude + " long: " + e.longitude + " meters: " + distanceResults[0]);
        return distanceMiles;
    }

    // helper function to build a map of {event_id: event_distance} for all events within .1 miles
    // map sorted by distance in ascending order
    private LinkedHashMap<Integer, Float> getSortedMap() {
        HashMap<Integer, Float> map = new HashMap<>();
        LinkedHashMap<Integer, Float> sortedMap = new LinkedHashMap<>();
        ArrayList<Float> list = new ArrayList<>();
        float threshold = 0.1f;

        updateKnownLocation();
        List<Event> existing = events.getAll();
        for (int i = 0; i < existing.size(); i++) {
            Event e = existing.get(i);
            float dist = getEventDistance(existing.get(i));
            if (dist < threshold) {
                map.put(e.id, dist);
            }
        }

        for (Map.Entry<Integer, Float> entry : map.entrySet()) {
            list.add(entry.getValue());
        }
        Collections.sort(list);
        for (float num : list) {
            for (Map.Entry<Integer, Float> entry : map.entrySet()) {
                if (entry.getValue().equals(num)) {
                    sortedMap.put(entry.getKey(), num);
                }
            }
        }
        System.out.println(sortedMap);
        return sortedMap;
    }

    // show nearby events within .1 mi in a list, click to view event details
    // if no such event, don't show the popup, directly go to report event view
    // hide placeholders if nearby events fewer than 5
    private void getNearbyEvents() {
        LinkedHashMap<Integer, Float> sortedMap = getSortedMap();

        List<Button> popupEvents = List.of(
                binding.popupEvent1,
                binding.popupEvent2,
                binding.popupEvent3,
                binding.popupEvent4,
                binding.popupEvent5
        );

        List<Button> eventDists = List.of(
                binding.eventDist1,
                binding.eventDist2,
                binding.eventDist3,
                binding.eventDist4,
                binding.eventDist5
        );

        popupEvents.forEach((v) -> {v.setVisibility(View.GONE);});
        eventDists.forEach((v) -> {v.setVisibility(View.GONE);});

        if (sortedMap.size() == 0) {
            currentState = AppState.Reporting;
            updateVisibility();
        } else {
            int i = 0; // counter of events < 0.1 mi
            DecimalFormat df = new DecimalFormat("#.## mi");
            for (Map.Entry<Integer, Float> entry : sortedMap.entrySet()) {
                if (i >= 5) break;

                int event_id = entry.getKey();
                Event event = events.getById(event_id);
                String event_title = event.title;
                float dist = entry.getValue();

                Button popupEvent = popupEvents.get(i);
                Button eventDist = eventDists.get(i);

                popupEvent.setText(event_title);
                eventDist.setText(df.format(dist));

                popupEvent.setVisibility(View.VISIBLE);
                eventDist.setVisibility(View.VISIBLE);

                popupEvent.setOnClickListener((View v) -> {
                    selectMarkerById(event.id);
                    populateEventInfo(event);
                });
                eventDist.setOnClickListener((View v) -> {
                    selectMarkerById(event.id);
                    populateEventInfo(event);
                });
                i++;
            }
        }
    }

    private void selectMarkerById(int id) {
        if (selectedMarker != null && currentlyViewedEventId != -1) {
            String tag = events.getById(currentlyViewedEventId).primaryTag;
            selectedMarker.setIcon(getEventMarkerBitmap(tag, false));
        }
        selectedMarker = null;

        for (Marker m : eventMarkers) {
            if ((int) m.getTag() == id) {
                m.setIcon(getEventMarkerBitmap(events.getById((int) m.getTag()).primaryTag, true));
                selectedMarker = m;
                break;
            }
        }
    }

    private void addEvent(Event e) {
        if (events.getByNameLocation(e.title, e.latitude, e.longitude) == null) {
            events.insertAll(e);
        }

        displayEventMarker(e);
    }

    private void populateEventInfo(Event e) {
        binding.eventName.setText(e.title);
        binding.eventDescription.setText(e.description);
        binding.eventNumNo.setText(Integer.toString(e.numDislikes));
        binding.eventNumYes.setText(Integer.toString(e.numLikes));
        binding.reporterId.setText(e.author);
        binding.reporterPoints.setText(Integer.toString(users.getUserById(e.author).points));
        binding.confirmButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.check)));
        binding.refuteButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.x)));


        Log.d("Author", e.author);
        if (e.author.equals(USER_NAME)){
//            binding.confirmButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.check_selected)));
            binding.confirmButton.setAlpha(0.5f);
            binding.confirmButton.setClickable(false);
//            binding.refuteButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.x_selected)));
            binding.refuteButton.setAlpha(0.5f);
            binding.refuteButton.setClickable(false);
        }
        else{
            binding.confirmButton.setAlpha(1f);
            binding.confirmButton.setClickable(true);
            binding.refuteButton.setAlpha(1f);
            binding.refuteButton.setClickable(true);
            //if not the author of this event and the event has been confirmed/refuted --> set colors
            if (e.isConfirmed) {
                binding.confirmButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.check_selected)));
                binding.refuteButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.x)));
            } else if (e.isRefuted) {
                binding.confirmButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.check)));
                binding.refuteButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.x_selected)));
            }
        }

        updateKnownLocation();

        float[] distanceResults = new float[]{-1.0f};
        Location.distanceBetween(e.latitude, e.longitude, locLat, locLong, distanceResults);

        float distanceMiles = distanceResults[0] * 0.000621371f;

        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);

        binding.distanceBox.setText(df.format(distanceMiles) + " mi");

        long timeDelta = System.currentTimeMillis() - e.lastConfirmed;
        long minutes = timeDelta / (1000 * 60);

        String deltaText = "just now";
        if (e.numLikes == 0) {
            binding.titleLastConfirmed.setText("Event created: ");
        }
        if (minutes > 0) {
            deltaText = Long.toString(minutes) + " minutes ago";
        }
        if (minutes > 60) {
            deltaText = Long.toString(minutes / 60) + " hours ago";
        }
        if (minutes > 1440) {
            deltaText = "a long time ago";
        }
        binding.lastConfirmed.setText(deltaText);

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
        } else if (e.primaryTag.equals("Miscellaneous")) {
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
        } else if (e.secondaryTag.equals("Miscellaneous")) {
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
        } else if (e.tertiaryTag.equals("Miscellaneous")) {
            binding.tag3.setText(e.tertiaryTag);
            binding.tag3.setIcon(getDrawable(R.drawable.ic_misc));
            binding.tag3.setStrokeColor(ColorStateList.valueOf(getColor(R.color.misc)));
            binding.tag3.setBackgroundColor(getColor(R.color.misc));
        } else {
            binding.tag3.setVisibility(View.GONE);
        }
        currentlyViewedEventId = e.id;

        currentState = AppState.EventDetails;
        updateVisibility();
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(e.latitude, e.longitude), 16f
        ));
    }

    private BitmapDescriptor getEventMarkerBitmap(String tag, boolean selected) {
        Map<String, String> tagToMarker = Map.of(
                "Activism", "ic_marker_activism",
                "Food", "ic_marker_food",
                "Miscellaneous", "ic_marker_misc",
                "Performance", "ic_marker_performance",
                "Professional", "ic_marker_professional",
                "Religion", "ic_marker_religion",
                "Shopping", "ic_marker_shopping",
                "Social", "ic_marker_social"
        );

        String identifier = tagToMarker.get(tag);
        if (selected) {
            identifier += "_selected";
        }
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(identifier, "drawable", getPackageName()));

        return BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(
                imageBitmap,
                selected ? 127 : 85,
                selected? 165 : 110,
                false));
    }

    private void displayEventMarker(Event e) {
        Event resolvedEvent = events.getByNameLocation(e.title, e.latitude, e.longitude);
        int id = resolvedEvent.id;

        BitmapDescriptor bitmap = getEventMarkerBitmap(resolvedEvent.primaryTag, false);

        LatLng eventLoc = new LatLng(e.latitude, e.longitude);
        Marker eventMarker = mMap.addMarker(new MarkerOptions()
                .position(eventLoc)
                .title(e.title)
                .icon(bitmap));

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

    private void updatePrimaryTagDropDownSpinner(){
        System.out.println("Inside the function");
        Spinner dynamicSpinnerTags = findViewById(R.id.reporting_spinner_for_primary_tag_dropdown);
        ArrayAdapter<String> dynamicAdapter = new ArrayAdapter<String>(this,R.layout.reporting_spinner_backgroud_color,CheckedTagNames);
        dynamicAdapter.setDropDownViewResource(R.layout.reporting_custom_spinner_dropdrown_text_colour);
        dynamicSpinnerTags.setAdapter(dynamicAdapter);
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.reporting_checkBox1:
                isCheckedOrNotCounter(checked);
                if(checked) {
                    CheckedTagNames.add(binding.reportingCheckBox1.getText().toString());
                    binding.reportingCheckBox1.setButtonTintList(ColorStateList.valueOf(getColor(R.color.food)));
                }
                else {
                    CheckedTagNames.remove(binding.reportingCheckBox1.getText().toString());
                    binding.reportingCheckBox1.setButtonTintList(ColorStateList.valueOf(getColor(R.color.food)));
                }
                System.out.println(CheckedTagNames);
                updatePrimaryTagDropDownSpinner();
                break;

            case R.id.reporting_checkBox2:
                 isCheckedOrNotCounter(checked);
                 if(checked) {
                     CheckedTagNames.add(binding.reportingCheckBox2.getText().toString());
                     binding.reportingCheckBox2.setButtonTintList(ColorStateList.valueOf(getColor(R.color.social)));
                 }
                 else {
                     CheckedTagNames.remove(binding.reportingCheckBox2.getText().toString());
                     binding.reportingCheckBox2.setButtonTintList(ColorStateList.valueOf(getColor(R.color.social)));
                 }
                 System.out.println(CheckedTagNames);
                 updatePrimaryTagDropDownSpinner();
                 break;


            case R.id.reporting_checkBox3:
                 isCheckedOrNotCounter(checked);
                 if(checked) {
                     CheckedTagNames.add(binding.reportingCheckBox3.getText().toString());
                     binding.reportingCheckBox3.setButtonTintList(ColorStateList.valueOf(getColor(R.color.shopping)));
                 }
                 else {
                     CheckedTagNames.remove(binding.reportingCheckBox3.getText().toString());
                     binding.reportingCheckBox3.setButtonTintList(ColorStateList.valueOf(getColor(R.color.shopping)));
                 }
                 System.out.println(CheckedTagNames);
                 updatePrimaryTagDropDownSpinner();
                 break;


            case R.id.reporting_checkBox4:
                 isCheckedOrNotCounter(checked);
                 if(checked) {
                     CheckedTagNames.add(binding.reportingCheckBox4.getText().toString());
                     binding.reportingCheckBox4.setButtonTintList(ColorStateList.valueOf(getColor(R.color.professional)));
                 }
                 else {
                     CheckedTagNames.remove(binding.reportingCheckBox4.getText().toString());
                     binding.reportingCheckBox4.setButtonTintList(ColorStateList.valueOf(getColor(R.color.professional)));
                 }
                 System.out.println(CheckedTagNames);
                 updatePrimaryTagDropDownSpinner();
                 break;

            case R.id.reporting_checkBox5:
                 isCheckedOrNotCounter(checked);
                 if(checked) {
                     CheckedTagNames.add(binding.reportingCheckBox5.getText().toString());
                     binding.reportingCheckBox5.setButtonTintList(ColorStateList.valueOf(getColor(R.color.performance)));
                 }
                 else {
                     CheckedTagNames.remove(binding.reportingCheckBox5.getText().toString());
                     binding.reportingCheckBox5.setButtonTintList(ColorStateList.valueOf(getColor(R.color.performance)));
                 }
                 System.out.println(CheckedTagNames);
                 updatePrimaryTagDropDownSpinner();
                 break;

            case R.id.reporting_checkBox6:
                 isCheckedOrNotCounter(checked);
                 if(checked) {
                     CheckedTagNames.add(binding.reportingCheckBox6.getText().toString());
                     binding.reportingCheckBox6.setButtonTintList(ColorStateList.valueOf(getColor(R.color.activism)));
                 }
                 else {
                     CheckedTagNames.remove(binding.reportingCheckBox6.getText().toString());
                     binding.reportingCheckBox6.setButtonTintList(ColorStateList.valueOf(getColor(R.color.activism)));
                 }
                 updatePrimaryTagDropDownSpinner();
                 System.out.println(CheckedTagNames);
                 break;

            case R.id.reporting_checkBox7:
                 isCheckedOrNotCounter(checked);
                 if(checked) {
                     CheckedTagNames.add(binding.reportingCheckBox7.getText().toString());
                     binding.reportingCheckBox7.setButtonTintList(ColorStateList.valueOf(getColor(R.color.religion)));
                 }
                 else {
                     CheckedTagNames.remove(binding.reportingCheckBox7.getText().toString());
                     binding.reportingCheckBox7.setButtonTintList(ColorStateList.valueOf(getColor(R.color.religion)));
                 }
                 System.out.println(CheckedTagNames);
                 updatePrimaryTagDropDownSpinner();
                 break;

            case R.id.reporting_checkBox8:
                 isCheckedOrNotCounter(checked);
                 if(checked) {
                     CheckedTagNames.add(binding.reportingCheckBox8.getText().toString());
                     binding.reportingCheckBox8.setButtonTintList(ColorStateList.valueOf(getColor(R.color.misc)));
                 }
                 else {
                     CheckedTagNames.remove(binding.reportingCheckBox8.getText().toString());
                     binding.reportingCheckBox8.setButtonTintList(ColorStateList.valueOf(getColor(R.color.misc)));
                 }
                 System.out.println(CheckedTagNames);
                 updatePrimaryTagDropDownSpinner();
                 break;
        }
    }

    private void populateUserInfo(String userId){
        User u = users.getUserById(userId);
        binding.username.setText(u.userId);
        binding.pointsField.setText(Integer.toString(u.points) + " points");
        //TODO: LEVEL, PROGRESS BAR
        int currLevel = (u.points / 50) + 1;
        int pts = u.points % 50;
        int nxtLevelPts = currLevel * 50;
        Log.d("exp", Integer.toString(pts));
        binding.determinateBar.setProgress(pts * 2);
        binding.currLevel.setText(Integer.toString(currLevel ));
        binding.nextLevel.setText(Integer.toString(currLevel + 1));
        binding.currPoints.setText(Integer.toString(nxtLevelPts - 50));
        binding.nextPoints.setText(Integer.toString(nxtLevelPts));
        binding.levelField.setText("Level " + Integer.toString(currLevel));
    }

    private void populateReporterLeaderboardInfo(){
        List<User> users_list = new ArrayList<User>();
        users_list = users.getAllUsers();

        binding.topReporter1Name.setText(users_list.get(0).userId);
        binding.topReporter1Points.setText(users_list.get(0).points + " points");


        if(users_list.size() > 1){
            binding.topReporter2Name.setText(users_list.get(1).userId);
            binding.topReporter2Points.setText(users_list.get(1).points + " points");
        }
        if(users_list.size() > 2){
            binding.topReporter3Name.setText(users_list.get(2).userId);
            binding.topReporter3Points.setText(users_list.get(2).points + " points");
        }
        if(users_list.size() > 3){
            binding.topReporter4Name.setText(users_list.get(3).userId);
            binding.topReporter4Points.setText(users_list.get(3).points + " points");
        }
        if(users_list.size() > 4){
            binding.topReporter5Name.setText(users_list.get(4).userId);
            binding.topReporter5Points.setText(users_list.get(4).points + " points");
        }

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
            if (selectedMarker != null && currentlyViewedEventId != -1) {
                String tag = events.getById(currentlyViewedEventId).primaryTag;
                selectedMarker.setIcon(getEventMarkerBitmap(tag, false));
            }
            selectedMarker = null;

            currentState = AppState.FullscreenMap;
            updateVisibility();
        });

        mMap.setOnMarkerClickListener((Marker m) -> {
            Event event = events.getById((int) m.getTag());

            selectMarkerById(event.id);
            populateEventInfo(event);

            selectedMarker = m;
            return true;
        });

        // Add a marker in Sydney and move the camera
        LatLng quad = new LatLng(40.1074821,-88.2265963);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(quad, 14f));

        Event exampleEvent1 = new Event();
        exampleEvent1.lastConfirmed = System.currentTimeMillis() - (1000 * 60 * 10);
        exampleEvent1.author = "userGuy123";
        exampleEvent1.authorPoints = 350;
        if (!users.isUserExists(exampleEvent1.author)) {
            User exampleUser1 = new User();
            exampleUser1.userId = exampleEvent1.author;
            exampleUser1.points = exampleEvent1.authorPoints;
            users.insertUser(exampleUser1);
        }
        exampleEvent1.description = "We have food. Pull up.";
        exampleEvent1.writtenLocation = "Fourth floor chemistry building!";
        exampleEvent1.title = "Super secret social stuff";
        exampleEvent1.latitude = 40.1074821; exampleEvent1.longitude = -88.2265963;
        exampleEvent1.primaryTag = "Social";
        exampleEvent1.secondaryTag = "Food";
        exampleEvent1.tertiaryTag = "Shopping";
        exampleEvent1.numDislikes = 10;
        exampleEvent1.numLikes = 154;

        Event exampleEvent2 = new Event();
        exampleEvent2.lastConfirmed = System.currentTimeMillis() - (1000 * 60 * 4);
        exampleEvent2.author = "anotherUser";
        exampleEvent2.authorPoints = 200;
        if (!users.isUserExists(exampleEvent2.author)) {
            User exampleUser2 = new User();
            exampleUser2.userId = exampleEvent2.author;
            exampleUser2.points = exampleEvent2.authorPoints;
            users.insertUser(exampleUser2);
        }
        exampleEvent2.description = "Tons of yummy baked goods!";
        exampleEvent2.title = "Amnesty Bake Sale";
        exampleEvent2.primaryTag = "Food";
        exampleEvent2.secondaryTag = "Activism";
        exampleEvent2.latitude = 40.108308; exampleEvent2.longitude = -88.227017;
        exampleEvent2.numDislikes = 3;
        exampleEvent2.numLikes = 26;

        Event exampleEvent3 = new Event();
        exampleEvent3.lastConfirmed = System.currentTimeMillis() - (1000 * 60 * 48);
        exampleEvent3.author = "anotherUser";
        exampleEvent3.authorPoints = 200;
        exampleEvent3.description = "Great tacos and such";
        exampleEvent3.title = "La Paloma Food Truck";
        exampleEvent3.primaryTag = "Food";
        exampleEvent3.latitude = 40.103279; exampleEvent3.longitude = -88.234597;
        exampleEvent3.numDislikes = 0;
        exampleEvent3.numLikes = 10;

        Event exampleEvent4 = new Event();
        exampleEvent4.lastConfirmed = System.currentTimeMillis() - (1000 * 60 * 1);
        exampleEvent4.author = "anotherUser";
        exampleEvent4.authorPoints = 200;
        exampleEvent4.description = "Grainger giving out free merch at their booth";
        exampleEvent4.writtenLocation = "First floor CIF";
        exampleEvent4.title = "Grainger free merch";
        exampleEvent4.primaryTag = "Professional";
        exampleEvent4.latitude = 40.1124; exampleEvent4.longitude = -88.228300;
        exampleEvent4.numDislikes = 1;
        exampleEvent4.numLikes = 12;

        Event exampleEvent5 = new Event();
        exampleEvent5.lastConfirmed = System.currentTimeMillis() - (1000 * 60 * 8);
        exampleEvent5.author = "userGuy123";
        exampleEvent5.authorPoints = 0;
        exampleEvent5.description = "All welcome, show up and play!";
        exampleEvent5.writtenLocation = "Frat park, near PKT house";
        exampleEvent5.title = "Beach volleyball";
        exampleEvent5.primaryTag = "Social";
        exampleEvent5.latitude = 40.105851; exampleEvent5.longitude = -88.236158;
        exampleEvent5.numDislikes = 0;
        exampleEvent5.numLikes = 4;


        Event exampleEvent6 = new Event();
        exampleEvent6.lastConfirmed = System.currentTimeMillis() - (1000 * 60 * 5);
        exampleEvent6.author = "PasserbyJane";
        exampleEvent6.authorPoints = 150;
        if (!users.isUserExists(exampleEvent6.author)) {
            User exampleUser6 = new User();
            exampleUser6.userId = exampleEvent6.author;
            exampleUser6.points = exampleEvent6.authorPoints;
            users.insertUser(exampleUser6);
        }
        exampleEvent6.description = "Christian preachers handing out pamphlets.";
        exampleEvent6.writtenLocation = "On the corner of Wright/Green, near the Alma";
        exampleEvent6.title = "Union preaching";
        exampleEvent6.primaryTag = "Religion";
        exampleEvent6.latitude = 40.1098; exampleEvent6.longitude = -88.2283;
        exampleEvent6.numDislikes = 10;
        exampleEvent6.numLikes = 40;

        Event exampleEvent7 = new Event();
        exampleEvent7.lastConfirmed = System.currentTimeMillis() - (1000 * 60 * 2);
        exampleEvent7.author = "PasserbyJane";
        exampleEvent7.authorPoints = 150;
        exampleEvent7.description = "Fresh fruits and veggies being sold on the quad!";
        exampleEvent7.writtenLocation = "North side quad";
        exampleEvent7.title = "Farmers market popup";
        exampleEvent7.primaryTag = "Shopping";
        exampleEvent7.secondaryTag = "Food";
        exampleEvent7.latitude = 40.1090; exampleEvent7.longitude = -88.2272;
        exampleEvent7.numDislikes = 0;
        exampleEvent7.numLikes = 15;

        Event exampleEvent8 = new Event();
        exampleEvent8.lastConfirmed = System.currentTimeMillis() - (1000 * 60 * 1);
        exampleEvent8.author = "UIUCTheatreClub";
        exampleEvent8.authorPoints = 50;
        if (!users.isUserExists(exampleEvent8.author)) {
            User exampleUser8 = new User();
            exampleUser8.userId = exampleEvent8.author;
            exampleUser8.points = exampleEvent8.authorPoints;
            users.insertUser(exampleUser8);
        }
        exampleEvent8.description = "Join us for our production of Freaky Friday now!";
        exampleEvent8.writtenLocation = "Lincoln hall auditorium";
        exampleEvent8.title = "UIUC Freaky Friday Production";
        exampleEvent8.primaryTag = "Performance";
        exampleEvent8.latitude = 40.1066; exampleEvent8.longitude = -88.2282;
        exampleEvent8.numDislikes = 0;
        exampleEvent8.numLikes = 25;

        Event exampleEvent9 = new Event();
        exampleEvent9.lastConfirmed = System.currentTimeMillis() - (1000 * 60 * 0);
        exampleEvent9.author = "PasserbyJane";
        exampleEvent9.authorPoints = 150;
        exampleEvent9.description = "Protest on the quad for Dreamers";
        exampleEvent9.writtenLocation = "Foellinger ";
        exampleEvent9.title = "Protest for DACA rights";
        exampleEvent9.primaryTag = "Activism";
        exampleEvent9.latitude = 40.1059; exampleEvent9.longitude = -88.2272;
        exampleEvent9.numDislikes = 1;
        exampleEvent9.numLikes = 100;

        Event exampleEvent10 = new Event();
        exampleEvent10.lastConfirmed = System.currentTimeMillis() - (1000 * 60 * 5);
        exampleEvent10.author = "anotherUser";
        exampleEvent10.authorPoints = 200;
        exampleEvent10.description = "Moving -- giving away furniture: Coffee table, couch, bar stools";
        exampleEvent10.writtenLocation = "Freer Hall, 906 S Goodwin Ave, Urbana";
        exampleEvent10.title = "Giving away free furniture";
        exampleEvent10.primaryTag = "Miscellaneous";
        exampleEvent10.latitude = 40.1048; exampleEvent10.longitude = -88.2229;
        exampleEvent10.numDislikes = 3;
        exampleEvent10.numLikes = 5;

        if (events.getByNameLocation(exampleEvent1.title, exampleEvent1.latitude, exampleEvent1.longitude) == null) {
            events.insertAll(exampleEvent1);
        }
        if (events.getByNameLocation(exampleEvent2.title, exampleEvent2.latitude, exampleEvent2.longitude) == null) {
            events.insertAll(exampleEvent2);
        }
        if (events.getByNameLocation(exampleEvent3.title, exampleEvent3.latitude, exampleEvent3.longitude) == null) {
            events.insertAll(exampleEvent3);
        }
        if (events.getByNameLocation(exampleEvent4.title, exampleEvent4.latitude, exampleEvent4.longitude) == null) {
            events.insertAll(exampleEvent4);
        }
        if (events.getByNameLocation(exampleEvent5.title, exampleEvent5.latitude, exampleEvent5.longitude) == null) {
            events.insertAll(exampleEvent5);
        }
        if (events.getByNameLocation(exampleEvent6.title, exampleEvent6.latitude, exampleEvent6.longitude) == null) {
            events.insertAll(exampleEvent6);
        }
        if (events.getByNameLocation(exampleEvent7.title, exampleEvent7.latitude, exampleEvent7.longitude) == null) {
            events.insertAll(exampleEvent7);
        }
        if (events.getByNameLocation(exampleEvent8.title, exampleEvent8.latitude, exampleEvent8.longitude) == null) {
            events.insertAll(exampleEvent8);
        }
        if (events.getByNameLocation(exampleEvent9.title, exampleEvent9.latitude, exampleEvent9.longitude) == null) {
            events.insertAll(exampleEvent9);
        }
        if (events.getByNameLocation(exampleEvent10.title, exampleEvent10.latitude, exampleEvent10.longitude) == null) {
            events.insertAll(exampleEvent10);
        }

        googleMap.setMyLocationEnabled(true);

        events.getAll().forEach(this::addEvent);

        currentState = AppState.PointsPopup;
        updateVisibility();
    }
}
