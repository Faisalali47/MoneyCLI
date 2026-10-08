package com.moneycli

class CommandParser {

    // ========================================================
    // PARSE COMMAND
    // ========================================================

    fun parse(command: String): ParsedCommand {
        val input = command.trim()

        if (input.isEmpty()) {
            throw IllegalArgumentException("Command is empty")
        }

        val lower = input.lowercase()

        return when {

            // ========================================================
            // BALANCE
            // ========================================================

            lower == "balance" || lower == "bal" -> {
                ParsedCommand.Balance
            }

            // ========================================================
            // TRANSACTIONS
            // ========================================================

            lower == "transactions" || lower == "tx" -> {
                ParsedCommand.Transactions
            }

            // ========================================================
            // STATS
            // ========================================================

            lower == "stats" || lower == "st" -> {
                ParsedCommand.Stats
            }

            // ========================================================
            // HELP
            // ========================================================

            lower == "help" -> {
                ParsedCommand.Help
            }

            // ========================================================
            // CLEAR
            // ========================================================

            lower == "clear" -> {
                ParsedCommand.Clear
            }

            // ========================================================
            // CANCEL
            // ========================================================

            lower == "cancel" -> {
                ParsedCommand.Cancel
            }

            // ========================================================
            // RESET
            // ========================================================

            lower == "transaction reset" || lower == "reset" -> {
                ParsedCommand.Reset
            }

            // ========================================================
            // INCOME
            // ========================================================

            lower.startsWith("income add") -> {
                ParsedCommand.Income(
                    amount = extractOldAmount(input),
                    description = extractOldDescription(input)
                )
            }

            lower.startsWith("in ") -> {
                val parts = input.split(
                    Regex("\\s+"),
                    limit = 3
                )

                if (parts.size < 3) {
                    throw IllegalArgumentException(
                        "Usage: in <amount> <description>"
                    )
                }

                ParsedCommand.Income(
                    amount = parts[1].toLongOrNull()
                        ?: throw IllegalArgumentException("Invalid amount"),
                    description = parts[2]
                )
            }

            // ========================================================
            // EXPENSE
            // ========================================================

            lower.startsWith("expense add") -> {
                ParsedCommand.Expense(
                    amount = extractOldAmount(input),
                    description = extractOldDescription(input)
                )
            }

            lower.startsWith("ex ") -> {
                val parts = input.split(
                    Regex("\\s+"),
                    limit = 3
                )

                if (parts.size < 3) {
                    throw IllegalArgumentException(
                        "Usage: ex <amount> <description>"
                    )
                }

                ParsedCommand.Expense(
                    amount = parts[1].toLongOrNull()
                        ?: throw IllegalArgumentException("Invalid amount"),
                    description = parts[2]
                )
            }

            // ========================================================
            // TRANSFER
            // ========================================================

            lower.startsWith("transfer") -> {
                ParsedCommand.Transfer(
                    amount = extractOldAmount(input)
                )
            }

            lower == "tr" || lower.startsWith("tr ") -> {
                val parts = input.split(
                    Regex("\\s+")
                )

                if (parts.size < 2) {
                    throw IllegalArgumentException(
                        "Usage: tr <amount>"
                    )
                }

                ParsedCommand.Transfer(
                    amount = parts[1].toLongOrNull()
                        ?: throw IllegalArgumentException("Invalid amount")
                )
            }

            // ========================================================
            // DELETE
            // ========================================================

            lower.startsWith("transaction delete") -> {
                ParsedCommand.Delete(
                    id = extractOldId(input)
                )
            }

            lower == "del" || lower.startsWith("del ") -> {
                val parts = input.split(
                    Regex("\\s+")
                )

                if (parts.size < 2) {
                    throw IllegalArgumentException(
                        "Usage: del <id>"
                    )
                }

                ParsedCommand.Delete(
                    id = parts[1].toIntOrNull()
                        ?: throw IllegalArgumentException("Invalid ID")
                )
            }

            // ========================================================
            // EDIT
            // ========================================================

            lower.startsWith("transaction edit") -> {
                ParsedCommand.Edit(
                    id = extractOldId(input),
                    amount = extractOldAmount(input)
                )
            }

            lower.startsWith("edit ") -> {
                val parts = input.split(
                    Regex("\\s+")
                )

                if (parts.size < 3) {
                    throw IllegalArgumentException(
                        "Usage: edit <id> <amount>"
                    )
                }

                ParsedCommand.Edit(
                    id = parts[1].toIntOrNull()
                        ?: throw IllegalArgumentException("Invalid ID"),
                    amount = parts[2].toLongOrNull()
                        ?: throw IllegalArgumentException("Invalid amount")
                )
            }

            else -> {
                ParsedCommand.Unknown(input)
            }
        }
    }

    // ========================================================
    // OLD COMMAND PARSING
    // ========================================================

    private fun extractOldAmount(command: String): Long {
        val regex = Regex("""-n\s+(\d+)""")

        val match = regex.find(command)
            ?: throw IllegalArgumentException("Amount not found")

        return match.groupValues[1].toLong()
    }

    private fun extractOldDescription(command: String): String {
        val regex = Regex("""-d\s+"([^"]*)"""")

        val match = regex.find(command)
            ?: throw IllegalArgumentException("Description not found")

        return match.groupValues[1]
    }

    private fun extractOldId(command: String): Int {
        val regex = Regex("""-id\s+(\d+)""")

        val match = regex.find(command)
            ?: throw IllegalArgumentException("Transaction ID not found")

        return match.groupValues[1].toInt()
    }
}

// ========================================================
// PARSED COMMAND
// ========================================================

sealed class ParsedCommand {

    data object Balance : ParsedCommand()

    data object Transactions : ParsedCommand()

    data object Stats : ParsedCommand()

    data object Help : ParsedCommand()

    data object Clear : ParsedCommand()

    data object Cancel : ParsedCommand()

    data object Reset : ParsedCommand()

    data class Income(
        val amount: Long,
        val description: String
    ) : ParsedCommand()

    data class Expense(
        val amount: Long,
        val description: String
    ) : ParsedCommand()

    data class Transfer(
        val amount: Long
    ) : ParsedCommand()

    data class Delete(
        val id: Int
    ) : ParsedCommand()

    data class Edit(
        val id: Int,
        val amount: Long
    ) : ParsedCommand()

    data class Unknown(
        val command: String
    ) : ParsedCommand()
}