package com.example.wlblu.chatchat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class StatusActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference changeStatusRef;

    private Toolbar mToolbar;
    private Button SaveChangesButton;
    private EditText StatusInput;

    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid();
        changeStatusRef = FirebaseDatabase.getInstance().getReference().child("Users").child(userId);

        mToolbar = (Toolbar)findViewById(R.id.status_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Change Status");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SaveChangesButton = (Button)findViewById(R.id.save_status_change_button);
        StatusInput = (EditText)findViewById(R.id.status_input);
        loadingBar = new ProgressDialog(this);

        String old_status = getIntent().getExtras().get("user_status").toString();
        StatusInput.setText(old_status);

        SaveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String new_status = StatusInput.getText().toString();

                ChangeProfileStatus(new_status);
            }
        });


    }

    private void ChangeProfileStatus(String new_status) {
        if(TextUtils.isEmpty(new_status)){
            Toast.makeText(StatusActivity.this,"Please Write Your Status.",Toast.LENGTH_LONG).show();
        }

        else{
            loadingBar.setTitle("Change Profile Status");
            loadingBar.setMessage("Please wait, while we are updating your profile status...");
            changeStatusRef.child("user_status").setValue(new_status).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        loadingBar.dismiss();
                        Intent settingIntent = new Intent(StatusActivity.this,SettingActivity.class);
                        startActivity(settingIntent);

                        Toast.makeText(StatusActivity.this, "Profile Status updated Successfully...",Toast.LENGTH_LONG).show();
                    }

                    else{
                        Toast.makeText(StatusActivity.this, "Error Occurred...",Toast.LENGTH_LONG).show();

                    }
                }
            });
        }
    }
}
