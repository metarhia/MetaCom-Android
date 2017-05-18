package com.metarhia.metacom;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.metarhia.metacom.Connection.ConnectionActivity;
import com.metarhia.metacom.Connection.ConnectionFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, new MainFragment())
                .commit();
        Intent intent = new Intent(this, ConnectionActivity.class);
        startActivity(intent);
    }
}
