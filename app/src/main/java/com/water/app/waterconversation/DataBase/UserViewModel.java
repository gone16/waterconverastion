package com.water.app.waterconversation.DataBase;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

public class UserViewModel extends AndroidViewModel {

    private String TAG = this.getClass().getSimpleName();
    private UserDao userDao;
    private AppDatabase appDatabase;

    public UserViewModel(@NonNull Application application) {
        super(application);

        appDatabase = AppDatabase.getDatabase(application);
        userDao = appDatabase.getUserDao();
    }

    public void insert(User user){
        new InsertAsyncTask(userDao).execute(user);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.i(TAG, "ViewModel Destroyed");
    }

    private class InsertAsyncTask extends AsyncTask<User,Void,Void>{

        UserDao mUserDao;

        public InsertAsyncTask(UserDao mUserDao){
            this.mUserDao = mUserDao;
        }

        @Override
        protected Void doInBackground(User... users) {
            mUserDao.addData(users[0]);
            Log.i(TAG, "doInBackground: add user"+users[0]);
            return null;
        }
    }
}
