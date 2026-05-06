# FileWatcher
Нужно оформить этот код в coroutine.

```kotlin
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
 
private fun prompt(msg : String) : String {
    print("$msg => ")
    return readLine() ?: ""
}
 
private fun Path.watch() : WatchService {
    //Create a watch service
    val watchService = this.fileSystem.newWatchService()
 
    //Register the service, specifying which events to watch
    register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.OVERFLOW, StandardWatchEventKinds.ENTRY_DELETE)
 
    //Return the watch service
    return watchService
}
 
fun main(args : Array<String>){
    val folder = prompt("Enter a folder to watch")
    val path = Paths.get(folder)
 
    val watcher = path.watch()
    println("Press ctrl+c to exit")
 
    while(true){
        //The watcher blocks until an event is available
        val key = watcher.take()
 
        //Now go through each event on the folder
        key.pollEvents().forEach { it ->
            //Print output according to the event
            when(it.kind().name()){
                "ENTRY_CREATE" -> println("${it.context()} was created")
                "ENTRY_MODIFY" -> println("${it.context()} was modified")
                "OVERFLOW" -> println("${it.context()} overflow")
                "ENTRY_DELETE" -> println("${it.context()} was deleted")
            }
        }
        //Call reset() on the key to watch for future events
        key.reset()
    }
}
```