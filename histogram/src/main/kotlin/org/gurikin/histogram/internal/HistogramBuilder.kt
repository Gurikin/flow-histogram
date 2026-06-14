package org.gurikin.histogram.internal

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.abs
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.gurikin.histogram.internal.HistogramSourceTypesEnum.DOUBLE
import org.gurikin.histogram.internal.HistogramSourceTypesEnum.FLOAT
import org.gurikin.histogram.internal.HistogramSourceTypesEnum.INT
import org.gurikin.histogram.internal.HistogramSourceTypesEnum.LONG

/**
 * API для работы непосредственно с гистограммами (построение, определение бина, аккумуляция данных)
 * Получает список чанков и аккумулирует их в общую гистограмму.
 * Устанавливает границы гистограммы из конфигурации
 * При старте использования при помощи `ChunkParameterBuilder` настраивает
 *  тип данных из источника и параметры чанков, такие как:
 *    - число чанков
 *    - размер каждого чанка
 */
interface HistogramBuilder<S : Comparable<S>> {
    /**
     * Init a histogram with manually defined binsCount
     *
     * @param xBorder [Border] histogram borders (from and to)
     * @param binsCount histogram bins count
     * @return
     */
    fun initHistogram(xBorder: Border<S>, binsCount: Int, yBorder: Border<S>? = null, zBorder: Border<S>? = null): Histogram<S>

    /**
     * Init a histogram with define binsCount by Sturges formula
     *
     * @param histogramConfiguration [HistogramConfiguration] histogram configuration
     *   (border [Border], step [S], type [HistogramSourceTypesEnum])
     * @return [Histogram] with source type [S]
     */
    fun initHistogram(histogramConfiguration: HistogramConfiguration<S>): Histogram<S>
}

/**
 * Алгоритм для одномерной гистограммы:
 *
 * 1. Находится номер бина, соответствующий значению `x`, с помощью метода `TAxis::FindBin`. Для равномерных бинов индекс вычисляется как `floor((x - xmin) / width) + 1` с учётом underflow/overflow. Для неравномерных бинов используется бинарный поиск по массиву границ.
 * 2. Увеличивается счётчик записей (`fEntries`).
 * 3. Вызывается виртуальный метод `AddBinContent(bin, weight)`, который в классах-наследниках (TH1C, TH1S, TH1I, TH1F, TH1D) инкрементирует соответствующее значение в массиве с проверкой на переполнение.
 * 4. Если включено хранение суммы квадратов весов (`Sumw2`), обновляется массив `fSumw2`.
 * 5. Обновляются статистические суммы (`fTsumw`, `fTsumw2`, `fTsumwx`, `fTsumwx2`) для вычисления среднего и среднеквадратичного отклонения.
 *
 * Для многомерных гистограмм (TH2, TH3) логика аналогична, только номер бина вычисляется как глобальный индекс по формуле: `bin = binx + nx * (biny + ny * binz)`, где `nx`, `ny` – число бинов по осям плюс underflow/overflow.
 *
 */
@Serializable
@OptIn(ExperimentalAtomicApi::class)

