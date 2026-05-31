package com.ycon.validadorinventario.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ycon.validadorinventario.R
import com.ycon.validadorinventario.data.entity.SaldoSkuItem
import com.ycon.validadorinventario.databinding.ItemSaldoSkuBinding

class SaldoSkuAdapter : ListAdapter<SaldoSkuItem, SaldoSkuAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemSaldoSkuBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemSaldoSkuBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SaldoSkuItem) {
            val ctx = itemView.context
            binding.txtSkuSaldo.text  = item.sku
            binding.txtEntradas.text  = "+${item.entradas} ent."
            binding.txtSaidas.text    = "-${item.saidas} saí."
            binding.txtSaldoSku.text  = "${item.saldo}"
            binding.txtSaldoSku.setTextColor(
                ContextCompat.getColor(ctx, if (item.saldo > 0) R.color.green else R.color.coral)
            )
        }
    }

    companion object {
        internal val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SaldoSkuItem>() {
            override fun areItemsTheSame(a: SaldoSkuItem, b: SaldoSkuItem) = a.sku == b.sku
            override fun areContentsTheSame(a: SaldoSkuItem, b: SaldoSkuItem) = a == b
        }
    }
}
