package com.rootachieve.koraph

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rootachieve.koraph.graphvisualizer.ForceLayoutConfig
import com.rootachieve.koraph.graphvisualizer.GraphAnimationFlags
import com.rootachieve.koraph.graphvisualizer.GraphInteractionConfig
import com.rootachieve.koraph.graphvisualizer.GraphLabelConfig
import com.rootachieve.koraph.graphvisualizer.GraphVisualizerOptions

private object SampleInteractionDefaults {
    const val minScale: Float = 0.4f
    const val maxScale: Float = 6f
    const val tapSelectionPadding: Float = 12f
}

private object SampleLabelDefaults {
    const val widthDp: Float = 96f
    const val fontSizeSp: Float = 12f
    const val verticalPaddingDp: Float = 6f
}

private object SampleLayoutDefaults {
    const val iterations: Int = 520
    const val nodeRepulsion: Float = 1450f
    const val repulsionExponent: Float = 1.2f
    const val edgeTension: Float = 0.018f
    const val centerTension: Float = 0.04f
    const val baseEdgeLength: Float = 96f
    const val edgeDistanceScale: Float = 1f
    const val damping: Float = 0.9f
    const val convergenceThreshold: Float = 0.14f
    const val collisionPadding: Float = 14f
    const val collisionStrength: Float = 0.85f
    const val maxVelocity: Float = 11f
}

private object ControlPanelDimensions {
    val cornerRadius = 18.dp
    val panelPadding = 14.dp
    val sectionSpacing = 14.dp
    val itemSpacing = 10.dp
    val fieldSpacing = 10.dp
}

internal enum class VisualizerPreset(
    val label: String,
) {
    Sample("Sample"),
    Default("Default"),
    Performance("Performance"),
    Presentation("Presentation"),
}

internal data class VisualizerControlState(
    val preset: VisualizerPreset,
    val scaleNodeSizeByDegree: Boolean,
    val emphasizeEdgeWeight: Boolean,
    val directed: Boolean,
    val showArrows: Boolean,
    val enablePanZoom: Boolean,
    val enableTapSelection: Boolean,
    val enableNodeDrag: Boolean,
    val fitToViewport: Boolean,
    val clearSelectionOnInit: Boolean,
    val animateColors: Boolean,
    val animateLabelFade: Boolean,
    val animateInitialRender: Boolean,
    val animateLayout: Boolean,
    val viewportPadding: String,
    val colorAnimationDurationMillis: String,
    val labelFadeAnimationDurationMillis: String,
    val layoutAnimationDurationMillis: String,
    val labelFadeZoomThreshold: String,
    val initialRenderAnimationDurationMillis: String,
    val minScale: String,
    val maxScale: String,
    val tapSelectionPadding: String,
    val clearSelectionOnBackgroundTap: Boolean,
    val keepLayoutPhysicsOnNodeDrag: Boolean,
    val dragPhysicsIterationsPerStep: String,
    val labelWidthDp: String,
    val labelFontSizeSp: String,
    val labelVerticalPaddingDp: String,
    val iterations: String,
    val nodeRepulsion: String,
    val repulsionExponent: String,
    val edgeTension: String,
    val degreeAwareEdgeTension: Boolean,
    val centerTension: String,
    val baseEdgeLength: String,
    val edgeDistanceScale: String,
    val damping: String,
    val convergenceThreshold: String,
    val randomSeed: String,
    val collisionPadding: String,
    val collisionStrength: String,
    val maxVelocity: String,
)

