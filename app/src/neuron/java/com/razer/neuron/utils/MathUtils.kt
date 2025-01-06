package com.razer.neuron.utils
import java.util.concurrent.ThreadLocalRandom

/**
 * [min] (inclusive)
 * [max] (inclusive)
 */
fun randomInt(min: Int, max: Int): Int {
    return ThreadLocalRandom.current().nextInt(min, max + 1)
}