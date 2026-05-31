package com.ycon.validadorinventario.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ycon.validadorinventario.R
import com.ycon.validadorinventario.data.entity.ProdutoEntity
import com.ycon.validadorinventario.databinding.ItemProdutoBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProdutoAdapter : ListAdapter<ProdutoEntity, ProdutoAdapter.ProdutoViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProdutoViewHolder {
        val binding = ItemProdutoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProdutoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProdutoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProdutoViewHolder(
        private val binding: ItemProdutoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val formatoData = SimpleDateFormat("dd/MM/yy HH:mm", Locale("pt", "BR"))

        fun bind(produto: ProdutoEntity) {
            val contexto = itemView.context
            val isEntrada = produto.tipo == "ENTRADA"

            val corPrincipal = ContextCompat.getColor(contexto,
                if (isEntrada) R.color.green_mid else R.color.coral)
            val corIconeFundo = ContextCompat.getColor(contexto,
                if (isEntrada) R.color.green_light else R.color.coral_light)

            binding.txtSku.text   = produto.sku
            binding.txtSetor.text = produto.setor
            binding.txtSetor.setTextColor(
                ContextCompat.getColor(contexto, if (isEntrada) R.color.green_mid else R.color.coral)
            )
            binding.txtTs.text = formatoData.format(Date(produto.ts))

            binding.txtTipo.text = produto.tipo
            binding.txtTipo.setTextColor(corPrincipal)

            binding.txtQty.text = if (isEntrada) "+${produto.qty} un." else "-${produto.qty} un."
            binding.txtQty.setTextColor(corPrincipal)

            binding.txtIcon.text = if (isEntrada) "📦" else "📤"
            binding.cardIcon.setCardBackgroundColor(corIconeFundo)
        }
    }

    companion object {
        internal val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ProdutoEntity>() {
            override fun areItemsTheSame(antigo: ProdutoEntity, novo: ProdutoEntity) =
                antigo.id == novo.id
            override fun areContentsTheSame(antigo: ProdutoEntity, novo: ProdutoEntity) =
                antigo == novo
        }
    }
}
