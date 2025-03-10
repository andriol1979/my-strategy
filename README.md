# my-strategy
My Strategy to go to the success

## Plan
1. Connect Binance websocket -> get price                                                        
2. Connect SQL lite -> store symbol config
3. Connect Redis -> store 10 latest price
4. Get Binance Exchange info -> get lot size -> calculate quantity based on order amount
5. Load WAIT orders when app starting
6. Base on latest price -> calculate average each price batch (5 prices/batch) -> find good entry -> trailing buy/sell
-------------------------------------------10.03.2025------------------------
7. Add new websocket to get volume ticker (Ticker Stream: provide closest price, volume... Ex: bnbusdt@ticker))
8. Update logic find good entry based on volume also 
9. Separate Binance API between DEV and PROD
10. Ordered -> call Binance API to create order -> update WAIT order -> ORDERED
11. Continue Order -> apply Binance trailing stop loss API
12. Continue Order -> isolated ...
13. Trailing take profit
14. Close order
15. Sync orders from Binance to app

### Main feature
1. Manual API to start order, position 
2. Trailing buy/sell to get the best entry for this position
3. Trailing stop loss (Binance API supported)
4. Trailing take profit
