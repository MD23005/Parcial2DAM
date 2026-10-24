package dam.project.parcial2dam.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dam.project.parcial2dam.R;
import dam.project.parcial2dam.db.AppDatabase;
import dam.project.parcial2dam.db.LocalUser;

public class ProfileActivity extends AppCompatActivity {
    private TextView tvEmail, tvName;
    private FloatingActionButton btnEditar;
    private ImageButton btnRegresar;
    private Button btnSincronizar;

    private String emailActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        btnEditar = findViewById(R.id.btnEditar);
        btnRegresar = findViewById(R.id.btnRegresar);
        btnSincronizar = findViewById(R.id.btnSincronizar);


        emailActual = getIntent().getStringExtra("email");
        if (emailActual == null) {
            SharedPreferences prefs = getSharedPreferences("session_prefs", MODE_PRIVATE);
            emailActual = prefs.getString("email", null);
        }

        cargarDatosPerfil();

        btnRegresar.setOnClickListener(v -> finish());
        btnEditar.setOnClickListener(v -> confirmDialog());
        btnSincronizar.setOnClickListener(v -> sincronizarPendientes());
    }

    private void cargarDatosPerfil() {
        if (emailActual == null) return;

        //  Buscamos al usuario en ROOM usando el email
        LocalUser localUser = AppDatabase.getInstance(this).userDAO().buscarUsuarioPorMail(emailActual);

        if (localUser != null) {
            // Mostramos los datos locales de inmediato
            tvName.setText(localUser.nombre);
            tvEmail.setText(localUser.email);

            //  Extraemos el UID de Room y lo usamos para buscar en Firestore
            String uidParaFirestore = localUser.uid;

            FirebaseFirestore.getInstance().collection("usuarios").document(uidParaFirestore)
                    .get(com.google.firebase.firestore.Source.SERVER) // Forzamos la red
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nombreNube = documentSnapshot.getString("nombre");
                            String emailNube = documentSnapshot.getString("email");
                            String passNube = documentSnapshot.getString("password");

                            // Actualizamos la UI con los datos frescos de la nube
                            tvName.setText(nombreNube);
                            tvEmail.setText(emailNube);
                            emailActual = emailNube; // Por si cambió el correo

                            // Actualizamos Room con los datos de la nube
                            localUser.nombre = nombreNube;
                            localUser.email = emailNube;
                            if (passNube != null) localUser.password = passNube;
                            AppDatabase.getInstance(this).userDAO().insertarUsuario(localUser);

                            // Actualizamos SharedPreferences
                            SharedPreferences prefs = getSharedPreferences("session_prefs", MODE_PRIVATE);
                            prefs.edit()
                                    .putString("nombre", nombreNube)
                                    .putString("email", emailNube)
                                    .apply();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Si no hay internet, nos quedamos con los datos que ya mostramos de Room
                        Toast.makeText(this, "Modo offline: Mostrando datos guardados localmente", Toast.LENGTH_SHORT).show();
                    });

        } else {
            // El usuario no está en Room (esto no debería pasar si inició sesión)
            Toast.makeText(this, "Error: Usuario no encontrado localmente", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarDatosPerfil();
    }

    private void confirmDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.confirm_dialog, null);
        TextInputEditText etPassword = dialogView.findViewById(R.id.et_password);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirmar identidad")
                .setMessage("Ingresa tu contraseña actual para editar tu perfil.")
                .setView(dialogView)
                .setPositiveButton("Verificar", null)
                .setNegativeButton("Cancelar", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String passIngresada = etPassword.getText().toString().trim();

                if (passIngresada.isEmpty()) {
                    etPassword.setError("La contraseña es obligatoria");
                    return;
                }

                // Verificamos en Room usando el email
                LocalUser usuarioLocal = AppDatabase.getInstance(this).userDAO().buscarUsuarioPorMail(emailActual);

                if (usuarioLocal != null) {
                    if (BCrypt.checkpw(passIngresada, usuarioLocal.password)) {
                        dialog.dismiss();
                        Intent intent = new Intent(ProfileActivity.this, RegisterActivity.class);
                        intent.putExtra("modo", "editar");
                        intent.putExtra("uid", usuarioLocal.uid);
                        startActivity(intent);
                    } else {
                        etPassword.setError("Contraseña incorrecta");
                    }
                } else {
                    Toast.makeText(this, "Error: Usuario no encontrado localmente", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void sincronizarPendientes() {
        List<LocalUser> pendientes = AppDatabase.getInstance(this).userDAO().obtenerPendientes();

        if (pendientes.isEmpty()) {
            Toast.makeText(this, "Todo está sincronizado. No hay pendientes.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Sincronizando " + pendientes.size() + " registro(s)...", Toast.LENGTH_SHORT).show();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (LocalUser usuario : pendientes) {
            Map<String, Object> datos = new HashMap<>();
            datos.put("nombre", usuario.nombre);
            datos.put("email", usuario.email);
            datos.put("password", usuario.password);

            db.collection("usuarios").document(usuario.uid).set(datos)
                    .addOnSuccessListener(unused -> {
                        usuario.sincronizado = true;
                        AppDatabase.getInstance(this).userDAO().insertarUsuario(usuario);
                        Toast.makeText(this, "Sincronización exitosa.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al sincronizar. Revisa tu conexión.", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}