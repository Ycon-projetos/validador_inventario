package com.ycon.validadorinventario.ui

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ycon.validadorinventario.data.ProdutoRepository
import com.ycon.validadorinventario.data.db.AppDatabase
import com.ycon.validadorinventario.data.entity.ProdutoEntity
import com.ycon.validadorinventario.data.entity.SaldoSkuItem
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InventarioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProdutoRepository

    val termosSetor: LiveData<List<String>>
    val saldosPorSku: LiveData<List<SaldoSkuItem>>

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
        repository   = ProdutoRepository(db.produtoDao(), db.termoPersonalizadoDao())
        termosSetor  = repository.termosSetor
        saldosPorSku = repository.saldosPorSku

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

    /** Remove todos os movimentos. Os setores personalizados são preservados. */
    fun limparInventario() {
        viewModelScope.launch {
            _carregando.value = true
            try {
                repository.limparInventario()
                atualizarMetricasSuspend()
                _mensagem.value = "✓ Inventário limpo com sucesso"
                _registroSucesso.value = true
            } catch (e: Exception) {
                _mensagem.value = "✗ Erro ao limpar: ${e.message}"
            } finally {
                _carregando.value = false
            }
        }
    }

    /**
     * Gera um arquivo CSV com todos os movimentos e retorna a URI para compartilhamento.
     * Retorna null e posta mensagem se não houver dados.
     */
    fun exportarCsv(): Uri? {
        val lista = repository.todosProdutos.value
        if (lista.isNullOrEmpty()) {
            _mensagem.value = "Nenhum movimento para exportar"
            return null
        }
        return try {
            val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
            val sb = StringBuilder("SKU,Quantidade,Tipo,Setor,Data\n")
            lista.forEach { p ->
                val prefixo = if (p.tipo == "ENTRADA") "+" else "-"
                sb.append("${p.sku},${prefixo}${p.qty},${p.tipo},${p.setor},${formato.format(Date(p.ts))}\n")
            }
            val app = getApplication<Application>()
            val arquivo = File(app.cacheDir, "inventario_${System.currentTimeMillis()}.csv")
            arquivo.writeText(sb.toString())
            FileProvider.getUriForFile(app, "${app.packageName}.provider", arquivo)
        } catch (e: Exception) {
            _mensagem.value = "✗ Erro ao exportar: ${e.message}"
            null
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
