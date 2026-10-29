# Отчёт по DevOps Midterm — Student API

| | |
|---|---|
| **Студент** | Nooruzbek Toktobaev (Токтобаев Ноорузбек) |
| **GitHub** | https://github.com/NooruzbekT/toktobaev-nooruzbek-student-api |
| **Приложение** | http://localhost:8080/api/info, http://localhost:8080/api/students |
| **Jenkins** | http://localhost:8090/job/Toktobaev_Nooruzbek_Student_API/ |
| **Окружение** | Windows 11 + WSL2 Ubuntu 24.04, Docker 28.4 (Docker Desktop), Java 21, Gradle 8.14 (wrapper) |

Все доказательства лежат в папке [`evidence/`](evidence). Каждый `.txt` — это реальная команда и её реальный вывод, записанные скриптом [`evidence/run.sh`](evidence/run.sh). Из этих `.txt` скрипт [`evidence/render.py`](evidence/render.py) делает PNG «как в терминале». Скриншоты GitHub и Jenkins сняты из браузера (Playwright, Chromium). Пароли и токены в evidence замаскированы (`********`).

**Приложение:** Spring Boot 3.5 + Java 21, Gradle, сущность `Student` (id, firstName, lastName, email, groupName, age), REST `/api/students`: POST, GET all, GET by id, PUT, DELETE и поиск `?lastName=`. Несуществующий id → **404** с JSON-ошибкой (`@RestControllerAdvice`), невалидные данные → **400**, дубликат email → **409**. `/api/info` возвращает `{"app":"Student API","author":"Nooruzbek Toktobaev","version":"1.1.0"}`. БД — PostgreSQL 16, все настройки подключения берутся из переменных окружения. Тесты (MockMvc + интеграционные на H2, профиль `test`) проходят без PostgreSQL: `./gradlew test`.

---

## Part 1 — Linux administration

### 1.1 Project structure
Структура создана командами `mkdir -p toktobaev-nooruzbek-student-api/{scripts,logs,backups,src}` и `touch Dockerfile docker-compose.yml Jenkinsfile build.gradle README.md`, плюс `touch scripts/Toktobaev_Nooruzbek_system_report.sh logs/Toktobaev_Nooruzbek_system_report.log`. Дерево показано через `tree`, наличие каждого файла проверено через `test -e`, а пустой лог — через `wc -c` (0 байт). Затем в `src/` и корень добавлены исходники Spring Boot и Gradle wrapper.

[🖼 01-create-structure](evidence/part1-linux/01-create-structure.png) [🖼 02-tree](evidence/part1-linux/02-tree.png) [🖼 03-verify-files](evidence/part1-linux/03-verify-files.png) [🖼 04-app-sources](evidence/part1-linux/04-app-sources.png) [🖼 15-structure-final](evidence/part1-linux/15-structure-final.png)

### 1.2 System monitoring script
[`scripts/Toktobaev_Nooruzbek_system_report.sh`](scripts/Toktobaev_Nooruzbek_system_report.sh) (`#!/bin/bash`, с комментариями) выводит: имя студента, дату и время, пользователя (`whoami`), ОС и ядро (`/etc/os-release`, `uname -a`), CPU (`lscpu`, `nproc`), память (`free -h`), диски (`df -h`), свободное место на `/` и в папке проекта, размер проекта (`du -sh`). Весь вывод идёт через `tee -a` и одновременно печатается и дописывается в [`logs/Toktobaev_Nooruzbek_system_report.log`](logs/Toktobaev_Nooruzbek_system_report.log). Для macOS есть fallback'и (`sw_vers`, `sysctl`, `vm_stat`). Скрипт запускался 3 раза (в том числе из другой папки), поэтому в логе 3 записи.

