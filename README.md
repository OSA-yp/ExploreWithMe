# Приложение ExploreWithMe

Оно позволяет пользователям делиться информацией об интересных событиях и находить компанию для участия в них.

## Custom feature: Comments

### Общее описание

Выбранная дополнительная фича - комментарии.

Добавляется новая функциональность, связанная с возможностью комментирования событий.
Комментарии привязаны к определенному событию.

Комментировать можно только опубликованные события, для предотвращения накрутки количества комментариев,
добавлять комментарии можно только для чужих событий.

Уровни доступа:

- PUBLIC
    * Просмотр комментариев
- PRIVATE
    * Добавление комментария к событию
    * Редактирование комментария
    * Удаление комментария
    * Получение всех своих комментариев
- ADMIN
    * Получение списка новых комментариев
    * Подтверждение публикации комментария
    * Удаление комментария любого пользователя
    

### Модель данных

Комментарий содержит следующие атрибуты:

* id комментария
* id пользователя, который добавляет комментарий
* id комментируемого события
* текст комментария
* дата создания комментария  (в формате "yyyy-MM-dd HH:mm:ss")
* дата публикации комментария  (в формате "yyyy-MM-dd HH:mm:ss")
* статус комментария (новый, опубликованный, отклоненный)


Статус комментария может быть:
* NEW - новый, при создании комментария
* PUBLISHED - опубликованный, после успешного прохождения модерации
* REJECTED - отклоненный, после отклонения модератором

_______________________________________________________

## PUBLIC endpoints

### GET /events/{eventId}/comments

Получение комментариев к определенному событию:
* Комментарии показываются только если они опубликованы
* Комментарии должны быть отсортированы от более новых к более старым
* Должна быть реализована настраиваемая пагинация

Так как это публичная часть API и внутренний процесс модерации не виден, поля created и status не передаются. 
При отсутствии комментариев должен возвращаться пустой список.

#### Параметры

PATH PARAMETERS

* __eventId__ (required) integer (int64) - id события, комментарии к которому запрашиваются

QUERY PARAMETERS

* __from__ (integer, >= 0, Default: 0) - количество элементов, которые нужно пропустить для формирования текущего набора
* __size__ (integer, Default: 10) - количество элементов в наборе

#### Пример ответа

```json
[
  {
    "id": 123,
    "commentator": 12,
    "event": 45,
    "text": "This is second comment",
    "published": "2025-07-21 17:12:45"
  },
  {
    "id": 12,
    "commentator": 11,
    "event": 45,
    "text": "This is first comment",
    "published": "2025-07-20 11:12:41"
  }
]
```

#### Статусы ответов

* 200 - Успешный ответ на запрос
* 400 - Запрос некорректный 
* 404 - Событие не найдено

_______________________________________________________

## PRIVATE endpoints

### POST /events/{eventId}/comments

Добавление комментария к событию:
* комментарий может быть только к опубликованному событию (EventState.PUBLISHED)
* комментарий может быть только к чужому событию
* при обработке текста комментария должен применяться trim

#### Параметры

HEADERS

* __X-EWM-User-Id__ (integer (int64) userId) - id пользователя

PATH PARAMETERS

* __eventId__ (required) integer (int64) - id комментируемого события

REQUEST BODY SCHEMA: application/json

Данные добавляемого комментария

* __comment__  (required) string [1 .. 2000] characters - текст комментария

#### Пример запроса

```json
{
  "text": "This is test comment"
}
```

#### Пример ответа

```json
{
  "id": 123,
  "commentator": 12,
  "event": 45,
  "text": "This is test comment",
  "created": "2025-07-21 15:32:00",
  "status": "NEW"
}
```

#### Статусы ответов
* 201 - Комментарий создан
* 400 - Запрос составлен некорректно
* 404 - Событие или пользователь не найдены, либо событие не опубликовано
* 403 - Комментировать можно только чужие события


_______________________________________________________

### PATCH /comments/{commentId}

Редактирование комментария:

* редактировать можно только собственные комментарии
* любое изменение комментария переводит его в статус нового

#### Параметры

HEADERS

* __X-EWM-User-Id__ (integer (int64) userId) - id пользователя

PATH PARAMETERS

* __commentId__ (required) integer (int64) - id редактируемого комментария

REQUEST BODY SCHEMA: application/json

Данные добавляемого комментария

* __comment__  (required) string [1 .. 2000] characters - текст комментария

#### Пример запроса

```json
{
  "text": "This is new text for test comment"
}
```

#### Пример ответа

```json
{
  "id": 123,
  "commentator": 12,
  "event": 45,
  "text": "This is new text for test comment",
  "created": "2025-07-21 15:32:00",
  "status": "NEW"
}
```

