package com.globalgang.spur.eventdb;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.ArrayList;

@Dao
public interface UserDao {

    @Query("SELECT * FROM user WHERE userId LIKE :userId LIMIT 1")
    User getUserById(String userId);

    @Query("UPDATE user SET points = :points WHERE userId =:userId")
    void updatePoints(String userId, int points);

    @Query("SELECT EXISTS(SELECT * FROM user where userId LIKE :userId)")
    Boolean isUserExists(String userId);

    @Insert
    void insertUser(User user);

}