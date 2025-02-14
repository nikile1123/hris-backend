package com.hris.reviews.model

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.util.*

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
}

@Serializable
data class PerformanceReview(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    @Serializable(with = UUIDSerializer::class)
    val employeeId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val teamId: UUID,
    val reviewDate: String,
    val performance: Int,
    val softSkills: Int,
    val independence: Int,
    val aspirationForGrowth: Int
)

object OutboxTable : Table("outbox") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val eventType = varchar("event_type", 50)
    val message = varchar("message", 512)
    val employeeId = uuid("employee_id")
    val teamId = uuid("team_id")
    val processed = bool("processed").default(false)
    val createdAt =
        datetime("created_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
}

object PerformanceReviewsTable : Table("performance_reviews") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val employeeId = uuid("employee_id")
    val teamId = uuid("team_id")
    val reviewDate = date("review_date")
    val performance = integer("performance")
    val softSkills = integer("soft_skills")
    val independence = integer("independence")
    val aspirationForGrowth = integer("aspiration_for_growth")
    override val primaryKey = PrimaryKey(id)
}

class PerformanceReviewService(private val database: Database) {
    private val logger =
        LoggerFactory.getLogger(PerformanceReviewService::class.java)

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }

    suspend fun createReview(review: PerformanceReview): UUID = dbQuery {
        val newId = PerformanceReviewsTable.insert {
            it[employeeId] = review.employeeId
            it[teamId] = review.teamId
            it[reviewDate] = java.time.LocalDate.parse(review.reviewDate)
            it[performance] = review.performance
            it[softSkills] = review.softSkills
            it[independence] = review.independence
            it[aspirationForGrowth] = review.aspirationForGrowth
        }[PerformanceReviewsTable.id]
        OutboxTable.insert {
            it[employeeId] = review.employeeId
            it[teamId] = review.teamId
            it[eventType] = "review.created"
            it[message] =
                "Review created on ${review.reviewDate}, Performance: ${review.performance}, Soft skills: ${review.softSkills}, Independence: ${review.independence}, Aspiration for growth: ${review.aspirationForGrowth}"
        }
        logger.info("Created review with id: $newId")
        newId
    }

    suspend fun getReviewById(id: UUID): PerformanceReview? = dbQuery {
        PerformanceReviewsTable.selectAll()
            .where { PerformanceReviewsTable.id eq id }
            .map { rowToReview(it) }
            .singleOrNull()
    }

    suspend fun getReviewsByEmployeeId(employeeId: UUID): List<PerformanceReview> =
        dbQuery {
            PerformanceReviewsTable.selectAll()
                .where { PerformanceReviewsTable.employeeId eq employeeId }
                .map { rowToReview(it) }
        }

    suspend fun getAllReviews(): List<PerformanceReview> = dbQuery {
        PerformanceReviewsTable.selectAll().map { rowToReview(it) }
    }

    suspend fun updateReview(id: UUID, review: PerformanceReview) {
        dbQuery {
            PerformanceReviewsTable.update({ PerformanceReviewsTable.id eq id }) {
                it[employeeId] = review.employeeId
                it[teamId] = review.teamId
                it[reviewDate] = java.time.LocalDate.parse(review.reviewDate)
                it[performance] = review.performance
                it[softSkills] = review.softSkills
                it[independence] = review.independence
                it[aspirationForGrowth] = review.aspirationForGrowth
            }
            OutboxTable.insert {
                it[employeeId] = review.employeeId
                it[teamId] = review.teamId
                it[eventType] = "review.updated"
                it[message] =
                    "Review updated on ${review.reviewDate}, Performance: ${review.performance}, Soft skills: ${review.softSkills}, Independence: ${review.independence}, Aspiration for growth: ${review.aspirationForGrowth}"
            }
            logger.info("Updated review with id: $id")
        }
    }


    suspend fun deleteReview(id: UUID): Boolean = dbQuery {
        val reviewRow = PerformanceReviewsTable.selectAll()
            .where { PerformanceReviewsTable.id eq id }.singleOrNull()
        val deleted =
            PerformanceReviewsTable.deleteWhere { PerformanceReviewsTable.id eq id } > 0
        if (deleted && reviewRow != null) {
            val curEmployeeId = reviewRow[PerformanceReviewsTable.employeeId]
            val curTeamId = reviewRow[PerformanceReviewsTable.teamId]
            val reviewDate =
                reviewRow[PerformanceReviewsTable.reviewDate].toString()
            OutboxTable.insert {
                it[employeeId] = curEmployeeId
                it[teamId] = curTeamId
                it[eventType] = "review.deleted"
                it[message] = "Review deleted which was created on $reviewDate"
            }
        }
        logger.info("Deleted review with id: $id, success: $deleted")
        deleted
    }


    private fun rowToReview(row: ResultRow): PerformanceReview =
        PerformanceReview(
            id = row[PerformanceReviewsTable.id],
            employeeId = row[PerformanceReviewsTable.employeeId],
            teamId = row[PerformanceReviewsTable.teamId],
            reviewDate = row[PerformanceReviewsTable.reviewDate].toString(),
            performance = row[PerformanceReviewsTable.performance],
            softSkills = row[PerformanceReviewsTable.softSkills],
            independence = row[PerformanceReviewsTable.independence],
            aspirationForGrowth = row[PerformanceReviewsTable.aspirationForGrowth]
        )
}
