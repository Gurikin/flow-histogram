package org.gurikin.histogram.internal

import kotlinx.serialization.Serializable

interface IPoint<S : Comparable<S>> {
    fun dimension(): PointDimension
    operator fun compareTo(other: Point<S>): Int
}

@Serializable
class Point<S : Comparable<S>>(
    val x: S,
    val y: S? = null,
    val z: S? = null,
) : IPoint<S>, Comparable<Point<S>> {
    init {
        if (z != null && y == null) {
            throw IllegalStateException("Possible combinations: [x], [x,y], [x,y,z]. Combination of coordinates [x,z] not allow")
        }
    }

    override fun dimension(): PointDimension =
        when {
            z == null -> PointDimension.POINT_2D
            y == null -> PointDimension.POINT_1D
            else -> PointDimension.POINT_3D
        }

    override fun compareTo(other: Point<S>): Int {
        if (this.dimension() != other.dimension()) {
            throw IllegalStateException("Could not compare point with different dimensions")
        }
        if (this.dimension() == PointDimension.POINT_1D) {
            return this.x.compareTo(other.x)
        }
        if (this.dimension() == PointDimension.POINT_2D) {
            return when {
                this.x < other.x -> -1
                this.y!! < other.y!! -> -1
                this.x == other.x && this.y == other.y -> 0
                else -> 1
            }
        }
        return when {
            this.x < other.x -> -1
            this.y!! < other.y!! -> -1
            this.z!! < other.z!! -> -1
            this.x == other.x && this.y == other.y && this.z == other.z -> 0
            else -> 1
        }
    }
}

enum class PointDimension {
    POINT_1D,
    POINT_2D,
    POINT_3D,
}