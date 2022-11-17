package com.globalgang.spur.eventdb;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EventDao {
    @Query("SELECT * FROM event")
    List<Event> getAll();

    @Query("SELECT * FROM event WHERE id LIKE :id LIMIT 1")
    Event getById(int id);

    @Query("SELECT * FROM event WHERE " +
            "title LIKE :name AND " +
            "loc_lat LIKE :latitude AND " +
            "loc_long LIKE :longitude " +
            "LIMIT 1")

    Event getByNameLocation(String name, double latitude, double longitude);

    @Insert
    void insertAll(Event... events);

    @Delete
    void delete(Event user);

    @Query("UPDATE event SET num_likes = num_likes+:increment WHERE id =:eventId")
    void updateLikes(Integer eventId, int increment);

    @Query("UPDATE event SET num_dislikes = num_dislikes+:increment WHERE id =:eventId")
    void updateDislikes(Integer eventId, int increment);

    @Query("UPDATE event SET is_confirmed = :is_confirmed WHERE id =:eventId")
    void setIsConfirmed(Integer eventId, boolean is_confirmed);

    @Query("UPDATE event SET last_confirmed = :timestamp WHERE id LIKE :id")
    void updateLastConfirmed(int id, long timestamp);

    @Query("UPDATE event SET is_refuted = :is_refuted WHERE id =:eventId")
    void setIsRefuted(Integer eventId, boolean is_refuted);
}