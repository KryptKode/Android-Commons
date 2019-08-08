package com.kryptkode.common.helpers

/**
Created by kryptkode on 8/8/2019
 */
class CallbackUtils {

    fun getFirstIndexOf(list:List<String>, key:Int): Int {
        return list.indexOfFirst { it.hashCode() == key }
    }

    fun getSelectedItems(linkedHashSet: LinkedHashSet<Int>, function:(Int) -> String): List<String> {
        return linkedHashSet.map { function(it) }
    }

    fun getItemWithKey(list:List<String>, key:Int): String? {
        return list.firstOrNull{it.hashCode() == key}
    }
}