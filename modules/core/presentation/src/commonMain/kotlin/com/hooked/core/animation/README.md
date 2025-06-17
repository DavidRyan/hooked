# Shared Element Transitions Guide

This module provides utilities for implementing shared element transitions in Compose Multiplatform.

## Quick Start

### 1. Wrap your screen with SharedTransitionContainer

```kotlin
SharedTransitionContainer(
    modifier = Modifier.fillMaxSize()
) {
    // Your content here
}
```

### 2. Use AnimatedContent for screen transitions

```kotlin
AnimatedContent(
    targetState = screenState,
    transitionSpec = { AnimationSpecs.contentTransitionSpec }
) { state ->
    // Screen content based on state
}
```

### 3. Add shared elements

Use the `sharedTransitionElement` extension on any composable you want to animate:

```kotlin
AsyncImage(
    imageUrl = imageUrl,
    modifier = Modifier
        .sharedTransitionElement(
            key = "unique-key",
            animatedVisibilityScope = animatedVisibilityScope
        )
)
```

## Animation Constants

All animation values are centralized in `AnimationConstants`:
- Durations
- Spring configurations
- Scale values
- Translation offsets

## Reusable Components

### CatchGridItem
A card component with built-in shared element support for grid displays.

### CatchDetailCard
A styled card for displaying catch details with animation support.

## Navigation

Use `CatchesNavigator` for managing screen state and animation keys:

```kotlin
val navigator = rememberCatchesNavigator()

// Navigate to details
navigator.navigateToDetails(catchId)

// Navigate back
navigator.navigateToGrid()
```

## Best Practices

1. **Unique Keys**: Always use unique keys for shared elements (e.g., `"image-${id}"`)
2. **Consistent Shapes**: Keep corner radius and shapes consistent between transitions
3. **Animation Timing**: Use the predefined animation specs for consistency
4. **State Management**: Use the navigator pattern for cleaner state management