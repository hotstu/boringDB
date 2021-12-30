package com.github.hotstu.boring

import com.github.hotstu.boring.anno.Column
import com.github.hotstu.boring.anno.Entity
import java.beans.IntrospectionException
import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.lang.reflect.Field
import java.sql.*
import kotlin.reflect.KClass

class Info(private val clazz: Class<*>, val tableName: String, val fields: Map<String, Field>) {
    val createTable: String
        get() {
            return StringBuilder().apply {
                append("CREATE TABLE IF NOT EXISTS ")
                append("`${tableName}`")
                append(" (")
                //date string,
                val sorted = fields.keys.sorted()
                sorted.forEachIndexed { index, s ->
                    append(" `")
                    append(s)
                    append("` ")
                    append(fields[s]!!.toSqliteType())
                    if (fields[s]?.getAnnotation(Column::class.java)?.primaryKey == true) {
                        append(" PRIMARY KEY")
                    }
                    if (index < sorted.size - 1) {
                        append(", ")
                    }
                }
                append(")")
            }.toString()
        }

    val props: Array<PropertyDescriptor> by lazy {
        try {
            Introspector.getBeanInfo(clazz).propertyDescriptors
        } catch (e: IntrospectionException) {
            throw SQLException(
                    "Bean introspection failed: " + e.message)
        }
    }
}

fun Field.toSqliteType(): String {


//    NULL. The value is a NULL value.
//
//    INTEGER. The value is a signed integer, stored in 1, 2, 3, 4, 6, or 8 bytes depending on the magnitude of the value.
//
//    REAL. The value is a floating point value, stored as an 8-byte IEEE floating point number.
//
//    TEXT. The value is a text string, stored using the database encoding (UTF-8, UTF-16BE or UTF-16LE).
//
//    BLOB. The value is a blob of data, stored exactly as it was input.
//    https://www.sqlite.org/datatype3.html
    return when {
        this.type.isAssignableFrom(Int::class.java) -> {
            "INTEGER"
        }
        this.type.isAssignableFrom(Float::class.java) || this.type.isAssignableFrom(Double::class.java) -> {
            "REAL"
        }
        this.type.isAssignableFrom(String::class.java) -> {
            "TEXT"
        }
        this.type.isAssignableFrom(Boolean::class.java) -> {
            "BOOLEAN"
        }

        else -> {
            throw IllegalStateException("暂不支持的数据参数类型")
        }
    }
}

interface ResultSetResolver<T> {
    fun resolve(rs: ResultSet): T
}

class DBSession(private val path: String, vararg tables: KClass<*>) {
    private var connection: Connection?
    private val entityInfos: Map<Class<*>, Info>

    init {
        entityInfos = tables.map {
            (it.java to retrieveInfo(it.java))
        }.toMap()
        connection = DriverManager.getConnection("jdbc:sqlite:${path}")
        connection?.createStatement()?.use { statement ->
            statement.queryTimeout = 30 // set timeout to 30 sec.
            entityInfos.forEach {
                val sql = it.value.createTable
                println("sql====>${sql}")
                statement.executeUpdate(it.value.createTable)
            }
        }
    }

    fun close() {
        connection?.close()
        connection = null
    }

    fun retrieveInfo(clazz: Class<*>): Info {
        val annotation = clazz.getAnnotation(Entity::class.java)
        val tableName = if (annotation.tableName.isNullOrBlank()) {
            clazz.simpleName.toLowerCase()
        } else annotation.tableName
        val fields = clazz.declaredFields.map {
            it.name to it
        }
        return Info(clazz = clazz, tableName = tableName, fields = fields.toMap())

    }

