for port in 8080 8081 8082 8083 8084 8085 8086 8087; do
    pid=$(ss -ltnp 2>/dev/null | grep ":$port " | grep -oP 'pid=\K[0-9]+' | head -1)
    if [ -n "$pid" ]; then
        kill "$pid" 2>/dev/null && echo "  ■ port $port (pid $pid) đã dừng"
    fi
done
sleep 2
ss -ltn 2>/dev/null | grep -E ":808[0-7] " || echo "Tất cả service đã dừng (Docker hạ tầng vẫn chạy)"