Chi tiết nhất về **response của API Orders từ Binance Futures**, bao gồm cả khi đặt lệnh (`newOrder`) và khi đóng lệnh (thường là thông qua `cancelOrder` hoặc `newOrder` với `side` ngược lại để đóng vị thế). Sau đó, tao sẽ phân tích các field liên quan đến **giá vào (entry price)**, **giá ra (exit price)**, và **PnL (Profit and Loss)** để mày tính lãi/lỗ. Cuối cùng, tao sẽ tạo một **entity Spring Boot** để lưu vào PostgreSQL dựa trên thông tin này.

---

### **1. Response chi tiết của API Orders (Binance Futures)**

#### **Khi đặt lệnh mới (`newOrder`)**
API: `/fapi/v1/order` (POST)  
Dùng để tạo lệnh Market, Limit, Trailing Stop, v.v.  
Ví dụ request đặt Market Order:
```java
NewOrderResponse response = futuresClient.order().newOrder(
    NewOrderParams.builder()
        .symbol("BTCUSDT")
        .side(OrderSide.BUY)
        .type(OrderType.MARKET)
        .quantity("0.01")
        .build()
);
```

**Response mẫu** (JSON):
```json
{
  "symbol": "BTCUSDT",
  "orderId": 123456789,
  "clientOrderId": "6gCrw2kRUAF9CvJDx2vC5g",
  "price": "0",              // Giá đặt lệnh, với Market Order là "0" (dùng giá thị trường)
  "origQty": "0.01000000",   // Số lượng đặt ban đầu
  "executedQty": "0.01000000", // Số lượng đã khớp
  "cumQuote": "500.12345678",  // Tổng giá trị đã khớp (quote asset, ví dụ USDT)
  "reduceOnly": false,        // Có phải lệnh giảm vị thế không
  "status": "FILLED",         // Trạng thái: NEW, PARTIALLY_FILLED, FILLED, CANCELED, etc.
  "timeInForce": "GTC",       // Thời gian hiệu lực (GTC, IOC, FOK), với Market thường mặc định GTC
  "type": "MARKET",           // Loại lệnh
  "side": "BUY",              // BUY hoặc SELL
  "stopPrice": "0",           // Giá stop (0 nếu không dùng)
  "workingType": "CONTRACT_PRICE", // Loại giá áp dụng (thường mặc định)
  "activatePrice": "0",       // Giá kích hoạt (dùng cho Trailing Stop)
  "priceRate": "0",           // Callback rate (dùng cho Trailing Stop)
  "updateTime": 1698765432100, // Thời gian cập nhật (Unix timestamp, ms)
  "avgPrice": "50012.345678", // Giá trung bình khớp lệnh
  "origType": "MARKET",       // Loại lệnh gốc
  "positionSide": "BOTH"      // BOTH (Hedge Mode), LONG, SHORT
}
```

**Các field quan trọng khi mở lệnh:**
- **`avgPrice`**: Giá trung bình khớp lệnh (entry price). Đây là **giá vào** mày cần để tính PnL.
- **`executedQty`**: Số lượng thực tế đã khớp.
- **`cumQuote`**: Tổng giá trị khớp (USDT), có thể dùng để tính giá trung bình nếu cần (`cumQuote / executedQty`).
- **`side`**: Hướng lệnh (BUY = mở Long, SELL = mở Short).
- **`updateTime`**: Thời gian lệnh hoàn tất.

---

#### **Khi đóng lệnh/vị thế**
Binance Futures không có API "close order" trực tiếp (như Spot). Để đóng vị thế:
1. **Đặt lệnh ngược lại** (ví dụ: SELL nếu đang Long, BUY nếu đang Short) với `reduceOnly=true` để chỉ đóng vị thế hiện tại.
2. Hoặc **hủy lệnh nếu chưa khớp** (`/fapi/v1/order`, DELETE).

##### **Đặt lệnh ngược lại để đóng vị thế**
Ví dụ: Đóng vị thế Long bằng Market Order SELL:
```java
NewOrderResponse closeResponse = futuresClient.order().newOrder(
    NewOrderParams.builder()
        .symbol("BTCUSDT")
        .side(OrderSide.SELL)
        .type(OrderType.MARKET)
        .quantity("0.01")
        .reduceOnly(true) // Đảm bảo chỉ đóng vị thế, không mở vị thế mới
        .build()
);
```

**Response mẫu**:
```json
{
  "symbol": "BTCUSDT",
  "orderId": 123456790,
  "clientOrderId": "7hDsw3kRUAF9CvJDx2vC5h",
  "price": "0",
  "origQty": "0.01000000",
  "executedQty": "0.01000000",
  "cumQuote": "510.23456789",
  "reduceOnly": true,
  "status": "FILLED",
  "timeInForce": "GTC",
  "type": "MARKET",
  "side": "SELL",
  "stopPrice": "0",
  "workingType": "CONTRACT_PRICE",
  "activatePrice": "0",
  "priceRate": "0",
  "updateTime": 1698765532100,
  "avgPrice": "51023.456789",
  "origType": "MARKET",
  "positionSide": "BOTH"
}
```

