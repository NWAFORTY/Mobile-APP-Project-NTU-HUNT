package com.example.treasurehuntapplication;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

/** Default map camera for NTU Clifton area. */
public final class CampusMapDefaults {

    public static final float ZOOM_OVERVIEW = 16f;

    private static final LatLng CLIFTON_CAMPUS = new LatLng(52.9115, -1.1865);
    /** Rough radius (m) within which we prefer the device location over the campus default. */
    private static final float NEAR_CAMPUS_METERS = 25_000f;

    private CampusMapDefaults() {
    }

    public static LatLng cliftonCampus() {
        return CLIFTON_CAMPUS;
    }

    public static boolean shouldUseDeviceLocationForCamera(Location location) {
        if (location == null) {
            return false;
        }
        float[] out = new float[1];
        Location.distanceBetween(
                location.getLatitude(),
                location.getLongitude(),
                CLIFTON_CAMPUS.latitude,
                CLIFTON_CAMPUS.longitude,
                out);
        return out[0] <= NEAR_CAMPUS_METERS;
    }
}