internal fun buildVisualizerControlState(
    preset: VisualizerPreset = VisualizerPreset.Sample,
    directed: Boolean = false,
): VisualizerControlState {
    val options = presetBaseOptions(
        preset = preset,
        directed = directed,
    )

    return VisualizerControlState(
        preset = preset,
        scaleNodeSizeByDegree = false,
        emphasizeEdgeWeight = false,
        directed = options.directed,
        showArrows = options.showArrows,
        enablePanZoom = options.enablePanZoom,
        enableTapSelection = options.enableTapSelection,
        enableNodeDrag = options.enableNodeDrag,
        fitToViewport = options.fitToViewport,
        clearSelectionOnInit = options.clearSelectionOnInit,
        animateColors = options.animationFlags.hasFlag(GraphAnimationFlags.COLOR_TRANSITION),
        animateLabelFade = options.animationFlags.hasFlag(GraphAnimationFlags.LABEL_VISIBILITY_FADE),
        animateInitialRender = options.animationFlags.hasFlag(GraphAnimationFlags.INITIAL_RENDER),
        animateLayout = options.animationFlags.hasFlag(GraphAnimationFlags.LAYOUT_TRANSITION),
        viewportPadding = options.viewportPadding.toFieldValue(),
        colorAnimationDurationMillis = options.colorAnimationDurationMillis.toString(),
        labelFadeAnimationDurationMillis = options.labelFadeAnimationDurationMillis.toString(),
        layoutAnimationDurationMillis = options.layoutAnimationDurationMillis.toString(),
        labelFadeZoomThreshold = options.labelFadeZoomThreshold.toFieldValue(),
        initialRenderAnimationDurationMillis = options.initialRenderAnimationDurationMillis.toString(),
        minScale = options.interaction.minScale.toFieldValue(),
        maxScale = options.interaction.maxScale.toFieldValue(),
        tapSelectionPadding = options.interaction.tapSelectionPadding.toFieldValue(),
        clearSelectionOnBackgroundTap = options.interaction.clearSelectionOnBackgroundTap,
        keepLayoutPhysicsOnNodeDrag = options.interaction.keepLayoutPhysicsOnNodeDrag,
        dragPhysicsIterationsPerStep = options.interaction.dragPhysicsIterationsPerStep.toString(),
        labelWidthDp = options.label.widthDp.toFieldValue(),
        labelFontSizeSp = options.label.fontSizeSp.toFieldValue(),
        labelVerticalPaddingDp = options.label.verticalPaddingDp.toFieldValue(),
        iterations = options.layout.iterations.toString(),
        nodeRepulsion = options.layout.nodeRepulsion.toFieldValue(),
        repulsionExponent = options.layout.repulsionExponent.toFieldValue(),
        edgeTension = options.layout.edgeTension.toFieldValue(),
        degreeAwareEdgeTension = options.layout.degreeAwareEdgeTension,
        centerTension = options.layout.centerTension.toFieldValue(),
        baseEdgeLength = options.layout.baseEdgeLength.toFieldValue(),
        edgeDistanceScale = options.layout.edgeDistanceScale.toFieldValue(),
        damping = options.layout.damping.toFieldValue(),
        convergenceThreshold = options.layout.convergenceThreshold.toFieldValue(),
        randomSeed = options.layout.randomSeed.toString(),
        collisionPadding = options.layout.collisionPadding.toFieldValue(),
        collisionStrength = options.layout.collisionStrength.toFieldValue(),
        maxVelocity = options.layout.maxVelocity.toFieldValue(),
    )
}

