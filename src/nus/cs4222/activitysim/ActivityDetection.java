package nus.cs4222.activitysim;

import java.util.*;
import java.text.*;

import android.hardware.SensorManager;
import android.util.*;

/**
 Class containing the activity detection algorithm.

 <p> You can code your activity detection algorithm in this class.
 (You may add more Java class files or add libraries in the 'libs'
 folder if you need).
 The different callbacks are invoked as per the sensor log files,
 in the increasing order of timestamps. In the best case, you will
 simply need to copy paste this class file (and any supporting class
 files and libraries) to the Android app without modification
 (in stage 2 of the project).

 <p> Remember that your detection algorithm executes as the sensor data arrives
 one by one. Once you have detected the user's current activity, output
 it using the {@link ActivitySimulator.outputDetectedActivity(UserActivities)}
 method. If the detected activity changes later on, then you need to output the
 newly detected activity using the same method, and so on.
 The detected activities are logged to the file "DetectedActivities.txt",
 in the same folder as your sensor logs.

 <p> To get the current simulator time, use the method
 {@link ActivitySimulator.currentTimeMillis()}. You can set timers using
 the {@link SimulatorTimer} class if you require. You can log to the
 console/DDMS using either {@code System.out.println()} or using the
 {@link android.util.Log} class. You can use the {@code SensorManager.getRotationMatrix()}
 method (and any other helpful methods) as you would normally do on Android.

 <p> Note: Since this is a simulator, DO NOT create threads, DO NOT sleep(),
 or do anything that can cause the simulator to stall/pause. You
 can however use timers if you require, see the documentation of the
 {@link SimulatorTimer} class.
 In the simulator, the timers are faked. When you copy the code into an
 actual Android app, the timers are real, but the code of this class
 does not need not be modified.
 */
public class ActivityDetection {

    private boolean last_outdoors = false;
    private boolean isIdle = false;

    private float speed;
    private UserActivities last_activity;

    // Avg of the accl window
    float acclSum = 0.0F;

    // Avg of the light window
    float lightSum = 0.0F;

    /* LINEAR ACCL */
    long ACCL_WINDOW_SIZE = 4 * 1000; // millis
    float ACCL_THRESHOLD = 0.01F * SensorManager.GRAVITY_EARTH;

    // Timestamped lin accl value
    private static class TimestampedLinAcclValue {
        public float mag;
        public long timestamp;
        public TimestampedLinAcclValue( float mag , long timestamp ) {
            this.timestamp = timestamp;
            this.mag = mag;
        }
    }
    // Window for lin accl values
    LinkedList< TimestampedLinAcclValue > acclWindow =
            new LinkedList<TimestampedLinAcclValue>();
    boolean isFirstAcclReading = true;



    /* LIGHT INTENSITY */
    long LIGHT_WINDOW_SIZE = 4 * 1000;
    float LIGHT_THRESHOLD = 300;

    private static class TimestampedLightValue {
        public float light;
        public long timestamp;
        public TimestampedLightValue(float light, long timestamp) {
            this.timestamp = timestamp;
            this.light = light;
        }
    }
    LinkedList<TimestampedLightValue> lightWindow =
            new LinkedList<>();
    boolean isFirstLightReading = true;




    public void initDetection()
            throws Exception {
        // Add initialisation code here, if any
    }

    /** De-initialises the detection algorithm. */
    public void deinitDetection()
            throws Exception {
        // Add de-initialisation code here, if any
    }

