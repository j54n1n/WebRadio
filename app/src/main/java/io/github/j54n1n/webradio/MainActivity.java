package io.github.j54n1n.webradio;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.button);
    }

    public void buttonClick(View view) {
        /*
        final Intent intent = new Intent(this, PlaybackService.class);
        final boolean isRunning = stopService(intent); // Terrible hack xD
        Toast.makeText(this, "PlaybackService.isRunning=" + isRunning, Toast.LENGTH_SHORT).show();
        if(!isRunning) {
            startService(intent);
        }
        */
    }
}
