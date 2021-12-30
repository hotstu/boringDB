package com.github.hotstu.boringapp

import com.github.hotstu.boring.BaseDateBase
import com.github.hotstu.boring.DatabaseBuilder
import com.github.hotstu.boring.anno.*

@Database(version = 1,
        entities = [
            User::class
        ])
interface AppDatabase : BaseDateBase {
    fun appDao(): AppDao
}

@Entity(tableName = "user")
data class User(
        @Column(primaryKey = true) var id: String,
        var name: String
) {
    constructor():this(id = "", name = "")

    override fun toString(): String {
        return "id=${id},name=${name}"
    }
}


@Dao
interface AppDao {
    @Query("select * from user")
    fun query(): List<User>

    @Query("select * from user WHERE id = ?")
    fun queryById(id: String): User

    @Query("select COUNT(*) from user")
    fun count(): Int

    @Insert
    fun add(user: User): Boolean

    @Update
    fun update(user: User): Boolean

    @Delete
    fun delete(user: User): Boolean
}


object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val database = DatabaseBuilder(AppDatabase::class.java).build()
        val dao: AppDao = database.appDao()
        println("dao = ${dao}")
        val user = User(
            id = "233",
            name = "zhangshan2"
        )
        dao.add(user)
        dao.add(User(
            id = "1",
            name = "admin"
        ))
        println("查询结果:${dao.query()},count=${dao.count()}")
        dao.update(user.apply {
            name = "法外狂徒"
        })
        println(dao.queryById("233"))
        println("查询结果:${dao.query()}")
        dao.delete(user)
        println("查询结果:${dao.query()}")
    }
}