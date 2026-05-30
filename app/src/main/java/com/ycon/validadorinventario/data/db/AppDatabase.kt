package com.ycon.validadorinventario.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ycon.validadorinventario.data.dao.ProdutoDao
import com.ycon.validadorinventario.data.dao.TermoPersonalizadoDao
import com.ycon.validadorinventario.data.entity.ProdutoEntity
import com.ycon.validadorinventario.data.entity.TermoPersonalizadoEntity

/** fallbackToDestructiveMigration() descarta todos os dados ao detectar versão sem migração explícita.
 *  Em produção, substituir por addMigrations() para não perder dados dos usuários. */
@Database(
    entities = [ProdutoEntity::class, TermoPersonalizadoEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun produtoDao(): ProdutoDao
    abstract fun termoPersonalizadoDao(): TermoPersonalizadoDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "inventario_ycon.db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
