package com.ycon.validadorinventario.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.ycon.validadorinventario.R
import com.ycon.validadorinventario.databinding.ActivityMainBinding
import com.ycon.validadorinventario.ui.adapter.ProdutoAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: InventarioViewModel by viewModels()
    private val adapter = ProdutoAdapter()

    // Opções do dropdown sem o placeholder (índice 0 da array)
    private lateinit var opcoesSetor: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        opcoesSetor = resources.getStringArray(R.array.setores).drop(1).toTypedArray()

        configurarRecyclerView()
        configurarToggleTipo()
        configurarDropdownSetor()
        configurarBotaoRegistrar()
        configurarBusca()
        observarViewModel()
    }

    // -------------------------------------------------------------------------
    // RecyclerView
    // -------------------------------------------------------------------------

    private fun configurarRecyclerView() {
        binding.recyclerProdutos.layoutManager = LinearLayoutManager(this)
        binding.recyclerProdutos.adapter = adapter
    }

    // -------------------------------------------------------------------------
    // Toggle ENTRADA / SAÍDA
    // -------------------------------------------------------------------------

    private fun configurarToggleTipo() {
        binding.toggleTipo.check(R.id.btnEntrada)
        atualizarEstadoTipo(isEntrada = true)

        binding.toggleTipo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) atualizarEstadoTipo(isEntrada = checkedId == R.id.btnEntrada)
        }
    }

    private fun atualizarEstadoTipo(isEntrada: Boolean) {
        binding.btnRegistrar.text =
            if (isEntrada) "REGISTRAR ENTRADA" else "REGISTRAR SAÍDA"

        val cor = ContextCompat.getColor(this, if (isEntrada) R.color.green else R.color.coral)
        binding.btnRegistrar.backgroundTintList = ColorStateList.valueOf(cor)
        binding.cardFormulario.setStrokeColor(ColorStateList.valueOf(cor))
    }

    // -------------------------------------------------------------------------
    // Dropdown Setor (ExposedDropdownMenu)
    // -------------------------------------------------------------------------

    private fun configurarDropdownSetor() {
        val adapterSetor = ArrayAdapter(this, android.R.layout.simple_list_item_1, opcoesSetor)
        binding.edtSetor.setAdapter(adapterSetor)

        binding.edtSetor.setOnItemClickListener { _, _, pos, _ ->
            val isOutro = opcoesSetor[pos] == "Outro"
            binding.layoutSetorCustom.visibility = if (isOutro) View.VISIBLE else View.GONE
            if (!isOutro) binding.edtSetorCustom.text?.clear()
        }
    }

    // -------------------------------------------------------------------------
    // Busca por SKU no histórico
    // -------------------------------------------------------------------------

    private fun configurarBusca() {
        binding.edtBusca.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.filtrar(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // -------------------------------------------------------------------------
    // Botão Registrar
    // -------------------------------------------------------------------------

    private fun configurarBotaoRegistrar() {
        binding.btnRegistrar.setOnClickListener {
            val sku    = binding.edtSku.text.toString()
            val qtyStr = binding.edtQty.text.toString()
            val qty    = qtyStr.toIntOrNull() ?: 0
            val tipo   = if (binding.toggleTipo.checkedButtonId == R.id.btnSaida) "SAIDA" else "ENTRADA"

            val setorTexto = binding.edtSetor.text.toString()
            if (setorTexto.isBlank()) {
                Snackbar.make(binding.root, "Selecione um setor", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val setorIsCustom = setorTexto == "Outro"
            val setor = if (setorIsCustom)
                binding.edtSetorCustom.text.toString().trim().uppercase()
            else
                setorTexto

            if (setorIsCustom && setor.isBlank()) {
                Snackbar.make(binding.root, "Digite o nome do setor", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            viewModel.registrarLote(sku, qty, setor, tipo, setorIsCustom)
        }
    }

    // -------------------------------------------------------------------------
    // Observadores de LiveData
    // -------------------------------------------------------------------------

    private fun observarViewModel() {

        viewModel.produtosFiltrados.observe(this) { lista ->
            adapter.submitList(lista)
            if (lista.isEmpty()) {
                val filtroAtivo = binding.edtBusca.text?.isNotBlank() == true
                binding.txtEmptyState.text = if (filtroAtivo)
                    "Nenhum produto encontrado para essa busca."
                else
                    "Nenhum lote registrado ainda."
                binding.txtEmptyState.visibility = View.VISIBLE
            } else {
                binding.txtEmptyState.visibility = View.GONE
            }
        }

        viewModel.totalItens.observe(this)    { binding.txtTotalEstoque.text = it.toString() }
        viewModel.totalLotes.observe(this)    { binding.txtTotalLotes.text = it.toString() }
        viewModel.ultimoLote.observe(this)    { binding.txtUltimoLote.text = it }
        viewModel.setoresAtivos.observe(this) { binding.txtSetoresAtivos.text = it.toString() }

        viewModel.carregando.observe(this) { binding.btnRegistrar.isEnabled = !it }

        // Consome a mensagem e limpa após exibir (evita re-disparo ao rotacionar)
        viewModel.mensagem.observe(this) { msg ->
            if (!msg.isNullOrBlank()) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                viewModel.limparMensagem()
            }
        }

        // Reseta o formulário após registro bem-sucedido e consome o evento
        viewModel.registroSucesso.observe(this) { sucesso ->
            if (sucesso == true) {
                binding.edtSku.text?.clear()
                binding.edtQty.text?.clear()
                binding.edtSetor.text?.clear()
                binding.layoutSetorCustom.visibility = View.GONE
                binding.edtSetorCustom.text?.clear()
                viewModel.limparRegistroSucesso()
            }
        }

    }
}
