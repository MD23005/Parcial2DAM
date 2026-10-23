package dam.project.parcial2dam.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.mindrot.jbcrypt.BCrypt;

import dam.project.parcial2dam.R;
import dam.project.parcial2dam.db.AppDatabase;
import dam.project.parcial2dam.db.LocalUser;

public class RegisterActivity extends AppCompatActivity {
    private EditText edtName, edtEmail, edtPass, edtConfirmPass;
    private Button btnRegister;
    private TextView tvTitle;
    private ImageButton btnRegresar;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private boolean editing = false;
    private String uid = null;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.register_activity);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPass = findViewById(R.id.edtPass);
        edtConfirmPass = findViewById(R.id.edtConfirmPass);

        tvTitle = findViewById(R.id.tvTitle);

        btnRegister = findViewById(R.id.btnRegister);
        btnRegresar = findViewById(R.id.btnRegresar);

        //Verificar si es edición
        String modo = getIntent().getStringExtra("modo");
        uid = getIntent().getStringExtra("uid");
        editing = "editar".equals(modo) && uid != null;

        if(editing){
            btnRegister.setText("Actualizar información");
            tvTitle.setText("Editar información");
            cargarDatos();
        }else{
            btnRegister.setText("Registrar cuenta");
            tvTitle.setText("Registrar cuenta");
        }

        btnRegresar.setOnClickListener(v -> {
            finish();
        });

        btnRegister.setOnClickListener(v -> {
            if(editing) actualizar();
            else registrar();
        });
    }

    private void cargarDatos(){
        db.collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if(!doc.exists()){
                        Toast.makeText(this, "Su usuario no ha sido encontrado", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    edtName.setText(doc.getString("nombre"));
                    edtEmail.setText(doc.getString("email"));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar los datos..."+ e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void registrar(){
        String nombre = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();
        String confirmPass = edtConfirmPass.getText().toString().trim();

        if(nombre.length() < 7) {
            edtName.setError("El nombre debe tener al menos 7 caracteres");
            return;
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Correo inválido");
            return;
        }
        if(pass.length() < 8 || !pass.matches("^(?=.*[a-zA-Z])(?=.*[0-9]).+$")) {
            edtPass.setError("Mínimo 8 caracteres, requiere números");
            return;
        }
        if(!pass.equals(confirmPass)) {
            edtConfirmPass.setError("Las contraseñas no coinciden");
            return;
        }


        String hashPassword = BCrypt.hashpw(pass, BCrypt.gensalt());
        String nuevoUid = java.util.UUID.randomUUID().toString();


        LocalUser nuevoUsuario = new LocalUser(nuevoUid, nombre, email, hashPassword, false);


        AppDatabase.getInstance(this).userDAO().insertarUsuario(nuevoUsuario);


        java.util.Map<String, Object> datos = new java.util.HashMap<>();
        datos.put("nombre", nombre);
        datos.put("email", email);
        datos.put("password", hashPassword);

        db.collection("usuarios").document(nuevoUid).set(datos)
                .addOnSuccessListener(unused -> {
                    // Actualizar estado a sincronizado en Room
                    nuevoUsuario.sincronizado = true;
                    AppDatabase.getInstance(this).userDAO().insertarUsuario(nuevoUsuario);

                    Toast.makeText(this, "Usuario registrado con éxito", Toast.LENGTH_SHORT).show();
                    finish(); // Volver al login
                })
                .addOnFailureListener(e -> {
                    // Si falla la nube, se queda guardado en Room (pendiente de sync)
                    Toast.makeText(this, "Guardado localmente. Se sincronizará luego.", Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void actualizar(){
        String nombre = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();
        String confirmPass = edtConfirmPass.getText().toString().trim();


        if (nombre.length() < 7) {
            edtName.setError("El nombre debe tener al menos 7 caracteres");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Correo inválido");
            return;
        }

        boolean cambiarPassword = !pass.isEmpty();
        String nuevoHash = null;

        if (cambiarPassword) {
            if (pass.length() < 8 || !pass.matches("^(?=.*[a-zA-Z])(?=.*[0-9]).+$")) {
                edtPass.setError("Mínimo 8 caracteres, alfanumérica");
                return;
            }
            if (!pass.equals(confirmPass)) {
                edtConfirmPass.setError("Las contraseñas no coinciden");
                return;
            }
            nuevoHash = BCrypt.hashpw(pass, BCrypt.gensalt());
        }

        String hashFinal = cambiarPassword ? nuevoHash : AppDatabase.getInstance(this).userDAO().buscarPorUid(uid).password;

        LocalUser usuarioActualizado = new LocalUser(uid, nombre, email, hashFinal, false);


        AppDatabase.getInstance(this).userDAO().insertarUsuario(usuarioActualizado);


        java.util.Map<String, Object> datosActualizados = new java.util.HashMap<>();
        datosActualizados.put("nombre", nombre);
        datosActualizados.put("email", email);

        if (cambiarPassword) {
            datosActualizados.put("password", nuevoHash);
        }

        db.collection("usuarios").document(uid)
                .update(datosActualizados) // Usamos update para no borrar otros campos que existan
                .addOnSuccessListener(unused -> {
                    // Éxito en la nube: actualizamos el estado en Room a sincronizado
                    usuarioActualizado.sincronizado = true;
                    AppDatabase.getInstance(this).userDAO().insertarUsuario(usuarioActualizado);

                    // Actualizamos SharedPreferences por si cambió el nombre o el correo
                    SharedPreferences prefs = getSharedPreferences("session_prefs", MODE_PRIVATE);
                    prefs.edit()
                            .putString("nombre", nombre)
                            .putString("email", email)
                            .apply();

                    Toast.makeText(this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show();
                    finish(); // Cierra la pantalla de edición y vuelve al Perfil/Home
                })
                .addOnFailureListener(e -> {
                    // Si falla la nube (ej. sin internet), el cambio se quedó en Room (pendiente de sync)
                    Toast.makeText(this, "Guardado localmente. Se sincronizará cuando haya conexión.", Toast.LENGTH_LONG).show();

                    // Actualizamos SharedPreferences de todos modos
                    SharedPreferences prefs = getSharedPreferences("session_prefs", MODE_PRIVATE);
                    prefs.edit()
                            .putString("nombre", nombre)
                            .putString("email", email)
                            .apply();

                    finish();
                });
    }
}
