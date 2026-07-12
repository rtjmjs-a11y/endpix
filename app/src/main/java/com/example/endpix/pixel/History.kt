package com.example.endpix.pixel

import java.util.ArrayDeque

class History(private val maxSize: Int = 60) {
    private val undoStack = ArrayDeque<IntArray>()
    private val redoStack = ArrayDeque<IntArray>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun push(snapshot: IntArray) {
        if (undoStack.size >= maxSize) {
            undoStack.pollFirst()
        }
        undoStack.push(snapshot)
        redoStack.clear()
    }

    fun undo(current: IntArray): IntArray? {
        if (undoStack.isEmpty()) return null
        redoStack.push(current)
        return undoStack.pop()
    }

    fun redo(current: IntArray): IntArray? {
        if (redoStack.isEmpty()) return null
        undoStack.push(current)
        return redoStack.pop()
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
