package com.rootachieve.koraph.graphvisualizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GraphModelTest {

    @Test
    fun buildGraphModel_includesNodesFromAdjacencyAndNodeInfo() {
        // given
        val adjacency = mapOf(
            "gateway" to listOf("search", "alerts"),
            "search" to listOf("users"),
        )
        val nodeInfo = mapOf(
            "gateway" to NodeInfo(name = "Gateway", size = 24f),
            "archive" to NodeInfo(name = "Archive", size = 15f),
        )

        // when
        val model = buildGraphModel(
            adjacency = adjacency,
            nodeInfo = nodeInfo,
        )

        // then
        assertEquals(5, model.nodeCount)
        assertTrue(model.nodeKeys.contains("archive"))
        assertEquals(3, model.edges.size)
        assertTrue(model.edges.any { it.from == "gateway" && it.to == "alerts" })
    }

    @Test
    fun buildRenderableEdges_undirectedModeDeduplicatesPairs() {
        // given
        val edges = listOf(
            GraphEdge(fromId = 0, toId = 1, from = "a", to = "b"),
            GraphEdge(fromId = 1, toId = 0, from = "b", to = "a"),
            GraphEdge(fromId = 1, toId = 2, from = "b", to = "c"),
        )

        // when
        val renderable = buildRenderableEdges(
            edges = edges,
            directed = false,
            showArrows = true,
        )

        // then
        assertEquals(2, renderable.size)
        assertTrue(renderable.all { !it.drawArrow })
        assertTrue(renderable.any { it.fromId == 0 && it.toId == 1 })
        assertTrue(renderable.any { it.fromId == 1 && it.toId == 2 })
    }

    @Test
    fun buildRenderableEdges_undirectedMode_alignsKeysWithCanonicalIds() {
        // given
        val edges = listOf(
            GraphEdge(fromId = 3, toId = 1, from = "payments", to = "auth"),
        )

        // when
        val renderable = buildRenderableEdges(
            edges = edges,
            directed = false,
            showArrows = true,
        )

        // then
        assertEquals(1, renderable.size)
        assertEquals(1, renderable[0].fromId)
        assertEquals(3, renderable[0].toId)
        assertEquals("auth", renderable[0].from)
        assertEquals("payments", renderable[0].to)
    }

    @Test
    fun buildRenderableEdges_directedModeRespectsArrowOption() {
        // given
        val edges = listOf(
            GraphEdge(fromId = 0, toId = 1, from = "a", to = "b"),
            GraphEdge(fromId = 1, toId = 2, from = "b", to = "c"),
        )

        // when
        val withArrows = buildRenderableEdges(
            edges = edges,
            directed = true,
            showArrows = true,
        )
        val withoutArrows = buildRenderableEdges(
            edges = edges,
            directed = true,
            showArrows = false,
        )

        // then
        assertTrue(withArrows.all { it.drawArrow })
        assertFalse(withoutArrows.any { it.drawArrow })
    }

    @Test
    fun adjacencySignature_changesWhenAdjacencyOrNodeInfoChanges() {
        // given
        val adjacency = mutableMapOf(
            "a" to listOf("b"),
        )
        val nodeInfo = mutableMapOf(
            "a" to NodeInfo(name = "A"),
        )

        // when
        val first = adjacencySignature(adjacency, nodeInfo)
        adjacency["b"] = listOf("a")
        val second = adjacencySignature(adjacency, nodeInfo)
        nodeInfo["b"] = NodeInfo(name = "B")
        val third = adjacencySignature(adjacency, nodeInfo)

        // then
        assertNotEquals(first, second)
        assertNotEquals(second, third)
    }

    @Test
    fun buildGraphModel_usesFallbackNodeInfoForMissingEntries() {
        // given
        val adjacency = mapOf(
            "gateway" to listOf("search"),
        )
        val nodeInfo = mapOf(
            "gateway" to NodeInfo(name = "Gateway"),
        )
        val fallbackNodeInfo: (String) -> NodeInfo = { key ->
            NodeInfo(
                name = "Auto-$key",
                size = 22f,
            )
        }

        // when
        val model = buildGraphModel(
            adjacency = adjacency,
            nodeInfo = nodeInfo,
            fallbackNodeInfo = fallbackNodeInfo,
        )

        // then
        assertEquals("Gateway", model.nodeInfoByKey.getValue("gateway").name)
        assertEquals("Auto-search", model.nodeInfoByKey.getValue("search").name)
        assertEquals(22f, model.nodeInfoByKey.getValue("search").size)
    }
}
