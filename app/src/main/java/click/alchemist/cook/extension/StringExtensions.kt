package click.alchemist.cook.extension

import kotlin.contracts.contract

fun CharSequence?.isNotNullOrBlank(): Boolean {
    contract {
        returns(true) implies (this@isNotNullOrBlank != null)
    }

    return !this.isNullOrBlank()
}