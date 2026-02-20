# Koraph

![Image](https://github.com/user-attachments/assets/a33957e1-f4bc-429e-a9ff-c58f99fe1187)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.rootachieve/Koraph)](https://central.sonatype.com/artifact/io.github.rootachieve/Koraph)

> Koraph is a Compose Multiplatform graph visualization library for turning adjacency maps into interactive node-link diagrams.

Koraph is a Compose Multiplatform project that includes:

- `composeApp`: demo app for Android, iOS, JS, and Wasm
- `graph-visualizer`: reusable graph visualization library (`Map<Key, List<Key>>` input)

## Sample

You can try the web sample app at:
[https://rootachieve.github.io/Koraph/](https://rootachieve.github.io/Koraph/)

## Installation

### Prerequisites

- JDK 17+
- Android Studio (or IntelliJ IDEA with Kotlin/Compose support)
- Xcode (for iOS target)

### Gradle

Koraph releases are published to Maven Central:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.rootachieve:Koraph:<version>")
}
```
### Build

```bash
./gradlew :graph-visualizer:allTests
./gradlew :composeApp:assembleDebug
```

## Basic Usage

Use `SimpleGraphVisualizer` for a quick start without manually creating full node metadata.

```kotlin
enum class NodeKey { Gateway, Search, Users }

val adjacency = mapOf(
    NodeKey.Gateway to listOf(NodeKey.Search, NodeKey.Users),
    NodeKey.Search to listOf(NodeKey.Users),
)

SimpleGraphVisualizer(
    adjacency = adjacency,
    onSelectionChange = { selected ->
        println("Selected node: ${selected?.name ?: "none"}")
    },
)
```

You can also apply lightweight per-node customization with `nodeInfoFactory`.

```kotlin
SimpleGraphVisualizer(
    adjacency = adjacency,
    nodeInfoFactory = { key ->
        NodeInfo(
            name = key.name,
            size = if (key == NodeKey.Gateway) 24f else 18f,
        )
    },
)
```

## Advanced Usage

For full control, provide `nodeInfo`, tuned options, and custom style rules.

```kotlin
enum class NodeKey { Gateway, Search, Users, Alerts, Custom }

val adjacency: Map<NodeKey, List<NodeKey>> = mapOf(
    NodeKey.Gateway to listOf(NodeKey.Search, NodeKey.Alerts),
    NodeKey.Search to listOf(NodeKey.Users),
    NodeKey.Users to listOf(NodeKey.Gateway),
)

val nodeInfo: Map<NodeKey, NodeInfo> = mapOf(
    NodeKey.Gateway to NodeInfo(name = "Gateway", style = NodeShape.RoundedRect, size = 24f),
    NodeKey.Search to NodeInfo(name = "Search", style = NodeShape.Circle, size = 18f),
    NodeKey.Users to NodeInfo(name = "Users", style = NodeShape.Hexagon, size = 20f),
    NodeKey.Alerts to NodeInfo(name = "Alerts", style = NodeShape.Diamond, size = 22f),
)

GraphVisualizer(
    adjacency = adjacency,
    nodeInfo = nodeInfo,
    options = GraphVisualizerOptions.presentation().copy(
        interaction = GraphInteractionConfig(
            minScale = 0.4f,
            maxScale = 6f,
            tapSelectionPadding = 12f,
            clearSelectionOnBackgroundTap = false,
        ),
        label = GraphLabelConfig(widthDp = 120f, fontSizeSp = 13f),
        layout = ForceLayoutConfig(centerTension = 0.035f),
    ),
    nodeStyle = { input ->
        val info = input.nodeInfo
        NodeStyle(
            shape = info.style,
            fillColor = when (input.selectionState) {
                SelectionState.Selected -> info.selectedColor
                SelectionState.OtherSelected -> Color(0xFFD1D5DB)
                SelectionState.NoneSelected -> info.color
            },
            strokeColor = when (input.selectionState) {
                SelectionState.Selected -> info.selectedStrokeColor
                SelectionState.OtherSelected -> Color(0xFF6B7280)
                SelectionState.NoneSelected -> info.strokeColor
            },
            radius = info.size,
            labelColor = info.labelColor,
        )
    },
)
```

More detailed advanced usage documentation will be added in a future docs update.

## License

Licensed under Apache-2.0. See [LICENSE](LICENSE).