    /**
     Called when the accelerometer sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   x            Accl x value (m/sec^2)
     @param   y            Accl y value (m/sec^2)
     @param   z            Accl z value (m/sec^2)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onAcclSensorChanged( long timestamp ,
                                     float x ,
                                     float y ,
                                     float z ,
                                     int accuracy ) {

        // Process the sensor data as they arrive in each callback,
        //  with all the processing in the callback itself (don't create threads).
    }

    /**
     Called when the gravity sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   x            Gravity x value (m/sec^2)
     @param   y            Gravity y value (m/sec^2)
     @param   z            Gravity z value (m/sec^2)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onGravitySensorChanged( long timestamp ,
                                        float x ,
                                        float y ,
                                        float z ,
                                        int accuracy ) {
    }

    /**
     Called when the linear accelerometer sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   x            Linear Accl x value (m/sec^2)
     @param   y            Linear Accl y value (m/sec^2)
     @param   z            Linear Accl z value (m/sec^2)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onLinearAcclSensorChanged( long timestamp ,
                                           float x ,
                                           float y ,
                                           float z ,
                                           int accuracy ) {
        // Add the value to the window
        float mag = (float) Math.sqrt( x * x + y * y + z * z);
        acclWindow.add( new TimestampedLinAcclValue( mag , timestamp ) );
        acclSum += mag;

        // Purge old values from the window
        Iterator< TimestampedLinAcclValue > i = acclWindow.iterator();
        while( i.hasNext() ) {
            TimestampedLinAcclValue value = i.next();
            if( timestamp - value.timestamp > ACCL_WINDOW_SIZE ) {
                i.remove();
                acclSum -= value.mag;
            }
        }

        float avgAccl = acclSum / acclWindow.size();
        float stddev = 0.0F;
        for( TimestampedLinAcclValue value : acclWindow ) {
            stddev += ( value.mag - avgAccl ) * ( value.mag - avgAccl );
        }
        stddev /= acclWindow.size();
        stddev = (float) Math.sqrt( stddev );
        if( stddev < ACCL_THRESHOLD ) {
            isIdle= true;
            if (last_outdoors) {
                ActivitySimulator.outputDetectedActivity(UserActivities.IDLE_OUTDOOR);
            } else {
                ActivitySimulator.outputDetectedActivity(UserActivities.IDLE_INDOOR);
            }
            //ActivitySimulator.outputDetectedActivity(isIdle);
        }
        else {
            isIdle = false;
            if(0.7 < stddev) {
                ActivitySimulator.outputDetectedActivity(UserActivities.WALKING);
            } else {
                ActivitySimulator.outputDetectedActivity(UserActivities.BUS);
            }
        }
    }

    /**
     Called when the magnetic sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   x            Magnetic x value (microTesla)
     @param   y            Magnetic y value (microTesla)
     @param   z            Magnetic z value (microTesla)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onMagneticSensorChanged( long timestamp ,
                                         float x ,
                                         float y ,
                                         float z ,
                                         int accuracy ) {
    }

    /**
     Called when the gyroscope sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   x            Gyroscope x value (rad/sec)
     @param   y            Gyroscope y value (rad/sec)
     @param   z            Gyroscope z value (rad/sec)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onGyroscopeSensorChanged( long timestamp ,
                                          float x ,
                                          float y ,
                                          float z ,
                                          int accuracy ) {
    }

    /**
     Called when the rotation vector sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   x            Rotation vector x value (unitless)
     @param   y            Rotation vector y value (unitless)
     @param   z            Rotation vector z value (unitless)
     @param   scalar       Rotation vector scalar value (unitless)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onRotationVectorSensorChanged( long timestamp ,
                                               float x ,
                                               float y ,
                                               float z ,
                                               float scalar ,
                                               int accuracy ) {
    }

    /**
     Called when the barometer sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   pressure     Barometer pressure value (millibar)
     @param   altitude     Barometer altitude value w.r.t. standard sea level reference (meters)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onBarometerSensorChanged( long timestamp ,
                                          float pressure ,
                                          float altitude ,
                                          int accuracy ) {
    }

    /**
     Called when the light sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   light        Light value (lux)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onLightSensorChanged( long timestamp ,
                                      float light ,
                                      int accuracy ) {
        // Add the value to the window
        lightWindow.add( new TimestampedLightValue( light , timestamp ) );
        lightSum += light;

        // Purge old values from the window
        Iterator< TimestampedLightValue > i = lightWindow.iterator();
        while( i.hasNext() ) {
            TimestampedLightValue value = i.next();
            if( timestamp - value.timestamp > LIGHT_WINDOW_SIZE ) {
                i.remove();
                lightSum -= value.light;
            }
        }

        if(isIdle) {
            float avgLight = lightSum / lightWindow.size();
            if( avgLight < LIGHT_THRESHOLD ) {
                last_outdoors = false;
                ActivitySimulator.outputDetectedActivity(UserActivities.IDLE_INDOOR);
            }
            else {
                last_outdoors = true;
                ActivitySimulator.outputDetectedActivity(UserActivities.IDLE_OUTDOOR);
            }
        }
    }

    /**
     Called when the proximity sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   proximity    Proximity value (cm)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onProximitySensorChanged( long timestamp ,
                                          float proximity ,
                                          int accuracy ) {
    }

    /**
     Called when the location sensor has changed.

     @param   timestamp    Timestamp of this location event
     @param   provider     "gps" or "network"
     @param   latitude     Latitude (deg)
     @param   longitude    Longitude (deg)
     @param   accuracy     Accuracy of the location data (you may use this) (meters)
     @param   altitude     Altitude (meters) (may be -1 if unavailable)
     @param   bearing      Bearing (deg) (may be -1 if unavailable)
     @param   speed        Speed (m/sec) (may be -1 if unavailable)
     */
    public void onLocationSensorChanged( long timestamp ,
                                         String provider ,
                                         double latitude ,
                                         double longitude ,
                                         float accuracy ,
                                         double altitude ,
                                         float bearing ,
                                         float speed ) {
        this.speed = speed;
    }

