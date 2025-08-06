
package com.example.habittracker

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import java.io.InputStream
import java.io.OutputStream

class ExcelExporter(private val context: Context) {

    fun exportToFolder(folderUri: Uri, registros: List<Registro>): Boolean {
        val resolver: ContentResolver = context.contentResolver
        // Create or overwrite file "registro_diario.xlsx"
        val docUri = android.provider.DocumentsContract.createDocument(
            resolver, folderUri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "registro_diario.xlsx"
        )
        val out: OutputStream = resolver.openOutputStream(docUri!!, "w")!!

        // Load template from assets
        val ins: InputStream = context.assets.open("registro_diario_template.xlsx")
        val wb = XSSFWorkbook(ins)
        val sheet = wb.getSheet("Registro")

        // Clear existing rows (except header)
        var last = sheet.lastRowNum
        while (last > 0) {
            sheet.removeRow(sheet.getRow(last))
            last--
        }

        // Write rows
        var rowIdx = 1
        val dateStyle: CellStyle = wb.createCellStyle().apply {
            alignment = HorizontalAlignment.LEFT
        }
        for (r in registros) {
            val row = sheet.createRow(rowIdx++)
            row.createCell(0).setCellValue(r.fecha)
            row.createCell(1).setCellValue(r.entrante ?: "")
            row.createCell(2).setCellValue(r.principal ?: "")
            row.createCell(3).setCellValue(r.postre ?: "")
            row.createCell(4).setCellValue((r.esfuerzo ?: 0).toDouble())
            row.createCell(5).setCellValue((r.horas ?: 0.0))
            row.createCell(6).setCellValue((r.lecturaMin ?: 0).toDouble())
        }

        wb.write(out)
        out.flush()
        out.close()
        wb.close()
        ins.close()
        return true
    }
}
