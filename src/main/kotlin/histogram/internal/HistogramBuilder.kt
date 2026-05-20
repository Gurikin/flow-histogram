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
    fun initHistogram(border: Border<HistogramSourceData<S>>, binsCount: Int): Histogram<S>

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
    @Transient
    private val _totalFrameSum: AtomicInt = AtomicInt(0)
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
}

fun <S : Comparable<S>> Histogram<S>.add(value: HistogramSourceData<S>) {
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
        _totalFrameSum = AtomicInt(this.getFrameSum())
    )
}

@Serializable
@OptIn(ExperimentalAtomicApi::class)
class Bin<S : Comparable<S>>(
    val border: Border<HistogramSourceData<S>>,
    private var frameSum: Int = 0,
    internal var weight: Double = 0.0,
    @Transient
    private val _frameSum: AtomicInt = AtomicInt(0),
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

fun <S : Comparable<S>> Bin<S>.frameInBorder(frame: HistogramSourceData<S>): Boolean =
    (this.border.from <= frame.value && this.border.to >= frame.value)

fun <S : Comparable<S>> Bin<S>.addFrame() {
    this.incrementFrameSum(1)
}

fun <S : Comparable<S>> Bin<S>.setWeight(weight: Double) {
    this.weight = BigDecimal(weight).setScale(4, RoundingMode.HALF_EVEN).toDouble()
}

fun <S : Comparable<S>> Bin<S>.chunkInBorder(chunk: Chunk<S>): Boolean =
    (chunk.histogram.bins.first().border.from >= this.border.from.value && chunk.histogram.bins.last().border.to <= this.border.to.value)

fun <S : Comparable<S>> Bin<S>.chunkLeftSideInBorder(chunk: Chunk<S>): Boolean =
    (chunk.histogram.bins.first().border.from < this.border.to.value && chunk.histogram.bins.last().border.to > this.border.to.value)

fun <S : Comparable<S>> Bin<S>.chunkRightSideInBorder(chunk: Chunk<S>): Boolean =
    (chunk.histogram.bins.first().border.from < this.border.from.value && chunk.histogram.bins.last().border.to > this.border.from.value)

fun <S : Comparable<S>> Bin<S>.binInBorder(otherBin: Bin<S>): Boolean =
    (otherBin.border.from >= this.border.from.value && otherBin.border.to <= this.border.to.value)

fun <S : Comparable<S>> Bin<S>.binIsCrossingBorder(otherBin: Bin<S>): Boolean =
    (otherBin.border.from < this.border.from.value && otherBin.border.to > this.border.from.value) || (otherBin.border.from < this.border.to.value && otherBin.border.to > this.border.to.value)

@Serializable
data class Border<S>(val from: S, val to: S)

fun Border<HistogramSourceData<Int>>.borderIntLenght(): Int = abs(this.to.value - this.from.value + 1)
fun Border<HistogramSourceData<Long>>.borderLongLenght(): Long = abs(this.to.value - this.from.value + 1)
fun Border<HistogramSourceData<Float>>.borderFloatLenght(): Float = abs(this.to.value - this.from.value + 1)
fun Border<HistogramSourceData<Double>>.borderDoubleLenght(): Double = abs(this.to.value - this.from.value + 1)

//fun Border<Int>.borderLength(): Int = abs(this.to - this.from + 1)
//fun Border<Long>.borderLength(): Long = abs(this.to - this.from + 1)
//fun Border<Float>.borderLength(): Float = abs(this.to - this.from + 1)
//fun Border<Double>.borderLength(): Double = abs(this.to - this.from + 1)