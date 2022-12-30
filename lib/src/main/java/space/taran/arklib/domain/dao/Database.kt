package space.taran.arklib.domain.dao

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import space.taran.arklib.Converters
import space.taran.arklib.app

@androidx.room.Database(
    entities = [
        Resource::class,
        ResourceExtra::class
    ],
    version = 18,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun resourceDao(): ResourceDao

    companion object {
        private const val DB_NAME = "ArkBrowser.db"

        fun build() =
            Room.databaseBuilder(app, Database::class.java, DB_NAME)
                .addMigrations(RoomMigration.MIGRATION_17_18)
                .fallbackToDestructiveMigration()
                .build()
    }
}

private object RoomMigration {
    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("DROP TABLE Root")
        }
    }
}
