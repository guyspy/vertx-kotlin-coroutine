package example.db

import com.mongodb.async.client.MongoClient
import com.mongodb.async.client.MongoDatabase
import example.models.Post
import io.vertx.core.Vertx
import io.vertx.core.shareddata.Shareable
import org.litote.kmongo.async.KMongo
import org.litote.kmongo.coroutine.createIndex
import org.litote.kmongo.coroutine.getCollection

class KMongo(val vertx: Vertx)  {

  data class MongoHolder(private val client: MongoClient) : Shareable {
    val db: MongoDatabase = client.getDatabase("test")
  }

  private fun getDB(): MongoDatabase {
    val localMap = vertx.sharedData().getLocalMap<String, MongoHolder>("__vertx.MongoClient.datasources")
    val holder = localMap.computeIfAbsent("DEFAULT_POOL", { _ -> MongoHolder(KMongo.createClient()) })
    return holder.db
  }

  suspend fun ensureIndexes() {
    getDB().getCollection<Post>().createIndex("{title:1}")
  }

}