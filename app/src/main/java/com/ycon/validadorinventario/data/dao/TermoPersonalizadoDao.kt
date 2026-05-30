package com.ycon.validadorinventario.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ycon.validadorinventario.data.entity.TermoPersonalizadoEntity

/**
 * TermoPersonalizadoDao — acesso aos termos digitados pelo usuário
 * ao escolher a opção "Outro" no campo de setor.
 *
 * O retorno como LiveData garante que o campo de autocomplete
 * receba novas sugestões automaticamente após cada inserção,
 * sem necessidade de recarregar a tela.
 */
@Dao
interface TermoPersonalizadoDao {

    /**
     * Salva um novo termo personalizado.
     * Se a combinação (categoria + valor) já existir, a operação é ignorada silenciosamente.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(termo: TermoPersonalizadoEntity)

    /**
     * Retorna todos os valores de uma categoria em ordem alfabética.
     * O Room emite uma nova lista automaticamente a cada inserção.
     */
    @Query("SELECT valor FROM termos_personalizados WHERE categoria = :categoria ORDER BY valor ASC")
    fun observar(categoria: String): LiveData<List<String>>
}
