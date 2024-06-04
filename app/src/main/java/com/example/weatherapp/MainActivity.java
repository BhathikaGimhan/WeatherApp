package com.example.weatherapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String API_KEY = "eb1ccc7ba01215bf3135cc28c81e4db3";
    private static final String TAG = "MainActivity";

    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvLatitude, tvLongitude, tvAddress, tvTime, tvWeatherInfo;
    private Button btnRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLatitude = findViewById(R.id.tv_latitude);
        tvLongitude = findViewById(R.id.tv_longitude);
        tvAddress = findViewById(R.id.tv_address);
        tvTime = findViewById(R.id.tv_time);
        tvWeatherInfo = findViewById(R.id.tv_weather_info);
        btnRefresh = findViewById(R.id.btn_refresh);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnRefresh.setOnClickListener(view -> fetchLocation());

        fetchLocation();
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    tvLatitude.setText("Latitude: " + latitude);
                    tvLongitude.setText("Longitude: " + longitude);
                    reverseGeocode(latitude, longitude);
                    fetchWeatherData(latitude, longitude);
                    updateTime();
                } else {
                    Log.e(TAG, "Location is null");
                }
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to get location", e));
    }

    @SuppressLint("SetTextI18n")
    private void reverseGeocode(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                tvAddress.setText("Address: " + address.getAddressLine(0));
            } else {
                tvAddress.setText("Address: Unable to find address");
            }
        } catch (IOException e) {
            e.printStackTrace();
            tvAddress.setText("Address: Unable to find address");
        }
    }

    @SuppressLint("SimpleDateFormat")
    private void updateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String currentTime = sdf.format(new Date());
        tvTime.setText("Current Time: " + currentTime);
    }

    private void fetchWeatherData(double latitude, double longitude) {
        String urlString = BASE_URL + "?lat=" + latitude + "&lon=" + longitude + "&appid=" + API_KEY + "&units=metric";
        new FetchWeatherTask().execute(urlString);
    }

    @SuppressLint("StaticFieldLeak")
    private class FetchWeatherTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    reader.close();
                } else {
                    Log.e(TAG, "HTTP error code: " + responseCode);
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Error fetching weather data", e);
                return null;
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONObject main = jsonObject.getJSONObject("main");
                    JSONArray weatherArray = jsonObject.getJSONArray("weather");
                    JSONObject weather = weatherArray.getJSONObject(0);

                    String weatherInfo = "Temperature: " + main.getDouble("temp") + "°C\n" +
                            "Humidity: " + main.getInt("humidity") + "%\n" +
                            "Description: " + weather.getString("description");
                    tvWeatherInfo.setText("Weather Info:\n" + weatherInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Error parsing weather data", e);
                    tvWeatherInfo.setText("Weather Info: Unable to fetch data");
                }
            } else {
                tvWeatherInfo.setText("Weather Info: Unable to fetch data");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                Log.e(TAG, "Permission denied");
            }
        }
    }
}