**Các field quan trọng khi đóng lệnh:**
- **`avgPrice`**: Giá trung bình khớp lệnh (exit price). Đây là **giá ra** mày cần.
- **`executedQty`**: Số lượng đã đóng.
- **`cumQuote`**: Tổng giá trị khớp khi đóng.
- **`updateTime`**: Thời gian đóng vị thế.

##### **Lấy PnL từ API Position**
Để tính chính xác PnL, mày cần dùng API `/fapi/v2/positionRisk` (GET) sau khi đóng vị thế:
```java
List<PositionRisk> positions = futuresClient.account().getPositionRisk(
    PositionRiskParams.builder()
        .symbol("BTCUSDT")
        .build()
);
```

**Response mẫu** (khi vị thế đã đóng, `positionAmt` = 0):
```json
[
  {
    "symbol": "BTCUSDT",
    "positionAmt": "0.00000000", // Số lượng vị thế (0 nếu đã đóng)
    "entryPrice": "50012.345678", // Giá vào
    "markPrice": "51023.456789",  // Giá thị trường hiện tại (không áp dụng nếu đã đóng)
    "unRealizedProfit": "0.00000000", // PnL chưa thực hiện (0 nếu đã đóng)
    "liquidationPrice": "0",         // Giá thanh lý (0 nếu không còn vị thế)
    "leverage": "5",                 // Đòn bẩy
    "maxNotionalValue": "5000000",
    "positionSide": "BOTH",
    "updateTime": 1698765532100
  }
]
```

Tuy nhiên, `unRealizedProfit` chỉ có giá trị khi vị thế còn mở. Khi đã đóng, mày cần tính **realized PnL** dựa trên giá vào (`entryPrice`) và giá ra (`avgPrice` từ lệnh đóng).

---

### **2. Tính PnL**
Công thức tính **Realized PnL** cho Futures (Hedge Mode, leverage 5x):
- **Long**: `PnL = (exitPrice - entryPrice) * quantity * leverage`
- **Short**: `PnL = (entryPrice - exitPrice) * quantity * leverage`
- Đơn vị: USDT (quote asset).

Ví dụ:
- Mở Long: `entryPrice = 50012.34`, `quantity = 0.01`, `leverage = 5`.
- Đóng Long: `exitPrice = 51023.45`.
- PnL = `(51023.45 - 50012.34) * 0.01 * 5 = 1011.11 * 0.01 * 5 = 50.5555 USDT`.

---

### **3. Entity Spring Boot cho Table Orders**
Dựa trên các field từ response và yêu cầu lưu giá vào, giá ra, PnL, tao thiết kế entity như sau:

#### **Entity `Order`**
```java
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false)
    private String symbol; // Ví dụ: "BTCUSDT"

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId; // ID lệnh từ Binance

    @Column(name = "client_order_id")
    private String clientOrderId; // ID tùy chỉnh (nếu có)

    @Column(name = "side", nullable = false)
    private String side; // BUY hoặc SELL

    @Column(name = "type", nullable = false)
    private String type; // MARKET, LIMIT, TRAILING_STOP_MARKET, etc.

    @Column(name = "quantity", nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity; // Số lượng

    @Column(name = "entry_price", precision = 18, scale = 8)
    private BigDecimal entryPrice; // Giá vào (avgPrice khi mở)

    @Column(name = "exit_price", precision = 18, scale = 8)
    private BigDecimal exitPrice; // Giá ra (avgPrice khi đóng)

    @Column(name = "cum_quote", precision = 18, scale = 8)
    private BigDecimal cumQuote; // Tổng giá trị khớp

    @Column(name = "leverage", nullable = false)
    private Integer leverage; // Đòn bẩy (mặc định 5)

    @Column(name = "pnl", precision = 18, scale = 8)
    private BigDecimal pnl; // Lãi/lỗ thực hiện

    @Column(name = "status", nullable = false)
    private String status; // NEW, FILLED, CANCELED, etc.

    @Column(name = "position_side", nullable = false)
    private String positionSide; // BOTH, LONG, SHORT

    @Column(name = "created_at", nullable = false)
    private Instant createdAt; // Thời gian mở lệnh

    @Column(name = "closed_at")
    private Instant closedAt; // Thời gian đóng lệnh

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt; // Thời gian cập nhật cuối
}
```

