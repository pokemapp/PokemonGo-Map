package com.pokemapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.joda.time.DateTime;
import rx.Subscription;
import rx.functions.Action1;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private final Handler handler = new Handler();

    private GoogleMap mMap;

    private Set<Pokemon> poks = new HashSet<>();

    private Subscription mSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
              .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 1);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String token = FirebaseInstanceId.getInstance().getToken();
                System.out.println(token);
                ((TextView) findViewById(R.id.tTV)).setText(token);

                if (token == null) {
                    handler.postDelayed(this, 1000);
                }
            }
        }, 1000);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                synchronized (poks) {
                    boolean removed = false;
                    for (Iterator<Pokemon> iter = poks.iterator(); iter.hasNext(); ) {
                        if (new DateTime(iter.next().getTimeMS()).isBeforeNow()) {
                            removed = true;
                            iter.remove();
                        }
                    }

                    if (removed) {
                        mMap.clear();

                        for (Pokemon pokemon : poks) {
                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(new LatLng(pokemon.getLat(), pokemon.getLon()));
                            markerOptions.title(pokemon.getName() + "; " + new DateTime(pokemon.getTimeMS()).toString("HH:mm:ss"));
                            Bitmap a = a("p_" + pokemon.getId());
                            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(a);
                            markerOptions.icon(bitmapDescriptor);
                            mMap.addMarker(markerOptions);
                        }
                    }
                }
                handler.postDelayed(this, 10000);
            }
        }, 10000);

        mSubscription = ((MyApplication) getApplication()).poks().subscribe(new Action1<Pokemon>() {
            @Override
            public void call(final Pokemon pokemon) {
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        ((TextView) findViewById(R.id.tTV)).setText("Last updated: " + DateTime.now().toString("HH:mm:ss"));

                        synchronized (poks) {
                            if (!poks.contains(pokemon)) {

                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.position(new LatLng(pokemon.getLat(), pokemon.getLon()));
                                markerOptions.title(pokemon.getName() + "; " + new DateTime(pokemon.getTimeMS()).toString("HH:mm:ss"));
                                Bitmap a = a("p_" + pokemon.getId());
                                BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(a);
                                markerOptions.icon(bitmapDescriptor);
                                mMap.addMarker(markerOptions);
                            }
                            poks.add(pokemon);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);

        double lat = intent.getDoubleExtra("lat", 0.0);
        double lon = intent.getDoubleExtra("lon", 0.0);
        final CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 17);

        if (mMap != null) {
            mMap.animateCamera(cameraUpdate);
            return;
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
              .findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {

                googleMap.animateCamera(cameraUpdate);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mSubscription != null) {
            mSubscription.unsubscribe();
        }
    }

    private Bitmap a(String name) {
        Resources resources = getResources();
        final int resourceId = resources.getIdentifier(name, "drawable",
              getPackageName());
        return BitmapFactory.decodeResource(resources, resourceId);
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }
}
