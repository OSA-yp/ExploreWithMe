#!/bin/bash

# Скрипт для пересборки и запуска проекта Explore With Me
# 
# Использование:
#   ./rebuild.sh          - пересборка и запуск в обычном режиме (Docker)
#   ./rebuild.sh --test   - пересборка и запуск в тестовом режиме (локально, H2, без Docker)
#
# В тестовом режиме:
#   - Проект запускается локально без Docker
#   - Используется H2 база данных (in-memory)
#   - Логи сохраняются в папку logs/
#   - Сервисы запускаются в фоновом режиме
#
# Примечание: Этот скрипт выполняет полную пересборку проекта.
# Для обычного запуска используйте ./start.sh
# Для остановки используйте ./stop.sh

# Цвета для вывода
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Флаг для тестового режима
TEST_MODE=false

# Обработка аргументов
if [[ "$1" == "--test" ]]; then
    TEST_MODE=true
fi

# Функция для вывода разделителя
print_separator() {
    echo -e "${BLUE}========================================${NC}"
}

# Функция для вывода заголовка
print_header() {
    echo -e "\n${BLUE}>>> $1${NC}"
    print_separator
}

# Функция для вывода успеха
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Функция для вывода информации
print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Функция для вывода ошибки
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Функция ожидания доступности сервиса
wait_for_url() {
    local url=$1
    local service_name=$2
    local max_retries=60
    local retry=0
    local response_code

    print_info "Ожидание запуска $service_name на $url"

    while [ $retry -lt $max_retries ]; do
        response_code=$(curl -k -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)
        if [[ "$response_code" -eq 200 ]] || [[ "$response_code" -eq 404 ]]; then
            print_success "$service_name доступен (HTTP $response_code)"
            return 0
        else
            if [ $((retry % 6)) -eq 0 ]; then
                echo -n "  Попытка $((retry + 1))/$max_retries... "
                if [ -n "$response_code" ]; then
                    echo "Код ответа: $response_code"
                else
                    echo "Сервис не отвечает"
                fi
            else
                echo -n "."
            fi
            sleep 5
            retry=$((retry + 1))
        fi
    done
    echo ""

    print_error "$service_name не стал доступным в течение $((max_retries * 5)) секунд"
    return 1
}

# Функция остановки процессов Java
stop_java_processes() {
    print_header "Остановка запущенных сервисов"
    
    # Остановка ewm-service
    local ewm_pid=$(ps aux | grep "[j]ava.*MainServer" | awk '{print $2}')
    if [ -n "$ewm_pid" ]; then
        print_info "Остановка EWM Service (PID: $ewm_pid)"
        kill $ewm_pid 2>/dev/null
        sleep 2
        kill -9 $ewm_pid 2>/dev/null
        print_success "EWM Service остановлен"
    fi
    
    # Остановка stats-server
    local stats_pid=$(ps aux | grep "[j]ava.*StatServer" | awk '{print $2}')
    if [ -n "$stats_pid" ]; then
        print_info "Остановка Stats Service (PID: $stats_pid)"
        kill $stats_pid 2>/dev/null
        sleep 2
        kill -9 $stats_pid 2>/dev/null
        print_success "Stats Service остановлен"
    fi
}

