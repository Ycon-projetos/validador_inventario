package com.ycon.validadorinventario.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ycon.validadorinventario.data.entity.ProdutoEntity
import com.ycon.validadorinventario.data.entity.SaldoSkuItem

/** O Room valida cada @Query em tempo de compilação — erros de SQL aparecem como erros de build. */
@Dao
interface ProdutoDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirProduto(produto: ProdutoEntity): Long

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

    @Query("SELECT COUNT(*) FROM produtos")
    suspend fun obterTotalLotes(): Int

    @Query("SELECT COUNT(DISTINCT setor) FROM produtos")
    suspend fun obterSetoresAtivos(): Int

    @Query("SELECT * FROM produtos ORDER BY ts DESC LIMIT 1")
    suspend fun obterUltimoLote(): ProdutoEntity?

    /** Usado para bloquear SAÍDA que geraria estoque negativo. */
    @Query("SELECT COALESCE(SUM(CASE WHEN tipo = 'ENTRADA' THEN qty ELSE -qty END), 0) FROM produtos WHERE sku = :sku")
    suspend fun obterSaldoPorSku(sku: String): Int

    @Query("""
        SELECT sku,
               COALESCE(SUM(CASE WHEN tipo = 'ENTRADA' THEN qty ELSE -qty END), 0) AS saldo,
               COALESCE(SUM(CASE WHEN tipo = 'ENTRADA' THEN qty ELSE 0 END), 0)    AS entradas,
               COALESCE(SUM(CASE WHEN tipo = 'SAIDA'   THEN qty ELSE 0 END), 0)    AS saidas
        FROM produtos GROUP BY sku ORDER BY sku ASC
    """)
    fun obterSaldosPorSku(): LiveData<List<SaldoSkuItem>>

    @Query("""
        SELECT sku,
               COALESCE(SUM(CASE WHEN tipo = 'ENTRADA' THEN qty ELSE -qty END), 0) AS saldo,
               COALESCE(SUM(CASE WHEN tipo = 'ENTRADA' THEN qty ELSE 0 END), 0)    AS entradas,
               COALESCE(SUM(CASE WHEN tipo = 'SAIDA'   THEN qty ELSE 0 END), 0)    AS saidas
        FROM produtos GROUP BY sku ORDER BY sku ASC
    """)
    suspend fun obterSaldosPorSkuSuspend(): List<SaldoSkuItem>

    /** Remove todos os registros — usado nos testes para garantir isolamento entre casos. */
    @Query("DELETE FROM produtos")
    suspend fun limparTabela()
}
