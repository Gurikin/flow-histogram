package org.gurikin.demo_3d

import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Point3D
import javafx.geometry.Pos
import javafx.scene.AmbientLight
import javafx.scene.Group
import javafx.scene.PerspectiveCamera
import javafx.scene.PointLight
import javafx.scene.Scene
import javafx.scene.SceneAntialiasing
import javafx.scene.SubScene
import javafx.scene.control.Label
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.shape.Sphere
import javafx.scene.transform.Rotate
import javafx.scene.transform.Scale
import javafx.scene.transform.Transform
import javafx.scene.transform.Translate
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json
import org.apache.commons.math3.linear.EigenDecomposition
import org.apache.commons.math3.util.FastMath.sqrt
import org.gurikin.demo_3d.utils.watchDir
import org.gurikin.histogram.DefaultHistogrammator
import org.gurikin.histogram.internal.Bin
import org.gurikin.histogram.internal.Border
import org.gurikin.histogram.internal.Chunk
import org.gurikin.histogram.internal.ChunkId
import org.gurikin.histogram.internal.CovarianceMatrix3D
import org.gurikin.histogram.internal.DefaultChunkAggregator
import org.gurikin.histogram.internal.DefaultChunkQueue
import org.gurikin.histogram.internal.DefaultChunkStorage
import org.gurikin.histogram.internal.Histogram
import org.gurikin.histogram.internal.IntFrame
import org.gurikin.histogram.num_histogram.Int3DFlowGenerator
import org.gurikin.histogram.num_histogram.Int3DHistogramBuilder
import java.io.File
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

val mainScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

class MainApp : Application() {

    private lateinit var rootGroup: Group
    private lateinit var camera: PerspectiveCamera
    private lateinit var cameraTransform: TransformGroup
    private var mousePosX = 0.0
    private var mousePosY = 0.0
    private var mouseOldX = 0.0
    private var mouseOldY = 0.0
    private var rotateX = 0.0
    private var rotateY = 0.0
    private var translateX = 0.0
    private var translateY = 0.0
    private var zoom = -1000.0

    val scope = mainScope

    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var infoLabel: Label
    private val spheresWithData = mutableListOf<SphereWithData>()
    private lateinit var primaryStage: Stage

    private lateinit var statsLabel: Label

    private data class SphereWithData(
        val sphere: Sphere,
        val bin: Bin<*>
    )

    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage
        primaryStage.title = "3D Histogram Viewer"
        val root = StackPane()

        statsLabel = Label().apply {
            style = "-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 5;"
            isWrapText = true
            isVisible = true
        }
        val statsBox = VBox(statsLabel).apply {
            alignment = Pos.TOP_RIGHT
            translateX = 0.5
            translateY = 0.5
        }
        // Добавляем поверх subScene
        root.children.add(statsBox)

        // корневой элемент
        val subScene = createSubScene()
        root.children.add(subScene)

        infoLabel = Label().apply {
            style =
                "-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: white; -fx-padding: 5; -fx-background-radius: 5;"
            isVisible = false
        }
        root.children.add(infoLabel) // будет поверх сцены


        // настройки сцены
        primaryStage.scene = Scene(root, 1024.0, 768.0, true)
        primaryStage.show()

        // запуск мониторинга файла
        startUpdating()

