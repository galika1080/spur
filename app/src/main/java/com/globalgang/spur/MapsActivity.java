package com.globalgang.spur;

import androidx.fragment.app.FragmentActivity;

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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    private EventDao events;

    private enum AppState {
        FullscreenMap,
        EventDetails,
        Reporting,
        ReportPopup,
        PointsPopup
    }

    private AppState currentState = AppState.FullscreenMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        ArrayAdapter<CharSequence>adapter=ArrayAdapter.createFromResource(this, R.array.tags_array, android.R.layout.simple_spinner_item);

        // @TODO: change this, the social button shouldn't show the event details popup
        binding.btnFilterSocial.setOnClickListener((View v) -> {
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
            currentState = AppState.Reporting; // @TODO: add layout for reporting
            updateVisibility();
        });

        binding.popupButtonNo.setOnClickListener((View v) -> {
            currentState = AppState.FullscreenMap; // @TODO: add layout for reporting
            updateVisibility();
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
        }

        if (currentState == AppState.ReportPopup) {
            binding.reportPopup.setVisibility(View.VISIBLE);
        } else {
            binding.reportPopup.setVisibility(View.GONE);
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