package org.gurikin.utils

import org.slf4j.LoggerFactory
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds

private val logger = LoggerFactory.getLogger("org.gurikin.watchDir")

fun watchDir(dirPath: Path, executor: () -> Unit) {
    val watchService = FileSystems.getDefault().newWatchService()

    // Register for specific events: CREATE, MODIFY, DELETE
    dirPath.register(
        watchService,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.ENTRY_DELETE
    )

    while (true) {
        val key = watchService.take() // Blocks until an event occurs
        for (event in key.pollEvents()) {
            val kind = event.kind()
            val fileName = event.context()
            logger.info("Event: $kind, File: $fileName")
            executor.invoke()
        }
        if (!key.reset()) break // Stop if directory is no longer accessible
    }
}