#### Статусы ответов
* 200 - Комментарий изменен
* 400 - Запрос составлен некорректно
* 404 - Комментарий или пользователь не найдены
* 403 - Редактировать можно только свои комментарии


_______________________________________________________

### DELETE /comments/{commentId}

Удаление комментария:
* удалять можно только свои комментарии
* удаление "жесткое"

#### Параметры

HEADERS

* __X-EWM-User-Id__ (integer (int64) userId) - id пользователя

PATH PARAMETERS

* __commentId__ (required) integer (int64) - id удаляемого комментария


#### Статусы ответов
* 202 - Комментарий удален
* 404 - Комментарий или пользователь не найдены
* 403 - Удалять можно только свои комментарии
_______________________________________________________

### GET /users/comments

Получение всех собственных комментариев пользователя:
* комментарии возвращаются по-умолчанию все, но есть фильтр на любой статус, фильтр работает на 1 значение
* сортировка от более новых к более старым

#### Параметры
HEADERS

* __X-EWM-User-Id__ (integer (int64) userId) - id пользователя


QUERY PARAMETERS
* __filter__ (string, Default: "all") - фильтр комментариев по статусу (new, rejected, published, all)
* __from__ (integer, >= 0, Default: 0) - количество элементов, которые нужно пропустить для формирования текущего набора
* __size__ (integer, Default: 10) - количество элементов в наборе

#### Пример ответа

```json
[
  {
    "id": 123,
    "commentator": 12,
    "event": 45,
    "text": "This is second comment",
    "created": "2025-07-20 21:11:05",
    "published": "2025-07-21 17:42:45",
    "status": "PUBLISHED"
  },
  {
    "id": 12,
    "commentator": 12,
    "event": 4,
    "text": "This is first comment",
    "created": "2025-07-20 21:11:05",
    "published": null,
    "status": "NEW"
  }
]
```
#### Статусы ответов
* 200 - Запрос успешно отработан
* 404 - Пользователь не найден

_______________________________________________________

## ADMIN endpoints


### GET admin/comments

Получение новых комментариев всех пользователей ко всем событиям:
* По-умолчанию показываются только новые комментарии, фильтр работает на 1 значение
* Комментарии должны быть отсортированы от более старых к более новым по дате создания
* Должна быть реализована настраиваемая пагинация

При отсутствии комментариев должен возвращаться пустой список.

#### Параметры

QUERY PARAMETERS
* __filter__ (string, Default: "new") - фильтр комментариев по статусу (new, rejected, published, all)
* __from__ (integer, >= 0, Default: 0) - количество элементов, которые нужно пропустить для формирования текущего набора
* __size__ (integer, Default: 10) - количество элементов в наборе


#### Пример ответа

```json
[
  {
    "id": 123,
    "commentator": 12,
    "event": 45,
    "text" : "Comment text",
    "created": "2025-07-27 21:11:05",
    "published": null,
    "status": "NEW"
  },
  {
    "id": 12,
    "commentator": 11,
    "event": 45,
    "text" : "Comment text",
    "created": "2025-07-21 11:01:35",
    "published": null,
    "status": "NEW"
  },
  {
    "id": 112,
    "commentator": 111,
    "event": 4,
    "text" : "Comment text",
    "created": "2025-07-19 11:01:35",
    "published": "2025-07-21 17:42:45",
    "status": "PUBLISHED"
  },
  {
    "id": 13,
    "commentator": 1,
    "event": 5,
    "text" : "Comment text",
    "created": "2025-01-21 11:01:35",
    "published": null,
    "status": "REJECTED"
  }
]
```

#### Статусы ответов

* 200 - Успешный ответ на запрос
* 400 - Запрос некорректный


### PATCH /admin/comments/{commentId}?status={approved|rejected}

Подтверждение или отклонение публикации комментария:
* применяется только к новым комментариям 


#### Параметры

PATH PARAMETERS

* __status__ (required) string - новый статус комментария, может быть approved или rejected

#### Статусы ответов
* 202 - Статус изменен
* 400 - Неправильный запрос
* 404 - Комментарий не найден
* 403 - Применимо только к новым комментариям

_______________________________________________________

### DELETE /admin/comments/{commentId}

Удаление комментария пользователя:
* комментарий может быть удален в любом статусе
* удаление "жесткое"

#### Параметры

PATH PARAMETERS

* __commentId__ (required) integer (int64) - id удаляемого комментария


#### Статусы ответов
* 204 - Комментарий удален
* 404 - Комментарий не найден

_______________________________________________________

## Архитектура (этап microservices)

