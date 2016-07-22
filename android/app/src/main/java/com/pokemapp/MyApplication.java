package com.pokemapp;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.joda.time.DateTime;
import rx.Observable;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

@SuppressWarnings("MissingPermission")
public class MyApplication extends Application {

    private final List<String> WANTED = Arrays.asList(
          "charmander",
          "charizard",
          "charmeleon",
          "bulbasaur",
          "pikachu",
          "squirtle",
          "mew",
          "mewto"
    );

    PublishSubject<Pokemon> s = PublishSubject.create();
    LocationManager locationManager;
    private Set<Pokemon> poks = new HashSet<>();
    private Set<Pokemon> notified = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        Realm.setDefaultConfiguration(new RealmConfiguration.Builder(this).build());

        RealmResults<Pokemon> hiddens = Realm.getDefaultInstance()
              .where(Pokemon.class)
              .greaterThan("timeMs", System.currentTimeMillis())
              .findAll();
        poks = new HashSet<>(hiddens);

        locationManager.requestLocationUpdates(
              LocationManager.GPS_PROVIDER,
              10,
              10,
              new LocationListener() {
                  @Override
                  public void onLocationChanged(final Location location) {
                      float[] results = new float[1];
                      if (location != null) {
                          for (Pokemon pok : poks) {
                              if (!notified.contains(pok)) {
                                  Location.distanceBetween(location.getLatitude(), location.getLongitude(), pok.getLat(), pok.getLon(), results);
                                  if (results[0] < 100) {
                                      MyApplication.this.notify(pok, results);
                                      notified.add(pok);
                                  }
                              }
                          }
                      }
                  }

                  @Override
                  public void onStatusChanged(final String s, final int i, final Bundle bundle) {

                  }

                  @Override
                  public void onProviderEnabled(final String s) {

                  }

                  @Override
                  public void onProviderDisabled(final String s) {

                  }
              }
        );
    }

    public Observable<Pokemon> poks() {
        return s.doOnNext(new Action1<Pokemon>() {
            @Override
            public void call(final Pokemon pokemon) {
                if (!poks.contains(pokemon)) {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    float[] results = new float[1];
                    if (lastKnownLocation != null) {
                        Location.distanceBetween(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), pokemon.getLat(), pokemon.getLon(), results);
                    }

                    if (results[0] < 100 || WANTED.contains(pokemon.getName())) {
                        MyApplication.this.notify(pokemon, results);
                    }
                }
            }
        }).doOnNext(new Action1<Pokemon>() {
            @Override
            public void call(final Pokemon pokemon) {
                synchronized (poks) {
                    poks.add(pokemon);
                    Iterator<Pokemon> iterator = poks.iterator();
                    while (iterator.hasNext()) {
                        if (new DateTime(iterator.next().getTimeMS()).isBeforeNow()) {
                            iterator.remove();
                        }
                    }
                }
            }
        })
              .startWith(poks);
    }

    private void notify(final Pokemon pokemon, final float[] results) {

        NotificationManager systemService = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(MyApplication.this, MapsActivity.class);
        intent.putExtra("lat", pokemon.getLat());
        intent.putExtra("lon", pokemon.getLon());
        intent.putExtra("a", System.currentTimeMillis());

        int id = (int) (Math.random() * 1000000);
        Notification no = new Notification.Builder(MyApplication.this)
              .setContentTitle(pokemon.name + " " + (int) results[0] + "m")
              .setContentText(new DateTime(pokemon.getTimeMS()).toString("HH:mm:ss"))
              .setSmallIcon(getResources().getIdentifier("p_" + pokemon.id, "drawable", getPackageName()))
              .setContentIntent(
                    PendingIntent.getActivity(MyApplication.this, id, intent, PendingIntent.FLAG_ONE_SHOT)
              )
              .setLargeIcon(a("p_" + pokemon.id))
              .setDefaults(Notification.DEFAULT_ALL)
              .setAutoCancel(true)
              .build();

        systemService.notify(id, no);
    }

    private Bitmap a(String name) {
        Resources resources = getResources();
        final int resourceId = resources.getIdentifier(name, "drawable",
              getPackageName());
        return BitmapFactory.decodeResource(resources, resourceId);
    }
}
