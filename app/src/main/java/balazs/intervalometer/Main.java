package balazs.intervalometer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Main extends ActionBarActivity implements LocationListener, SensorEventListener {
    static Bitmap img;
    static int counter = 0;
    static SurfaceView sv;
    static CameraPreview camPreview;
    //from http://stackoverflow.com/questions/3972912/android-direction-sensor
    static int roll, pitch, azimuth;
    static SensorManager sensorManager;
    static Sensor accelerometer, magnetometer;
    static float[] data_accelerometer, data_magnetometer;
    Button btn1, btn2;
    ImageView iv;
    String TAG = "INTERVALOMETER";
    boolean camready = true;
    SeekBar sb;
    TextView tv, tx_gps, tx_orientation;
    boolean running = false;
    int interval_millis = 15000;
    Handler handler = new Handler();
    Location lastloc = new Location("dummy");
    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "onJPEGTaken");
            BitmapFactory.Options bfo = new BitmapFactory.Options();
            //scale down, or else out of memory
            bfo.inJustDecodeBounds = true;
            img = BitmapFactory.decodeByteArray(data, 0, data.length, bfo);
            int heightRatio = nextpowerof2((int) Math.ceil(bfo.outHeight / (float) iv.getHeight()));
            int widthRatio = nextpowerof2((int) Math.ceil(bfo.outWidth / (float) iv.getWidth()));
            if (heightRatio > 1 || widthRatio > 1) {
                if (heightRatio > widthRatio) {
                    bfo.inSampleSize = heightRatio;
                } else {
                    bfo.inSampleSize = widthRatio;
                }
            }
            bfo.inJustDecodeBounds = false;
            img = BitmapFactory.decodeByteArray(data, 0, data.length, bfo);
            iv.setImageBitmap(img);
            File dir = new File(Environment.getExternalStorageDirectory() + "/_IMAGES");
            dir.mkdirs();
            String datestr = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            File file = new File(Environment.getExternalStorageDirectory() + "/_IMAGES/" +
                    datestr + ".jpg");
            File dir2 = new File(Environment.getExternalStorageDirectory() + "/_GPS");
            dir2.mkdirs();
            File file2 = new File(Environment.getExternalStorageDirectory() + "/_GPS/" +
                    datestr + ".txt");
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.flush();
                fos.close();
                FileOutputStream fos2 = new FileOutputStream(file2);
                fos2.write(("latitude=" + lastloc.getLatitude() + "\nlongitude=" + lastloc.getLongitude() +
                        "\naccuracy=" + lastloc.getAccuracy() + "\naltitude=" + lastloc.getAltitude() +
                        "\nspeed=" + lastloc.getSpeed() + "\ndirection=" + lastloc.getBearing() +
                        "\nroll=" + roll + "\npitch=" + pitch + "\nazimuth=" + azimuth).getBytes());
                fos2.flush();
                fos2.close();
                Toast.makeText(btn1.getContext(), "SAVED", Toast.LENGTH_SHORT).show();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            setTitle((++counter) + " PICTURES TAKEN");
            camPreview.resumePreview();
            camready = true;
        }
    };
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (!camready)
                handler.postDelayed(this, 1000);
            if (!running)
                return;
            Toast.makeText(tv.getContext(), "TAKING PICTURE", Toast.LENGTH_SHORT).show();
            camready = false;
            camPreview.takePicture(jpegCallback);
            handler.postDelayed(this, interval_millis);
        }
    };

    static int nextpowerof2(int num) {
        return (int) Math.pow(2, Math.ceil(Math.log(num) / Math.log(2)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
        btn1 = (Button) findViewById(R.id.btn1);
        btn2 = (Button) findViewById(R.id.btn2);
        iv = (ImageView) findViewById(R.id.iv);
        tv = (TextView) findViewById(R.id.tv);
        sb = (SeekBar) findViewById(R.id.sb);
        tx_gps = (TextView) findViewById(R.id.tx_gps);
        tx_orientation = (TextView) findViewById(R.id.tx_orientation);
        sv = (SurfaceView) findViewById(R.id.surfaceView);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int time = 5 + progress * 5;
                tv.setText(time + "s");
                interval_millis = time * 1000;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        SurfaceHolder holder = sv.getHolder();
        int width = 352; // must set a compatible value, otherwise it gets the default width and height
        int height = 288;
        camPreview = new CameraPreview(width, height);
        holder.addCallback(camPreview);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn1.setEnabled(false);
                btn2.setEnabled(true);
                running = true;
                sb.setEnabled(false);
                Toast.makeText(v.getContext(), "STARTING", Toast.LENGTH_SHORT).show();
                handler.postDelayed(runnable, 1000);
            }
        });
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn1.setEnabled(true);
                btn2.setEnabled(false);
                running = false;
                sb.setEnabled(true);
                Toast.makeText(v.getContext(), "STOPPING", Toast.LENGTH_SHORT).show();
                handler.removeCallbacks(runnable);
            }
        });
        //  Toast.makeText(this, "CAMERA: " + sizes.get(maxId).width + "x" + sizes.get(maxId).height, Toast.LENGTH_SHORT).show();
        LocationManager locman = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, magnetometer);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "GOT LOCATION");
        lastloc = location;
        tx_gps.setText(
                "LAT: " + location.getLatitude() + "\nLON: " + location.getLongitude() +
                        "\nACC: " + location.getAccuracy() + "\nSPD:" + location.getSpeed() +
                        "\nDIR: " + location.getBearing() + "\nALT:" + location.getAltitude());
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        // onSensorChanged gets called for each sensor so we have to remember the values
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            data_accelerometer = event.values;
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            data_magnetometer = event.values;
        }

        if (data_accelerometer != null && data_magnetometer != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, data_accelerometer, data_magnetometer);

            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                // at this point, orientation contains the azimuth(direction), pitch and roll values.
                azimuth = (int) (180 * orientation[0] / Math.PI);
                pitch = (int) (180 * orientation[1] / Math.PI);
                roll = (int) (180 * orientation[2] / Math.PI);
                tx_orientation.setText("AZIMUTH: " + azimuth + "\nPITCH: " + pitch + "\nROLL: " + roll);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