[🖼 05-script-source (1/2)](evidence/part1-linux/05-script-source-1.png) [🖼 05-script-source (2/2)](evidence/part1-linux/05-script-source-2.png) [🖼 06-script-run-1](evidence/part1-linux/06-script-run-1.png) [🖼 08-script-run-2](evidence/part1-linux/08-script-run-2.png)

### 1.3 Permissions and backup
Сначала скрипт имел права `-rw-r--r--`, и `./script` выдавал `Permission denied`. После `chmod +x` он стал `-rwxr-xr-x`, затем `chmod 744` дал `-rwxr--r--`: владелец rwx, группа и остальные только read. Это проверено через `ls -l` и `stat`. README скопирован в `backups/README_Toktobaev_Nooruzbek_backup.md`; одинаковое содержимое доказано через `diff` (нет различий) и совпадающие `sha256sum`.

[🖼 07-permissions](evidence/part1-linux/07-permissions.png) [🖼 09-readme](evidence/part1-linux/09-readme.png) [🖼 10-backup](evidence/part1-linux/10-backup.png)

### 1.4 Processes and logs
`ps aux | grep -i java` и `pgrep -a java` сначала ничего не нашли: Java не была запущена. После `./gradlew clean build --daemon` в системе появился Java-процесс Gradle Daemon, и он виден в `ps aux`, `pgrep -a java` и `ps -o pid,ppid,user,...`. Позже Java-процесс приложения в контейнере показан через `docker top` (Part 3, 10-docker-exec). `tail -n 5` выводит последние 5 строк лога. `grep -n "Toktobaev" ... > logs/Toktobaev_Nooruzbek_log_analysis.txt` сохраняет найденные строки с номерами в [`logs/Toktobaev_Nooruzbek_log_analysis.txt`](logs/Toktobaev_Nooruzbek_log_analysis.txt).

[🖼 11-processes](evidence/part1-linux/11-processes.png) [🖼 12-log-tail](evidence/part1-linux/12-log-tail.png) [🖼 13-log-search](evidence/part1-linux/13-log-search.png) [🖼 14-redirect-demo](evidence/part1-linux/14-redirect-demo.png)

**Разница `>` и `>>` (output redirection):**
- `>` — **redirect with overwrite**: файл создаётся или очищается, и туда пишется новый вывод. Два раза `echo ... > file` дают в файле только последнюю строку.
- `>>` — **append redirect**: вывод дописывается в конец файла, старое содержимое сохраняется.
- В демонстрации (14-redirect-demo) после двух `>` в файле одна строка, а после двух `>>` — три строки. Поэтому для анализа лога я использовал `>` (каждый раз новый результат поиска), а скрипт мониторинга пишет в лог через `tee -a`, то есть как `>>` (история накапливается).

---

## Part 2 — Git and GitHub

### 2.1 Repository initialization
`git init -b main`, локальный `git config user.name "Nooruzbek Toktobaev"` и `user.email`. В [`.gitignore`](.gitignore) — `build/`, `.gradle/`, `*.class`, `.idea/`, `*.iml`, **`.env`**, `*.log` (с исключением для моего лога-evidence). Есть `.env.example` без реальных значений, а `git check-ignore -v .env` подтверждает, что `.env` игнорируется. `gradlew` и скрипты закоммичены с битом `+x`, в `.gitattributes` задано `eol=lf`. Первый commit: **`Initial commit: Toktobaev Nooruzbek Student API`**.

[🖼 01-git-init](evidence/part2-git/01-git-init.png) [🖼 02-gitignore](evidence/part2-git/02-gitignore.png) [🖼 03-first-commit](evidence/part2-git/03-first-commit.png)

### 2.2 Feature branch `feature/toktobaev-nooruzbek-crud`
Улучшена операция Read: добавлен **поиск студентов по фамилии** `GET /api/students?lastName=tok` (без учёта регистра, частичное совпадение, сортировка; пустой параметр возвращает весь список). Добавлен интеграционный тест `StudentSearchTest`, README обновлён (имя и описание изменения). Три коммита:
- `feat(crud): add search of students by last name — Toktobaev Nooruzbek`
- `test(crud): cover last name search with integration test — Toktobaev Nooruzbek`
- `docs: describe CRUD search change by Toktobaev Nooruzbek`

