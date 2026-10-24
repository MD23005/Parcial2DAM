package dam.project.parcial2dam.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import dam.project.parcial2dam.R;

public class IndexActivity extends AppCompatActivity {

    String emailUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.index_activity);

        emailUser = getIntent().getStringExtra("email");
        if(emailUser == null){
            SharedPreferences pref = getSharedPreferences("session_prefs", MODE_PRIVATE);
            emailUser = pref.getString("email", null);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.profile) {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("email", emailUser);
            startActivity(intent);
            return true;
        } else if (id == R.id.logout) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Cerrar Sesión")
                    .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                    .setPositiveButton("Sí, salir", (dialog, which) -> {


                        SharedPreferences prefs = getSharedPreferences("session_prefs", MODE_PRIVATE);
                        prefs.edit().clear().apply();


                        Intent intent = new Intent(IndexActivity.this, LoginActivity.class);


                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                        startActivity(intent);
                        finish();

                    })
                    .setNegativeButton("Cancelar", null)
                    .show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
