package org.ametro.render

import android.graphics.Bitmap
import android.graphics.Picture
import android.graphics.Rect
import android.graphics.RectF
import org.ametro.model.MapContainer
import org.ametro.model.entities.MapScheme
import org.ametro.model.entities.MapSchemeLine
import org.ametro.model.entities.MapSchemeTransfer
import org.ametro.render.clipping.ClippingTree
import org.ametro.render.elements.*
import java.util.*

class RenderProgram(container: MapContainer, schemeName: String) {
    val allDrawingElements: MutableList<DrawingElement>
    private val clippingTree: ClippingTree

    init {
        allDrawingElements = createElementsTree(container, schemeName)
        highlightsElements(null)
        clippingTree = ClippingTree(getBoundingBox(allDrawingElements), allDrawingElements)
    }

    fun highlightsElements(ids: HashSet<Int>?) {
        for (element in allDrawingElements) {
            if (ids == null) {
                element.layer = RenderConstants.LAYER_VISIBLE
                continue
            }
            element.layer =
                if (element.uid != null && ids.contains(element.uid)) RenderConstants.LAYER_VISIBLE
                else RenderConstants.LAYER_GRAYED
        }
        allDrawingElements.sort()
    }

    fun getClippedDrawingElements(viewport: RectF): List<DrawingElement> {
        return getClippedDrawingElements(viewport, null)
    }

    fun getClippedDrawingElements(viewport1: RectF, viewport2: RectF?): MutableList<DrawingElement> {
        val clippedElements = clippingTree.getClippedElements(viewport1, viewport2)
        clippedElements.sort()
        return clippedElements
    }

    private fun createElementsTree(container: MapContainer, schemeName: String): MutableList<DrawingElement> {
        val elements: MutableList<DrawingElement> = ArrayList()
        val scheme = container.getScheme(schemeName)
        for (line in scheme.lines) {
            createLine(elements, scheme, line)
        }
        for (transfer in scheme.transfers) {
            createTransfer(elements, scheme, transfer)
        }
        for (imageName in scheme.imageNames) {
            val background = scheme.getBackgroundObject(imageName)
            if (background is Picture) {
                elements.add(PictureBackgroundElement(scheme, background))
            } else if (background is Bitmap) {
                elements.add(BitmapBackgroundElement(scheme, background))
            }
        }
        return elements
    }

    private fun createLine(elements: MutableList<DrawingElement>, scheme: MapScheme, line: MapSchemeLine) {
        if (line.segments != null) {
            for (segment in line.segments) {
                elements.add(SegmentElement(line, segment))
            }
        }
        if (line.stations != null) {
            for (station in line.stations) {
                if (station.position != null) {
                    elements.add(StationElement(scheme, line, station))
                }
                if (station.labelPosition != null) {
                    elements.add(StationNameElement(scheme, line, station))
                }
            }
        }
    }

    private fun createTransfer(elements: MutableList<DrawingElement>, scheme: MapScheme, transfer: MapSchemeTransfer) {
        if (transfer.fromStationPosition == null || transfer.toStationPosition == null) {
            return
        }
        elements.add(TransferElement(scheme, transfer))
        elements.add(TransferBackgroundElement(scheme, transfer))
    }

    companion object {
        private fun getBoundingBox(elements: Collection<DrawingElement>): Rect {
            var bounds: Rect? = null
            for (element in elements) {
                if (bounds == null) {
                    bounds = element.boundingBox
                    continue
                }
                bounds.union(element.boundingBox)
            }
            return bounds ?: throw Exception("No drawing elements")
        }
    }
}