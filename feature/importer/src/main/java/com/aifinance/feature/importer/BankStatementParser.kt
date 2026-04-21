package com.aifinance.feature.importer

import android.content.Context
import android.net.Uri
import com.aifinance.core.model.TransactionType
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedInputStream
import java.math.BigDecimal
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object BankStatementParser {

    private val dateKeywords = listOf("交易时间", "交易日期", "记账日期", "入账日期", "日期", "时间")
    private val incomeKeywords = listOf("收入金额", "贷方发生额", "收入", "入账金额")
    private val expenseKeywords = listOf("支出金额", "借方发生额", "支出", "出账金额")
    private val amountKeywords = listOf("交易金额", "金额", "发生额", "发生金额")
    private val directionKeywords = listOf("收支类型", "交易方向", "借贷", "借贷标志", "方向", "交易类型")
    private val titleKeywords = listOf("摘要", "交易摘要", "交易名称", "用途", "商户名称", "对方户名", "备注")
    private val noteKeywords = listOf("备注", "附言", "说明", "用途", "交易摘要")

    private val dateTimePatterns = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy/MM/dd HH:mm",
        "yyyy.MM.dd HH:mm:ss",
        "yyyy.MM.dd HH:mm",
        "yyyyMMddHHmmss",
        "yyyyMMddHHmm",
    )

    private val dateOnlyPatterns = listOf(
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "yyyy.MM.dd",
        "yyyyMMdd",
    )

    fun parse(context: Context, uri: Uri): List<ParsedBankBill> {
        val lowerName = (uri.lastPathSegment ?: "").lowercase(Locale.ROOT)
        return if (lowerName.endsWith(".csv")) {
            parseCsv(context, uri)
        } else {
            runCatching { parseExcel(context, uri) }
                .getOrElse { parseCsv(context, uri) }
        }
    }

    private fun parseExcel(context: Context, uri: Uri): List<ParsedBankBill> {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bufferedInput = BufferedInputStream(input)
            val workbook = WorkbookFactory.create(bufferedInput)
            workbook.use { wb ->
                val allRows = mutableListOf<ParsedBankBill>()
                for (sheetIndex in 0 until wb.numberOfSheets) {
                    val sheet = wb.getSheetAt(sheetIndex)
                    if (sheet.physicalNumberOfRows <= 1) continue

                    val headerInfo = detectHeaderFromSheetRows(sheet.rowIterator().asSequence().take(40).toList())
                    if (headerInfo.isUseful) {
                        val rows = sheet.rowIterator().asSequence().drop(headerInfo.headerRowIndex + 1)
                        rows.forEach { row ->
                            parseDataRow(
                                getValue = { index -> row.getCell(index)?.asDisplayText().orEmpty() },
                                dateCell = { index -> row.getCell(index) },
                                header = headerInfo
                            )?.let { allRows += it }
                        }
                    }
                    // 兜底：针对银行常见“日期在第5列、金额在第6列”的流水格式做硬匹配
                    if (allRows.isEmpty()) {
                        parseWithFixedBankPattern(sheet).forEach { allRows += it }
                    }
                }
                return allRows
            }
        }
        error("无法读取文件")
    }

    private fun parseCsv(context: Context, uri: Uri): List<ParsedBankBill> {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("无法读取 CSV 文件")
        if (bytes.isEmpty()) return emptyList()

        val lines = decodeCsvLines(bytes)
        val rows = lines.map { splitCsvLine(it) }
        val headerInfo = detectHeader(rows.take(40))
        if (!headerInfo.isUseful) {
            return parseCsvWithFixedBankPattern(rows)
        }
        val parsed = rows.drop(headerInfo.headerRowIndex + 1).mapNotNull { values ->
            parseDataRow(
                getValue = { index -> values.getOrNull(index).orEmpty() },
                dateCell = { null },
                header = headerInfo
            )
        }
        if (parsed.isNotEmpty()) return parsed
        return parseCsvWithFixedBankPattern(rows)
    }

    private fun parseDataRow(
        getValue: (Int) -> String,
        dateCell: (Int) -> Cell?,
        header: HeaderInfo,
    ): ParsedBankBill? {
        val dateText = header.dateIndex?.let(getValue).orEmpty()
        val dateTime = parseDateTime(dateText, header.dateIndex?.let(dateCell))
            ?: return null

        val income = parseAmount(header.incomeIndex?.let(getValue).orEmpty())
        val expense = parseAmount(header.expenseIndex?.let(getValue).orEmpty())
        val directAmountText = header.amountIndex?.let(getValue).orEmpty()
        val directAmount = parseAmount(directAmountText)
        val direction = header.directionIndex?.let(getValue).orEmpty()

        val titleText = header.titleIndex?.let(getValue).orEmpty()
        val noteText = header.noteIndex?.let(getValue).orEmpty()

        val typeAndAmount = resolveTypeAndAmount(
            income = income,
            expense = expense,
            directAmount = directAmount,
            directAmountText = directAmountText,
            direction = direction,
            title = titleText,
            note = noteText,
        ) ?: return null

        val title = buildTitle(
            title = titleText,
            note = noteText,
            type = typeAndAmount.first
        )
        val note = noteText.takeIf { it.isNotBlank() }

        return ParsedBankBill(
            dateTime = dateTime,
            amount = typeAndAmount.second,
            type = typeAndAmount.first,
            title = title,
            note = note
        )
    }

    private fun resolveTypeAndAmount(
        income: BigDecimal?,
        expense: BigDecimal?,
        directAmount: BigDecimal?,
        directAmountText: String,
        direction: String,
        title: String,
        note: String,
    ): Pair<TransactionType, BigDecimal>? {
        if (income != null && income != BigDecimal.ZERO) {
            return if (income.signum() >= 0) {
                TransactionType.INCOME to income.abs()
            } else {
                TransactionType.EXPENSE to income.abs()
            }
        }
        if (expense != null && expense != BigDecimal.ZERO) {
            return if (expense.signum() <= 0) {
                TransactionType.EXPENSE to expense.abs()
            } else {
                TransactionType.INCOME to expense.abs()
            }
        }
        if (directAmount != null && directAmount != BigDecimal.ZERO) {
            if (directAmount.signum() > 0) {
                return TransactionType.INCOME to directAmount
            }
            return TransactionType.EXPENSE to directAmount.abs()
        }

        val directionText = direction.lowercase(Locale.ROOT)
        if (directionText.contains("收入") || directionText.contains("贷") || directionText.contains("入")) {
            return directAmount?.takeIf { it != BigDecimal.ZERO }?.let { TransactionType.INCOME to it.abs() }
        }
        if (directionText.contains("支出") || directionText.contains("借") || directionText.contains("出")) {
            return directAmount?.takeIf { it != BigDecimal.ZERO }?.let { TransactionType.EXPENSE to it.abs() }
        }

        val keywordsExpense = listOf("消费", "付款", "取现", "提现", "转出")
        if (keywordsExpense.any { directionText.contains(it) }) {
            return directAmount?.takeIf { it != BigDecimal.ZERO }?.let { TransactionType.EXPENSE to it.abs() }
        }

        val keywordsIncome = listOf("存入", "存款", "工资", "转入")
        if (keywordsIncome.any { directionText.contains(it) }) {
            return directAmount?.takeIf { it != BigDecimal.ZERO }?.let { TransactionType.INCOME to it.abs() }
        }

        val directText = directAmountText.trim()
        if (directText.startsWith("-")) {
            return directAmount?.let { TransactionType.EXPENSE to it.abs() }
        }
        if (directText.startsWith("+")) {
            return directAmount?.let { TransactionType.INCOME to it.abs() }
        }

        val mergedContext = "$title $note".lowercase(Locale.ROOT)
        if (listOf("消费", "餐", "支付", "转出").any { mergedContext.contains(it) }) {
            return directAmount?.takeIf { it != BigDecimal.ZERO }?.let { TransactionType.EXPENSE to it.abs() }
        }
        if (listOf("工资", "收入", "奖金", "转入").any { mergedContext.contains(it) }) {
            return directAmount?.takeIf { it != BigDecimal.ZERO }?.let { TransactionType.INCOME to it.abs() }
        }
        return null
    }

    private fun buildTitle(title: String, note: String, type: TransactionType): String {
        if (title.isNotBlank()) return title.trim()
        if (note.isNotBlank()) return note.trim()
        return if (type == TransactionType.INCOME) "银行收入导入" else "银行卡支出导入"
    }

    private fun parseDateTime(raw: String, cell: Cell?): LocalDateTime? {
        if (cell != null && cell.cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return runCatching {
                val date = cell.dateCellValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                val hasTime = containsTimeToken(cell.cellStyle?.dataFormatString)
                if (hasTime) date else LocalDateTime.of(date.toLocalDate(), LocalTime.of(1, 0))
            }.getOrNull()
        }

        val normalized = raw.trim()
            .replace("年", "-")
            .replace("月", "-")
            .replace("日", " ")
            .replace("T", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (normalized.isBlank()) return null

        dateTimePatterns.forEach { pattern ->
            runCatching {
                return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern(pattern))
            }
        }
        dateOnlyPatterns.forEach { pattern ->
            runCatching {
                val date = LocalDate.parse(normalized, DateTimeFormatter.ofPattern(pattern))
                return LocalDateTime.of(date, LocalTime.of(1, 0))
            }
        }

        return try {
            val epoch = normalized.toDouble()
            if (DateUtil.isValidExcelDate(epoch)) {
                val instant = DateUtil.getJavaDate(epoch).toInstant()
                LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseAmount(raw: String): BigDecimal? {
        val cleaned = raw.trim()
            .replace(",", "")
            .replace("￥", "")
            .replace("¥", "")
            .replace("人民币", "")
            .replace(Regex("[^\\d+\\-.]"), "")
        if (cleaned.isBlank() || cleaned == "-" || cleaned == "." || cleaned == "+") return null
        return try {
            BigDecimal(cleaned)
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun detectHeaderFromSheetRows(rows: List<Row>): HeaderInfo {
        val asValues = rows.map { row ->
            (0..row.lastCellNum.toInt().coerceAtLeast(20)).map { col ->
                row.getCell(col)?.asDisplayText().orEmpty()
            }
        }
        return detectHeader(asValues)
    }

    private fun detectHeader(rows: List<List<String>>): HeaderInfo {
        var best = HeaderInfo()
        rows.forEachIndexed { rowIndex, row ->
            if (row.all { it.isBlank() }) return@forEachIndexed
            val info = HeaderInfo(
                headerRowIndex = rowIndex,
                dateIndex = findColumn(row, dateKeywords),
                incomeIndex = findColumn(row, incomeKeywords),
                expenseIndex = findColumn(row, expenseKeywords),
                amountIndex = findColumn(row, amountKeywords),
                directionIndex = findColumn(row, directionKeywords),
                titleIndex = findColumn(row, titleKeywords),
                noteIndex = findColumn(row, noteKeywords),
            )
            if (info.score > best.score) best = info
        }
        if (best.isUseful) return best
        return inferHeaderByDataPattern(rows)
    }

    private fun inferHeaderByDataPattern(rows: List<List<String>>): HeaderInfo {
        if (rows.isEmpty()) return HeaderInfo()
        val colCount = rows.maxOfOrNull { it.size } ?: 0
        if (colCount == 0) return HeaderInfo()

        val dateScores = IntArray(colCount)
        val amountScores = IntArray(colCount)
        val negativeScores = IntArray(colCount)
        val textScores = IntArray(colCount)

        rows.forEach { row ->
            for (col in 0 until colCount) {
                val value = row.getOrNull(col).orEmpty().trim()
                if (value.isBlank()) continue
                if (parseDateTime(value, null) != null) {
                    dateScores[col]++
                    continue
                }
                if (parseAmount(value) != null) {
                    amountScores[col]++
                    if (value.startsWith("-")) negativeScores[col]++
                    continue
                }
                textScores[col]++
            }
        }

        val dateIndex = dateScores.indices.maxByOrNull { dateScores[it] }?.takeIf { dateScores[it] >= 2 }
        val amountIndex = amountScores.indices
            .filter { it != dateIndex }
            .maxByOrNull { negativeScores[it] * 2 + amountScores[it] }
            ?.takeIf { amountScores[it] >= 2 }
        if (dateIndex == null || amountIndex == null) return HeaderInfo()

        val firstDataRow = rows.indexOfFirst { row ->
            val dateOk = parseDateTime(row.getOrNull(dateIndex).orEmpty(), null) != null
            val amountOk = parseAmount(row.getOrNull(amountIndex).orEmpty()) != null
            dateOk && amountOk
        }.takeIf { it >= 0 } ?: 1

        val titleIndex = textScores.indices
            .filter { it != dateIndex && it != amountIndex }
            .maxByOrNull { textScores[it] }
            ?.takeIf { textScores[it] >= 2 }

        return HeaderInfo(
            headerRowIndex = (firstDataRow - 1).coerceAtLeast(0),
            dateIndex = dateIndex,
            amountIndex = amountIndex,
            titleIndex = titleIndex,
            noteIndex = titleIndex,
        )
    }

    private fun findColumn(row: List<String>, keywords: List<String>): Int? {
        return row.indexOfFirst { value ->
            // 关键：移除标题中的所有不可见字符（空格、换行、特殊空格）
            val text = value.replace(Regex("\\s+"), "").replace("\u00A0", "")
            keywords.any { keyword -> text.contains(keyword, ignoreCase = true) }
        }.takeIf { it >= 0 }
    }

    private fun splitCsvLine(line: String): List<String> {
        if (!line.contains(',')) return listOf(line)
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        line.forEach { ch ->
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result += current.toString().trim().trim('"')
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result += current.toString().trim().trim('"')
        return result
    }

    private fun parseWithFixedBankPattern(sheet: org.apache.poi.ss.usermodel.Sheet): List<ParsedBankBill> {
        val parsed = mutableListOf<ParsedBankBill>()
        val rows = sheet.rowIterator()
        while (rows.hasNext()) {
            val row = rows.next()
            val dateRaw = row.getCell(4)?.asDisplayText().orEmpty()
            val amountRaw = row.getCell(5)?.asDisplayText().orEmpty()
            val titleRaw = row.getCell(1)?.asDisplayText().orEmpty()
            val noteRaw = row.getCell(7)?.asDisplayText().orEmpty()

            val dateTime = parseDateTime(dateRaw, row.getCell(4)) ?: continue
            val amount = parseAmount(amountRaw) ?: continue
            if (amount == BigDecimal.ZERO) continue

            val type = if (amount.signum() >= 0) TransactionType.INCOME else TransactionType.EXPENSE
            val finalTitle = buildTitle(titleRaw, noteRaw, type)

            parsed += ParsedBankBill(
                dateTime = dateTime,
                amount = amount.abs(),
                type = type,
                title = finalTitle,
                note = noteRaw.takeIf { it.isNotBlank() }
            )
        }
        return parsed
    }

    private fun parseCsvWithFixedBankPattern(rows: List<List<String>>): List<ParsedBankBill> {
        return rows.mapNotNull { values ->
            val dateRaw = values.getOrNull(4).orEmpty()
            val amountRaw = values.getOrNull(5).orEmpty()
            val titleRaw = values.getOrNull(1).orEmpty()
            val noteRaw = values.getOrNull(7).orEmpty()

            val dateTime = parseDateTime(dateRaw, null) ?: return@mapNotNull null
            val amount = parseAmount(amountRaw) ?: return@mapNotNull null
            if (amount == BigDecimal.ZERO) return@mapNotNull null

            val type = if (amount.signum() >= 0) TransactionType.INCOME else TransactionType.EXPENSE
            ParsedBankBill(
                dateTime = dateTime,
                amount = amount.abs(),
                type = type,
                title = buildTitle(titleRaw, noteRaw, type),
                note = noteRaw.takeIf { it.isNotBlank() }
            )
        }
    }

    private fun decodeCsvLines(bytes: ByteArray): List<String> {
        val candidates = listOf("UTF-8", "GB18030", "GBK").map { Charset.forName(it) }
        candidates.forEach { charset ->
            val text = runCatching { String(bytes, charset) }.getOrNull() ?: return@forEach
            if (text.isBlank()) return@forEach
            val lines = text.lines().filter { it.isNotBlank() }
            if (lines.size < 2) return@forEach
            val preview = lines.take(6).joinToString(" ")
            if (
                preview.contains("交易") ||
                preview.contains("金额") ||
                preview.contains("日期") ||
                preview.contains("摘要")
            ) {
                return lines
            }
        }
        return String(bytes, Charsets.UTF_8).lines().filter { it.isNotBlank() }
    }

    private fun Cell.asDisplayText(): String {
        return when (cellType) {
            CellType.STRING -> stringCellValue.orEmpty().trim()
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(this)) {
                    runCatching {
                        dateCellValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString()
                    }.getOrElse { numericCellValue.toString() }
                } else {
                    numericCellValue.toBigDecimal().stripTrailingZeros().toPlainString()
                }
            }
            CellType.BOOLEAN -> booleanCellValue.toString()
            CellType.FORMULA -> runCatching {
                when (cachedFormulaResultType) {
                    CellType.STRING -> stringCellValue.orEmpty().trim()
                    CellType.NUMERIC -> numericCellValue.toBigDecimal().stripTrailingZeros().toPlainString()
                    else -> toString().trim()
                }
            }.getOrElse { toString().trim() }
            else -> toString().trim()
        }
    }

    private fun containsTimeToken(dataFormat: String?): Boolean {
        val value = dataFormat?.lowercase(Locale.ROOT).orEmpty()
        return value.contains("h") || value.contains("m") || value.contains("s")
    }
}

private data class HeaderInfo(
    val headerRowIndex: Int = 0,
    val dateIndex: Int? = null,
    val incomeIndex: Int? = null,
    val expenseIndex: Int? = null,
    val amountIndex: Int? = null,
    val directionIndex: Int? = null,
    val titleIndex: Int? = null,
    val noteIndex: Int? = null,
) {
    val score: Int
        get() {
            var s = 0
            if (dateIndex != null) s += 3
            if (incomeIndex != null) s += 2
            if (expenseIndex != null) s += 2
            if (amountIndex != null) s += 1
            if (directionIndex != null) s += 1
            if (titleIndex != null) s += 1
            return s
        }

    val isUseful: Boolean
        get() = dateIndex != null && (incomeIndex != null || expenseIndex != null || amountIndex != null)
}