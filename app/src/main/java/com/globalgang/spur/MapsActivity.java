package com.globalgang.spur;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import android.content.res.ColorStateList;
import android.opengl.Visibility;
import android.os.Bundle;

import com.globalgang.spur.eventdb.AppDatabase;
import com.globalgang.spur.eventdb.Event;
import com.globalgang.spur.eventdb.EventDao;
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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventMarkers = new ArrayList<>();

        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "spur-db").allowMainThreadQueries().build();
        events = db.eventDao();

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

        // filter buttons change marker visibility
        binding.btnFilterFood.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag == "Food") {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.secondaryTag == "Food") {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.tertiaryTag == "Food") {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterSocial.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag == "Social") {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.secondaryTag == "Social") {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.tertiaryTag == "Social") {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterShopping.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag == "Shopping") {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.secondaryTag == "Shopping") {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.tertiaryTag == "Shopping") {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterProfessional.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag == "Professional") {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.secondaryTag == "Professional") {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.tertiaryTag == "Professional") {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterPerformance.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag == "Performance") {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.secondaryTag == "Performance") {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.tertiaryTag == "Performance") {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterActivism.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag == "Activism") {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.secondaryTag == "Activism") {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.tertiaryTag == "Activism") {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterReligion.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag == "Religion") {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.secondaryTag == "Religion") {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.tertiaryTag == "Religion") {
                    eventMarkers.get(i).setVisible(true);
                } else {
                    eventMarkers.get(i).setVisible(false);
                }
            }
        });
        binding.btnFilterMisc.setOnClickListener((View v) -> {
            for (int i = 0; i < eventMarkers.size(); i++) {
                Event event = events.getById((int) eventMarkers.get(i).getTag());
                if (event.primaryTag == "Misc") {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.secondaryTag == "Misc") {
                    eventMarkers.get(i).setVisible(true);
                } else if (event.tertiaryTag == "Misc") {
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
            currentState = AppState.Reporting; // @TODO: add layout for reporting
            updateVisibility();
        });

        binding.popupButtonNo.setOnClickListener((View v) -> {
            currentState = AppState.FullscreenMap; // @TODO: add layout for reporting
            updateVisibility();
        });

        //clicking on submit button on report screen take you back to the main map screen
        //@TODO: clicking on submit button should take to event screen
        binding.reportingSubmitButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                currentState = AppState.FullscreenMap;
                updateVisibility();
            }
        });

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

        if(currentState == AppState.Reporting) {
            binding.reportingPrimaryLL.setVisibility(View.VISIBLE);
            binding.filterScrollView.setVisibility(View.GONE);
            binding.navi.setVisibility(View.GONE);
            binding.btnAddEvent.setVisibility(View.GONE);
        } else {
            binding.reportingPrimaryLL.setVisibility(View.GONE);
        }

        // @TODO: move profile.xml code to activity_maps.xml and set visibility
        //update to profile view
        if(currentState == AppState.ProfileView) {
            //profile state, show layout as visible

        }

        // @TODO: move points_popup_display.xml code to activity_maps.xml and set visibility
        if(currentState == AppState.PointsPopup) {
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
        binding.reporterPoints.setText(e.authorPoints);

        //fixing tag display (top 3 tags)
        if (e.primaryTag == "Food") {
            binding.tag1.setText(e.primaryTag);
            binding.tag1.setIcon(getDrawable(R.drawable.ic_food));
            binding.tag1.setStrokeColor(ColorStateList.valueOf(getColor(R.color.food)));
            binding.tag1.setBackgroundColor(getColor(R.color.food));
        } else if (e.primaryTag == "Social") {
            binding.tag1.setText(e.primaryTag);
            binding.tag1.setIcon(getDrawable(R.drawable.ic_social));
            binding.tag1.setStrokeColor(ColorStateList.valueOf(getColor(R.color.social)));
            binding.tag1.setBackgroundColor(getColor(R.color.social));
        } else if (e.primaryTag == "Shopping") {
            binding.tag1.setText(e.primaryTag);
            binding.tag1.setIcon(getDrawable(R.drawable.ic_shopping));
            binding.tag1.setStrokeColor(ColorStateList.valueOf(getColor(R.color.shopping)));
            binding.tag1.setBackgroundColor(getColor(R.color.shopping));
        } else if (e.primaryTag == "Professional") {
            binding.tag1.setText(e.primaryTag);
            binding.tag1.setIcon(getDrawable(R.drawable.ic_professional));
            binding.tag1.setStrokeColor(ColorStateList.valueOf(getColor(R.color.professional)));
            binding.tag1.setBackgroundColor(getColor(R.color.professional));
        } else if (e.primaryTag == "Performance") {
            binding.tag1.setText(e.primaryTag);
            binding.tag1.setIcon(getDrawable(R.drawable.ic_performance));
            binding.tag1.setStrokeColor(ColorStateList.valueOf(getColor(R.color.performance)));
            binding.tag1.setBackgroundColor(getColor(R.color.performance));
        } else if (e.primaryTag == "Activism") {
            binding.tag1.setText(e.primaryTag);
            binding.tag1.setIcon(getDrawable(R.drawable.ic_activism));
            binding.tag1.setStrokeColor(ColorStateList.valueOf(getColor(R.color.activism)));
            binding.tag1.setBackgroundColor(getColor(R.color.activism));
        } else if (e.primaryTag == "Religion") {
            binding.tag1.setText(e.primaryTag);
            binding.tag1.setIcon(getDrawable(R.drawable.ic_religion));
            binding.tag1.setStrokeColor(ColorStateList.valueOf(getColor(R.color.religion)));
            binding.tag1.setBackgroundColor(getColor(R.color.religion));
        } else if (e.primaryTag == "Misc") {
            binding.tag1.setText(e.primaryTag);
            binding.tag1.setIcon(getDrawable(R.drawable.ic_misc));
            binding.tag1.setStrokeColor(ColorStateList.valueOf(getColor(R.color.misc)));
            binding.tag1.setBackgroundColor(getColor(R.color.misc));
        } else {
            binding.tag1.setVisibility(View.GONE);
        }

        if (e.secondaryTag == "Food") {
            binding.tag2.setText(e.secondaryTag);
            binding.tag2.setIcon(getDrawable(R.drawable.ic_food));
            binding.tag2.setStrokeColor(ColorStateList.valueOf(getColor(R.color.food)));
            binding.tag2.setBackgroundColor(getColor(R.color.food));
        } else if (e.secondaryTag == "Social") {
            binding.tag2.setText(e.secondaryTag);
            binding.tag2.setIcon(getDrawable(R.drawable.ic_social));
            binding.tag2.setStrokeColor(ColorStateList.valueOf(getColor(R.color.social)));
            binding.tag2.setBackgroundColor(getColor(R.color.social));
        } else if (e.secondaryTag == "Shopping") {
            binding.tag2.setText(e.secondaryTag);
            binding.tag2.setIcon(getDrawable(R.drawable.ic_shopping));
            binding.tag2.setStrokeColor(ColorStateList.valueOf(getColor(R.color.shopping)));
            binding.tag2.setBackgroundColor(getColor(R.color.shopping));
        } else if (e.secondaryTag == "Professional") {
            binding.tag2.setText(e.secondaryTag);
            binding.tag2.setIcon(getDrawable(R.drawable.ic_professional));
            binding.tag2.setStrokeColor(ColorStateList.valueOf(getColor(R.color.professional)));
            binding.tag2.setBackgroundColor(getColor(R.color.professional));
        } else if (e.secondaryTag == "Performance") {
            binding.tag2.setText(e.secondaryTag);
            binding.tag2.setIcon(getDrawable(R.drawable.ic_performance));
            binding.tag2.setStrokeColor(ColorStateList.valueOf(getColor(R.color.performance)));
            binding.tag2.setBackgroundColor(getColor(R.color.performance));
        } else if (e.secondaryTag == "Activism") {
            binding.tag2.setText(e.secondaryTag);
            binding.tag2.setIcon(getDrawable(R.drawable.ic_activism));
            binding.tag2.setStrokeColor(ColorStateList.valueOf(getColor(R.color.activism)));
            binding.tag2.setBackgroundColor(getColor(R.color.activism));
        } else if (e.secondaryTag == "Religion") {
            binding.tag2.setText(e.secondaryTag);
            binding.tag2.setIcon(getDrawable(R.drawable.ic_religion));
            binding.tag2.setStrokeColor(ColorStateList.valueOf(getColor(R.color.religion)));
            binding.tag2.setBackgroundColor(getColor(R.color.religion));
        } else if (e.secondaryTag == "Misc") {
            binding.tag2.setText(e.secondaryTag);
            binding.tag2.setIcon(getDrawable(R.drawable.ic_misc));
            binding.tag2.setStrokeColor(ColorStateList.valueOf(getColor(R.color.misc)));
            binding.tag2.setBackgroundColor(getColor(R.color.misc));
        } else {
            binding.tag2.setVisibility(View.GONE);
        }

        if (e.tertiaryTag == "Food") {
            binding.tag3.setText(e.tertiaryTag);
            binding.tag3.setIcon(getDrawable(R.drawable.ic_food));
            binding.tag3.setStrokeColor(ColorStateList.valueOf(getColor(R.color.food)));
            binding.tag3.setBackgroundColor(getColor(R.color.food));
        } else if (e.tertiaryTag == "Social") {
            binding.tag3.setText(e.tertiaryTag);
            binding.tag3.setIcon(getDrawable(R.drawable.ic_social));
            binding.tag3.setStrokeColor(ColorStateList.valueOf(getColor(R.color.social)));
            binding.tag3.setBackgroundColor(getColor(R.color.social));
        } else if (e.tertiaryTag == "Shopping") {
            binding.tag3.setText(e.tertiaryTag);
            binding.tag3.setIcon(getDrawable(R.drawable.ic_shopping));
            binding.tag3.setStrokeColor(ColorStateList.valueOf(getColor(R.color.shopping)));
            binding.tag3.setBackgroundColor(getColor(R.color.shopping));
        } else if (e.tertiaryTag == "Professional") {
            binding.tag3.setText(e.tertiaryTag);
            binding.tag3.setIcon(getDrawable(R.drawable.ic_professional));
            binding.tag3.setStrokeColor(ColorStateList.valueOf(getColor(R.color.professional)));
            binding.tag3.setBackgroundColor(getColor(R.color.professional));
        } else if (e.tertiaryTag == "Performance") {
            binding.tag3.setText(e.tertiaryTag);
            binding.tag3.setIcon(getDrawable(R.drawable.ic_performance));
            binding.tag3.setStrokeColor(ColorStateList.valueOf(getColor(R.color.performance)));
            binding.tag3.setBackgroundColor(getColor(R.color.performance));
        } else if (e.tertiaryTag == "Activism") {
            binding.tag3.setText(e.tertiaryTag);
            binding.tag3.setIcon(getDrawable(R.drawable.ic_activism));
            binding.tag3.setStrokeColor(ColorStateList.valueOf(getColor(R.color.activism)));
            binding.tag3.setBackgroundColor(getColor(R.color.activism));
        } else if (e.tertiaryTag == "Religion") {
            binding.tag3.setText(e.tertiaryTag);
            binding.tag3.setIcon(getDrawable(R.drawable.ic_religion));
            binding.tag3.setStrokeColor(ColorStateList.valueOf(getColor(R.color.religion)));
            binding.tag3.setBackgroundColor(getColor(R.color.religion));
        } else if (e.tertiaryTag == "Misc") {
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

        LatLng eventLoc = new LatLng(e.latitude, e.longitude);
        Marker eventMarker = mMap.addMarker(new MarkerOptions()
                .position(eventLoc)
                .title(e.title)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_pin_food)));

        eventMarker.setTag(id);
        eventMarkers.add(eventMarker);
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
        exampleEvent1.writtenLocation = "Fourth floor chemistry building!";
        exampleEvent1.title = "Super secret social stuff";
        exampleEvent1.latitude = 40.1074821; exampleEvent1.longitude = -88.2265963;
        exampleEvent1.primaryTag = "Social";

        Event exampleEvent2 = new Event();
        exampleEvent2.author = "anotherUser";
        exampleEvent2.description = "Tons of food!";
        exampleEvent1.writtenLocation = "South of the Union, north quad";
        exampleEvent2.title = "Bake sale on the quad";
        exampleEvent2.latitude = 40.108308; exampleEvent2.longitude = -88.227017;

        addEvent(exampleEvent1);
        addEvent(exampleEvent2);
    }
}