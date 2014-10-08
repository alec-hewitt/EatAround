package com.ahewdev.eataround;

import android.location.Location;
import android.location.LocationManager;

/**
 * Created by Alec on 8/1/2014.
 */
public class Est extends Location{

    boolean visible;

    double alt_diff;
    double distance;
    double altitude;

    double yOffset;
    double xOffset;

    double xPos;
    double yPos;

    int id;

    public Est(String provider, double latitude, double longitude, double altitude) {
        super(provider);

        this.setLatitude(latitude);
        this.setLongitude(longitude);
        this.setAltitude(altitude);

    }

}
