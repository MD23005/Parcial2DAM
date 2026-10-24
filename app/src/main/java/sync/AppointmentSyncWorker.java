
package dam.project.parcial2dam.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dam.project.parcial2dam.db.AppDatabase;
import dam.project.parcial2dam.db.Appointment;

public class AppointmentSyncWorker extends Worker {

    public AppointmentSyncWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        AppDatabase db =
                AppDatabase.getInstance(getApplicationContext());

        FirebaseFirestore firestore =
                FirebaseFirestore.getInstance();

        List<Appointment> pendientes =
                db.appointmentDAO().obtenerPendientes();

        for (Appointment cita : pendientes) {

            if (isStopped()) {
                return Result.retry();
            }

            // Conserva la versión que se intenta enviar.
            long timestampEnviado = cita.updatedAt;

            try {
                Map<String, Object> datos = new HashMap<>();

                datos.put("id", cita.id);
                datos.put("userId", cita.userId);
                datos.put("nombreCliente", cita.nombreCliente);
                datos.put("telefono", cita.telefono);
                datos.put("servicio", cita.servicio);
                datos.put("fecha", cita.fecha);
                datos.put("hora", cita.hora);
                datos.put("estado", cita.estado);
                datos.put("updatedAt", timestampEnviado);
                datos.put("deleted", cita.deleted);

                Tasks.await(
                        firestore.collection("citas")
                                .document(cita.id)
                                .set(datos),
                        30,
                        TimeUnit.SECONDS
                );

                // Solo marca como sincronizada la versión enviada.
                db.appointmentDAO().marcarSincronizadaSiNoCambio(
                        cita.id,
                        timestampEnviado
                );

            } catch (Exception e) {
                // WorkManager volverá a intentarlo.
                return Result.retry();
            }
        }

        return Result.success();
    }
}
