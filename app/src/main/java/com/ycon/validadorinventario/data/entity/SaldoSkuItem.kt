package com.ycon.validadorinventario.data.entity

import androidx.room.ColumnInfo

/** Resultado da query de saldo agrupado por SKU — não é uma entidade persistida. */
data class SaldoSkuItem(
    @ColumnInfo(name = "sku")      val sku: String,
    @ColumnInfo(name = "saldo")    val saldo: Int,
    @ColumnInfo(name = "entradas") val entradas: Int,
    @ColumnInfo(name = "saidas")   val saidas: Int
)