#### **Giải thích các field**
- **`id`**: Khóa chính, tự tăng.
- **`symbol`, `orderId`, `side`, `type`**: Thông tin cơ bản từ Binance response.
- **`quantity`**: Số lượng lệnh (`origQty` hoặc `executedQty`).
- **`entryPrice`**: Lưu `avgPrice` từ lệnh mở (ví dụ: BUY để Long).
- **`exitPrice`**: Lưu `avgPrice` từ lệnh đóng (ví dụ: SELL để thoát Long).
- **`cumQuote`**: Tổng giá trị khớp, để kiểm tra nếu cần.
- **`leverage`**: Đòn bẩy (mặc định 5 như yêu cầu).
- **`pnl`**: Lãi/lỗ tính được sau khi đóng lệnh.
- **`status`**: Trạng thái lệnh.
- **`positionSide`**: Hỗ trợ Hedge Mode (BOTH, LONG, SHORT).
- **`createdAt`, `closedAt`, `updatedAt`**: Thời gian để theo dõi lifecycle của lệnh.

#### **SQL Table (PostgreSQL)**
```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    order_id BIGINT NOT NULL UNIQUE,
    client_order_id VARCHAR(50),
    side VARCHAR(10) NOT NULL,
    type VARCHAR(20) NOT NULL,
    quantity NUMERIC(18,8) NOT NULL,
    entry_price NUMERIC(18,8),
    exit_price NUMERIC(18,8),
    cum_quote NUMERIC(18,8),
    leverage INTEGER NOT NULL,
    pnl NUMERIC(18,8),
    status VARCHAR(20) NOT NULL,
    position_side VARCHAR(10) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
```

---

### **4. Cách lưu vào DB**
Ví dụ logic trong service:
```java
@Service
public class BinanceOrderService {
    private final FuturesClient futuresClient;
    private final OrderRepository orderRepository;

    public BinanceOrderService(FuturesClient futuresClient, OrderRepository orderRepository) {
        this.futuresClient = futuresClient;
        this.orderRepository = orderRepository;
    }

    public void placeAndTrackOrder(String symbol, String side, String quantity) {
        // Đặt lệnh mở
        NewOrderResponse openResponse = futuresClient.order().newOrder(
            NewOrderParams.builder()
                .symbol(symbol)
                .side(OrderSide.valueOf(side))
                .type(OrderType.MARKET)
                .quantity(quantity)
                .build()
        );

        // Lưu lệnh mở vào DB
        Order order = new Order();
        order.setSymbol(openResponse.getSymbol());
        order.setOrderId(openResponse.getOrderId());
        order.setClientOrderId(openResponse.getClientOrderId());
        order.setSide(openResponse.getSide().name());
        order.setType(openResponse.getType().name());
        order.setQuantity(new BigDecimal(openResponse.getExecutedQty()));
        order.setEntryPrice(new BigDecimal(openResponse.getAvgPrice()));
        order.setCumQuote(new BigDecimal(openResponse.getCumQuote()));
        order.setLeverage(5);
        order.setStatus(openResponse.getStatus());
        order.setPositionSide("BOTH");
        order.setCreatedAt(Instant.ofEpochMilli(openResponse.getUpdateTime()));
        order.setUpdatedAt(Instant.ofEpochMilli(openResponse.getUpdateTime()));
        orderRepository.save(order);

        // Đặt lệnh đóng (ví dụ SELL để đóng Long)
        NewOrderResponse closeResponse = futuresClient.order().newOrder(
            NewOrderParams.builder()
                .symbol(symbol)
                .side(OrderSide.SELL)
                .type(OrderType.MARKET)
                .quantity(quantity)
                .reduceOnly(true)
                .build()
        );

        // Cập nhật lệnh sau khi đóng
        order.setExitPrice(new BigDecimal(closeResponse.getAvgPrice()));
        order.setCumQuote(new BigDecimal(closeResponse.getCumQuote()));
        order.setStatus(closeResponse.getStatus());
        order.setClosedAt(Instant.ofEpochMilli(closeResponse.getUpdateTime()));
        order.setUpdatedAt(Instant.ofEpochMilli(closeResponse.getUpdateTime()));

        // Tính PnL
        BigDecimal entry = order.getEntryPrice();
        BigDecimal exit = order.getExitPrice();
        BigDecimal qty = order.getQuantity();
        BigDecimal lev = BigDecimal.valueOf(order.getLeverage());
        BigDecimal pnl = side.equals("BUY") 
            ? exit.subtract(entry).multiply(qty).multiply(lev) // Long
            : entry.subtract(exit).multiply(qty).multiply(lev); // Short
        order.setPnl(pnl);

        orderRepository.save(order);
    }
}

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
```

---

### **Kết luận**
- **Response chi tiết**: Đã cung cấp từ `newOrder` (mở và đóng) và `positionRisk`.
- **Field quan trọng**: `avgPrice` (entry/exit), `quantity`, `leverage` để tính PnL.
- **Entity**: Đã tạo `Order` với đầy đủ field cần thiết, sẵn sàng lưu vào PostgreSQL.
- **Logic mẫu**: Đã tích hợp cách lưu và tính PnL.

Mày thấy ổn không? Nếu cần thêm logic xử lý Trailing Stop hoặc tối ưu entity, cứ bảo tao nhé!