package dam.project.parcial2dam;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.splashscreen.SplashScreen;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import dam.project.parcial2dam.ui.IndexActivity;
import dam.project.parcial2dam.ui.LoginActivity;

public class MainActivity extends AppCompatActivity {

    boolean isReady = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        splashScreen.setKeepOnScreenCondition(() -> !isReady);

        super.onCreate(savedInstanceState);

        SharedPreferences pref = getSharedPreferences("session_prefs", MODE_PRIVATE);
        String savedEmail = pref.getString("email", null);
        isReady = true;

        Intent intent = (savedEmail != null && !savedEmail.isEmpty()) ? new Intent(this, IndexActivity.class) : new Intent(this, LoginActivity.class);

        startActivity(intent);

        finish();


    }

}