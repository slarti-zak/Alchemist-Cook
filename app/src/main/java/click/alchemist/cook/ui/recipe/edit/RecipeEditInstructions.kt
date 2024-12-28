package click.alchemist.cook.ui.recipe.edit

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.DurationPickerDialog
import click.alchemist.cook.compose.MarkdownEditText
import click.alchemist.cook.compose.SimpleTextField
import click.alchemist.cook.extension.humanReadable
import click.alchemist.cook.service.markdown.MarkdownService
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes


@Composable
fun RecipeEditInstructions(instructions: String, onTextChanged: (String) -> Unit = {}, markdownService: MarkdownService? = null) {
    var timerDialogOpen by remember { mutableStateOf<EditData?>(null) }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    MarkdownEditText(
        instructions,
        onTextChanged,
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        markdownService,
        factoryModifier = { editText ->
            val timerMenuId = View.generateViewId()
            val value = object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean = true

                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    if (menu != null && menu.findItem(timerMenuId) == null) {
                        menu.add(0, timerMenuId, 0, R.string.dialog_add_timer_context_action)
                        return true
                    }
                    return false
                }

                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                    if (item?.itemId == timerMenuId) {
                        val start = editText.selectionStart
                        val end = editText.selectionEnd
                        val initialText = editText.editableText.subSequence(start, end)
                        timerDialogOpen = EditData(editText, start, end, initialText.toString())
                        return true
                    }
                    return false
                }

                override fun onDestroyActionMode(mode: ActionMode?) = Unit
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                editText.customInsertionActionModeCallback = value
            }
            editText.customSelectionActionModeCallback = value

            editText.setTextColor(textColor)
        }
    )

    val timerDialogData = timerDialogOpen
    if (timerDialogData != null) {
        AddTimerDialog(timerDialogData.initialText,
            onAccept = { title, duration ->
                val timerText = duration.toComponents { hours, minutes, seconds, _ ->
                    "((${title.trim()}-$hours:$minutes:$seconds))"
                }

                val newInstructions = timerDialogData.editText.editableText.replace(timerDialogData.start, timerDialogData.end, timerText)
                timerDialogData.editText.setText(newInstructions)

                timerDialogOpen = null
            }, onDismiss = { timerDialogOpen = null })
    }
}


@Composable
private fun AddTimerDialog(initialText: String, onAccept: (String, Duration) -> Unit, onDismiss: () -> Unit) {
    var durationDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(initialText) }
    var duration by remember { mutableStateOf(1.minutes) }

    AlertDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onAccept(title, duration) }) { Text(stringResource(R.string.general_accept)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.general_cancel)) } },
        title = { Text("Add Timer") },
        text = { AddTimerDialogContent(title, duration, onValueChange = { title = it }, onDurationClicked = { durationDialog = true }) }
    )

    if (durationDialog) {
        DurationPickerDialog(duration,
            {
                duration = it
                durationDialog = false
            }, { durationDialog = false })
    }
}

@Composable
private fun AddTimerDialogContent(title: String, duration: Duration, onValueChange: (String) -> Unit, onDurationClicked: () -> Unit) {
    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextField(value = title, onValueChange = onValueChange, Modifier.fillMaxWidth())

        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Duration", Modifier.alignByBaseline())
            SimpleTextField(
                value = duration.humanReadable(), onValueChange = {},
                Modifier
                    .weight(1f)
                    .alignByBaseline()
                    .clickable(onClick = onDurationClicked),
                enabled = false,
            )
        }
    }
}

private data class EditData(val editText: EditText, val start: Int, val end: Int, val initialText: String)


@Preview
@Composable
private fun Preview() {
    AppTheme {
        RecipeEditInstructions("Instructions")
    }
}

@Preview("Timer")
@Composable
private fun PreviewTimer() {
    AppTheme {
		AddTimerDialogContent("Title", 1.minutes, onValueChange = {}, onDurationClicked = {})
    }
}