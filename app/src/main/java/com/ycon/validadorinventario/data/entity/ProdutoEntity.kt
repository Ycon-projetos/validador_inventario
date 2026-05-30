package com.ycon.validadorinventario.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ProdutoEntity — representa um registro de movimentação de estoque.
 *
 * Cada linha da tabela `produtos` corresponde a um único movimento:
 * uma ENTRADA (mercadoria chegando) ou uma SAÍDA (mercadoria saindo).
 * O saldo atual de um produto é calculado dinamicamente somando todas
 * as entradas e subtraindo todas as saídas do mesmo SKU.
 *
 * Decisões de projeto:
 *   - @PrimaryKey(autoGenerate = true) → o Room gera o ID automaticamente,
 *     equivalente ao AUTOINCREMENT do SQLite.
 *   - O campo `ts` armazena o instante do registro como epoch Unix (ms),
 *     permitindo ordenação eficiente com ORDER BY ts DESC.
 */
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

    /** Tipo do movimento: "ENTRADA" (mercadoria entra) ou "SAIDA" (mercadoria sai) */
    @ColumnInfo(name = "tipo")
    val tipo: String = "ENTRADA"
)
