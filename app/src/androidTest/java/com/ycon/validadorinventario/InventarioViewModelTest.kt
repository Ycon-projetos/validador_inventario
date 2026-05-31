package com.ycon.validadorinventario

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ycon.validadorinventario.data.db.AppDatabase
import com.ycon.validadorinventario.data.entity.ProdutoEntity
import com.ycon.validadorinventario.ui.InventarioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** InstantTaskExecutorRule foi removido intencionalmente: ele redireciona o executor de I/O do
 *  Room para a thread principal, o que faz o próprio Room lançar IllegalStateException.
 *  Em seu lugar, usamos runOnMainSync para garantir que setValue e viewModelScope rodem
 *  na thread correta sem interferir no dispatcher de I/O do Room. */
@RunWith(AndroidJUnit4::class)
class InventarioViewModelTest {

    private lateinit var viewModel: InventarioViewModel
    private val instr = InstrumentationRegistry.getInstrumentation()

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        runBlocking {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(app).clearAllTables()
            }
        }
        instr.runOnMainSync {
            viewModel = InventarioViewModel(app)
        }
    }

    // ── Validações síncronas ──────────────────────────────────────────────────

    @Test
    fun `registrarLote com sku em branco deve postar erro imediatamente`() {
        instr.runOnMainSync { viewModel.registrarLote(sku = "   ", qty = 10, setor = "Setor A") }
        assertEquals("SKU não pode ser vazio", viewModel.mensagem.value)
    }

    @Test
    fun `registrarLote com quantidade zero deve postar erro imediatamente`() {
        instr.runOnMainSync { viewModel.registrarLote(sku = "SKU-001", qty = 0, setor = "Setor A") }
        assertEquals("Quantidade deve ser ≥ 1", viewModel.mensagem.value)
    }

    @Test
    fun `registrarLote com setor em branco deve postar erro imediatamente`() {
        instr.runOnMainSync { viewModel.registrarLote(sku = "SKU-001", qty = 5, setor = "") }
        assertEquals("Selecione um setor", viewModel.mensagem.value)
    }

    // ── Utilitários de estado ─────────────────────────────────────────────────

    @Test
    fun `limparMensagem deve zerar LiveData de mensagem`() {
        instr.runOnMainSync { viewModel.registrarLote(sku = "", qty = 1, setor = "A") }
        assertNotNull(viewModel.mensagem.value)
        instr.runOnMainSync { viewModel.limparMensagem() }
        assertNull(viewModel.mensagem.value)
    }

    @Test
    fun `limparRegistroSucesso deve manter null quando nao ha sucesso pendente`() {
        assertNull(viewModel.registroSucesso.value)
        instr.runOnMainSync { viewModel.limparRegistroSucesso() }
        assertNull(viewModel.registroSucesso.value)
    }

    // ── Fluxos assíncronos ────────────────────────────────────────────────────

    @Test
    fun `registrarLote valido deve sinalizar registroSucesso`() {
        val latch = CountDownLatch(1)
        instr.runOnMainSync {
            viewModel.registroSucesso.observeForever { sucesso ->
                if (sucesso == true) latch.countDown()
            }
        }
        instr.runOnMainSync { viewModel.registrarLote("SKU-OK", 10, "Setor A", "ENTRADA") }
        assertTrue("Timeout: registroSucesso não foi sinalizado", latch.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun `filtrar deve restringir produtosFiltrados ao sku informado`() {
        // Insere dois SKUs distintos diretamente no banco (mesmo singleton do ViewModel)
        val dao = AppDatabase.getInstance(
            ApplicationProvider.getApplicationContext<Application>()
        ).produtoDao()
        runBlocking(Dispatchers.IO) {
            dao.inserirProduto(ProdutoEntity(sku = "ALVO-001", qty = 10, setor = "A", bairro = ""))
            dao.inserirProduto(ProdutoEntity(sku = "OUTRO-002", qty = 20, setor = "B", bairro = ""))
        }

        // Aguarda Room emitir os 2 produtos via LiveData
        val latch = CountDownLatch(1)
        instr.runOnMainSync {
            viewModel.produtosFiltrados.observeForever { lista ->
                if (lista.size >= 2) latch.countDown()
            }
        }
        assertTrue("Timeout aguardando produtos iniciais", latch.await(5, TimeUnit.SECONDS))

        // Aplica o filtro na main thread — MediatorLiveData atualiza sincronamente
        instr.runOnMainSync { viewModel.filtrar("ALVO") }

        val filtrada = viewModel.produtosFiltrados.value ?: emptyList()
        assertEquals(1, filtrada.size)
        assertEquals("ALVO-001", filtrada.first().sku)
    }

    @Test
    fun `limparInventario deve zerar totalItens e postar mensagem`() {
        // Registrar produto primeiro
        val latchEntrada = CountDownLatch(1)
        instr.runOnMainSync {
            viewModel.registroSucesso.observeForever { if (it == true) latchEntrada.countDown() }
        }
        instr.runOnMainSync { viewModel.registrarLote("SKU-LIMPAR", 100, "Setor A", "ENTRADA") }
        assertTrue("Timeout registrando produto", latchEntrada.await(5, TimeUnit.SECONDS))

        // Limpar inventário
        val latchLimpar = CountDownLatch(1)
        instr.runOnMainSync {
            viewModel.mensagem.observeForever { msg ->
                if (msg?.contains("limpo") == true) latchLimpar.countDown()
            }
        }
        instr.runOnMainSync { viewModel.limparInventario() }
        assertTrue("Timeout esperando limpeza", latchLimpar.await(5, TimeUnit.SECONDS))

        assertEquals(0, viewModel.totalItens.value)
        assertEquals(0, viewModel.totalLotes.value)
    }

    @Test
    fun `saida sem estoque previo deve postar mensagem de erro de saldo`() {
        val latch = CountDownLatch(1)
        instr.runOnMainSync {
            viewModel.mensagem.observeForever { msg ->
                if (msg != null && ("sem estoque" in msg || "Saldo insuficiente" in msg)) latch.countDown()
            }
        }
        instr.runOnMainSync { viewModel.registrarLote("SKU-VAZIO", 10, "Setor A", "SAIDA") }
        assertTrue("Timeout: erro de saldo não foi postado", latch.await(5, TimeUnit.SECONDS))
    }
}
