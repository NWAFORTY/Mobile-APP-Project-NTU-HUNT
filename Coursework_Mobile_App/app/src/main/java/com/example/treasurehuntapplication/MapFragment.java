package com.example.treasurehuntapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    /** Only the accepted hunt target pin appears, and only when the user is within this distance. */
    private static final float PIN_VISIBLE_METERS = 40f;
    /** User must be at least this close to tap the pin and collect. */
    private static final float COLLECT_TAP_METERS = 28f;
    /** Red circle on the map shows this hunt zone (meters); visible whenever a hunt is active. */
    private static final double SEARCH_AREA_RADIUS_METERS = 40.0;

    private static final float DEFAULT_ZOOM = CampusMapDefaults.ZOOM_OVERVIEW;

    // --- Map & location ---
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback proximityCallback;
    private boolean locationUpdatesActive;
    private Location lastUserLocation;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // --- Fused location + callback: walk tracking + refresh markers ---
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        proximityCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc != null) {
                    lastUserLocation = loc;
                    WalkDistanceTracker.recordLocation(requireContext().getApplicationContext(), loc);
                    refreshTreasureMarkers();
                }
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        if (supportMapFragment != null) {
            supportMapFragment.getMapAsync(this);
        }
        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        // --- Map UI defaults & campus overview ---
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        mMap.setOnMarkerClickListener(this::onTreasureMarkerClick);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CampusMapDefaults.cliftonCampus(), DEFAULT_ZOOM));

        if (hasLocationPermission()) {
            try {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } catch (SecurityException ignored) {
            }
            moveCameraToLastKnownOrUserOrStayOnCampus();
            seedLastLocationOnce();
        }

        refreshTreasureMarkers();
        maybeStartProximityUpdates();
    }

    /** Prime lastUserLocation from cache so markers can appear without waiting for first update. */
    private void seedLastLocationOnce() {
        if (!hasLocationPermission()) {
            return;
        }
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), loc -> {
                if (loc != null) {
                    lastUserLocation = loc;
                    refreshTreasureMarkers();
                }
            });
        } catch (SecurityException ignored) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMap != null) {
            refreshTreasureMarkers();
            maybeStartProximityUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopProximityUpdates();
    }

    /** Fine location granted — required for my-location layer and hunt logic. */
    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /** Prefer device position when near campus; otherwise stay on Clifton default. */
    private void moveCameraToLastKnownOrUserOrStayOnCampus() {
        if (!hasLocationPermission()) {
            return;
        }
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (mMap == null) {
                    return;
                }
                if (location != null && CampusMapDefaults.shouldUseDeviceLocationForCamera(location)) {
                    lastUserLocation = location;
                    LatLng cur = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cur, DEFAULT_ZOOM));
                } else if (location == null) {
                    requestFreshLocationOrCampus();
                } else {
                    lastUserLocation = location;
                }
            });
        } catch (SecurityException ignored) {
        }
    }

    /** If last location is null, try a fresh high-accuracy read before falling back to campus. */
    private void requestFreshLocationOrCampus() {
        if (!hasLocationPermission()) {
            return;
        }
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (mMap == null) {
                            return;
                        }
                        if (location != null && CampusMapDefaults.shouldUseDeviceLocationForCamera(location)) {
                            lastUserLocation = location;
                            LatLng cur = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cur, DEFAULT_ZOOM));
                        } else {
                            if (location != null) {
                                lastUserLocation = location;
                            }
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CampusMapDefaults.cliftonCampus(), DEFAULT_ZOOM));
                        }
                        refreshTreasureMarkers();
                    });
        } catch (SecurityException ignored) {
        }
    }

    /**
     * Draws the accepted active treasure: a red search circle at the target always (so you can pan to it),
     * plus a tappable pin when you are within {@link #PIN_VISIBLE_METERS}.
     */
    private void refreshTreasureMarkers() {
        if (mMap == null) {
            return;
        }
        mMap.clear();
        if (hasLocationPermission()) {
            try {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } catch (SecurityException ignored) {
            }
        }

        Treasure active = ActiveTreasureManager.getActiveTreasure(requireContext());
        if (active == null || TreasureManager.isFound(requireContext(), active.getId())) {
            return;
        }

        LatLng pos = new LatLng(active.getLatitude(), active.getLongitude());

        int strokeRgb = ContextCompat.getColor(requireContext(), R.color.ntuRed);
        int fill = Color.argb(55, Color.red(strokeRgb), Color.green(strokeRgb), Color.blue(strokeRgb));
        mMap.addCircle(new CircleOptions()
                .center(pos)
                .radius(SEARCH_AREA_RADIUS_METERS)
                .strokeColor(strokeRgb)
                .strokeWidth(5f)
                .fillColor(fill));

        if (lastUserLocation == null) {
            return;
        }

        float[] distance = new float[1];
        Location.distanceBetween(
                lastUserLocation.getLatitude(),
                lastUserLocation.getLongitude(),
                active.getLatitude(),
                active.getLongitude(),
                distance);
        if (distance[0] > PIN_VISIBLE_METERS) {
            return;
        }

        MarkerOptions mo = new MarkerOptions()
                .position(pos)
                .title(active.getTitle())
                .snippet(getString(R.string.map_pin_snippet))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        Marker marker = mMap.addMarker(mo);
        if (marker != null) {
            marker.setTag(active.getId());
        }
    }

    /** Distance-gated collect dialog; ignores wrong marker / inactive hunts. */
    private boolean onTreasureMarkerClick(@NonNull Marker marker) {
        Object tag = marker.getTag();
        if (!(tag instanceof Integer)) {
            return false;
        }
        int treasureId = (Integer) tag;
        Treasure treasure = TreasureManager.getById(treasureId);
        if (treasure == null || TreasureManager.isFound(requireContext(), treasureId)) {
            return true;
        }
        if (!ActiveTreasureManager.isActive(requireContext(), treasureId)) {
            return true;
        }
        if (lastUserLocation == null) {
            Toast.makeText(requireContext(), R.string.map_wait_for_location, Toast.LENGTH_SHORT).show();
            return true;
        }
        float[] d = new float[1];
        Location.distanceBetween(
                lastUserLocation.getLatitude(),
                lastUserLocation.getLongitude(),
                treasure.getLatitude(),
                treasure.getLongitude(),
                d);
        if (d[0] > COLLECT_TAP_METERS) {
            Toast.makeText(requireContext(), R.string.map_too_far_to_collect, Toast.LENGTH_SHORT).show();
            return true;
        }

        NtuMaterialDialogs.builder(requireContext())
                .setTitle(R.string.map_collect_title)
                .setMessage(getString(R.string.map_collect_message, treasure.getTitle(), treasure.getPoints()))
                .setPositiveButton(R.string.map_collect_confirm, (dialog, which) -> onTreasureCollected(treasure))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        return true;
    }

    /** Periodic GPS updates while map is visible (walk distance + marker refresh). */
    private void maybeStartProximityUpdates() {
        if (!hasLocationPermission() || mMap == null) {
            return;
        }
        Treasure active = ActiveTreasureManager.getActiveTreasure(requireContext());
        if (active == null || TreasureManager.isFound(requireContext(), active.getId())) {
            stopProximityUpdates();
            return;
        }
        if (locationUpdatesActive) {
            return;
        }
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
                .setMinUpdateIntervalMillis(2_000L)
                .setMaxUpdateDelayMillis(8_000L)
                .build();
        try {
            fusedLocationClient.requestLocationUpdates(request, proximityCallback, Looper.getMainLooper());
            locationUpdatesActive = true;
        } catch (SecurityException e) {
            locationUpdatesActive = false;
        }
    }

    /** Stops GPS updates when fragment pauses to save battery. */
    private void stopProximityUpdates() {
        if (!locationUpdatesActive) {
            return;
        }
        try {
            fusedLocationClient.removeLocationUpdates(proximityCallback);
        } catch (SecurityException ignored) {
        }
        locationUpdatesActive = false;
    }

    /** Persists found state, points, clears active hunt, syncs Firestore, refreshes map. */
    private void onTreasureCollected(@NonNull Treasure treasure) {
        TreasureManager.markFound(requireContext(), treasure.getId());
        ActiveTreasureManager.clearActiveTreasure(requireContext());
        ScoreRepository.addPoints(requireContext(), treasure.getPoints());
        UserGameStateFirestoreSync.pushGameStateToFirestore(requireContext());
        Toast.makeText(requireContext(),
                getString(R.string.toast_treasure_found, treasure.getPoints()),
                Toast.LENGTH_LONG).show();
        stopProximityUpdates();
        refreshTreasureMarkers();
    }
}
