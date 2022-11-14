package com.globalgang.spur.eventdb;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

@Entity
public class User {
    @NonNull
    @PrimaryKey(autoGenerate = false)
    public String userId;

    @ColumnInfo(name = "points")
    public int points;
}