internal fun VisualizerControlState.toGraphVisualizerOptions(): GraphVisualizerOptions {
    val fallback = presetBaseOptions(
        preset = preset,
        directed = directed,
    )

    return fallback.copy(
        directed = directed,
        showArrows = showArrows,
        enablePanZoom = enablePanZoom,
        enableTapSelection = enableTapSelection,
        enableNodeDrag = enableNodeDrag,
        fitToViewport = fitToViewport,
        viewportPadding = viewportPadding.toFloatOr(fallback.viewportPadding),
        clearSelectionOnInit = clearSelectionOnInit,
        animationFlags = animationFlags(),
        colorAnimationDurationMillis = colorAnimationDurationMillis.toIntOr(fallback.colorAnimationDurationMillis),
        labelFadeAnimationDurationMillis = labelFadeAnimationDurationMillis.toIntOr(
            fallback.labelFadeAnimationDurationMillis,
        ),
        layoutAnimationDurationMillis = layoutAnimationDurationMillis.toIntOr(
            fallback.layoutAnimationDurationMillis,
        ),
        labelFadeZoomThreshold = labelFadeZoomThreshold.toFloatOr(fallback.labelFadeZoomThreshold),
        initialRenderAnimationDurationMillis = initialRenderAnimationDurationMillis.toIntOr(
            fallback.initialRenderAnimationDurationMillis,
        ),
        interaction = GraphInteractionConfig(
            minScale = minScale.toFloatOr(fallback.interaction.minScale),
            maxScale = maxScale.toFloatOr(fallback.interaction.maxScale),
            tapSelectionPadding = tapSelectionPadding.toFloatOr(fallback.interaction.tapSelectionPadding),
            clearSelectionOnBackgroundTap = clearSelectionOnBackgroundTap,
            keepLayoutPhysicsOnNodeDrag = keepLayoutPhysicsOnNodeDrag,
            dragPhysicsIterationsPerStep = dragPhysicsIterationsPerStep.toIntOr(
                fallback.interaction.dragPhysicsIterationsPerStep,
            ),
        ),
        label = GraphLabelConfig(
            widthDp = labelWidthDp.toFloatOr(fallback.label.widthDp),
            fontSizeSp = labelFontSizeSp.toFloatOr(fallback.label.fontSizeSp),
            verticalPaddingDp = labelVerticalPaddingDp.toFloatOr(fallback.label.verticalPaddingDp),
        ),
        layout = ForceLayoutConfig(
            iterations = iterations.toIntOr(fallback.layout.iterations),
            nodeRepulsion = nodeRepulsion.toFloatOr(fallback.layout.nodeRepulsion),
            repulsionExponent = repulsionExponent.toFloatOr(fallback.layout.repulsionExponent),
            edgeTension = edgeTension.toFloatOr(fallback.layout.edgeTension),
            degreeAwareEdgeTension = degreeAwareEdgeTension,
            centerTension = centerTension.toFloatOr(fallback.layout.centerTension),
            baseEdgeLength = baseEdgeLength.toFloatOr(fallback.layout.baseEdgeLength),
            edgeDistanceScale = edgeDistanceScale.toFloatOr(fallback.layout.edgeDistanceScale),
            damping = damping.toFloatOr(fallback.layout.damping),
            convergenceThreshold = convergenceThreshold.toFloatOr(fallback.layout.convergenceThreshold),
            randomSeed = randomSeed.toIntOr(fallback.layout.randomSeed),
            collisionPadding = collisionPadding.toFloatOr(fallback.layout.collisionPadding),
            collisionStrength = collisionStrength.toFloatOr(fallback.layout.collisionStrength),
            maxVelocity = maxVelocity.toFloatOr(fallback.layout.maxVelocity),
        ),
    )
}

