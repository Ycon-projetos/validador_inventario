package com.ycon.validadorinventario

import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ycon.validadorinventario.data.dao.TermoPersonalizadoDao
import com.ycon.validadorinventario.data.db.AppDatabase
import com.ycon.validadorinventario.data.entity.TermoPersonalizadoEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TermoPersonalizadoDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TermoPersonalizadoDao

    @Before
    fun criarBanco() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = db.termoPersonalizadoDao()
    }

    @After
    fun fecharBanco() {
        db.close()
    }

    @Test
    fun `inserir termo deve persistir valor corretamente`() = runTest {
        dao.inserir(TermoPersonalizadoEntity(categoria = "SETOR", valor = "PATIO EXTERNO"))

        val termos = observarUmaVez(dao.observar("SETOR"))
        assertEquals(1, termos.size)
        assertEquals("PATIO EXTERNO", termos.first())
    }

    @Test
    fun `inserir termo duplicado deve ser ignorado`() = runTest {
        dao.inserir(TermoPersonalizadoEntity(categoria = "SETOR", valor = "MEZANINO"))
        dao.inserir(TermoPersonalizadoEntity(categoria = "SETOR", valor = "MEZANINO"))

        val termos = observarUmaVez(dao.observar("SETOR"))
        assertEquals(1, termos.size) // IGNORE descarta a duplicata sem erro
    }

    @Test
    fun `observar deve retornar apenas termos da categoria solicitada`() = runTest {
        dao.inserir(TermoPersonalizadoEntity(categoria = "SETOR",  valor = "PATIO"))
        dao.inserir(TermoPersonalizadoEntity(categoria = "OUTRO",  valor = "VALOR X"))
        dao.inserir(TermoPersonalizadoEntity(categoria = "OUTRO",  valor = "VALOR Y"))

        val setores = observarUmaVez(dao.observar("SETOR"))
        val outros  = observarUmaVez(dao.observar("OUTRO"))

        assertEquals(1, setores.size)
        assertEquals(2, outros.size)
        assertTrue(outros.contains("VALOR X"))
        assertTrue(outros.contains("VALOR Y"))
    }

    @Test
    fun `observar deve retornar lista vazia quando categoria nao tem termos`() = runTest {
        val termos = observarUmaVez(dao.observar("SETOR"))
        assertTrue(termos.isEmpty())
    }

    @Test
    fun `observar deve retornar termos em ordem alfabetica`() = runTest {
        dao.inserir(TermoPersonalizadoEntity(categoria = "SETOR", valor = "ZEBRA"))
        dao.inserir(TermoPersonalizadoEntity(categoria = "SETOR", valor = "ALPHA"))
        dao.inserir(TermoPersonalizadoEntity(categoria = "SETOR", valor = "MEDIO"))

        val termos = observarUmaVez(dao.observar("SETOR"))
        assertEquals(listOf("ALPHA", "MEDIO", "ZEBRA"), termos)
    }

    // Coleta o primeiro valor emitido pelo LiveData de forma síncrona (necessário em testes instrumentados)
    private fun <T> observarUmaVez(liveData: androidx.lifecycle.LiveData<T>): T {
        var resultado: T? = null
        val trava = CountDownLatch(1)
        val observador = Observer<T> { valor ->
            resultado = valor
            trava.countDown()
        }
        liveData.observeForever(observador)
        trava.await(2, TimeUnit.SECONDS)
        liveData.removeObserver(observador)
        @Suppress("UNCHECKED_CAST")
        return resultado as T
    }
}
