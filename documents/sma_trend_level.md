
### Cách tính Slope (SMA Trend Level)
- **Ý tưởng**: Slope đo tốc độ thay đổi trung bình của SMA Price qua 5 giá trị. Công thức là:
  ```
  Slope = (SMA cuối - SMA đầu) / (Số khoảng cách giữa các điểm)
  ```
    - `SMA cuối`: Giá trị SMA Price mới nhất (index 0 trong danh sách).
    - `SMA đầu`: Giá trị SMA Price cũ nhất (index 4 trong danh sách 5 điểm).
    - `Số khoảng cách`: Với 5 điểm, có 4 khoảng cách (5 - 1 = 4).

- **Kết quả**:
    - Slope > 0: Xu hướng **Up** (tăng).
    - Slope < 0: Xu hướng **Down** (giảm).
    - Slope gần 0 (trong một ngưỡng): Xu hướng **Sideways** (đi ngang).

---

### Giải thích
1. **Dữ liệu đầu vào**:
    - `smaPrices` là `List<BigDecimal>` chứa 5 SMA Price, sắp xếp từ mới nhất (index 0) đến cũ nhất (index 4).

2. **Tính Slope**:
    - `smaEnd - smaStart`: Sự thay đổi từ đầu đến cuối.
    - Chia cho `intervals` (4): Chuẩn hóa thành độ dốc trung bình trên mỗi bước.

3. **Ngưỡng Sideways**:
    - Dùng ngưỡng 0.1% của `smaStart` (có thể tùy chỉnh, ví dụ 0.05% hoặc 0.2%) để xác định khi nào slope quá nhỏ, coi là đi ngang.
    - Nếu không cần ngưỡng, bạn chỉ cần kiểm tra slope > 0 hoặc < 0.

4. **Kết quả**:
    - Ví dụ trên:  
      Slope = (52000 - 50500) / 4 = 1500 / 4 = 375 (dương) → Trend = **UP**.

---

### Đề xuất
- **Điều chỉnh ngưỡng**: Ngưỡng 0.1% trong `threshold` có thể thay đổi tùy thị trường (crypto biến động mạnh thì tăng lên 0.2-0.5%).
- **Kết hợp volume**: Nếu có volume, bạn có thể nhân slope với tỷ lệ volume để tăng độ chính xác.
- **Test dữ liệu**: Chạy thử với dữ liệu WebSocket từ Binance để xem slope phản ánh đúng xu hướng không.
