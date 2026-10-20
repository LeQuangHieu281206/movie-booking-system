cd "$(dirname "$0")"
mkdir -p logs

SERVICES=("venue-service" "user-service" "movie-service" "booking-service" "payment-service" "notification-service" "api-gateway")

for s in "${SERVICES[@]}"; do
    mvn -q -pl "$s" -DskipTests spring-boot:run > "logs/$s.log" 2>&1 &
    echo $! >> logs/pids.txt
    echo "  ▶ $s  (pid $!, log: logs/$s.log)"
done

echo "Đợi service khởi động..."; sleep 45
printf "PORT  SERVICE\n"
for p in 8081:movie 8082:booking 8083:venue 8084:user 8086:payment 8087:notification 8080:gateway; do
    port="${p%%:*}"; name="${p##*:}"
    if ss -ltn 2>/dev/null | grep -q ":$port "; then
        printf "%s UP      %s\n" "$port" "$name"
    else
        printf "%s DOWN ✗  %s (xem logs/)\n" "$port" "$name"
    fi
done
echo "Dừng tất cả: ./stop-services.sh   |  Xem log: tail -f logs/movie-service.log"