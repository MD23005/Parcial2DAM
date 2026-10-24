
package dam.project.parcial2dam.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "citas")
public class Appointment {

    @PrimaryKey
    @NonNull
    public String id = "";

    @NonNull
    public String userId = "";

    public String nombreCliente = "";
    public String telefono = "";
    public String servicio = "";
    public String fecha = "";
    public String hora = "";
    public String estado = "pendiente";

    public long updatedAt = 0L;

    // Indica si la cita necesita sincronizarse con Firestore
    public boolean syncPending = true;

    // Permite registrar eliminaciones pendientes de sincronizar
    public boolean deleted = false;

    public Appointment() {
    }
}
