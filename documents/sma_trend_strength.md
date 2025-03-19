### 1. Tính SMA Trend Strength (Độ mạnh của thị trường)
Để tính độ mạnh (strength), bạn có thể dựa vào **biến động giá (volatility)**, **khoảng cách giữa SMA Price**, hoặc kết hợp với **volume** (khối lượng giao dịch) nếu bạn có dữ liệu từ WebSocket. Dưới đây là vài cách:

#### Cách 1: Dựa vào khoảng cách giữa max và min SMA Price
- Ý tưởng: Nếu khoảng cách giữa `resistance` (max) và `support` (min) lớn, thị trường có biến động mạnh → strength cao.
- Công thức:
  ```
  Strength = (max SMA Price - min SMA Price) / min SMA Price * 100
  ```
  (Tính theo phần trăm để chuẩn hóa).

#### Code mẫu
```java
public double calculateStrength(List<BigDecimal> smaPrices) {
    if (smaPrices.isEmpty()) return 0.0;

    BigDecimal maxPrice = smaPrices.stream().max(BigDecimal::compareTo).get();
    BigDecimal minPrice = smaPrices.stream().min(BigDecimal::compareTo).get();

    if (minPrice.compareTo(BigDecimal.ZERO) == 0) return 0.0; // Tránh chia cho 0
    return maxPrice.subtract(minPrice)
                   .divide(minPrice, 4, RoundingMode.HALF_UP)
                   .multiply(BigDecimal.valueOf(100))
                   .doubleValue();
}
```

- Kết quả:
    - Strength càng cao (ví dụ > 5%) → thị trường biến động mạnh.
    - Strength thấp (ví dụ < 1%) → thị trường ổn định.
---

### 3. Tích hợp vào SMA Trend
Dựa trên cách bạn đang làm, mình đề xuất cấu trúc `SmaTrend` như sau:

```java
public class SmaTrend {
    private BigDecimal supportPrice; // min SMA Price
    private BigDecimal resistancePrice; // max SMA Price
    private TrendDirection trendDirection; // Up, Down, Sideways
    private double strength; // Độ mạnh

    public SmaTrend(List<BigDecimal> smaPrices, List<BigDecimal> volumes, BigDecimal currentVolume) {
        this.supportPrice = smaPrices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        this.resistancePrice = smaPrices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        this.trendDirection = calculateTrend(smaPrices);
        this.strength = volumes != null && currentVolume != null 
                        ? calculateStrengthWithVolume(volumes, currentVolume) 
                        : calculateStrength(smaPrices);
    }

    // Getters
    public BigDecimal getSupportPrice() { return supportPrice; }
    public BigDecimal getResistancePrice() { return resistancePrice; }
    public TrendDirection getTrendDirection() { return trendDirection; }
    public double getStrength() { return strength; }
}
```

- Thêm điều kiện `smaTrend.getTrendDirection() == TrendDirection.UP` và `smaTrend.getStrength() > 5` (ngưỡng tùy bạn điều chỉnh) để đảm bảo chỉ entry khi xu hướng mạnh.

---

### Đề xuất thêm
- **Ngưỡng Strength**: Tùy thị trường (crypto biến động mạnh), bạn có thể đặt ngưỡng strength từ 3-10 để lọc tín hiệu.
- **Kết hợp RSI**: Nếu có RSI, bạn có thể dùng để xác nhận strength (RSI > 70 → mạnh lên, < 30 → mạnh xuống).
- **Test thực tế**: Chạy thử với dữ liệu WebSocket từ Binance để xem trend và strength có khớp với biến động thật không.
