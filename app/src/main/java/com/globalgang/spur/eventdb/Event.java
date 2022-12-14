package com.globalgang.spur.eventdb;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

@Entity
public class Event {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "loc_lat")
    public double latitude;

    @ColumnInfo(name = "loc_long")
    public double longitude;

    @ColumnInfo(name = "written_location")
    public String writtenLocation;

    @ColumnInfo(name = "primary_tag")
    public String primaryTag;

    @ColumnInfo(name = "secondary_tag")
    public String secondaryTag;

    @ColumnInfo(name = "tertiary_tag")
    public String tertiaryTag;

    @ColumnInfo(name = "author")
    public String author;

    @ColumnInfo(name = "author_points")
    public int authorPoints;

    @ColumnInfo(name = "num_likes")
    public int numLikes;

    @ColumnInfo(name = "num_dislikes")
    public int numDislikes;

    @ColumnInfo(name = "is_refuted")
    public boolean isRefuted;

    @ColumnInfo(name = "is_confirmed")
    public boolean isConfirmed;

    @ColumnInfo(name = "first_confirmed")
    public boolean firstConfirmed;

    @ColumnInfo(name = "last_confirmed")
    public long lastConfirmed;
}
