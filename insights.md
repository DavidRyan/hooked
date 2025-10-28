# Stats Screen Implementation Plan

## Overview
A statistics dashboard showing catch analytics, starting with a species breakdown chart. Accessible via FAB from the catch grid, with plans for filtering and AI insights.

---

## Architecture

### Module Structure
```
modules/catches/
├── domain/
│   ├── entities/
│   │   └── StatsEntity.kt (NEW)
│   ├── usecases/
│   │   └── GetCatchStatsUseCase.kt (NEW)
│   └── repositories/
│       └── CatchRepository.kt (UPDATE)
├── data/
│   └── repo/
│       └── CatchRepository.kt (UPDATE)
└── presentation/
    ├── stats/
    │   ├── StatsViewModel.kt (NEW)
    │   ├── StatsScreen.kt (NEW)
    │   └── model/
    │       ├── StatsIntent.kt (NEW)
    │       ├── StatsState.kt (NEW)
    │       └── StatsEffect.kt (NEW)
    └── components/
        └── SpeciesChart.kt (NEW)
```

---

## Phase 1: Data Layer

### 1.1 Stats Entity
**File**: `modules/catches/domain/src/commonMain/kotlin/com/hooked/catches/domain/entities/StatsEntity.kt`

```kotlin
package com.hooked.catches.domain.entities

data class StatsEntity(
    val totalCatches: Int,
    val speciesBreakdown: Map<String, Int>,
    val uniqueSpecies: Int,
    val uniqueLocations: Int,
    val averageWeight: Double?,
    val averageLength: Double?,
    val biggestCatch: CatchEntity?,
    val mostRecentCatch: CatchEntity?
)

data class SpeciesData(
    val name: String,
    val count: Int,
    val percentage: Float
)
```

### 1.2 Repository Update
**File**: `modules/catches/domain/src/commonMain/kotlin/com/hooked/catches/domain/repositories/CatchRepository.kt`

```kotlin
// Add to existing interface
interface CatchRepository {
    // ... existing methods
    suspend fun getCatchStats(): Result<StatsEntity>
}
```

**File**: `modules/catches/data/src/commonMain/kotlin/com/hooked/catches/data/repo/CatchRepository.kt`

```kotlin
// Add to implementation
override suspend fun getCatchStats(): Result<StatsEntity> {
    return try {
        // For now, calculate stats client-side from all catches
        val catches = getCatches().getOrNull() ?: emptyList()
        
        val speciesBreakdown = catches
            .mapNotNull { it.name }
            .groupingBy { it }
            .eachCount()
        
        val stats = StatsEntity(
            totalCatches = catches.size,
            speciesBreakdown = speciesBreakdown,
            uniqueSpecies = speciesBreakdown.keys.size,
            uniqueLocations = catches.mapNotNull { it.location }.distinct().size,
            averageWeight = catches.mapNotNull { it.weight }.average().takeIf { !it.isNaN() },
            averageLength = catches.mapNotNull { it.length }.average().takeIf { !it.isNaN() },
            biggestCatch = catches.maxByOrNull { it.weight ?: 0.0 },
            mostRecentCatch = catches.maxByOrNull { it.dateCaught ?: "" }
        )
        
        Result.success(stats)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 1.3 Use Case
**File**: `modules/catches/domain/src/commonMain/kotlin/com/hooked/catches/domain/usecases/GetCatchStatsUseCase.kt`

```kotlin
package com.hooked.catches.domain.usecases

import com.hooked.catches.domain.entities.StatsEntity
import com.hooked.catches.domain.repositories.CatchRepository
import com.hooked.core.domain.UseCaseResult

