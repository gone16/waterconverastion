package com.water.app.waterconversation.Activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.water.app.waterconversation.GlobalVariable;
import com.water.app.waterconversation.R;

public class LoginActivity extends AppCompatActivity {

    private String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button buttonLogin = findViewById(R.id.button_login);
        Button buttonRegister = findViewById(R.id.button_register);
        EditText editTextUserId = findViewById(R.id.edittext_login_userID);
        EditText editTextPassword = findViewById(R.id.edittext_login_password);

        GlobalVariable globalVariable = (GlobalVariable) getApplicationContext();

        if(globalVariable!=null) {
            if( globalVariable.getDetecting()) {
                Intent intentLogin = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intentLogin);
            }
        }

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean isUserIdFilled,isPasswordFilled = false;

                Intent intentLogin = new Intent(LoginActivity.this,MainActivity.class);
                startActivity(intentLogin);

//                if(editTextUserId.getText().length()==0){
//                    setAlertDialog(getString(R.string.alert_login_insert_userID));
//                } else if(editTextUserId.getText().length()<8){
//                    setAlertDialog(getString(R.string.alert_login_insert_userID_range));
//                } else if(editTextPassword.getText().length()==0){
//                    setAlertDialog(getString(R.string.alert_login_insert_password));
//                }  else if(editTextPassword.getText().length()<6){
//                    setAlertDialog(getString(R.string.alert_login_insert_password_range));
//                } else{
////                    Intent intentLogin = new Intent(LoginActivity.this,MainActivity.class);
//                    startActivity(intentLogin);
//                }
            }
        });

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentRegister = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intentRegister);
            }
        });
    }

    private void setAlertDialog(String title){
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setPositiveButton(R.string.alert_reset_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                Log.d(TAG, "click ok");
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setTitle(title);
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        return;
    }
}
