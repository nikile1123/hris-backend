package com.hris.employees.model

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
data class Employee(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    @Serializable(with = UUIDSerializer::class)
    val teamId: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val position: String,
    @Serializable(with = UUIDSerializer::class)
    val supervisorId: UUID? = null
)

@Serializable
data class EmployeeHierarchy(
    val manager: Employee?,
    val subordinates: List<Employee>,
    val colleagues: List<Employee>
)

class EmployeesService(private val database: Database) {

    private val logger = LoggerFactory.getLogger(EmployeesService::class.java)

    object EmployeesTable : Table("employees") {
        val id = uuid("id").clientDefault { UUID.randomUUID() }
        val teamId = reference("team_id", TeamsTable.id)
        val firstName = varchar("first_name", 50)
        val lastName = varchar("last_name", 50)
        val email = varchar("email", 100).uniqueIndex()
        val position = varchar("position", 50)
        val supervisorId = reference("supervisor_id", id).nullable()
        val subordinatesCount = integer("subordinates_count").default(0)
        override val primaryKey = PrimaryKey(id)
    }

    object OutboxTable : Table("outbox") {
        val id = uuid("id").clientDefault { UUID.randomUUID() }
        val eventType = varchar("event_type", 50)
        val message = varchar("message", 512)
        val processed = bool("processed").default(false)
        val employeeId = uuid("employee_id")
        val teamId = uuid("team_id")
        val createdAt = datetime("created_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
        override val primaryKey = PrimaryKey(id)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }

    private fun rowToEmployee(row: ResultRow): Employee = Employee(
        id = row[EmployeesTable.id],
        teamId = row[EmployeesTable.teamId],
        firstName = row[EmployeesTable.firstName],
        lastName = row[EmployeesTable.lastName],
        email = row[EmployeesTable.email],
        position = row[EmployeesTable.position],
        supervisorId = row[EmployeesTable.supervisorId]
    )

    //TODO: up, down, all
    suspend fun getEmployeeHierarchy(employeeId: UUID): EmployeeHierarchy = dbQuery {
        val employee = EmployeesTable.selectAll()
            .where { EmployeesTable.id eq employeeId }
            .map { rowToEmployee(it) }
            .singleOrNull() ?: throw IllegalArgumentException("Employee not found")
        val manager = employee.supervisorId?.let { supId ->
            EmployeesTable.selectAll().where { EmployeesTable.id eq supId }
                .map { rowToEmployee(it) }
                .singleOrNull()
        }
        val subordinates = EmployeesTable.selectAll()
            .where { EmployeesTable.supervisorId eq employeeId }
            .map { rowToEmployee(it) }
        val colleagues = EmployeesTable.selectAll()
            .where { (EmployeesTable.teamId eq employee.teamId) and (EmployeesTable.id neq employeeId) }
            .map { rowToEmployee(it) }
        EmployeeHierarchy(manager, subordinates, colleagues)
    }

    suspend fun createEmployee(employee: Employee): UUID = dbQuery {
        val newId = EmployeesTable.insert {
            it[firstName] = employee.firstName
            it[lastName] = employee.lastName
            it[email] = employee.email
            it[position] = employee.position
            it[teamId] = employee.teamId
            it[supervisorId] = employee.supervisorId
        }[EmployeesTable.id]
        if (employee.supervisorId != null) {
            EmployeesTable.update({ EmployeesTable.id eq employee.supervisorId }) {
                with(SqlExpressionBuilder) { it.update(subordinatesCount, subordinatesCount + 1) }
            }
        }
        val mes = "Employee ${employee.firstName} ${employee.lastName} created."
        OutboxTable.insert {
            it[employeeId] = newId
            it[teamId] = employee.teamId
            it[eventType] = "employee.created"
            it[message] = mes
        }
        logger.info("Created employee with id: $newId")
        newId
    }

    suspend fun readEmployee(id: UUID): Employee? = dbQuery {
        EmployeesTable.selectAll().where { EmployeesTable.id eq id }
            .map { rowToEmployee(it) }
            .singleOrNull()
    }