        // обработка закрытия окна
        primaryStage.setOnCloseRequest {
            scope.cancel()
            Platform.exit()
        }
    }

    private fun createGrid(): Group {
        val gridGroup = Group()
        val lineColor = Color.GRAY
        val lineWidth = 1.0

        // Размер сетки (диапазон координат)
        val range = 1200  // подберите под свои данные
        val coordStep = 50

        // Линии по оси X (вдоль X, на уровне Y=0, Z=0)
        for (x in -range..range step coordStep) {
            val line = Box(1.0, range.toDouble() * 2, 1.0)
            line.material = PhongMaterial(lineColor)
            line.translateX = x.toDouble()
            line.translateY = 0.0//range / 2.0
            line.translateZ = 0.0
            gridGroup.children.add(line)
        }

        // Линии по оси Y (вдоль Y, на уровне Y=0, Z=0)
        for (y in -range..range step coordStep) {
            val line = Box(1.0, 1.0, range.toDouble() * 2)
            line.material = PhongMaterial(lineColor)
            line.translateX = 0.0
            line.translateY = y.toDouble()
            line.translateZ = 0.0//-range / 2.0
            gridGroup.children.add(line)
        }

        // Линии по оси Z
        for (z in -range..range step coordStep) {
            val line = Box(range.toDouble() * 2, 1.0, 1.0)
            line.material = PhongMaterial(lineColor)
            line.translateX = 0.0//range / 2.0
            line.translateY = 0.0
            line.translateZ = z.toDouble()
            gridGroup.children.add(line)
        }

        // Оси с толстыми линиями и стрелками (упрощённо)
        val axisX = Box(range.toDouble() * 2, 4.0, 4.0)
        axisX.material = PhongMaterial(Color.RED)
        val axisY = Box(4.0, range.toDouble() * 2, 4.0)
        axisY.material = PhongMaterial(Color.GREEN)
        val axisZ = Box(4.0, 4.0, range.toDouble() * 2)
        axisZ.material = PhongMaterial(Color.BLUE)

        gridGroup.children.addAll(axisX, axisY, axisZ)

        // Подписи осей (простые текстовые метки)
        val font = javafx.scene.text.Font.font("Arial", 30.0)
        val textX = javafx.scene.text.Text("X")
        textX.font = font
        textX.fill = Color.RED
        textX.translateX = range / 8.0
        textX.translateY = -20.0
        textX.translateZ = 0.0
        val textY = javafx.scene.text.Text("Y")
        textY.font = font
        textY.fill = Color.GREEN
        textY.translateX = -30.0
        textY.translateY = range / 8.0
        textY.translateZ = 0.0
        val textZ = javafx.scene.text.Text("Z")
        textZ.font = font
        textZ.fill = Color.BLUE
        textZ.translateX = 0.0
        textZ.translateY = -30.0
        textZ.translateZ = range / 8.0

        gridGroup.children.addAll(textX, textY, textZ)
        return gridGroup
    }

    private fun startUpdating() {
        scope.launch {
            val filePath = getFilePathFromUser()
            val dir = File(filePath).toPath().parent
            watchDir(dir) { Platform.runLater { updateSpheres(filePath) } }
        }
        scope.launch {
            while (this.isActive) {
                val filePath = getFilePathFromUser()
                val file = File(filePath)
                val content = file.readText()
                val histogram = json.decodeFromString<Histogram<Int>>(content)
                updateStatistics(histogram)
                delay(1000.milliseconds)
            }
        }
    }

    private fun updateStatistics(histogram: Histogram<Int>) {
        val cov = histogram.covariance
        val varX = cov.varX
        val varY = cov.varY
        val varZ = cov.varZ
        val covXY = cov.covXY
        val covXZ = cov.covXZ
        val covYZ = cov.covYZ

        val corrXY = if (varX > 0 && varY > 0) covXY / sqrt(varX * varY) else 0.0
        val corrXZ = if (varX > 0 && varZ > 0) covXZ / sqrt(varX * varZ) else 0.0
        val corrYZ = if (varY > 0 && varZ > 0) covYZ / sqrt(varY * varZ) else 0.0

        val info = """
        Covariance Matrix:
        varX = ${"%.2f".format(varX)}
        varY = ${"%.2f".format(varY)}
        varZ = ${"%.2f".format(varZ)}
        covXY = ${"%.2f".format(covXY)}  |  corrXY = ${"%.3f".format(corrXY)}
        covXZ = ${"%.2f".format(covXZ)}  |  corrXZ = ${"%.3f".format(corrXZ)}
        covYZ = ${"%.2f".format(covYZ)}  |  corrYZ = ${"%.3f".format(corrYZ)}
    """.trimIndent()
        Platform.runLater { statsLabel.text = info }
    }

    private fun getFilePathFromUser(): String {
        // Для демонстрации можно передать аргументом или спросить диалогом
        // Здесь упростим: читаем из аргументов приложения, если нет - используем дефолтный
        val parameters = parameters
        return if (parameters.raw.isNotEmpty()) parameters.raw[0] else this::class.java.classLoader.getResource("histogram.json")!!.file
    }

    private fun createSubScene(): SubScene {
        // корневая группа для 3D объектов
        val world = Group()

        // камера
        camera = PerspectiveCamera(true)
        camera.nearClip = 0.1
        camera.farClip = 10000.0
        camera.translateZ = zoom

        cameraTransform = TransformGroup()
        cameraTransform.apply {
            add(Rotate(0.0, Rotate.X_AXIS))
            add(Rotate(0.0, Rotate.Y_AXIS))
            add(Translate(0.0, 0.0, zoom))
        }
        camera.transforms.addAll(cameraTransform.transforms)

        world.children.add(camera)

        // освещение
        val ambientLight = AmbientLight(Color.WHITE)
        val pointLight = PointLight(Color.WHITE)
        pointLight.translateZ = -500.0
        pointLight.translateX = 200.0
        pointLight.translateY = 200.0
        world.children.addAll(ambientLight, pointLight)

        world.children.add(createGrid())

        rootGroup = Group()
        world.children.add(rootGroup)

        val subScene = SubScene(world, 1024.0, 768.0, true, SceneAntialiasing.BALANCED)
        subScene.fill = Color.WHITE
        subScene.camera = camera

        // управление мышью
        setupMouseControl(subScene)

        return subScene
    }

    private fun setupMouseControl(subScene: SubScene) {
        subScene.setOnMousePressed { event ->
            mousePosX = event.sceneX
            mousePosY = event.sceneY
            mouseOldX = event.sceneX
            mouseOldY = event.sceneY
        }

        subScene.setOnMouseDragged { event ->
            val dx = event.sceneX - mouseOldX
            val dy = event.sceneY - mouseOldY
            if (event.button == MouseButton.PRIMARY) {
                rotateX += dy * 0.5
                rotateY += dx * 0.5
                updateCameraRotation()
            } else if (event.button == MouseButton.SECONDARY) {
                translateX -= dx * 5
                translateY -= dy * 5
                camera.translateX = max(-2000.0, translateX)
                camera.translateY = max(-2000.0, translateY)
            }
            mouseOldX = event.sceneX
            mouseOldY = event.sceneY
        }

        subScene.setOnScroll { event ->
            val delta = event.deltaY
            zoom += delta * 2  // коэффициент чувствительности
            zoom = max(-5000.0, min(-200.0, zoom))
            camera.translateZ = zoom
        }

        subScene.setOnMouseMoved { event ->
            val pickResult = event.pickResult
            val intersectedNode = pickResult?.intersectedNode

            // Проверяем, является ли выбранный узел сферой
            if (intersectedNode is Sphere) {
                // Здесь у вас есть сфера, на которую наведена мышь.
                // Теперь нужно найти связанные с ней данные (Bin).
                val sphereData = spheresWithData.find { it.sphere === intersectedNode }
                sphereData?.let {
                    showTooltip(event, it.bin)
                }
            } else {
                hideTooltip()
            }
        }
    }

    private fun showTooltip(event: MouseEvent, bin: Bin<*>) {
        val info = buildString {
            appendLine("x: [${bin.xBorder.from.value()} , ${bin.xBorder.to.value()}]")
            bin.yBorder?.let { appendLine("y: [${it.from.value()} , ${it.to.value()}]") }
            bin.zBorder?.let { appendLine("z: [${it.from.value()} , ${it.to.value()}]") }
            appendLine("frameSum: ${bin.draftFrameSum}")
            appendLine("weight: " + String.format("%.4f", bin.weight))
        }
        infoLabel.text = info
        infoLabel.isVisible = true
        // Позиционируем тултип относительно мыши
        infoLabel.layoutX = event.screenX - primaryStage.x - infoLabel.width / 2
        infoLabel.layoutY = event.screenY - primaryStage.y - 30
    }

    private fun hideTooltip() {
        infoLabel.isVisible = false
    }

    private fun updateCameraRotation() {
        val rotX = cameraTransform.transforms.find { it is Rotate && it.axis == Rotate.X_AXIS } as Rotate
        val rotY = cameraTransform.transforms.find { it is Rotate && it.axis == Rotate.Y_AXIS } as Rotate
        rotX.angle = rotateX
        rotY.angle = rotateY
    }

    private fun updateSpheres(filePath: String) {
        val file = File(filePath)
        val content = file.readText()
        val histogram = json.decodeFromString<Histogram<Int>>(content)
        val validBins = histogram.bins.filter {
            it.weight > 0.005
        }

        if (validBins.isEmpty()) {
            rootGroup.children.clear()
            return
        }

        // находим максимальный вес для масштабирования радиусов
        val maxWeight = validBins.maxOf { it.weight }
        // базовый радиус (минимальный) и множитель
        val minRadius = 1.0
        val maxRadius = 100.0

        // удаляем старые шары
        rootGroup.children.clear()
        spheresWithData.clear()

        validBins.forEach { bin ->
            // вычисляем центр по каждому измерению
            val cx = (bin.xBorder.from.value() + bin.xBorder.to.value()) / 2.0
            val cy = (bin.yBorder!!.from.value() + bin.yBorder!!.to.value()) / 2.0
            val cz = (bin.zBorder!!.from.value() + bin.zBorder!!.to.value()) / 2.0

            // радиус пропорционален весу
            val radius = minRadius + (bin.weight / maxWeight) * (maxRadius - minRadius)
            val sphere = Sphere(radius)
            // материал: синий с небольшим блеском
            val material = PhongMaterial(Color.DARKBLUE)
            sphere.material = material
            sphere.translateX = cx
            sphere.translateY = cy
            sphere.translateZ = cz

            spheresWithData.add(SphereWithData(sphere, bin))
            rootGroup.children.add(sphere)
        }
    }

    private class TransformGroup {
        val transforms = mutableListOf<Transform>()
        fun add(transform: Transform) {
            transforms.add(transform)
        }
    }

    override fun stop() {
        super.stop()
        scope.cancel()
    }
}

