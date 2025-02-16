import http from 'k6/http';
import { sleep, check } from 'k6';

// Функция для генерации UUID v4 (если потребуется для тестов)
function uuidv4() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    let r = Math.random() * 16 | 0;
    let v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

// Функция для случайного выбора элемента из массива
function randomItem(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

export let options = {
  scenarios: {
    createReviews: {
      executor: 'ramping-arrival-rate',
      startRate: 10,
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
    spikeUpdates: {
      executor: 'constant-arrival-rate',
      rate: 100,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      duration: '30s',
      exec: 'updateEmployees',
    },
    soakEmployees: {
      executor: 'constant-vus',
      vus: 50,
      duration: '10m',
      exec: 'getEmployees',
    },
  },
};

// Функция setup – создание команд и сотрудников
export function setup() {
  const teamCount = 10;
  let teams = [];
  // Создаём команды через POST /teams
  for (let i = 0; i < teamCount; i++) {
    let payload = JSON.stringify({
      name: "Team " + (i + 1)
    });
    let params = { headers: { "Content-Type": "application/json" } };
    let res = http.post("http://localhost:8081/teams", payload, params);
    if (res.status === 201) {
      let teamId = JSON.parse(res.body).id;
      teams.push(teamId);
    } else {
      console.error("Failed to create team " + (i + 1) + ": status " + res.status);
    }
    sleep(0.1);
  }

  // Инициализируем счетчики для каждой команды и массив для хранения созданных employeeId
  let teamCounts = {};
  let teamManagers = {};
  teams.forEach(t => { teamCounts[t] = 0; });

  let createdEmployeeIds = [];
  const totalEmployees = 10000;

  // Создаем сотрудников, распределяя их по командам (максимум 1000 в каждой)
  for (let i = 0; i < totalEmployees; i++) {
    let availableTeams = teams.filter(t => teamCounts[t] < 1000);
    if (availableTeams.length === 0) break;
    let teamId = randomItem(availableTeams);
    // Если в команде ещё нет менеджера, то supervisorId = null, сотрудник становится менеджером
    let supervisorId = teamManagers[teamId] || null;

    let payload = JSON.stringify({
      teamId: teamId,
      firstName: `FirstName${i}`,
      lastName: `LastName${i}`,
      email: `employee${i}@example.com`,
      position: "Developer",
      joiningDate: new Date().toISOString().split('T')[0],
      supervisorId: supervisorId
    });
    let params = { headers: { "Content-Type": "application/json" } };
    let res = http.post("http://localhost:8081/employees", payload, params);
    if (res.status === 201) {
      let newId = JSON.parse(res.body).id;
      createdEmployeeIds.push(newId);
      teamCounts[teamId]++;
      if (!teamManagers[teamId]) {
        teamManagers[teamId] = newId;
      }
    } else {
      console.error(`Failed to create employee ${i}: status ${res.status}`);
    }
    sleep(0.005);
  }

  return { employeeIds: createdEmployeeIds, teamIds: teams, teamManagers: teamManagers };
}

// Сценарий: Создание ревью для случайного сотрудника из пула
export function createReviews(data) {
  let empId = randomItem(data.employeeIds);
  let teamId = randomItem(data.teamIds);
  let payload = JSON.stringify({
    employeeId: empId,
    teamId: teamId,
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

// Сценарий: Запрос иерархии для случайного сотрудника
export function getHierarchy(data) {
  let empId = randomItem(data.employeeIds);
  let res = http.get(`http://localhost:8081/employees/${empId}/hierarchy`);
  check(res, { "hierarchy retrieved": (r) => r.status === 200 });
  sleep(1);
}

// Сценарий: Обновление сотрудника (кадровые изменения, спайк)
export function updateEmployees(data) {
  let empId = randomItem(data.employeeIds);
  let teamId = randomItem(data.teamIds);
  let payload = JSON.stringify({
    teamId: teamId,
    firstName: "Updated",
    lastName: "User",
    email: `updated_${__VU}_${__ITER}@example.com`,
    position: "Senior Developer",
    supervisorId: null // или можно назначать случайного менеджера
  });
  let params = { headers: { "Content-Type": "application/json" } };
  let res = http.put(`http://localhost:8081/employees/${empId}`, payload, params);
  check(res, { "employee updated": (r) => r.status === 200 });
  sleep(1);
}

// Сценарий: Получение списка всех сотрудников (soak тест)
export function getEmployees() {
  let res = http.get("http://localhost:8081/employees");
  check(res, { "employees list retrieved": (r) => r.status === 200 });
  sleep(1);
}
