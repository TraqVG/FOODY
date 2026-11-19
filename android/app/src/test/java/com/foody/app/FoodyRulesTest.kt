package com.foody.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodyRulesTest {
    private fun order(id:Int,status:String,price:Int=69,qty:Int=1) =
        Order(id,1,"staff@foody.shop",1000L,listOf(Line(1,"ข้าวกะเพราหมูสับ",price,qty)),status)

    @Test fun emptyTableCannotCheckout() {
        assertFalse(FoodyRules.canCheckout(emptyList()))
    }
    @Test fun pendingAndReadyAreNotServed() {
        assertFalse(FoodyRules.canCheckout(listOf(order(1,"PENDING"))))
        assertFalse(FoodyRules.canCheckout(listOf(order(1,"READY"))))
        assertFalse(FoodyRules.canCheckout(listOf(order(1,"SERVED"),order(2,"READY"))))
    }
    @Test fun allServedMayCheckout() {
        assertTrue(FoodyRules.canCheckout(listOf(order(1,"SERVED"),order(2,"SERVED"))))
    }
    @Test fun totalPreservesOrderSnapshotPrices() {
        assertEquals(138,FoodyRules.total(listOf(order(1,"SERVED",69,2))))
    }
}
