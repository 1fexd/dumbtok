package fe.dumbtok.composable.component.bottomsheet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fe.dumbtok.module.tiktok.TiktokEvent
import kotlinx.coroutines.flow.StateFlow

@Composable
fun LoadingIndicator(
    events: StateFlow<TiktokEvent>,
) {
    val event by events.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        CircularProgressIndicator()

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Loading"
//                stringResource(id = R.string.loading_link)
            ,
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = event.toString(),
//            stringResource(id = event.id, *event.args),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
