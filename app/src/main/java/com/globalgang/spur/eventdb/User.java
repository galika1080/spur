package com.globalgang.spur.eventdb;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.google.android.gms.maps.model.LatLng;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
public class User {
    @NonNull
    @PrimaryKey(autoGenerate = false)
    public String userId;

    @ColumnInfo(name = "points")
    public int points;

    @Embedded
    public ArrayList<Event> confirmed_events = new ArrayList<Event>();

    @Embedded
    public ArrayList<Event> refuted_events = new ArrayList<Event>();

}

