package com.example.data.database

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

fun doesTableExist(tableName: String): Boolean {
    return transaction {
        val result = exec("""
            SELECT EXISTS (
                SELECT 1
                FROM information_schema.tables
                WHERE table_name = '$tableName' AND table_schema = 'public'
            )
        """) { resultSet ->
            resultSet.next()
            resultSet.getBoolean(1)
        }
        result ?: false
    }
}
fun createTableIfNotExists(table: Table) {
    transaction {
        val tableNameLowerCase = table.tableName.lowercase() // Convert table name to lowercase
        if (!doesTableExist(tableNameLowerCase)) {
            SchemaUtils.create(table)
            println("$tableNameLowerCase table created.")
        } else {
            println("$tableNameLowerCase table already exists.")
        }
    }
}
