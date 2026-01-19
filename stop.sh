#!/bin/bash

# Скрипт для остановки проекта Explore With Me
# 
# Использование:
#   ./stop.sh           - остановка в обычном режиме (Docker)
#   ./stop.sh --test    - остановка в тестовом режиме (локальные процессы)

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

# Функция остановки процессов Java (тестовый режим)
stop_java_processes() {
    print_header "Остановка запущенных сервисов"
    
    local stopped_any=false
    
    # Остановка ewm-service по PID файлу
    if [ -f "logs/ewm-service.pid" ]; then
        local ewm_pid=$(cat logs/ewm-service.pid 2>/dev/null)
        if [ -n "$ewm_pid" ] && ps -p $ewm_pid > /dev/null 2>&1; then
            print_info "Остановка EWM Service (PID: $ewm_pid)"
            kill $ewm_pid 2>/dev/null
            sleep 2
            if ps -p $ewm_pid > /dev/null 2>&1; then
                kill -9 $ewm_pid 2>/dev/null
            fi
            print_success "EWM Service остановлен"
            stopped_any=true
        fi
        rm -f logs/ewm-service.pid
    fi
    
    # Остановка stats-server по PID файлу
    if [ -f "logs/stats-server.pid" ]; then
        local stats_pid=$(cat logs/stats-server.pid 2>/dev/null)
        if [ -n "$stats_pid" ] && ps -p $stats_pid > /dev/null 2>&1; then
            print_info "Остановка Stats Service (PID: $stats_pid)"
            kill $stats_pid 2>/dev/null
            sleep 2
            if ps -p $stats_pid > /dev/null 2>&1; then
                kill -9 $stats_pid 2>/dev/null
            fi
            print_success "Stats Service остановлен"
            stopped_any=true
        fi
        rm -f logs/stats-server.pid
    fi
    
    # Дополнительная проверка процессов по имени (на случай, если PID файлы отсутствуют)
    local ewm_pid=$(ps aux | grep "[j]ava.*MainServer" | awk '{print $2}')
    if [ -n "$ewm_pid" ]; then
        print_info "Обнаружен процесс EWM Service (PID: $ewm_pid), останавливаем..."
        kill $ewm_pid 2>/dev/null
        sleep 2
        kill -9 $ewm_pid 2>/dev/null
        print_success "EWM Service остановлен"
        stopped_any=true
    fi
    
    local stats_pid=$(ps aux | grep "[j]ava.*StatServer" | awk '{print $2}')
    if [ -n "$stats_pid" ]; then
        print_info "Обнаружен процесс Stats Service (PID: $stats_pid), останавливаем..."
        kill $stats_pid 2>/dev/null
        sleep 2
        kill -9 $stats_pid 2>/dev/null
        print_success "Stats Service остановлен"
        stopped_any=true
    fi
    
    if [ "$stopped_any" = false ]; then
        print_info "Сервисы не запущены"
    fi
    
    return 0
}

if [ "$TEST_MODE" = true ]; then
    # ========== ТЕСТОВЫЙ РЕЖИМ (локальные процессы) ==========
    
    print_header "Остановка сервисов в тестовом режиме"
    
    stop_java_processes
    
    print_header "Результат"
    print_success "Все сервисы остановлены!"
    echo ""
    print_info "PID файлы удалены из папки logs/"
    echo ""
    
else
    # ========== ОБЫЧНЫЙ РЕЖИМ (Docker) ==========
    
    print_header "Остановка контейнеров Docker"
    
    docker compose down
    if [ $? -eq 0 ]; then
        print_success "Контейнеры остановлены"
    else
        print_error "Ошибка при остановке контейнеров"
        exit 1
    fi
    
    print_header "Результат"
    print_success "Все контейнеры остановлены!"
    echo ""
    print_info "Для полной очистки (включая volumes) используйте:"
    echo "  docker compose down -v"
    echo ""
fi
