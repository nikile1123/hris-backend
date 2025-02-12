package com.hris.employees

import com.hris.employees.model.Employee
import com.hris.employees.model.EmployeesService
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmployeesServiceTest {

    private lateinit var database: Database
    private lateinit var service: EmployeesService

    @BeforeAll
    fun setup() {
        // Подключаемся к in-memory H2 базе в режиме PostgreSQL
        database = Database.connect("jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        service = EmployeesService(database)
    }

    @Test
    fun createEmployeeUpdatesSupervisorSubordinatesCount() = runBlocking {
        // Создаем руководителя (без supervisor)
        val supervisor = Employee(
            teamId = UUID.randomUUID(),
            firstName = "Supervisor",
            lastName = "One",
            email = "sup.one@example.com",
            position = "Manager",
            supervisorId = null
        )
        val supId = service.createEmployee(supervisor)
        // Проверяем, что у руководителя подчинённых 0
        var supRow = transaction(database) {
            EmployeesService.EmployeesTable.selectAll()
                .where { EmployeesService.EmployeesTable.id eq supId }.single()
        }
        assertEquals(0, supRow[EmployeesService.EmployeesTable.subordinatesCount])

        // Создаем подчиненного с supervisorId = supId
        val subordinate = Employee(
            teamId = supRow[EmployeesService.EmployeesTable.teamId],
            firstName = "Subordinate",
            lastName = "One",
            email = "sub.one@example.com",
            position = "Developer",
            supervisorId = supId
        )
        val subId = service.createEmployee(subordinate)
        // Перечитываем данные руководителя
        supRow = transaction(database) {
            EmployeesService.EmployeesTable.selectAll()
                .where { EmployeesService.EmployeesTable.id eq supId }.single()
        }
        assertEquals(1, supRow[EmployeesService.EmployeesTable.subordinatesCount])
    }

    @Test
    fun `updateEmployee should throw exception when cycle is detected`() = runBlocking {
        // Создаем сотрудника A (руководитель)
        val employeeA = Employee(
            teamId = UUID.randomUUID(),
            firstName = "Alice",
            lastName = "A",
            email = "alice@example.com",
            position = "Manager",
            supervisorId = null
        )
        val aId = service.createEmployee(employeeA)
        // Создаем сотрудника B с supervisor A
        val employeeB = Employee(
            teamId = employeeA.teamId,
            firstName = "Bob",
            lastName = "B",
            email = "bob@example.com",
            position = "Developer",
            supervisorId = aId
        )
        val bId = service.createEmployee(employeeB)
        // Попытка обновить сотрудника A, установив его supervisor равным B, что создаст цикл
        val updatedA = employeeA.copy(supervisorId = bId)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { service.updateEmployee(aId, updatedA) }
        }
        assertTrue(exception.message!!.contains("Cycle detected"))
    }

    @Test
    fun `getEmployeeHierarchy should return correct hierarchy`() = runBlocking {
        // Создаем команду и сотрудников: менеджера, подчиненного и коллегу
        val teamId = UUID.randomUUID()
        val manager = Employee(
            teamId = teamId,
            firstName = "Manager",
            lastName = "M",
            email = "manager@example.com",
            position = "Manager",
            supervisorId = null
        )
        val mId = service.createEmployee(manager)
        val subordinate = Employee(
            teamId = teamId,
            firstName = "Sub",
            lastName = "S",
            email = "sub@example.com",
            position = "Developer",
            supervisorId = mId
        )
        val sId = service.createEmployee(subordinate)
        val colleague = Employee(
            teamId = teamId,
            firstName = "Colleague",
            lastName = "C",
            email = "colleague@example.com",
            position = "Developer",
            supervisorId = mId
        )
        val cId = service.createEmployee(colleague)
        // Получаем иерархию для подчиненного
        val hierarchy = service.getEmployeeHierarchy(sId)
        assertNotNull(hierarchy.manager)
        assertEquals(mId, hierarchy.manager?.id)
        assertTrue(hierarchy.subordinates.isEmpty())
        assertEquals(1, hierarchy.colleagues.size)
        assertEquals(cId, hierarchy.colleagues.first().id)
    }
}
