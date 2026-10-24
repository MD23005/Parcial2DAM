package dam.project.parcial2dam.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.mindrot.jbcrypt.BCrypt;

import dam.project.parcial2dam.R;
import dam.project.parcial2dam.db.AppDatabase;
import dam.project.parcial2dam.db.LocalUser;
import dam.project.parcial2dam.db.UserDAO;

public class LoginActivity extends AppCompatActivity {

    private final static String PREF = "session_prefs";
    private EditText edtEmail, edtPass;
    private Button btnLogin;
    private SwitchMaterial swLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        edtEmail = findViewById(R.id.edtEmail);
        edtPass = findViewById(R.id.edtPass);
        btnLogin = findViewById(R.id.btnLogin);
        swLogin = findViewById(R.id.swLogin);

        TextView tvRegister = findViewById(R.id.tvRegister);

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );

        if(!swLogin.isChecked()){
            getSharedPreferences(PREF, MODE_PRIVATE).edit().clear().apply();
        }

        swLogin.setOnCheckedChangeListener((btn, isChecked) -> {
            if(!isChecked){
                getSharedPreferences(PREF, MODE_PRIVATE).edit().clear().apply();
            }
        });

        btnLogin.setOnClickListener(v -> login());
    }

    private void login(){
        String email = edtEmail.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();

        if(email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            edtEmail.setError("Correo inválido");
            return;
        }

        if(pass.length() < 8 || !pass.matches("^(?=.*[a-zA-Z])(?=.*[0-9]).+$")){
            edtPass.setError("Contraseña inválida, se requieren al menos 8 caracteres y debe  números");
            return;
        }

        LocalUser userLocal = AppDatabase.getInstance(this).userDAO().buscarUsuarioPorMail(email);

        if(userLocal != null){
            if(BCrypt.checkpw(pass, userLocal.password)){
                guardarSesion(userLocal.uid, userLocal.email, userLocal.nombre);
                goToIndex(email);
            }else{
                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(this, "Buscando en la nube...", Toast.LENGTH_SHORT).show();

            FirebaseFirestore.getInstance().collection("usuarios")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {

                            com.google.firebase.firestore.DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                            String hashNube = doc.getString("password");
                            String nombreNube = doc.getString("nombre");
                            String uidNube = doc.getId();

                            if (hashNube != null && BCrypt.checkpw(pass, hashNube)) {

                                LocalUser nuevoLocal = new LocalUser(uidNube, nombreNube, email, hashNube, true);
                                AppDatabase.getInstance(this).userDAO().insertarUsuario(nuevoLocal);

                                // Iniciar sesión
                                guardarSesion(uidNube, email, nombreNube);
                                goToIndex(email);
                            } else {
                                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "El correo no está registrado", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Sin conexión a internet y usuario no guardado localmente", Toast.LENGTH_LONG).show();
                    });
        }
    }


    private void guardarSesion(String uid, String email, String nombre) {
        SharedPreferences pref = getSharedPreferences(
                "session_prefs",
                MODE_PRIVATE
        );

        pref.edit()
                .putString("uid", uid)
                .putString("email", email)
                .putString("nombre", nombre)
                .apply();
    }


    private void goToIndex(String email){
        Intent intent = new Intent(this, IndexActivity.class);
        intent.putExtra("email", email);
        startActivity(intent);
        finish();
    }
}