На этапе `microservices` проект разделён на инфраструктурные сервисы и доменные микросервисы.

### Инфраструктура

- **Gateway**: `gateway-server` (внешняя точка входа, порт **8080**)
- **Discovery**: `discovery-server` (Eureka, порт **8761**)
- **Config**: `config-server` (порт **8888**, native-конфиги из репозитория)
- **Stats**: `stats-server` (порт **9090**)
- **Kafka**: `kafka` (порт **9092** для клиентов, **9101** для JMX)

### Доменные сервисы

Доменные сервисы расположены в модуле `core/`:

- `core/user-service` — пользователи (`/admin/users/**`) + внутренний API `/internal/users/**`
- `core/category-service` — категории (`/admin/categories/**`, `/categories/**`) + `/internal/categories/**`
- `core/event-service` — события (`/admin/events/**`, `/users/*/events/**`, `/events/**`) + `/internal/events/**`
- `core/request-service` — заявки (`/users/*/requests/**`, `/users/*/events/*/requests/**`) + `/internal/requests/**`
- `core/compilation-service` — подборки (`/admin/compilations/**`, `/compilations/**`)
- `core/comment-service` — комментарии (см. раздел выше)

### Рекомендательные сервисы

Рекомендательные сервисы расположены в модуле `recommendation-services/`:

- `recommendation-services/collector-service` — сбор действий пользователей через gRPC и отправка в Kafka
- `recommendation-services/aggregator-service` — вычисление косинусного сходства мероприятий на основе действий пользователей
- `recommendation-services/analyzer-service` — предоставление рекомендаций через gRPC API
- `recommendation-services/common-proto` — общие Protobuf схемы для gRPC
- `recommendation-services/common-avro` — общие Avro схемы для Kafka

### Где лежат конфигурации

ConfigServer берёт конфиги из:

- `infra/config-server/src/main/resources/config/infra/*` — инфраструктура (в т.ч. маршруты Gateway)
- `infra/config-server/src/main/resources/config/core/*` — доменные сервисы
- `infra/config-server/src/main/resources/config/stats-server/*` — статистика
- `infra/config-server/src/main/resources/config/recommendation-services/*` — рекомендательные сервисы

Маршрутизация внешнего API через Gateway описана в:

- `infra/config-server/src/main/resources/config/infra/gateway-server/application.yaml`

### Внутренний API (межсервисный)

Используется Feign + Eureka. Основные внутренние эндпоинты:

- `GET /internal/users/{userId}`, `GET /internal/users?ids=...`
- `GET /internal/categories/{catId}`, `GET /internal/categories?ids=...`
- `GET /internal/events/{eventId}`, `GET /internal/events/short?ids=...`, `GET /internal/events/exists-by-category?categoryId=...`
- `GET /internal/requests/confirmed-count?eventIds=...`

### Новые эндпоинты (этап 3-2: Рекомендательный сервис)

#### GET /events/recommendations

Получение рекомендаций мероприятий для пользователя на основе его истории взаимодействий.

**HEADERS:**
- `X-EWM-USER-ID` (required) - id пользователя

**QUERY PARAMETERS:**
- `size` (integer, Default: 10) - количество рекомендаций

**Ответ:** Список `EventShortDto` с полем `rating` вместо `views`

#### PUT /events/{eventId}/like

Поставить лайк мероприятию. Пользователь должен предварительно просмотреть мероприятие.

**HEADERS:**
- `X-EWM-USER-ID` (required) - id пользователя

**PATH PARAMETERS:**
- `eventId` (required) - id мероприятия

**Статусы ответов:**
- 200 - Лайк успешно поставлен
- 400 - Пользователь должен просмотреть мероприятие перед тем, как поставить лайк
- 404 - Событие не найдено

## Запуск локально (docker compose)

1) Собрать проект (jar-файлы нужны для docker build, Dockerfile копирует `target/*.jar`):

```bash
mvn clean package -DskipTests
```

2) Поднять микросервисы:

```bash
docker compose up --build
```

После старта:

- внешний API: `http://localhost:8080`
- Eureka: `http://localhost:8761`
- ConfigServer: `http://localhost:8888`
- Stats: `http://localhost:9090`
- Kafka: `localhost:9092`

### Изменения в модели данных

В этапе 3-2 поле `views` (Long) в `EventShortDto` и `EventFullDto` заменено на `rating` (Double). Рейтинг рассчитывается как сумма максимальных весов взаимодействий всех пользователей с мероприятием через сервис Analyzer.

Примечание: в этой ветке запуск рассчитан на микросервисную архитектуру (маршрутизация через Gateway + обнаружение сервисов через Eureka).