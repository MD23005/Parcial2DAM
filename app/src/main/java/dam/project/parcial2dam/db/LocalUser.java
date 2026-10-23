package dam.project.parcial2dam.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usuarios")
public class LocalUser {
    @PrimaryKey
    @NonNull
    public String uid;
    public String nombre;
    public String email;
    public String password;
    public boolean sincronizado;

    public LocalUser(@NonNull String uid, String nombre, String email, String password, boolean sincronizado){
        this.uid = uid;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.sincronizado = sincronizado;

    }
}
