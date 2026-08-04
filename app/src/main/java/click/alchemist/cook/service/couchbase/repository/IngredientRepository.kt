package click.alchemist.cook.service.couchbase.repository

import click.alchemist.cook.service.store.WebDavService

class IngredientRepository(webDavService: WebDavService) {
	val all = webDavService.liveIngredientNames()
}
