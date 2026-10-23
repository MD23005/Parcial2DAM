package dam.project.parcial2dam.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarUsuario(LocalUser user);

    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    LocalUser buscarUsuarioPorMail(String email);

    @Query("SELECT * FROM usuarios WHERE sincronizado = 0")
    List<LocalUser> obtenerPendientes();

    @Query("SELECT * FROM usuarios WHERE uid = :uid LIMIT 1")
    LocalUser buscarPorUid(String uid);
}
