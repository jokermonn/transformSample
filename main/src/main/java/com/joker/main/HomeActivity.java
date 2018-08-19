package com.joker.main;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import com.joker.hunter.HunterRegistry;
import com.joker.service.MovieService;

public class HomeActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);

    ((TextView) findViewById(R.id.tv)).setText(HunterRegistry.get(MovieService.class).movieName());
  }
}
