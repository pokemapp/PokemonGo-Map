package com.pokemapp;

import io.realm.RealmObject;

public class Pokemon extends RealmObject {

    String name;

    int id;

    double lat;

    double lon;

    long timeMs;

    public Pokemon() {
    }

    public int getId() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getName() {
        return name;
    }

    public long getTimeMS() {
        return timeMs;
    }

       @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pokemon pokemon = (Pokemon) o;

        if (id != pokemon.id) return false;
        if (Double.compare(pokemon.lat, lat) != 0) return false;
        if (Double.compare(pokemon.lon, lon) != 0) return false;
        return name.equals(pokemon.name);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = name.hashCode();
        result = 31 * result + id;
        temp = Double.doubleToLongBits(lat);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(lon);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        return result;
    }
}
