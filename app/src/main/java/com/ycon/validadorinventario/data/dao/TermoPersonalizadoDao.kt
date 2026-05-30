package com.ycon.validadorinventario.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ycon.validadorinventario.data.entity.TermoPersonalizadoEntity

@Dao
interface TermoPersonalizadoDao {

    /** Se (categoria + valor) já existir, a operação é ignorada silenciosamente. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(termo: TermoPersonalizadoEntity)

    @Query("SELECT valor FROM termos_personalizados WHERE categoria = :categoria ORDER BY valor ASC")
    fun observar(categoria: String): LiveData<List<String>>
}
