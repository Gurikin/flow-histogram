package org.gurikin.histogram.internal

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.abs
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
     * @param border [Border] histogram borders (from and to)
     * @param binsCount histogram bins count
     * @return
     */
    fun initHistogram(border: Border<S>, binsCount: Int): Histogram<S>

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
class Histogram<S : Comparable<S>>(
    val bins: List<Bin<S>>,
    private var totalFrameSum: Int,
    var average: Double = 0.0,
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
        this.bins.forEach { bin ->
            val binAvg = when (bin.border.type) {
                HistogramSourceTypesEnum.INT -> avg(
                    (bin.border as Border<Int>).from,
                    bin.border.to,
                    bin.getFrameSum(),
                    this.getFrameSum()
                )

                HistogramSourceTypesEnum.LONG -> avg(
                    (bin.border as Border<Long>).from,
                    bin.border.to,
                    bin.getFrameSum(),
                    this.getFrameSum()
                )

                HistogramSourceTypesEnum.FLOAT -> avg(
                    (bin.border as Border<Float>).from,
                    bin.border.to,
                    bin.getFrameSum(),
                    this.getFrameSum()
                )

                HistogramSourceTypesEnum.DOUBLE -> avg(
                    (bin.border as Border<Double>).from,
                    bin.border.to,
                    bin.getFrameSum(),
                    this.getFrameSum()
                )

                else -> throw UnsupportedOperationException("Unknown type for frame ${bin.border.from::class}")
            }.toDouble()
            this.average += binAvg
        }
    }
}

fun <S : Comparable<S>> Histogram<S>.add(value: Frame<S>) {
    // TODO replace with binary search
    for (bin in this.bins) {
        if (bin.frameInBorder(value)) {
            incrementFrameSum(1)
            bin.addFrame()
            break
        }
    }
    this.bins.forEach { bin ->
        bin.setWeight(bin.getFrameSum().toDouble() / this.getFrameSum())
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
    val border: Border<S>,
    private var frameSum: Int = 0,
    internal var weight: Double = 0.0,
    @Transient
    private val _frameSum: AtomicInt = AtomicInt(0),
    val type: HistogramSourceTypesEnum = HistogramSourceTypesEnum.INT,
) {

    fun initFrameSum() {
        _frameSum.store(0)
        frameSum = _frameSum.load()
    }

    fun incrementFrameSum(incrementValue: Int) {
        frameSum = _frameSum.addAndFetch(incrementValue)
    }

    fun getFrameSum(): Int {
        return if (_frameSum.load() == frameSum) {
            frameSum
        } else {
            throw RuntimeException("Bin FrameSum unsync")
        }
    }
}

@OptIn(ExperimentalAtomicApi::class)
fun <S : Comparable<S>> Bin<S>.copy(): Bin<S> = Bin(
    border = this.border,
    frameSum = this.getFrameSum(),
    weight = this.weight,
    _frameSum = AtomicInt(this.getFrameSum())
)

fun <S : Comparable<S>> Bin<S>.frameInBorder(frame: Frame<S>): Boolean =
    (this.border.from <= frame && this.border.to >= frame)

fun <S : Comparable<S>> Bin<S>.addFrame() {
    this.incrementFrameSum(1)
}

fun <S : Comparable<S>> Bin<S>.setWeight(weight: Double) {
    this.weight = BigDecimal(weight).setScale(4, RoundingMode.HALF_EVEN).toDouble()
}

fun <S : Comparable<S>> Bin<S>.chunkInBorder(chunk: Chunk<S>): Boolean =
    (chunk.histogram.bins.first().border.from >= this.border.from && chunk.histogram.bins.last().border.to <= this.border.to)

fun <S : Comparable<S>> Bin<S>.chunkLeftSideInBorder(chunk: Chunk<S>): Boolean =
    (chunk.histogram.bins.first().border.from < this.border.to && chunk.histogram.bins.last().border.to > this.border.to)

fun <S : Comparable<S>> Bin<S>.chunkRightSideInBorder(chunk: Chunk<S>): Boolean =
    (chunk.histogram.bins.first().border.from < this.border.from && chunk.histogram.bins.last().border.to > this.border.from)

fun <S : Comparable<S>> Bin<S>.binInBorder(otherBin: Bin<S>): Boolean =
    (otherBin.border.from >= this.border.from && otherBin.border.to <= this.border.to)

fun <S : Comparable<S>> Bin<S>.binIsCrossingBorder(otherBin: Bin<S>): Boolean =
    (otherBin.border.from < this.border.from && otherBin.border.to > this.border.from) || (otherBin.border.from < this.border.to && otherBin.border.to > this.border.to)

@Serializable
data class Border<S>(
    val from: Frame<S>,
    val to: Frame<S>,
    val type: HistogramSourceTypesEnum = HistogramSourceTypesEnum.INT
)

fun Border<Int>.borderIntLenght(): Int = abs(this.to.value() - this.from.value() + 1)
fun Border<Long>.borderLongLenght(): Long = abs(this.to.value() - this.from.value() + 1)
fun Border<Float>.borderFloatLenght(): Float = abs(this.to.value() - this.from.value() + 1)
fun Border<Double>.borderDoubleLenght(): Double = abs(this.to.value() + 1)

fun Border<Int>.borderIntCenter(): Int = abs(this.to.value() - this.from.value() + 1) / 2
fun Border<Long>.borderLongCenter(): Long = abs(this.to.value() - this.from.value() + 1) / 2
fun Border<Float>.borderFloatCenter(): Float = abs(this.to.value() - this.from.value() + 1) / 2.0f
fun Border<Double>.borderDoubleCenter(): Double = abs(this.to.value() + 1) / 2.0