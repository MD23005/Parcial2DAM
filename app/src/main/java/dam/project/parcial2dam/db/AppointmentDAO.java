
package dam.project.parcial2dam.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AppointmentDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardar(Appointment cita);

    @Query("SELECT * FROM citas " +
            "WHERE userId = :userId AND deleted = 0 " +
            "ORDER BY fecha, hora")
    List<Appointment> obtenerPorUsuario(String userId);

    @Query("SELECT * FROM citas WHERE id = :id LIMIT 1")
    Appointment buscarPorId(String id);

    @Query("SELECT * FROM citas WHERE syncPending = 1")
    List<Appointment> obtenerPendientes();

    @Query("UPDATE citas SET deleted = 1, " +
            "syncPending = 1, updatedAt = :timestamp " +
            "WHERE id = :id")
    void marcarEliminada(String id, long timestamp);

    @Query("UPDATE citas SET syncPending = 0 " +
            "WHERE id = :id AND updatedAt = :timestamp " +
            "AND syncPending = 1")
    void marcarSincronizadaSiNoCambio(String id, long timestamp);

    @Query("UPDATE citas SET syncPending = 0 WHERE id = :id")
    void marcarSincronizada(String id);

    @Query("DELETE FROM citas WHERE id = :id")
    void eliminarDefinitivamente(String id);
}
