package com.hris.employees

import com.hris.employees.service.Employee
import com.hris.employees.service.EmployeesService
import com.hris.employees.service.TeamsTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
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
            supervisorId = null,
            joiningDate = "2021-05-10"
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
            supervisorId = supId,
            joiningDate = "2021-05-10"
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
    fun updateEmployeeShouldThrowExceptionWhenCycle() =
        runBlocking {
            val employeeA = Employee(
                teamId = TEAM_ID,
                firstName = "Alice",
                lastName = "A",
                email = "alice@example.com",
                position = "Manager",
                supervisorId = null,
                joiningDate = "2021-05-10"
            )
            val aId = service.createEmployee(employeeA)
            val employeeB = Employee(
                teamId = TEAM_ID,
                firstName = "Bob",
                lastName = "B",
                email = "bob@example.com",
                position = "Developer",
                supervisorId = aId,
                joiningDate = "2021-05-10"
            )
            val bId = service.createEmployee(employeeB)
            val updatedA = employeeA.copy(supervisorId = bId)
            val exception = assertThrows(IllegalArgumentException::class.java) {
                runBlocking { service.updateEmployee(aId, updatedA) }
            }
            assertTrue(exception.message!!.contains("Cycle detected"))
        }

    @Test
    fun getEmployeeHierarchyTest() = runBlocking {
        val manager = Employee(
            teamId = TEAM_ID,
            firstName = "Manager",
            lastName = "M",
            email = "manager@example.com",
            position = "Manager",
            supervisorId = null,
            joiningDate = "2021-05-10"
        )
        val mId = service.createEmployee(manager)
        val subordinate = Employee(
            teamId = TEAM_ID,
            firstName = "Sub",
            lastName = "S",
            email = "sub@example.com",
            position = "Developer",
            supervisorId = mId,
            joiningDate = "2021-05-10"
        )
        val sId = service.createEmployee(subordinate)
        val colleague = Employee(
            teamId = TEAM_ID,
            firstName = "Colleague",
            lastName = "C",
            email = "colleague@example.com",
            position = "Developer",
            supervisorId = mId,
            joiningDate = "2021-05-10"
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

    @Test
    fun testGetManager() = runBlocking {
        val manager = Employee(
            teamId = TEAM_ID,
            firstName = "Manager",
            lastName = "One",
            email = "manager@example.com",
            position = "Manager",
            joiningDate = LocalDate.now().toString(),
            supervisorId = null
        )
        val managerId = service.createEmployee(manager)
        val subordinate = Employee(
            teamId = TEAM_ID,
            firstName = "Employee",
            lastName = "Two",
            email = "employee2@example.com",
            position = "Developer",
            joiningDate = LocalDate.now().toString(),
            supervisorId = managerId
        )
        val subordinateId = service.createEmployee(subordinate)
        val retrievedManager = service.getManager(subordinateId)
        assertNotNull(retrievedManager)
        assertEquals(managerId, retrievedManager!!.id)
    }

    @Test
    fun testGetSubordinates() = runBlocking {
        val manager = Employee(
            teamId = TEAM_ID,
            firstName = "Manager",
            lastName = "One",
            email = "manager@example.com",
            position = "Manager",
            joiningDate = LocalDate.now().toString(),
            supervisorId = null
        )
        val managerId = service.createEmployee(manager)
        val subordinate1 = Employee(
            teamId = TEAM_ID,
            firstName = "Subordinate",
            lastName = "One",
            email = "sub1@example.com",
            position = "Developer",
            joiningDate = LocalDate.now().toString(),
            supervisorId = managerId
        )
        val subordinate2 = Employee(
            teamId = TEAM_ID,
            firstName = "Subordinate",
            lastName = "Two",
            email = "sub2@example.com",
            position = "Developer",
            joiningDate = LocalDate.now().toString(),
            supervisorId = managerId
        )
        service.createEmployee(subordinate1)
        service.createEmployee(subordinate2)
        val subs = service.getSubordinates(managerId)
        assertEquals(2, subs.size)
    }

    @Test
    fun testGetColleagues() = runBlocking {
        val manager = Employee(
            teamId = TEAM_ID,
            firstName = "Manager",
            lastName = "One",
            email = "manager@example.com",
            position = "Manager",
            joiningDate = LocalDate.now().toString(),
            supervisorId = null
        )
        val managerId = service.createEmployee(manager)
        val employee1 = Employee(
            teamId = TEAM_ID,
            firstName = "Employee",
            lastName = "One",
            email = "employee1@example.com",
            position = "Developer",
            joiningDate = LocalDate.now().toString(),
            supervisorId = managerId
        )
        val employee2 = Employee(
            teamId = TEAM_ID,
            firstName = "Employee",
            lastName = "Two",
            email = "employee2@example.com",
            position = "Developer",
            joiningDate = LocalDate.now().toString(),
            supervisorId = managerId
        )
        val emp1Id = service.createEmployee(employee1)
        val emp2Id = service.createEmployee(employee2)
        val colleagues = service.getColleagues(emp1Id)
        assertEquals(2, colleagues.size)

        val colleaguesIds = colleagues.map { it.id }
        assertTrue(colleaguesIds.contains(emp2Id))
        assertTrue(colleaguesIds.contains(managerId))
    }

    @Test
    fun testCreateEmployee() = runBlocking {
        val employee = Employee(
            teamId = TEAM_ID,
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            position = "Developer",
            joiningDate = LocalDate.now().toString(),
            supervisorId = null
        )
        val newId = service.createEmployee(employee)
        val readEmp = service.readEmployee(newId)
        assertNotNull(readEmp)
        assertEquals("John", readEmp!!.firstName)

        val outboxCount = transaction(database) {
            EmployeesService.OutboxTable.selectAll().count()
        }
        assertTrue(
            outboxCount > 0,
            "Outbox should contain at least one entry after employee creation"
        )
    }

    @Test
    fun testGetEmployeesSortedPaginated() = runBlocking {
        for (i in 1..50) {
            val emp = Employee(
                teamId = TEAM_ID,
                firstName = "FirstName$i",
                lastName = "LastName$i",
                email = "employee$i@example.com",
                position = "Position$i",
                joiningDate = LocalDate.now().minusDays(i.toLong()).toString(),
                supervisorId = null
            )
            service.createEmployee(emp)
        }
        val page1 = service.getEmployeesSortedPaginated(
            sortBy = "joiningDate",
            order = SortOrder.ASC,
            page = 1,
            pageSize = 10
        )
        val page2 = service.getEmployeesSortedPaginated(
            sortBy = "joiningDate",
            order = SortOrder.ASC,
            page = 2,
            pageSize = 10
        )
        assertEquals(10, page1.size)
        assertEquals(10, page2.size)
        assertTrue(page1[0].joiningDate <= page1[1].joiningDate)
    }

    @Test
    fun testDeleteEmployee() = runBlocking {
        val emp = Employee(
            teamId = TEAM_ID,
            firstName = "Delete",
            lastName = "Me",
            email = "deleteme@example.com",
            position = "Tester",
            joiningDate = LocalDate.now().toString(),
            supervisorId = null
        )
        val empId = service.createEmployee(emp)
        val readBefore = service.readEmployee(empId)
        assertNotNull(readBefore)
        val deleted = service.deleteEmployee(empId)
        assertTrue(deleted)
        val readAfter = service.readEmployee(empId)
        assertNull(readAfter)
    }
}
