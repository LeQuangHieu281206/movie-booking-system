# Graph Report - movie-booking-system  (2026-11-21)

## Corpus Check
- 241 files · ~99,370 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 32 file(s) not represented in the graph (top: .css 18, (none) 6, .properties 3)

## Summary
- 1840 nodes · 5323 edges · 98 communities (62 shown, 36 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 304 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `8b7d57e0`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- App.jsx
- Access
- MovieResponse
- offsetdatetime
- SeatResponse
- BookingResponse
- CinemaResponse
- RoomResponse
- BookingServiceImplTest.java
- PaymentServiceTest
- BookingDetail
- AuthServiceImpl
- org.springframework.web.bind.annotation.GetMapping
- list
- JwtService
- lombok.extern.slf4j.Slf4j
- spring
- 001_payments_and_hardening.sql
- org.springframework.http.ResponseEntity
- spring
- main.jsx
- environment
- BookingPaymentView
- AgeRestriction
- lombok.RequiredArgsConstructor
- org.junit.jupiter.api.Test
- domain.js
- package.json
- UserResponse
- eureka-server/src/main/resources/application.yml
- eureka-server
- GatewayJwtFilterTest.java
- security
- Icon
- api.js
- context
- services
- BookingResponse
- AuthResponse
- lombok.extern.slf4j.Slf4j
- org.springframework.web.bind.annotation.PostMapping
- BookingServiceImpl.java
- ApiExceptionHandler.java
- rabbitmq
- NotFoundException
- org.springframework.context.annotation.Bean
- SalesDashboard.jsx
- x-common-env
- healthcheck
- Skeleton.jsx
- frontend
- User
- PaymentService.java
- com.ttcsn:common
- user-service
- JwtServiceImpl
- run-services.sh
- stop-services.sh
- org.springframework.transaction.annotation.Transactional
- domain.js
- api-gateway
- com.ttcsn:api-gateway
- com.ttcsn:movie-booking-parent
- GatewaySecurityProperties
- movie-service
- notification-service
- ScheduleBoard.jsx
- com.ttcsn:eureka-server
- JwtAuthenticationFilter.java
- .getBookingStats
- AuthServiceImpl.java
- RoomType
- Seat
- BookingServiceImplTest
- GatewayJwtFilter.java
- CineRoomPostgresDialect
- ShowtimeServiceImpl.java
- SecurityConfig.java
- RoomServiceImpl
- Booking
- admin.js
- Payment.java
- MovieServiceImpl.java
- SeatController
- .verify
- BookingStatus
- ForbiddenException
- .createPaymentUsesHeaderUserAndIgnoresClientUserAndAmount

## God Nodes (most connected - your core abstractions)
1. `GatewayJwtFilterTest` - 46 edges
2. `Icon()` - 43 edges
3. `BookingServiceImplTest` - 41 edges
4. `BookingResponse` - 32 edges
5. `User` - 32 edges
6. `BookingServiceImpl` - 31 edges
7. `ShowtimeServiceImpl` - 31 edges
8. `NotFoundException` - 30 edges
9. `Booking` - 29 edges
10. `ErrorBox()` - 29 edges

## Surprising Connections (you probably didn't know these)
- `com.ttcsn:booking-service` --depends_on--> `com.ttcsn:common`  [EXTRACTED]
  booking-service/pom.xml → common/pom.xml
- `BookingNotFoundException` --inherits--> `NotFoundException`  [EXTRACTED]
  booking-service/src/main/java/com/ttcsn/booking/exception/BookingNotFoundException.java → common/src/main/java/com/ttcsn/common/exception/NotFoundException.java
- `com.ttcsn:movie-service` --depends_on--> `com.ttcsn:common`  [EXTRACTED]
  movie-service/pom.xml → common/pom.xml
- `com.ttcsn:notification-service` --depends_on--> `com.ttcsn:common`  [EXTRACTED]
  notification-service/pom.xml → common/pom.xml
- `com.ttcsn:payment-service` --depends_on--> `com.ttcsn:common`  [EXTRACTED]
  payment-service/pom.xml → common/pom.xml

## Import Cycles
- None detected.

## Communities (98 total, 36 thin omitted)

### Community 0 - "App.jsx"
Cohesion: 0.11
Nodes (27): adminLink, App(), nav, readRoute(), readTheme(), scrollBehavior(), Workspace(), goLogin() (+19 more)

### Community 1 - "Access"
Cohesion: 0.18
Nodes (8): Access, ADMIN, CUSTOMER, CUSTOMER_OR_ADMIN, INVALID, PUBLIC, GatewayAuthorizationRules, Rule

### Community 2 - "MovieResponse"
Cohesion: 0.10
Nodes (9): MovieController, MovieRequest, MovieResponse, MovieStatus, ENDED, SHOWING, UPCOMING, MovieServiceImpl (+1 more)

### Community 3 - "offsetdatetime"
Cohesion: 0.12
Nodes (3): Genre, Movie, Cinema

### Community 4 - "SeatResponse"
Cohesion: 0.26
Nodes (3): SeatRequest, SeatResponse, SeatServiceImpl

### Community 5 - "BookingResponse"
Cohesion: 0.14
Nodes (5): BookingController, BookingCreateRequest, BookingResponse, BookingStatsResponse, BookingService

### Community 6 - "CinemaResponse"
Cohesion: 0.11
Nodes (5): CinemaController, CinemaRequest, CinemaResponse, CinemaService, CinemaServiceImpl

### Community 7 - "RoomResponse"
Cohesion: 0.17
Nodes (4): RoomController, RoomRequest, RoomResponse, RoomService

### Community 8 - "BookingServiceImplTest.java"
Cohesion: 0.10
Nodes (5): PaymentMethod, BANK_TRANSFER, CASH, BookingControllerTest, BookingExpirationSchedulerTest

### Community 9 - "PaymentServiceTest"
Cohesion: 0.18
Nodes (4): SePayWebhookDto, PaymentRepository, PaymentService, PaymentServiceTest

### Community 11 - "AuthServiceImpl"
Cohesion: 0.29
Nodes (4): InvalidTokenException, AuthServiceImpl, TypeReference<>, GoogleUserProfile

### Community 12 - "org.springframework.web.bind.annotation.GetMapping"
Cohesion: 0.14
Nodes (7): MovieClient, UserClient, VenueClient, RoomDto, SeatDto, VenueClient, InternalVenueController

### Community 13 - "list"
Cohesion: 0.13
Nodes (5): GenreRepository, MovieRepository, Room, CinemaRepository, RoomRepository

### Community 15 - "lombok.extern.slf4j.Slf4j"
Cohesion: 0.12
Nodes (8): ApiGatewayApplication, BookingServiceApplication, EurekaServerApplication, MovieServiceApplication, NotificationServiceApplication, PaymentServiceApplication, UserServiceApplication, VenueServiceApplication

### Community 16 - "spring"
Cohesion: 0.06
Nodes (33): app, booking, name, expiry-check-interval-ms, expiry-initial-delay-ms, pending-expiry-minutes, service-url, driver-class-name (+25 more)

### Community 17 - "001_payments_and_hardening.sql"
Cohesion: 0.09
Nodes (20): idx_booking_details_booking, idx_booking_details_seat, idx_booking_details_showtime_seat, idx_bookings_showtime_id, idx_bookings_status_time, idx_bookings_user_id, idx_movie_genres_genre, idx_payments_booking (+12 more)

### Community 18 - "org.springframework.http.ResponseEntity"
Cohesion: 0.11
Nodes (6): GenreController, GenreRequest, GenreResponse, GenreNotFoundException, GenreService, GenreServiceImpl

### Community 19 - "spring"
Cohesion: 0.05
Nodes (37): app, payment, sepay, name, service-url, driver-class-name, password, url (+29 more)

### Community 20 - "main.jsx"
Cohesion: 0.27
Nodes (5): ClickSpark(), EASINGS, resolveColor(), ErrorBoundary, react

### Community 21 - "environment"
Cohesion: 0.48
Nodes (18): environment, environment, EUREKA_HOSTNAME, EUREKA_URL, GOOGLE_CLIENT_ID, INTERNAL_TOKEN, JWT_SECRET, SEPAY_WEBHOOK_API_KEY (+10 more)

### Community 22 - "BookingPaymentView"
Cohesion: 0.16
Nodes (10): BookingPaymentView, BookingClient, PaymentStatus, CANCELLED, EXPIRED, FAILED, PENDING, SUCCESS (+2 more)

### Community 23 - "AgeRestriction"
Cohesion: 0.25
Nodes (6): AgeRestriction, K, P, T13, T16, T18

### Community 24 - "lombok.RequiredArgsConstructor"
Cohesion: 0.17
Nodes (10): InternalBookingController, InternalShowtimeSeatController, PaymentController, PaymentRequest, PaymentResponse, SepayController, AuthController, InternalUserController (+2 more)

### Community 26 - "domain.js"
Cohesion: 0.22
Nodes (18): PaymentPanel(), cancel(), createQr(), refresh(), run(), bookingTime(), date(), DEFAULT_PAYMENT_METHOD (+10 more)

### Community 27 - "package.json"
Cohesion: 0.10
Nodes (22): dependencies, react@19.3.1, react-dom@19.3.1, @react-oauth/google@^0.13.5, devDependencies, vite@8.3.5, @vitejs/plugin-react@6.1.2, engines (+14 more)

### Community 28 - "UserResponse"
Cohesion: 0.13
Nodes (10): UserController, ChangePasswordRequest, ProfileUpdateRequest, UserCreateRequest, UserUpdateRequest, UserResponse, UserNotFoundException, AuthenticatedUser (+2 more)

### Community 29 - "eureka-server/src/main/resources/application.yml"
Cohesion: 0.14
Nodes (13): name, fetch-registry, register-with-eureka, eureka, client, server, server, address (+5 more)

### Community 30 - "eureka-server"
Cohesion: 0.13
Nodes (20): depends_on, condition, depends_on, booking-service, eureka-server, movie-service, payment-service, rabbitmq (+12 more)

### Community 32 - "security"
Cohesion: 0.08
Nodes (23): name, service-url, gateway, eureka, client, instance, gateway, security (+15 more)

### Community 33 - "Icon"
Cohesion: 0.19
Nodes (17): DiscoverButton(), Select(), Empty(), Icon(), Modal(), Poster(), SearchSelect(), byId() (+9 more)

### Community 34 - "api.js"
Cohesion: 0.15
Nodes (18): ADMIN_RESOURCES, fromDateTimeInput(), toDateTimeInput(), joinSeats(), createApi(), createHttpClient(), send(), createLiveApi() (+10 more)

### Community 35 - "context"
Cohesion: 0.10
Nodes (24): build, build, command, image, restart, context, dockerfile, build (+16 more)

### Community 36 - "services"
Cohesion: 0.17
Nodes (11): command, image, ports, restart, name, image, ports, restart (+3 more)

### Community 39 - "AuthResponse"
Cohesion: 0.27
Nodes (4): RegisterRequest, AuthResponse, UserSummary, AuthService

### Community 40 - "lombok.extern.slf4j.Slf4j"
Cohesion: 0.26
Nodes (5): ShowtimeClient, BookingExpirationScheduler, BookingSeatReleaser, PaymentCompletedListener, PaymentCompletedListener

### Community 43 - "ApiExceptionHandler.java"
Cohesion: 0.17
Nodes (3): ErrorResponse, ApiExceptionHandler, ErrorResponseFactory

### Community 44 - "rabbitmq"
Cohesion: 0.11
Nodes (17): name, service-url, eureka, client, instance, hostname, host, password (+9 more)

### Community 45 - "NotFoundException"
Cohesion: 0.12
Nodes (7): BookingDetailNotFoundException, NotFoundException, ShowtimeNotFoundException, ShowtimeSeatNotFoundException, CinemaNotFoundException, RoomNotFoundException, SeatNotFoundException

### Community 46 - "org.springframework.context.annotation.Bean"
Cohesion: 0.17
Nodes (4): RabbitMQConfig, Topics, RabbitMQConfig, RabbitMQConfig

### Community 47 - "SalesDashboard.jsx"
Cohesion: 0.23
Nodes (15): count(), niceCeil(), SalesDashboard(), shortMoney(), SKELETON_BARS, SKELETON_HEIGHTS, Sparkline(), trendOf() (+7 more)

### Community 48 - "x-common-env"
Cohesion: 0.25
Nodes (8): x-common-env, EUREKA_URL, GOOGLE_CLIENT_ID, INTERNAL_TOKEN, JWT_SECRET, SEPAY_WEBHOOK_API_KEY, SERVER_ADDRESS, TZ

### Community 49 - "healthcheck"
Cohesion: 0.27
Nodes (10): healthcheck, interval, retries, test, timeout, healthcheck, image, ports (+2 more)

### Community 50 - "Skeleton.jsx"
Cohesion: 0.60
Nodes (9): CinemaGridSkeleton(), MovieGridSkeleton(), Placeholder(), ScheduleSkeleton(), SeatMapSkeleton(), Skeleton(), TableSkeleton(), TicketListSkeleton() (+1 more)

### Community 51 - "frontend"
Cohesion: 0.25
Nodes (8): condition, api-gateway, build, depends_on, image, ports, restart, frontend

### Community 52 - "User"
Cohesion: 0.18
Nodes (4): GoogleLoginRequest, User, UserRepository, AuthServiceImplTest

### Community 54 - "com.ttcsn:common"
Cohesion: 0.29
Nodes (7): com.ttcsn:booking-service, com.ttcsn:common, com.ttcsn:movie-service, com.ttcsn:notification-service, com.ttcsn:payment-service, com.ttcsn:user-service, com.ttcsn:venue-service

### Community 55 - "user-service"
Cohesion: 0.40
Nodes (5): user-service, build, command, image, restart

### Community 60 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.06
Nodes (13): SeatAlreadyBookedException, ConflictException, ShowtimeController, ShowtimeRequest, ShowtimeSeatInteractionRequest, ShowtimeResponse, ShowtimeSeatView, ShowtimeSeat (+5 more)

### Community 61 - "domain.js"
Cohesion: 0.20
Nodes (17): assertBooking(), assertBookingShape(), assertCreatedBooking(), assertUserBookings(), BOOKING_STATUSES, changedShow(), differs(), filterTickets() (+9 more)

### Community 62 - "api-gateway"
Cohesion: 0.40
Nodes (5): command, image, ports, restart, api-gateway

### Community 65 - "GatewaySecurityProperties"
Cohesion: 0.14
Nodes (3): GatewaySecurityProperties, SepayProperties, JwtProperties

### Community 66 - "movie-service"
Cohesion: 0.40
Nodes (5): build, command, image, restart, movie-service

### Community 67 - "notification-service"
Cohesion: 0.40
Nodes (5): build, command, image, restart, notification-service

### Community 68 - "ScheduleBoard.jsx"
Cohesion: 0.21
Nodes (20): ScheduleBoard(), STEPS, dayKey(), sameId(), useHorizontalWheel(), useNow(), bookableShowtimes(), cinemaList() (+12 more)

### Community 74 - "JwtAuthenticationFilter.java"
Cohesion: 0.15
Nodes (4): UserRole, ADMIN, CUSTOMER, JwtAuthenticationFilter

### Community 75 - ".getBookingStats"
Cohesion: 0.15
Nodes (5): RevenueBucket, StatsPeriod, DAY, MONTH, YEAR

### Community 76 - "AuthServiceImpl.java"
Cohesion: 0.13
Nodes (3): UnauthorizedException, EmailAlreadyExistsException, InvalidCredentialsException

### Community 77 - "RoomType"
Cohesion: 0.16
Nodes (6): RoomType, FOUR_DX, IMAX, THREE_D, TWO_D, RoomTypeConverter

### Community 78 - "Seat"
Cohesion: 0.13
Nodes (6): SeatType, COUPLE, NORMAL, VIP, Seat, SeatRepository

### Community 82 - "ShowtimeServiceImpl.java"
Cohesion: 0.07
Nodes (12): SeatIds, SeatStatus, AVAILABLE, BOOKED, HOLDING, ShowtimeStatus, CANCELLED, ENDED (+4 more)

### Community 86 - "admin.js"
Cohesion: 0.13
Nodes (20): AGE, columnText(), filterRows(), inDayRange(), MOVIE_STATUS, options(), ROLE, ROOM_TYPE (+12 more)

### Community 95 - "BookingStatus"
Cohesion: 0.33
Nodes (5): BookingStatus, CANCELLED, EXPIRED, PAID, PENDING

## Knowledge Gaps
- **52 isolated node(s):** `com.ttcsn:api-gateway`, `PUBLIC`, `CUSTOMER`, `CUSTOMER_OR_ADMIN`, `ADMIN` (+47 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 432 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **36 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Booking` connect `Booking` to `BookingResponse`, `BookingServiceImplTest.java`, `lombok.extern.slf4j.Slf4j`, `BookingDetail`, `org.springframework.web.bind.annotation.PostMapping`, `BookingServiceImpl.java`, `list`, `BookingServiceImplTest`, `Payment.java`, `.verify`, `.expireStalePendingBookings`, `BookingStatus`?**
  _High betweenness centrality (0.021) - this node is a cross-community bridge._
- **What connects `com.ttcsn:api-gateway`, `PUBLIC`, `CUSTOMER` to the rest of the system?**
  _52 weakly-connected nodes found - possible missing edges._
- **Should `App.jsx` be split into smaller, more focused modules?**
  _Cohesion score 0.11025641025641025 - nodes in this community are weakly interconnected._
- **Why does `NotFoundException` connect `NotFoundException` to `BookingResponse`, `BookingServiceImplTest.java`, `org.springframework.web.bind.annotation.PostMapping`, `BookingServiceImpl.java`, `ApiExceptionHandler.java`, `PaymentServiceTest`, `UserResponse`, `org.springframework.http.ResponseEntity`, `ShowtimeServiceImpl.java`, `PaymentService.java`, `MovieServiceImpl.java`, `org.springframework.transaction.annotation.Transactional`?**
  _High betweenness centrality (0.016) - this node is a cross-community bridge._
- **Should `MovieResponse` be split into smaller, more focused modules?**
  _Cohesion score 0.10101010101010101 - nodes in this community are weakly interconnected._
- **Why does `User` connect `User` to `AuthResponse`, `BookingServiceImplTest.java`, `JwtAuthenticationFilter.java`, `AuthServiceImpl`, `AuthServiceImpl.java`, `list`, `JwtService`, `Payment.java`, `JwtServiceImpl`, `UserResponse`, `GatewayJwtFilterTest.java`?**
  _High betweenness centrality (0.015) - this node is a cross-community bridge._
- **Should `offsetdatetime` be split into smaller, more focused modules?**
  _Cohesion score 0.12473118279569892 - nodes in this community are weakly interconnected._