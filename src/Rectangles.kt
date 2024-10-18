import org.intellij.lang.annotations.Language
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.jvm.Throws
import kotlin.math.roundToInt

private const val UNIT_DIMENSION = 100f
private const val PALETTE_COLORS_SIZE = 8

private data class Rectangle(
    val id: Int,
    val top: Int,
    val left: Int,
    val bottom: Int,
    val right: Int,
) {

    val height = bottom - top
    val width = right - left
}

private data class Row(
    val top: Int,
    val bottom: Int,
) {

    val height: Int = bottom - top
}

private data class Column(
    val left: Int,
    val right: Int,
) {

    val width: Int = right - left
}

private class RectanglesVisualizer(
    private val rectangles: List<Rectangle>
) {

    @Throws(IOException::class)
    fun processToFile(outputFile: String) {
        val horizontalMesh = TreeSet<Int>()
        val verticalMesh = TreeSet<Int>()
        var minDimension: Int = Int.MAX_VALUE

        for (rectangle in rectangles) {
            horizontalMesh.add(rectangle.left)
            horizontalMesh.add(rectangle.right)
            verticalMesh.add(rectangle.top)
            verticalMesh.add(rectangle.bottom)

            if (rectangle.height < minDimension) {
                minDimension = rectangle.height
            }
            if (rectangle.width < minDimension) {
                minDimension = rectangle.width
            }
        }

        val scale: Float = UNIT_DIMENSION / minDimension

        val columns = ArrayList<Column>()
        val horizontalIterator = horizontalMesh.iterator()
        var prevLine: Int
        if (horizontalIterator.hasNext()) {
            prevLine = horizontalIterator.next()
            while (horizontalIterator.hasNext()) {
                val nextLine = horizontalIterator.next()
                columns.add(Column(prevLine, nextLine))
                prevLine = nextLine
            }
        }

        val rows = ArrayList<Row>()
        val verticalIterator = verticalMesh.iterator()
        if (verticalIterator.hasNext()) {
            prevLine = verticalIterator.next()
            while (verticalIterator.hasNext()) {
                val nextLine = verticalIterator.next()
                rows.add(Row(prevLine, nextLine))
                prevLine = nextLine
            }
        }

        val cells = IntArray(rows.size * columns.size) {
            -1
        }
        val sortedByTop = rectangles.sortedBy { it.top }
        val sortedByLeft = rectangles.sortedBy { it.left }

        var topIndex = 0
        val rectanglesInCurrentRow = HashSet<Rectangle>()
        for (i in rows.indices) {
            val rowTop = rows[i].top
            while (topIndex < sortedByTop.size && sortedByTop[topIndex].top == rowTop) {
                rectanglesInCurrentRow.add(sortedByTop[topIndex])
                topIndex++
            }

            var leftIndex = 0
            val rectanglesInCurrentCell = HashSet<Rectangle>()
            for (j in columns.indices) {
                val columnLeft = columns[j].left
                while (leftIndex < sortedByLeft.size && sortedByLeft[leftIndex].left == columnLeft) {
                    if (rectanglesInCurrentRow.contains(sortedByLeft[leftIndex])) {
                        rectanglesInCurrentCell.add(sortedByLeft[leftIndex])
                    }
                    leftIndex++
                }

                val currentRectangle = rectanglesInCurrentCell.firstOrNull()
                if (currentRectangle != null) {
                    cells[i * columns.size + j] = currentRectangle.id % PALETTE_COLORS_SIZE
                }

                val rectanglesInCurrentCellIterator = rectanglesInCurrentCell.iterator()
                while (rectanglesInCurrentCellIterator.hasNext()) {
                    val rectangle = rectanglesInCurrentCellIterator.next()
                    if (rectangle.right <= columns[j].right) {
                        rectanglesInCurrentCellIterator.remove()
                    }
                }
            }

            val rectanglesInCurrentRowIterator = rectanglesInCurrentRow.iterator()
            while (rectanglesInCurrentRowIterator.hasNext()) {
                val rectangle = rectanglesInCurrentRowIterator.next()
                if (rectangle.bottom <= rows[i].bottom) {
                    rectanglesInCurrentRowIterator.remove()
                }
            }
        }

        writeToFile(outputFile, rows, columns, scale) { i, j ->
            cells[i * columns.size + j]
        }
    }

    @Throws(IOException::class)
    private fun writeToFile(
        outputFile: String, rows: List<Row>, columns: List<Column>, scale: Float, fillColorIndex: (Int, Int) -> Int
    ) {
        @Language("html")
        val header = """
            <html>
            <head>
                <style>
                    tr { border: 1px solid #000; }
                    td { border: 1px solid #000; }
                    td.filled0 { background-color: #f5d7b0; }
                    td.filled1 { background-color: #d15b56; }
                    td.filled2 { background-color: #007896; }
                    td.filled3 { background-color: #3e909d; }
                    td.filled4 { background-color: #c43138; }
                    td.filled5 { background-color: #c7522a; }
                    td.filled6 { background-color: #004e61; }
                    td.filled7 { background-color: #7ba8a3; }
                </style>
            </head>
            <body>
            <table style="border-collapse: collapse; border: 1px solid #000;">
        """.trimIndent()

        @Language("html")
        val footer = """
            </table>
            </body>
            </html>
        """.trimIndent()

        File(outputFile).printWriter().use { out ->
            out.println(header)

            for (i in rows.indices) {
                val row = rows[i]
                val heightPx = (row.height * scale).roundToInt()
                out.println("""<tr style="height: ${heightPx}px">""")
                for (j in columns.indices) {
                    val column = columns[j]
                    val widthPx = (column.width * scale).roundToInt()
                    val colorIndex = fillColorIndex(i, j)
                    val cssClass = if (colorIndex >= 0) """ class="filled${colorIndex}" """ else ""
                    out.println("""<td style="width: ${widthPx}px" $cssClass>&nbsp;</td>""")
                }
                out.println("</tr>")
            }

            out.println(footer)
        }
    }

    companion object {

        @Throws(IOException::class)
        fun loadFromFile(inputFile: String): RectanglesVisualizer {
            val rectangles = ArrayList<Rectangle>()
            var id = 0
            Scanner(FileReader(inputFile)).use { scanner ->
                val rectanglesCount = scanner.nextInt()
                for (i in 0 until rectanglesCount) {
                    val top = scanner.nextInt()
                    val left = scanner.nextInt()
                    val bottom = scanner.nextInt()
                    val right = scanner.nextInt()
                    rectangles.add(Rectangle(id, top, left, bottom, right))
                    id++
                }
            }
            return RectanglesVisualizer(rectangles)
        }
    }
}

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Required arguments: <input file (txt)> <output file (html)>")
        return
    }
    val inputFile = args[0]
    val outputFile = args[1]

    val visualizer = try {
        RectanglesVisualizer.loadFromFile(inputFile)
    } catch (e: IOException) {
        println("Unable to read input file $inputFile!")
        e.printStackTrace()
        return
    }
    try {
        visualizer.processToFile(outputFile)
    } catch (e: IOException) {
        println("Unable to write to output file $outputFile!")
        e.printStackTrace()
        return
    }
}
