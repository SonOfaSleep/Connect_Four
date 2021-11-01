package connectfour

class ConnectFourGame() {
    companion object Regexps {
        val boardRegex = Regex("\\d+[x,X]\\d+")
        val rowRegex = Regex("[5-9][x,X]\\d+")
        val columnRegex = Regex("\\d+[x,X][5-9]")
    }

    private var board = Board()
    private lateinit var firstPlayer: Player
    private lateinit var secondPlayer: Player
    private lateinit var currentPlayer: Player
    var winOrDraw = false
    var totalNumberOfGames = 1
    var gameNumber = 1

    inner class Player(val name: String, val dot: String) {
        var score = 0
    }

    inner class Board(val rows: Int = 6, val columns: Int = 7) {
        var dots = MutableList(columns) { MutableList(rows) {" "} }
        private val bottomFill = MutableList(columns - 1) { "═╩" }
        private val splitter = "║"
        private val leftCorner = "╚"
        private val rightCorner = "═╝"

        fun render() {
            repeat(columns) { print(" ${it + 1}") }
            println()
            var lastIndexOfBoard = rows - 1
            for (i in rows downTo 1) {
                print(splitter)
                for (index in 0 until columns) {
                    print(dots[index][lastIndexOfBoard])
                    print(splitter)
                }
                println()
                lastIndexOfBoard -= 1
            }
            println("$leftCorner${bottomFill.joinToString("")}$rightCorner")
        }
    }

    fun intro() {
        println("Connect Four\nFirst player's name:")
        firstPlayer = Player(readLine()!!,"o")
        println("Second player's name:")
        secondPlayer = Player(readLine()!!,"*")
        currentPlayer = firstPlayer
        chooseBoardSize()
    }

    private fun chooseBoardSize() {
        while(true) {
            println("Set the board dimensions (Rows x Columns)\nPress Enter for default (6 x 7)")
            val input = readLine()!!.replace("\\s+".toRegex(), "")
            if (input.isEmpty()) {
                break
            }
            if (!input.matches(boardRegex)) {
                println("Invalid input")
            } else if (!input.matches(rowRegex)) {
                println("Board rows should be from 5 to 9")
            } else if (!input.matches(columnRegex)) {
                println("Board columns should be from 5 to 9")
            } else {
                board = Board(input.first().digitToInt(), input.last().digitToInt())
                break
            }
        }
        chooseGameVariant()
    }

    private fun chooseGameVariant() {
        while (true) {
            println("""
            Do you want to play single or multiple games?
            For a single game, input 1 or press Enter
            Input a number of games:
        """.trimIndent())
            val input = readLine()!!
            if (input.matches(Regex("\\d+")) && input.toInt() > 0 || input.isEmpty()) {
                when {
                    input.isEmpty() || input == "1" -> setNumberOfGamesAndEnterMainLoop(1)
                    else -> setNumberOfGamesAndEnterMainLoop(input.toInt())
                }
                break
            } else {
                println("Invalid input")
            }
        }
    }

    private fun setNumberOfGamesAndEnterMainLoop(games: Int) {
        println("${firstPlayer.name} VS ${secondPlayer.name}\n${board.rows} X ${board.columns} board")
        if (games == 1) {
            println("Single game")
        } else {
            totalNumberOfGames = games
            println("Total $totalNumberOfGames games\nGame #$gameNumber")
        }
        board.render()
        mainGameLoop()
    }

    private fun mainGameLoop() {
        do {
            println("${if (currentPlayer == firstPlayer) firstPlayer.name else secondPlayer.name}'s turn:")
            val input = readLine()!!.lowercase()
            when (input) {
                "end" -> break
                else -> checkInputAndPlaceDot(input)
            }
        } while (!winOrDraw && input != "end")
    }

    private fun checkInputAndPlaceDot(input: String) {
        val inputCheck = Regex("\\d+")
        val numberCheck = Regex("[1-${board.columns}]")
        if (!input.matches(inputCheck)) println("Incorrect column number")
        else if (!input.matches(numberCheck)) println("The column number is out of range (1 - ${board.columns})")
        else if (!checkColumnForEmptySpace(input.toInt())) println("Column $input is full")
        else placeDotRenderBoardCheckWinAndDraw(input.toInt() - 1)
    }

    private fun checkColumnForEmptySpace(input: Int): Boolean {
        return " " in board.dots[input - 1]
    }

    private fun placeDotRenderBoardCheckWinAndDraw(input: Int) {
        board.dots[input][board.dots[input].indexOf(" ")] = when (currentPlayer) {
            firstPlayer -> firstPlayer.dot else -> secondPlayer.dot
        }
        board.render()
        checkDraw()
        checkWinChangeTurnResetBoardIfWin(currentPlayer)
    }

