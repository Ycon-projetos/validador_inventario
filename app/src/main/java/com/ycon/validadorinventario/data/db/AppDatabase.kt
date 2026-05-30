package com.ycon.validadorinventario.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ycon.validadorinventario.data.dao.ProdutoDao
import com.ycon.validadorinventario.data.dao.TermoPersonalizadoDao
import com.ycon.validadorinventario.data.entity.ProdutoEntity
import com.ycon.validadorinventario.data.entity.TermoPersonalizadoEntity

/**
 * AppDatabase — banco de dados local do aplicativo (versão 2).
 *
 * Mudanças em relação à versão 1:
 *   - ProdutoEntity: adicionado o campo `tipo` (ENTRADA ou SAIDA)
 *   - Nova tabela `termos_personalizados` para sugestões do campo "Outro"
 *
 * O método fallbackToDestructiveMigration() recria o banco automaticamente
 * quando detecta uma versão diferente sem migração explícita definida.
 * Aceitável em desenvolvimento; em produção, substituir por addMigrations()
 * para não perder os dados dos usuários.
 *
 * Implementação Singleton com Double-Checked Locking para garantir que
 * apenas uma instância do banco exista durante toda a vida do aplicativo.
 */
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