class GetCatchStatsUseCase(private val catchRepository: CatchRepository) {
    suspend operator fun invoke(): UseCaseResult<StatsEntity> {
        return try {
            val result = catchRepository.getCatchStats()
            if (result.isSuccess) {
                UseCaseResult.Success(result.getOrNull()!!)
            } else {
                val exception = result.exceptionOrNull()
                UseCaseResult.Error(
                    exception?.message ?: "Failed to load stats",
                    exception,
                    "GetCatchStatsUseCase"
                )
            }
        } catch (e: Exception) {
            UseCaseResult.Error(e.message ?: "Unknown error", e, "GetCatchStatsUseCase")
        }
    }
}
```

---

## Phase 2: Presentation Layer

### 2.1 State Management
**File**: `modules/catches/presentation/src/commonMain/kotlin/com/hooked/catches/presentation/model/StatsModels.kt`

```kotlin
package com.hooked.catches.presentation.model

import com.hooked.catches.domain.entities.SpeciesData

data class StatsState(
    val isLoading: Boolean = false,
    val totalCatches: Int = 0,
    val speciesData: List<SpeciesData> = emptyList(),
    val uniqueSpecies: Int = 0,
    val uniqueLocations: Int = 0,
    val averageWeight: String? = null,
    val averageLength: String? = null,
    val biggestCatchName: String? = null,
    val biggestCatchWeight: String? = null,
    val error: String? = null
)

sealed class StatsIntent {
    object LoadStats : StatsIntent()
    object Refresh : StatsIntent()
    object NavigateBack : StatsIntent()
}

sealed class StatsEffect {
    object NavigateBack : StatsEffect()
    data class ShowError(val message: String) : StatsEffect()
}
```

### 2.2 ViewModel
**File**: `modules/catches/presentation/src/commonMain/kotlin/com/hooked/catches/presentation/StatsViewModel.kt`

```kotlin
package com.hooked.catches.presentation

import com.hooked.core.HookedViewModel
import com.hooked.catches.domain.usecases.GetCatchStatsUseCase
import com.hooked.catches.domain.entities.SpeciesData
import com.hooked.core.domain.UseCaseResult
import kotlinx.coroutines.launch
import com.hooked.catches.presentation.model.StatsEffect
import com.hooked.catches.presentation.model.StatsIntent
import com.hooked.catches.presentation.model.StatsState
import com.hooked.core.logging.logError

