package org.ametro.render.clipping

import android.graphics.Rect
import android.graphics.RectF
import org.ametro.render.elements.DrawingElement

class ClippingTree(bounds: Rect, elements: List<DrawingElement>) {
    private val rootNode: ClippingTreeNode = ClippingTreeNode(bounds)

    init {
        for (element in elements) {
            layoutElementsIntoClippingTree(rootNode, element)
        }
    }

    fun getClippedElements(v1: RectF, v2: RectF?): MutableList<DrawingElement> {
        val elements: MutableList<DrawingElement> = ArrayList()
        clipping(rootNode, toRectWithOffset(v1), v2?.let { toRectWithOffset(it) }, elements)
        return elements
    }

    private fun clipping(
        node: ClippingTreeNode,
        firstVolume: Rect,
        secondVolume: Rect?,
        elements: MutableList<DrawingElement>
    ) {
        if (!(Rect.intersects(node.volume, firstVolume) ||
                    secondVolume != null &&
                    Rect.intersects(node.volume, secondVolume))
        ) return

        for (element in node.drawingElements) {
            val box = element.boundingBox
            if (Rect.intersects(firstVolume, box)) {
                elements.add(element)
                continue
            }
            if (secondVolume != null && Rect.intersects(secondVolume, box)) {
                elements.add(element)
            }
        }
        node.leftChild?.let {
            clipping(it, firstVolume, secondVolume, elements)
        }
        node.rightChild?.let {
            clipping(it, firstVolume, secondVolume, elements)
        }
    }

    private fun layoutElementsIntoClippingTree(node: ClippingTreeNode, element: DrawingElement) {
        val leftChild = node.leftChild
        val rightChild = node.rightChild
        if (leftChild == null || rightChild == null) {
            node.drawingElements.add(element)
            return
        }

        val rect = element.boundingBox
        if (leftChild.volume.contains(rect))
            layoutElementsIntoClippingTree(leftChild, element)
        else if (rightChild.volume.contains(rect))
            layoutElementsIntoClippingTree(rightChild, element)
        else
            node.drawingElements.add(element)
    }

    private fun toRectWithOffset(rect: RectF): Rect {
        return Rect(
            (rect.left - CLIPPING_OFFSET).toInt(),
            (rect.top - CLIPPING_OFFSET).toInt(),
            (rect.right + CLIPPING_OFFSET).toInt(),
            (rect.bottom + CLIPPING_OFFSET).toInt()
        )
    }

    private class ClippingTreeNode(clippingVolume: Rect) {
        val drawingElements: MutableList<DrawingElement>
        val volume: Rect
        var leftChild: ClippingTreeNode?
        var rightChild: ClippingTreeNode?

        init {
            drawingElements = ArrayList()
            volume = clippingVolume
            val width = volume.width()
            val height = volume.height()
            if (width < CLIPPING_TREE_GRANULARITY && height < CLIPPING_TREE_GRANULARITY) {
                leftChild = null
                rightChild = null
            } else {
                val x = volume.left
                val y = volume.top
                val left = Rect(volume)
                val right = Rect(volume)
                if (width > height) {
                    val half = x + width / 2
                    left.right = half
                    right.left = half
                } else {
                    val half = y + height / 2
                    left.bottom = half
                    right.top = half
                }
                leftChild = ClippingTreeNode(left)
                rightChild = ClippingTreeNode(right)
            }
        }
    }

    companion object {
        private const val CLIPPING_OFFSET = 10
        private const val CLIPPING_TREE_GRANULARITY = 100
    }
}