    suspend fun isCycle(supervisorId: UUID?, employeeId: UUID): Boolean = dbQuery {
        var currentSupervisorId = supervisorId
        while (currentSupervisorId != null) {
            if (currentSupervisorId == employeeId) return@dbQuery true
            currentSupervisorId = EmployeesTable.selectAll()
                .where { EmployeesTable.id eq currentSupervisorId!! }
                .map { it[EmployeesTable.supervisorId] }
                .singleOrNull()
        }
        false
    }

    suspend fun updateEmployee(id: UUID, employee: Employee) {
        dbQuery {
            if (employee.supervisorId != null && isCycle(employee.supervisorId, id)) {
                throw IllegalArgumentException("Cycle detected: supervisor is subordinate")
            }
            val currentSupervisorId = EmployeesTable.selectAll()
                .where { EmployeesTable.id eq id }
                .map { it[EmployeesTable.supervisorId] }
                .singleOrNull()
            if (currentSupervisorId != employee.supervisorId) {
                if (currentSupervisorId != null) {
                    EmployeesTable.update({ EmployeesTable.id eq currentSupervisorId }) {
                        with(SqlExpressionBuilder) { it.update(subordinatesCount, subordinatesCount - 1) }
                    }
                }
                if (employee.supervisorId != null) {
                    EmployeesTable.update({ EmployeesTable.id eq employee.supervisorId }) {
                        with(SqlExpressionBuilder) { it.update(subordinatesCount, subordinatesCount + 1) }
                    }
                }
            }
            EmployeesTable.update({ EmployeesTable.id eq id }) {
                it[teamId] = employee.teamId
                it[firstName] = employee.firstName
                it[lastName] = employee.lastName
                it[email] = employee.email
                it[position] = employee.position
                it[supervisorId] = employee.supervisorId
            }
            val mes = "Employee ${employee.firstName} ${employee.lastName} updated with id $id."
            OutboxTable.insert {
                it[employeeId] = id
                it[teamId] = employee.teamId
                it[eventType] = "employee.updated"
                it[message] = mes
            }
        }
        logger.info("Updated employee with id: $id")
    }

    suspend fun deleteEmployee(id: UUID): Boolean = dbQuery {
        val employeeRow = EmployeesTable.selectAll()
            .where { EmployeesTable.id eq id }.singleOrNull()
        if (employeeRow == null) return@dbQuery false
        val employee = rowToEmployee(employeeRow)
        val subordinateCount = employeeRow[EmployeesTable.subordinatesCount]
        val managerSupervisor = employeeRow[EmployeesTable.supervisorId]
        if (subordinateCount > 0) {
            EmployeesTable.update({ EmployeesTable.supervisorId eq id }) {
                it[supervisorId] = managerSupervisor
            }
            if (managerSupervisor != null) {
                EmployeesTable.update({ EmployeesTable.id eq managerSupervisor }) {
                    with(SqlExpressionBuilder) { it.update(subordinatesCount, subordinatesCount + subordinateCount) }
                }
            }
        }
        if (managerSupervisor != null) {
            EmployeesTable.update({ EmployeesTable.id eq managerSupervisor }) {
                with(SqlExpressionBuilder) { it.update(subordinatesCount, subordinatesCount - 1) }
            }
        }
        val deleted = EmployeesTable.deleteWhere { EmployeesTable.id eq id } > 0
        if (deleted) {
            val mes = "Employee ${employee.firstName} ${employee.lastName} deleted."
            OutboxTable.insert {
                it[employeeId] = id
                it[teamId] = employee.teamId
                it[eventType] = "employee.deleted"
                it[message] = mes
            }
        }
        logger.info("Deleted employee with id: $id, success: $deleted")
        deleted
    }

    suspend fun getAllEmployees(): List<Employee> = dbQuery {
        EmployeesTable.selectAll().map { rowToEmployee(it) }
    }
}
