### Method: `calculateBullBearVolumeDivergence`
- **Mục đích**: Tính độ lệch (divergence) giữa khối lượng mua (bull volume) và khối lượng bán (bear volume), biểu thị sự thống trị của bên mua hoặc bên bán dưới dạng phần trăm.
- **Input**:
    - `bullVolume`: Khối lượng mua (BigDecimal)
    - `bearVolume`: Khối lượng bán (BigDecimal)
- **Output**: Một giá trị `BigDecimal` biểu thị độ lệch, nằm trong khoảng từ **-100** đến **100**.

---

### Phân tích từng phần code:

1. **Khai báo biến ban đầu**:
   ```java
   BigDecimal bullBearVolumeDivergence;
   final BigDecimal sumBullBearVolume = bullVolume.add(bearVolume);
   boolean bullVolumeIsZero = bullVolume.compareTo(BigDecimal.ZERO) == 0;
   boolean bearVolumeIsZero = bearVolume.compareTo(BigDecimal.ZERO) == 0;
   ```
    - `sumBullBearVolume`: Tổng khối lượng mua và bán.
    - `bullVolumeIsZero` và `bearVolumeIsZero`: Kiểm tra xem khối lượng mua hoặc bán có bằng 0 không (dùng `compareTo` vì BigDecimal không dùng `==` trực tiếp).

2. **Trường hợp 1: Cả hai đều bằng 0**:
   ```java
   if (bullVolumeIsZero && bearVolumeIsZero) {
       bullBearVolumeDivergence = BigDecimal.ZERO;
   }
   ```
    - Nếu cả `bullVolume` và `bearVolume` đều = 0 → Độ lệch = **0**.
    - Ý nghĩa: Không có hoạt động nào, không có sự thống trị.

3. **Trường hợp 2: Chỉ bearVolume bằng 0**:
   ```java
   else if (bearVolumeIsZero) {
       bullBearVolumeDivergence = Calculator.ONE_HUNDRED; // Bull thắng tuyệt đối
   }
   ```
    - Nếu `bearVolume = 0` và `bullVolume > 0` → Độ lệch = **100**.
    - Ý nghĩa: Bên mua (bull) thắng tuyệt đối.

4. **Trường hợp 3: Chỉ bullVolume bằng 0**:
   ```java
   else if (bullVolumeIsZero) {
       bullBearVolumeDivergence = Calculator.ONE_HUNDRED.negate(); // Bear thắng tuyệt đối
   }
   ```
    - Nếu `bullVolume = 0` và `bearVolume > 0` → Độ lệch = **-100**.
    - Ý nghĩa: Bên bán (bear) thắng tuyệt đối.

5. **Trường hợp 4: bullVolume lớn hơn bearVolume**:
   ```java
   else if (bullVolume.compareTo(bearVolume) > 0) {
       bullBearVolumeDivergence = ((bullVolume.subtract(bearVolume))
               .divide(sumBullBearVolume, Calculator.SCALE, Calculator.ROUNDING_MODE_HALF_UP))
               .multiply(Calculator.ONE_HUNDRED).setScale(2, Calculator.ROUNDING_MODE_HALF_UP);
   }
   ```
    - **Công thức**: `((bullVolume - bearVolume) / (bullVolume + bearVolume)) * 100`
    - Ý nghĩa: Tính tỷ lệ phần trăm mà bên mua vượt trội hơn bên bán.
    - Các bước:
        - `(bullVolume - bearVolume)`: Độ chênh lệch tuyệt đối.
        - `.divide(sumBullBearVolume, ...)`: Chia cho tổng khối lượng để ra tỷ lệ tương đối.
        - `.multiply(Calculator.ONE_HUNDRED)`: Nhân 100 để đổi sang phần trăm.
        - `.setScale(2, ...)`: Làm tròn về 2 chữ số thập phân.
    - Kết quả: Giá trị dương từ **0** đến **100** (không bao gồm 100 vì đã xử lý trường hợp `bearVolume = 0`).

6. **Trường hợp 5: bearVolume lớn hơn hoặc bằng bullVolume**:
   ```java
   else {
       bullBearVolumeDivergence = ((bearVolume.subtract(bullVolume))
               .divide(sumBullBearVolume, Calculator.SCALE, Calculator.ROUNDING_MODE_HALF_UP))
               .multiply(Calculator.ONE_HUNDRED)
               .negate().setScale(2, Calculator.ROUNDING_MODE_HALF_UP);
   }
   ```
    - **Công thức**: `-((bearVolume - bullVolume) / (bullVolume + bearVolume)) * 100`
    - Ý nghĩa: Tính tỷ lệ phần trăm mà bên bán vượt trội hơn bên mua, đổi dấu âm để biểu thị bear thắng.
    - Các bước tương tự trường hợp 4, nhưng:
        - `.negate()`: Đổi dấu thành âm.
    - Kết quả: Giá trị âm từ **-100** đến **0** (không bao gồm -100 vì đã xử lý trường hợp `bullVolume = 0`).

---

### Giá trị trả về:
- **Khoảng giá trị**: Từ **-100** đến **100**.
    - **-100**: Bear thắng tuyệt đối (bullVolume = 0, bearVolume > 0).
    - **0**: Không có sự thống trị (bullVolume = bearVolume, hoặc cả hai = 0).
    - **100**: Bull thắng tuyệt đối (bearVolume = 0, bullVolume > 0).
- **Độ chính xác**: Làm tròn đến 2 chữ số thập phân (do `setScale(2, ...)`).

---

### Ví dụ minh họa:
1. `bullVolume = 80`, `bearVolume = 20`:
    - `(80 - 20) / (80 + 20) = 60 / 100 = 0.6`
    - `0.6 * 100 = 60.00`
    - Kết quả: `60.00` (bull thắng).

2. `bullVolume = 30`, `bearVolume = 70`:
    - `(70 - 30) / (30 + 70) = 40 / 100 = 0.4`
    - `0.4 * 100 = 40`
    - `-40 = -40.00`
    - Kết quả: `-40.00` (bear thắng).

3. `bullVolume = 0`, `bearVolume = 50`:
    - Kết quả: `-100.00`.

4. `bullVolume = 50`, `bearVolume = 0`:
    - Kết quả: `100.00`.

5. `bullVolume = 0`, `bearVolume = 0`:
    - Kết quả: `0`.

---

### Ý nghĩa thực tế:
- Method này thường dùng trong phân tích thị trường tài chính để đo lường sự chênh lệch giữa lực mua và bán.
- Giá trị dương → Thị trường nghiêng về bên mua.
- Giá trị âm → Thị trường nghiêng về bên bán.
- Độ lớn của giá trị thể hiện mức độ thống trị (càng gần 100 hoặc -100 thì càng mạnh).