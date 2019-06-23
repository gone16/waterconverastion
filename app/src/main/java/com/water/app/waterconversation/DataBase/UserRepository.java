package com.water.app.waterconversation.DataBase;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.water.app.waterconversation.Constants;

import java.lang.ref.WeakReference;
import java.util.List;

public class UserRepository {

    private final String TAG = getClass().getSimpleName();

    private final UserDao userDao;

    private UpdateAccumulationTextAfterDBOperation delegate;

    public UserRepository(Context context){
        AppDatabase db = AppDatabase.getDatabase(context);
        userDao = db.getUserDao();
    }

    public void insert (User user){
//        new InsertAsyncTask(userDao,delegate).execute(user);
    }

    public void getAccidentByState(int state){
        new getAccidentByStateAsyncTask(userDao,delegate).execute(state);
    }

    public void getPortentByState(int state){
        new getPortentByStateAsyncTask(userDao,delegate).execute(state);
    }

    public void setDelegate(UpdateAccumulationTextAfterDBOperation delegate){
        this.delegate = delegate;
    }

//    private static class InsertAsyncTask extends AsyncTask<User,Void, Integer>{
//        private UserDao asyncUserDao;
//        private WeakReference<AfterDBOperationListener> asyncDelegate;
//
//        InsertAsyncTask(UserDao userDao, AfterDBOperationListener afterDBOperationListener) {
//            asyncUserDao = userDao;
//            asyncDelegate = new WeakReference<>(afterDBOperationListener);
//        }
//
//        @Override
//        protected Integer doInBackground(User... users) {
//            asyncUserDao.addData(users[0]);
//            return 1;
//        }
//
//        @Override
//        protected void onPostExecute(Integer result) {
//            final AfterDBOperationListener delegate = asyncDelegate.get();
//            if (delegate != null)
//                delegate.afterDBOperation(result);
//        }
//    }

    private static class getAccidentByStateAsyncTask extends  AsyncTask<Integer,Void,int[]>{
        private UserDao asyncUserDao;
        private WeakReference<UpdateAccumulationTextAfterDBOperation> asyncDelegate;

        getAccidentByStateAsyncTask(UserDao userDao, UpdateAccumulationTextAfterDBOperation afterDBOperationListener) {
            asyncUserDao = userDao;
            asyncDelegate = new WeakReference<>(afterDBOperationListener);
        }


        @Override
        protected int[] doInBackground(Integer... state) {
            List<User> userList;
            userList = asyncUserDao.findAccidentByState(state[0]);
            int[] integers = new int[3];
            integers[0] = Constants.TYPE.ACCIDENT;
            integers[1] = state[0];
            integers[2] = userList.size();
            return integers;
        }

        @Override
        protected void onPostExecute(int[] result) {
            final UpdateAccumulationTextAfterDBOperation delegate = asyncDelegate.get();
            if (delegate != null)
                delegate.updateAccumulationTextAfterDBOperation(result);
        }
    }

    private static class getPortentByStateAsyncTask extends  AsyncTask<Integer,Void,int[]>{
        private UserDao asyncUserDao;
        private WeakReference<UpdateAccumulationTextAfterDBOperation> asyncDelegate;

        getPortentByStateAsyncTask(UserDao userDao, UpdateAccumulationTextAfterDBOperation afterDBOperationListener) {
            asyncUserDao = userDao;
            asyncDelegate = new WeakReference<>(afterDBOperationListener);
        }


        @Override
        protected int[] doInBackground(Integer... state) {
            List<User> userList;
            userList = asyncUserDao.findPortentByState(state[0]);
            Log.d("UserRepository", "doInBackground: "+userList.size());
            int[] integers = new int[3];
            integers[0] = Constants.TYPE.PORTENT;
            integers[1] = state[0];
            integers[2] = userList.size();
            return integers;
        }

        @Override
        protected void onPostExecute(int[] result) {
            final UpdateAccumulationTextAfterDBOperation delegate = asyncDelegate.get();
//            Log.d("UserRepository", "doInBackground: "+result.size());
            if (delegate != null)
                delegate.updateAccumulationTextAfterDBOperation(result);
        }
    }
}
