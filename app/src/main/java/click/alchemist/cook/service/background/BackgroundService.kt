package click.alchemist.cook.service.background

interface BackgroundService {
    fun cancelSyncWorker()
    fun startSyncWorker()
}

