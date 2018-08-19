package com.joker.transformsample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.joker.main.HomeActivity;

public class MainActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    findViewById(R.id.jump).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        startActivity(new Intent(MainActivity.this, HomeActivity.class));
      }
    });
  }
}
