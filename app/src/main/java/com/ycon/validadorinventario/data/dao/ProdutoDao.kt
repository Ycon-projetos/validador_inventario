package com.ycon.validadorinventario.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ycon.validadorinventario.data.entity.ProdutoEntity

/**
 * ProdutoDao — interface de acesso à tabela de movimentações.
 *
 * O Room valida cada consulta (@Query) em tempo de compilação — erros de SQL
 * aparecem como erros de build, nunca como falhas em produção.
 *
 * As funções marcadas como `suspend` executam em segundo plano (coroutines),
 * garantindo que nenhuma operação de banco bloqueie a tela do usuário.
 */
@Dao
interface ProdutoDao {

    /**
     * Insere um novo registro de movimentação.
     * Em caso de conflito de chave primária, a operação é cancelada com erro (ABORT).
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirProduto(produto: ProdutoEntity): Long

    /**
     * Retorna todos os registros em ordem decrescente de data/hora.
     * LiveData — o Room observa a tabela e emite uma nova lista automaticamente
     * sempre que houver inserção, atualização ou exclusão.
     */
    @Query("SELECT * FROM produtos ORDER BY ts DESC")
    fun obterTodosOsProdutos(): LiveData<List<ProdutoEntity>>

    /**
     * Versão suspensa de obterTodosOsProdutos — usada nos testes instrumentados
     * e em operações que não dependem do ciclo de vida do LiveData.
     */
    @Query("SELECT * FROM produtos ORDER BY ts DESC")
    suspend fun obterTodosOsProdutosSuspend(): List<ProdutoEntity>

    /**
     * Saldo líquido total do estoque: soma as entradas e subtrai as saídas.
     * COALESCE garante que o resultado seja 0 quando não há registros.
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN tipo = 'ENTRADA' THEN qty ELSE -qty END), 0) FROM produtos")
    suspend fun obterTotalItensEstoque(): Int

    /** Contagem total de registros — exibida no card "Movimentos". */
    @Query("SELECT COUNT(*) FROM produtos")
    suspend fun obterTotalLotes(): Int

    /** Contagem de setores distintos que possuem ao menos um registro. */
    @Query("SELECT COUNT(DISTINCT setor) FROM produtos")
    suspend fun obterSetoresAtivos(): Int

    /** Retorna o registro mais recente — exibido no card "Último Mov." */
    @Query("SELECT * FROM produtos ORDER BY ts DESC LIMIT 1")
    suspend fun obterUltimoLote(): ProdutoEntity?

    /**
     * Saldo líquido de um SKU específico.
     * Usado para bloquear uma SAÍDA que geraria estoque negativo.
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN tipo = 'ENTRADA' THEN qty ELSE -qty END), 0) FROM produtos WHERE sku = :sku")
    suspend fun obterSaldoPorSku(sku: String): Int

    /** Lista de SKUs distintos em ordem alfabética. */
    @Query("SELECT DISTINCT sku FROM produtos ORDER BY sku ASC")
    fun obterSkusDistintos(): LiveData<List<String>>

    /** Remove todos os registros — usado nos testes para garantir isolamento entre casos. */
    @Query("DELETE FROM produtos")
    suspend fun limparTabela()
}
