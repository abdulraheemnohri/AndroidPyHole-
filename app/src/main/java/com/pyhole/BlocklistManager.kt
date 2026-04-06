package com.pyhole

import android.content.Context
import com.chaquo.python.Python

class BlocklistManager(private val context: Context) {
    fun updateBlocklists() {
        val py = Python.getInstance()
        val blocklist = py.getModule("blocklist")
        val manager = blocklist.get("BlocklistManager").call()
        manager.callAttr("update_blocklists")
    }
}