    private fun checkWinChangeTurnResetBoardIfWin(currentPlayer: Player) {
        var bool = false
        /**
         * For counting repeating dots in lists
         */
        fun countDotsInList(list: MutableList<String>): Boolean {
            val dot = if (currentPlayer == firstPlayer) "*" else "o"
            val splitList = list.joinToString("").split(" ", dot)
            for (i in splitList) {
                if (i.count { it.toString() == currentPlayer.dot } > 3) bool = true
            }
            return bool
        }

        /**
         * Taking vertical lists and counting dots in it
         */
        for (column in board.dots) {
            if (countDotsInList(column)) bool = true
        }

        /**
         * Making horizontal lists and counting dots in it
         */
        mother@ for (secondIndex in 0 until board.rows) {
            val list = mutableListOf<String>()
            for (firstIndex in 0 until board.columns) {
                list.add(board.dots[firstIndex][secondIndex])
            }
            if (countDotsInList(list)) {
                bool = true
                break@mother
            }
        }

        /**
         * Making diagonal left (like this: \) lists and counting dots in it
         */
        //first half of "square", including the middle diagonal
        mother@ for (firstIndex in 0 until board.dots.size) {
            var addUp = 0
            val list = mutableListOf<String>()
            for (secondIndex in 0 until board.dots[0].size) {
                list.add(board.dots[firstIndex - addUp][secondIndex])
                addUp++
                if (firstIndex - addUp < 0) {
                    if (countDotsInList(list)) bool = true
                    continue@mother
                }
            }
        }
        //second half, excluding the middle diagonal
        var startingIndexLeft = 1
        for (count in board.dots[0].lastIndex downTo 1) {
            val list = mutableListOf<String>()
            var up = 0
            for (secondCount in count downTo 1) {
                list.add(board.dots[board.dots.lastIndex - up][startingIndexLeft + up])
                up++
            }
            startingIndexLeft++
            if (countDotsInList(list)) bool = true
        }

        /**
         * Making diagonal right lists (like this: /) lists and counting dots in it
         */
        //first half of "square", including the middle diagonal
        mother@ for (firstIndex in 0 until board.dots.size) {
            var addUp = 0
            val list = mutableListOf<String>()
            for (secondIndex in board.dots[0].lastIndex downTo 0) {
                list.add(board.dots[firstIndex - addUp][secondIndex])
                addUp++
                if (firstIndex - addUp < 0) {
                    if (countDotsInList(list)) bool = true
                    continue@mother
                }
            }
        }
        //second half, excluding the middle diagonal
        var startingIndex = board.dots[0].lastIndex - 1
        for (count in board.dots[0].lastIndex downTo 1) {
            val list = mutableListOf<String>()
            var up = 0
            for (secondCount in count downTo 1) {
                list.add(board.dots[board.dots.lastIndex - up][startingIndex - up])
                up++
            }
            startingIndex--
            if (countDotsInList(list)) bool = true
        }

        /**
         * Reset board if win and game > 1
         */
        if (!bool) {
            this.currentPlayer = if (currentPlayer == firstPlayer) secondPlayer else firstPlayer
        } else {
            winOrDraw = true
            currentPlayer.score += 2
            gameNumber++
            println("Player ${currentPlayer.name} won")
            if (totalNumberOfGames > 1) printScore()
            if (totalNumberOfGames > 1 && gameNumber <= totalNumberOfGames) {
                resetBoard()
            }
        }
    }

    private fun checkDraw() {
        var count = 0
        for (column in board.dots) {
            count += column.count { it == " " }
        }
        if (count == 0) {
            winOrDraw = true
            firstPlayer.score += 1
            secondPlayer.score += 1
            gameNumber++
            println("It is a draw")
            if (totalNumberOfGames > 1) printScore()
            if (totalNumberOfGames > 1 && gameNumber <= totalNumberOfGames) {
                resetBoard()
            }
        }
    }

    private fun printScore() {
        println("Score\n${firstPlayer.name}: ${firstPlayer.score} ${secondPlayer.name}: ${secondPlayer.score}")
    }

    private fun resetBoard() {
        this.currentPlayer = if (gameNumber % 2 == 1) firstPlayer else secondPlayer
        winOrDraw = false
        println("Game #$gameNumber")
        board.dots = MutableList(board.columns) { MutableList(board.rows) {" "} }
        board.render()
        mainGameLoop()
    }
}

fun main() {
    val game = ConnectFourGame()
    game.intro()
    println("Game over!")
}