if [ "$TEST_MODE" = true ]; then
    # ========== ТЕСТОВЫЙ РЕЖИМ (локально, H2, без Docker) ==========
    
    print_header "ТЕСТОВЫЙ РЕЖИМ: Локальный запуск с H2"
    
    # Остановка предыдущих запущенных процессов
    if [ -f "stop.sh" ]; then
        ./stop.sh --test
    else
        stop_java_processes
    fi
    
    # Создание папки для логов
    print_header "Создание папки для логов"
    mkdir -p logs
    if [ $? -eq 0 ]; then
        print_success "Папка logs создана"
    else
        print_error "Ошибка при создании папки logs"
        exit 1
    fi
    
    # Очистка проекта
    print_header "Очистка проекта Maven"
    mvn clean
    if [ $? -eq 0 ]; then
        print_success "Проект очищен"
    else
        print_error "Ошибка при очистке проекта"
        exit 1
    fi
    
    # Сборка проекта
    print_header "Сборка проекта Maven"
    mvn package -DskipTests
    if [ $? -eq 0 ]; then
        print_success "Проект собран"
    else
        print_error "Ошибка при сборке проекта"
        exit 1
    fi
    
    # Получаем абсолютный путь к корню проекта
    PROJECT_ROOT=$(pwd)
    
    # Запуск stats-server в фоновом режиме
    print_header "Запуск Stats Service (порт 9090)"
    cd "$PROJECT_ROOT/stats-server/service"
    nohup java -jar -Dspring.profiles.active=test -Dlogging.file.name="$PROJECT_ROOT/logs/stats-server.log" target/service-0.0.1-SNAPSHOT.jar > "$PROJECT_ROOT/logs/stats-server-console.log" 2>&1 &
    STATS_PID=$!
    cd "$PROJECT_ROOT"
    
    if [ $? -eq 0 ] && [ -n "$STATS_PID" ]; then
        print_success "Stats Service запущен (PID: $STATS_PID)"
        echo $STATS_PID > logs/stats-server.pid
    else
        print_error "Ошибка при запуске Stats Service"
        exit 1
    fi
    
    # Небольшая задержка перед запуском основного сервиса
    sleep 3
    
    # Запуск ewm-service в фоновом режиме
    print_header "Запуск EWM Service (порт 8080)"
    cd "$PROJECT_ROOT/main-service"
    nohup java -jar -Dspring.profiles.active=test -Dlogging.file.name="$PROJECT_ROOT/logs/ewm-service.log" target/main-service-0.0.1-SNAPSHOT.jar > "$PROJECT_ROOT/logs/ewm-service-console.log" 2>&1 &
    EWM_PID=$!
    cd "$PROJECT_ROOT"
    
    if [ $? -eq 0 ] && [ -n "$EWM_PID" ]; then
        print_success "EWM Service запущен (PID: $EWM_PID)"
        echo $EWM_PID > logs/ewm-service.pid
    else
        print_error "Ошибка при запуске EWM Service"
        stop_java_processes
        exit 1
    fi
    
    # Ожидание готовности сервисов
    print_header "Ожидание готовности сервисов"
    sleep 5
    
    # Проверка stats-server
    if wait_for_url "http://localhost:9090" "Stats Service (порт 9090)"; then
        print_success "Stats Service готов"
    else
        print_error "Stats Service не запустился"
        print_info "Проверьте логи: tail -f logs/stats-server.log"
        stop_java_processes
        exit 1
    fi
    
    # Проверка ewm-service
    if wait_for_url "http://localhost:8080" "EWM Service (порт 8080)"; then
        # Попытка проверить health endpoint, если доступен
        curl -s http://localhost:8080/actuator/health > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            health_status=$(curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4 || echo "unknown")
            print_info "Health статус EWM Service: $health_status"
        fi
        print_success "EWM Service готов"
    else
        print_error "EWM Service не запустился"
        print_info "Проверьте логи: tail -f logs/ewm-service.log"
        stop_java_processes
        exit 1
    fi
    
    print_header "Результат"
    print_success "Все сервисы успешно запущены в тестовом режиме!"
    echo ""
    print_info "EWM Service: http://localhost:8080 (PID: $EWM_PID)"
    print_info "Stats Service: http://localhost:9090 (PID: $STATS_PID)"
    echo ""
    print_info "Логи находятся в папке logs/:"
    echo "  - logs/ewm-service.log - логи EWM Service"
    echo "  - logs/stats-server.log - логи Stats Service"
    echo "  - logs/ewm-service-console.log - консольный вывод EWM Service"
    echo "  - logs/stats-server-console.log - консольный вывод Stats Service"
    echo ""
    print_info "Для просмотра логов используйте:"
    echo "  tail -f logs/ewm-service.log"
    echo "  tail -f logs/stats-server.log"
    echo ""
    print_info "Для остановки сервисов используйте:"
    echo "  kill \$(cat logs/ewm-service.pid)"
    echo "  kill \$(cat logs/stats-server.pid)"
    echo ""
    print_info "Или запустите скрипт снова с --test для перезапуска"
    echo ""
    
else
    # ========== ОБЫЧНЫЙ РЕЖИМ (Docker) ==========
    
    print_header "Остановка контейнеров"
    if [ -f "stop.sh" ]; then
        ./stop.sh
    else
        docker compose down
        if [ $? -eq 0 ]; then
            print_success "Контейнеры остановлены"
        else
            print_error "Ошибка при остановке контейнеров"
            exit 1
        fi
    fi
    
    print_header "Очистка проекта Maven"
    mvn clean
    if [ $? -eq 0 ]; then
        print_success "Проект очищен"
    else
        print_error "Ошибка при очистке проекта"
        exit 1
    fi
    
    print_header "Сборка проекта Maven"
    mvn package -DskipTests
    if [ $? -eq 0 ]; then
        print_success "Проект собран"
    else
        print_error "Ошибка при сборке проекта"
        exit 1
    fi
    
    print_header "Сборка Docker образов"
    docker compose build --no-cache
    if [ $? -eq 0 ]; then
        print_success "Docker образы собраны"
    else
        print_error "Ошибка при сборке Docker образов"
        exit 1
    fi
    
    print_header "Запуск контейнеров"
    docker compose up -d
    if [ $? -eq 0 ]; then
        print_success "Контейнеры запущены"
    else
        print_error "Ошибка при запуске контейнеров"
        exit 1
    fi
    
    # Функция проверки статуса контейнеров
    check_containers_status() {
        print_header "Статус контейнеров"
        docker compose ps
        echo ""
    }
    
    print_header "Проверка статуса контейнеров"
    sleep 3
    check_containers_status
    
    print_header "Ожидание готовности сервисов"
    
    # Проверка основного сервиса (ewm-service)
    if wait_for_url "http://localhost:8080" "EWM Service (порт 8080)"; then
        # Попытка проверить health endpoint, если доступен
        curl -s http://localhost:8080/actuator/health > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            health_status=$(curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4 || echo "unknown")
            print_info "Health статус EWM Service: $health_status"
        fi
    else
        print_error "EWM Service не запустился"
        print_info "Проверьте логи: docker compose logs ewm-service"
        exit 1
    fi
    
    # Проверка сервиса статистики (stats-server)
    if wait_for_url "http://localhost:9090" "Stats Service (порт 9090)"; then
        # Попытка проверить health endpoint, если доступен
        curl -s http://localhost:9090/actuator/health > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            health_status=$(curl -s http://localhost:9090/actuator/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4 || echo "unknown")
            print_info "Health статус Stats Service: $health_status"
        fi
    else
        print_error "Stats Service не запустился"
        print_info "Проверьте логи: docker compose logs stats-server"
        exit 1
    fi
    
    print_header "Финальный статус"
    check_containers_status
    
    print_header "Результат"
    print_success "Все сервисы успешно запущены!"
    echo ""
    print_info "EWM Service: http://localhost:8080"
    print_info "Stats Service: http://localhost:9090"
    echo ""
    print_info "Для просмотра логов используйте:"
    echo "  docker compose logs -f ewm-service"
    echo "  docker compose logs -f stats-server"
    echo ""
fi
