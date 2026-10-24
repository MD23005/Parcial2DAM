
package dam.project.parcial2dam.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import dam.project.parcial2dam.R;
import dam.project.parcial2dam.ui.AppointmentsActivity;

public class IndexActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private String emailUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.index_activity);

        // Recuperar el correo del usuario
        emailUser = getIntent().getStringExtra("email");

        if (emailUser == null || emailUser.isEmpty()) {
            SharedPreferences pref =
                    getSharedPreferences("session_prefs", MODE_PRIVATE);

            emailUser = pref.getString("email", null);
        }

        // Configurar la barra superior
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Abrir la gestión de citas
        Button btnGestionCitas = findViewById(R.id.btnGestionCitas);

        btnGestionCitas.setOnClickListener(v -> {
            Intent intent = new Intent(
                    IndexActivity.this,
                    AppointmentsActivity.class
            );
            intent.putExtra("email", emailUser);
            startActivity(intent);
        });

        // Inicializar el fragmento de Google Maps
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.mapFragment);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        // Ubicación aproximada para la demostración del parcial
        LatLng ubicacionBarberia = new LatLng(13.6795, -89.2528);

        // Marcador de la barbería ficticia
        googleMap.addMarker(
                new MarkerOptions()
                        .position(ubicacionBarberia)
                        .title("Barbería El niño")
                        .snippet("Ubicación de demostración")
        );

        // Centrar el mapa en el marcador
        googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                        ubicacionBarberia,
                        16f
                )
        );

        // Mostrar controles de zoom
        googleMap.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // Abrir el perfil
        if (id == R.id.profile) {
            Intent intent = new Intent(
                    this,
                    ProfileActivity.class
            );
            intent.putExtra("email", emailUser);
            startActivity(intent);
            return true;
        }

        // Cerrar sesión
        if (id == R.id.logout) {
            new AlertDialog.Builder(this)
                    .setTitle("Cerrar sesión")
                    .setMessage(
                            "¿Estás seguro de que quieres cerrar sesión?"
                    )
                    .setPositiveButton("Sí, salir", (dialog, which) -> {
                        SharedPreferences prefs =
                                getSharedPreferences(
                                        "session_prefs",
                                        MODE_PRIVATE
                                );

                        prefs.edit().clear().apply();

                        Intent intent = new Intent(
                                IndexActivity.this,
                                LoginActivity.class
                        );

                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

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
