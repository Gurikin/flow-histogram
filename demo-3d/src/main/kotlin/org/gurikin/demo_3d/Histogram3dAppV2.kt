package org.gurikin.demo_3d

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.AmbientLight
import javafx.scene.Group
import javafx.scene.PerspectiveCamera
import javafx.scene.PointLight
import javafx.scene.Scene
import javafx.scene.SceneAntialiasing
import javafx.scene.SubScene
import javafx.scene.input.MouseButton
import javafx.scene.layout.BorderPane
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.shape.Sphere
import javafx.scene.transform.Rotate
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
import org.gurikin.demo_3d.utils.watchDir
import org.gurikin.histogram.DefaultHistogrammator
import org.gurikin.histogram.internal.Border
import org.gurikin.histogram.internal.Chunk
import org.gurikin.histogram.internal.DefaultChunkAggregator
import org.gurikin.histogram.internal.DefaultChunkQueue
import org.gurikin.histogram.internal.DefaultChunkStorage
import org.gurikin.histogram.internal.Histogram
import org.gurikin.histogram.internal.HistogramConfiguration
import org.gurikin.histogram.internal.HistogramSourceTypesEnum
import org.gurikin.histogram.internal.IntFrame
import org.gurikin.histogram.internal.generateChunks
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
    private var zoom = -2000.0

    val scope = mainScope

    private val json = Json { ignoreUnknownKeys = true }

    override fun start(primaryStage: Stage) {
        primaryStage.title = "3D Histogram Viewer"

        // корневой элемент
        val root = BorderPane()
        rootGroup = Group()
        val subScene = createSubScene()
        root.center = subScene

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
        val coordStep = 100

        // Линии по оси X (вдоль X, на уровне Y=0, Z=0)
        for (x in -range..range step coordStep) {
            val line = Box(1.0, 0.5, 0.5)
            line.material = PhongMaterial(lineColor)
            line.translateX = x.toDouble()
            line.translateY = 0.0
            line.translateZ = 0.0
            gridGroup.children.add(line)
        }
        // Линии по оси Z
        for (z in -range..range step coordStep) {
            val line = Box(0.5, 0.5, 1.0)
            line.material = PhongMaterial(lineColor)
            line.translateX = 0.0
            line.translateY = 0.0
            line.translateZ = z.toDouble()
            gridGroup.children.add(line)
        }

        // Оси с толстыми линиями и стрелками (упрощённо)
        val axisX = Box(1000.0, 2.0, 2.0)
        axisX.material = PhongMaterial(Color.RED)
        val axisY = Box(2.0, 1000.0, 2.0)
        axisY.material = PhongMaterial(Color.GREEN)
        val axisZ = Box(2.0, 2.0, 1000.0)
        axisZ.material = PhongMaterial(Color.BLUE)

        gridGroup.children.addAll(axisX, axisY, axisZ)

        // Подписи осей (простые текстовые метки)
        val font = javafx.scene.text.Font.font("Arial", 20.0)
        val textX = javafx.scene.text.Text("X")
        textX.font = font
        textX.fill = Color.RED
        textX.translateX = range + 50.0
        textX.translateY = -20.0
        textX.translateZ = 0.0
        val textY = javafx.scene.text.Text("Y")
        textY.font = font
        textY.fill = Color.GREEN
        textY.translateX = -30.0
        textY.translateY = range + 50.0
        textY.translateZ = 0.0
        val textZ = javafx.scene.text.Text("Z")
        textZ.font = font
        textZ.fill = Color.BLUE
        textZ.translateX = 0.0
        textZ.translateY = -30.0
        textZ.translateZ = range + 50.0

        gridGroup.children.addAll(textX, textY, textZ)
        return gridGroup
    }

    private fun startUpdating() {
        scope.launch {
            val filePath = getFilePathFromUser()
            val dir = File(filePath).toPath().parent
            watchDir(dir) { Platform.runLater { updateSpheres(filePath) } }
        }
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
                zoom += dy * 5
                zoom = max(-5000.0, min(-200.0, zoom))
                camera.translateZ = zoom
            }
            mouseOldX = event.sceneX
            mouseOldY = event.sceneY
        }
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
            it.weight > 0.0 &&
                    it.yBorder?.from?.value() is Int &&
                    it.zBorder?.from?.value() is Int
        }

        if (validBins.isEmpty()) {
            rootGroup.children.clear()
            return
        }

        // находим максимальный вес для масштабирования радиусов
        val maxWeight = validBins.maxOf { it.weight }
        // базовый радиус (минимальный) и множитель
        val minRadius = 3.0
        val maxRadius = 15.0

        // удаляем старые шары
        rootGroup.children.clear()

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

fun launch3DHistogrammator(mainAppScope: CoroutineScope) = runBlocking {
    val histogramBuilder = Int3DHistogramBuilder()
    val configuration = HistogramConfiguration(
        sourceType = HistogramSourceTypesEnum.INT,
        histogramBorder = Border(
            IntFrame(
                0
            ), IntFrame(999)
        ),
        minStep = 1,
        valueList = null
    )
    val chunks = TreeSet<Chunk<Int>>()
    chunks.addAll(configuration.generateChunks(histogramBuilder))
    val chunkStorage = DefaultChunkStorage<Int>(this)
    val chunkQueue = DefaultChunkQueue(this)
    val expectedMessageCnt = 1000
    val sourceFlowGenerator =
        Int3DFlowGenerator(0..<999, expectedMessageCnt)
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
        while(mainAppScope.isActive) {
            println("Main app alive...")
            delay(2000.milliseconds)
        }
        this@runBlocking.coroutineContext.cancelChildren()
    }


    this.launch {
        val globalBorder: Border<Int> =
            Border(
                IntFrame(
                    0
                ),
                IntFrame(chunks.last().histogram.bins.last().xBorder.to.value())
            )
        val histogram = histogramBuilder.initHistogram(globalBorder, 10)
        val histogrammator = DefaultHistogrammator(
            histogram = histogram,
            chunkQueue = chunkQueue,
            chunkStorage = chunkStorage,
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
                delay(500.milliseconds)
                runCatching { file.writeText(Json.encodeToString(histogrammator.histogram)) }
            }
        }

        checkHistogrammatorJob.join()
        this@runBlocking.coroutineContext.cancelChildren()
    }
}