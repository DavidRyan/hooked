package grid


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import grid.model.CatchGridIntent
import grid.model.CatchModel
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import theme.HookedTheme
import io.kamel.core.Resource


@Composable
fun CatchGrid(
    modifier: Modifier = Modifier,
    viewModel: CatchGridViewModel
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.sendIntent(CatchGridIntent.LoadCatches(true))
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(state.catches) { catch ->
            CatchGridItem(
                catch = catch,
                onClick = {
                    //todo:
                }
            )
        }
    }
}

@Composable
fun CatchGridItem(
    catch: CatchModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = HookedTheme.surface)
    ) {
        val getPainterResource: @Composable (BoxWithConstraintsScope.() -> Resource<Painter>) = {
            asyncPainterResource(
                catch.imageUrl,
                filterQuality = FilterQuality.High
            )
        }
        KamelImage(
            resource = getPainterResource,
            contentDescription = null,
            modifier = Modifier
                .aspectRatio(1F)
                .padding(8.dp)
                .shadow(elevation = 8.dp, RoundedCornerShape(16.dp))
                .background(Color.White, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop,
            onLoading = { CircularProgressIndicator(it) },
            onFailure = { exception: Throwable ->
                exception.printStackTrace()
            },
        )    }
}