package com.ycon.validadorinventario

import org.junit.Assert.assertEquals
import org.junit.Test

class InventarioViewModelLogicTest {

    private fun validarCampos(
        sku: String,
        qty: Int,
        setor: String
    ): String? {
        if (sku.isBlank())   return "SKU não pode ser vazio"
        if (qty < 1)         return "Quantidade deve ser ≥ 1"
        if (setor.isBlank()) return "Selecione um setor"
        return null // null = campos válidos
    }

    @Test
    fun `deve rejeitar SKU vazio`() {
        val erro = validarCampos(sku = "", qty = 10, setor = "Setor A")
        assertEquals("SKU não pode ser vazio", erro)
    }

    @Test
    fun `deve rejeitar SKU somente espacos`() {
        val erro = validarCampos(sku = "   ", qty = 10, setor = "Setor A")
        assertEquals("SKU não pode ser vazio", erro)
    }

    @Test
    fun `deve rejeitar quantidade zero`() {
        val erro = validarCampos(sku = "SKU-001", qty = 0, setor = "Setor A")
        assertEquals("Quantidade deve ser ≥ 1", erro)
    }

    @Test
    fun `deve rejeitar quantidade negativa`() {
        val erro = validarCampos(sku = "SKU-001", qty = -5, setor = "Setor A")
        assertEquals("Quantidade deve ser ≥ 1", erro)
    }

    @Test
    fun `deve aceitar quantidade igual a 1`() {
        val erro = validarCampos(sku = "SKU-001", qty = 1, setor = "Setor A")
        assertEquals(null, erro)
    }

    @Test
    fun `deve rejeitar setor vazio`() {
        val erro = validarCampos(sku = "SKU-001", qty = 10, setor = "")
        assertEquals("Selecione um setor", erro)
    }

    @Test
    fun `deve rejeitar setor somente espacos`() {
        val erro = validarCampos(sku = "SKU-001", qty = 10, setor = "   ")
        assertEquals("Selecione um setor", erro)
    }

    @Test
    fun `deve aceitar todos os campos validos`() {
        val erro = validarCampos(
            sku   = "SKU-XYZ",
            qty   = 500,
            setor = "Setor A — Recebimento"
        )
        assertEquals(null, erro)
    }

    @Test
    fun `deve aceitar SKU com espacos nas bordas`() {
        val erro = validarCampos(sku = "  SKU-001  ", qty = 1, setor = "Setor B")
        assertEquals(null, erro)
    }
}