suspend fun main(args: Array<String>) {
    supervisorScope {
        launch { Application.launch(MainApp::class.java, *args) }
        launch { launch3DHistogrammator(mainScope) }
    }
}

fun splitIntoParts(from: Int, to: Int, parts: Int): List<Pair<Int, Int>> {
    val total = to - from + 1
    val partSize = total / parts
    val remainder = total % parts
    val result = mutableListOf<Pair<Int, Int>>()
    var current = from
    for (i in 0 until parts) {
        val extra = if (i < remainder) 1 else 0
        val end = current + partSize - 1 + extra
        result.add(current to end)
        current = end + 1
    }
    return result
}

fun launch3DHistogrammator(mainAppScope: CoroutineScope) = runBlocking {
    val histogramBuilder = Int3DHistogramBuilder()
    val from = -1000
    val to = 999
    val chunksPerAxis = 5               // можно изменить: 2, 4, 5 ...
    val binsPerChunk = 10               // количество бинов внутри каждого чанка

    val xIntervals = splitIntoParts(from, to, chunksPerAxis)
    val yIntervals = splitIntoParts(from, to, chunksPerAxis)
    val zIntervals = splitIntoParts(from, to, chunksPerAxis)

    val chunks = TreeSet<Chunk<Int>>()

    for (xRange in xIntervals) {
        val xBorder = Border(IntFrame(xRange.first), IntFrame(xRange.second))
        for (yRange in yIntervals) {
            val yBorder = Border(IntFrame(yRange.first), IntFrame(yRange.second))
            for (zRange in zIntervals) {
                val zBorder = Border(IntFrame(zRange.first), IntFrame(zRange.second))
                val histogram = histogramBuilder.initHistogram(xBorder, binsPerChunk, yBorder, zBorder)
                val chunk = Chunk(histogram, ChunkId())
                chunks.add(chunk)
            }
        }
    }
    chunks.forEachIndexed { index, chunk ->
        val l = chunk.histogram.bins.first()
        val r = chunk.histogram.bins.last()
        println("Chunk: [$index]\tFrom: [${l.xBorder.from},${l.yBorder?.from},${l.zBorder?.from}].\tTo [${r.xBorder.to},${r.yBorder?.to},${r.zBorder?.to}]")
    }
    val chunkStorage = DefaultChunkStorage<Int>(this)
    val chunkQueue = DefaultChunkQueue(this)
    val expectedMessageCnt = 250
    val sourceFlowGenerator =
        Int3DFlowGenerator(-1000..<1000, expectedMessageCnt, true)
    val sourceFlow = sourceFlowGenerator.flowData()
    val chunkAggregator = DefaultChunkAggregator(
        framesFlow = sourceFlow,
        chunks = chunks,
        chunkStorage = chunkStorage,
        chunkQueue = chunkQueue,
        scope = this,
        queueSendTimeout = 200.milliseconds,
    )

    this.launch { chunkAggregator.collectData() }

    this.launch {
        while (mainAppScope.isActive) {
            println("Main app alive...")
            delay(2000.milliseconds)
        }
        this@runBlocking.coroutineContext.cancelChildren()
    }


    this.launch {
        val globalBorder: Border<Int> =
            Border(
                IntFrame(from),
                IntFrame(chunks.last().histogram.bins.last().xBorder.to.value())
            )
        val histogram = histogramBuilder.initHistogram(globalBorder, 8, globalBorder, globalBorder)
        val histogrammator = DefaultHistogrammator(
            histogram = histogram,
            chunkQueue = chunkQueue,
            chunkStorage = chunkStorage,
            accumulateDelay = 50.milliseconds,
            scope = this
        )
        launch {
            histogrammator.accumulate()
            histogrammator.calcAvg()
        }
        val filePath = this::class.java.classLoader.getResource("histogram.json")!!.file
        val file = File(filePath)
        if (!file.exists()) {
            println("File not found: $filePath")
        }

        val checkHistogrammatorJob = launch {
            while (histogrammator.histogram.getFrameSum() < 200000) {
                println("Accumulate general histogram...")
                println("Total message count = ${histogrammator.histogram.getFrameSum()}")
                delay(1000.milliseconds)
                runCatching { file.writeText(Json.encodeToString(histogrammator.histogram)) }
            }
        }

        checkHistogrammatorJob.join()
        this@runBlocking.coroutineContext.cancelChildren()
    }
}