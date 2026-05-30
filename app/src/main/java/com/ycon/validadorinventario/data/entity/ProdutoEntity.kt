package com.ycon.validadorinventario.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** O saldo por SKU é calculado dinamicamente (ENTRADAS − SAÍDAS); não há coluna de saldo armazenada. */
@Entity(tableName = "produtos")
data class ProdutoEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "sku")
    val sku: String,

    @ColumnInfo(name = "qty")
    val qty: Int,

    @ColumnInfo(name = "setor")
    val setor: String,

    @ColumnInfo(name = "bairro")
    val bairro: String,

    /** Instante do registro em milissegundos (epoch Unix) */
    @ColumnInfo(name = "ts")
    val ts: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "tipo")
    val tipo: String = "ENTRADA"
)
