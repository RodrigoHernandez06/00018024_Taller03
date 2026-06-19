package com.example.testeableapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias de [RestaurantViewModel].
 *
 * Nota técnica: los StateFlow expuestos por el ViewModel se construyen con
 * `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ...)`. Esto significa
 * que el flujo de origen solo se "activa" cuando alguien lo está colectando. Por eso,
 * en cada test se lanza una corutina que colecta el flujo correspondiente ANTES de
 * disparar las acciones del ViewModel; de lo contrario `.value` quedaría siempre con
 * el valor inicial (vacío) y las aserciones fallarían aunque la lógica sea correcta.
 *
 * Se usa [UnconfinedTestDispatcher] como Main dispatcher para que las corutinas del
 * viewModelScope se ejecuten de forma inmediata dentro del test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RestaurantViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- 1) Agregar item al pedido ---
    @Test
    fun agregarItemAlPedido_loIncluyeEnElPedidoConCantidad1() = runTest {
        val viewModel = RestaurantViewModel()
        val job = launch { viewModel.orderedItems.collect {} }

        viewModel.addItem(1) // Patatas Bravas

        assertEquals(1, viewModel.quantities.value[1])
        assertTrue(viewModel.orderedItems.value.any { it.id == 1 })

        job.cancel()
    }

    // --- 2) Incrementar/Decrementar cantidad ---
    @Test
    fun incrementarYDecrementarCantidad_actualizanCorrectamente() = runTest {
        val viewModel = RestaurantViewModel()
        val job = launch { viewModel.quantities.collect {} }

        viewModel.addItem(1)
        viewModel.incrementItem(1)
        assertEquals(2, viewModel.quantities.value[1])

        viewModel.decrementItem(1)
        assertEquals(1, viewModel.quantities.value[1])

        job.cancel()
    }

    // --- 3) Eliminar item al decrementar desde 1 ---
    @Test
    fun decrementarDesdeUno_eliminaElItemDelPedido() = runTest {
        val viewModel = RestaurantViewModel()
        val job = launch { viewModel.quantities.collect {} }

        viewModel.addItem(2) // cantidad pasa a 1
        assertEquals(1, viewModel.quantities.value[2])

        viewModel.decrementItem(2) // debe eliminar la entrada, no dejarla en 0
        assertFalse(viewModel.quantities.value.containsKey(2))
        assertNull(viewModel.quantities.value[2])

        job.cancel()
    }

    // --- 4) Cálculo del total a pagar ---
    @Test
    fun calculoDelTotal_sumaCorrectamenteLosSubtotales() = runTest {
        val viewModel = RestaurantViewModel()
        val jobQuantities = launch { viewModel.quantities.collect {} }
        val jobTotal = launch { viewModel.total.collect {} }

        viewModel.addItem(1) // Patatas Bravas 5.50
        viewModel.addItem(5) // Agua mineral 1.50
        viewModel.incrementItem(5) // Agua x2 -> 3.00

        val totalEsperado = 5.50 + (1.50 * 2)
        assertEquals(totalEsperado, viewModel.total.value, 0.001)

        jobQuantities.cancel()
        jobTotal.cancel()
    }

    // --- Pruebas unitarias adicionales (análisis de lógica interna) ---

    /**
     * Aspecto adicional 1: el pedido debe reportarse vacío (isEmpty = true) cuando no
     * se ha agregado ningún ítem, y dejar de estarlo en cuanto se agrega el primero.
     * Es relevante porque la UI usa exactamente este flag para decidir si muestra el
     * mensaje de "pedido vacío" o la lista de ítems/total/botón de pedido.
     */
    @Test
    fun isEmpty_reflejaCorrectamenteElEstadoDelPedido() = runTest {
        val viewModel = RestaurantViewModel()
        val job = launch { viewModel.isEmpty.collect {} }

        assertTrue(viewModel.isEmpty.value)

        viewModel.addItem(3)
        assertFalse(viewModel.isEmpty.value)

        job.cancel()
    }

    /**
     * Aspecto adicional 2: al confirmar el pedido (placeOrder) se debe generar una
     * confirmación con el conteo total de artículos (sumando cantidades, no solo
     * tipos de producto distintos) y el total correcto; y al cerrarla
     * (dismissConfirmation) el pedido debe reiniciarse por completo. Es relevante
     * porque es la lógica de "checkout" de la app y un error aquí afecta directamente
     * lo que se le comunica al usuario final.
     */
    @Test
    fun placeOrderYDismissConfirmation_generanYReinicianElPedidoCorrectamente() = runTest {
        val viewModel = RestaurantViewModel()
        val jobOrdered = launch { viewModel.orderedItems.collect {} }
        val jobTotal = launch { viewModel.total.collect {} }
        val jobConfirmation = launch { viewModel.confirmation.collect {} }

        viewModel.addItem(1)
        viewModel.incrementItem(1) // 2 unidades de Patatas Bravas (5.50 c/u)

        viewModel.placeOrder()

        val confirmation = viewModel.confirmation.value
        assertNotNull(confirmation)
        assertEquals(2, confirmation!!.itemCount)
        assertEquals(11.00, confirmation.total, 0.001)

        viewModel.dismissConfirmation()

        assertNull(viewModel.confirmation.value)
        assertTrue(viewModel.quantities.value.isEmpty())

        jobOrdered.cancel()
        jobTotal.cancel()
        jobConfirmation.cancel()
    }
}
