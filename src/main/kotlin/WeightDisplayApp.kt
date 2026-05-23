package org.gurikin

import java.io.File
import java.util.*
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.stage.Stage
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json
import org.gurikin.histogram.DefaultHistogrammator
import org.gurikin.histogram.internal.Border
import org.gurikin.histogram.internal.Chunk
import org.gurikin.histogram.internal.DefaultChunkAggregator
import org.gurikin.histogram.internal.DefaultChunkQueue
import org.gurikin.histogram.internal.DefaultChunkStorage
import org.gurikin.histogram.internal.Histogram
import org.gurikin.histogram.internal.HistogramConfiguration
import org.gurikin.histogram.internal.HistogramSourceTypesEnum
import org.gurikin.histogram.internal.Frame
import org.gurikin.histogram.internal.IntFrame
import org.gurikin.histogram.internal.generateChunks
import org.gurikin.histogram.num_histogram.IntFlowGenerator
import org.gurikin.histogram.num_histogram.IntHistogramBuilder
import org.gurikin.utils.watchDir

class WeightDisplayApp : Application() {

    private lateinit var labels: List<Label>
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun start(primaryStage: Stage) {
        // Создаём UI
        val root = HBox(20.0).apply {
            alignment = Pos.CENTER
            style = "-fx-padding: 20; -fx-background-color: #2b2b2b;"
        }

        // Сначала пустые метки, потом обновим
        labels = emptyList()
        val scene = Scene(root, 800.0, 200.0)
        primaryStage.title = "Weight Display"
        primaryStage.scene = scene
        primaryStage.show()

        // Запускаем периодическое обновление
        startUpdating(root)
    }

    private fun startUpdating(root: HBox) {
        scope.launch {
            val filePath = getFilePathFromUser()
            val dir = File(filePath).toPath().parent
            watchDir(dir) { updateDisplay(filePath, root) }
        }
    }

    private fun updateDisplay(filePath: String, root: HBox) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                println("File not found: $filePath")
                return
            }
            val content = file.readText()
            val rootData = json.decodeFromString<Histogram<Int>>(content)
            val weights = rootData.bins.map { it.weight }

            if (weights.isNotEmpty()) {
                Platform.runLater {
                    updateUI(weights, root)
                }
            } else {
                println("No weights found in bins")
            }
        } catch (e: Exception) {
            println("Error reading/parsing file: ${e.message}")
        }
    }

    private fun updateUI(weights: List<Double>, root: HBox) {
        // Находим максимальный вес для масштабирования шрифта
        val maxWeight = weights.maxOrNull() ?: 1.0
        val minFontSize = 14.0
        val maxFontSize = 72.0

        // Создаём или обновляем метки
        // Пересоздаём
        root.children.clear()
        labels = weights.map { weight ->
            Label().apply {
                text = weight.toString()
                style = "-fx-text-fill: white; -fx-font-weight: bold;"
            }
        }
        root.children.addAll(labels)

        // Обновляем размер шрифта пропорционально весу
        labels.zip(weights).forEach { (label, weight) ->
            // Линейное масштабирование: от min до max в зависимости от отношения веса к максимальному
            val fontSize = if (maxWeight > 0) {
                minFontSize + (maxFontSize - minFontSize) * (weight / maxWeight)
            } else {
                minFontSize
            }
            label.style = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: ${fontSize}px;"
        }
    }

    private fun getFilePathFromUser(): String {
        // Для демонстрации можно передать аргументом или спросить диалогом
        // Здесь упростим: читаем из аргументов приложения, если нет - используем дефолтный
        val parameters = parameters
        return if (parameters.raw.isNotEmpty()) parameters.raw[0] else this::class.java.classLoader.getResource("histogram.json")!!.file
    }

    override fun stop() {
        scope.cancel() // отменяем корутины при закрытии окна
        super.stop()
    }
}

suspend fun main(args: Array<String>) {
    supervisorScope {
        launch { Application.launch(WeightDisplayApp::class.java, *args) }
        launch { launchHistogrammator() }
    }
}

fun launchHistogrammator() = runBlocking {
    val histogramBuilder = IntHistogramBuilder()
    val configuration = HistogramConfiguration(
        sourceType = HistogramSourceTypesEnum.INT,
        histogramBorder = Border(IntFrame(0), IntFrame(999)),
        minStep = 1,
        valueList = null
    )
    val chunks = TreeSet<Chunk<Int>>()
    chunks.addAll(configuration.generateChunks(histogramBuilder))
    val chunkStorage = DefaultChunkStorage<Int>(this)
    val chunkQueue = DefaultChunkQueue(this)
    val expectedMessageCnt = 1000
    val sourceFlowGenerator = IntFlowGenerator(0..<expectedMessageCnt, expectedMessageCnt)
    val sourceFlow = sourceFlowGenerator.flowData()
    val chunkAggregator = DefaultChunkAggregator(
        framesFlow = sourceFlow,
        chunks = chunks,
        chunkStorage = chunkStorage,
        chunkQueue = chunkQueue,
        scope = this,
        queueSendTimeout = 100.milliseconds,
    )

    this.launch { chunkAggregator.collectData() }


    this.launch {
        val globalBorder: Border<Int> =
            Border(IntFrame(0), IntFrame(chunks.last().histogram.bins.last().border.to.value()))
        val histogram = histogramBuilder.initHistogram(globalBorder, 10)
        val histogrammator = DefaultHistogrammator(
            histogram = histogram,
            chunkQueue = chunkQueue,
            chunkStorage = chunkStorage,
            scope = this
        )
        launch { histogrammator.accumulate() }
        val filePath = this::class.java.classLoader.getResource("histogram.json")!!.file
        val file = File(filePath)
        if (!file.exists()) {
            println("File not found: $filePath")
        }

        val checkHistogrammatorJob = launch {
            while (histogrammator.histogram.getFrameSum() < 200000) {
                println("Accumulate general histogram...")
                println("Total message count = ${histogrammator.histogram.getFrameSum()}")
                delay(500.milliseconds)
                runCatching { file.writeText(Json.encodeToString(histogrammator.histogram)) }
            }
        }

        checkHistogrammatorJob.join()
        this@runBlocking.coroutineContext.cancelChildren()
    }
}