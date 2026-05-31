package com.ycon.validadorinventario

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ycon.validadorinventario.data.entity.ProdutoEntity
import com.ycon.validadorinventario.ui.adapter.ProdutoAdapter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProdutoAdapterItemCallbackTest {

    private val callback = ProdutoAdapter.DIFF_CALLBACK

    @Test
    fun `areItemsTheSame retorna true para o mesmo id`() {
        val p1 = criarProduto(id = 1, sku = "SKU-A")
        val p2 = criarProduto(id = 1, sku = "SKU-B") // mesmo id, conteúdo diferente
        assertTrue(callback.areItemsTheSame(p1, p2))
    }

    @Test
    fun `areItemsTheSame retorna false para ids diferentes`() {
        val p1 = criarProduto(id = 1)
        val p2 = criarProduto(id = 2)
        assertFalse(callback.areItemsTheSame(p1, p2))
    }

    @Test
    fun `areContentsTheSame retorna true para cópias identicas`() {
        val produto = criarProduto(id = 1, sku = "SKU-Z", qty = 50)
        assertTrue(callback.areContentsTheSame(produto, produto.copy()))
    }

    @Test
    fun `areContentsTheSame retorna false quando quantidade difere`() {
        val p1 = criarProduto(id = 1, qty = 10)
        val p2 = criarProduto(id = 1, qty = 99)
        assertFalse(callback.areContentsTheSame(p1, p2))
    }

    private fun criarProduto(
        id: Int = 0,
        sku: String = "SKU-TEST",
        qty: Int = 10
    ) = ProdutoEntity(id = id, sku = sku, qty = qty, setor = "Setor A", bairro = "")
}
