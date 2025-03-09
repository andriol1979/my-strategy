# my-strategy
My Strategy to go to the success

## Plan
1. Connect Binance websocket -> get price
2. Connect SQL lite -> store symbol config
3. Connect Redis -> store 10 latest price
4. Get Binance Exchange info -> get lot size -> calculate quantity based on order amount
5. Load WAIT orders when app starting
6. Base on latest price -> find good entry -> trailing buy/sell
7. Separate Binance API between DEV and PROD
8. Ordered -> call Binance API to create order -> update WAIT order -> ORDERED
9. Continue Order -> apply Binance trailing stop loss API
10. Continue Order -> isolated ...
11. Trailing take profit
12. Close order

### Main feature
1. Manual API to start order, position 
2. Trailing buy/sell to get the best entry for this position
3. Trailing stop loss (Binance API supported)
4. Trailing take profit
