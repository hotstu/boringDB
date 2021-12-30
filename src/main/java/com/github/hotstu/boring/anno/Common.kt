package com.github.hotstu.boring.anno

import javax.management.Query
import kotlin.reflect.KClass


@Target(AnnotationTarget.CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Database(

        /**
         * The database version.
         *
         * @return The database version.
         */
        val version: Int,
        /**
         * The list of entities included in the database. Each entity turns into a table in the
         * database.
         *
         * @return The list of entities in the database.
         */
        val entities: Array<KClass<*>>,

        )


/**
 * Marks the class as a Data Access Object.
 *
 *
 * Data Access Objects are the main classes where you define your database interactions. They can
 * include a variety of query methods.
 *
 *
 * The class marked with `@Dao` should either be an interface or an abstract class. At compile
 * time, Room will generate an implementation of this class when it is referenced by a
 * [Database].
 *
 *
 * An abstract `@Dao` class can optionally have a constructor that takes a [Database]
 * as its only parameter.
 *
 *
 * It is recommended to have multiple `Dao` classes in your codebase depending on the tables
 * they touch.
 *
 * @see Query
 *
 * @see Delete
 *
 * @see Insert
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Dao


@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Entity(
        val tableName: String = "",
)

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(
        val name: String = "", val primaryKey: Boolean = false
)


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Query(
        /**
         * The SQLite query to be run.
         * @return The query to be run.
         */
        val value: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Insert()


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Update()

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Delete()