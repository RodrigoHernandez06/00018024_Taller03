package com.example.testeableapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.testeableapp.model.MenuData
import org.junit.Rule
import org.junit.Test

class RestaurantOrderTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun test_MensajePedidoVacioAlInicio() {
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("emptyOrderMessage").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("emptyOrderMessage").performScrollTo()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("emptyOrderMessage").assertIsDisplayed()
        composeTestRule.onNodeWithTag("emptyOrderMessage").assertTextContains("vacío", substring = true)
    }

    @Test
    fun test_TodosLosItemsDelMenuVisibles() {
        MenuData.items.forEach { item ->
            composeTestRule.onNodeWithTag("menuItem_${item.id}")
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun test_AgregarPedido() {
        composeTestRule.onNodeWithTag("addButton_1").performScrollTo().performClick()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("orderItem_1").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("orderItem_1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("orderItemName_1").assertTextEquals("Patatas Bravas")
    }

    @Test
    fun test_IncrementarDisminuirCantidad() {
        composeTestRule.onNodeWithTag("addButton_1").performScrollTo().performClick()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("incrementOrderItem_1").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("incrementOrderItem_1").performScrollTo().performClick()
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("orderItemQuantity_1").assertTextEquals("2")
                true
            } catch (e: AssertionError) { false }
        }

        composeTestRule.onNodeWithTag("decrementOrderItem_1").performScrollTo().performClick()
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("orderItemQuantity_1").assertTextEquals("1")
                true
            } catch (e: AssertionError) { false }
        }
    }

    @Test
    fun test_EliminarItemAlDecrementarDesde1() {
        composeTestRule.onNodeWithTag("addButton_1").performScrollTo().performClick()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("decrementOrderItem_1").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("decrementOrderItem_1").performScrollTo().performClick()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("orderItem_1").fetchSemanticsNodes().isEmpty()
        }

        composeTestRule.onNodeWithTag("emptyOrderMessage").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test_TotalGeneralSeActualiza() {
        composeTestRule.onNodeWithTag("addButton_1").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("addButton_5").performScrollTo().performClick()
        val totalEsperado1 = "%.2f €".format(5.50 + 1.50)
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("totalValue").performScrollTo().assertTextEquals(totalEsperado1)
                true
            } catch (e: AssertionError) { false }
        }
        composeTestRule.onNodeWithTag("incrementOrderItem_5").performScrollTo().performClick()

        val totalEsperado2 = "%.2f €".format(5.50 + 1.50 * 2)
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("totalValue").performScrollTo().assertTextEquals(totalEsperado2)
                true
            } catch (e: AssertionError) { false }
        }
    }

    @Test
    fun test_CalculoTotalAlPagar() {
        composeTestRule.onNodeWithTag("addButton_1").performScrollTo().performClick()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("placeOrderButton").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("placeOrderButton").performScrollTo().performClick()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("confirmationDialog").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("confirmationDialog").assertIsDisplayed()

        val mensajeEsperado =
            "¡Pedido de 1 artículos por un total de %.2f € recibido! Preparen los fogones.".format(5.50)

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("confirmationMessage").assertTextEquals(mensajeEsperado)
                true
            } catch (e: AssertionError) { false }
        }
    }

    @Test
    fun test_UI_CategoriasVisibles() {
        MenuData.categories.forEach { category ->
            composeTestRule.onNodeWithTag("categoryTitle_$category")
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun test_UI_CerrarConfirmacionReiniciaApp() {
        composeTestRule.onNodeWithTag("addButton_1").performScrollTo().performClick()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("placeOrderButton").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("placeOrderButton").performScrollTo().performClick()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("confirmationOkButton").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("confirmationOkButton").performClick()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("confirmationDialog").fetchSemanticsNodes().isEmpty()
        }

        composeTestRule.onNodeWithTag("emptyOrderMessage").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test_FUNC_SubtotalesCorrectos() {
        composeTestRule.onNodeWithTag("addButton_2").performScrollTo().performClick() // Croquetas 6.00

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("incrementOrderItem_2").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("incrementOrderItem_2").performScrollTo().performClick() // x2 = 12.00

        val subtotalEsperado = "%.2f €".format(12.00)
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("orderItemSubtotal_2").performScrollTo().assertTextEquals(subtotalEsperado)
                true
            } catch (e: AssertionError) { false }
        }
    }

    @Test
    fun test_FUNC_BotonPedidoNoVisibleSiVacio() {
        composeTestRule.onNodeWithTag("placeOrderButton").assertDoesNotExist()

        composeTestRule.onNodeWithTag("addButton_1").performScrollTo().performClick()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("placeOrderButton").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("placeOrderButton").performScrollTo().assertIsDisplayed()
    }
}
