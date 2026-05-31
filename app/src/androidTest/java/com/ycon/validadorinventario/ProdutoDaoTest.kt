package com.ycon.validadorinventario

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ycon.validadorinventario.data.dao.ProdutoDao
import com.ycon.validadorinventario.data.db.AppDatabase
import com.ycon.validadorinventario.data.entity.ProdutoEntity
import com.ycon.validadorinventario.data.entity.SaldoSkuItem
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Room.inMemoryDatabaseBuilder() cria um banco temporário descartado após cada teste.
 *  @RunWith(AndroidJUnit4::class) é necessário porque o Room exige Context do Android, mesmo em memória. */
@RunWith(AndroidJUnit4::class)
class ProdutoDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ProdutoDao

    @Before
    fun criarBanco() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build() // allowMainThreadQueries() permitido apenas em testes

        dao = db.produtoDao()
    }

    @After
    fun fecharBanco() {
        db.close()
    }

    @Test
    fun `inserirProduto deve retornar id maior que zero`() = runTest {
        val produto = criarProduto(sku = "SKU-001", qty = 10)
        val id = dao.inserirProduto(produto)
        assert(id > 0) { "ID gerado pelo autoincrement deve ser > 0" }
    }

    @Test
    fun `inserirProduto deve persistir todos os campos corretamente`() = runTest {
        val produto = ProdutoEntity(
            sku    = "SKU-ABC",
            qty    = 55,
            setor  = "Setor A",
            bairro = ""
        )
        dao.inserirProduto(produto)

        val lista = dao.obterTodosOsProdutosSuspend()
        assertEquals(1, lista.size)

        val salvo = lista.first()
        assertEquals("SKU-ABC", salvo.sku)
        assertEquals(55,        salvo.qty)
        assertEquals("Setor A", salvo.setor)
    }

    @Test
    fun `obterTodosOsProdutos deve retornar lista vazia quando banco esta vazio`() = runTest {
        val lista = dao.obterTodosOsProdutosSuspend()
        assertEquals(0, lista.size)
    }

    @Test
    fun `obterTodosOsProdutos deve retornar em ordem decrescente por data`() = runTest {
        dao.inserirProduto(criarProduto("SKU-A", 10, ts = 1000L))
        dao.inserirProduto(criarProduto("SKU-B", 20, ts = 3000L))
        dao.inserirProduto(criarProduto("SKU-C", 30, ts = 2000L))

        val lista = dao.obterTodosOsProdutosSuspend()

        assertEquals("SKU-B", lista[0].sku) // ts = 3000 → mais recente
        assertEquals("SKU-C", lista[1].sku) // ts = 2000
        assertEquals("SKU-A", lista[2].sku) // ts = 1000 → mais antigo
    }

    @Test
    fun `obterTotalItensEstoque deve retornar zero quando banco esta vazio`() = runTest {
        val total = dao.obterTotalItensEstoque()
        assertEquals(0, total)
    }

    @Test
    fun `obterTotalItensEstoque deve somar corretamente multiplas entradas`() = runTest {
        dao.inserirProduto(criarProduto("SKU-1", 100))
        dao.inserirProduto(criarProduto("SKU-2", 250))
        dao.inserirProduto(criarProduto("SKU-3",  50))

        val total = dao.obterTotalItensEstoque()
        assertEquals(400, total) // 100 + 250 + 50
    }

    @Test
    fun `obterTotalLotes deve contar registros corretamente`() = runTest {
        repeat(5) { i -> dao.inserirProduto(criarProduto("SKU-$i", i + 1)) }
        assertEquals(5, dao.obterTotalLotes())
    }

    @Test
    fun `obterSetoresAtivos deve contar setores distintos`() = runTest {
        dao.inserirProduto(criarProduto("SKU-1", 10, setor = "Setor A"))
        dao.inserirProduto(criarProduto("SKU-2", 20, setor = "Setor A")) // duplicado
        dao.inserirProduto(criarProduto("SKU-3", 30, setor = "Setor B"))
        dao.inserirProduto(criarProduto("SKU-4", 40, setor = "Setor C"))

        val setores = dao.obterSetoresAtivos()
        assertEquals(3, setores) // A, B e C — não conta A duas vezes
    }

    @Test
    fun `obterUltimoLote deve retornar null quando banco esta vazio`() = runTest {
        val ultimo = dao.obterUltimoLote()
        assertNull(ultimo)
    }

    @Test
    fun `obterUltimoLote deve retornar o produto com maior timestamp`() = runTest {
        dao.inserirProduto(criarProduto("SKU-OLD", 10, ts = 1000L))
        dao.inserirProduto(criarProduto("SKU-NEW", 99, ts = 9999L))

        val ultimo = dao.obterUltimoLote()
        assertNotNull(ultimo)
        assertEquals("SKU-NEW", ultimo!!.sku)
    }

    @Test
    fun `limparTabela deve remover todos os registros`() = runTest {
        repeat(3) { dao.inserirProduto(criarProduto("SKU-$it", it + 1)) }
        dao.limparTabela()

        assertEquals(0, dao.obterTotalItensEstoque())
        assertEquals(0, dao.obterTotalLotes())
    }

    @Test
    fun `obterTotalItensEstoque deve subtrair saida corretamente`() = runTest {
        dao.inserirProduto(criarProduto("SKU-1", 100, tipo = "ENTRADA"))
        dao.inserirProduto(criarProduto("SKU-1",  30, tipo = "SAIDA"))

        val saldo = dao.obterTotalItensEstoque()
        assertEquals(70, saldo) // 100 - 30
    }

    @Test
    fun `obterTotalItensEstoque deve calcular saldo correto com multiplos movimentos mistos`() = runTest {
        dao.inserirProduto(criarProduto("SKU-A", 200, tipo = "ENTRADA"))
        dao.inserirProduto(criarProduto("SKU-B", 100, tipo = "ENTRADA"))
        dao.inserirProduto(criarProduto("SKU-A",  50, tipo = "SAIDA"))
        dao.inserirProduto(criarProduto("SKU-B",  30, tipo = "SAIDA"))

        val saldo = dao.obterTotalItensEstoque()
        assertEquals(220, saldo) // (200-50) + (100-30)
    }

    @Test
    fun `obterSaldoPorSku deve retornar apenas saldo do sku especificado`() = runTest {
        dao.inserirProduto(criarProduto("SKU-X", 150, tipo = "ENTRADA"))
        dao.inserirProduto(criarProduto("SKU-X",  40, tipo = "SAIDA"))
        dao.inserirProduto(criarProduto("SKU-Y", 999, tipo = "ENTRADA"))

        val saldoX = dao.obterSaldoPorSku("SKU-X")
        assertEquals(110, saldoX) // 150 - 40 (SKU-Y não interfere)
    }

    @Test
    fun `obterSaldoPorSku deve retornar zero para sku inexistente`() = runTest {
        val saldo = dao.obterSaldoPorSku("NAO-EXISTE")
        assertEquals(0, saldo)
    }

    @Test
    fun `obterSaldosPorSkuSuspend deve agrupar movimentos corretamente por sku`() = runTest {
        dao.inserirProduto(criarProduto("SKU-A", 100, tipo = "ENTRADA"))
        dao.inserirProduto(criarProduto("SKU-A",  30, tipo = "SAIDA"))
        dao.inserirProduto(criarProduto("SKU-B",  50, tipo = "ENTRADA"))

        val saldos = dao.obterSaldosPorSkuSuspend()
        assertEquals(2, saldos.size)

        val saldoA = saldos.first { it.sku == "SKU-A" }
        assertEquals(70,  saldoA.saldo)
        assertEquals(100, saldoA.entradas)
        assertEquals(30,  saldoA.saidas)

        val saldoB = saldos.first { it.sku == "SKU-B" }
        assertEquals(50, saldoB.saldo)
        assertEquals(50, saldoB.entradas)
        assertEquals(0,  saldoB.saidas)
    }

    private fun criarProduto(
        sku: String,
        qty: Int,
        setor: String = "Setor A",
        ts: Long = System.currentTimeMillis(),
        tipo: String = "ENTRADA"
    ) = ProdutoEntity(sku = sku, qty = qty, setor = setor, bairro = "", ts = ts, tipo = tipo)
}
