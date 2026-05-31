package com.ycon.validadorinventario

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ycon.validadorinventario.data.ProdutoRepository
import com.ycon.validadorinventario.data.db.AppDatabase
import com.ycon.validadorinventario.data.entity.ProdutoEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ProdutoRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ProdutoRepository

    @Before
    fun criarRepositorio() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = ProdutoRepository(db.produtoDao(), db.termoPersonalizadoDao())
    }

    @After
    fun fecharBanco() {
        db.close()
    }

    @Test
    fun `inserir deve retornar id valido`() = runTest {
        val id = repository.inserir(criarProduto("SKU-001", 10))
        assertTrue("ID deve ser > 0", id > 0)
    }

    @Test
    fun `totalItens deve retornar zero quando banco esta vazio`() = runTest {
        assertEquals(0, repository.totalItens())
    }

    @Test
    fun `totalItens deve calcular saldo liquido entre entradas e saidas`() = runTest {
        repository.inserir(criarProduto("SKU-A", 200, tipo = "ENTRADA"))
        repository.inserir(criarProduto("SKU-A",  50, tipo = "SAIDA"))
        assertEquals(150, repository.totalItens())
    }

    @Test
    fun `totalItens deve agregar multiplos skus corretamente`() = runTest {
        repository.inserir(criarProduto("SKU-1", 100, tipo = "ENTRADA"))
        repository.inserir(criarProduto("SKU-2", 200, tipo = "ENTRADA"))
        repository.inserir(criarProduto("SKU-1",  30, tipo = "SAIDA"))
        assertEquals(270, repository.totalItens()) // (100-30) + 200
    }

    @Test
    fun `saldoPorSku deve retornar zero para sku inexistente`() = runTest {
        assertEquals(0, repository.saldoPorSku("NAO-EXISTE"))
    }

    @Test
    fun `saldoPorSku deve calcular saldo isolado por sku`() = runTest {
        repository.inserir(criarProduto("SKU-X", 100, tipo = "ENTRADA"))
        repository.inserir(criarProduto("SKU-X",  30, tipo = "SAIDA"))
        repository.inserir(criarProduto("SKU-Y", 999, tipo = "ENTRADA")) // não deve interferir
        assertEquals(70, repository.saldoPorSku("SKU-X"))
    }

    @Test
    fun `totalLotes deve contar todos os movimentos`() = runTest {
        repeat(4) { i -> repository.inserir(criarProduto("SKU-$i", i + 1)) }
        assertEquals(4, repository.totalLotes())
    }

    @Test
    fun `setoresAtivos deve contar setores distintos`() = runTest {
        repository.inserir(criarProduto("SKU-1", 10, setor = "Setor A"))
        repository.inserir(criarProduto("SKU-2", 20, setor = "Setor A")) // duplicado
        repository.inserir(criarProduto("SKU-3", 30, setor = "Setor B"))
        assertEquals(2, repository.setoresAtivos())
    }

    @Test
    fun `ultimoLote deve retornar null quando banco esta vazio`() = runTest {
        assertNull(repository.ultimoLote())
    }

    @Test
    fun `ultimoLote deve retornar o movimento com maior timestamp`() = runTest {
        repository.inserir(criarProduto("SKU-OLD", 5,  ts = 1000L))
        repository.inserir(criarProduto("SKU-NEW", 99, ts = 9000L))
        assertEquals("SKU-NEW", repository.ultimoLote()?.sku)
    }

    @Test
    fun `limparInventario deve remover todos os movimentos`() = runTest {
        repository.inserir(criarProduto("SKU-X", 100))
        repository.inserir(criarProduto("SKU-Y", 200))
        repository.limparInventario()
        assertEquals(0, repository.totalLotes())
        assertEquals(0, repository.totalItens())
    }

    @Test
    fun `saldosPorSkuAtual deve retornar saldo correto agrupado`() = runTest {
        repository.inserir(criarProduto("SKU-A", 200, tipo = "ENTRADA"))
        repository.inserir(criarProduto("SKU-A",  50, tipo = "SAIDA"))
        val saldos = repository.saldosPorSkuAtual()
        assertEquals(1, saldos.size)
        assertEquals(150, saldos.first().saldo)
        assertEquals(200, saldos.first().entradas)
        assertEquals(50,  saldos.first().saidas)
    }

    @Test
    fun `todosProdutos deve emitir lista atualizada apos insercao`() = runTest {
        repository.inserir(criarProduto("SKU-LIVE-A", 10))
        repository.inserir(criarProduto("SKU-LIVE-B", 20))
        val lista = observarUmaVez(repository.todosProdutos)
        assertEquals(2, lista.size)
        assertTrue(lista.any { it.sku == "SKU-LIVE-A" })
        assertTrue(lista.any { it.sku == "SKU-LIVE-B" })
    }

    @Test
    fun `salvarTermoCustom deve persistir novo termo`() = runTest {
        repository.salvarTermoCustom("SETOR", "MEZANINO")
        val termos = observarUmaVez(repository.termosSetor)
        assertEquals(listOf("MEZANINO"), termos)
    }

    @Test
    fun `salvarTermoCustom deve ignorar duplicatas silenciosamente`() = runTest {
        repository.salvarTermoCustom("SETOR", "PATIO")
        repository.salvarTermoCustom("SETOR", "PATIO")
        val termos = observarUmaVez(repository.termosSetor)
        assertEquals(1, termos.size)
    }

    @Test
    fun `salvarTermoCustom deve ignorar valor em branco`() = runTest {
        repository.salvarTermoCustom("SETOR", "")
        val termos = observarUmaVez(repository.termosSetor)
        assertTrue(termos.isEmpty())
    }

    private fun criarProduto(
        sku: String,
        qty: Int,
        setor: String = "Setor A",
        ts: Long = System.currentTimeMillis(),
        tipo: String = "ENTRADA"
    ) = ProdutoEntity(sku = sku, qty = qty, setor = setor, bairro = "", ts = ts, tipo = tipo)

    private fun <T> observarUmaVez(liveData: LiveData<T>): T {
        var resultado: T? = null
        val trava = CountDownLatch(1)
        val observador = Observer<T> { valor ->
            resultado = valor
            trava.countDown()
        }
        val instr = InstrumentationRegistry.getInstrumentation()
        instr.runOnMainSync { liveData.observeForever(observador) }
        trava.await(2, TimeUnit.SECONDS)
        instr.runOnMainSync { liveData.removeObserver(observador) }
        @Suppress("UNCHECKED_CAST")
        return resultado as T
    }
}
