package com.hris.employees.model

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.util.*


@Serializable
data class Team(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val name: String,
    val createdAt: String? = null
)

class TeamsService(private val database: Database) {

    private val logger = LoggerFactory.getLogger(EmployeesService::class.java)

    object TeamsTable : Table("teams") {
        val id = uuid("id").clientDefault { UUID.randomUUID() }
        val name = varchar("name", 100).uniqueIndex()
        val createdAt =
            datetime("created_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
        override val primaryKey = PrimaryKey(id)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }

    suspend fun createTeam(team: Team): UUID = dbQuery {
        val newId = TeamsTable.insert {
            it[name] = team.name
        }[TeamsTable.id]
        logger.info("Created team with id: $newId")
        newId
    }

    suspend fun readTeam(id: UUID): Team? = dbQuery {
        TeamsTable.selectAll().where { TeamsTable.id eq id }
            .map {
                Team(
                    id = it[TeamsTable.id],
                    name = it[TeamsTable.name],
                    createdAt = it[TeamsTable.createdAt].toString()
                )
            }
            .singleOrNull()
    }

    suspend fun updateTeam(id: UUID, team: Team) = dbQuery {
        TeamsTable.update({ TeamsTable.id eq id }) {
            it[name] = team.name
        }
        logger.info("Updated team with id: $id")
    }

    suspend fun deleteTeam(id: UUID): Boolean = dbQuery {
        val deleted = TeamsTable.deleteWhere { TeamsTable.id eq id } > 0
        logger.info("Deleted team with id: $id, success: $deleted")
        deleted
    }

    suspend fun getAllTeams(): List<Team> = dbQuery {
        TeamsTable.selectAll().map {
            Team(
                id = it[TeamsTable.id],
                name = it[TeamsTable.name],
                createdAt = it[TeamsTable.createdAt].toString()
            )
        }
    }
}