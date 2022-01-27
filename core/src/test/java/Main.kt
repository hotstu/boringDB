import io.github.hotstu.boring.DatabaseBuilder
import io.github.hotstu.boring.anno.*

@Database(version = 1,
        entities = [
            User::class
        ])
interface AppDatabase {
    fun appDao(): AppDao
}

@Entity(tableName = "user", unique = ["name"])
data class User(
        @Column(primaryKey = true, autoIncrement = true) var id: Long = 0,
        var name: String
) {
    constructor():this(id = 0, name = "")

    override fun toString(): String {
        return "id=${id},name=${name}"
    }
}


@Dao
interface AppDao {
    @Query("select * from user")
    fun query(): List<User>

    @Query("select * from user WHERE id = ?")
    fun queryById(id: Long): User

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
                name = "zhangshan2"
        )
        dao.add(user)
        dao.add(User(
                name = "admin"
        ))
        val list = dao.query()
        println("查询结果:$list,count=${dao.count()}")
        dao.update(list[0].apply {
            name = "法外狂徒"
        })
        println(dao.queryById(list[0]!!.id))
        println("查询结果:${dao.query()}")
        dao.delete(list[0])
        println("查询结果:${dao.query()}")
    }
}