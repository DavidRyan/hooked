package details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import core.components.AsyncImage
import details.model.CatchDetailsIntent

@Composable
fun CatchDetailsScreen(viewModel: CatchDetailsViewModel, catchId: Long) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.sendIntent(CatchDetailsIntent.LoadCatchDetails(catchId))
    }

    if (state.isLoading) {
        //CircularProgressIndicator()
    } else {
        state.catchDetails?.let { details ->
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                AsyncImage(
                    imageUrl = details.photoUrl,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Species: ${details.species}")
                Text(text = "Weight: ${details.weight} kg")
                Text(text = "Length: ${details.length} cm")
            }
        }
    }
}