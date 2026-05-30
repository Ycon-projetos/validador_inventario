package com.ycon.validadorinventario.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * TermoPersonalizadoEntity — armazena valores digitados pelo usuário
 * ao selecionar "Outro" no campo de seleção de setor.
 *
 * O índice único em (categoria + valor) impede que o mesmo termo
 * seja salvo mais de uma vez. Quando ocorre uma duplicata, a operação
 * é simplesmente ignorada — sem erros, sem repetições nas sugestões.
 */
@Entity(
    tableName = "termos_personalizados",
    indices = [Index(value = ["categoria", "valor"], unique = true)]
)
data class TermoPersonalizadoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Identifica o campo de origem — atualmente somente "SETOR" */
    @ColumnInfo(name = "categoria")
    val categoria: String,

    /** Valor digitado pelo usuário, normalizado (sem espaços extras, em maiúsculas) */
    @ColumnInfo(name = "valor")
    val valor: String
)