    /**
     * @param op 0 insert 1 delete 2 update
     */
    fun exec(op: Int, entity: Any): Boolean {
        connection!!.autoCommit = false
        try {
            when (op) {
                0 -> {
                    val info = retrieveInfo(entity::class.java)
                    val sqlBuilder = StringBuilder()
                    sqlBuilder.append("INSERT OR ABORT INTO `${info.tableName}` (")
                    val sorted = info.fields.keys.sorted()
                    sorted.forEachIndexed { index, s ->
                        sqlBuilder.append(" `")
                        sqlBuilder.append(s)
                        sqlBuilder.append("`")
                        if (index < sorted.size - 1) {
                            sqlBuilder.append(", ")
                        }
                    }
                    sqlBuilder.append(") VALUES (")
                    for (i in 0..(sorted.size - 1)) {
                        sqlBuilder.append("?")
                        if (i < sorted.size - 1) {
                            sqlBuilder.append(",")
                        }
                    }
                    sqlBuilder.append(")")
                    val sql = sqlBuilder.toString()
                    println("sql===>${sql}")
                    val stmt = connection!!.prepareStatement(sql)
                    val properties = info.props.map {
                        it.name to it
                    }.toMap()
                    sorted.forEachIndexed { index, p ->
                        setParam(stmt, index + 1, properties[p]!!.readMethod.invoke(entity))
                    }
                    val r = stmt.execute()

                }
                1 -> {
                    val info = retrieveInfo(entity::class.java)
                    val sqlBuilder = StringBuilder()
                    //`camera_log` WHERE `id` = ?
                    sqlBuilder.append("DELETE FROM `${info.tableName}` WHERE ")
                    val sorted = info.fields.keys.sorted()
                    val primaryKeys = sorted.filter {
                        val column = info.fields[it]?.getAnnotation(Column::class.java) ?: return@filter false
                        column.primaryKey
                    }
                    if (primaryKeys.isNullOrEmpty()) {
                        throw RuntimeException("deleting need a primary key")
                    }
                    primaryKeys.forEachIndexed { index, s ->
                        sqlBuilder.append("`")
                        sqlBuilder.append(s)
                        sqlBuilder.append("` = ?")
                        if (index < primaryKeys.size - 1) {
                            sqlBuilder.append(" AND ")
                        }
                    }
                    val sql = sqlBuilder.toString()
                    println("sql===>${sql}")
                    val stmt = connection!!.prepareStatement(sql)

                    val properties = info.props.map {
                        it.name to it
                    }.toMap()
                    primaryKeys.forEachIndexed { index, p ->
                        setParam(stmt, index + 1, properties[p]!!.readMethod.invoke(entity))
                    }
                    val r = stmt.execute()

                }
                2 -> {
                    val info = retrieveInfo(entity::class.java)
                    val sqlBuilder = StringBuilder()
                    //"UPDATE OR ABORT `camera_log` SET `id` = ?,`status` = ?,`ctime` = ?,`etime` = ? WHERE `id` = ?";
                    sqlBuilder.append("UPDATE OR ABORT `${info.tableName}` SET ")
                    val sorted = info.fields.keys.sorted()
                    sorted.forEachIndexed { index, s ->
                        sqlBuilder.append(" `")
                        sqlBuilder.append(s)
                        sqlBuilder.append("` = ?")
                        if (index < sorted.size - 1) {
                            sqlBuilder.append(", ")
                        }
                    }
                    sqlBuilder.append(" WHERE ")
                    val primaryKeys = sorted.filter {
                        val column = info.fields[it]?.getAnnotation(Column::class.java) ?: return@filter false
                        column.primaryKey
                    }
                    if (primaryKeys.isNullOrEmpty()) {
                        throw RuntimeException("deleting need a primary key")
                    }
                    primaryKeys.forEachIndexed { index, s ->
                        sqlBuilder.append("`")
                        sqlBuilder.append(s)
                        sqlBuilder.append("` = ?")
                        if (index < primaryKeys.size - 1) {
                            sqlBuilder.append(" AND ")
                        }
                    }
                    val sql = sqlBuilder.toString()
                    println("sql===>${sql}")
                    val stmt = connection!!.prepareStatement(sql)
                    val properties = info.props.map {
                        it.name to it
                    }.toMap()
                    (sorted + primaryKeys).forEachIndexed { index, p ->
                        setParam(stmt, index + 1, properties[p]!!.readMethod.invoke(entity))
                    }

                    val r = stmt.execute()
                }
            }
            connection!!.commit()
            return true
        } catch (e: Exception) {
            connection!!.rollback()
            throw e
        } finally {
            connection?.autoCommit = true
            //TODO trigger
        }
    }

    private fun setParam(stmt: PreparedStatement, index: Int, v: Any?) {
        when (v) {
            null -> {
                //stmt.setNull() 暂不支持
            }
            is Int -> {
                stmt.setInt(index, v)
            }
            is Long -> {
                stmt.setLong(index, v)
            }
            is String -> {
                stmt.setString(index, v)
            }
            is Float -> {
                stmt.setFloat(index, v)
            }
            is Double -> {
                stmt.setDouble(index, v)
            }
            else -> {
                println("暂不支持的数据参数类型")
            }
        }
    }

    fun <T>query(sql: String, resolver: ResultSetResolver<T>, vararg args: Any?): T {
        //TODO lint sql
        val stmt = connection!!.prepareStatement(sql)
        args.forEachIndexed { index, p ->
            setParam(stmt, index + 1, p)
        }

        val ret: T
        stmt.executeQuery().use { ret = resolver.resolve(it) }

        return ret

    }
}