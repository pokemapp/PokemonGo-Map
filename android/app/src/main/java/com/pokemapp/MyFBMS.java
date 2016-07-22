package com.pokemapp;

import android.os.Handler;
import android.os.Looper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import io.realm.Realm;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MyFBMS extends FirebaseMessagingService {

    private static final List<String> IGNORED = Arrays.asList(
          "rattata",
          "pidgey",
          "drowsee",
          "raticate",
          "pidgeot",
          "pidgeotto",
          "spearow",
          "fearow",
          "zubat",
          "golbat"
    );

    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        final Map<String, String> data = remoteMessage.getData();
        System.out.println(data);

        if (IGNORED.contains(data.get("pokename"))) {
            return;
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();

                Pokemon pokemon = realm.createObject(Pokemon.class);
                pokemon.name = data.get("pokename");
                pokemon.id = Integer.parseInt(data.get("pokeId"));
                pokemon.lat = Double.parseDouble(data.get("lat"));
                pokemon.lon = Double.parseDouble(data.get("lon"));
                pokemon.timeMs = Double.valueOf(data.get("hiddens")).longValue() * 1000;

                realm.commitTransaction();

                ((MyApplication) getApplication()).s.onNext(
                      pokemon);
            }
        });
    }
}
