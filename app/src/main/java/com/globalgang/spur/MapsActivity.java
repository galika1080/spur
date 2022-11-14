package com.globalgang.spur;

import static com.globalgang.spur.BuildConfig.DEBUG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import android.opengl.Visibility;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.CompoundButton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

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
    //initialising the state to FullScreenMap (filters + bottom bav bar)
    private AppState currentState = AppState.FullscreenMap;

    // one boolean variable to check whether all the text fields in Reporting Screen
    // are filled by the user, properly or not.
    boolean isAllFieldsCheckedReporting = false;
    // one boolean variable to check whether all the tags in Reporting Screen
    // are filled by the user, properly or not.
    boolean isAllTagsCheckedReporting = false;

    private int CheckboxCounterTracker = 0;

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

        //Obtain the dropdown id for reporting screen
        Spinner spinnerTags = findViewById(R.id.reporting_spinner_for_primary_tag_dropdown);
        ArrayAdapter<CharSequence>adapter=ArrayAdapter.createFromResource(this, R.array.tags_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerTags.setAdapter(adapter);

        //init user profile
        populateUserInfo("rick");

        // @TODO: change this, the social button shouldn't show the event details popup
        binding.btnFilterFood.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (!event.tags.contains("food")) {
                    eventMarkers.get(i).hideInfoWindow();
                }
            }
        });
        binding.btnFilterSocial.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (!event.tags.contains("social")) {
                    eventMarkers.get(i).setVisible(false);
                } else {
                    eventMarkers.get(i).setVisible(true);
                }
            }
        });
        binding.btnFilterShopping.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (!event.tags.contains("shopping")) {
                    eventMarkers.get(i).hideInfoWindow();
                }
            }
        });
        binding.btnFilterProfessional.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (!event.tags.contains("professional")) {
                    eventMarkers.get(i).hideInfoWindow();
                }
            }
        });
        binding.btnFilterPerformance.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (!event.tags.contains("performance")) {
                    eventMarkers.get(i).hideInfoWindow();
                }
            }
        });
        binding.btnFilterActivism.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (!event.tags.contains("activism")) {
                    eventMarkers.get(i).hideInfoWindow();
                }
            }
        });
        binding.btnFilterReligion.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (!event.tags.contains("religion")) {
                    eventMarkers.get(i).hideInfoWindow();
                }
            }
        });
        binding.btnFilterMisc.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (!event.tags.contains("misc")) {
                    eventMarkers.get(i).hideInfoWindow();
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


        //clicking on submit button on report screen take you back to the main map screen
        //if the form has been filled properly
        binding.reportingSubmitButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                isAllFieldsCheckedReporting = CheckAllFields();
                isAllTagsCheckedReporting = CheckAllTags();
                if (isAllFieldsCheckedReporting && isAllTagsCheckedReporting) {
                    currentState = AppState.FullscreenMap;
                    updateVisibility();
                }
            }
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
    }

    private void addEvent(Event e) {
        events.insertAll(e);

        populateEventInfo(e);
        displayEventMarker(e);
    }

    private void populateEventInfo(Event e) {
        binding.eventName.setText(e.title);
        binding.eventDescription.setText(e.description);
        binding.eventNumNo.setText(Integer.toString(e.numDislikes));
        binding.eventNumYes.setText(Integer.toString(e.numLikes));
    }

    private void displayEventMarker(Event e) {
        Event resolvedEvent = events.getByNameLocation(e.title, e.latitude, e.longitude);
        int id = resolvedEvent.id;

        LatLng eventLoc = new LatLng(e.latitude, e.longitude);
        Marker eventMarker = mMap.addMarker(new MarkerOptions()
                .position(eventLoc)
                .title(e.title)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_pin_food)));

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

    private void populateUserInfo(String userId){
        User u = new User();
        u = users.getUserById(userId);
        binding.username.setText(u.userId);
        binding.pointsField.setText(u.points);

        //TODO: LEVEL, PROGRESS BAR
    }

    private void addUser(User u) {
        users.insertUser(u);
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
        exampleEvent1.description = "This is an event. You should pull up.";
        exampleEvent1.title = "Super secret social stuff";
        exampleEvent1.latitude = 40.1074821; exampleEvent1.longitude = -88.2265963;

        Event exampleEvent2 = new Event();
        exampleEvent2.author = "anotherUser";
        exampleEvent2.description = "Tons of food!";
        exampleEvent2.title = "Bake sale on the quad";
        exampleEvent2.latitude = 40.108308; exampleEvent2.longitude = -88.227017;

        addEvent(exampleEvent1);
        addEvent(exampleEvent2);
    }
}
