package com.foody.app

object FoodyRules {
    // A READY order is not yet served. Only SERVED is eligible to be billed.
    fun canCheckout(orders: List<Order>): Boolean =
        orders.isNotEmpty() && orders.all { it.status == "SERVED" }

    fun total(orders: List<Order>): Int =
        orders.sumOf { order -> order.lines.sumOf { line -> line.price * line.qty } }
}
