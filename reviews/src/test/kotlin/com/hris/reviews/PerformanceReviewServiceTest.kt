package com.hris.reviews

import com.hris.reviews.service.OutboxTable
import com.hris.reviews.service.PerformanceReview
import com.hris.reviews.service.PerformanceReviewService
import com.hris.reviews.service.PerformanceReviewsTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PerformanceReviewServiceTest {

    private lateinit var database: Database
    private lateinit var service: PerformanceReviewService

    private val TEAM_ID: UUID =
        UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val EMPLOYEE_ID: UUID =
        UUID.fromString("11dc4a8e-c1f4-4684-8521-eb5716d65574")

    @BeforeAll
    fun setupAll() {
        database = Database.connect(
            "jdbc:h2:mem:reviewsTest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )
    }

    @BeforeEach
    fun setup() {
        transaction(database) {
            SchemaUtils.create(PerformanceReviewsTable, OutboxTable)
        }
        service = PerformanceReviewService(database)
    }

    @AfterEach
    fun tearDown() {
        transaction(database) {
            SchemaUtils.drop(OutboxTable, PerformanceReviewsTable)
        }
    }

    @Test
    fun testCreateReview() = runBlocking {
        val review = PerformanceReview(
            teamId = TEAM_ID,
            employeeId = EMPLOYEE_ID,
            reviewDate = LocalDate.now().toString(),
            performance = 8,
            softSkills = 7,
            independence = 9,
            aspirationForGrowth = 8
        )
        val newId = service.createReview(review)
        val readReview = service.getReviewById(newId)
        assertNotNull(readReview)
        assertEquals(8, readReview!!.performance)

        val outboxCount = transaction(database) {
            OutboxTable.selectAll().count()
        }
        assertTrue(
            outboxCount > 0,
            "Outbox should contain at least one entry after review creation"
        )
    }

    @Test
    fun testGetReviewById() = runBlocking {
        val review = PerformanceReview(
            teamId = TEAM_ID,
            employeeId = EMPLOYEE_ID,
            reviewDate = LocalDate.now().toString(),
            performance = 9,
            softSkills = 8,
            independence = 7,
            aspirationForGrowth = 9
        )
        val newId = service.createReview(review)
        val retrievedReview = service.getReviewById(newId)
        assertNotNull(retrievedReview)
        assertEquals(9, retrievedReview!!.performance)
    }

    @Test
    fun testGetReviewsByEmployeeId() = runBlocking {
        repeat(3) {
            val review = PerformanceReview(
                teamId = TEAM_ID,
                employeeId = EMPLOYEE_ID,
                reviewDate = LocalDate.now().minusDays(it.toLong()).toString(),
                performance = 7 + it,
                softSkills = 6 + it,
                independence = 8,
                aspirationForGrowth = 7
            )
            service.createReview(review)
        }
        val reviews = service.getReviewsByEmployeeId(EMPLOYEE_ID)
        assertEquals(3, reviews.size)
    }

    @Test
    fun testGetAllReviews() = runBlocking {
        repeat(5) {
            val review = PerformanceReview(
                teamId = TEAM_ID,
                employeeId = EMPLOYEE_ID,
                reviewDate = LocalDate.now().minusDays(it.toLong()).toString(),
                performance = 5 + it,
                softSkills = 5 + it,
                independence = 5 + it,
                aspirationForGrowth = 5 + it
            )
            service.createReview(review)
        }
        val allReviews = service.getAllReviews()
        assertTrue(allReviews.size >= 5)
    }

    @Test
    fun testUpdateReview() = runBlocking {
        val review = PerformanceReview(
            teamId = TEAM_ID,
            employeeId = EMPLOYEE_ID,
            reviewDate = LocalDate.now().toString(),
            performance = 6,
            softSkills = 6,
            independence = 6,
            aspirationForGrowth = 6
        )
        val newId = service.createReview(review)
        val updatedReview = review.copy(
            performance = 10,
            softSkills = 10,
            independence = 10,
            aspirationForGrowth = 10,
            reviewDate = LocalDate.now().toString()
        )
        service.updateReview(newId, updatedReview)
        val retrievedReview = service.getReviewById(newId)
        assertNotNull(retrievedReview)
        assertEquals(10, retrievedReview!!.performance)
    }

    @Test
    fun testDeleteReview() = runBlocking {
        val review = PerformanceReview(
            teamId = TEAM_ID,
            employeeId = EMPLOYEE_ID,
            reviewDate = LocalDate.now().toString(),
            performance = 7,
            softSkills = 7,
            independence = 7,
            aspirationForGrowth = 7
        )
        val newId = service.createReview(review)
        val readBefore = service.getReviewById(newId)
        assertNotNull(readBefore)
        val deleted = service.deleteReview(newId)
        assertTrue(deleted)
        val readAfter = service.getReviewById(newId)
        assertNull(readAfter)
    }

    @Test
    fun testGetReviewsSortedPaginated() = runBlocking {
        for (i in 1..30) {
            val review = PerformanceReview(
                teamId = TEAM_ID,
                employeeId = EMPLOYEE_ID,
                reviewDate = LocalDate.now().minusDays(i.toLong()).toString(),
                performance = i % 10 + 1,
                softSkills = (i % 10) + 1,
                independence = (i % 10) + 1,
                aspirationForGrowth = (i % 10) + 1
            )
            service.createReview(review)
        }
        val paginatedReviews = service.getReviewsSortedPaginated(
            sortBy = "reviewDate",
            order = SortOrder.ASC,
            page = 2,
            pageSize = 10
        )
        assertEquals(10, paginatedReviews.size)
        for (i in 0 until paginatedReviews.size - 1) {
            assertTrue(
                paginatedReviews[i].reviewDate <= paginatedReviews[i + 1].reviewDate,
                "Reviews are not sorted in ascending order by reviewDate"
            )
        }
    }
}