Ветка влита в `main` через `git merge --no-ff`, поэтому в истории виден merge commit.

[🖼 04-feature-crud (1/2)](evidence/part2-git/04-feature-crud-1.png) [🖼 04-feature-crud (2/2)](evidence/part2-git/04-feature-crud-2.png) [🖼 05-merge-crud](evidence/part2-git/05-merge-crud.png)

### 2.3 Conflict resolution
В ветке `feature/toktobaev-nooruzbek-documentation` и в `main` по-разному изменена одна и та же строка README (`**Description:** ...`). `git merge` выдал `CONFLICT (content): Merge conflict in README.md`; `git status` показал `both modified`, в файле появились маркеры `<<<<<<<`, `=======`, `>>>>>>>`. Конфликт разрешён вручную: обе формулировки объединены в одну строку, маркеры удалены (проверено через `grep -c`). Затем `git add README.md` и commit **`Resolve README merge conflict — Toktobaev Nooruzbek`**. Итоговая история — `git log --oneline --graph --all --decorate`.

[🖼 06-conflict (1/3)](evidence/part2-git/06-conflict-1.png) [🖼 06-conflict (2/3)](evidence/part2-git/06-conflict-2.png) [🖼 06-conflict (3/3)](evidence/part2-git/06-conflict-3.png) [🖼 07-merge-conflict (1/2)](evidence/part2-git/07-merge-conflict-1.png) [🖼 07-merge-conflict (2/2)](evidence/part2-git/07-merge-conflict-2.png) [🖼 08-resolve](evidence/part2-git/08-resolve.png)

### 2.4 Remote repository and tag
Перед push проверено, что секретов в git нет: `.env` не отслеживается, `git grep password|token` находит только плейсхолдеры (`${SPRING_DATASOURCE_PASSWORD}`, `change_me` в `.env.example`), а реальный пароль БД не встречается ни в одном коммите (`git grep` по `git rev-list --all`). Репозиторий создан через `gh repo create toktobaev-nooruzbek-student-api --public`, запушены `main` и обе feature-ветки. Создан и запушен annotated tag **`v1.0-toktobaev-nooruzbek`**.

[🖼 09-secrets-check](evidence/part2-git/09-secrets-check.png) [🖼 10-github](evidence/part2-git/10-github.png) [🖼 11-tag](evidence/part2-git/11-tag.png) [🖼 12-remote-summary](evidence/part2-git/12-remote-summary.png)

Скриншоты GitHub: [🖼 13-github-repo](evidence/part2-git/13-github-repo.png) [🖼 14-github-commits](evidence/part2-git/14-github-commits.png) [🖼 15-github-branches](evidence/part2-git/15-github-branches.png) [🖼 16-github-tags](evidence/part2-git/16-github-tags.png) [🖼 17-github-merge-crud-commit](evidence/part2-git/17-github-merge-crud-commit.png) [🖼 18-github-conflict-resolution-commit](evidence/part2-git/18-github-conflict-resolution-commit.png) [🖼 19-github-network-graph](evidence/part2-git/19-github-network-graph.png)

**Почему `.env`, пароли и учётные данные БД нельзя коммитить:**
1. Git хранит **всю историю**. Даже если файл потом удалить, пароль останется в старом commit, и его достанет любой, у кого есть клон.
2. Публичный репозиторий читают все, включая ботов, которые автоматически ищут на GitHub ключи и пароли. Утёкший пароль от БД или токен — это доступ к данным и инфраструктуре.
3. У каждого окружения (dev/test/prod) свои секреты, и в коде им не место (принцип 12-factor: config in the environment).
4. Правильно так: `.env` в `.gitignore`, в репозитории только `.env.example` с плейсхолдерами, значения передаются через переменные окружения или Docker secrets, в Jenkins — через **Credentials**. Если секрет всё-таки попал в git, его надо **сменить (rotate)**, а не только удалить из файла.

