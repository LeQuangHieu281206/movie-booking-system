# Graph Report - movie-booking-system  (2026-11-21)

## Corpus Check
- 234 files · ~87,671 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 32 file(s) not represented in the graph (top: .css 18, (none) 6, .properties 3)

## Summary
- 1796 nodes · 5161 edges · 93 communities (64 shown, 29 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 282 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `0b2feb03`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- App.jsx
- Access
- MovieResponse
- offsetdatetime
- NotFoundException
- domain.js
- CinemaResponse
- RoomResponse
- BookingServiceImplTest.java
- org.springframework.context.annotation.Bean
- BookingDetail
- org.springframework.web.bind.annotation.GetMapping
- list
- JwtServiceTest
- lombok.extern.slf4j.Slf4j
- spring
- 001_payments_and_hardening.sql
- org.springframework.http.ResponseEntity
- spring
- BookingServiceImpl.java
- environment
- Movie
- AgeRestriction
- org.springframework.web.bind.annotation.PostMapping
- org.junit.jupiter.api.Test
- BookingServiceImplTest
- package.json
- Showtime
- eureka-server/src/main/resources/application.yml
- eureka-server
- security
- Icon
- api.js
- context
- services
- BookingService
- AuthServiceImpl.java
- Booking
- ApiExceptionHandler.java
- BookingRepository
- ErrorBox
- rabbitmq
- Booking
- UserResponse
- SalesDashboard.jsx
- x-common-env
- healthcheck
- Skeleton.jsx
- frontend
- User
- GatewaySecurityProperties
- com.ttcsn:common
- user-service
- BookingResponse
- run-services.sh
- AuthServiceImplTest.java
- stop-services.sh
- ShowtimeService
- domain.js
- api-gateway
- com.ttcsn:api-gateway
- com.ttcsn:movie-booking-parent
- GenreResponse
- movie-service
- notification-service
- ScheduleBoard.jsx
- com.ttcsn:eureka-server
- org.springframework.transaction.annotation.Transactional
- GatewayJwtFilter.java
- SecurityConfig.java
- ConflictException
- NotFoundException
- ShowtimeSeat
- PaymentService.java
- ShowtimeServiceImpl.java
- RoomType
- Room
- .verify
- admin.js
- Payment.java
- MovieServiceImpl.java
- StatsPeriod
- .createPaymentUsesHeaderUserAndIgnoresClientUserAndAmount

## God Nodes (most connected - your core abstractions)
1. `GatewayJwtFilterTest` - 46 edges
2. `BookingServiceImplTest` - 41 edges
3. `Icon()` - 41 edges
4. `BookingResponse` - 32 edges
5. `User` - 32 edges
6. `BookingServiceImpl` - 31 edges
7. `ShowtimeServiceImpl` - 31 edges
8. `NotFoundException` - 30 edges
9. `Booking` - 29 edges
10. `Movie` - 29 edges

## Surprising Connections (you probably didn't know these)
- `com.ttcsn:booking-service` --depends_on--> `com.ttcsn:common`  [EXTRACTED]
  booking-service/pom.xml → common/pom.xml
- `com.ttcsn:movie-service` --depends_on--> `com.ttcsn:common`  [EXTRACTED]
  movie-service/pom.xml → common/pom.xml
- `com.ttcsn:notification-service` --depends_on--> `com.ttcsn:common`  [EXTRACTED]
  notification-service/pom.xml → common/pom.xml
- `com.ttcsn:payment-service` --depends_on--> `com.ttcsn:common`  [EXTRACTED]
  payment-service/pom.xml → common/pom.xml
- `com.ttcsn:user-service` --depends_on--> `com.ttcsn:common`  [EXTRACTED]
  user-service/pom.xml → common/pom.xml

## Import Cycles
- None detected.

## Communities (93 total, 29 thin omitted)

### Community 0 - "App.jsx"
Cohesion: 0.17
Nodes (19): adminLink, nav, readRoute(), readTheme(), scrollBehavior(), Workspace(), goLogin(), logout() (+11 more)

### Community 1 - "Access"
Cohesion: 0.18
Nodes (8): Access, ADMIN, CUSTOMER, CUSTOMER_OR_ADMIN, INVALID, PUBLIC, GatewayAuthorizationRules, Rule

### Community 2 - "MovieResponse"
Cohesion: 0.11
Nodes (7): MovieController, MovieRequest, MovieStatus, ENDED, SHOWING, UPCOMING, MovieService

### Community 4 - "NotFoundException"
Cohesion: 0.07
Nodes (13): SeatController, SeatRequest, SeatResponse, SeatType, COUPLE, NORMAL, VIP, Seat (+5 more)

### Community 5 - "domain.js"
Cohesion: 0.21
Nodes (19): filterTickets(), PaymentPanel(), cancel(), createQr(), refresh(), run(), bookingTime(), date() (+11 more)

### Community 6 - "CinemaResponse"
Cohesion: 0.11
Nodes (5): CinemaController, CinemaRequest, CinemaResponse, CinemaService, CinemaServiceImpl

### Community 7 - "RoomResponse"
Cohesion: 0.16
Nodes (4): RoomController, RoomRequest, RoomResponse, RoomService

### Community 8 - "BookingServiceImplTest.java"
Cohesion: 0.18
Nodes (4): PaymentMethod, BANK_TRANSFER, CASH, BookingExpirationSchedulerTest

### Community 9 - "org.springframework.context.annotation.Bean"
Cohesion: 0.17
Nodes (4): RabbitMQConfig, Topics, RabbitMQConfig, RabbitMQConfig

### Community 12 - "org.springframework.web.bind.annotation.GetMapping"
Cohesion: 0.14
Nodes (7): UserClient, VenueClient, BookingPaymentView, RoomDto, SeatDto, VenueClient, InternalVenueController

### Community 13 - "list"
Cohesion: 0.09
Nodes (8): ShowtimeRepository, BookingClient, SePayWebhookDto, PaymentRepository, PaymentService, PaymentServiceTest, CinemaRepository, RoomRepository

### Community 15 - "lombok.extern.slf4j.Slf4j"
Cohesion: 0.11
Nodes (8): ApiGatewayApplication, BookingServiceApplication, EurekaServerApplication, MovieServiceApplication, NotificationServiceApplication, PaymentServiceApplication, UserServiceApplication, VenueServiceApplication

### Community 16 - "spring"
Cohesion: 0.06
Nodes (31): app, booking, name, expiry-check-interval-ms, expiry-initial-delay-ms, pending-expiry-minutes, service-url, driver-class-name (+23 more)

### Community 17 - "001_payments_and_hardening.sql"
Cohesion: 0.10
Nodes (19): idx_booking_details_booking, idx_booking_details_seat, idx_booking_details_showtime_seat, idx_bookings_showtime_id, idx_bookings_status_time, idx_bookings_user_id, idx_movie_genres_genre, idx_payments_booking (+11 more)

### Community 18 - "org.springframework.http.ResponseEntity"
Cohesion: 0.14
Nodes (3): GenreController, GenreRequest, GenreService

### Community 19 - "spring"
Cohesion: 0.06
Nodes (31): app, sepay, name, service-url, driver-class-name, password, url, username (+23 more)

### Community 20 - "BookingServiceImpl.java"
Cohesion: 0.09
Nodes (8): MovieClient, SeatAlreadyBookedException, SeatHoldResult, SeatIdsRequest, ShowtimeView, ConflictException, ForbiddenException, Roles

### Community 21 - "environment"
Cohesion: 0.48
Nodes (18): environment, environment, EUREKA_HOSTNAME, EUREKA_URL, GOOGLE_CLIENT_ID, INTERNAL_TOKEN, JWT_SECRET, SEPAY_WEBHOOK_API_KEY (+10 more)

### Community 22 - "Movie"
Cohesion: 0.19
Nodes (4): MovieResponse, Movie, MovieRepository, MovieServiceImpl

### Community 23 - "AgeRestriction"
Cohesion: 0.25
Nodes (6): AgeRestriction, K, P, T13, T16, T18

### Community 24 - "org.springframework.web.bind.annotation.PostMapping"
Cohesion: 0.18
Nodes (7): InternalBookingController, InternalShowtimeSeatController, PaymentController, PaymentRequest, PaymentResponse, SepayController, InternalUserController

### Community 27 - "package.json"
Cohesion: 0.07
Nodes (27): dependencies, react@19.3.1, react-dom@19.3.1, @react-oauth/google@^0.13.5, devDependencies, vite@8.3.5, @vitejs/plugin-react@6.1.2, engines (+19 more)

### Community 28 - "Showtime"
Cohesion: 0.12
Nodes (6): ShowtimeStatus, CANCELLED, ENDED, ONGOING, SCHEDULED, Showtime

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
Cohesion: 0.25
Nodes (15): DiscoverButton(), Select(), Empty(), Icon(), Modal(), Poster(), SearchSelect(), filterMovies() (+7 more)

### Community 34 - "api.js"
Cohesion: 0.18
Nodes (15): joinSeats(), createApi(), createHttpClient(), send(), createLiveApi(), createAuthService(), readSession(), refreshSession() (+7 more)

### Community 35 - "context"
Cohesion: 0.10
Nodes (24): build, build, command, image, restart, context, dockerfile, build (+16 more)

### Community 36 - "services"
Cohesion: 0.17
Nodes (11): command, image, ports, restart, name, image, ports, restart (+3 more)

### Community 38 - "BookingService"
Cohesion: 0.16
Nodes (5): BookingController, BookingCreateRequest, BookingResponse, BookingStatsResponse, BookingService

### Community 39 - "AuthServiceImpl.java"
Cohesion: 0.11
Nodes (11): AuthController, GoogleLoginRequest, LoginRequest, RefreshTokenRequest, RegisterRequest, AuthResponse, UserSummary, UserRole (+3 more)

### Community 40 - "Booking"
Cohesion: 0.18
Nodes (6): ShowtimeClient, BookingExpirationScheduler, BookingSeatReleaser, PaymentCompletedListener, PaymentCompletedEvent, PaymentCompletedListener

### Community 41 - "ApiExceptionHandler.java"
Cohesion: 0.17
Nodes (3): ErrorResponse, ApiExceptionHandler, ErrorResponseFactory

### Community 42 - "BookingRepository"
Cohesion: 0.23
Nodes (3): RevenueBucket, BookingRepository, BookingServiceImplTest

### Community 43 - "ErrorBox"
Cohesion: 0.14
Nodes (20): Button(), ErrorBox(), byId(), columnText(), filterRows(), inDayRange(), text(), DateField() (+12 more)

### Community 44 - "rabbitmq"
Cohesion: 0.11
Nodes (17): name, service-url, eureka, client, instance, hostname, host, password (+9 more)

### Community 45 - "Booking"
Cohesion: 0.13
Nodes (6): Booking, BookingStatus, CANCELLED, EXPIRED, PAID, PENDING

### Community 46 - "UserResponse"
Cohesion: 0.13
Nodes (10): UserController, ProfileUpdateRequest, UserCreateRequest, UserUpdateRequest, UserResponse, EmailAlreadyExistsException, UserNotFoundException, AuthenticatedUser (+2 more)

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
Cohesion: 0.06
Nodes (12): UnauthorizedException, User, InvalidCredentialsException, InvalidTokenException, UserRepository, JwtServiceImpl, JwtAuthenticationFilter, JwtService (+4 more)

### Community 53 - "GatewaySecurityProperties"
Cohesion: 0.13
Nodes (3): GatewaySecurityProperties, SepayProperties, JwtProperties

### Community 54 - "com.ttcsn:common"
Cohesion: 0.29
Nodes (7): com.ttcsn:booking-service, com.ttcsn:common, com.ttcsn:movie-service, com.ttcsn:notification-service, com.ttcsn:payment-service, com.ttcsn:user-service, com.ttcsn:venue-service

### Community 55 - "user-service"
Cohesion: 0.40
Nodes (5): user-service, build, command, image, restart

### Community 60 - "ShowtimeService"
Cohesion: 0.13
Nodes (6): ShowtimeController, ShowtimeRequest, ShowtimeSeatInteractionRequest, ShowtimeResponse, ShowtimeSeatView, ShowtimeService

### Community 61 - "domain.js"
Cohesion: 0.23
Nodes (16): assertBooking(), assertBookingShape(), assertCreatedBooking(), assertUserBookings(), BOOKING_STATUSES, changedShow(), differs(), prepareBooking() (+8 more)

### Community 62 - "api-gateway"
Cohesion: 0.40
Nodes (5): command, image, ports, restart, api-gateway

### Community 65 - "GenreResponse"
Cohesion: 0.23
Nodes (5): GenreResponse, Genre, GenreNotFoundException, GenreRepository, GenreServiceImpl

### Community 66 - "movie-service"
Cohesion: 0.40
Nodes (5): build, command, image, restart, movie-service

### Community 67 - "notification-service"
Cohesion: 0.40
Nodes (5): build, command, image, restart, notification-service

### Community 68 - "ScheduleBoard.jsx"
Cohesion: 0.20
Nodes (19): ScheduleBoard(), STEPS, dayKey(), useHorizontalWheel(), useNow(), bookableShowtimes(), cinemaList(), cityList() (+11 more)

### Community 76 - "GatewayJwtFilter.java"
Cohesion: 0.20
Nodes (3): GatewayJwtFilter, JwtVerifier, VerifiedUser

### Community 79 - "NotFoundException"
Cohesion: 0.21
Nodes (4): BookingDetailNotFoundException, BookingNotFoundException, NotFoundException, ShowtimeSeatNotFoundException

### Community 80 - "ShowtimeSeat"
Cohesion: 0.14
Nodes (6): SeatStatus, AVAILABLE, BOOKED, HOLDING, ShowtimeSeat, ShowtimeSeatRepository

### Community 81 - "PaymentService.java"
Cohesion: 0.18
Nodes (4): PaymentStatus, FAILED, PENDING, SUCCESS

### Community 82 - "ShowtimeServiceImpl.java"
Cohesion: 0.11
Nodes (3): SeatIds, MovieNotFoundException, ShowtimeNotFoundException

### Community 83 - "RoomType"
Cohesion: 0.16
Nodes (6): RoomType, FOUR_DX, IMAX, THREE_D, TWO_D, RoomTypeConverter

### Community 84 - "Room"
Cohesion: 0.16
Nodes (3): Room, CinemaNotFoundException, RoomServiceImpl

### Community 86 - "admin.js"
Cohesion: 0.15
Nodes (11): ADMIN_RESOURCES, AGE, fromDateTimeInput(), MOVIE_STATUS, options(), ROLE, ROOM_TYPE, SEAT_TYPE (+3 more)

### Community 89 - "StatsPeriod"
Cohesion: 0.25
Nodes (4): StatsPeriod, DAY, MONTH, YEAR

## Knowledge Gaps
- **50 isolated node(s):** `com.ttcsn:api-gateway`, `PUBLIC`, `CUSTOMER`, `CUSTOMER_OR_ADMIN`, `ADMIN` (+45 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 420 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **29 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Booking` connect `Booking` to `offsetdatetime`, `BookingServiceImplTest.java`, `Booking`, `BookingDetail`, `BookingRepository`, `list`, `BookingServiceImpl.java`, `.verify`, `BookingResponse`, `BookingServiceImplTest`?**
  _High betweenness centrality (0.020) - this node is a cross-community bridge._
- **What connects `com.ttcsn:api-gateway`, `PUBLIC`, `CUSTOMER` to the rest of the system?**
  _50 weakly-connected nodes found - possible missing edges._
- **Should `MovieResponse` be split into smaller, more focused modules?**
  _Cohesion score 0.10804597701149425 - nodes in this community are weakly interconnected._
- **Why does `UserRole` connect `AuthServiceImpl.java` to `offsetdatetime`, `notblank`, `UserResponse`, `User`, `AuthServiceImplTest.java`, `GatewayJwtFilterTest.java`?**
  _High betweenness centrality (0.016) - this node is a cross-community bridge._
- **Should `NotFoundException` be split into smaller, more focused modules?**
  _Cohesion score 0.07320024198427103 - nodes in this community are weakly interconnected._
- **Why does `JwtServiceImpl` connect `User` to `Booking`, `Room`, `GatewaySecurityProperties`, `org.springframework.web.bind.annotation.PostMapping`, `AuthServiceImplTest.java`, `GatewayJwtFilterTest.java`?**
  _High betweenness centrality (0.014) - this node is a cross-community bridge._
- **Should `CinemaResponse` be split into smaller, more focused modules?**
  _Cohesion score 0.1106612685560054 - nodes in this community are weakly interconnected._