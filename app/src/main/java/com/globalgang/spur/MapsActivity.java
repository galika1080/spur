package com.globalgang.spur;

import androidx.fragment.app.FragmentActivity;

import android.opengl.Visibility;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.globalgang.spur.databinding.ActivityMapsBinding;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    private enum AppState {
        FullscreenMap,
        EventDetails,
        Reporting,
        ReportPopup,
        PointsPopup
    }
    //initialising the state to FullScreenMap (filters + bottom bav bar)
    private AppState currentState = AppState.FullscreenMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        // @TODO: change this, the social button shouldn't show the event details popup
        binding.btnFilterSocial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentState = AppState.EventDetails;
                updateVisibility();
            }
        });


        // report popup button
        binding.btnAddEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentState = AppState.ReportPopup;
                updateVisibility();
            }
        });


        // yes and no buttons on "heads up, nearby events" popup
        binding.popupButtonYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentState = AppState.Reporting; // @TODO: add layout for reporting //reporting screen has been added
                updateVisibility();
            }
        });

        binding.popupButtonNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentState = AppState.FullscreenMap;
                updateVisibility();
            }
        });

        //clicking on submit button on report screen take you back to the main map screen
        binding.reportingSubmitButton.setOnClickListener(new View.OnClickListener(){
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

        // Add a marker in Sydney and move the camera
        LatLng quad = new LatLng(40.1074821,-88.2265963);
        LatLng quad2 = new LatLng(40.108308, -88.227017);
        LatLng quad3 = new LatLng(40.108111, -88.227671);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(quad));

        mMap.addMarker(new MarkerOptions()
                .position(quad)
                .title("Food truck!")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_pin_food)));

        mMap.addMarker(new MarkerOptions()
                .position(quad2)
                .title("Social event!")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_pin_social)));

        mMap.addMarker(new MarkerOptions()
                .position(quad3)
                .title("Prayer group!")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_pin_religion)));

    }
}