@Composable
internal fun VisualizerControlPanel(
    controls: VisualizerControlState,
    onControlsChange: (VisualizerControlState) -> Unit,
    onApplyPreset: (VisualizerPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(ControlPanelDimensions.cornerRadius),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(ControlPanelDimensions.cornerRadius),
            )
            .padding(ControlPanelDimensions.panelPadding)
            .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ControlPanelDimensions.sectionSpacing),
    ) {
        SettingsSection(title = "Engine Presets") {
            Text(
                text = "Preset defaults load into the fields below. Invalid input falls back to the selected preset.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ControlPanelDimensions.fieldSpacing),
            ) {
                PresetChip(
                    preset = VisualizerPreset.Sample,
                    currentPreset = controls.preset,
                    onApplyPreset = onApplyPreset,
                    modifier = Modifier.weight(1f),
                )
                PresetChip(
                    preset = VisualizerPreset.Default,
                    currentPreset = controls.preset,
                    onApplyPreset = onApplyPreset,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ControlPanelDimensions.fieldSpacing),
            ) {
                PresetChip(
                    preset = VisualizerPreset.Performance,
                    currentPreset = controls.preset,
                    onApplyPreset = onApplyPreset,
                    modifier = Modifier.weight(1f),
                )
                PresetChip(
                    preset = VisualizerPreset.Presentation,
                    currentPreset = controls.preset,
                    onApplyPreset = onApplyPreset,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        SettingsSection(title = "Sample Style") {
            ToggleRow(
                firstLabel = "Scale node size by degree",
                firstValue = controls.scaleNodeSizeByDegree,
                onFirstChange = {
                    onControlsChange(controls.copy(scaleNodeSizeByDegree = it))
                },
                secondLabel = "Emphasize selected edge weight",
                secondValue = controls.emphasizeEdgeWeight,
                onSecondChange = {
                    onControlsChange(controls.copy(emphasizeEdgeWeight = it))
                },
            )
        }

        SettingsSection(title = "Behavior") {
            ToggleRow(
                firstLabel = "Directed graph",
                firstValue = controls.directed,
                onFirstChange = {
                    onControlsChange(controls.copy(directed = it))
                },
                secondLabel = "Show arrows",
                secondValue = controls.showArrows,
                onSecondChange = {
                    onControlsChange(controls.copy(showArrows = it))
                },
            )
            ToggleRow(
                firstLabel = "Pan and zoom",
                firstValue = controls.enablePanZoom,
                onFirstChange = {
                    onControlsChange(controls.copy(enablePanZoom = it))
                },
                secondLabel = "Tap selection",
                secondValue = controls.enableTapSelection,
                onSecondChange = {
                    onControlsChange(controls.copy(enableTapSelection = it))
                },
            )
            ToggleRow(
                firstLabel = "Node drag",
                firstValue = controls.enableNodeDrag,
                onFirstChange = {
                    onControlsChange(controls.copy(enableNodeDrag = it))
                },
                secondLabel = "Fit to viewport",
                secondValue = controls.fitToViewport,
                onSecondChange = {
                    onControlsChange(controls.copy(fitToViewport = it))
                },
            )
            ToggleRow(
                firstLabel = "Clear selection on init",
                firstValue = controls.clearSelectionOnInit,
                onFirstChange = {
                    onControlsChange(controls.copy(clearSelectionOnInit = it))
                },
                secondLabel = "Clear selection on background tap",
                secondValue = controls.clearSelectionOnBackgroundTap,
                onSecondChange = {
                    onControlsChange(controls.copy(clearSelectionOnBackgroundTap = it))
                },
            )
            ToggleRow(
                firstLabel = "Keep physics while dragging",
                firstValue = controls.keepLayoutPhysicsOnNodeDrag,
                onFirstChange = {
                    onControlsChange(controls.copy(keepLayoutPhysicsOnNodeDrag = it))
                },
            )
        }

        SettingsSection(title = "Animation Flags") {
            ToggleRow(
                firstLabel = "Color transition",
                firstValue = controls.animateColors,
                onFirstChange = {
                    onControlsChange(controls.copy(animateColors = it))
                },
                secondLabel = "Label fade",
                secondValue = controls.animateLabelFade,
                onSecondChange = {
                    onControlsChange(controls.copy(animateLabelFade = it))
                },
            )
            ToggleRow(
                firstLabel = "Initial render",
                firstValue = controls.animateInitialRender,
                onFirstChange = {
                    onControlsChange(controls.copy(animateInitialRender = it))
                },
                secondLabel = "Layout transition",
                secondValue = controls.animateLayout,
                onSecondChange = {
                    onControlsChange(controls.copy(animateLayout = it))
                },
            )
        }

        SettingsSection(title = "Engine Values (Text)") {
            Text(
                text = "These engine values are adjusted with text input only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FieldRow(
                firstLabel = "Viewport padding",
                firstValue = controls.viewportPadding,
                onFirstChange = {
                    onControlsChange(controls.copy(viewportPadding = it))
                },
                secondLabel = "Iterations",
                secondValue = controls.iterations,
                onSecondChange = {
                    onControlsChange(controls.copy(iterations = it))
                },
            )
            FieldRow(
                firstLabel = "Node repulsion",
                firstValue = controls.nodeRepulsion,
                onFirstChange = {
                    onControlsChange(controls.copy(nodeRepulsion = it))
                },
                secondLabel = "Repulsion exponent",
                secondValue = controls.repulsionExponent,
                onSecondChange = {
                    onControlsChange(controls.copy(repulsionExponent = it))
                },
            )
            FieldRow(
                firstLabel = "Edge tension",
                firstValue = controls.edgeTension,
                onFirstChange = {
                    onControlsChange(controls.copy(edgeTension = it))
                },
                secondLabel = "Center tension",
                secondValue = controls.centerTension,
                onSecondChange = {
                    onControlsChange(controls.copy(centerTension = it))
                },
            )
            FieldRow(
                firstLabel = "Base edge length",
                firstValue = controls.baseEdgeLength,
                onFirstChange = {
                    onControlsChange(controls.copy(baseEdgeLength = it))
                },
                secondLabel = "Edge distance scale",
                secondValue = controls.edgeDistanceScale,
                onSecondChange = {
                    onControlsChange(controls.copy(edgeDistanceScale = it))
                },
            )
            FieldRow(
                firstLabel = "Damping",
                firstValue = controls.damping,
                onFirstChange = {
                    onControlsChange(controls.copy(damping = it))
                },
                secondLabel = "Convergence threshold",
                secondValue = controls.convergenceThreshold,
                onSecondChange = {
                    onControlsChange(controls.copy(convergenceThreshold = it))
                },
            )
            FieldRow(
                firstLabel = "Collision padding",
                firstValue = controls.collisionPadding,
                onFirstChange = {
                    onControlsChange(controls.copy(collisionPadding = it))
                },
                secondLabel = "Collision strength",
                secondValue = controls.collisionStrength,
                onSecondChange = {
                    onControlsChange(controls.copy(collisionStrength = it))
                },
            )
            FieldRow(
                firstLabel = "Maximum velocity",
                firstValue = controls.maxVelocity,
                onFirstChange = {
                    onControlsChange(controls.copy(maxVelocity = it))
                },
            )
        }

        SettingsSection(title = "Animation Timing") {
            FieldRow(
                firstLabel = "Label fade zoom threshold",
                firstValue = controls.labelFadeZoomThreshold,
                onFirstChange = {
                    onControlsChange(controls.copy(labelFadeZoomThreshold = it))
                },
                secondLabel = "Color duration (ms)",
                secondValue = controls.colorAnimationDurationMillis,
                onSecondChange = {
                    onControlsChange(controls.copy(colorAnimationDurationMillis = it))
                },
            )
            FieldRow(
                firstLabel = "Label fade duration (ms)",
                firstValue = controls.labelFadeAnimationDurationMillis,
                onFirstChange = {
                    onControlsChange(controls.copy(labelFadeAnimationDurationMillis = it))
                },
                secondLabel = "Layout duration (ms)",
                secondValue = controls.layoutAnimationDurationMillis,
                onSecondChange = {
                    onControlsChange(controls.copy(layoutAnimationDurationMillis = it))
                },
            )
            FieldRow(
                firstLabel = "Initial render duration (ms)",
                firstValue = controls.initialRenderAnimationDurationMillis,
                onFirstChange = {
                    onControlsChange(controls.copy(initialRenderAnimationDurationMillis = it))
                },
            )
        }

        SettingsSection(title = "Interaction") {
            FieldRow(
                firstLabel = "Minimum scale",
                firstValue = controls.minScale,
                onFirstChange = {
                    onControlsChange(controls.copy(minScale = it))
                },
                secondLabel = "Maximum scale",
                secondValue = controls.maxScale,
                onSecondChange = {
                    onControlsChange(controls.copy(maxScale = it))
                },
            )
            FieldRow(
                firstLabel = "Tap selection padding",
                firstValue = controls.tapSelectionPadding,
                onFirstChange = {
                    onControlsChange(controls.copy(tapSelectionPadding = it))
                },
                secondLabel = "Drag physics iterations",
                secondValue = controls.dragPhysicsIterationsPerStep,
                onSecondChange = {
                    onControlsChange(controls.copy(dragPhysicsIterationsPerStep = it))
                },
            )
        }

        SettingsSection(title = "Labels") {
            FieldRow(
                firstLabel = "Label width (dp)",
                firstValue = controls.labelWidthDp,
                onFirstChange = {
                    onControlsChange(controls.copy(labelWidthDp = it))
                },
                secondLabel = "Font size (sp)",
                secondValue = controls.labelFontSizeSp,
                onSecondChange = {
                    onControlsChange(controls.copy(labelFontSizeSp = it))
                },
            )
            FieldRow(
                firstLabel = "Vertical padding (dp)",
                firstValue = controls.labelVerticalPaddingDp,
                onFirstChange = {
                    onControlsChange(controls.copy(labelVerticalPaddingDp = it))
                },
            )
        }

        SettingsSection(title = "Layout Engine Flags") {
            ToggleRow(
                firstLabel = "Degree-aware edge tension",
                firstValue = controls.degreeAwareEdgeTension,
                onFirstChange = {
                    onControlsChange(controls.copy(degreeAwareEdgeTension = it))
                },
            )
            FieldRow(
                firstLabel = "Random seed",
                firstValue = controls.randomSeed,
                onFirstChange = {
                    onControlsChange(controls.copy(randomSeed = it))
                },
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ControlPanelDimensions.itemSpacing),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}

@Composable
private fun PresetChip(
    preset: VisualizerPreset,
    currentPreset: VisualizerPreset,
    onApplyPreset: (VisualizerPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = {
            onApplyPreset(preset)
        },
        modifier = modifier,
        label = {
            Text(
                if (preset == currentPreset) {
                    "${preset.label} active"
                } else {
                    preset.label
                },
            )
        },
    )
}

@Composable
private fun ToggleRow(
    firstLabel: String,
    firstValue: Boolean,
    onFirstChange: (Boolean) -> Unit,
    secondLabel: String? = null,
    secondValue: Boolean = false,
    onSecondChange: ((Boolean) -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ControlPanelDimensions.fieldSpacing),
    ) {
        ToggleCell(
            label = firstLabel,
            value = firstValue,
            onValueChange = onFirstChange,
            modifier = Modifier.weight(1f),
        )
        if (secondLabel != null && onSecondChange != null) {
            ToggleCell(
                label = secondLabel,
                value = secondValue,
                onValueChange = onSecondChange,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ToggleCell(
    label: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = value,
            onCheckedChange = onValueChange,
        )
    }
}

@Composable
private fun FieldRow(
    firstLabel: String,
    firstValue: String,
    onFirstChange: (String) -> Unit,
    secondLabel: String? = null,
    secondValue: String = "",
    onSecondChange: ((String) -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ControlPanelDimensions.fieldSpacing),
    ) {
        SettingField(
            label = firstLabel,
            value = firstValue,
            onValueChange = onFirstChange,
            modifier = Modifier.weight(1f),
        )
        if (secondLabel != null && onSecondChange != null) {
            SettingField(
                label = secondLabel,
                value = secondValue,
                onValueChange = onSecondChange,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SettingField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        label = {
            Text(label)
        },
    )
}

private fun presetBaseOptions(
    preset: VisualizerPreset,
    directed: Boolean,
): GraphVisualizerOptions {
    return when (preset) {
        VisualizerPreset.Sample -> sampleGraphVisualizerOptions(directed = directed)
        VisualizerPreset.Default -> GraphVisualizerOptions.default().copy(
            directed = directed,
            showArrows = directed,
        )
        VisualizerPreset.Performance -> GraphVisualizerOptions.performance(directed = directed)
        VisualizerPreset.Presentation -> GraphVisualizerOptions.presentation(directed = directed)
    }
}

private fun sampleGraphVisualizerOptions(
    directed: Boolean,
): GraphVisualizerOptions {
    return GraphVisualizerOptions.presentation(directed = directed).copy(
        fitToViewport = true,
        interaction = GraphInteractionConfig(
            minScale = SampleInteractionDefaults.minScale,
            maxScale = SampleInteractionDefaults.maxScale,
            tapSelectionPadding = SampleInteractionDefaults.tapSelectionPadding,
        ),
        label = GraphLabelConfig(
            widthDp = SampleLabelDefaults.widthDp,
            fontSizeSp = SampleLabelDefaults.fontSizeSp,
            verticalPaddingDp = SampleLabelDefaults.verticalPaddingDp,
        ),
        layout = ForceLayoutConfig(
            iterations = SampleLayoutDefaults.iterations,
            nodeRepulsion = SampleLayoutDefaults.nodeRepulsion,
            repulsionExponent = SampleLayoutDefaults.repulsionExponent,
            edgeTension = SampleLayoutDefaults.edgeTension,
            centerTension = SampleLayoutDefaults.centerTension,
            baseEdgeLength = SampleLayoutDefaults.baseEdgeLength,
            edgeDistanceScale = SampleLayoutDefaults.edgeDistanceScale,
            damping = SampleLayoutDefaults.damping,
            convergenceThreshold = SampleLayoutDefaults.convergenceThreshold,
            collisionPadding = SampleLayoutDefaults.collisionPadding,
            collisionStrength = SampleLayoutDefaults.collisionStrength,
            maxVelocity = SampleLayoutDefaults.maxVelocity,
        ),
    )
}

private fun VisualizerControlState.animationFlags(): Int {
    var flags = GraphAnimationFlags.NONE
    if (animateColors) {
        flags = flags or GraphAnimationFlags.COLOR_TRANSITION
    }
    if (animateLabelFade) {
        flags = flags or GraphAnimationFlags.LABEL_VISIBILITY_FADE
    }
    if (animateInitialRender) {
        flags = flags or GraphAnimationFlags.INITIAL_RENDER
    }
    if (animateLayout) {
        flags = flags or GraphAnimationFlags.LAYOUT_TRANSITION
    }
    return flags
}

private fun Int.hasFlag(
    flag: Int,
): Boolean = (this and flag) == flag

private fun Float.toFieldValue(): String = toString()

private fun String.toFloatOr(
    fallback: Float,
): Float = toFloatOrNull() ?: fallback

private fun String.toIntOr(
    fallback: Int,
): Int = toIntOrNull() ?: fallback
