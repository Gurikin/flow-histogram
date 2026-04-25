package org.gurikin.histogram.internal

import kotlinx.serialization.Serializable

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
    fun initHistogram(border: Border<S>, binsCount: Int): Histogram<S>
}

//fun HistogramBuilder<*>.createBuilder(): HistogramBuilder<*> {
//    when {
//        this.value is Int -> return IntHistogramBuilder()
//        else -> throw UnsupportedOperationException("HistogramBuilder for type ${this.value::class.qualifiedName} not implemented yet")
//    }
//}

@Serializable
data class HistogramConfiguration<S : Comparable<S>>(
    val histogramBorder: Border<S>,
    val binsCount: Int,
)

/**
 * Алгоритм для одномерной гистограммы:
 *
 * 1. Находится номер бина, соответствующий значению `x`, с помощью метода `TAxis::FindBin`. Для равномерных бинов индекс вычисляется как `floor((x - xmin) / width) + 1` с учётом underflow/overflow. Для неравномерных бинов используется бинарный поиск по массиву границ.
 *
 * 2. Увеличивается счётчик записей (`fEntries`).
 *
 * 3. Вызывается виртуальный метод `AddBinContent(bin, weight)`, который в классах-наследниках (TH1C, TH1S, TH1I, TH1F, TH1D) инкрементирует соответствующее значение в массиве с проверкой на переполнение.
 *
 * 4. Если включено хранение суммы квадратов весов (`Sumw2`), обновляется массив `fSumw2`.
 *
 * 5. Обновляются статистические суммы (`fTsumw`, `fTsumw2`, `fTsumwx`, `fTsumwx2`) для вычисления среднего и среднеквадратичного отклонения.
 *
 *
 * Для многомерных гистограмм (TH2, TH3) логика аналогична, только номер бина вычисляется как глобальный индекс по формуле: `bin = binx + nx * (biny + ny * binz)`, где `nx`, `ny` – число бинов по осям плюс underflow/overflow.
 *
 */
@Serializable
class Histogram<S : Comparable<S>>(
    val bins: List<Bin<S>>,
    var totalFrameSum: Int,
)

fun <S : Comparable<S>> Histogram<S>.add(value: Frame<S>) {
    // TODO replace with binary search
    for (bin in this.bins) {
        if (bin.frameInBorder(value)) {
            this.totalFrameSum += 1
            bin.addFrame(value)
        }
    }
    this.bins.forEach { bin ->
        bin.setWeight(bin.frameSum.toDouble() / this.totalFrameSum)
    }
}

@Serializable
class Bin<S : Comparable<S>>(val border: Border<S>) {
    internal var frameSum: Int = 0
    internal var weight: Double = 0.0
}

fun <S : Comparable<S>> Bin<S>.frameInBorder(frame: Frame<S>): Boolean =
    (frame.value < this.border.to && frame.value >= this.border.from)

fun <S : Comparable<S>> Bin<S>.addFrame(frame: Frame<S>) {
    this.frameSum += 1
}

fun <S : Comparable<S>> Bin<S>.setWeight(weight: Double) {
    this.weight = weight
}

fun <S : Comparable<S>> Bin<S>.chunkInBorder(chunk: Chunk<S>): Boolean =
    (chunk.histogram.bins.first().border.from > this.border.from && chunk.histogram.bins.last().border.to <= this.border.to)

fun <S : Comparable<S>> Bin<S>.chunkLeftSideInBorder(chunk: Chunk<S>): Boolean =
    (chunk.histogram.bins.first().border.from < this.border.to && chunk.histogram.bins.last().border.to > this.border.to)

fun <S : Comparable<S>> Bin<S>.chunkRightSideInBorder(chunk: Chunk<S>): Boolean =
    (chunk.histogram.bins.first().border.from < this.border.from && chunk.histogram.bins.last().border.to > this.border.from)

fun <S : Comparable<S>> Bin<S>.binInBorder(otherBin: Bin<S>): Boolean =
    (otherBin.border.from > this.border.from && otherBin.border.to <= this.border.to)

fun <S : Comparable<S>> Bin<S>.binIsCrossingBorder(otherBin: Bin<S>): Boolean =
    (otherBin.border.from < this.border.from && otherBin.border.to > this.border.from) || (otherBin.border.from < this.border.to && otherBin.border.to > this.border.to)

@Serializable
data class Border<S : Comparable<S>>(val from: S, val to: S)