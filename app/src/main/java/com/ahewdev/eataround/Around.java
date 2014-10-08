package com.ahewdev.eataround;

import java.math.*;
import java.lang.StrictMath.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.math.BigInteger;

public class Around extends Activity {

    //debug
    TextView xoffset;
    TextView yoffset;
    TextView zoffset;

    //sensors
    public static SensorManager mSensorManager;
    public static Sensor accelerometer;
    public static Sensor magnetometer;
    public static SensorEventListener SEL;

    //sensors
    float[] values = new float[3];
    float[] rotationMatrix = new float[16];
    float[] Ra = new float[16];
    float[] I = new float[16];
    float[] gravity = new float[16];
    float[] geomagnetic = new float[16];

    private List<float[]> mRotHist = new ArrayList<float[]>();
    private int mRotHistIndex;
    // Change the value so that the azimuth is stable and fit your requirement
    private int mHistoryMaxLength = 40;
    float[] mGravity;
    float[] mMagnetic;
    float[] mRotationMatrix = new float[9];
    // the direction of the back camera, only valid if the device is tilted up by
// at least 25 degrees.
    private float mFacing = Float.NaN;

    public static final float TWENTY_FIVE_DEGREE_IN_RADIAN = 0.436332313f;
    public static final float ONE_FIFTY_FIVE_DEGREE_IN_RADIAN = 2.7052603f;

    double accy;
    double accx;
    double accz;
    double tempXoffset;
    double tempYoffset;

    //location
    double altitude;
    double alt_angle;
    double bearingTo;

    //Est
    Est[] ests;
    Est sample;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_around);

        //debug screen
        xoffset = (TextView) findViewById(R.id.xoffset);
        yoffset = (TextView) findViewById(R.id.yoffset);
        zoffset = (TextView) findViewById(R.id.zoffset);

        //Sensor
        //accelerometer sensor
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        SEL = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                {
                    mGravity = event.values.clone();
                }
                else
                {
                    mMagnetic = event.values.clone();
                }

                if (mGravity != null && mMagnetic != null)
                {
                    //fill rotation matrix with sensor data
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, mGravity, mMagnetic))
                    {
                        // inclination is the degree of tilt by the device independent of orientation (portrait or landscape)
                        // if less than 25 or more than 155 degrees the device is considered lying flat
                        float inclination = (float) Math.acos(rotationMatrix[8]);
                        if (inclination < TWENTY_FIVE_DEGREE_IN_RADIAN
                                || inclination > ONE_FIFTY_FIVE_DEGREE_IN_RADIAN)
                        {
                            // mFacing is undefined, so we need to clear the history
                            clearRotHist();
                            mFacing = Float.NaN;
                        }
                        else
                        {
                            setRotHist();
                            // mFacing = azimuth is in radian
                            mFacing = findFacing();
                            xoffset.setText(Float.toString(mFacing));
                        }
                    }
                }
            };

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        //Location
        //refernce to system location manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        //define listener that responds to location update
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                getRestaurants(location);
                setPositions(location);
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
            @Override
            public void onProviderEnabled(String provider) {
            }
            @Override
            public void onProviderDisabled(String provider) {
            }
        }; //location listener

        //Register location listener with location manager to recieve updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        //get location
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        int rate = SensorManager.SENSOR_DELAY_UI;
        mSensorManager.registerListener(SEL, accelerometer, rate);
        mSensorManager.registerListener(SEL, magnetometer, rate);

    } //onCreate

    //retrieve yelp data within range, and set in array and make class
    public void getRestaurants(Location location) {
        sample = new Est("manual", 43.623029, -70.262042, 100.50); //mock Est
    }

    //Calculates X and Y offset, then translates in-range targets to pixel positions.
    public void setPositions(Location location){

        //trigonometric degree calculations

        //bearingTo
        bearingTo = location.bearingTo(sample);
        if(bearingTo < 0){
            bearingTo = 360 + bearingTo;
        }

        //tangent on x-axis
        sample.alt_diff = sample.getAltitude() - location.getAltitude();
        sample.distance = location.distanceTo(sample);
        alt_angle = StrictMath.atan(sample.alt_diff / sample.distance);

        //**temporary xOffset & yOffset for 360 degree redefinition and calculation**//
        //x
        tempXoffset = bearingTo - accy;

        //y
        tempYoffset = alt_angle - accx;

        //assign offset values to Est class iterations
        sample.yOffset = tempYoffset;
        sample.xOffset = tempXoffset;

    }

    private void clearRotHist()
    {
        mRotHist.clear();
        mRotHistIndex = 0;
    }

    private void setRotHist()
    {
        float[] hist = mRotationMatrix.clone();
        if (mRotHist.size() == mHistoryMaxLength)
        {
            mRotHist.remove(mRotHistIndex);
        }
        mRotHist.add(mRotHistIndex++, hist);
        mRotHistIndex %= mHistoryMaxLength;
    }

    private float findFacing()
    {
        float[] averageRotHist = average(mRotHist);
        return (float) Math.atan2(-averageRotHist[2], -averageRotHist[5]);
    }

    public float[] average(List<float[]> values)
    {
        float[] result = new float[9];
        for (float[] value : values)
        {
            for (int i = 0; i < 9; i++)
            {
                result[i] += value[i];
            }
        }

        for (int i = 0; i < 9; i++)
        {
            result[i] = result[i] / values.size();
        }

        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.around, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}