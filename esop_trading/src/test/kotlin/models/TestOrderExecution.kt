package models

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import services.saveUser
import kotlin.math.roundToLong
class TestOrderExecution {
    @BeforeEach
    fun setup(){
        val buyer = User("jake", "Jake", "Peralta", "9844427549", "jake@gmail.com") //Buyer
        buyer.account.wallet.addMoneyToWallet(10000)
        val seller = User("amy", "Amy", "Santiago", "9472919384", "amy@gmail.com") //Seller
        seller.account.inventory.addEsopToInventory(100, "NON-PERFORMANCE")
        seller.account.inventory.addEsopToInventory(100, "PERFORMANCE")

        saveUser(buyer)
        saveUser(seller)
    }

    @AfterEach
    fun tearDown(){
        DataStorage.userList.clear()
        DataStorage.registeredEmails.clear()
        DataStorage.registeredPhoneNumbers.clear()
        DataStorage.buyList.clear()
        DataStorage.sellList.clear()
        DataStorage.performanceSellList.clear()
        DataStorage.orderId = 1
        DataStorage.orderExecutionId = 1
    }
    @Test
    fun `multiple buy orders by one user and one sell order by another user to fulfill them completely`(){
        val buyer = DataStorage.userList["jake"]!!
        val seller = DataStorage.userList["amy"]!!
        val expectedSellerWallet = (150*(1-DataStorage.COMMISSION_FEE_PERCENTAGE*0.01)).roundToLong()

        buyer.addOrder(5, "BUY", 10)
        buyer.addOrder(5, "BUY", 10)
        buyer.addOrder(5, "BUY", 10)
        seller.addOrder(15, "SELL", 10)

        assert(DataStorage.buyList.isEmpty())
        assert(DataStorage.sellList.isEmpty())
        assertEquals(9850, buyer.account.wallet.getFreeMoney())
        assertEquals(15, buyer.account.inventory.getFreeInventory())
        assertEquals(expectedSellerWallet, seller.account.wallet.getFreeMoney())
        assertEquals(85, seller.account.inventory.getFreeInventory())
    }

    @Test
    fun `should take sell price as order price when buy price is higher`(){
        val buyer = DataStorage.userList["jake"]!!
        val seller = DataStorage.userList["amy"]!!
        val expectedSellerWallet = (5*(1-DataStorage.COMMISSION_FEE_PERCENTAGE*0.01)).roundToLong()

        buyer.addOrder(1, "BUY", 10)
        seller.addOrder(1, "SELL", 5)

        assertEquals(10000 - 5, buyer.account.wallet.getFreeMoney())
        assertEquals(expectedSellerWallet, seller.account.wallet.getFreeMoney())
    }

    @Test
    fun `should prioritize sell order that has lower price`(){
        val buyer = DataStorage.userList["jake"]!!
        val seller = DataStorage.userList["amy"]!!

        seller.addOrder(1, "SELL", 10)
        seller.addOrder(1,"SELL", 5)
        buyer.addOrder(1, "BUY", 10)

        assertEquals("Unfilled", seller.orders[0].orderStatus)
        assertEquals(10, seller.orders[0].orderPrice)
        assertEquals("Filled", seller.orders[1].orderStatus)
        assertEquals(5, seller.orders[1].orderPrice)
        assertEquals("Filled", buyer.orders[0].orderStatus)
        assertEquals(10, buyer.orders[0].orderPrice)
        assertEquals(10000-5, buyer.account.wallet.getFreeMoney())
    }

    @Test
    fun `should prioritize buy order that has higher price`(){
        val buyer = DataStorage.userList["jake"]!!
        val seller = DataStorage.userList["amy"]!!

        buyer.addOrder(1,"BUY",5)
        buyer.addOrder(1,"BUY",10)
        seller.addOrder(1, "SELL", 5)

        assertEquals("Unfilled", buyer.orders[0].orderStatus)
        assertEquals(5, buyer.orders[0].orderPrice)
        assertEquals("Filled", buyer.orders[1].orderStatus)
        assertEquals(10, buyer.orders[1].orderPrice)
        assertEquals("Filled", seller.orders[0].orderStatus)
        assertEquals(5, seller.orders[0].orderPrice)
    }

    @Test
    fun `should prioritize performance ESOP sell orders over non-performance ESOP sell orders`(){
        val buyer = DataStorage.userList["jake"]!!
        val seller = DataStorage.userList["amy"]!!

        seller.addOrder(1, "SELL", 5, "NON-PERFORMANCE")
        seller.addOrder(1, "SELL", 10, "PERFORMANCE")
        buyer.addOrder(1,"BUY", 10)

        assertEquals("Unfilled", seller.orders[0].orderStatus)
        assertEquals(5, seller.orders[0].orderPrice)
        assertEquals("Filled", seller.orders[1].orderStatus)
        assertEquals(10, seller.orders[1].orderPrice)
        assertEquals("Filled", buyer.orders[0].orderStatus)
        assertEquals(10, buyer.orders[0].orderPrice)
        assertEquals(10000-10, buyer.account.wallet.getFreeMoney())
    }

    @Test
    fun `buyer should get non-performance ESOP even if seller sells performance ESOPs`(){
        val buyer = DataStorage.userList["jake"]!!
        val seller = DataStorage.userList["amy"]!!

        seller.addOrder(1, "SELL", 10, "PERFORMANCE")
        buyer.addOrder(1,"BUY", 10)

        assertEquals(0, buyer.account.inventory.getLockedPerformanceInventory())
        assertEquals(0, buyer.account.inventory.getFreePerformanceInventory())
        assertEquals(0, buyer.account.inventory.getLockedInventory())
        assertEquals(1, buyer.account.inventory.getFreeInventory())
    }

    @Test
    fun `should prioritize order that came first among multiple performance ESOP sell orders irrespective of price`(){
        val buyer = DataStorage.userList["jake"]!!
        val seller = DataStorage.userList["amy"]!!

        seller.addOrder(1, "SELL", 10, "PERFORMANCE")
        seller.addOrder(1, "SELL", 5, "PERFORMANCE")
        buyer.addOrder(1,"BUY",10)

        assertEquals(10000-10, buyer.account.wallet.getFreeMoney())
        assertEquals(0,buyer.account.wallet.getLockedMoney())
        assertEquals("Filled", seller.orders[0].orderStatus)
        assertEquals(10, seller.orders[0].orderPrice)
        assertEquals("Unfilled", seller.orders[1].orderStatus)
        assertEquals(5, seller.orders[1].orderPrice)
        assertEquals("Filled", buyer.orders[0].orderStatus)
        assertEquals(10, buyer.orders[0].orderPrice)
    }
}