package fr.driffaud.band2fit

data class Datapoint(val timestamp: Long, val rawIntensity: Int, val steps: Int, val rawKind: Int, val heartRate: Int)