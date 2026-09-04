package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.MasterAkunSaldo
import com.example.data.model.MasterPelanggan
import com.example.data.model.MutasiManualKeluarMasuk
import com.example.data.model.TransaksiOrderMasuk
import com.example.ui.screens.AuditRecord
import com.example.ui.screens.AuditStorageHelper
import com.example.ui.viewmodel.AccountDashboardRow
import com.example.ui.viewmodel.AllocationComparisonItem
import com.example.ui.viewmodel.DashboardSummary
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.max

/**
 * ReportExportManager
 * Comprehensive and professionally styled multi-tab Excel (.xlsx) and multi-page PDF exporter
 * for Pradipta Graha Digital (PGD Order) financial management system.
 */
object ReportExportManager {

    private val rupiahFormat: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        DecimalFormat("Rp #,##0", symbols)
    }

    private val numberFormat: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        DecimalFormat("#,##0", symbols)
    }

    fun formatRupiah(amount: Double): String {
        return rupiahFormat.format(amount)
    }

    fun formatNumber(amount: Number): String {
        return numberFormat.format(amount)
    }

    fun formatPercent(amount: Double): String {
        return String.format(Locale.US, "%.1f%%", amount)
    }

    /**
     * Compute Pos Budget Allocation comparison items for a specific date range and dataset.
     */
    fun computeAllocationItems(
        accounts: List<MasterAkunSaldo>,
        orders: List<TransaksiOrderMasuk>,
        mutations: List<MutasiManualKeluarMasuk>
    ): List<AllocationComparisonItem> {
        val ordersWithPayment = orders.filter { it.effectiveJumlahDibayar > 0.0 }
        
        val kertasHpp = accounts.find { it.namaAkun.contains("Kertas", ignoreCase = true) }?.konstanHppUnit?.toDouble() ?: 106.0
        val tintaHpp = accounts.find { it.namaAkun.contains("Tinta", ignoreCase = true) }?.konstanHppUnit?.toDouble() ?: 25.0
        val pengemasanHpp = accounts.find { it.namaAkun.contains("Pengemasan", ignoreCase = true) }?.konstanHppUnit?.toDouble() ?: 300.0
        val wastePct = accounts.find { it.namaAkun.contains("Waste", ignoreCase = true) }?.persentaseOperasional?.toDouble() ?: 0.05
        val tenagaKerjaPct = accounts.find { it.namaAkun.contains("Tenaga Kerja", ignoreCase = true) }?.persentaseOperasional?.toDouble() ?: 0.07
        val listrikPct = accounts.find { it.namaAkun.contains("Listrik", ignoreCase = true) }?.persentaseOperasional?.toDouble() ?: 0.02
        val maintenancePct = accounts.find { it.namaAkun.contains("Maintenance", ignoreCase = true) }?.persentaseOperasional?.toDouble() ?: 0.05

        return accounts.map { account ->
            val name = account.namaAkun
            val masukPlotting = when {
                name.contains("Kertas", ignoreCase = true) -> ordersWithPayment.sumOf { it.qtyOrder.toDouble() * kertasHpp * it.paymentRatio }
                name.contains("Tinta", ignoreCase = true) -> ordersWithPayment.sumOf { it.qtyOrder.toDouble() * tintaHpp * it.paymentRatio }
                name.contains("Pengemasan", ignoreCase = true) -> ordersWithPayment.sumOf { it.jumlahPlastikPengemasan.toDouble() * pengemasanHpp * it.paymentRatio }
                name.contains("Waste", ignoreCase = true) -> ordersWithPayment.sumOf { wastePct * it.effectiveJumlahDibayar }
                name.contains("Tenaga Kerja", ignoreCase = true) -> ordersWithPayment.sumOf { tenagaKerjaPct * it.effectiveJumlahDibayar }
                name.contains("Listrik", ignoreCase = true) -> ordersWithPayment.sumOf { listrikPct * it.effectiveJumlahDibayar }
                name.contains("Maintenance", ignoreCase = true) -> ordersWithPayment.sumOf { maintenancePct * it.effectiveJumlahDibayar }
                name.contains("Laba", ignoreCase = true) -> ordersWithPayment.sumOf { order ->
                    val paid = order.effectiveJumlahDibayar
                    val ratio = order.paymentRatio
                    val alokasiKertasVal = order.qtyOrder.toDouble() * kertasHpp * ratio
                    val alokasiTintaVal = order.qtyOrder.toDouble() * tintaHpp * ratio
                    val alokasiPengemasanVal = order.jumlahPlastikPengemasan.toDouble() * pengemasanHpp * ratio
                    val alokasiWasteVal = wastePct * paid
                    val alokasiTenagaKerjaVal = tenagaKerjaPct * paid
                    val alokasiListrikVal = listrikPct * paid
                    val alokasiMaintenanceVal = maintenancePct * paid
                    val totalModalDasar = alokasiKertasVal + alokasiTintaVal + alokasiPengemasanVal + alokasiWasteVal + alokasiTenagaKerjaVal + alokasiListrikVal + alokasiMaintenanceVal
                    paid - totalModalDasar
                }
                else -> 0.0
            }

            val mutasiMasuk = mutations.filter {
                (it.jenisMutasi == "Uang Masuk" && it.idAkun == account.idAkun) ||
                (it.jenisMutasi == "Pindah Saldo" && it.idAkunTujuan == account.idAkun)
            }.sumOf { it.nominal }

            val totalMasuk = masukPlotting + mutasiMasuk

            val keluarRiil = mutations.filter {
                (it.jenisMutasi == "Uang Keluar" && it.idAkun == account.idAkun) ||
                (it.jenisMutasi == "Pindah Saldo" && it.idAkun == account.idAkun)
            }.sumOf { it.nominal }

            val sisa = totalMasuk - keluarRiil
            val serapanPct = if (totalMasuk > 0.0) (keluarRiil / totalMasuk) * 100.0 else if (keluarRiil > 0.0) 100.0 else 0.0

            AllocationComparisonItem(
                idAkun = account.idAkun,
                namaAkun = account.namaAkun,
                totalMasukPlotting = totalMasuk,
                totalKeluarRiil = keluarRiil,
                sisaSaldo = sisa,
                persentaseSerapan = serapanPct
            )
        }
    }

    /**
     * Save generated file to app storage and public Downloads folder, returning shareable Content Uri.
     */
    fun saveExportedFile(
        context: Context,
        fileName: String,
        mimeType: String,
        writeBlock: (OutputStream) -> Unit
    ): Uri? {
        val exportsDir = File(context.filesDir, "exports")
        if (!exportsDir.exists()) {
            exportsDir.mkdirs()
        }
        val localFile = File(exportsDir, fileName)
        try {
            FileOutputStream(localFile).use { fos ->
                writeBlock(fos)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal menulis berkas lokal: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            return null
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PGD Order")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        FileInputStream(localFile).use { fis ->
                            fis.copyTo(os)
                        }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PGD Order")
                if (!publicDir.exists()) {
                    publicDir.mkdirs()
                }
                val publicFile = File(publicDir, fileName)
                FileOutputStream(publicFile).use { fos ->
                    FileInputStream(localFile).use { fis ->
                        fis.copyTo(fos)
                    }
                }
                MediaScannerConnection.scanFile(context, arrayOf(publicFile.absolutePath), null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", localFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // =========================================================================
    // 1. EXCEL (.XLSX) EXPORT IMPLEMENTATION (Multi-Tab OpenXML SpreadsheetML)
    // =========================================================================

    fun exportToExcel(
        context: Context,
        startDate: String,
        endDate: String,
        orders: List<TransaksiOrderMasuk>,
        mutations: List<MutasiManualKeluarMasuk>,
        accounts: List<MasterAkunSaldo>,
        pelangganList: List<MasterPelanggan>,
        onExportSuccess: (String, Uri, String) -> Unit
    ) {
        val fileName = "Laporan_Pembukuan_PGD_${startDate}_to_${endDate}.xlsx"
        val mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        
        val auditRecords = AuditStorageHelper.loadAuditHistory(context)
        val pelangganMap = pelangganList.associateBy { it.idPelanggan }
        val accountMap = accounts.associateBy { it.idAkun }
        val posItems = computeAllocationItems(accounts, orders, mutations)

        val totalUnits = orders.sumOf { it.qtyOrder }
        val totalOmzet = orders.sumOf { it.qtyOrder.toDouble() * it.hargaSatuan }
        val totalMutationOut = mutations.filter { it.jenisMutasi == "Uang Keluar" }.sumOf { it.nominal }
        val totalMutationIn = mutations.filter { it.jenisMutasi == "Uang Masuk" }.sumOf { it.nominal }
        val grandTotalMasukPlotting = posItems.sumOf { it.totalMasukPlotting }
        val grandTotalKeluarRiil = posItems.sumOf { it.totalKeluarRiil }
        val grandTotalSisaRiil = posItems.sumOf { it.sisaSaldo }

        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("id", "ID")).format(Date())

        val uri = saveExportedFile(context, fileName, mimeType) { outputStream ->
            ZipOutputStream(outputStream).use { zip ->
                // 1. [Content_Types].xml
                zip.putNextEntry(ZipEntry("[Content_Types].xml"))
                zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""".toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 2. _rels/.rels
                zip.putNextEntry(ZipEntry("_rels/.rels"))
                zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 3. xl/_rels/workbook.xml.rels
                zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
                zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 4. xl/workbook.xml
                zip.putNextEntry(ZipEntry("xl/workbook.xml"))
                zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Ringkasan &amp; Pos Dompet" sheetId="1" r:id="rId1"/>
    <sheet name="Riwayat Order &amp; Nota" sheetId="2" r:id="rId2"/>
    <sheet name="Mutasi &amp; Audit Kas" sheetId="3" r:id="rId3"/>
  </sheets>
</workbook>""".toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 5. xl/styles.xml
                zip.putNextEntry(ZipEntry("xl/styles.xml"))
                zip.write(buildStylesXml().toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 6. xl/worksheets/sheet1.xml (Ringkasan & Pos Dompet)
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
                zip.write(buildSheet1Xml(
                    startDate, endDate, nowStr, totalUnits, totalOmzet, totalMutationOut,
                    totalMutationIn, grandTotalMasukPlotting, grandTotalKeluarRiil,
                    grandTotalSisaRiil, posItems
                ).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 7. xl/worksheets/sheet2.xml (Riwayat Order & Nota)
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet2.xml"))
                zip.write(buildSheet2Xml(startDate, endDate, orders, pelangganMap).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 8. xl/worksheets/sheet3.xml (Mutasi & Audit Kas)
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet3.xml"))
                zip.write(buildSheet3Xml(startDate, endDate, mutations, accountMap, auditRecords).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }

        if (uri != null) {
            onExportSuccess(fileName, uri, mimeType)
        } else {
            Toast.makeText(context, "Gagal mengekspor berkas Excel", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildStylesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="3">
    <numFmt numFmtId="164" formatCode="&quot;Rp &quot;#,##0;[Red]-&quot;Rp &quot;#,##0;&quot;Rp 0&quot;"/>
    <numFmt numFmtId="165" formatCode="0.0%"/>
    <numFmt numFmtId="166" formatCode="#,##0"/>
  </numFmts>
  <fonts count="7">
    <!-- 0: Regular 10pt Arial -->
    <font><sz val="10"/><name val="Arial"/></font>
    <!-- 1: Title 16pt Bold #4A3B6C -->
    <font><b/><sz val="16"/><color rgb="FF4A3B6C"/><name val="Arial"/></font>
    <!-- 2: Subtitle 10pt Italic #554B6E -->
    <font><i/><sz val="10"/><color rgb="FF554B6E"/><name val="Arial"/></font>
    <!-- 3: Table Header 11pt Bold White -->
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Arial"/></font>
    <!-- 4: Total / Subtotal 10pt Bold #3B2369 -->
    <font><b/><sz val="10"/><color rgb="FF3B2369"/><name val="Arial"/></font>
    <!-- 5: Section Header 12pt Bold #4A3B6C -->
    <font><b/><sz val="12"/><color rgb="FF4A3B6C"/><name val="Arial"/></font>
    <!-- 6: Card Label 9pt Bold Gray -->
    <font><b/><sz val="9"/><color rgb="FF7C7094"/><name val="Arial"/></font>
  </fonts>
  <fills count="7">
    <!-- 0: none -->
    <fill><patternFill patternType="none"/></fill>
    <!-- 1: gray125 -->
    <fill><patternFill patternType="gray125"/></fill>
    <!-- 2: Header Deep Purple #4A3B6C -->
    <fill><patternFill patternType="solid"><fgColor rgb="FF4A3B6C"/><bgColor indexed="64"/></patternFill></fill>
    <!-- 3: Zebra Tint #F5F2F9 -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFF5F2F9"/><bgColor indexed="64"/></patternFill></fill>
    <!-- 4: Total Row Soft Lavender #E8E0F0 -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFE8E0F0"/><bgColor indexed="64"/></patternFill></fill>
    <!-- 5: Metric Card Fill #F1ECF8 -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFF1ECF8"/><bgColor indexed="64"/></patternFill></fill>
    <!-- 6: Secondary Header #5C4B82 -->
    <fill><patternFill patternType="solid"><fgColor rgb="FF5C4B82"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="4">
    <!-- 0: None -->
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <!-- 1: Standard Thin Border #D1C8DF -->
    <border>
      <left style="thin"><color rgb="FFD1C8DF"/></left>
      <right style="thin"><color rgb="FFD1C8DF"/></right>
      <top style="thin"><color rgb="FFD1C8DF"/></top>
      <bottom style="thin"><color rgb="FFD1C8DF"/></bottom>
    </border>
    <!-- 2: Total Double Bottom Border #4A3B6C -->
    <border>
      <left style="thin"><color rgb="FFD1C8DF"/></left>
      <right style="thin"><color rgb="FFD1C8DF"/></right>
      <top style="thin"><color rgb="FFD1C8DF"/></top>
      <bottom style="double"><color rgb="FF4A3B6C"/></bottom>
    </border>
    <!-- 3: Metric Card Border -->
    <border>
      <left style="medium"><color rgb="FFBFAFD3"/></left>
      <right style="medium"><color rgb="FFBFAFD3"/></right>
      <top style="medium"><color rgb="FFBFAFD3"/></top>
      <bottom style="medium"><color rgb="FFBFAFD3"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="17">
    <!-- 0: Default Normal Left (no border) -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <!-- 1: Title (16pt Bold #4A3B6C) -->
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <!-- 2: Subtitle (10pt Italic #554B6E) -->
    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <!-- 3: Table Header (#4A3B6C Fill, Bold White Font, Thin Border, Center) -->
    <xf numFmtId="0" fontId="3" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 4: Data Normal Left with Border -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="left" vertical="center"/>
    </xf>
    <!-- 5: Data Zebra Left with Border (#F5F2F9) -->
    <xf numFmtId="0" fontId="0" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="left" vertical="center"/>
    </xf>
    <!-- 6: Data Normal Center with Border -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- 7: Data Zebra Center with Border -->
    <xf numFmtId="0" fontId="0" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- 8: Currency Normal Right with Border -->
    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 9: Currency Zebra Right with Border -->
    <xf numFmtId="164" fontId="0" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 10: Percentage Normal Right with Border -->
    <xf numFmtId="165" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 11: Percentage Zebra Right with Border -->
    <xf numFmtId="165" fontId="0" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 12: Integer Normal Right with Border -->
    <xf numFmtId="166" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 13: Integer Zebra Right with Border -->
    <xf numFmtId="166" fontId="0" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 14: Total Row Label (Bold #3B2369, #E8E0F0 Fill, Double Bottom Border) -->
    <xf numFmtId="0" fontId="4" fillId="4" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="left" vertical="center"/>
    </xf>
    <!-- 15: Total Row Currency (Bold #3B2369, #E8E0F0 Fill, Double Bottom Border) -->
    <xf numFmtId="164" fontId="4" fillId="4" borderId="2" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 16: Section Header (12pt Bold #4A3B6C) -->
    <xf numFmtId="0" fontId="5" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
</styleSheet>"""
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun buildSheet1Xml(
        startDate: String,
        endDate: String,
        nowStr: String,
        totalUnits: Int,
        totalOmzet: Double,
        totalMutationOut: Double,
        totalMutationIn: Double,
        grandMasuk: Double,
        grandKeluar: Double,
        grandSisa: Double,
        posItems: List<AllocationComparisonItem>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <cols>
    <col min="1" max="1" width="8" customWidth="1"/>
    <col min="2" max="2" width="28" customWidth="1"/>
    <col min="3" max="3" width="24" customWidth="1"/>
    <col min="4" max="4" width="22" customWidth="1"/>
    <col min="5" max="5" width="22" customWidth="1"/>
    <col min="6" max="6" width="22" customWidth="1"/>
    <col min="7" max="7" width="16" customWidth="1"/>
    <col min="8" max="8" width="20" customWidth="1"/>
  </cols>
  <sheetData>
""")

        var rowIdx = 1

        // Row 1: Title
        sb.append("""    <row r="$rowIdx" ht="28" customHeight="1">
      <c r="A$rowIdx" s="1" t="inlineStr"><is><t>PRADIPTA GRAHA DIGITAL (PGD ORDER)</t></is></c>
    </row>
""")
        rowIdx++

        // Row 2: Subtitle
        sb.append("""    <row r="$rowIdx" ht="20" customHeight="1">
      <c r="A$rowIdx" s="16" t="inlineStr"><is><t>Laporan Pembukuan Eksekutif &amp; Realisasi Pos Anggaran</t></is></c>
    </row>
""")
        rowIdx++

        // Row 3: Metadata
        sb.append("""    <row r="$rowIdx" ht="18" customHeight="1">
      <c r="A$rowIdx" s="2" t="inlineStr"><is><t>Periode: $startDate s.d $endDate | Waktu Ekspor: $nowStr</t></is></c>
    </row>
""")
        rowIdx += 2

        // Executive Summary Metrics Table
        sb.append("""    <row r="$rowIdx" ht="22" customHeight="1">
      <c r="A$rowIdx" s="16" t="inlineStr"><is><t>I. RINGKASAN EKSEKUTIF KINERJA KEUANGAN</t></is></c>
    </row>
""")
        rowIdx++

        // Metric Headers
        sb.append("""    <row r="$rowIdx" ht="24" customHeight="1">
      <c r="A$rowIdx" s="3" t="inlineStr"><is><t>No</t></is></c>
      <c r="B$rowIdx" s="3" t="inlineStr"><is><t>Indikator Metrik Keuangan</t></is></c>
      <c r="C$rowIdx" s="3" t="inlineStr"><is><t>Nilai / Akumulasi</t></is></c>
      <c r="D$rowIdx" s="3" t="inlineStr"><is><t>Keterangan / Status</t></is></c>
    </row>
""")
        rowIdx++

        val metrics = listOf(
            Triple("Total Omset Penjualan Cetak", formatRupiah(totalOmzet), "Total penerimaan bruto pesanan cetak"),
            Triple("Total Volume Unit Terproduksi", "${formatNumber(totalUnits)} pcs", "Akumulasi unit diproduksi & diselesaikan"),
            Triple("Total Pengeluaran Mutasi Kas", formatRupiah(totalMutationOut), "Realisasi kas keluar dari dompet operasional"),
            Triple("Total Pemasukan Mutasi Kas", formatRupiah(totalMutationIn), "Realisasi kas masuk / top up tambahan"),
            Triple("Sisa Saldo Kas Riil Terintegrasi", formatRupiah(grandSisa), "Akumulasi saldo kas riil per pos dompet")
        )

        metrics.forEachIndexed { i, m ->
            val isZebra = i % 2 == 1
            val styleTxt = if (isZebra) 5 else 4
            val styleCtr = if (isZebra) 7 else 6
            sb.append("""    <row r="$rowIdx" ht="20" customHeight="1">
      <c r="A$rowIdx" s="$styleCtr"><v>${i + 1}</v></c>
      <c r="B$rowIdx" s="$styleTxt" t="inlineStr"><is><t>${escapeXml(m.first)}</t></is></c>
      <c r="C$rowIdx" s="$styleTxt" t="inlineStr"><is><t>${escapeXml(m.second)}</t></is></c>
      <c r="D$rowIdx" s="$styleTxt" t="inlineStr"><is><t>${escapeXml(m.third)}</t></is></c>
    </row>
""")
            rowIdx++
        }

        rowIdx += 2

        // Section II: Pos Anggaran Table
        sb.append("""    <row r="$rowIdx" ht="22" customHeight="1">
      <c r="A$rowIdx" s="16" t="inlineStr"><is><t>II. REKAPITULASI DETAIL PER POS ANGGARAN &amp; DOMPET</t></is></c>
    </row>
""")
        rowIdx++

        // Table Header
        sb.append("""    <row r="$rowIdx" ht="26" customHeight="1">
      <c r="A$rowIdx" s="3" t="inlineStr"><is><t>No</t></is></c>
      <c r="B$rowIdx" s="3" t="inlineStr"><is><t>Pos Dompet Anggaran</t></is></c>
      <c r="C$rowIdx" s="3" t="inlineStr"><is><t>Formula / Dasar Plotting</t></is></c>
      <c r="D$rowIdx" s="3" t="inlineStr"><is><t>Pemasukan Plotting (IDR)</t></is></c>
      <c r="E$rowIdx" s="3" t="inlineStr"><is><t>Pengeluaran Riil (IDR)</t></is></c>
      <c r="F$rowIdx" s="3" t="inlineStr"><is><t>Sisa Saldo Kas (IDR)</t></is></c>
      <c r="G$rowIdx" s="3" t="inlineStr"><is><t>Serapan (%)</t></is></c>
      <c r="H$rowIdx" s="3" t="inlineStr"><is><t>Status Evaluasi</t></is></c>
    </row>
""")
        rowIdx++

        posItems.forEachIndexed { i, item ->
            val isZebra = i % 2 == 1
            val styleTxt = if (isZebra) 5 else 4
            val styleCtr = if (isZebra) 7 else 6
            val styleCur = if (isZebra) 9 else 8
            val stylePct = if (isZebra) 11 else 10

            val formulaDesc = when {
                item.namaAkun.contains("Kertas", ignoreCase = true) -> "Rp 106 / pcs order"
                item.namaAkun.contains("Tinta", ignoreCase = true) -> "Rp 25 / pcs order"
                item.namaAkun.contains("Pengemasan", ignoreCase = true) -> "Rp 300 / plastik"
                item.namaAkun.contains("Waste", ignoreCase = true) -> "5.0% dari Omzet"
                item.namaAkun.contains("Tenaga Kerja", ignoreCase = true) -> "7.0% dari Omzet"
                item.namaAkun.contains("Listrik", ignoreCase = true) -> "2.0% dari Omzet"
                item.namaAkun.contains("Maintenance", ignoreCase = true) -> "5.0% dari Omzet"
                item.namaAkun.contains("Laba", ignoreCase = true) -> "Omzet - Total HPP Modal"
                else -> "Standar Dompet"
            }

            val statusSerapan = when {
                item.persentaseSerapan > 100.0 -> "Overbudget (>100%)"
                item.persentaseSerapan >= 70.0 -> "Optimal (70-100%)"
                item.persentaseSerapan > 0.0 -> "Efisien (<70%)"
                else -> "Belum Ada Realisasi"
            }

            val serapanVal = item.persentaseSerapan / 100.0

            sb.append("""    <row r="$rowIdx" ht="20" customHeight="1">
      <c r="A$rowIdx" s="$styleCtr"><v>${i + 1}</v></c>
      <c r="B$rowIdx" s="$styleTxt" t="inlineStr"><is><t>${escapeXml(item.namaAkun)}</t></is></c>
      <c r="C$rowIdx" s="$styleTxt" t="inlineStr"><is><t>${escapeXml(formulaDesc)}</t></is></c>
      <c r="D$rowIdx" s="$styleCur"><v>${item.totalMasukPlotting}</v></c>
      <c r="E$rowIdx" s="$styleCur"><v>${item.totalKeluarRiil}</v></c>
      <c r="F$rowIdx" s="$styleCur"><v>${item.sisaSaldo}</v></c>
      <c r="G$rowIdx" s="$stylePct"><v>$serapanVal</v></c>
      <c r="H$rowIdx" s="$styleCtr" t="inlineStr"><is><t>${escapeXml(statusSerapan)}</t></is></c>
    </row>
""")
            rowIdx++
        }

        // Grand Total Row
        val totalSerapanVal = if (grandMasuk > 0) (grandKeluar / grandMasuk) else 0.0
        sb.append("""    <row r="$rowIdx" ht="24" customHeight="1">
      <c r="A$rowIdx" s="14" t="inlineStr"><is><t></t></is></c>
      <c r="B$rowIdx" s="14" t="inlineStr"><is><t>TOTAL KESELURUHAN POS</t></is></c>
      <c r="C$rowIdx" s="14" t="inlineStr"><is><t>Akumulasi Seluruh Dompet</t></is></c>
      <c r="D$rowIdx" s="15"><v>$grandMasuk</v></c>
      <c r="E$rowIdx" s="15"><v>$grandKeluar</v></c>
      <c r="F$rowIdx" s="15"><v>$grandSisa</v></c>
      <c r="G$rowIdx" s="15"><v>$totalSerapanVal</v></c>
      <c r="H$rowIdx" s="14" t="inlineStr"><is><t>Kondisi Seimbang</t></is></c>
    </row>
""")

        sb.append("""  </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun buildSheet2Xml(
        startDate: String,
        endDate: String,
        orders: List<TransaksiOrderMasuk>,
        pelangganMap: Map<Int, MasterPelanggan>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <cols>
    <col min="1" max="1" width="6" customWidth="1"/>
    <col min="2" max="2" width="14" customWidth="1"/>
    <col min="3" max="3" width="14" customWidth="1"/>
    <col min="4" max="4" width="22" customWidth="1"/>
    <col min="5" max="5" width="20" customWidth="1"/>
    <col min="6" max="6" width="30" customWidth="1"/>
    <col min="7" max="7" width="14" customWidth="1"/>
    <col min="8" max="8" width="10" customWidth="1"/>
    <col min="9" max="9" width="12" customWidth="1"/>
    <col min="10" max="10" width="16" customWidth="1"/>
    <col min="11" max="11" width="18" customWidth="1"/>
    <col min="12" max="12" width="10" customWidth="1"/>
    <col min="13" max="13" width="18" customWidth="1"/>
  </cols>
  <sheetData>
""")

        var rowIdx = 1

        // Title
        sb.append("""    <row r="$rowIdx" ht="28" customHeight="1">
      <c r="A$rowIdx" s="1" t="inlineStr"><is><t>REKAPITULASI TRANSAKSI ORDER &amp; NOTA CETAK</t></is></c>
    </row>
""")
        rowIdx++

        sb.append("""    <row r="$rowIdx" ht="18" customHeight="1">
      <c r="A$rowIdx" s="2" t="inlineStr"><is><t>Periode: $startDate s.d $endDate | Total: ${orders.size} Pesanan Masuk</t></is></c>
    </row>
""")
        rowIdx += 2

        // Table Header
        sb.append("""    <row r="$rowIdx" ht="26" customHeight="1">
      <c r="A$rowIdx" s="3" t="inlineStr"><is><t>No</t></is></c>
      <c r="B$rowIdx" s="3" t="inlineStr"><is><t>Tanggal</t></is></c>
      <c r="C$rowIdx" s="3" t="inlineStr"><is><t>No ID Order</t></is></c>
      <c r="D$rowIdx" s="3" t="inlineStr"><is><t>Nama Pelanggan</t></is></c>
      <c r="E$rowIdx" s="3" t="inlineStr"><is><t>Instansi / Kontak</t></is></c>
      <c r="F$rowIdx" s="3" t="inlineStr"><is><t>Nama Pesanan Cetak</t></is></c>
      <c r="G$rowIdx" s="3" t="inlineStr"><is><t>Kategori</t></is></c>
      <c r="H$rowIdx" s="3" t="inlineStr"><is><t>Qty</t></is></c>
      <c r="I$rowIdx" s="3" t="inlineStr"><is><t>Satuan</t></is></c>
      <c r="J$rowIdx" s="3" t="inlineStr"><is><t>Harga Satuan</t></is></c>
      <c r="K$rowIdx" s="3" t="inlineStr"><is><t>Total Omzet</t></is></c>
      <c r="L$rowIdx" s="3" t="inlineStr"><is><t>Plastik</t></is></c>
      <c r="M$rowIdx" s="3" t="inlineStr"><is><t>Status Pembayaran</t></is></c>
    </row>
""")
        rowIdx++

        var totalQty = 0
        var totalOmzet = 0.0
        var totalPlastik = 0

        orders.forEachIndexed { i, order ->
            val isZebra = i % 2 == 1
            val styleTxt = if (isZebra) 5 else 4
            val styleCtr = if (isZebra) 7 else 6
            val styleCur = if (isZebra) 9 else 8
            val styleInt = if (isZebra) 13 else 12

            val orderTotal = order.qtyOrder.toDouble() * order.hargaSatuan
            totalQty += order.qtyOrder
            totalOmzet += orderTotal
            totalPlastik += order.jumlahPlastikPengemasan

            // Attempt to resolve customer name
            val custName = "Pelanggan #${order.idOrder}"
            val custInfo = "-"

            sb.append("""    <row r="$rowIdx" ht="20" customHeight="1">
      <c r="A$rowIdx" s="$styleCtr"><v>${i + 1}</v></c>
      <c r="B$rowIdx" s="$styleCtr" t="inlineStr"><is><t>${escapeXml(order.tanggalOrder)}</t></is></c>
      <c r="C$rowIdx" s="$styleCtr" t="inlineStr"><is><t>#ORD-${order.idOrder}</t></is></c>
      <c r="D$rowIdx" s="$styleTxt" t="inlineStr"><is><t>${escapeXml(custName)}</t></is></c>
      <c r="E$rowIdx" s="$styleTxt" t="inlineStr"><is><t>${escapeXml(custInfo)}</t></is></c>
      <c r="F$rowIdx" s="$styleTxt" t="inlineStr"><is><t>${escapeXml(order.namaPesanan)}</t></is></c>
      <c r="G$rowIdx" s="$styleCtr" t="inlineStr"><is><t>${escapeXml(order.kategori)}</t></is></c>
      <c r="H$rowIdx" s="$styleInt"><v>${order.qtyOrder}</v></c>
      <c r="I$rowIdx" s="$styleCtr" t="inlineStr"><is><t>${escapeXml(order.satuan)}</t></is></c>
      <c r="J$rowIdx" s="$styleCur"><v>${order.hargaSatuan}</v></c>
      <c r="K$rowIdx" s="$styleCur"><v>$orderTotal</v></c>
      <c r="L$rowIdx" s="$styleInt"><v>${order.jumlahPlastikPengemasan}</v></c>
      <c r="M$rowIdx" s="$styleCtr" t="inlineStr"><is><t>${escapeXml(order.status)}</t></is></c>
    </row>
""")
            rowIdx++
        }

        // Grand Total Row
        sb.append("""    <row r="$rowIdx" ht="24" customHeight="1">
      <c r="A$rowIdx" s="14" t="inlineStr"><is><t></t></is></c>
      <c r="B$rowIdx" s="14" t="inlineStr"><is><t></t></is></c>
      <c r="C$rowIdx" s="14" t="inlineStr"><is><t></t></is></c>
      <c r="D$rowIdx" s="14" t="inlineStr"><is><t></t></is></c>
      <c r="E$rowIdx" s="14" t="inlineStr"><is><t></t></is></c>
      <c r="F$rowIdx" s="14" t="inlineStr"><is><t>TOTAL TRANSAKSI ORDER</t></is></c>
      <c r="G$rowIdx" s="14" t="inlineStr"><is><t>${orders.size} Item</t></is></c>
      <c r="H$rowIdx" s="15"><v>$totalQty</v></c>
      <c r="I$rowIdx" s="14" t="inlineStr"><is><t>pcs</t></is></c>
      <c r="J$rowIdx" s="14" t="inlineStr"><is><t></t></is></c>
      <c r="K$rowIdx" s="15"><v>$totalOmzet</v></c>
      <c r="L$rowIdx" s="15"><v>$totalPlastik</v></c>
      <c r="M$rowIdx" s="14" t="inlineStr"><is><t>Selesai Terverifikasi</t></is></c>
    </row>
""")

        sb.append("""  </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun buildSheet3Xml(
        startDate: String,
        endDate: String,
        mutations: List<MutasiManualKeluarMasuk>,
        accountMap: Map<Int, MasterAkunSaldo>,
        auditRecords: List<AuditRecord>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <cols>
    <col min="1" max="1" width="6" customWidth="1"/>
    <col min="2" max="2" width="16" customWidth="1"/>
    <col min="3" max="3" width="22" customWidth="1"/>
    <col min="4" max="4" width="16" customWidth="1"/>
    <col min="5" max="5" width="20" customWidth="1"/>
    <col min="6" max="6" width="22" customWidth="1"/>
    <col min="7" max="7" width="36" customWidth="1"/>
  </cols>
  <sheetData>
""")

        var rowIdx = 1

        // Title
        sb.append("""    <row r="$rowIdx" ht="28" customHeight="1">
      <c r="A$rowIdx" s="1" t="inlineStr"><is><t>LOG MUTASI KAS &amp; AUDIT SELISIH KAS</t></is></c>
    </row>
""")
        rowIdx++

        sb.append("""    <row r="$rowIdx" ht="18" customHeight="1">
      <c r="A$rowIdx" s="2" t="inlineStr"><is><t>Periode: $startDate s.d $endDate | Dokumen Audit &amp; Rekonsiliasi Kas</t></is></c>
    </row>
""")
        rowIdx += 2

        // Section A: Mutasi Log
        sb.append("""    <row r="$rowIdx" ht="22" customHeight="1">
      <c r="A$rowIdx" s="16" t="inlineStr"><is><t>A. LOG MUTASI MANUAL &amp; PENYESUAIAN KAS OPERASIONAL</t></is></c>
    </row>
""")
        rowIdx++

        sb.append("""    <row r="$rowIdx" ht="26" customHeight="1">
      <c r="A$rowIdx" s="3" t="inlineStr"><is><t>No</t></is></c>
      <c r="B$rowIdx" s="3" t="inlineStr"><is><t>Tanggal &amp; Waktu</t></is></c>
      <c r="C$rowIdx" s="3" t="inlineStr"><is><t>Pos / Dompet Sumber</t></is></c>
      <c r="D$rowIdx" s="3" t="inlineStr"><is><t>Jenis Mutasi</t></is></c>
      <c r="E$rowIdx" s="3" t="inlineStr"><is><t>Nominal Kas (IDR)</t></is></c>
      <c r="F$rowIdx" s="3" t="inlineStr"><is><t>Dompet Tujuan</t></is></c>
      <c r="G$rowIdx" s="3" t="inlineStr"><is><t>Keterangan / Keperluan</t></is></c>
    </row>
""")
        rowIdx++

        var totalMutKeluar = 0.0
        var totalMutMasuk = 0.0

        if (mutations.isEmpty()) {
            sb.append("""    <row r="$rowIdx" ht="20" customHeight="1">
      <c r="A$rowIdx" s="6"><v>1</v></c>
      <c r="B$rowIdx" s="4" t="inlineStr"><is><t>-</t></is></c>
      <c r="C$rowIdx" s="4" t="inlineStr"><is><t>Tidak ada mutasi manual pada periode ini</t></is></c>
      <c r="D$rowIdx" s="4" t="inlineStr"><is><t>-</t></is></c>
      <c r="E$rowIdx" s="8"><v>0</v></c>
      <c r="F$rowIdx" s="4" t="inlineStr"><is><t>-</t></is></c>
      <c r="G$rowIdx" s="4" t="inlineStr"><is><t>-</t></is></c>
    </row>
""")
            rowIdx++
        } else {
            mutations.forEachIndexed { i, m ->
                val isZebra = i % 2 == 1
                val styleTxt = if (isZebra) 5 else 4
                val styleCtr = if (isZebra) 7 else 6
                val styleCur = if (isZebra) 9 else 8

                val srcName = accountMap[m.idAkun]?.namaAkun ?: "Pos #${m.idAkun}"
                val dstName = if (m.idAkunTujuan != null) accountMap[m.idAkunTujuan]?.namaAkun ?: "Pos #${m.idAkunTujuan}" else "-"
                val timeDisplay = "${m.tanggalMutasi} ${m.waktuMutasi}"

                if (m.jenisMutasi == "Uang Keluar") totalMutKeluar += m.nominal
                if (m.jenisMutasi == "Uang Masuk") totalMutMasuk += m.nominal

                sb.append("""    <row r="$rowIdx" ht="20" customHeight="1">
      <c r="A$rowIdx" s="$styleCtr"><v>${i + 1}</v></c>
      <c r="B$rowIdx" s="$styleCtr" t="inlineStr"><is><t>${escapeXml(timeDisplay)}</t></is></c>
      <c r="C$rowIdx" s="$styleTxt" t="inlineStr"><is><t>${escapeXml(srcName)}</t></is></c>
      <c r="D$rowIdx" s="$styleCtr" t="inlineStr"><is><t>${escapeXml(m.jenisMutasi)}</t></is></c>
      <c r="E$rowIdx" s="$styleCur"><v>${m.nominal}</v></c>
      <c r="F$rowIdx" s="$styleTxt" t="inlineStr"><is><t>${escapeXml(dstName)}</t></is></c>
      <c r="G$rowIdx" s="$styleTxt" t="inlineStr"><is><t>${escapeXml(m.keterangan)}</t></is></c>
    </row>
""")
                rowIdx++
            }
        }

        // Subtotal Mutasi
        sb.append("""    <row r="$rowIdx" ht="24" customHeight="1">
      <c r="A$rowIdx" s="14" t="inlineStr"><is><t></t></is></c>
      <c r="B$rowIdx" s="14" t="inlineStr"><is><t></t></is></c>
      <c r="C$rowIdx" s="14" t="inlineStr"><is><t>TOTAL REALISASI PENGELUARAN MUTASI</t></is></c>
      <c r="D$rowIdx" s="14" t="inlineStr"><is><t>Keluar</t></is></c>
      <c r="E$rowIdx" s="15"><v>$totalMutKeluar</v></c>
      <c r="F$rowIdx" s="14" t="inlineStr"><is><t></t></is></c>
      <c r="G$rowIdx" s="14" t="inlineStr"><is><t>Akumulasi Pengeluaran</t></is></c>
    </row>
""")
        rowIdx += 3

        // Section B: Riwayat Audit Selisih Kas
        sb.append("""    <row r="$rowIdx" ht="22" customHeight="1">
      <c r="A$rowIdx" s="16" t="inlineStr"><is><t>B. RIWAYAT AUDIT SELISIH KAS (FISIK VS SISTEM)</t></is></c>
    </row>
""")
        rowIdx++

        sb.append("""    <row r="$rowIdx" ht="26" customHeight="1">
      <c r="A$rowIdx" s="3" t="inlineStr"><is><t>No</t></is></c>
      <c r="B$rowIdx" s="3" t="inlineStr"><is><t>Waktu Audit</t></is></c>
      <c r="C$rowIdx" s="3" t="inlineStr"><is><t>Saldo Kas Sistem</t></is></c>
      <c r="D$rowIdx" s="3" t="inlineStr"><is><t>Hasil Hitung Fisik</t></is></c>
      <c r="E$rowIdx" s="3" t="inlineStr"><is><t>Selisih Kas (+/-)</t></is></c>
      <c r="F$rowIdx" s="3" t="inlineStr"><is><t>Status Rekonsiliasi</t></is></c>
      <c r="G$rowIdx" s="3" t="inlineStr"><is><t>Catatan &amp; Evaluasi Auditor</t></is></c>
    </row>
""")
        rowIdx++

        if (auditRecords.isEmpty()) {
            sb.append("""    <row r="$rowIdx" ht="20" customHeight="1">
      <c r="A$rowIdx" s="6"><v>1</v></c>
      <c r="B$rowIdx" s="4" t="inlineStr"><is><t>-</t></is></c>
      <c r="C$rowIdx" s="8"><v>0</v></c>
      <c r="D$rowIdx" s="8"><v>0</v></c>
      <c r="E$rowIdx" s="8"><v>0</v></c>
      <c r="F$rowIdx" s="4" t="inlineStr"><is><t>Belum Ada Sesi Audit</t></is></c>
      <c r="G$rowIdx" s="4" t="inlineStr"><is><t>Silakan jalankan audit fisik kas secara berkala pada menu Audit Selisih Kas</t></is></c>
    </row>
""")
        } else {
            auditRecords.forEachIndexed { i, a ->
                val isZebra = i % 2 == 1
                val styleTxt = if (isZebra) 5 else 4
                val styleCtr = if (isZebra) 7 else 6
                val styleCur = if (isZebra) 9 else 8

                val statusText = if (a.isAdjusted) "Rekonsiliasi Diterapkan" else "Disimpan Sebagai Evaluasi"
                val note = if (a.keterangan.isNotBlank()) "${a.keterangan} ${a.detailPenyesuaian}".trim() else "Audit Berkala Kas PGD"

                sb.append("""    <row r="$rowIdx" ht="20" customHeight="1">
      <c r="A$rowIdx" s="$styleCtr"><v>${i + 1}</v></c>
      <c r="B$rowIdx" s="$styleCtr" t="inlineStr"><is><t>${escapeXml(a.timestamp)}</t></is></c>
      <c r="C$rowIdx" s="$styleCur"><v>${a.saldoSistem}</v></c>
      <c r="D$rowIdx" s="$styleCur"><v>${a.saldoFisik}</v></c>
      <c r="E$rowIdx" s="$styleCur"><v>${a.selisih}</v></c>
      <c r="F$rowIdx" s="$styleCtr" t="inlineStr"><is><t>${escapeXml(statusText)}</t></is></c>
      <c r="G$rowIdx" s="$styleTxt" t="inlineStr"><is><t>${escapeXml(note)}</t></is></c>
    </row>
""")
                rowIdx++
            }
        }

        sb.append("""  </sheetData>
</worksheet>""")
        return sb.toString()
    }

    // =========================================================================
    // 2. PDF EXPORT IMPLEMENTATION (Multi-Page Styled Android PdfDocument)
    // =========================================================================

    fun exportToPdf(
        context: Context,
        startDate: String,
        endDate: String,
        orders: List<TransaksiOrderMasuk>,
        mutations: List<MutasiManualKeluarMasuk>,
        accounts: List<MasterAkunSaldo>,
        pelangganList: List<MasterPelanggan>,
        onExportSuccess: (String, Uri, String) -> Unit
    ) {
        val fileName = "Laporan_Pembukuan_PGD_${startDate}_to_${endDate}.pdf"
        val mimeType = "application/pdf"

        val auditRecords = AuditStorageHelper.loadAuditHistory(context)
        val posItems = computeAllocationItems(accounts, orders, mutations)

        val totalUnits = orders.sumOf { it.qtyOrder }
        val totalOmzet = orders.sumOf { it.qtyOrder.toDouble() * it.hargaSatuan }
        val totalMutationOut = mutations.filter { it.jenisMutasi == "Uang Keluar" }.sumOf { it.nominal }
        val totalMutationIn = mutations.filter { it.jenisMutasi == "Uang Masuk" }.sumOf { it.nominal }
        val grandTotalMasukPlotting = posItems.sumOf { it.totalMasukPlotting }
        val grandTotalKeluarRiil = posItems.sumOf { it.totalKeluarRiil }
        val grandTotalSisaRiil = posItems.sumOf { it.sisaSaldo }

        val nowStr = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")).format(Date())

        val uri = saveExportedFile(context, fileName, mimeType) { outputStream ->
            val pdfDocument = PdfDocument()

            // Page dimensions: A4 (595 x 842 points)
            val pageWidth = 595
            val pageHeight = 842
            val margin = 36f

            // Common Paints
            val primaryPurple = AndroidColor.parseColor("#4A3B6C")
            val darkPurple = AndroidColor.parseColor("#3B2369")
            val softBg = AndroidColor.parseColor("#F5F2F9")
            val borderColor = AndroidColor.parseColor("#D1C8DF")
            val totalRowBg = AndroidColor.parseColor("#E8E0F0")
            val greenColor = AndroidColor.parseColor("#166534")
            val redColor = AndroidColor.parseColor("#C62828")
            val grayText = AndroidColor.parseColor("#554B6E")

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkPurple
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = grayText
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkPurple
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val tdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#1E1B26")
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val tdBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkPurple
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val tdRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#1E1B26")
                textSize = 7.5f
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val tdBoldRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkPurple
                textSize = 8f
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = borderColor
                strokeWidth = 0.75f
                style = Paint.Style.STROKE
            }
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

            var currentPageNum = 1

            // Helper to draw standard Header Kop
            fun drawHeaderKop(canvas: Canvas, title: String, subtitle: String): Float {
                var y = 28f
                // Top Brand Bar Accent
                fillPaint.color = primaryPurple
                canvas.drawRect(margin, y, pageWidth - margin, y + 4f, fillPaint)
                y += 14f

                // Logo/Brand Title
                canvas.drawText("PRADIPTA GRAHA DIGITAL", margin, y + 10f, titlePaint)
                y += 14f

                canvas.drawText("PGD Order - Sistem Manajemen Keuangan & Pembukuan Percetakan Digital", margin, y + 8f, subTitlePaint)
                y += 12f

                canvas.drawText("Periode Laporan: $startDate s.d $endDate   |   Waktu Cetak: $nowStr", margin, y + 8f, subTitlePaint)
                y += 14f

                // Divider line
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 14f

                if (title.isNotBlank()) {
                    canvas.drawText(title, margin, y + 10f, sectionPaint)
                    y += 14f
                }
                return y
            }

            // Helper to draw Footer
            fun drawFooter(canvas: Canvas, pageNum: Int) {
                val y = pageHeight - 24f
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                val footerText = "Dokumen dicetak otomatis oleh Sistem PGD Order - Pradipta Graha Digital"
                canvas.drawText(footerText, margin, y + 12f, subTitlePaint)
                val pageText = "Halaman $pageNum"
                val textWidth = subTitlePaint.measureText(pageText)
                canvas.drawText(pageText, pageWidth - margin - textWidth, y + 12f, subTitlePaint)
            }

            // -------------------------------------------------------------
            // PAGE 1: Ringkasan Eksekutif & Realisasi Pos Dompet
            // -------------------------------------------------------------
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNum).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            var curY = drawHeaderKop(canvas, "I. RINGKASAN EKSEKUTIF KINERJA KEUANGAN", "")

            // KPI Grid (3x2 or 2x3 boxes)
            val cardW = (pageWidth - margin * 2 - 16f) / 3f
            val cardH = 44f

            val kpis = listOf(
                Triple("TOTAL OMSET PENJUALAN", formatRupiah(totalOmzet), greenColor),
                Triple("TOTAL PRODUKSI", "${formatNumber(totalUnits)} pcs", primaryPurple),
                Triple("SISA SALDO KAS RIIL", formatRupiah(grandTotalSisaRiil), darkPurple),
                Triple("PENGELUARAN MUTASI", formatRupiah(totalMutationOut), redColor),
                Triple("PEMASUKAN MUTASI", formatRupiah(totalMutationIn), primaryPurple),
                Triple("TOTAL MASUK PLOTTING", formatRupiah(grandTotalMasukPlotting), darkPurple)
            )

            for (i in 0 until 6) {
                val col = i % 3
                val row = i / 3
                val left = margin + col * (cardW + 8f)
                val top = curY + row * (cardH + 8f)
                val rect = RectF(left, top, left + cardW, top + cardH)

                // Background box
                fillPaint.color = softBg
                canvas.drawRoundRect(rect, 6f, 6f, fillPaint)
                canvas.drawRoundRect(rect, 6f, 6f, linePaint)

                // Left accent bar
                fillPaint.color = kpis[i].third
                canvas.drawRoundRect(RectF(left, top, left + 4f, top + cardH), 2f, 2f, fillPaint)

                // Texts
                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = grayText
                    textSize = 7f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = kpis[i].third
                    textSize = 9.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                canvas.drawText(kpis[i].first, left + 8f, top + 14f, labelPaint)
                canvas.drawText(kpis[i].second, left + 8f, top + 32f, valPaint)
            }

            curY += (cardH * 2 + 24f)

            // Section II: Tabel Realisasi Pos Anggaran
            canvas.drawText("II. REKAPITULASI DETAIL PER POS ANGGARAN & DOMPET", margin, curY + 10f, sectionPaint)
            curY += 16f

            // Table Header definition
            val thH = 20f
            val trH = 18f
            val colX = floatArrayOf(margin, margin + 20f, margin + 110f, margin + 180f, margin + 255f, margin + 330f, margin + 400f, margin + 455f)
            val tableRight = pageWidth - margin

            // Draw TH
            fillPaint.color = primaryPurple
            canvas.drawRect(margin, curY, tableRight, curY + thH, fillPaint)

            canvas.drawText("No", colX[0] + 4f, curY + 13f, thPaint)
            canvas.drawText("Pos Anggaran", colX[1] + 4f, curY + 13f, thPaint)
            canvas.drawText("Dasar Formula", colX[2] + 4f, curY + 13f, thPaint)
            canvas.drawText("Masuk Plotting", colX[3] + 4f, curY + 13f, thPaint)
            canvas.drawText("Keluar Riil", colX[4] + 4f, curY + 13f, thPaint)
            canvas.drawText("Sisa Kas", colX[5] + 4f, curY + 13f, thPaint)
            canvas.drawText("Serapan", colX[6] + 4f, curY + 13f, thPaint)
            canvas.drawText("Status", colX[7] + 4f, curY + 13f, thPaint)

            curY += thH

            posItems.forEachIndexed { i, item ->
                val isZebra = i % 2 == 1
                fillPaint.color = if (isZebra) softBg else AndroidColor.WHITE
                canvas.drawRect(margin, curY, tableRight, curY + trH, fillPaint)
                canvas.drawLine(margin, curY + trH, tableRight, curY + trH, linePaint)

                val formulaDesc = when {
                    item.namaAkun.contains("Kertas", ignoreCase = true) -> "Rp 106/pcs"
                    item.namaAkun.contains("Tinta", ignoreCase = true) -> "Rp 25/pcs"
                    item.namaAkun.contains("Pengemasan", ignoreCase = true) -> "Rp 300/plst"
                    item.namaAkun.contains("Waste", ignoreCase = true) -> "5.0% Omzet"
                    item.namaAkun.contains("Tenaga Kerja", ignoreCase = true) -> "7.0% Omzet"
                    item.namaAkun.contains("Listrik", ignoreCase = true) -> "2.0% Omzet"
                    item.namaAkun.contains("Maintenance", ignoreCase = true) -> "5.0% Omzet"
                    item.namaAkun.contains("Laba", ignoreCase = true) -> "Sisa Hasil Usaha"
                    else -> "Standar"
                }

                val statusSerapan = when {
                    item.persentaseSerapan > 100.0 -> "Over"
                    item.persentaseSerapan >= 70.0 -> "Optimal"
                    item.persentaseSerapan > 0.0 -> "Efisien"
                    else -> "-"
                }

                val textY = curY + 12f
                canvas.drawText("${i + 1}", colX[0] + 4f, textY, tdPaint)
                canvas.drawText(item.namaAkun, colX[1] + 4f, textY, tdBoldPaint)
                canvas.drawText(formulaDesc, colX[2] + 4f, textY, tdPaint)
                canvas.drawText(formatRupiah(item.totalMasukPlotting), colX[4] - 6f, textY, tdRightPaint)
                canvas.drawText(formatRupiah(item.totalKeluarRiil), colX[5] - 6f, textY, tdRightPaint)
                canvas.drawText(formatRupiah(item.sisaSaldo), colX[6] - 6f, textY, tdRightPaint)
                canvas.drawText(formatPercent(item.persentaseSerapan), colX[7] - 6f, textY, tdRightPaint)
                canvas.drawText(statusSerapan, colX[7] + 4f, textY, tdPaint)

                curY += trH
            }

            // Total Pos Row
            fillPaint.color = totalRowBg
            canvas.drawRect(margin, curY, tableRight, curY + trH + 2f, fillPaint)
            canvas.drawLine(margin, curY + trH + 2f, tableRight, curY + trH + 2f, linePaint)

            val totalY = curY + 13f
            canvas.drawText("TOTAL", colX[1] + 4f, totalY, tdBoldPaint)
            canvas.drawText("Semua Pos", colX[2] + 4f, totalY, tdPaint)
            canvas.drawText(formatRupiah(grandTotalMasukPlotting), colX[4] - 6f, totalY, tdBoldRightPaint)
            canvas.drawText(formatRupiah(grandTotalKeluarRiil), colX[5] - 6f, totalY, tdBoldRightPaint)
            canvas.drawText(formatRupiah(grandTotalSisaRiil), colX[6] - 6f, totalY, tdBoldRightPaint)
            val avgSerapan = if (grandTotalMasukPlotting > 0) (grandTotalKeluarRiil / grandTotalMasukPlotting) * 100.0 else 0.0
            canvas.drawText(formatPercent(avgSerapan), colX[7] - 6f, totalY, tdBoldRightPaint)

            drawFooter(canvas, currentPageNum)
            pdfDocument.finishPage(page)

            // -------------------------------------------------------------
            // PAGE 2+: Riwayat Order & Transaksi Nota Cetak
            // -------------------------------------------------------------
            currentPageNum++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas

            curY = drawHeaderKop(canvas, "III. REKAPITULASI TRANSAKSI ORDER & NOTA CETAK", "")

            // Order Table Columns
            val ordColX = floatArrayOf(margin, margin + 20f, margin + 80f, margin + 140f, margin + 270f, margin + 325f, margin + 380f, margin + 450f)

            fun drawOrderTableHeader(c: Canvas, y: Float): Float {
                fillPaint.color = primaryPurple
                c.drawRect(margin, y, tableRight, y + thH, fillPaint)
                c.drawText("No", ordColX[0] + 4f, y + 13f, thPaint)
                c.drawText("Tanggal", ordColX[1] + 4f, y + 13f, thPaint)
                c.drawText("No Order", ordColX[2] + 4f, y + 13f, thPaint)
                c.drawText("Nama Pesanan", ordColX[3] + 4f, y + 13f, thPaint)
                c.drawText("Qty (Pcs)", ordColX[4] + 4f, y + 13f, thPaint)
                c.drawText("Harga Satuan", ordColX[5] + 4f, y + 13f, thPaint)
                c.drawText("Total Omzet", ordColX[6] + 4f, y + 13f, thPaint)
                c.drawText("Status", ordColX[7] + 4f, y + 13f, thPaint)
                return y + thH
            }

            curY = drawOrderTableHeader(canvas, curY)

            orders.forEachIndexed { i, order ->
                if (curY + trH > pageHeight - 45f) {
                    drawFooter(canvas, currentPageNum)
                    pdfDocument.finishPage(page)

                    currentPageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    curY = drawHeaderKop(canvas, "III. REKAPITULASI TRANSAKSI ORDER CETAK (Lanjutan)", "")
                    curY = drawOrderTableHeader(canvas, curY)
                }

                val isZebra = i % 2 == 1
                fillPaint.color = if (isZebra) softBg else AndroidColor.WHITE
                canvas.drawRect(margin, curY, tableRight, curY + trH, fillPaint)
                canvas.drawLine(margin, curY + trH, tableRight, curY + trH, linePaint)

                val orderTotal = order.qtyOrder.toDouble() * order.hargaSatuan
                val pesananTrim = if (order.namaPesanan.length > 24) order.namaPesanan.take(22) + "..." else order.namaPesanan

                val textY = curY + 12f
                canvas.drawText("${i + 1}", ordColX[0] + 4f, textY, tdPaint)
                canvas.drawText(order.tanggalOrder, ordColX[1] + 4f, textY, tdPaint)
                canvas.drawText("#${order.idOrder}", ordColX[2] + 4f, textY, tdPaint)
                canvas.drawText(pesananTrim, ordColX[3] + 4f, textY, tdBoldPaint)
                canvas.drawText("${order.qtyOrder} ${order.satuan.take(3)}", ordColX[5] - 6f, textY, tdRightPaint)
                canvas.drawText(formatRupiah(order.hargaSatuan), ordColX[6] - 6f, textY, tdRightPaint)
                canvas.drawText(formatRupiah(orderTotal), ordColX[7] - 6f, textY, tdBoldRightPaint)
                canvas.drawText(order.status, ordColX[7] + 4f, textY, tdPaint)

                curY += trH
            }

            drawFooter(canvas, currentPageNum)
            pdfDocument.finishPage(page)

            // -------------------------------------------------------------
            // PAGE 3: Log Mutasi Kas & Audit Selisih Kas
            // -------------------------------------------------------------
            currentPageNum++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas

            curY = drawHeaderKop(canvas, "IV. LOG MUTASI OPERASIONAL & AUDIT KAS FISIK", "")

            // Sub-Table 1: Mutasi
            canvas.drawText("A. CATATAN MUTASI MANUAL DAN PENGELUARAN DOMPET", margin, curY + 8f, sectionPaint)
            curY += 14f

            val mutColX = floatArrayOf(margin, margin + 20f, margin + 85f, margin + 185f, margin + 255f, margin + 335f)

            fillPaint.color = primaryPurple
            canvas.drawRect(margin, curY, tableRight, curY + thH, fillPaint)
            canvas.drawText("No", mutColX[0] + 4f, curY + 13f, thPaint)
            canvas.drawText("Tanggal", mutColX[1] + 4f, curY + 13f, thPaint)
            canvas.drawText("Pos Dompet", mutColX[2] + 4f, curY + 13f, thPaint)
            canvas.drawText("Jenis", mutColX[3] + 4f, curY + 13f, thPaint)
            canvas.drawText("Nominal", mutColX[4] + 4f, curY + 13f, thPaint)
            canvas.drawText("Keterangan", mutColX[5] + 4f, curY + 13f, thPaint)
            curY += thH

            val displayMuts = mutations.take(12)
            if (displayMuts.isEmpty()) {
                fillPaint.color = AndroidColor.WHITE
                canvas.drawRect(margin, curY, tableRight, curY + trH, fillPaint)
                canvas.drawText("Tidak ada mutasi kas manual pada periode ini", margin + 10f, curY + 12f, tdPaint)
                curY += trH
            } else {
                displayMuts.forEachIndexed { i, m ->
                    val isZebra = i % 2 == 1
                    fillPaint.color = if (isZebra) softBg else AndroidColor.WHITE
                    canvas.drawRect(margin, curY, tableRight, curY + trH, fillPaint)
                    canvas.drawLine(margin, curY + trH, tableRight, curY + trH, linePaint)

                    val posName = accounts.find { it.idAkun == m.idAkun }?.namaAkun ?: "Pos #${m.idAkun}"
                    val ketTrim = if (m.keterangan.length > 28) m.keterangan.take(26) + "..." else m.keterangan

                    val textY = curY + 12f
                    canvas.drawText("${i + 1}", mutColX[0] + 4f, textY, tdPaint)
                    canvas.drawText(m.tanggalMutasi, mutColX[1] + 4f, textY, tdPaint)
                    canvas.drawText(posName, mutColX[2] + 4f, textY, tdBoldPaint)
                    canvas.drawText(m.jenisMutasi, mutColX[3] + 4f, textY, tdPaint)
                    canvas.drawText(formatRupiah(m.nominal), mutColX[5] - 6f, textY, tdRightPaint)
                    canvas.drawText(ketTrim, mutColX[5] + 4f, textY, tdPaint)

                    curY += trH
                }
            }

            curY += 18f

            // Sub-Table 2: Audit Selisih Kas
            canvas.drawText("B. RIWAYAT AUDIT INDEPENDEN SELISIH KAS FISIK", margin, curY + 8f, sectionPaint)
            curY += 14f

            val audColX = floatArrayOf(margin, margin + 20f, margin + 110f, margin + 185f, margin + 260f, margin + 335f, margin + 410f)

            fillPaint.color = primaryPurple
            canvas.drawRect(margin, curY, tableRight, curY + thH, fillPaint)
            canvas.drawText("No", audColX[0] + 4f, curY + 13f, thPaint)
            canvas.drawText("Waktu Audit", audColX[1] + 4f, curY + 13f, thPaint)
            canvas.drawText("Saldo Sistem", audColX[2] + 4f, curY + 13f, thPaint)
            canvas.drawText("Hitung Fisik", audColX[3] + 4f, curY + 13f, thPaint)
            canvas.drawText("Selisih (+/-)", audColX[4] + 4f, curY + 13f, thPaint)
            canvas.drawText("Status Tindakan", audColX[5] + 4f, curY + 13f, thPaint)
            canvas.drawText("Catatan Auditor", audColX[6] + 4f, curY + 13f, thPaint)
            curY += thH

            val displayAudits = auditRecords.take(8)
            if (displayAudits.isEmpty()) {
                fillPaint.color = AndroidColor.WHITE
                canvas.drawRect(margin, curY, tableRight, curY + trH, fillPaint)
                canvas.drawText("Belum ada catatan riwayat audit kas fisik", margin + 10f, curY + 12f, tdPaint)
                curY += trH
            } else {
                displayAudits.forEachIndexed { i, a ->
                    val isZebra = i % 2 == 1
                    fillPaint.color = if (isZebra) softBg else AndroidColor.WHITE
                    canvas.drawRect(margin, curY, tableRight, curY + trH, fillPaint)
                    canvas.drawLine(margin, curY + trH, tableRight, curY + trH, linePaint)

                    val statusText = if (a.isAdjusted) "Rekonsiliasi" else "Evaluasi"
                    val noteTrim = if (a.keterangan.length > 20) a.keterangan.take(18) + "..." else if (a.keterangan.isNotBlank()) a.keterangan else "-"

                    val textY = curY + 12f
                    canvas.drawText("${i + 1}", audColX[0] + 4f, textY, tdPaint)
                    canvas.drawText(a.timestamp.take(16), audColX[1] + 4f, textY, tdPaint)
                    canvas.drawText(formatRupiah(a.saldoSistem), audColX[3] - 6f, textY, tdRightPaint)
                    canvas.drawText(formatRupiah(a.saldoFisik), audColX[4] - 6f, textY, tdRightPaint)
                    canvas.drawText(formatRupiah(a.selisih), audColX[5] - 6f, textY, tdRightPaint)
                    canvas.drawText(statusText, audColX[5] + 4f, textY, tdBoldPaint)
                    canvas.drawText(noteTrim, audColX[6] + 4f, textY, tdPaint)

                    curY += trH
                }
            }

            // Signature & Validation Block
            curY += 24f
            val sigW = 160f
            val sigLeft = pageWidth - margin - sigW

            canvas.drawText("Disiapkan & Diverifikasi Oleh:", sigLeft, curY, subTitlePaint)
            curY += 38f
            canvas.drawLine(sigLeft, curY, sigLeft + sigW, curY, linePaint)
            canvas.drawText("Tim Administrasi & Keuangan PGD", sigLeft, curY + 12f, tdBoldPaint)

            drawFooter(canvas, currentPageNum)
            pdfDocument.finishPage(page)

            // Write final PDF document to outputStream
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
        }

        if (uri != null) {
            onExportSuccess(fileName, uri, mimeType)
        } else {
            Toast.makeText(context, "Gagal mengekspor berkas PDF", Toast.LENGTH_SHORT).show()
        }
    }
}
