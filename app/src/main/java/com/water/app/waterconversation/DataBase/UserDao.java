package com.water.app.waterconversation.DataBase;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM user")
    List<User> getAll();

    @Query("SELECT * FROM user WHERE userId IN (:userIds)")
    List<User> loadAllByIds(String[] userIds);

    @Query("SELECT * FROM user WHERE accident = :state")
    List<User> findAccidentByState(Integer state);

    @Query("SELECT * FROM user WHERE portent = :state")
    List<User> findPortentByState(Integer state);

    @Query("SELECT * FROM user WHERE accident =:state AND date =:date")
    List<User> findAccidentByStateAndDate(Integer state, String date);


    @Insert
//            (onConflict = OnConflictStrategy.REPLACE)
    void addData(User user);

    @Delete
    void delete(User user);

    @Query("DELETE FROM user")
     void deleteAll();
}
