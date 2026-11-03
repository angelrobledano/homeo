package com.salud.personal.data.gpx

import com.salud.personal.domain.gps.GpsLocationPoint
import java.io.InputStream
import java.io.StringWriter

object GpxParser {

    /**
     * Exporta una lista de puntos GPS a formato GPX estándar (XML).
     */
    fun exportToGpx(trackName: String, points: List<GpsLocationPoint>): String {
        val writer = StringWriter()
        writer.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        writer.append("""<gpx version="1.1" creator="SaludPersonal" xmlns="http://www.topografix.com/GPX/1/1">""").append("\n")
        writer.append("  <trk>\n")
        writer.append("    <name>").append(trackName).append("</name>\n")
        writer.append("    <trkseg>\n")

        for (pt in points) {
            writer.append("""      <trkpt lat="${pt.latitude}" lon="${pt.longitude}">""").append("\n")
            writer.append("        <ele>").append(pt.altitudeMeters.toString()).append("</ele>\n")
            writer.append("      </trkpt>\n")
        }

        writer.append("    </trkseg>\n")
        writer.append("  </trk>\n")
        writer.append("</gpx>\n")
        return writer.toString()
    }

    /**
     * Lee un stream GPX y extrae las coordenadas de los trackpoints.
     */
    fun parseGpx(inputStream: InputStream): List<GpsLocationPoint> {
        val points = mutableListOf<GpsLocationPoint>()
        val text = inputStream.bufferedReader().use { it.readText() }

        // Extracción regex robusta y ligera de lat/lon sin dependencias pesadas de XML
        val trkptRegex = """<trkpt\s+lat="([^"]+)"\s+lon="([^"]+)"""".toRegex()
        val matches = trkptRegex.findAll(text)

        for (match in matches) {
            val lat = match.groupValues[1].toDoubleOrNull() ?: continue
            val lon = match.groupValues[2].toDoubleOrNull() ?: continue
            points.add(GpsLocationPoint(latitude = lat, longitude = lon))
        }

        return points
    }
}
