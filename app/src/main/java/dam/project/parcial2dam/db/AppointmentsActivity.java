
package dam.project.parcial2dam.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import dam.project.parcial2dam.R;
import dam.project.parcial2dam.db.AppDatabase;
import dam.project.parcial2dam.db.Appointment;
import dam.project.parcial2dam.sync.AppointmentSyncWorker;

public class AppointmentsActivity extends AppCompatActivity {

    private EditText edtCliente;
    private EditText edtTelefono;
    private EditText edtServicio;
    private EditText edtFecha;
    private EditText edtHora;

    private LinearLayout layoutCitas;
    private Button btnGuardarCita;
    private Button btnCancelarEdicion;

    private AppDatabase db;
    private String userId;
    private String citaEditandoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments);

        db = AppDatabase.getInstance(this);

        userId = getSharedPreferences(
                "session_prefs", MODE_PRIVATE
        ).getString("uid", null);

        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "No se encontró la sesión. Inicia sesión nuevamente.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        edtCliente = findViewById(R.id.edtCliente);
        edtTelefono = findViewById(R.id.edtTelefono);
        edtServicio = findViewById(R.id.edtServicio);
        edtFecha = findViewById(R.id.edtFecha);
        edtHora = findViewById(R.id.edtHora);

        layoutCitas = findViewById(R.id.layoutCitas);
        btnGuardarCita = findViewById(R.id.btnGuardarCita);
        btnCancelarEdicion =
                findViewById(R.id.btnCancelarEdicion);

        edtFecha.setFocusable(false);
        edtFecha.setClickable(true);
        edtFecha.setOnClickListener(v -> mostrarSelectorFecha());

        edtHora.setFocusable(false);
        edtHora.setClickable(true);
        edtHora.setOnClickListener(v -> mostrarSelectorHora());

        btnGuardarCita.setOnClickListener(v -> guardarCita());
        btnCancelarEdicion.setOnClickListener(
                v -> limpiarFormulario()
        );

        btnCancelarEdicion.setVisibility(View.GONE);

        mostrarCitas();
        programarSincronizacion();
    }

    private void mostrarSelectorFecha() {
        Calendar calendario = Calendar.getInstance();

        new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    String fecha = String.format(
                            Locale.US,
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            day
                    );

                    edtFecha.setText(fecha);
                },
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void mostrarSelectorHora() {
        Calendar calendario = Calendar.getInstance();

        new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    String hora = String.format(
                            Locale.US,
                            "%02d:%02d",
                            hour,
                            minute
                    );

                    edtHora.setText(hora);
                },
                calendario.get(Calendar.HOUR_OF_DAY),
                calendario.get(Calendar.MINUTE),
                true
        ).show();
    }

    private void guardarCita() {
        String cliente =
                edtCliente.getText().toString().trim();

        String telefono =
                edtTelefono.getText().toString().trim();

        String servicio =
                edtServicio.getText().toString().trim();

        String fecha =
                edtFecha.getText().toString().trim();

        String hora =
                edtHora.getText().toString().trim();

        if (cliente.isEmpty()
                || telefono.isEmpty()
                || servicio.isEmpty()
                || fecha.isEmpty()
                || hora.isEmpty()) {

            Toast.makeText(
                    this,
                    "Completa todos los campos.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        boolean esNueva = citaEditandoId == null;
        Appointment cita;

        if (esNueva) {
            cita = new Appointment();
            cita.id = UUID.randomUUID().toString();
            cita.userId = userId;
            cita.estado = "pendiente";
            cita.deleted = false;
        } else {
            cita = db.appointmentDAO()
                    .buscarPorId(citaEditandoId);

            if (cita == null
                    || !userId.equals(cita.userId)
                    || cita.deleted) {

                Toast.makeText(
                        this,
                        "No se encontró la cita.",
                        Toast.LENGTH_SHORT
                ).show();

                limpiarFormulario();
                mostrarCitas();
                return;
            }
        }

        cita.nombreCliente = cliente;
        cita.telefono = telefono;
        cita.servicio = servicio;
        cita.fecha = fecha;
        cita.hora = hora;
        cita.updatedAt = System.currentTimeMillis();
        cita.syncPending = true;
        cita.deleted = false;

        db.appointmentDAO().guardar(cita);

        Toast.makeText(
                this,
                esNueva
                        ? "Cita registrada."
                        : "Cita actualizada.",
                Toast.LENGTH_SHORT
        ).show();

        limpiarFormulario();
        mostrarCitas();
        programarSincronizacion();
    }

    private void mostrarCitas() {
        layoutCitas.removeAllViews();

        List<Appointment> citas =
                db.appointmentDAO().obtenerPorUsuario(userId);

        if (citas == null || citas.isEmpty()) {
            TextView mensaje = new TextView(this);
            mensaje.setText("No hay citas registradas.");
            mensaje.setTextSize(16);
            mensaje.setPadding(8, 16, 8, 16);

            layoutCitas.addView(mensaje);
            return;
        }

        for (Appointment cita : citas) {
            LinearLayout tarjeta = new LinearLayout(this);
            tarjeta.setOrientation(LinearLayout.VERTICAL);
            tarjeta.setPadding(20, 16, 20, 16);
            tarjeta.setBackgroundResource(
                    android.R.drawable.dialog_holo_light_frame
            );

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            params.setMargins(0, 0, 0, 20);
            tarjeta.setLayoutParams(params);

            TextView informacion = new TextView(this);

            informacion.setText(
                    "Cliente: " + seguro(cita.nombreCliente) + "\n"
                            + "Teléfono: " + seguro(cita.telefono) + "\n"
                            + "Servicio: " + seguro(cita.servicio) + "\n"
                            + "Fecha: " + seguro(cita.fecha) + "\n"
                            + "Hora: " + seguro(cita.hora) + "\n"
                            + "Estado: " + seguro(cita.estado)
            );

            informacion.setTextSize(16);
            informacion.setPadding(0, 0, 0, 12);
            tarjeta.addView(informacion);

            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(Gravity.END);

            Button editar = new Button(this);
            editar.setText("Editar");

            Button eliminar = new Button(this);
            eliminar.setText("Eliminar");

            fila.addView(editar);
            fila.addView(eliminar);
            tarjeta.addView(fila);
            layoutCitas.addView(tarjeta);

            editar.setOnClickListener(
                    v -> cargarCitaParaEditar(cita)
            );

            eliminar.setOnClickListener(
                    v -> eliminarCita(cita)
            );
        }
    }

    private String seguro(String valor) {
        return valor == null ? "" : valor;
    }

    private void cargarCitaParaEditar(Appointment cita) {
        if (!userId.equals(cita.userId) || cita.deleted) {
            return;
        }

        citaEditandoId = cita.id;

        edtCliente.setText(seguro(cita.nombreCliente));
        edtTelefono.setText(seguro(cita.telefono));
        edtServicio.setText(seguro(cita.servicio));
        edtFecha.setText(seguro(cita.fecha));
        edtHora.setText(seguro(cita.hora));

        btnGuardarCita.setText("Actualizar cita");
        btnCancelarEdicion.setVisibility(View.VISIBLE);
    }

    private void eliminarCita(Appointment cita) {
        if (!userId.equals(cita.userId)) {
            Toast.makeText(
                    this,
                    "No puedes eliminar esta cita.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        db.appointmentDAO().marcarEliminada(
                cita.id,
                System.currentTimeMillis()
        );

        if (cita.id.equals(citaEditandoId)) {
            limpiarFormulario();
        }

        mostrarCitas();
        programarSincronizacion();

        Toast.makeText(
                this,
                "Cita eliminada.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void limpiarFormulario() {
        edtCliente.setText("");
        edtTelefono.setText("");
        edtServicio.setText("");
        edtFecha.setText("");
        edtHora.setText("");

        citaEditandoId = null;

        btnGuardarCita.setText("Guardar cita");
        btnCancelarEdicion.setVisibility(View.GONE);
    }

    private void programarSincronizacion() {
        Constraints restricciones =
                new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

        OneTimeWorkRequest solicitud =
                new OneTimeWorkRequest.Builder(
                        AppointmentSyncWorker.class
                )
                        .setConstraints(restricciones)
                        .setBackoffCriteria(
                                BackoffPolicy.EXPONENTIAL,
                                30,
                                TimeUnit.SECONDS
                        )
                        .build();

        WorkManager.getInstance(getApplicationContext())
                .enqueueUniqueWork(
                        "sincronizar_citas",
                        ExistingWorkPolicy.APPEND_OR_REPLACE,
                        solicitud
                );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (db != null
                && layoutCitas != null
                && userId != null) {

            mostrarCitas();
            programarSincronizacion();
        }
    }
}
