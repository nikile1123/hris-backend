package com.hris.employees

import com.hris.employees.service.Team
import com.hris.employees.service.TeamsService
import com.hris.employees.service.TeamsTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TeamsServiceTest {

    private lateinit var database: Database
    private lateinit var service: TeamsService

    @BeforeAll
    fun setupAll() {
        database = Database.connect(
            "jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )
    }

    @BeforeEach
    fun setup() {
        transaction(database) {
            SchemaUtils.create(TeamsTable)
        }
        service = TeamsService(database)
    }

    @AfterEach
    fun tearDown() {
        transaction(database) {
            SchemaUtils.drop(TeamsTable)
        }
    }

    @Test
    fun testCreateTeam() = runBlocking {
        val team = Team(
            name = "Alpha",
            createdAt = LocalDate.now().toString()
        )
        val newId = service.createTeam(team)
        val readTeam = service.readTeam(newId)
        assertNotNull(readTeam)
        assertEquals("Alpha", readTeam!!.name)
    }

    @Test
    fun testReadTeam() = runBlocking {
        val team = Team(
            name = "Beta",
            createdAt = LocalDate.now().toString()
        )
        val newId = service.createTeam(team)
        val readTeam = service.readTeam(newId)
        assertNotNull(readTeam)
        assertEquals("Beta", readTeam!!.name)
    }

    @Test
    fun testUpdateTeam() = runBlocking {
        val team = Team(
            name = "Gamma",
            createdAt = LocalDate.now().toString()
        )
        val newId = service.createTeam(team)
        val updatedTeam = team.copy(name = "Gamma Updated")
        service.updateTeam(newId, updatedTeam)
        val readTeam = service.readTeam(newId)
        assertNotNull(readTeam)
        assertEquals("Gamma Updated", readTeam!!.name)
    }

    @Test
    fun testDeleteTeam() = runBlocking {
        val team = Team(
            name = "Delta",
            createdAt = LocalDate.now().toString()
        )
        val newId = service.createTeam(team)
        val readBefore = service.readTeam(newId)
        assertNotNull(readBefore)
        val deleted = service.deleteTeam(newId)
        assertTrue(deleted)
        val readAfter = service.readTeam(newId)
        assertNull(readAfter)
    }

    @Test
    fun testGetAllTeams() = runBlocking {
        val team1 =
            Team(name = "Team A", createdAt = LocalDate.now().toString())
        val team2 =
            Team(name = "Team B", createdAt = LocalDate.now().toString())
        service.createTeam(team1)
        service.createTeam(team2)
        val allTeams = service.getAllTeams()
        assertTrue(allTeams.size >= 2)
    }

    @Test
    fun testGetTeamsSortedPaginated() = runBlocking {
        for (i in 1..30) {
            val team = Team(
                name = "Team $i",
                createdAt = LocalDate.now().toString()
            )
            service.createTeam(team)
        }
        val paginatedTeams = service.getTeamsSortedPaginated(
            sortBy = "name",
            order = SortOrder.ASC,
            page = 2,
            pageSize = 10
        )
        assertEquals(10, paginatedTeams.size)
        for (i in 0 until paginatedTeams.size - 1) {
            assertTrue(paginatedTeams[i].name <= paginatedTeams[i + 1].name)
        }
    }
}
