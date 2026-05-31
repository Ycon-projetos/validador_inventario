/*
 * Projeto de Extensão — Programação para Dispositivos Móveis em Android
 * Curso: Análise e Desenvolvimento de Sistemas (ADS) — Estácio
 *
 * Desenvolvedor : Ramon Bianco Gonçalves
 * Matrícula     : 202401194166
 * Parceiro      : Ycon Inteligência e Tecnologia — Sorocaba/SP
 * Homologação   : Vitor Hugo de Paula Pereira (Diretor de Tecnologia — Ycon)
 */
package com.ycon.validadorinventario.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_saldo_sku -> {
                SaldoSkuBottomSheet().show(supportFragmentManager, SaldoSkuBottomSheet.TAG)
                true
            }
            R.id.action_exportar_csv -> {
                val uri = viewModel.exportarCsv()
                if (uri != null) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Controle de Inventário — ${android.text.format.DateFormat.format("dd/MM/yyyy", System.currentTimeMillis())}")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, "Compartilhar inventário"))
                }
                true
            }
            R.id.action_limpar_inventario -> {
                AlertDialog.Builder(this)
                    .setTitle("Limpar inventário")
                    .setMessage("Todos os movimentos serão removidos. Esta ação não pode ser desfeita.")
                    .setPositiveButton("Limpar") { _, _ -> viewModel.limparInventario() }
                    .setNegativeButton("Cancelar", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun configurarRecyclerView() {
        binding.recyclerProdutos.layoutManager = LinearLayoutManager(this)
        binding.recyclerProdutos.adapter = adapter
    }

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

    private fun configurarDropdownSetor() {
        val adapterSetor = ArrayAdapter(this, android.R.layout.simple_list_item_1, opcoesSetor)
        binding.edtSetor.setAdapter(adapterSetor)

        binding.edtSetor.setOnItemClickListener { _, _, pos, _ ->
            val isOutro = opcoesSetor[pos] == "Outro"
            binding.layoutSetorCustom.visibility = if (isOutro) View.VISIBLE else View.GONE
            if (!isOutro) binding.edtSetorCustom.text?.clear()
        }
    }

    private fun configurarBusca() {
        binding.edtBusca.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.filtrar(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

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
