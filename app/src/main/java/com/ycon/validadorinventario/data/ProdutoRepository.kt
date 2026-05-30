package com.ycon.validadorinventario.data

import androidx.lifecycle.LiveData
import com.ycon.validadorinventario.data.dao.ProdutoDao
import com.ycon.validadorinventario.data.dao.TermoPersonalizadoDao
import com.ycon.validadorinventario.data.entity.ProdutoEntity
import com.ycon.validadorinventario.data.entity.TermoPersonalizadoEntity

class ProdutoRepository(
    private val dao: ProdutoDao,
    private val termoDao: TermoPersonalizadoDao
) {

    val todosProdutos: LiveData<List<ProdutoEntity>> = dao.obterTodosOsProdutos()

    val termosSetor: LiveData<List<String>> = termoDao.observar("SETOR")

    suspend fun inserir(produto: ProdutoEntity): Long = dao.inserirProduto(produto)

    suspend fun totalItens(): Int = dao.obterTotalItensEstoque()

    /** Saldo atual de um SKU — usado para bloquear SAÍDA que geraria estoque negativo. */
    suspend fun saldoPorSku(sku: String): Int = dao.obterSaldoPorSku(sku)

    suspend fun totalLotes(): Int = dao.obterTotalLotes()
    suspend fun setoresAtivos(): Int = dao.obterSetoresAtivos()
    suspend fun ultimoLote(): ProdutoEntity? = dao.obterUltimoLote()

    /**
     * Salva um valor personalizado digitado no campo "Outro".
     * Duplicatas são ignoradas silenciosamente pelo índice único da tabela.
     */
    suspend fun salvarTermoCustom(categoria: String, valor: String) {
        if (valor.isBlank()) return
        termoDao.inserir(TermoPersonalizadoEntity(categoria = categoria, valor = valor))
    }
}
