# my-strategy
My Strategy to go to the success

## Plan
1. Connect Binance websocket -> get price                                                        
2. Connect SQL lite -> store symbol config
3. Connect Redis -> store 10 latest price
4. Get Binance Exchange info -> get lot size -> calculate quantity based on order amount
5. Load WAIT orders when app starting
6. Base on latest price -> calculate average each price batch (5 prices/batch)
----------------------------10.03.2025--------------------------
7. Apply EMA & SMA
----------------------------12.03.2025--------------------------
8. Process ticker data + store redis to prepare find good entry logic
----------------------------17.03.2025--------------------------
9. Find good entry -> trailing buy/sell
10. Call order (fake data for testing purpose) 
11. Separate Binance API between DEV and PROD
12. Ordered -> call Binance API to create order -> update WAIT order -> ORDERED
13. Continue Order -> apply Binance trailing stop loss API
14. Continue Order -> isolated ...
15. Trailing take profit
16. Close order
17. Sync orders from Binance to app

### Main feature
1. Manual API to start order, position 
2. Trailing buy/sell to get the best entry for this position
3. Trailing stop loss (Binance API supported)
4. Trailing take profit

### Updated - Not yet finished:
1. add threshold in trading_config (done + not use) -> use it to monitor price trend
2. delay_milliseconds: dont use (all symbol use 1 config in application)

### TechDebt:
1. Use sliding windows đ tính AveragePrice gần nhau quá, dẫn đến lúc tính PriceTrend luôn luôn sideways. 
