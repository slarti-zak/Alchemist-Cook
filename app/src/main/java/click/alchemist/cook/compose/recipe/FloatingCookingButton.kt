package click.alchemist.cook.compose.recipe

import androidx.compose.animation.Crossfade
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import click.alchemist.cook.R

@Composable
fun FloatingCookingButton(isPlaning: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(onClick = onClick, modifier) {
        Crossfade(targetState = isPlaning) {
            Icon(painterResource(if (it) R.drawable.ic_check else R.drawable.ic_chef_hat), "Toggle Cooking")
        }
    }
}