---

## Part 3 — Docker and Docker Compose

### 3.1 Dockerfile (multi-stage)
[`Dockerfile`](Dockerfile): stage `build` на `eclipse-temurin:21-jdk` (официальный JDK). Сначала копируются Gradle wrapper и build-файлы, и зависимости скачиваются отдельным слоем (layer cache). Потом копируется `src` и выполняется `./gradlew clean bootJar -x test`. Тесты здесь пропускаются, потому что Jenkins запускает их отдельной стадией Test **до** Docker Build. Runtime stage — `eclipse-temurin:21-jre-alpine`: только JRE и `app.jar`, приложение работает под non-root пользователем `student`, `EXPOSE 8080`, `ENTRYPOINT ["java","-jar","/app/app.jar"]`, в LABEL указаны автор и версия. Есть [`.dockerignore`](.dockerignore). Образ `toktobaev-nooruzbek-student-api:v1.0-toktobaev-nooruzbek`; размер и дата создания показаны через `docker images`, `docker image inspect` и `docker history`.

[🖼 01-dockerfile](evidence/part3-docker/01-dockerfile.png) [🖼 02-docker-build](evidence/part3-docker/02-docker-build.png) [🖼 03-image-details](evidence/part3-docker/03-image-details.png)

### 3.2 Docker Compose + PostgreSQL
[`docker-compose.yml`](docker-compose.yml): `name: toktobaev-nooruzbek-student-api`. Сервис `db` (`postgres:16-alpine`, контейнер `toktobaev-nooruzbek-postgres`) берёт переменные `POSTGRES_*` из `.env`, хранит данные в named volume `toktobaev-nooruzbek-pgdata`, проверяется healthcheck'ом `pg_isready` и подключён к сети `toktobaev-nooruzbek-net`. Сервис `app` (контейнер `toktobaev-nooruzbek-app`) подключается по `SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/...` (через **имя сервиса `db`**, а не localhost), запускается после `depends_on: db: condition: service_healthy`, порт `8080:8080`, `restart: unless-stopped`. После `docker compose up -d --build` оба контейнера работают, db в статусе `healthy`. В логах приложения видно подключение HikariPool к PostgreSQL 16, а `getent hosts db` внутри app показывает, как Docker DNS резолвит имя сервиса.

[🖼 04-compose-file (1/2)](evidence/part3-docker/04-compose-file-1.png) [🖼 04-compose-file (2/2)](evidence/part3-docker/04-compose-file-2.png) [🖼 05-compose-up](evidence/part3-docker/05-compose-up.png) [🖼 06-compose-logs](evidence/part3-docker/06-compose-logs.png)

### 3.3 Container management
1. Список контейнеров — `docker ps`. [🖼 07-docker-ps](evidence/part3-docker/07-docker-ps.png)
2. Логи — `docker logs toktobaev-nooruzbek-app --tail 50`. [🖼 08-docker-logs (1/2)](evidence/part3-docker/08-docker-logs-1.png) [🖼 08-docker-logs (2/2)](evidence/part3-docker/08-docker-logs-2.png)
3. Конфигурация — `docker inspect` (env, порты, сеть, restart policy, mounts и healthcheck БД; пароль замаскирован). [🖼 09-docker-inspect](evidence/part3-docker/09-docker-inspect.png)
4. Команды внутри контейнера — `docker exec`: `java -version`, `whoami` (non-root), `ls /app`, `env | grep SPRING_`, `/actuator/health`, `psql \dt` в БД, `docker top`. [🖼 10-docker-exec](evidence/part3-docker/10-docker-exec.png)
5. Stop/start/restart — `docker compose stop app` (API недоступен), `start`, `restart`. [🖼 11-stop-start](evidence/part3-docker/11-stop-start.png)
6. Новая версия — `1.0.0 → 1.1.0` в `build.gradle`, Dockerfile и compose, новый тег образа `v1.1-toktobaev-nooruzbek`. После `docker compose up -d --build` в `docker images` два тега, а `/api/info` показывает `"version":"1.1.0"`. [🖼 19-version-bump (1/2)](evidence/part3-docker/19-version-bump-1.png) [🖼 19-version-bump (2/2)](evidence/part3-docker/19-version-bump-2.png)
7. Persistence — создан студент, затем `docker compose down` (без `-v`): контейнеры удалены, volume остался. После `docker compose up -d` запись на месте (и через API, и через `psql`). [🖼 20-persistence (1/2)](evidence/part3-docker/20-persistence-1.png) [🖼 20-persistence (2/2)](evidence/part3-docker/20-persistence-2.png)

