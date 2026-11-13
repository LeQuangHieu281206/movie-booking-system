# Graph Report - movie-booking-system  (2026-11-12)

## Corpus Check
- 224 files · ~77,439 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 32 file(s) not represented in the graph (top: .css 18, (none) 6, .properties 3)

## Summary
- 1694 nodes · 4851 edges · 73 communities (58 shown, 15 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 263 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `2bd99b48`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- App.jsx
- org.springframework.context.annotation.Bean
- GenreResponse
- offsetdatetime
- NotFoundException
- NotFoundException
- CinemaResponse
- Room
- BookingServiceImplTest.java
- lombok.RequiredArgsConstructor
- BookingResponse
- UserResponse
- BookingServiceImpl.java
- list
- JwtService
- lombok.extern.slf4j.Slf4j
- spring
- 001_payments_and_hardening.sql
- ApiExceptionHandler.java
- spring
- BookingDetail
- environment
- Movie
- MovieResponse
- Icon
- org.junit.jupiter.api.Test
- BookingServiceImplTest
- package.json
- ShowtimeServiceImpl.java
- org.springframework.transaction.annotation.Transactional
- api-gateway
- GatewayJwtFilterTest.java
- security
- ScheduleBoard.jsx
- api.js
- context
- services
- admin.js
- AuthServiceImpl.java
- Booking
- User
- BookingRepository
- org.springframework.transaction.annotation.Transactional
- rabbitmq
- ShowtimeController.java
- payment-service
- SalesDashboard.jsx
- ShowtimeSeat
- healthcheck
- CinemaController
- frontend
- User
- domain.js
- com.ttcsn:common
- user-service
- BookingCreateRequest.java
- run-services.sh
- AgeRestriction
- stop-services.sh
- ShowtimeResponse
- domain.js
- UserServiceImpl
- com.ttcsn:api-gateway
- com.ttcsn:movie-booking-parent
- react
- SeatHoldResult
- SepayProperties
- Skeleton.jsx
- GenreServiceImpl
- org.springframework.web.bind.annotation.GetMapping
- SeatStatus
- Room

## God Nodes (most connected - your core abstractions)
1. `GatewayJwtFilterTest` - 45 edges
2. `Icon()` - 40 edges
3. `BookingServiceImplTest` - 37 edges
4. `ShowtimeServiceImpl` - 31 edges
5. `BookingServiceImpl` - 30 edges
6. `NotFoundException` - 30 edges
7. `BookingResponse` - 29 edges
8. `Booking` - 29 edges
9. `Movie` - 29 edges
10. `ErrorBox()` - 28 edges

## Surprising Connections (you probably didn't know these)
- `com.ttcsn:booking-service` --depends_on--> `com.ttcsn:common`  [EXTRACTED]
  booking-service/pom.xml → common/pom.xml
- `BookingNotFoundException` --inherits--> `NotFoundException`  [EXTRACTED]
  booking-service/src/main/java/com/ttcsn/booking/exception/BookingNotFoundException.java → common/src/main/java/com/ttcsn/common/exception/NotFoundException.java
- `SeatAlreadyBookedException` --inherits--> `ConflictException`  [EXTRACTED]
  booking-service/src/main/java/com/ttcsn/booking/exception/SeatAlreadyBookedException.java → common/src/main/java/com/ttcsn/common/exception/ConflictException.java
- `com.ttcsn:movie-service` --depends_on--> `com.ttcsn:common`  [EXTRACTED]
  movie-service/pom.xml → common/pom.xml
- `com.ttcsn:notification-service` --depends_on--> `com.ttcsn:common`  [EXTRACTED]
  notification-service/pom.xml → common/pom.xml

## Import Cycles
- None detected.

## Communities (73 total, 15 thin omitted)

### Community 0 - "App.jsx"
Cohesion: 0.14
Nodes (19): adminLink, App(), nav, readRoute(), readTheme(), scrollBehavior(), Workspace(), goLogin() (+11 more)

### Community 1 - "org.springframework.context.annotation.Bean"
Cohesion: 0.11
Nodes (5): RabbitMQConfig, Topics, RabbitMQConfig, RabbitMQConfig, SecurityConfig

### Community 2 - "GenreResponse"
Cohesion: 0.13
Nodes (4): GenreController, GenreRequest, GenreResponse, GenreService

### Community 4 - "NotFoundException"
Cohesion: 0.10
Nodes (9): SeatController, SeatRequest, SeatResponse, SeatType, COUPLE, NORMAL, VIP, SeatServiceImpl (+1 more)

### Community 5 - "NotFoundException"
Cohesion: 0.13
Nodes (6): BookingDetailNotFoundException, NotFoundException, GenreNotFoundException, MovieNotFoundException, ShowtimeSeatNotFoundException, SeatNotFoundException

### Community 6 - "CinemaResponse"
Cohesion: 0.20
Nodes (3): CinemaRequest, CinemaResponse, CinemaServiceImpl

### Community 7 - "Room"
Cohesion: 0.06
Nodes (10): RoomController, RoomRequest, RoomResponse, RoomType, FOUR_DX, IMAX, THREE_D, TWO_D (+2 more)

### Community 8 - "BookingServiceImplTest.java"
Cohesion: 0.12
Nodes (4): PaymentMethod, BANK_TRANSFER, CASH, BookingExpirationSchedulerTest

### Community 9 - "lombok.RequiredArgsConstructor"
Cohesion: 0.06
Nodes (22): InternalBookingController, PaymentCompletedListener, PaymentCompletedListenerTest, PaymentCompletedEvent, InternalShowtimeSeatController, PaymentCompletedListener, BookingClient, SepayProperties (+14 more)

### Community 10 - "BookingResponse"
Cohesion: 0.10
Nodes (7): BookingController, BookingCreateRequest, BookingStatsResponse, RevenueBucket, BookingExpirationScheduler, BookingService, BookingControllerTest

### Community 11 - "UserResponse"
Cohesion: 0.14
Nodes (10): UserController, ProfileUpdateRequest, UserCreateRequest, UserUpdateRequest, UserResponse, UserRole, ADMIN, CUSTOMER (+2 more)

### Community 12 - "BookingServiceImpl.java"
Cohesion: 0.12
Nodes (4): BookingNotFoundException, SeatAlreadyBookedException, BookingPaymentView, ForbiddenException

### Community 13 - "list"
Cohesion: 0.12
Nodes (3): GenreRepository, Showtime, ShowtimeRepository

### Community 14 - "JwtService"
Cohesion: 0.12
Nodes (3): JwtAuthenticationFilter, JwtService, JwtServiceTest

### Community 15 - "lombok.extern.slf4j.Slf4j"
Cohesion: 0.13
Nodes (7): ApiGatewayApplication, BookingServiceApplication, MovieServiceApplication, NotificationServiceApplication, PaymentServiceApplication, UserServiceApplication, VenueServiceApplication

### Community 16 - "spring"
Cohesion: 0.07
Nodes (29): app, booking, services, name, expiry-check-interval-ms, expiry-initial-delay-ms, pending-expiry-minutes, driver-class-name (+21 more)

### Community 17 - "001_payments_and_hardening.sql"
Cohesion: 0.11
Nodes (17): idx_booking_details_booking, idx_booking_details_seat, idx_booking_details_showtime_seat, idx_bookings_showtime_id, idx_bookings_status_time, idx_bookings_user_id, idx_movie_genres_genre, idx_payments_booking (+9 more)

### Community 18 - "ApiExceptionHandler.java"
Cohesion: 0.20
Nodes (3): ErrorResponse, ApiExceptionHandler, ErrorResponseFactory

### Community 19 - "spring"
Cohesion: 0.07
Nodes (27): app, sepay, services, name, driver-class-name, password, url, username (+19 more)

### Community 20 - "BookingDetail"
Cohesion: 0.16
Nodes (5): ShowtimeClient, BookingDetail, BookingDetailRepository, BookingSeatReleaser, SeatIdsRequest

### Community 21 - "environment"
Cohesion: 0.21
Nodes (23): environment, environment, APP_SERVICES_BOOKING_URL, APP_SERVICES_MOVIE_URL, APP_SERVICES_USER_URL, APP_SERVICES_VENUE_URL, BOOKING_SERVICE_URL, INTERNAL_TOKEN (+15 more)

### Community 22 - "Movie"
Cohesion: 0.14
Nodes (3): Movie, MovieRepository, MovieSpecifications

### Community 23 - "MovieResponse"
Cohesion: 0.10
Nodes (9): MovieController, MovieRequest, MovieResponse, MovieStatus, ENDED, SHOWING, UPCOMING, MovieServiceImpl (+1 more)

### Community 24 - "Icon"
Cohesion: 0.21
Nodes (16): DiscoverButton(), Empty(), Icon(), Modal(), Poster(), SearchSelect(), byId(), filterMovies() (+8 more)

### Community 25 - "org.junit.jupiter.api.Test"
Cohesion: 0.06
Nodes (14): GatewaySecurityProperties, Access, ADMIN, CUSTOMER, CUSTOMER_OR_ADMIN, INVALID, PUBLIC, GatewayAuthorizationRules (+6 more)

### Community 26 - "BookingServiceImplTest"
Cohesion: 0.17
Nodes (4): BookingUpdateRequest, BookingResponse, BookingServiceImpl, BookingServiceImplTest

### Community 27 - "package.json"
Cohesion: 0.11
Nodes (20): dependencies, react@19.3.1, react-dom@19.3.1, devDependencies, vite@8.3.5, @vitejs/plugin-react@6.1.2, engines, node (+12 more)

### Community 28 - "ShowtimeServiceImpl.java"
Cohesion: 0.12
Nodes (6): ShowtimeStatus, CANCELLED, ENDED, ONGOING, SCHEDULED, ShowtimeNotFoundException

### Community 29 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.13
Nodes (5): Cinema, CinemaNotFoundException, RoomNotFoundException, CinemaRepository, RoomServiceImpl

### Community 30 - "api-gateway"
Cohesion: 0.11
Nodes (20): command, depends_on, image, ports, restart, build, command, depends_on (+12 more)

### Community 32 - "security"
Cohesion: 0.11
Nodes (17): name, gateway, gateway, security, server, cors-configurations, internal-token, jwt-issuer (+9 more)

### Community 33 - "ScheduleBoard.jsx"
Cohesion: 0.22
Nodes (18): ScheduleBoard(), dayKey(), useLoad(), useNow(), bookableShowtimes(), cityList(), groupByCinema(), longDayFormat (+10 more)

### Community 34 - "api.js"
Cohesion: 0.19
Nodes (14): joinSeats(), createHttpClient(), send(), createLiveApi(), createAuthService(), readSession(), refreshSession(), sessionFromAuthResponse() (+6 more)

### Community 35 - "context"
Cohesion: 0.14
Nodes (18): build, context, dockerfile, build, command, image, restart, build (+10 more)

### Community 36 - "services"
Cohesion: 0.12
Nodes (16): name, image, ports, restart, services, redis, venue-service, command (+8 more)

### Community 38 - "admin.js"
Cohesion: 0.12
Nodes (18): ADMIN_RESOURCES, AGE, columnText(), filterRows(), fromDateTimeInput(), inDayRange(), MOVIE_STATUS, ROLE (+10 more)

### Community 39 - "AuthServiceImpl.java"
Cohesion: 0.29
Nodes (7): AuthController, LoginRequest, RefreshTokenRequest, RegisterRequest, AuthResponse, UserSummary, AuthService

### Community 40 - "Booking"
Cohesion: 0.13
Nodes (6): Booking, BookingStatus, CANCELLED, EXPIRED, PAID, PENDING

### Community 42 - "BookingRepository"
Cohesion: 0.16
Nodes (5): BookingRepository, StatsPeriod, DAY, MONTH, YEAR

### Community 43 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.22
Nodes (3): ConflictException, ShowtimeResponse, ShowtimeServiceImpl

### Community 44 - "rabbitmq"
Cohesion: 0.17
Nodes (11): name, host, password, port, username, server, port, shutdown (+3 more)

### Community 46 - "payment-service"
Cohesion: 0.18
Nodes (11): condition, booking-service, rabbitmq, depends_on, build, command, depends_on, image (+3 more)

### Community 47 - "SalesDashboard.jsx"
Cohesion: 0.23
Nodes (15): count(), niceCeil(), SalesDashboard(), shortMoney(), SKELETON_BARS, SKELETON_HEIGHTS, Sparkline(), trendOf() (+7 more)

### Community 48 - "ShowtimeSeat"
Cohesion: 0.16
Nodes (3): ShowtimeSeat, ShowtimeSeatRepository, ShowtimeServiceImplTest

### Community 49 - "healthcheck"
Cohesion: 0.22
Nodes (9): interval, retries, test, timeout, healthcheck, image, ports, restart (+1 more)

### Community 51 - "frontend"
Cohesion: 0.25
Nodes (8): condition, api-gateway, build, depends_on, image, ports, restart, frontend

### Community 52 - "User"
Cohesion: 0.12
Nodes (6): UnauthorizedException, User, EmailAlreadyExistsException, InvalidCredentialsException, UserRepository, AuthServiceImpl

### Community 53 - "domain.js"
Cohesion: 0.16
Nodes (25): PaymentPanel(), cancel(), createQr(), refresh(), run(), Button(), ErrorBox(), bookingTime() (+17 more)

### Community 54 - "com.ttcsn:common"
Cohesion: 0.29
Nodes (7): com.ttcsn:booking-service, com.ttcsn:common, com.ttcsn:movie-service, com.ttcsn:notification-service, com.ttcsn:payment-service, com.ttcsn:user-service, com.ttcsn:venue-service

### Community 55 - "user-service"
Cohesion: 0.40
Nodes (5): user-service, build, command, image, restart

### Community 58 - "AgeRestriction"
Cohesion: 0.25
Nodes (6): AgeRestriction, K, P, T13, T16, T18

### Community 60 - "ShowtimeResponse"
Cohesion: 0.13
Nodes (4): ShowtimeController, ShowtimeSeatInteractionRequest, ShowtimeSeatView, ShowtimeService

### Community 61 - "domain.js"
Cohesion: 0.21
Nodes (18): assertBooking(), assertBookingShape(), assertCreatedBooking(), assertUserBookings(), BOOKING_STATUSES, changedShow(), differs(), filterTickets() (+10 more)

### Community 65 - "react"
Cohesion: 0.20
Nodes (9): ClickSpark(), EASINGS, resolveColor(), ErrorBoundary, DateField(), commit(), dayText(), dayValue() (+1 more)

### Community 68 - "Skeleton.jsx"
Cohesion: 0.60
Nodes (9): CinemaGridSkeleton(), MovieGridSkeleton(), Placeholder(), ScheduleSkeleton(), SeatMapSkeleton(), Skeleton(), TableSkeleton(), TicketListSkeleton() (+1 more)

### Community 70 - "org.springframework.web.bind.annotation.GetMapping"
Cohesion: 0.12
Nodes (8): MovieClient, UserClient, VenueClient, RoomDto, SeatDto, ShowtimeView, VenueClient, RoomSeatsCache

### Community 71 - "SeatStatus"
Cohesion: 0.40
Nodes (4): SeatStatus, AVAILABLE, BOOKED, HOLDING

### Community 76 - "Room"
Cohesion: 0.11
Nodes (5): InternalVenueController, Room, Seat, RoomRepository, SeatRepository

## Knowledge Gaps
- **49 isolated node(s):** `com.ttcsn:api-gateway`, `PUBLIC`, `CUSTOMER`, `CUSTOMER_OR_ADMIN`, `ADMIN` (+44 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 405 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **15 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `NotFoundException` connect `NotFoundException` to `org.springframework.web.bind.annotation.GetMapping`, `BookingServiceImplTest.java`, `lombok.RequiredArgsConstructor`, `org.springframework.transaction.annotation.Transactional`, `BookingServiceImpl.java`, `ApiExceptionHandler.java`, `ShowtimeServiceImpl.java`, `org.springframework.transaction.annotation.Transactional`, `UserServiceImpl`?**
  _High betweenness centrality (0.030) - this node is a cross-community bridge._
- **What connects `com.ttcsn:api-gateway`, `PUBLIC`, `CUSTOMER` to the rest of the system?**
  _49 weakly-connected nodes found - possible missing edges._
- **Should `App.jsx` be split into smaller, more focused modules?**
  _Cohesion score 0.14 - nodes in this community are weakly interconnected._
- **Why does `BookingDetail` connect `BookingDetail` to `offsetdatetime`, `Booking`, `BookingServiceImplTest.java`, `BookingServiceImpl.java`, `list`?**
  _High betweenness centrality (0.017) - this node is a cross-community bridge._
- **Should `org.springframework.context.annotation.Bean` be split into smaller, more focused modules?**
  _Cohesion score 0.10606060606060606 - nodes in this community are weakly interconnected._
- **Why does `BookingServiceImpl` connect `BookingServiceImplTest` to `org.springframework.web.bind.annotation.GetMapping`, `Booking`, `lombok.RequiredArgsConstructor`, `BookingRepository`, `BookingResponse`, `BookingServiceImpl.java`, `BookingDetail`, `org.springframework.transaction.annotation.Transactional`?**
  _High betweenness centrality (0.015) - this node is a cross-community bridge._
- **Should `GenreResponse` be split into smaller, more focused modules?**
  _Cohesion score 0.12561576354679804 - nodes in this community are weakly interconnected._