class StatsViewModel(
    private val getCatchStatsUseCase: GetCatchStatsUseCase
) : HookedViewModel<StatsIntent, StatsState, StatsEffect>() {

    init {
        handleIntent(StatsIntent.LoadStats)
    }

    override fun handleIntent(intent: StatsIntent) {
        when (intent) {
            is StatsIntent.LoadStats -> loadStats()
            is StatsIntent.Refresh -> loadStats()
            is StatsIntent.NavigateBack -> sendEffect { StatsEffect.NavigateBack }
        }
    }

    private fun loadStats() {
        setState { copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                when (val result = getCatchStatsUseCase()) {
                    is UseCaseResult.Success -> {
                        val stats = result.data
                        
                        val totalCount = stats.totalCatches
                        val speciesData = stats.speciesBreakdown.map { (name, count) ->
                            SpeciesData(
                                name = name,
                                count = count,
                                percentage = if (totalCount > 0) count.toFloat() / totalCount else 0f
                            )
                        }.sortedByDescending { it.count }
                        
                        setState {
                            copy(
                                isLoading = false,
                                totalCatches = stats.totalCatches,
                                speciesData = speciesData,
                                uniqueSpecies = stats.uniqueSpecies,
                                uniqueLocations = stats.uniqueLocations,
                                averageWeight = stats.averageWeight?.let { "%.1f lbs".format(it) },
                                averageLength = stats.averageLength?.let { "%.1f in".format(it) },
                                biggestCatchName = stats.biggestCatch?.name,
                                biggestCatchWeight = stats.biggestCatch?.weight?.let { "%.1f lbs".format(it) }
                            )
                        }
                    }
                    is UseCaseResult.Error -> {
                        setState { 
                            copy(
                                isLoading = false,
                                error = result.message
                            ) 
                        }
                        sendEffect { StatsEffect.ShowError(result.message) }
                    }
                }
            } catch (e: Exception) {
                logError("Failed to load stats", e)
                setState { 
                    copy(
                        isLoading = false,
                        error = e.message
                    ) 
                }
                sendEffect { StatsEffect.ShowError("Failed to load stats: ${e.message}") }
            }
        }
    }

    override fun createInitialState(): StatsState {
        return StatsState()
    }
}
```

### 2.3 Species Chart Component
**File**: `modules/catches/presentation/src/commonMain/kotlin/com/hooked/catches/presentation/components/SpeciesChart.kt`

```kotlin
package com.hooked.catches.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.hooked.catches.domain.entities.SpeciesData
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeciesPieChart(
    speciesData: List<SpeciesData>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color(0xFF6200EE),
        Color(0xFF03DAC5),
        Color(0xFFFF5722),
        Color(0xFF4CAF50),
        Color(0xFFFFC107),
        Color(0xFF9C27B0),
        Color(0xFF00BCD4),
        Color(0xFFFF9800)
    )
    
    var startAnimation by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )
    
    LaunchedEffect(Unit) {
        startAnimation = true
    }
    
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(220.dp)
            ) {
                val canvasSize = size.minDimension
                val radius = canvasSize / 2
                val strokeWidth = 50f
                
                var currentAngle = -90f
                
                speciesData.take(8).forEachIndexed { index, data ->
                    val sweepAngle = 360f * data.percentage * animatedProgress
                    
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = currentAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth),
                        topLeft = Offset(
                            x = size.width / 2 - radius,
                            y = size.height / 2 - radius
                        ),
                        size = Size(radius * 2, radius * 2)
                    )
                    
                    currentAngle += sweepAngle
                }
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${speciesData.sumOf { it.count }}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Total Catches",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        speciesData.take(8).forEachIndexed { index, data ->
            SpeciesLegendItem(
                species = data.name,
                count = data.count,
                percentage = data.percentage,
                color = colors[index % colors.size],
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun SpeciesLegendItem(
    species: String,
    count: Int,
    percentage: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = species,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "(${(percentage * 100).toInt()}%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### 2.4 Stats Screen
**File**: `modules/catches/presentation/src/commonMain/kotlin/com/hooked/catches/presentation/StatsScreen.kt`

```kotlin
package com.hooked.catches.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hooked.catches.presentation.components.SpeciesPieChart
import com.hooked.catches.presentation.model.StatsEffect
import com.hooked.catches.presentation.model.StatsIntent
import com.hooked.core.presentation.toast.ToastManager
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatsViewModel = koinViewModel(),
    toastManager: ToastManager = koinInject()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is StatsEffect.NavigateBack -> onNavigateBack()
                is StatsEffect.ShowError -> toastManager.showError(effect.message)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.handleIntent(StatsIntent.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.handleIntent(StatsIntent.Refresh) }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (state.isLoading && state.totalCatches == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.totalCatches == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "No Data Yet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start logging catches to see your statistics",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Catches",
                        value = state.totalCatches.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Species",
                        value = state.uniqueSpecies.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.averageWeight?.let {
                        StatCard(
                            title = "Avg Weight",
                            value = it,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    state.averageLength?.let {
                        StatCard(
                            title = "Avg Length",
                            value = it,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Species Breakdown Chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Species Breakdown",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (state.speciesData.isNotEmpty()) {
                            SpeciesPieChart(
                                speciesData = state.speciesData,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = "No species data available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // AI Insights Placeholder
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🤖 AI Insights",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Coming soon: Personalized fishing insights and recommendations based on your catch history.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Filtering Placeholder
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📊 Advanced Filters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Coming soon: Filter statistics by date range, location, and species to analyze your fishing patterns.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}
```

---

## Phase 3: Integration

### 3.1 Add Navigation Route
**File**: `modules/core/presentation/src/commonMain/kotlin/com/hooked/core/nav/Screens.kt`

```kotlin
sealed class Screens() {
    // ... existing screens
    @Serializable
    object Stats : Screens()
}
```

### 3.2 Add FAB to Catch Grid
**File**: `modules/catches/presentation/src/commonMain/kotlin/com/hooked/catches/presentation/CatchesScreen.kt`

Add to CatchGridScreen's Scaffold:

```kotlin
floatingActionButton = {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats FAB
        FloatingActionButton(
            onClick = { /* Navigate to Stats */ },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(Icons.Default.BarChart, "Statistics")
        }
        
        // Existing Add FAB
        FloatingActionButton(
            onClick = { /* Navigate to Submit */ }
        ) {
            Icon(Icons.Default.Add, "Add catch")
        }
    }
}
```

### 3.3 DI Setup
**File**: `composeApp/src/commonMain/kotlin/di/Modules.kt`

```kotlin
// Add to catches module
single { GetCatchStatsUseCase(get()) }
viewModel { StatsViewModel(get()) }
```

### 3.4 Navigation Setup
Add route in NavHost (wherever that's defined):

```kotlin
composable<Screens.Stats> {
    StatsScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

---

## Phase 4: Future Enhancements

### 4.1 Advanced Filtering
```kotlin
data class StatsFilter(
    val dateRange: DateRange? = null,
    val species: List<String>? = null,
    val locations: List<String>? = null,
    val minWeight: Double? = null,
    val maxWeight: Double? = null
)
```

### 4.2 AI Insights Integration
```kotlin
data class AIInsight(
    val type: InsightType,
    val title: String,
    val description: String,
    val confidence: Float
)

enum class InsightType {
    BEST_TIME_TO_FISH,
    BEST_LOCATION,
    SPECIES_RECOMMENDATION,
    SEASONAL_PATTERN
}
```

### 4.3 More Chart Types
- Line chart for catches over time
- Bar chart for location comparison
- Heatmap for time/day patterns

### 4.4 Backend Optimization
Currently calculating stats client-side. Eventually add backend endpoint:

```elixir
# GET /api/user_catches/stats
def stats(conn, _params) do
  user_id = conn.assigns.current_user.id
  stats = Catches.get_user_stats(user_id)
  json(conn, stats)
end
```

---

## Implementation Checklist

### Phase 1: Core Stats (Day 1-2)
- [x] Create StatsEntity
- [x] Add getCatchStats to repository
- [x] Create GetCatchStatsUseCase
- [x] Add DI bindings

### Phase 2: UI (Day 3-4)
- [x] Create StatsViewModel and state models
- [x] Build SpeciesPieChart component
- [x] Build StatsScreen
- [x] Add empty states

### Phase 3: Navigation (Day 4)
- [x] Add Stats screen to Screens.kt
- [x] Add navigation route
- [x] Add FAB to CatchGridScreen
- [x] Test navigation flow

### Phase 4: Polish (Day 5)
- [x] Add animations to charts
- [x] Implement pull-to-refresh
- [x] Add placeholder cards for AI/filters
- [x] Error handling
- [x] Loading states

### Phase 5: Testing (Day 5-6)
- [ ] Unit tests for StatsViewModel
- [ ] Test stats calculation logic
- [ ] Test empty states
- [ ] Test error handling

---

## Design Notes

### Colors for Species Chart
Using Material 3 dynamic colors with fallback palette:
- Purple: #6200EE
- Teal: #03DAC5
- Orange: #FF5722
- Green: #4CAF50
- Amber: #FFC107
- Deep Purple: #9C27B0
- Cyan: #00BCD4
- Deep Orange: #FF9800

### Layout Structure
```
StatsScreen
├── TopAppBar (Back + Refresh)
├── Summary Cards (2x2 grid)
│   ├── Total Catches
│   ├── Unique Species
│   ├── Avg Weight
│   └── Avg Length
├── Species Chart Card
│   ├── Pie Chart (animated)
│   └── Legend List
├── AI Insights Placeholder
└── Filters Placeholder
```

### Animation Timing
- Chart entrance: 1000ms ease-out
- Card entrance: 300ms stagger
- Number counters: 800ms

---

## Estimated Timeline
- **Day 1**: Data layer + use case (3-4 hours)
- **Day 2**: ViewModel + state management (3-4 hours)
- **Day 3**: Species chart component (4-5 hours)
- **Day 4**: Stats screen + navigation (4-5 hours)
- **Day 5**: Polish + testing (4-5 hours)

**Total**: 2-3 days for complete implementation

---

*Last updated: 2025-10-27*
