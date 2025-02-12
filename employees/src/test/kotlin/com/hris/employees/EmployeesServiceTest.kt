package com.hris.employees

import com.hris.employees.model.Employee
import com.hris.employees.model.EmployeesService
import com.hris.employees.model.TeamsTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

private val TEAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111")

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmployeesServiceTest {

    private lateinit var database: Database
    private lateinit var service: EmployeesService

    @BeforeEach
    fun setup() {
        database = Database.connect(
            "jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )
        transaction(database) {
            SchemaUtils.create(
                TeamsTable,
                EmployeesService.EmployeesTable, EmployeesService.OutboxTable
            )
            TeamsTable.insert {
                it[id] = TEAM_ID
                it[name] = "Development"
            }
        }
        service = EmployeesService(database)
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                EmployeesService.OutboxTable,
                EmployeesService.EmployeesTable,
                TeamsTable
            )
        }
    }

    @Test
    fun createEmployeeUpdatesSupervisorSubordinatesCount() = runBlocking {
        val supervisor = Employee(
            teamId = TEAM_ID,
            firstName = "Supervisor",
            lastName = "One",
            email = "sup.one@example.com",
            position = "Manager",
            supervisorId = null
        )
        val supId = service.createEmployee(supervisor)
        var supRow = transaction(database) {
            EmployeesService.EmployeesTable.selectAll()
                .where { EmployeesService.EmployeesTable.id eq supId }.single()
        }
        assertEquals(
            0,
            supRow[EmployeesService.EmployeesTable.subordinatesCount]
        )

        val subordinate = Employee(
            teamId = supRow[EmployeesService.EmployeesTable.teamId],
            firstName = "Subordinate",
            lastName = "One",
            email = "sub.one@example.com",
            position = "Developer",
            supervisorId = supId
        )
        service.createEmployee(subordinate)
        supRow = transaction(database) {
            EmployeesService.EmployeesTable.selectAll()
                .where { EmployeesService.EmployeesTable.id eq supId }.single()
        }
        assertEquals(
            1,
            supRow[EmployeesService.EmployeesTable.subordinatesCount]
        )
    }

    @Test
    fun `updateEmployee should throw exception when cycle is detected`() =
        runBlocking {
            val employeeA = Employee(
                teamId = TEAM_ID,
                firstName = "Alice",
                lastName = "A",
                email = "alice@example.com",
                position = "Manager",
                supervisorId = null
            )
            val aId = service.createEmployee(employeeA)
            val employeeB = Employee(
                teamId = TEAM_ID,
                firstName = "Bob",
                lastName = "B",
                email = "bob@example.com",
                position = "Developer",
                supervisorId = aId
            )
            val bId = service.createEmployee(employeeB)
            val updatedA = employeeA.copy(supervisorId = bId)
            val exception = assertThrows(IllegalArgumentException::class.java) {
                runBlocking { service.updateEmployee(aId, updatedA) }
            }
            assertTrue(exception.message!!.contains("Cycle detected"))
        }

    @Test
    fun `getEmployeeHierarchy should return correct hierarchy`() = runBlocking {
        val manager = Employee(
            teamId = TEAM_ID,
            firstName = "Manager",
            lastName = "M",
            email = "manager@example.com",
            position = "Manager",
            supervisorId = null
        )
        val mId = service.createEmployee(manager)
        val subordinate = Employee(
            teamId = TEAM_ID,
            firstName = "Sub",
            lastName = "S",
            email = "sub@example.com",
            position = "Developer",
            supervisorId = mId
        )
        val sId = service.createEmployee(subordinate)
        val colleague = Employee(
            teamId = TEAM_ID,
            firstName = "Colleague",
            lastName = "C",
            email = "colleague@example.com",
            position = "Developer",
            supervisorId = mId
        )
        val cId = service.createEmployee(colleague)
        val hierarchy = service.getEmployeeHierarchy(sId)
        assertNotNull(hierarchy.manager)
        assertEquals(mId, hierarchy.manager?.id)
        assertTrue(hierarchy.subordinates.isEmpty())
        assertEquals(2, hierarchy.colleagues.size)

        val colleaguesIds = hierarchy.colleagues.map { it.id }
        assertTrue(colleaguesIds.contains(cId))
        assertTrue(colleaguesIds.contains(mId))
    }
}