class Histogram<S : Comparable<S>> (
    val bins: List<Bin<S>>,
    private var totalFrameSum: Int,
    var average: Double = 0.0,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault
    var covariance: CovarianceMatrix3D = CovarianceMatrix3D(),
    @Transient
    private val _totalFrameSum: AtomicInt = AtomicInt(0),
    val type: HistogramSourceTypesEnum = HistogramSourceTypesEnum.INT,
) {

    fun initFrameSum() {
        _totalFrameSum.store(0)
        totalFrameSum = _totalFrameSum.load()
    }

    fun incrementFrameSum(incrementValue: Int) {
        totalFrameSum = _totalFrameSum.addAndFetch(incrementValue)
    }

    fun getFrameSum(): Int {
        return if (_totalFrameSum.load() == totalFrameSum) {
            totalFrameSum
        } else {
            throw RuntimeException("Histogram TotalFrameSum unsync")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun refreshAvg() {
        this.bins.filter { it.getFrameSum() != 0 }.forEach { bin ->
            val binAvg = when (bin.xBorder.type) {
                HistogramSourceTypesEnum.INT -> avg(
                    (bin.xBorder as Border<Int>).from,
                    bin.xBorder.to,
                    bin.getFrameSum(),
                    this.getFrameSum()
                )

                HistogramSourceTypesEnum.LONG -> avg(
                    (bin.xBorder as Border<Long>).from,
                    bin.xBorder.to,
                    bin.getFrameSum(),
                    this.getFrameSum()
                )

                HistogramSourceTypesEnum.FLOAT -> avg(
                    (bin.xBorder as Border<Float>).from,
                    bin.xBorder.to,
                    bin.getFrameSum(),
                    this.getFrameSum()
                )

                HistogramSourceTypesEnum.DOUBLE -> avg(
                    (bin.xBorder as Border<Double>).from,
                    bin.xBorder.to,
                    bin.getFrameSum(),
                    this.getFrameSum()
                )

                else -> throw UnsupportedOperationException("Unknown type for frame ${bin.xBorder.from::class}")
            }.toDouble()
            this.average += binAvg
        }
    }
}

fun <S : Comparable<S>> Histogram<S>.add(value: Frame<Point<S>>) {
    // TODO replace with binary search
    for (bin in this.bins) {
        if (bin.frameInBorder(value.value())) {
            incrementFrameSum(1)
            bin.addFrame()
            break
        }
    }
    this.bins.filter { it.getFrameSum() != 0 }.forEach { bin ->
        bin.setWeight(bin.getFrameSum().toDouble() / this.getFrameSum().toDouble())
    }
}

fun <S : Comparable<S>> Histogram<S>.clear() {
    // TODO replace with binary search
    this.bins.forEach {
        it.weight = 0.0
        it.initFrameSum()
    }
    this.average = 0.0
    this.initFrameSum()
}

@OptIn(ExperimentalAtomicApi::class)
fun <S : Comparable<S>> Histogram<S>.copy(): Histogram<S> {
    val binList = mutableListOf<Bin<S>>()
    this.bins.forEach {
        binList.add(it.copy())
    }
    return Histogram(
        bins = binList,
        totalFrameSum = this.getFrameSum(),
        average = this.average,
        _totalFrameSum = AtomicInt(this.getFrameSum()),
    )
}

@Serializable
@OptIn(ExperimentalAtomicApi::class)
class Bin<S : Comparable<S>>(
    val xBorder: Border<S>,
    val yBorder: Border<S>? = null,
    val zBorder: Border<S>? = null,
    var draftFrameSum: Int = 0,
    var weight: Double = 0.0,
    @Transient
    private val _frameSum: AtomicInt = AtomicInt(0),
    val type: HistogramSourceTypesEnum = HistogramSourceTypesEnum.INT,
) {

    fun initFrameSum() {
        _frameSum.store(0)
        draftFrameSum = _frameSum.load()
    }

    fun incrementFrameSum(incrementValue: Int) {
        draftFrameSum = _frameSum.addAndFetch(incrementValue)
    }

    fun getFrameSum(): Int {
        return if (_frameSum.load() == draftFrameSum) {
            draftFrameSum
        } else {
            throw RuntimeException("Bin FrameSum unsync")
        }
    }
}

@OptIn(ExperimentalAtomicApi::class)
fun <S : Comparable<S>> Bin<S>.copy(): Bin<S> = Bin(
    xBorder = this.xBorder,
    yBorder = this.yBorder,
    zBorder = this.zBorder,
    draftFrameSum = this.getFrameSum(),
    weight = this.weight,
    type = this.type,
    _frameSum = AtomicInt(this.getFrameSum())
)

fun <S : Comparable<S>> Bin<S>.frameInBorder(frame: Point<S>): Boolean {
    fun <S : Comparable<S>> checkBorder(border: Border<S>, frame: S): Boolean {
        return border.from <= frame && border.to >= frame
    }

    var frameInBorder: Boolean = true
    if (this.zBorder != null) {
        frameInBorder = frame.z?.let { checkBorder(this.zBorder, it) } ?: false
    }
    if (frameInBorder && this.yBorder != null) {
        frameInBorder = frame.y?.let { checkBorder(this.yBorder, it) } ?: false
    }
    if (frameInBorder) {
        frameInBorder = checkBorder(this.xBorder, frame.x)
    }
    return frameInBorder
}

fun <S : Comparable<S>> Bin<S>.addFrame() {
    this.incrementFrameSum(1)
}

fun <S : Comparable<S>> Bin<S>.setWeight(weight: Double) {
    this.weight = weight //BigDecimal(weight).setScale(4, RoundingMode.HALF_EVEN).toDouble()
}

fun <S : Comparable<S>> Bin<S>.chunkInBorder(chunk: Chunk<S>): Boolean =
    (chunk.histogram.bins.first().xBorder.from >= this.xBorder.from && chunk.histogram.bins.last().xBorder.to <= this.xBorder.to) &&
            (this.yBorder == null || chunk.histogram.bins.first().yBorder!!.from >= this.yBorder.from && chunk.histogram.bins.last().yBorder!!.to <= this.yBorder.to) &&
            (this.zBorder == null || chunk.histogram.bins.first().zBorder!!.from >= this.zBorder.from && chunk.histogram.bins.last().zBorder!!.to <= this.zBorder.to)

fun <S : Comparable<S>> Bin<S>.chunkLeftSideInBorder(chunk: Chunk<S>): Boolean =
    (chunk.histogram.bins.first().xBorder.from < this.xBorder.to && chunk.histogram.bins.last().xBorder.to > this.xBorder.to) &&
            (this.yBorder == null || chunk.histogram.bins.first().yBorder!!.from < this.yBorder.to && chunk.histogram.bins.last().yBorder!!.to > this.yBorder.to) &&
            (this.zBorder == null || chunk.histogram.bins.first().zBorder!!.from < this.zBorder.to && chunk.histogram.bins.last().zBorder!!.to > this.zBorder.to)

fun <S : Comparable<S>> Bin<S>.chunkRightSideInBorder(chunk: Chunk<S>): Boolean =
    (chunk.histogram.bins.first().xBorder.from < this.xBorder.from && chunk.histogram.bins.last().xBorder.to > this.xBorder.from) &&
            (this.yBorder == null || chunk.histogram.bins.first().yBorder!!.from < this.yBorder.from && chunk.histogram.bins.last().yBorder!!.to > this.yBorder.from) &&
            (this.zBorder == null || chunk.histogram.bins.first().zBorder!!.from < this.zBorder.from && chunk.histogram.bins.last().zBorder!!.to > this.zBorder.from)

fun <S : Comparable<S>> Bin<S>.binInBorder(otherBin: Bin<S>): Boolean =
    (otherBin.xBorder.from >= this.xBorder.from && otherBin.xBorder.to <= this.xBorder.to) &&
            (this.yBorder == null || otherBin.yBorder!!.from >= this.yBorder.from && otherBin.yBorder.to <= this.yBorder.to) &&
            (this.zBorder == null || otherBin.zBorder!!.from >= this.zBorder.from && otherBin.zBorder.to <= this.zBorder.to)

fun <S : Comparable<S>> Bin<S>.binIsCrossingBorder(otherBin: Bin<S>): Boolean {
    return (otherBin.xBorder.from < this.xBorder.from && otherBin.xBorder.to > this.xBorder.from) || (otherBin.xBorder.from < this.xBorder.to && otherBin.xBorder.to > this.xBorder.to) &&
            (this.yBorder == null || (otherBin.yBorder!!.from < this.yBorder.from && otherBin.yBorder.to > this.yBorder.from) || (otherBin.yBorder.from < this.yBorder.to && otherBin.yBorder.to > this.yBorder.to)) &&
            (this.zBorder == null || (otherBin.zBorder!!.from < this.zBorder.from && otherBin.zBorder.to > this.zBorder.from) || (otherBin.zBorder.from < this.zBorder.to && otherBin.zBorder.to > this.zBorder.to))
}


@Serializable
data class Border<S>(
    val from: Frame<S>,
    val to: Frame<S>,
    val type: HistogramSourceTypesEnum = INT
)

fun Border<Int>.borderIntLength(): Int = abs(this.to.value() - this.from.value() + 1)
fun Border<Long>.borderLongLength(): Long = abs(this.to.value() - this.from.value() + 1)
fun Border<Float>.borderFloatLength(): Float = abs(this.to.value() - this.from.value() + 1)
fun Border<Double>.borderDoubleLength(): Double = abs(this.to.value() - this.from.value() + 1)

fun Border<Int>.borderIntCenter(): Int = abs(this.to.value() + this.from.value() + 1) / 2
fun Border<Long>.borderLongCenter(): Long = abs(this.to.value() + this.from.value() + 1) / 2
fun Border<Float>.borderFloatCenter(): Float = abs(this.to.value() + this.from.value() + 1) / 2.0f
fun Border<Double>.borderDoubleCenter(): Double = abs(this.to.value() + this.from.value() + 1) / 2.0

@Suppress("UNCHECKED_CAST")
fun <S> Border<S>.getBorderLength(): Double {
    return when (this.type) {
        INT -> {
            (this as Border<Int>).borderIntLength()
        }

        LONG -> {
            (this as Border<Long>).borderLongLength()
        }

        FLOAT -> {
            (this as Border<Float>).borderFloatLength()
        }

        DOUBLE -> {
            (this as Border<Double>).borderDoubleLength()
        }

        else -> throw UnsupportedOperationException("Unknown type of histogram source")
    }.toDouble()
}

@Suppress("UNCHECKED_CAST")
fun <S> Border<S>.getBorderCenter(): Double {
    return when (this.type) {
        INT -> {
            (this as Border<Int>).borderIntCenter()
        }

        LONG -> {
            (this as Border<Long>).borderLongCenter()
        }

        FLOAT -> {
            (this as Border<Float>).borderFloatCenter()
        }

        DOUBLE -> {
            (this as Border<Double>).borderDoubleCenter()
        }

        else -> throw UnsupportedOperationException("Unknown type of histogram source")
    }.toDouble()
}