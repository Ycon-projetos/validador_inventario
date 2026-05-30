package com.ycon.validadorinventario.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ycon.validadorinventario.data.ProdutoRepository
import com.ycon.validadorinventario.data.db.AppDatabase
import com.ycon.validadorinventario.data.entity.ProdutoEntity
import kotlinx.coroutines.launch

class InventarioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProdutoRepository

    // Termos personalizados para autocomplete do campo "Outro — Setor"
    val termosSetor: LiveData<List<String>>

    // Filtro de busca por SKU + lista filtrada
    private val _filtroSku = MutableLiveData("")

    /**
     * Lista filtrada para o RecyclerView.
     * MediatorLiveData combina a lista completa do banco com o texto de busca
     * em memória, sem consultas extras ao SQLite.
     */
    val produtosFiltrados = MediatorLiveData<List<ProdutoEntity>>()

    // Evento de registro bem-sucedido — nullable para evitar re-disparo ao rotacionar
    private val _registroSucesso = MutableLiveData<Boolean?>()
    val registroSucesso: LiveData<Boolean?> = _registroSucesso

    // Métricas do painel
    private val _totalItens    = MutableLiveData(0)
    private val _totalLotes    = MutableLiveData(0)
    private val _setoresAtivos = MutableLiveData(0)
    private val _ultimoLote    = MutableLiveData("—")
    private val _mensagem      = MutableLiveData<String?>()
    private val _carregando    = MutableLiveData(false)

    val totalItens:    LiveData<Int>      = _totalItens
    val totalLotes:    LiveData<Int>      = _totalLotes
    val setoresAtivos: LiveData<Int>      = _setoresAtivos
    val ultimoLote:    LiveData<String>   = _ultimoLote
    val mensagem:      LiveData<String?>  = _mensagem
    val carregando:    LiveData<Boolean>  = _carregando

    init {
        val db = AppDatabase.getInstance(application)
        repository  = ProdutoRepository(db.produtoDao(), db.termoPersonalizadoDao())
        termosSetor = repository.termosSetor

        val todosProdutos = repository.todosProdutos
        produtosFiltrados.addSource(todosProdutos) { lista ->
            val filtro = _filtroSku.value?.trim()?.uppercase() ?: ""
            produtosFiltrados.value =
                if (filtro.isBlank()) lista else lista.filter { it.sku.contains(filtro) }
        }
        produtosFiltrados.addSource(_filtroSku) { texto ->
            val lista = todosProdutos.value ?: emptyList()
            val filtro = texto.trim().uppercase()
            produtosFiltrados.value =
                if (filtro.isBlank()) lista else lista.filter { it.sku.contains(filtro) }
        }

        atualizarMetricas()
    }

    fun filtrar(texto: String) {
        _filtroSku.value = texto
    }

    // Chamados pela Activity após consumir o evento (evita sticky re-disparo)
    fun limparMensagem() { _mensagem.value = null }
    fun limparRegistroSucesso() { _registroSucesso.value = null }

    /**
     * Registra um movimento de estoque (ENTRADA ou SAIDA).
     * Para SAIDA: bloqueia se a quantidade solicitada superar o saldo atual do SKU.
     */
    fun registrarLote(
        sku: String,
        qty: Int,
        setor: String,
        tipo: String = "ENTRADA",
        setorIsCustom: Boolean = false
    ) {
        if (sku.isBlank())   { _mensagem.value = "SKU não pode ser vazio"; return }
        if (qty < 1)         { _mensagem.value = "Quantidade deve ser ≥ 1"; return }
        if (setor.isBlank()) { _mensagem.value = "Selecione um setor"; return }

        val skuNorm = sku.trim().uppercase()

        viewModelScope.launch {
            _carregando.value = true
            try {
                if (tipo == "SAIDA") {
                    val saldoAtual = repository.saldoPorSku(skuNorm)
                    if (qty > saldoAtual) {
                        _mensagem.value = if (saldoAtual == 0)
                            "✗ $skuNorm sem estoque. Registre uma ENTRADA primeiro."
                        else
                            "✗ Saldo insuficiente: disponível $saldoAtual un. para $skuNorm"
                        return@launch
                    }
                }

                val produto = ProdutoEntity(
                    sku    = skuNorm,
                    qty    = qty,
                    setor  = setor,
                    bairro = "",
                    tipo   = tipo
                )
                repository.inserir(produto)

                if (setorIsCustom) repository.salvarTermoCustom("SETOR", setor)

                // Aguarda as métricas antes de sinalizar sucesso (evita race condition)
                atualizarMetricasSuspend()

                val prefixo = if (tipo == "ENTRADA") "+" else "-"
                _mensagem.value = "✓ $skuNorm · ${prefixo}${qty} un. registradas"
                _registroSucesso.value = true

            } catch (e: Exception) {
                _mensagem.value = "✗ Erro ao registrar: ${e.message}"
            } finally {
                _carregando.value = false
            }
        }
    }

    fun atualizarMetricas() {
        viewModelScope.launch { atualizarMetricasSuspend() }
    }

    private suspend fun atualizarMetricasSuspend() {
        _totalItens.value    = repository.totalItens()
        _totalLotes.value    = repository.totalLotes()
        _setoresAtivos.value = repository.setoresAtivos()
        val ultimo = repository.ultimoLote()
        _ultimoLote.value = when {
            ultimo == null           -> "—"
            ultimo.tipo == "ENTRADA" -> "+${ultimo.qty}"
            else                     -> "-${ultimo.qty}"
        }
    }
}
