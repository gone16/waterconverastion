package com.water.app.waterconversation.DataBase;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.water.app.waterconversation.Constants;
import com.water.app.waterconversation.MyTime;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class UserRepository {

    private final String TAG = getClass().getSimpleName();

    private final UserDao userDao;

    private UpdateAccumulationTextAfterDBOperation delegate;
    private UpdateAccumulationBarChartAfterDBOperation delegateBar;

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

    public void getAccidentByStateAndDate(int state, String date){
        Bundle bundle = new Bundle();
        bundle.putInt("state",state);
        bundle.putString("date",date);
        new getAccidentByStateAndDateAsyncTask(userDao,delegateBar).execute(bundle);
    }

    public void setDelegate(UpdateAccumulationTextAfterDBOperation delegate){
        this.delegate = delegate;
    }

    public void setDelegateBar(UpdateAccumulationBarChartAfterDBOperation delegateBar){
        this.delegateBar = delegateBar;
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

    private static class getAccidentByStateAndDateAsyncTask extends AsyncTask<Bundle,Void,float[]>{
        private UserDao asyncUserDao;
        private WeakReference<UpdateAccumulationBarChartAfterDBOperation> asyncDelegate;

        getAccidentByStateAndDateAsyncTask(UserDao userDao, UpdateAccumulationBarChartAfterDBOperation afterDBOperationListener) {
            asyncUserDao = userDao;
            asyncDelegate = new WeakReference<>(afterDBOperationListener);
        }


        @Override
        protected float[] doInBackground(Bundle... bundles) {
            int state = bundles[0].getInt("state");
            String date = bundles[0].getString("date");


            MyTime myTime = new MyTime();
            String time = myTime.getCurrentTime();

            List<User> userList;
            userList = asyncUserDao.getAll();
            int dataSize = userList.size();

            float[] DropArray = new float[12];
            float[] FallArray = new float[12];
            float[] ComaArray = new float[12];
            float[] PortentsArray = new float[12];


            for (int i =0; i< dataSize; i++){
                switch (userList.get(i).getAccident()){
                    case Constants.ACCIDENTS.NORMAL:
                        if(userList.get(i).getPortent() !=0){
                            PortentsArray = getBarArray(i,PortentsArray,userList);
                        }
                        break;
                    case Constants.ACCIDENTS.DROP:
                        DropArray =getBarArray(i,DropArray,userList);
                        break;
                    case Constants.ACCIDENTS.FALL:
                        FallArray =getBarArray(i,FallArray,userList);
                        break;
                    case Constants.ACCIDENTS.COMA:
                        ComaArray =getBarArray(i,ComaArray,userList);
                        break;
                }
            }

            float[] BarArray = new float[48];

            System.arraycopy(PortentsArray, 0, BarArray, 0, 12);
            System.arraycopy(DropArray, 0, BarArray, 12, 12);
            System.arraycopy(FallArray, 0, BarArray, 24, 12);
            System.arraycopy(ComaArray, 0, BarArray, 36, 12);


            Calendar mCal = Calendar.getInstance();
            SimpleDateFormat dfTime = new SimpleDateFormat("kk:mm:ss");
            SimpleDateFormat dfDate = new SimpleDateFormat("MM-dd");
            mCal.add(Calendar.HOUR, -12);

//            Log.d("UserRepository", "doInBackground: "+userList.size());
//            String[] strings  = new String[3];
//            strings[0] = Integer.toString(state);
//            strings[1] =date;
//            strings[2] = dfTime.format(mCal.getTime());
            return BarArray;
        }

        @Override
        protected void onPostExecute(float[] result) {
            final UpdateAccumulationBarChartAfterDBOperation delegate = asyncDelegate.get();
//            Log.d("UserRepository", "doInBackground: "+result.size());
            if (delegate != null)
                delegate.updateAccumulationBarChart(result);
        }

        private float[] getBarArray(int count, float[] barArray, List<User> userList){
            MyTime myTime = new MyTime();
            long nowTime = myTime.getLongCurrentTime();
            long dbTime = myTime.getLongTime(userList.get(count).getDate(),userList.get(count).getTime());
            Log.e("第"+count,dbTime+"");
            //相減算區間
            long tmp = nowTime - dbTime;
            //計算時間 秒數,要放入的Array,頻率(半小時)
            //換算成毫秒來進行時間加減
            long Hour = 60 * 60 * 1000;

            if (0 <= tmp && tmp < Hour) {
                barArray[11]++;
            }  else if (Hour <= tmp && tmp < Hour * 2) {
                barArray[10]++;
            } else if (Hour * 2 <= tmp && tmp < Hour * 3) {
                barArray[9]++;
            } else if (Hour * 3 <= tmp && tmp < Hour * 4) {
                barArray[8]++;
            } else if (Hour * 4  <= tmp && tmp < Hour * 5) {
                barArray[7]++;
            } else if (Hour * 5 <= tmp && tmp < Hour * 6) {
                barArray[6]++;
            } else if (Hour * 6 <= tmp && tmp < Hour * 7) {
                barArray[5]++;
            } else if (Hour * 7 <= tmp && tmp < Hour * 8) {
                barArray[4]++;
            } else if (Hour *8 <= tmp && tmp < Hour * 9) {
                barArray[3]++;
            } else if (Hour * 9 <= tmp && tmp < Hour * 10) {
                barArray[2]++;
            } else if (Hour * 10 <= tmp && tmp < Hour * 11) {
                barArray[1]++;
            } else if (Hour * 11 <= tmp && tmp < Hour * 12) {
                barArray[0]++;
            }
            return barArray;
        }



    }
}
