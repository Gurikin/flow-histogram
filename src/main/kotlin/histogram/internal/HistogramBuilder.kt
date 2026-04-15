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
interface HistogramBuilder<S: Comparable<S>> {
    fun initHistogram(border: Border<S>, binsCount: Int): Histogram<S>
    fun add(value: Frame<S>)
}

@Serializable
data class HistogramConfiguration<S: Comparable<S>>(
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
public open class Histogram<S: Comparable<S>>(
    private val bins: List<Bin<S>>,
)

@Serializable
class Bin<S: Comparable<S>>(val border: Border<S>) {
    private val sum: Int = 0
    private val weight: Double = 0.0

    fun frameInBorder(frame: Frame<S>): Boolean = throw UnsupportedOperationException("Need to implement for particular type `S`")
}

@Serializable
data class Border<S: Comparable<S>>(val from: S, val to: S)