### 3.4 API testing (curl + Postman)
Все запросы шли на `http://localhost:8080`, тестовые данные — с моим именем:
- POST «Nooruzbek Toktobaev» → **201 Created** + `Location: /api/students/1`; ещё два студента; невалидный POST → **400** с `fieldErrors`. [🖼 12-api-create](evidence/part3-docker/12-api-create.png)
- GET all (+ поиск `?lastName=tokto`) → **200**. [🖼 13-api-get-all](evidence/part3-docker/13-api-get-all.png)
- GET by id → **200**. [🖼 14-api-get-by-id](evidence/part3-docker/14-api-get-by-id.png)
- PUT (группа DevOps-1 → DevOps-2, возраст 21 → 22) → **200**. [🖼 15-api-update](evidence/part3-docker/15-api-update.png)
- DELETE → **204**, повторный GET → **404**. [🖼 16-api-delete](evidence/part3-docker/16-api-delete.png)
- GET/DELETE несуществующего id 9999 → **404** с JSON `{"status":404,"message":"Student with id 9999 not found",...}`. [🖼 17-api-not-found](evidence/part3-docker/17-api-not-found.png)
- Postman collection: [`postman/Toktobaev_Nooruzbek_Student_API.postman_collection.json`](postman/Toktobaev_Nooruzbek_Student_API.postman_collection.json) — 10 запросов с тестами (`pm.test`), `studentId` сохраняется автоматически. [🖼 18-postman](evidence/part3-docker/18-postman.png)

---

## Part 4 — Jenkins CI/CD

### 4.1 Jenkins setup
Jenkins работает локально в Docker и настроен полностью как код. Настройки лежат **вне репозитория** в `~/toktobaev-nooruzbek-jenkins/`, потому что там токен:
- `Dockerfile`: `FROM jenkins/jenkins:lts-jdk21`, Docker CLI + buildx (из `docker:28-cli`), плагины ставятся через `jenkins-plugin-cli`: workflow-aggregator, git, github, credentials-binding, configuration-as-code, job-dsl, pipeline-stage-view, timestamper, junit, docker-workflow;
- `casc.yaml` (JCasC): пользователь `admin`, инструмент **JDK `jdk21`** (Gradle запускается через wrapper `./gradlew`), credential **`github-token`** (GitHub user + token, значение берётся из переменной окружения), Job DSL `pipelineJob('Toktobaev_Nooruzbek_Student_API')`: мой GitHub-репозиторий, ветка `*/${BRANCH}`, параметр `BRANCH=main`, `scriptPath('Jenkinsfile')`, триггер `pollSCM('H/2 * * * *')`;
- `jenkins.env` (chmod 600) — пароль admin и токен, в git не попадает;
- `start-jenkins.sh` — `docker run` с портом **8090** (8080 занят приложением), volume `toktobaev-nooruzbek-jenkins-home`, проброшенным `/var/run/docker.sock` (Docker-capable агент; контейнер запущен от root, чтобы был доступ к сокету) и `-Djenkins.install.runSetupWizard=false`.

