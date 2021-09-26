package click.alchemist.cook.ui.settings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import click.alchemist.cook.R
import click.alchemist.cook.service.couchbase.CouchbaseState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SyncStatusPreference @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr) {

    private lateinit var obs: Flow<CouchbaseState>
    private lateinit var lifecycleScope: LifecycleCoroutineScope

    private var disposable: Job? = null

    init {
        layoutResource = R.layout.preference_sync_status
    }

    fun update(obs: Flow<CouchbaseState>, lifecycleScope: LifecycleCoroutineScope) {
        this.obs = obs
        this.lifecycleScope = lifecycleScope
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)

        holder?.apply {
            itemView.isClickable = false

            val statusText = itemView.findViewById<TextView>(R.id.statusText)
            val errorLabel = itemView.findViewById<TextView>(R.id.errorLabel)
            val error = itemView.findViewById<TextView>(R.id.error)

            disposable?.cancel()
            disposable = lifecycleScope.launch {
                obs.collect {
                    if (it is CouchbaseState.GuestState) {
                        statusText.setText(R.string.sync_status_guest)

                        errorLabel.visibility = View.INVISIBLE
                        error.visibility = View.INVISIBLE
                    } else {
                        it as CouchbaseState.AccountState
                        statusText.text = it.status.activityLevel.toString()

                        if (it.status.error != null) {
                            errorLabel.visibility = View.VISIBLE
                            error.text = it.status.error?.localizedMessage ?: "Unknown Error"
                            error.visibility = View.VISIBLE

                        } else {
                            errorLabel.visibility = View.INVISIBLE
                            error.visibility = View.INVISIBLE
                        }
                    }
                }
            }
        }
    }
}