    /** Helper method to convert UNIX millis time into a human-readable string. */
    private static String convertUnixTimeToReadableString( long millisec ) {
        return sdf.format( new Date( millisec ) );
    }

    /** To format the UNIX millis time as a human-readable string. */
    private static final SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd-h-mm-ssa" );

    // Dummy variables used in the dummy timer code example
    //private boolean isFirstAcclReading = true;
    //private boolean isUserOutside = false;
    private int numberTimers = 1;

    private Runnable task = new Runnable() {
        public void run() {

            // Logging to the DDMS (in the simulator, the DDMS log is to the console)
            System.out.println();
            Log.i( "ActivitySim" , "Timer " + numberTimers + ": Current simulator time: " +
                    convertUnixTimeToReadableString( ActivitySimulator.currentTimeMillis() ) );
            System.out.println( "Timer " + numberTimers + ": Current simulator time: " +
                    convertUnixTimeToReadableString( ActivitySimulator.currentTimeMillis() ) );

            // Dummy example of outputting a detected activity
            //  (to the file "DetectedActivities.txt" in the trace folder).
            //  (here we just alternate between indoor and walking every 10 min)
            UserActivities detectedActivity;

            // Avg value over the slidung window

//                // TODO: Detection Algorithm
//                /*x within -1 to 1 and y within -1 to 1, idling*/
//                if ( (x<1 && x>-1) && (y<1 && y>-1) ) {
//                  isIdle = true;
//                } else {
//                  isIdle = false;
//                }
//
//                if(isIdle) {
//                  if (light<300) {
//                    detectedActivity = UserActivities.IDLE_INDOOR;
//                  } else {
//                    detectedActivity = UserActivities.IDLE_OUTDOOR;
//                  }
//                } else {
//                  if(speed > 3) {
//                    detectedActivity = UserActivities.BUS;
//                  } else {
//                    detectedActivity =  UserActivities.WALKING;
//                  }
//                }

//                last_activity = detectedActivity;
//                ActivitySimulator.outputDetectedActivity(detectedActivity);

            //isUserOutside = !isUserOutside;

            SimulatorTimer timer = new SimulatorTimer();
            timer.schedule( task ,             // Task to be executed
                    2 * 1000 );  // Delay in millisec (10 min)
        }
    };
}