Сборка #1 показала, что Jenkins забирает код из GitHub с credential `github-token` (`Checking out Revision ... (refs/remotes/origin/main)`).

[🖼 02-jenkins-setup (1/3)](evidence/part4-jenkins/02-jenkins-setup-1.png) [🖼 02-jenkins-setup (2/3)](evidence/part4-jenkins/02-jenkins-setup-2.png) [🖼 02-jenkins-setup (3/3)](evidence/part4-jenkins/02-jenkins-setup-3.png) [🖼 03-jenkins-start](evidence/part4-jenkins/03-jenkins-start.png) [🖼 04-jenkins-config-api](evidence/part4-jenkins/04-jenkins-config-api.png) [🖼 24-jenkins-credentials](evidence/part4-jenkins/24-jenkins-credentials.png) [🖼 25-jenkins-tools-jdk](evidence/part4-jenkins/25-jenkins-tools-jdk.png)

### 4.2 Jenkinsfile
[`Jenkinsfile`](Jenkinsfile) (declarative): `environment` — `STUDENT_NAME = 'Nooruzbek Toktobaev'`, `IMAGE_NAME = 'toktobaev-nooruzbek-student-api'`, `IMAGE_TAG = 'v1.1-toktobaev-nooruzbek'`. В `options` — `timestamps()`, `buildDiscarder`, `disableConcurrentBuilds`; `tools { jdk 'jdk21' }`; параметр `BRANCH`, триггер `pollSCM`. Стадии:
1. **Checkout** — `checkout scmGit(...)` ветки из параметра `BRANCH` с credential `github-token`, `echo "Build by ${STUDENT_NAME}"`, `git log -1` (имя видно в логе);
2. **Build** — `chmod +x gradlew`, `./gradlew clean assemble`;
3. **Test** — `./gradlew test`, в `post { always { junit ... } }` публикуется отчёт;
4. **Docker Build** — `docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .`;
5. **Verify** — `docker image inspect`, `docker images | grep`, итоговое сообщение;
6. `post { success / failure }` — сообщения с именем студента.

Ошибки нигде не подавляются (нет `|| true`, `catchError`), поэтому падение Build или Test останавливает pipeline.

[🖼 01-jenkinsfile (1/2)](evidence/part4-jenkins/01-jenkinsfile-1.png) [🖼 01-jenkinsfile (2/2)](evidence/part4-jenkins/01-jenkinsfile-2.png)

### 4.3 Pipeline execution and troubleshooting
| Build | Ветка | Результат | Что показывает |
|---|---|---|---|
| #1 | `main` | ✅ SUCCESS | первый запуск, все 5 стадий |
| #2 | `test/toktobaev-nooruzbek-pipeline-failure` | ❌ FAILURE | намеренно сломанный тест |
| #3 | `test/toktobaev-nooruzbek-pipeline-failure` | ✅ SUCCESS | после исправления |
| #4 | `main` | ✅ SUCCESS | повторный запуск на main |
| #5 | `main` | ✅ SUCCESS | после исправления Checkout для polling (см. 4.4) |
| #6 | `main` | ✅ SUCCESS | **Started by an SCM change** — автоматический запуск |
| #7 | `main` | ✅ SUCCESS | Started by an SCM change — коммит с отчётом |
| #8 | `main` | ✅ SUCCESS | Started by an SCM change — коммит с новыми записями лога |

[🖼 05-build-main-success](evidence/part4-jenkins/05-build-main-success.png) [🖼 06-failure-branch](evidence/part4-jenkins/06-failure-branch.png) [🖼 07-build-failure (1/2)](evidence/part4-jenkins/07-build-failure-1.png) [🖼 07-build-failure (2/2)](evidence/part4-jenkins/07-build-failure-2.png) [🖼 08-fix-and-rerun (1/2)](evidence/part4-jenkins/08-fix-and-rerun-1.png) [🖼 08-fix-and-rerun (2/2)](evidence/part4-jenkins/08-fix-and-rerun-2.png) [🖼 09-main-rerun](evidence/part4-jenkins/09-main-rerun.png)

