import http from 'k6/http';
import { sleep, check, group } from 'k6';

// Define multiple scenarios using k6's multi-scenario options.
export let options = {
  scenarios: {
    // Load test for creating performance reviews for many employees (simulating weekly review for 10k employees)
    createReviews: {
      executor: 'ramping-arrival-rate',
      startRate: 10,                // start at 10 iterations per second
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { target: 200, duration: '1m' },
        { target: 200, duration: '3m' },
        { target: 10, duration: '1m' }
      ],
      exec: 'createReviews',
    },
    // Stress test for hierarchy retrieval (simulate many simultaneous hierarchy requests)
    stressHierarchy: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { target: 300, duration: '2m' },
        { target: 300, duration: '2m' },
        { target: 10, duration: '2m' }
      ],
      exec: 'getHierarchy',
    },
    // Spike test for personnel updates (sudden burst of updates)
    spikeUpdates: {
      executor: 'constant-arrival-rate',
      rate: 100,                  // 100 iterations per second (spike)
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      duration: '30s',
      exec: 'updateEmployees',
    },
    // Soak test for employee listing (continuous moderate load over an extended period)
    soakEmployees: {
      executor: 'constant-vus',
      vus: 50,
      duration: '10m',
      exec: 'getEmployees',
    },
  },
};

// Adjust these IDs to match your seed data
const EXISTING_TEAM_ID = "11111111-1111-1111-1111-111111111111";
const EXISTING_EMPLOYEE_ID = "11dc4a8e-c1f4-4684-8521-eb5716d65574";

// Scenario: Create Performance Reviews
export function createReviews() {
  // Simulate creation of a performance review.
  let payload = JSON.stringify({
    employeeId: EXISTING_EMPLOYEE_ID,  // In a real test, this might be randomized over your 10k employees
    teamId: EXISTING_TEAM_ID,
    reviewDate: "2025-02-11",
    performance: Math.floor(Math.random() * 10) + 1,
    softSkills: Math.floor(Math.random() * 10) + 1,
    independence: Math.floor(Math.random() * 10) + 1,
    aspirationForGrowth: Math.floor(Math.random() * 10) + 1
  });
  let params = { headers: { "Content-Type": "application/json" } };
  let res = http.post("http://localhost:8083/performance_reviews", payload, params);
  check(res, { "review created": (r) => r.status === 201 });
  sleep(1);
}

// Scenario: Retrieve Employee Hierarchy
export function getHierarchy() {
  // Simulate retrieval of hierarchy for a given employee
  let res = http.get(`http://localhost:8081/employees/${EXISTING_EMPLOYEE_ID}/hierarchy`);
  check(res, { "hierarchy retrieved": (r) => r.status === 200 });
  sleep(1);
}

// Scenario: Update Employee (simulate personnel changes)
export function updateEmployees() {
  // For spike, simulate an update with a random new email and position
  let payload = JSON.stringify({
    teamId: EXISTING_TEAM_ID,
    firstName: "Updated",
    lastName: "User",
    email: `updated_${__VU}_${__ITER}@example.com`,
    position: "Senior Developer",
    supervisorId: null // or a valid supervisor ID if needed
  });
  let params = { headers: { "Content-Type": "application/json" } };
  let res = http.put(`http://localhost:8081/employees/${EXISTING_EMPLOYEE_ID}`, payload, params);
  check(res, { "employee updated": (r) => r.status === 200 });
  sleep(1);
}

// Scenario: Get All Employees (soak test)
export function getEmployees() {
  let res = http.get("http://localhost:8081/employees");
  check(res, { "employees list retrieved": (r) => r.status === 200 });
  sleep(1);
}
