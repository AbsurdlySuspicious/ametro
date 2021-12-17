package org.ametro.routes.algorithms

import java.util.*

object DijkstraHeap {
    const val INF = Long.MAX_VALUE / 10
    const val NO_WAY = -1

    fun dijkstra(graph: TransportGraph, start: Int): Result {
        val distances = LongArray(graph.count) { INF }
        val predecessors = IntArray(graph.count) { NO_WAY }
        val queue: Queue<QueueItem> = PriorityQueue()

        distances[start] = 0
        queue.add(QueueItem(0, start))

        while (!queue.isEmpty()) {
            val currentItem = queue.poll()!!
            if (currentItem.priority != distances[currentItem.value])
                continue

            for (edge in graph.edges[currentItem.value]) {
                if (edge.used) continue

                val distance = distances[currentItem.value] + edge.weight
                if (distances[edge.end] > distance) {
                    distances[edge.end] = distance
                    predecessors[edge.end] = edge.start
                    queue.add(QueueItem(distance, edge.end))
                }
            }
        }

        return Result(distances, predecessors)
    }

    data class Edge(val start: Int, val end: Int, val weight: Int,
               val transfer: Boolean, var used: Boolean = false)
    class EdgeList : ArrayList<Edge>()

    class Result(val distances: LongArray, val predecessors: IntArray) {
        fun same(other: Result?): Boolean {
            if (other == null) return false
            if (!distances.contentEquals(other.distances)) return false
            if (!predecessors.contentEquals(other.predecessors)) return false
            return true
        }
    }

    class TransportGraph(val count: Int) {
        val edges: Array<EdgeList> = Array(count) { EdgeList() }

        fun addEdge(start: Int, end: Int, weight: Int, transfer: Boolean) {
            edges[start].add(Edge(start, end, weight, transfer))
        }
    }

    class QueueItem(var priority: Long, var value: Int) : Comparable<QueueItem> {
        override fun compareTo(other: QueueItem): Int {
            return if (priority < other.priority) NO_WAY else if (priority > other.priority) 1 else 0
        }
    }
}