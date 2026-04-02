src/main/java/com/auction/
 - model: 
    - Entity.java
    - User.java, Bidder.java, Seller.java, Admin.java
    - Item.java, Electronics.java, Art.java, Vehicle.java
    - ItemType.java, ItemStatus.java, ItemFactory.java
    - Auction.java, BidTransaction.java, AuctionStatus.java
- service: logic 
    - UserManager.java
    - AuctionService.java (concurrency, auto-bidding,...)
- utils: tiện ích dùng chung (format thời gian, tiền tệ, ...)
 - controller: các lớp xử lý luồng sự kiện từ giao diện (JavaFX Controller)
 - dao: Data Access Object - code kết nối, truy cập Database
 - network: client-server (socket / REST API)
 

    