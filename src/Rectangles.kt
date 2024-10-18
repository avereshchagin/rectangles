import org.intellij.lang.annotations.Language
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.jvm.Throws
import kotlin.math.roundToInt

private const val UNIT_DIMENSION = 100f

private data class Rectangle(
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

        val cells = BooleanArray(rows.size * columns.size)
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

                cells[i * columns.size + j] = rectanglesInCurrentCell.isNotEmpty()

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
        outputFile: String, rows: List<Row>, columns: List<Column>, scale: Float, isFilled: (Int, Int) -> Boolean
    ) {
        @Language("html")
        val header = """
            <html>
            <head>
                <style>
                    tr { border: 1px solid #000; }
                    td { border: 1px solid #000; }
                    td.filled { background-color: #ccc; }
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
                    val cssClass = if (isFilled(i, j)) """ class="filled" """ else ""
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
            Scanner(FileReader(inputFile)).use { scanner ->
                val rectanglesCount = scanner.nextInt()
                for (i in 0 until rectanglesCount) {
                    rectangles.add(Rectangle(scanner.nextInt(), scanner.nextInt(), scanner.nextInt(), scanner.nextInt()))
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
