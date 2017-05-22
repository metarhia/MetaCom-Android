package com.metarhia.metacom.activities.connection;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.metarhia.metacom.activities.MainActivity;
import com.metarhia.metacom.R;
import com.metarhia.metacom.interfaces.ConnectionCallback;

public class ConnectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, new ConnectionFragment())
                .commit();
    }

}
