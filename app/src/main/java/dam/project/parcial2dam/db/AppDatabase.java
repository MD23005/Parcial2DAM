package dam.project.parcial2dam.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {LocalUser.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCIA;

    public abstract UserDAO userDAO();

    public static AppDatabase getInstance(Context context){
        if (INSTANCIA == null){
            synchronized (AppDatabase.class){
                if (INSTANCIA ==null){
                    INSTANCIA = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "barberia_db")
                    .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCIA;
    }
}