Скриншоты Jenkins: [🖼 15-jenkins-job-page](evidence/part4-jenkins/15-jenkins-job-page.png) [🖼 16-jenkins-console-build-1-success](evidence/part4-jenkins/16-jenkins-console-build-1-success.png) [🖼 17-jenkins-console-build-2-failure](evidence/part4-jenkins/17-jenkins-console-build-2-failure.png) [🖼 18-jenkins-test-report-build-2](evidence/part4-jenkins/18-jenkins-test-report-build-2.png) [🖼 19-jenkins-console-build-3-fixed](evidence/part4-jenkins/19-jenkins-console-build-3-fixed.png)

Полные консоли всех сборок лежат в [`evidence/part4-jenkins/consoles/`](evidence/part4-jenkins/consoles).

**Анализ падения (build #2):**
- **Что сделано:** в отдельной ветке `test/toktobaev-nooruzbek-pipeline-failure` (не в `main`) в тесте `StudentControllerTest.createReturns201WithLocation` ожидаемая фамилия изменена с `Toktobaev` на `Toktobayev`. Commit: `test: intentional failing test for pipeline demo — Toktobaev Nooruzbek`.
- **Где упало:** Checkout и Build прошли успешно (код компилируется). Упала стадия **Test**: `./gradlew test` вернул ненулевой exit code. JUnit-отчёт показывает 1 failed из 8 (`AssertionError: JSON path "$.lastName" expected:<Toktobayev> but was:<Toktobaev>`).
- **Почему следующие стадии пропущены:** в declarative pipeline упавший шаг переводит стадию и весь build в FAILURE, поэтому **Docker Build и Verify не выполнялись**. В консоли: `Stage "Docker Build" skipped due to earlier failure(s)`, `Stage "Verify" skipped due to earlier failure(s)`. Stage View/wfapi помечает их красным, но длительность у них ~0.3 с: шаги внутри не запускались. Образ из непроверенного кода не собирается, и именно в этом смысл CI. Выполнился только `post { failure }`.
- **Исправление:** ожидаемое значение возвращено (`fix(test): restore correct expected last name after pipeline demo — Toktobaev Nooruzbek`), push, повторный запуск (#3) → все стадии SUCCESS. `main` всё это время не ломался.

### 4.4 Automation
**Trigger:** Jenkins работает на `localhost:8090`, и GitHub не может достучаться до него webhook'ом: для webhook нужен публичный URL `http://<host>/github-webhook/`. Поэтому используется **SCM polling** — `pollSCM('H/2 * * * *')`: Jenkins каждые ~2 минуты делает `git ls-remote` и запускает build, если в ветке появился новый commit. (Альтернатива для webhook на локальной машине — туннель smee.io или ngrok, который пробрасывает запросы GitHub к локальному Jenkins.)

**Troubleshooting polling:** первый commit на `main` (`docs: add Docker and Jenkins CI sections to README`) сборку не запустил: polling log каждые 2 минуты писал `No changes`. Причина — branch spec job'а `*/${BRANCH}`: во время сборки параметр подставляется, а при **polling Git-плагин параметры не раскрывает**, поэтому шаблон не совпадал ни с одной веткой. Исправление: стадия Checkout делает явный `checkout scmGit(branches: [[name: "*/${params.BRANCH}"]], ...)`. Тогда в сборке записывается реальная ветка (`*/main`), и polling сравнивает именно её. Commit `ci: check out the BRANCH parameter explicitly so SCM polling works — Toktobaev Nooruzbek`, ручной build #5.

[🖼 10-polling-fix (1/2)](evidence/part4-jenkins/10-polling-fix-1.png) [🖼 10-polling-fix (2/2)](evidence/part4-jenkins/10-polling-fix-2.png)

**Демонстрация:** на `main` сделан новый commit (`docs: link midterm report from README — Toktobaev Nooruzbek`) и push. Сборку я вручную не запускал: через ~1–2 минуты Jenkins сам запустил build **#6**, у которого cause — **«Started by an SCM change»**. В polling log видно `Changes found`. История сборок и консоль — ниже.

[🖼 11-scm-trigger](evidence/part4-jenkins/11-scm-trigger.png) [🖼 12-scm-build](evidence/part4-jenkins/12-scm-build.png) [🖼 13-build-history](evidence/part4-jenkins/13-build-history.png) [🖼 14-jenkins-dashboard](evidence/part4-jenkins/14-jenkins-dashboard.png) [🖼 20-jenkins-console-build-6-scm](evidence/part4-jenkins/20-jenkins-console-build-6-scm.png) [🖼 21-jenkins-polling-log](evidence/part4-jenkins/21-jenkins-polling-log.png) [🖼 22-jenkins-build-history](evidence/part4-jenkins/22-jenkins-build-history.png) [🖼 23-jenkins-build-6-cause](evidence/part4-jenkins/23-jenkins-build-6-cause.png)

**CI vs Continuous Delivery vs Continuous Deployment:**
- **Continuous Integration (CI)** — каждое изменение в общем репозитории автоматически собирается и тестируется (у меня: commit → pollSCM → Checkout → Build → Test → Docker Build → Verify). Цель — быстро найти ошибку и держать `main` в рабочем состоянии.
- **Continuous Delivery** — продолжение CI: после успешных проверок артефакт (JAR или Docker image) автоматически готов к релизу и может быть выкачен в production в любой момент, но **сам релиз запускается вручную** (кнопка или approval).
- **Continuous Deployment** — следующий шаг: каждое изменение, прошедшее все проверки, **автоматически, без участия человека** выкатывается в production.
- В этом проекте реализован CI с автоматической сборкой Docker-образа; deployment не делался (по заданию не требуется).

---

## Итог по требованиям индивидуализации

| Ресурс | Значение |
|---|---|
| Linux script | `scripts/Toktobaev_Nooruzbek_system_report.sh` |
| Log / analysis | `logs/Toktobaev_Nooruzbek_system_report.log`, `logs/Toktobaev_Nooruzbek_log_analysis.txt` |
| Backup | `backups/README_Toktobaev_Nooruzbek_backup.md` |
| Git repository | `toktobaev-nooruzbek-student-api` |
| Feature branches | `feature/toktobaev-nooruzbek-crud`, `feature/toktobaev-nooruzbek-documentation`, `test/toktobaev-nooruzbek-pipeline-failure` |
| Commits | все коммиты с «Toktobaev Nooruzbek», первый — `Initial commit: Toktobaev Nooruzbek Student API` |
| Tag | `v1.0-toktobaev-nooruzbek` |
| Docker image / tag | `toktobaev-nooruzbek-student-api:v1.0-toktobaev-nooruzbek`, `:v1.1-toktobaev-nooruzbek` |
| Compose project / containers | `toktobaev-nooruzbek-student-api` / `toktobaev-nooruzbek-app`, `toktobaev-nooruzbek-postgres` |
| Network / volume | `toktobaev-nooruzbek-net` / `toktobaev-nooruzbek-pgdata` |
| Jenkins job | `Toktobaev_Nooruzbek_Student_API` |
| Jenkins console | `Build by Nooruzbek Toktobaev`, `SUCCESS: pipeline of Nooruzbek Toktobaev ...` |
| Spring Boot app | `/api/info` → `"author":"Nooruzbek Toktobaev"`, тестовые данные «Nooruzbek Toktobaev» |
